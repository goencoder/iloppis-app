import Foundation
import OSLog

private let logger = Logger(subsystem: "se.iloppis.app", category: "ApiClient")

@MainActor
final class DebugLogStore: ObservableObject {
    static let shared = DebugLogStore()

    @Published private(set) var lines: [String] = []
    @Published var isPresented: Bool = false

    private init() {}

    func append(_ message: String) {
        let timestamp = DebugLogStore.timestamp()
        lines.append("[\(timestamp)] \(message)")

        if lines.count > 500 {
            lines.removeFirst(lines.count - 500)
        }
    }

    func clear() {
        lines.removeAll()
    }

    private static func timestamp() -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm:ss.SSS"
        return formatter.string(from: Date())
    }
}

struct ApiClient {
    let baseURL = URL(string: "https://iloppis-staging.fly.dev/")!

    private let enableDebugLogging: Bool

    private let session: URLSession
    private let jsonDecoder: JSONDecoder
    private let jsonEncoder: JSONEncoder

    init(session: URLSession = .shared, enableDebugLogging: Bool = true) {
        self.session = session
        self.enableDebugLogging = enableDebugLogging
        self.jsonDecoder = JSONDecoder()
        self.jsonEncoder = JSONEncoder()
    }

    func listEvents() async throws -> [EventDto] {
        let response: EventListResponse = try await request(
            path: "v1/events",
            method: .get,
            authorization: nil,
            body: Optional<EmptyBody>.none
        )
        return response.events
    }

    func filterEvents(filter: EventFilter) async throws -> [EventDto] {
        let requestBody = EventFilterRequest(filter: filter)
        let response: EventListResponse = try await request(
            path: "v1/events:filter",
            method: .post,
            authorization: nil,
            body: requestBody
        )
        return response.events
    }

    func getApiKeyByAlias(eventId: String, alias: String) async throws -> ApiKeyResponse {
        let encodedAlias = ApiClient.encodePathSegment(alias)
        let response: ApiKeyResponse = try await request(
            path: "v1/events/\(eventId)/api-keys/alias/\(encodedAlias)",
            method: .get,
            authorization: nil,
            body: Optional<EmptyBody>.none
        )
        return response
    }

    func listVendors(
        eventId: String,
        apiKey: String,
        pageSize: Int = 100,
        nextPageToken: String? = nil
    ) async throws -> ListVendorsResponse {
        var query: [URLQueryItem] = [URLQueryItem(name: "pageSize", value: String(pageSize))]
        if let nextPageToken, !nextPageToken.isEmpty {
            query.append(URLQueryItem(name: "nextPageToken", value: nextPageToken))
        }

        return try await request(
            path: "v1/events/\(eventId)/vendors",
            method: .get,
            authorization: "Bearer \(apiKey)",
            queryItems: query,
            body: Optional<EmptyBody>.none
        )
    }

    func createSoldItems(
        eventId: String,
        apiKey: String,
        requestBody: CreateSoldItemsRequest
    ) async throws -> CreateSoldItemsResponse {
        try await request(
            path: "v1/events/\(eventId)/sold-items",
            method: .post,
            authorization: "Bearer \(apiKey)",
            body: requestBody
        )
    }

    func scanVisitorTicket(
        eventId: String,
        apiKey: String,
        ticketId: String
    ) async throws -> ScanVisitorTicketResponse {
        try await request(
            path: "v1/events/\(eventId)/visitor_tickets/\(ticketId)/scan",
            method: .post,
            authorization: "Bearer \(apiKey)",
            body: [String: String]()
        )
    }

    func getVisitorTicket(
        eventId: String,
        apiKey: String,
        ticketId: String
    ) async throws -> GetVisitorTicketResponse {
        try await request(
            path: "v1/events/\(eventId)/visitor_tickets/\(ticketId)",
            method: .get,
            authorization: "Bearer \(apiKey)",
            body: Optional<EmptyBody>.none
        )
    }

    // MARK: - Core request

