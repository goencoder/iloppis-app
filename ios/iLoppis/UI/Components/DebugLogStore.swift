import Foundation

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
