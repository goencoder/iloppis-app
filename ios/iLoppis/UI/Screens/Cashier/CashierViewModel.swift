import Foundation
import OSLog
import Security

private let logger = Logger(subsystem: "se.iloppis.app", category: "CashierViewModel")

@MainActor
final class CashierViewModel: ObservableObject {
    @Published private(set) var state: CashierState

    private let eventId: String
    private let apiKey: String
    private let apiClient: ApiClient
    private lazy var heartbeatCoordinator = CashierHeartbeatCoordinator(
        shouldRun: { [eventId, apiKey] in
            !eventId.isEmpty && !apiKey.isEmpty
        },
        requestFactory: { [weak self] in
            guard let self else { return nil }
            return await self.makeHeartbeatRequestForHeartbeatLoop()
        },
        sendHeartbeat: { [apiClient, eventId, apiKey] request in
            try await apiClient.updateCashierPresence(
                eventId: eventId,
                apiKey: apiKey,
                requestBody: request
            )
        },
        onHeartbeatResponse: { [weak self] response in
            await self?.applyHeartbeatResponse(response)
        },
        onHeartbeatFailure: { error in
            logger.warning("Cashier heartbeat failed: \(error.localizedDescription, privacy: .public)")
        }
    )

    init(eventId: String, eventName: String, apiKey: String, apiClient: ApiClient = ApiClient()) {
        self.eventId = eventId
        self.apiKey = apiKey
        self.apiClient = apiClient
        self.state = CashierState(eventName: eventName)

        heartbeatCoordinator.start()
        Task { await loadVendors() }
    }

    func onAction(_ action: CashierAction) {
        switch action {
        case .keypadPress(let digit):
            handleKeypadPress(digit)
        case .keypadClear:
            handleClear()
        case .keypadBackspace:
            handleBackspace()
        case .keypadOk:
            handleOk()
        case .keypadSpace:
            handleSpace()
        case .setActiveField(let field):
            state.activeField = field
        case .removeItem(let id):
            removeItem(id)
        case .clearAllItems:
            clearAllItems()
        case .checkout(let method):
            checkout(method)
        case .setPaidAmount(let amount):
            setPaidAmount(amount)
        case .dismissWarning:
            state.warningMessage = nil
        case .dismissError:
            state.errorMessage = nil
        }
    }

    // MARK: - Data loading

    private func loadVendors() async {
        state.isLoading = true
        defer { state.isLoading = false }

        do {
            logger.info("Loading vendors for event: \(self.eventId, privacy: .public)")
            var allSellers = Set<Int>()
            var nextToken: String? = nil
            var pageCount = 0

            repeat {
                pageCount += 1
                logger.info("Fetching vendor page \(pageCount), pageToken: \(nextToken ?? "nil", privacy: .public)")
                
                let response = try await apiClient.listVendors(
                    eventId: eventId,
                    apiKey: apiKey,
                    pageSize: 100,
                    nextPageToken: nextToken
                )
                
                logger.info("Page \(pageCount): Received \(response.vendors.count, privacy: .public) vendors")
                
                // Log details about each vendor for debugging
                for (index, vendor) in response.vendors.enumerated() {
                    logger.debug("  Vendor[\(index)]: id=\(vendor.id, privacy: .public), sellerNumber=\(vendor.sellerNumber, privacy: .public), status=\(vendor.status ?? "nil", privacy: .public)")
                    allSellers.insert(vendor.sellerNumber)
                }
                
                nextToken = response.nextPageToken
                logger.info("Next page token: \(nextToken ?? "nil", privacy: .public)")
            } while !(nextToken ?? "").isEmpty

            state.validSellers = allSellers
            logger.info("✅ Successfully loaded \(allSellers.count, privacy: .public) unique sellers across \(pageCount) pages")
        } catch let error as ApiError {
            logger.error("❌ Failed to load vendors: \(error.localizedDescription, privacy: .public)")
            
            // Log additional details for decode errors
            if case .decoding(let underlyingError, let data, let responseType) = error {
                logger.error("  Response type: \(responseType, privacy: .public)")
                logger.error("  Underlying error: \(underlyingError.localizedDescription, privacy: .public)")
                if let jsonString = String(data: data, encoding: .utf8) {
                    let preview = String(jsonString.prefix(1000))
                    logger.error("  JSON preview: \(preview, privacy: .public)")
                }
            }
            
            state.errorMessage = "Failed to load vendors: \(error.localizedDescription)"
        } catch {
            logger.error("❌ Unexpected error loading vendors: \(error.localizedDescription, privacy: .public)")
            state.errorMessage = "Failed to load vendors: \(error.localizedDescription)"
        }
    }

    // MARK: - Keypad

    private func handleKeypadPress(_ digit: String) {
        switch state.activeField {
        case .seller:
            state.sellerNumber += digit
        case .price:
            state.priceString += digit
        }
    }

    private func handleClear() {
        switch state.activeField {
        case .seller: state.sellerNumber = ""
        case .price: state.priceString = ""
        }
    }

    private func handleBackspace() {
        switch state.activeField {
        case .seller:
            if !state.sellerNumber.isEmpty { state.sellerNumber.removeLast() }
        case .price:
            if !state.priceString.isEmpty { state.priceString.removeLast() }
        }
    }

    private func handleSpace() {
        guard state.activeField == .price else { return }
        state.priceString += " "
    }

    private func handleOk() {
        switch state.activeField {
        case .seller:
            let sellerNum = Int(state.sellerNumber)
            guard let sellerNum, state.validSellers.contains(sellerNum) else {
                state.warningMessage = "Invalid seller number"
                return
            }
            state.activeField = .price

        case .price:
            addPrices()
        }
    }