    private func request<Response: Decodable, Body: Encodable>(
        path: String,
        method: HttpMethod,
        authorization: String?,
        queryItems: [URLQueryItem] = [],
        body: Body?
    ) async throws -> Response {
        let requestStart = Date()
        
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)
        if !queryItems.isEmpty {
            components?.queryItems = queryItems
        }
        guard let url = components?.url else {
            throw ApiError.invalidUrl
        }

        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let authorization {
            request.setValue(authorization, forHTTPHeaderField: "Authorization")
        }

        if method.allowsBody {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            if let body {
                request.httpBody = try jsonEncoder.encode(body)
            }
        }

        if enableDebugLogging {
            debugLogRequest(request)
        }

        let (data, response) = try await session.data(for: request)
        let requestDuration = Date().timeIntervalSince(requestStart)
        
        guard let http = response as? HTTPURLResponse else {
            throw ApiError.invalidResponse
        }

        if enableDebugLogging {
            debugLogResponse(request: request, response: http, data: data, duration: requestDuration)
        }

        if (200...299).contains(http.statusCode) {
            do {
                return try jsonDecoder.decode(Response.self, from: data)
            } catch {
                // Enhanced decode error logging
                logDecodeError(error: error, data: data, url: url, responseType: String(describing: Response.self))
                throw ApiError.decoding(error, data: data, responseType: String(describing: Response.self))
            }
        }

