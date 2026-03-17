import SwiftUI

private let liveStatsPollIntervalNs: UInt64 = 10_000_000_000

struct LiveStatsScreen: View {
    let event: Event
    let apiKey: String
    let onClose: () -> Void

    @StateObject private var viewModel: LiveStatsViewModel

    init(event: Event, apiKey: String, onClose: @escaping () -> Void) {
        self.event = event
        self.apiKey = apiKey
        self.onClose = onClose
        _viewModel = StateObject(
            wrappedValue: LiveStatsViewModel(event: event, apiKey: apiKey)
        )
    }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Group {
                if viewModel.state.isLoading && viewModel.state.snapshot == nil {
                    ProgressView()
                        .tint(AppColors.primary)
                        .scaleEffect(1.2)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let snapshot = viewModel.state.snapshot {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            summaryCard(snapshot: snapshot)

                            HStack(spacing: 12) {
                                statCard(
                                    titleKey: "live_stats_purchases_label",
                                    value: "\(snapshot.sales?.purchasesTotal ?? 0)"
                                )
                                statCard(
                                    titleKey: "live_stats_items_label",
                                    value: "\(snapshot.sales?.itemsTotal ?? 0)"
                                )
                            }

                            HStack(spacing: 12) {
                                statCard(
                                    titleKey: "live_stats_total_label",
                                    value: sekFormatter.string(from: NSNumber(value: snapshot.sales?.revenueTotalSek ?? 0)) ?? "0 kr"
                                )
                                statCard(
                                    titleKey: "live_stats_cashiers_label",
                                    value: "\(cashierCount(snapshot))"
                                )
                            }

                            Text(LocalizedStringKey("live_stats_cashier_list_title"))
                                .font(.headline)
                                .foregroundColor(AppColors.textPrimary)

                            if snapshot.cashierStatuses.isEmpty {
                                infoCard(text: NSLocalizedString("live_stats_empty_cashiers", comment: ""))
                            } else {
                                ForEach(snapshot.cashierStatuses) { cashier in
                                    cashierCard(cashier)
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 72)
                        .padding(.bottom, 24)
                    }
                } else {
                    VStack(spacing: 12) {
                        Text(LocalizedStringKey(viewModel.state.errorKey == "server" ? "live_stats_error_server" : "live_stats_error_network"))
                            .foregroundColor(AppColors.textPrimary)
                            .multilineTextAlignment(.center)

                        Button(action: viewModel.retry) {
                            Text(LocalizedStringKey("live_stats_retry"))
                                .frame(maxWidth: .infinity)
                                .padding()
                                .foregroundColor(.white)
                                .background(AppColors.buttonPrimary)
                                .cornerRadius(12)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .background(AppColors.background.ignoresSafeArea())

            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppColors.textSecondary)
                    .padding(12)
                    .background(AppColors.cardBackground.opacity(0.88))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .padding(.top, 14)
            .padding(.trailing, 16)
            .accessibilityLabel(LocalizedStringKey("live_stats_close"))
        }
    }

    private func summaryCard(snapshot: LiveStatsResponse) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(snapshot.eventName?.nilIfBlank ?? event.name)
                .font(.title2.weight(.bold))
                .foregroundColor(AppColors.textPrimary)

            if let city = (snapshot.eventCity?.nilIfBlank ?? event.addressCity?.nilIfBlank) {
                Text(city)
                    .font(.subheadline)
                    .foregroundColor(AppColors.textSecondary)
            }

            Text(String(format: NSLocalizedString("opening_hours_label", comment: ""), eventHoursLabel))
                .font(.subheadline)
                .foregroundColor(AppColors.textSecondary)

            connectionBadge(isStale: viewModel.state.errorKey != nil)

            if let generatedAt = snapshot.generatedAt,
               let timeText = timeLabel(from: generatedAt) {
                Text(String(format: NSLocalizedString("live_stats_updated_at", comment: ""), timeText))
                    .font(.footnote)
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(AppColors.cardBackground)
        .cornerRadius(20)
    }

    private func connectionBadge(isStale: Bool) -> some View {
        Text(LocalizedStringKey(isStale ? "live_stats_connection_polling" : "live_stats_connection_live"))
            .font(.footnote.weight(.semibold))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .foregroundColor(isStale ? AppColors.warning : AppColors.success)
            .background((isStale ? AppColors.warning : AppColors.success).opacity(0.14))
            .cornerRadius(999)
    }

    private func statCard(titleKey: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(value)
                .font(.title.weight(.bold))
                .foregroundColor(AppColors.textPrimary)
            Text(LocalizedStringKey(titleKey))
                .font(.subheadline)
                .foregroundColor(AppColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(AppColors.cardBackground)
        .cornerRadius(16)
    }

    private func infoCard(text: String) -> some View {
        Text(text)
            .foregroundColor(AppColors.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(AppColors.cardBackground)
            .cornerRadius(16)
    }

    private func cashierCard(_ cashier: LiveCashierStatus) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(cashier.displayName?.nilIfBlank ?? NSLocalizedString("live_stats_unknown_cashier", comment: ""))
                .font(.headline)
                .foregroundColor(AppColors.textPrimary)
                .lineLimit(1)

            Text([
                stateLabel(cashier.state),
                clientTypeLabel(cashier.clientType),
                String(format: NSLocalizedString("live_stats_pending_count", comment: ""), cashier.pendingPurchasesCount)
            ]
            .filter { !$0.isEmpty }
            .joined(separator: " • "))
            .font(.subheadline)
            .foregroundColor(AppColors.textSecondary)

            if let purchaseAt = cashier.lastPurchaseAt,
               let timeText = timeLabel(from: purchaseAt) {
                Text(String(format: NSLocalizedString("live_stats_updated_at", comment: ""), timeText))
                    .font(.footnote)
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(AppColors.cardBackground)
        .cornerRadius(16)
    }

    private var eventHoursLabel: String {
        let parts = [
            event.startTime.flatMap(dateLabel),
            event.startTime.flatMap(timeLabel),
            event.endTime.flatMap(timeLabel)
        ]

        if let date = parts[0], let start = parts[1], let end = parts[2] {
            return "\(date) \(start)-\(end)"
        }
        if let start = parts[1], let end = parts[2] {
            return "\(start)-\(end)"
        }
        return "-"
    }

    private func cashierCount(_ snapshot: LiveStatsResponse) -> Int {
        if !snapshot.cashierStatuses.isEmpty {
            return snapshot.cashierStatuses.count
        }
        return (snapshot.cashiers?.openCount ?? 0)
            + (snapshot.cashiers?.processingCount ?? 0)
            + (snapshot.cashiers?.stalledCount ?? 0)
    }

    private func stateLabel(_ raw: String?) -> String {
        switch raw {
        case "STATE_OPEN":
            return NSLocalizedString("live_stats_state_open", comment: "")
        case "STATE_PROCESSING":
            return NSLocalizedString("live_stats_state_processing", comment: "")
        case "STATE_STALLED":
            return NSLocalizedString("live_stats_state_stalled", comment: "")
        default:
            return NSLocalizedString("live_stats_state_offline", comment: "")
        }
    }

    private func clientTypeLabel(_ raw: String?) -> String {
        switch raw {
        case "CASHIER_CLIENT_TYPE_ANDROID":
            return NSLocalizedString("live_stats_client_type_android", comment: "")
        case "CASHIER_CLIENT_TYPE_IOS":
            return NSLocalizedString("live_stats_client_type_ios", comment: "")
        default:
            return NSLocalizedString("live_stats_client_type_unknown", comment: "")
        }
    }

    private func timeLabel(from raw: String) -> String? {
        guard let date = parseISODate(raw) else { return nil }
        return timeFormatter.string(from: date)
    }

    private func dateLabel(from raw: String) -> String? {
        guard let date = parseISODate(raw) else { return nil }
        return dateFormatter.string(from: date)
    }
}

@MainActor
private final class LiveStatsViewModel: ObservableObject {
    @Published private(set) var state = LiveStatsViewState()

    private let event: Event
    private let apiKey: String
    private let apiClient: ApiClient
    private var pollTask: Task<Void, Never>?

    init(event: Event, apiKey: String, apiClient: ApiClient = ApiClient()) {
        self.event = event
        self.apiKey = apiKey
        self.apiClient = apiClient
        startPolling()
    }

    deinit {
        pollTask?.cancel()
    }

    func retry() {
        Task { await fetch(forceLoading: state.snapshot == nil) }
    }

    private func startPolling() {
        pollTask?.cancel()
        pollTask = Task {
            while !Task.isCancelled {
                await fetch(forceLoading: state.snapshot == nil)
                try? await Task.sleep(nanoseconds: liveStatsPollIntervalNs)
            }
        }
    }

    private func fetch(forceLoading: Bool) async {
        if forceLoading {
            state.isLoading = true
            state.errorKey = nil
        }

        do {
            let snapshot = try await apiClient.getEventLiveStats(
                eventId: event.id,
                apiKey: apiKey
            )
            state.snapshot = snapshot
            state.isLoading = false
            state.errorKey = nil
        } catch let error as ApiError {
            DebugLogStore.shared.append("[LiveStats] request failed: \(error.localizedDescription)")
            state.isLoading = false
            switch error {
            case .http(let statusCode, _):
                state.errorKey = (500...599).contains(statusCode) ? "server" : "network"
            default:
                state.errorKey = "network"
            }
        } catch {
            DebugLogStore.shared.append("[LiveStats] request failed: \(error.localizedDescription)")
            state.isLoading = false
            state.errorKey = "network"
        }
    }
}

private struct LiveStatsViewState {
    var snapshot: LiveStatsResponse?
    var isLoading: Bool = true
    var errorKey: String?
}

private let isoFormatter: ISO8601DateFormatter = {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return formatter
}()

private let isoFormatterNoFractionalSeconds: ISO8601DateFormatter = {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime]
    return formatter
}()

private let timeFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "sv_SE")
    formatter.dateFormat = "HH:mm"
    return formatter
}()

private let dateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "sv_SE")
    formatter.dateFormat = "d MMM"
    return formatter
}()

private let sekFormatter: NumberFormatter = {
    let formatter = NumberFormatter()
    formatter.locale = Locale(identifier: "sv_SE")
    formatter.numberStyle = .currency
    formatter.currencyCode = "SEK"
    formatter.maximumFractionDigits = 0
    return formatter
}()

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

private func parseISODate(_ raw: String) -> Date? {
    isoFormatter.date(from: raw) ?? isoFormatterNoFractionalSeconds.date(from: raw)
}