    private func addPrices() {
        let sellerNum = Int(state.sellerNumber)
        guard let sellerNum, state.validSellers.contains(sellerNum) else {
            state.warningMessage = "Invalid seller number"
            return
        }

        let priceParts = state.priceString
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .split(whereSeparator: { $0.isWhitespace })
            .map(String.init)

        guard !priceParts.isEmpty else {
            state.warningMessage = "Enter at least one price"
            return
        }

        var newItems: [TransactionItem] = []
        for part in priceParts {
            guard let price = Int(part), price > 0 else {
                state.warningMessage = "Invalid price: \(part)"
                return
            }
            newItems.append(TransactionItem(sellerNumber: sellerNum, price: price))
        }

        state.transactions = newItems + state.transactions
        state.sellerNumber = ""
        state.priceString = ""
        state.activeField = .seller
        state.paidAmount = String(state.nextHundred)
    }

    private func removeItem(_ id: String) {
        state.transactions.removeAll { $0.id == id }
        state.paidAmount = String(state.nextHundred)
    }

    private func clearAllItems() {
        state.transactions = []
        state.paidAmount = "0"
    }

    private func setPaidAmount(_ amount: String) {
        state.paidAmount = amount.filter { $0.isNumber }
    }

    // MARK: - Checkout

    private func checkout(_ method: PaymentMethodType) {
        guard !state.transactions.isEmpty else {
            state.warningMessage = "No items to checkout"
            return
        }

        state.isProcessingPayment = true
        let purchaseId = Self.makePurchaseId()
        let paymentMethodStr: String = (method == .cash) ? "KONTANT" : "SWISH"

        let items = state.transactions.map { tx in
            SoldItemRequest(
                purchaseId: purchaseId,
                seller: tx.sellerNumber,
                price: tx.price,
                paymentMethod: paymentMethodStr
            )
        }

        Task {
            defer { state.isProcessingPayment = false }

            do {
                let response = try await apiClient.createSoldItems(
                    eventId: eventId,
                    apiKey: apiKey,
                    requestBody: CreateSoldItemsRequest(items: items)
                )

                let acceptedCount = response.acceptedItems?.count ?? 0
                if acceptedCount > 0 {
                    let completed = CompletedPurchase(
                        purchaseId: purchaseId,
                        items: state.transactions,
                        total: state.total,
                        paymentMethod: method
                    )
                    state.transactions = []
                    state.sellerNumber = ""
                    state.priceString = ""
                    state.activeField = .seller
                    state.paidAmount = "0"
                    state.lastPurchase = completed
                } else {
                    let reason = response.rejectedItems?.first?.reason ?? "Unknown error"
                    state.errorMessage = "Payment failed: \(reason)"
                }
            } catch {
                state.errorMessage = "Payment failed: \(error.localizedDescription)"
            }
        }
    }

    private static func makePurchaseId() -> String {
        let timestampMs = UInt64(Date().timeIntervalSince1970 * 1000)
        var bytes = [UInt8](repeating: 0, count: 16)
        bytes[0] = UInt8((timestampMs >> 40) & 0xFF)
        bytes[1] = UInt8((timestampMs >> 32) & 0xFF)
        bytes[2] = UInt8((timestampMs >> 24) & 0xFF)
        bytes[3] = UInt8((timestampMs >> 16) & 0xFF)
        bytes[4] = UInt8((timestampMs >> 8) & 0xFF)
        bytes[5] = UInt8(timestampMs & 0xFF)

        var entropy = [UInt8](repeating: 0, count: 10)
        let status = SecRandomCopyBytes(kSecRandomDefault, entropy.count, &entropy)
        if status == errSecSuccess {
            bytes.replaceSubrange(6..<16, with: entropy)
        } else {
            var uuid = UUID().uuid
            let uuidBytes = withUnsafeBytes(of: &uuid) { Array($0) }
            bytes.replaceSubrange(6..<16, with: uuidBytes.prefix(10))
        }

        return encodeCrockfordBase32(bytes)
    }

    private func makeHeartbeatRequest() -> CashierPresenceHeartbeatRequest {
        let snapshot = state.heartbeatSnapshot()
        return CashierPresenceHeartbeatRequest(
            clientState: snapshot.clientState,
            pendingPurchasesCount: snapshot.pendingPurchasesCount,
            clientType: .ios,
            displayName: snapshot.displayName
        )
    }

    private func makeHeartbeatRequestForHeartbeatLoop() -> CashierPresenceHeartbeatRequest {
        makeHeartbeatRequest()
    }

    private func applyHeartbeatResponse(_ response: CashierPresenceHeartbeatResponse) {
        state.heartbeatDisplayName = response.displayName
    }
}

private let crockfordBase32Alphabet = Array("0123456789ABCDEFGHJKMNPQRSTVWXYZ")

private func encodeCrockfordBase32(_ bytes: [UInt8]) -> String {
    var output = ""
    output.reserveCapacity(26)

    var buffer = 0
    var bitCount = 0

    for byte in bytes {
        buffer = (buffer << 8) | Int(byte)
        bitCount += 8

        while bitCount >= 5 {
            let index = (buffer >> (bitCount - 5)) & 0x1F
            output.append(crockfordBase32Alphabet[index])
            bitCount -= 5
        }
    }

    if bitCount > 0 {
        let index = (buffer << (5 - bitCount)) & 0x1F
        output.append(crockfordBase32Alphabet[index])
    }

    return output
}