        let message = ApiClient.extractErrorMessage(from: data)
        throw ApiError.http(statusCode: http.statusCode, message: message)
    }

    private static func extractErrorMessage(from data: Data) -> String? {
        guard !data.isEmpty else { return nil }
        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            if let message = json["message"] as? String, !message.isEmpty { return message }
        }
        return String(data: data, encoding: .utf8)
    }

    private static func encodePathSegment(_ value: String) -> String {
        // Encode anything that isn't URL-path safe. Keep '-' unescaped.
        var allowed = CharacterSet.urlPathAllowed
        allowed.insert(charactersIn: "-")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }

    private func debugLogRequest(_ request: URLRequest) {
        let method = request.httpMethod ?? "?"
        let url = request.url?.absoluteString ?? "(no url)"
        let hasAuth = (request.value(forHTTPHeaderField: "Authorization") != nil)

        let line = "[ApiClient] → \(method) \(url) auth=\(hasAuth ? "yes" : "no")"
        logger.info("→ \(method, privacy: .public) \(url, privacy: .public) auth=\(hasAuth ? "yes" : "no", privacy: .public)")
        print(line)
        Task { @MainActor in DebugLogStore.shared.append(line) }
        
        // Log all request headers (excluding sensitive auth values)
        if let headers = request.allHTTPHeaderFields {
            var sanitizedHeaders = headers
            if sanitizedHeaders["Authorization"] != nil {
                sanitizedHeaders["Authorization"] = "[REDACTED]"
            }
            let headersLine = "[ApiClient]   headers: \(sanitizedHeaders)"
            logger.info("  headers: \(String(describing: sanitizedHeaders), privacy: .public)")
            print(headersLine)
            Task { @MainActor in DebugLogStore.shared.append(headersLine) }
        }

        if let body = request.httpBody, !body.isEmpty,
           let bodyString = String(data: body, encoding: .utf8) {
            let bodyLine = "[ApiClient]   body: \(bodyString)"
            logger.info("  body: \(bodyString, privacy: .public)")
            print(bodyLine)
            Task { @MainActor in DebugLogStore.shared.append(bodyLine) }
        }
    }

    private func debugLogResponse(request: URLRequest, response: HTTPURLResponse, data: Data, duration: TimeInterval) {
        let method = request.httpMethod ?? "?"
        let url = request.url?.absoluteString ?? "(no url)"
        let durationMs = Int(duration * 1000)

        let line = "[ApiClient] ← \(response.statusCode) \(method) \(url) (\(durationMs)ms)"
        logger.info("← \(response.statusCode, privacy: .public) \(method, privacy: .public) \(url, privacy: .public) (\(durationMs)ms)")
        print(line)
        Task { @MainActor in DebugLogStore.shared.append(line) }
        
        // Log response headers
        let headers = response.allHeaderFields
        let headersLine = "[ApiClient]   response headers: \(headers)"
        logger.info("  response headers: \(String(describing: headers), privacy: .public)")
        print(headersLine)
        Task { @MainActor in DebugLogStore.shared.append(headersLine) }

        // Log response size
        let sizeLine = "[ApiClient]   response size: \(data.count) bytes"
        logger.info("  response size: \(data.count) bytes")
        print(sizeLine)
        Task { @MainActor in DebugLogStore.shared.append(sizeLine) }

        guard !data.isEmpty else { return }
        if let string = String(data: data, encoding: .utf8), !string.isEmpty {
            let trimmed = string.count > 2000 ? String(string.prefix(2000)) + "…" : string
            let responseLine = "[ApiClient]   response body: \(trimmed)"
            logger.info("  response body: \(trimmed, privacy: .public)")
            print(responseLine)
            Task { @MainActor in DebugLogStore.shared.append(responseLine) }
        }
    }
    
    private func logDecodeError(error: Error, data: Data, url: URL, responseType: String) {
        let errorLine = "[ApiClient] ❌ DECODE ERROR for \(responseType)"
        logger.error("❌ DECODE ERROR for \(responseType, privacy: .public)")
        print(errorLine)
        Task { @MainActor in DebugLogStore.shared.append(errorLine) }
        
        let urlLine = "[ApiClient]   URL: \(url.absoluteString)"
        logger.error("  URL: \(url.absoluteString, privacy: .public)")
        print(urlLine)
        Task { @MainActor in DebugLogStore.shared.append(urlLine) }
        
        // Log the underlying error details
        if let decodingError = error as? DecodingError {
            let details = decodeErrorDetails(decodingError)
            let detailsLine = "[ApiClient]   Error: \(details)"
            logger.error("  Error: \(details, privacy: .public)")
            print(detailsLine)
            Task { @MainActor in DebugLogStore.shared.append(detailsLine) }
        } else {
            let detailsLine = "[ApiClient]   Error: \(error.localizedDescription)"
            logger.error("  Error: \(error.localizedDescription, privacy: .public)")
            print(detailsLine)
            Task { @MainActor in DebugLogStore.shared.append(detailsLine) }
        }
        
        // Log raw JSON
        if let jsonString = String(data: data, encoding: .utf8) {
            let trimmed = jsonString.count > 3000 ? String(jsonString.prefix(3000)) + "…" : jsonString
            let jsonLine = "[ApiClient]   Raw JSON: \(trimmed)"
            logger.error("  Raw JSON: \(trimmed, privacy: .public)")
            print(jsonLine)
            Task { @MainActor in DebugLogStore.shared.append(jsonLine) }
        }
    }
    
    private func decodeErrorDetails(_ error: DecodingError) -> String {
        switch error {
        case .typeMismatch(let type, let context):
            return "Type mismatch: Expected \(type), at path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> ")). \(context.debugDescription)"
        case .valueNotFound(let type, let context):
            return "Value not found: Missing \(type) at path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> ")). \(context.debugDescription)"
        case .keyNotFound(let key, let context):
            return "Key not found: Missing key '\(key.stringValue)' at path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> ")). \(context.debugDescription)"
        case .dataCorrupted(let context):
            return "Data corrupted at path: \(context.codingPath.map { $0.stringValue }.joined(separator: " -> ")). \(context.debugDescription)"
        @unknown default:
            return error.localizedDescription
        }
    }
}

enum HttpMethod: String {
    case get = "GET"
    case post = "POST"

    var allowsBody: Bool {
        switch self {
        case .post: return true
        case .get: return false
        }
    }
}

enum ApiError: Error, LocalizedError {
    case invalidUrl
    case invalidResponse
    case decoding(Error, data: Data, responseType: String)
    case http(statusCode: Int, message: String?)

