import SwiftUI

@main
struct iLoppisApp: App {
    @StateObject private var viewModel = EventListViewModel()

    var body: some Scene {
        WindowGroup {
            RootView(viewModel: viewModel)
        }
    }
}

private struct RootView: View {
    @ObservedObject var viewModel: EventListViewModel

    var body: some View {
        switch viewModel.state.currentScreen {
        case .eventList:
            EventListScreen(viewModel: viewModel)
                .background(AppColors.background.ignoresSafeArea())
        case let .cashier(event, apiKey):
                CashierScreen(event: event, apiKey: apiKey) {
                viewModel.onAction(.navigateBack)
            }
        case let .scanner(event, apiKey):
                ScannerScreen(event: event, apiKey: apiKey) {
                viewModel.onAction(.navigateBack)
            }
        }
    }
}
