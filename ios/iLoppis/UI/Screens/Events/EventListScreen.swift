import SwiftUI

struct EventListScreen: View {
    @ObservedObject var viewModel: EventListViewModel
        @StateObject private var debugLogs = DebugLogStore.shared

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                header
                searchBar
                filterRow
                content
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button {
                            debugLogs.isPresented = true
                        } label: {
                            Image(systemName: "ladybug")
                        }
                        .accessibilityLabel("Debug Logs")
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .background(AppColors.background.ignoresSafeArea())
            .sheet(item: Binding(
                get: { viewModel.state.selectedEvent },
                set: { _ in viewModel.onAction(.dismissEventDetail) }
            )) { event in
                EventDetailDialog(
                    event: event,
                    onDismiss: { viewModel.onAction(.dismissEventDetail) },
                    onCashierClick: { viewModel.onAction(.startCodeEntry(mode: .cashier, event: event)) },
                    onScannerClick: { viewModel.onAction(.startCodeEntry(mode: .scanner, event: event)) }
                )
                .presentationDetents([.medium, .large])
            }
            .sheet(isPresented: Binding(
                get: { viewModel.state.codeEntryState != nil },
                set: { if !$0 { viewModel.onAction(.dismissCodeEntry) } }
            )) {
                if let entry = viewModel.state.codeEntryState {
                    CodeEntryDialog(
                        state: entry,
                        onCodeChange: { viewModel.onAction(.updateCode($0)) },
                        onSubmit: { viewModel.onAction(.submitCode($0)) },
                        onDismiss: { viewModel.onAction(.dismissCodeEntry) }
                    )
                    .presentationDetents([.medium])
                }
            }
            .sheet(isPresented: $debugLogs.isPresented) {
                DebugConsoleView(store: debugLogs)
            }
        }
    }

    // MARK: - Subviews

    private var header: some View {
        Text(LocalizedStringKey("app_title"))
            .font(.system(size: 28, weight: .bold))
            .foregroundColor(AppColors.textPrimary)
            .padding(.vertical, 8)
    }

    private var searchBar: some View {
        TextField(LocalizedStringKey("search_placeholder"), text: Binding(
            get: { viewModel.state.searchQuery },
            set: { viewModel.onAction(.updateSearch($0)) }
        ))
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(AppColors.cardBackground)
        .clipShape(Capsule())
    }

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                filterChip(titleKey: "filter_all", isSelected: viewModel.state.filter == .all) {
                    viewModel.onAction(.updateFilter(.all))
                }
                filterChip(titleKey: "filter_open", isSelected: viewModel.state.filter == .open) {
                    viewModel.onAction(.updateFilter(.open))
                }
                filterChip(titleKey: "filter_upcoming", isSelected: viewModel.state.filter == .upcoming) {
                    viewModel.onAction(.updateFilter(.upcoming))
                }
                filterChip(titleKey: "filter_past", isSelected: viewModel.state.filter == .past) {
                    viewModel.onAction(.updateFilter(.past))
                }
            }
            .padding(.vertical, 4)
        }
    }

    private func filterChip(titleKey: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(LocalizedStringKey(titleKey))
                .font(.callout)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .foregroundColor(isSelected ? .white : AppColors.textPrimary)
                .background(isSelected ? AppColors.buttonPrimary : AppColors.cardBackground)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(AppColors.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private var content: some View {
        Group {
            if viewModel.state.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            } else if let error = viewModel.state.errorMessage {
                Text(String(format: NSLocalizedString("error_prefix", comment: ""), error))
                    .foregroundColor(AppColors.textError)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            } else if viewModel.state.filteredEvents.isEmpty {
                Text(LocalizedStringKey("no_events_found"))
                    .foregroundColor(AppColors.textMuted)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.state.filteredEvents) { event in
                            EventCard(event: event) {
                                viewModel.onAction(.selectEvent(event))
                            }
                        }
                    }
                    .padding(.bottom, 16)
                }
            }
        }
    }
}

private struct DebugConsoleView: View {
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