    var errorDescription: String? {
        switch self {
        case .invalidUrl:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response"
        case .decoding(let error, let data, let responseType):
            var description = "Failed to decode \(responseType): \(error.localizedDescription)"
            if let decodingError = error as? DecodingError {
                description += "\n\nDetails: \(Self.formatDecodingError(decodingError))"
            }
            if let jsonString = String(data: data, encoding: .utf8) {
                let preview = jsonString.prefix(500)
                description += "\n\nJSON preview: \(preview)\(jsonString.count > 500 ? "..." : "")"
            }
            return description
        case .http(let statusCode, let message):
            if let message, !message.isEmpty {
                return "HTTP \(statusCode): \(message)"
            }
            return "HTTP \(statusCode)"
        }
    }
    
    private static func formatDecodingError(_ error: DecodingError) -> String {
        switch error {
        case .typeMismatch(let type, let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " -> ")
            return "Type mismatch for \(type) at '\(path)': \(context.debugDescription)"
        case .valueNotFound(let type, let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " -> ")
            return "Missing required value of type \(type) at '\(path)': \(context.debugDescription)"
        case .keyNotFound(let key, let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " -> ")
            return "Missing required key '\(key.stringValue)' at '\(path)': \(context.debugDescription)"
        case .dataCorrupted(let context):
            let path = context.codingPath.map { $0.stringValue }.joined(separator: " -> ")
            return "Data corrupted at '\(path)': \(context.debugDescription)"
        @unknown default:
            return error.localizedDescription
        }
    }
}

private struct EmptyBody: Encodable {}

// MARK: - DTOs (match Android ApiClient.kt)

struct EventDto: Codable {
    let id: String
    let name: String
    let description: String?
    let startTime: String?
    let endTime: String?
    let addressStreet: String?
    let addressCity: String?
    let lifecycleState: String?
}

struct EventListResponse: Codable {
    let events: [EventDto]
}

struct EventFilter: Codable {
    let city: String?
    let dateFrom: String?
    let dateTo: String?
    let searchText: String?
    let lifecycleStates: [String]?

    enum CodingKeys: String, CodingKey {
        case city
        case dateFrom = "date_from"
        case dateTo = "date_to"
        case searchText = "search_text"
        case lifecycleStates = "lifecycle_states"
    }
}

struct EventFilterRequest: Codable {
    let filter: EventFilter
    let pagination: [String: String]

    init(filter: EventFilter, pagination: [String: String] = [:]) {
        self.filter = filter
        self.pagination = pagination
    }
}

struct ApiKeyResponse: Codable {
    let alias: String
    let apiKey: String
    let isActive: Bool
    let type: String?
    let tags: [String]?
    let id: String?
}

struct VendorDto: Codable {
    let id: String
    let sellerNumber: Int
    let firstName: String?
    let lastName: String?
    let email: String?
    let phone: String?
    let status: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "vendorId"  // Backend returns "vendorId", we map it to "id"
        case sellerNumber
        case firstName
        case lastName
        case email
        case phone
        case status
    }
}

struct ListVendorsResponse: Codable {
    let vendors: [VendorDto]
    let nextPageToken: String?
}

struct SoldItemRequest: Codable {
    let purchaseId: String
    let seller: Int
    let price: Int
    let paymentMethod: String
}

struct CreateSoldItemsRequest: Codable {
    let items: [SoldItemRequest]
}

struct SoldItemDto: Codable {
    let itemId: String?
    let eventId: String?
    let cashierAlias: String?
    let purchaseId: String
    let seller: Int
    let price: Int
    let paymentMethod: String?
    let soldTime: String?
    let collectedBySeller: Bool?
    let collectedTime: String?
    let isArchived: Bool?
}

struct RejectedItem: Codable {
    let item: SoldItemDto
    let reason: String
}

struct CreateSoldItemsResponse: Codable {
    let acceptedItems: [SoldItemDto]?
    let rejectedItems: [RejectedItem]?
}

struct VisitorTicketDto: Codable {
    let id: String
    let eventId: String
    let ticketType: String?
    let email: String?
    let status: String?
    let issuedAt: String?
    let validFrom: String?
    let validUntil: String?
    let scannedAt: String?
}

struct ScanVisitorTicketResponse: Codable {
    let ticket: VisitorTicketDto?
}

struct GetVisitorTicketResponse: Codable {
    let ticket: VisitorTicketDto?
}
