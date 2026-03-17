import SwiftUI

struct CodeConfirmDialog: View {
    let state: CodeConfirmationState
    let onConfirm: () -> Void
    let onBack: () -> Void

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text(LocalizedStringKey("confirm_event_title"))
                    .font(.title3.weight(.bold))
                    .foregroundColor(AppColors.textPrimary)

                Text(LocalizedStringKey("confirm_event_message"))
                    .font(.subheadline)
                    .foregroundColor(AppColors.textSecondary)

                VStack(alignment: .leading, spacing: 10) {
                    Text(state.event.name)
                        .font(.headline)
                        .foregroundColor(AppColors.textPrimary)

                    Text(String(format: NSLocalizedString("confirm_event_tool_label", comment: ""), toolLabel))
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(AppColors.textPrimary)

                    Text(locationText)
                        .font(.subheadline)
                        .foregroundColor(AppColors.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(AppColors.cardBackground)
                .cornerRadius(12)

                Button(action: onConfirm) {
                    Text(openButtonTitle)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(.white)
                        .background(AppColors.buttonPrimary)
                        .cornerRadius(12)
                }
                .buttonStyle(.plain)

                Button(action: onBack) {
                    Text(LocalizedStringKey("button_back"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(AppColors.textPrimary)
                        .background(AppColors.cardBackground)
                        .cornerRadius(12)
                }
                .buttonStyle(.plain)

                Spacer()
            }
            .padding(20)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("button_cancel"), action: onBack)
                }
            }
        }
    }

    private var toolLabel: String {
        switch state.resolvedMode {
        case .tool:
            return NSLocalizedString("button_tools", comment: "")
        case .cashier:
            return NSLocalizedString("tool_type_cashier", comment: "")
        case .scanner:
            return NSLocalizedString("tool_type_scanner", comment: "")
        case .liveStats:
            return NSLocalizedString("tool_type_live_stats", comment: "")
        }
    }

    private var openButtonTitle: LocalizedStringKey {
        switch state.resolvedMode {
        case .tool:
            return LocalizedStringKey("button_tools")
        case .cashier:
            return LocalizedStringKey("open_cashier_button")
        case .scanner:
            return LocalizedStringKey("open_scanner_button")
        case .liveStats:
            return LocalizedStringKey("open_live_stats_button")
        }
    }

    private var locationText: String {
        let parts = [state.event.addressStreet, state.event.addressCity]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        if parts.isEmpty {
            return NSLocalizedString("location_not_specified", comment: "")
        }
        return parts.joined(separator: ", ")
    }
}
