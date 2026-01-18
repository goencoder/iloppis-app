import SwiftUI

struct CodeEntryDialog: View {
    let state: CodeEntryState
    let onCodeChange: (String) -> Void
    let onSubmit: (String) -> Void
    let onDismiss: () -> Void

    private var titleKey: String {
        switch state.mode {
        case .cashier: return "code_entry_title_cashier"
        case .scanner: return "code_entry_title_scanner"
        }
    }

    private var subtitleKey: String {
        switch state.mode {
        case .cashier: return "code_entry_subtitle_cashier"
        case .scanner: return "code_entry_subtitle_scanner"
        }
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text(LocalizedStringKey(titleKey))
                    .font(.title3.weight(.bold))
                    .foregroundColor(AppColors.textPrimary)

                Text(LocalizedStringKey(subtitleKey))
                    .font(.subheadline)
                    .foregroundColor(AppColors.textSecondary)

                TextField(LocalizedStringKey("code_entry_placeholder"), text: Binding(
                    get: { state.code },
                    set: { onCodeChange($0.uppercased()) }
                ))
                .textInputAutocapitalization(.characters)
                .keyboardType(.asciiCapable)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(AppColors.inputBackground)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(state.errorMessage == nil ? AppColors.inputBorder : AppColors.textError, lineWidth: 1)
                )
                .cornerRadius(10)

                if let error = state.errorMessage {
                    Text(error)
                        .font(.footnote)
                        .foregroundColor(AppColors.textError)
                }

                Button(action: { onSubmit(state.code) }) {
                    HStack {
                        if state.isValidating { ProgressView() }
                        Text(LocalizedStringKey("button_verify_continue"))
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.white)
                    .background(state.code.isEmpty || state.isValidating ? AppColors.buttonPrimaryDisabled : AppColors.buttonPrimary)
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
                .disabled(state.code.isEmpty || state.isValidating)

                Spacer()
            }
            .padding(20)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("button_cancel"), action: onDismiss)
                }
            }
        }
    }
}
