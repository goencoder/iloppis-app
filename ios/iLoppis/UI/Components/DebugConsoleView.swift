import SwiftUI

struct DebugConsoleView: View {
    @ObservedObject var store: DebugLogStore

    var body: some View {
        NavigationView {
            List {
                ForEach(Array(store.lines.enumerated()), id: \.offset) { _, line in
                    Text(line)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundColor(.primary)
                        .textSelection(.enabled)
                        .padding(.vertical, 2)
                }
            }
            .navigationTitle("Debug Logs")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { store.isPresented = false }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear") { store.clear() }
                }
            }
        }
    }
}
