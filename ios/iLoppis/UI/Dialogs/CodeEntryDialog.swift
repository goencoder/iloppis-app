import SwiftUI

struct CodeEntryDialog: View {
    let state: CodeEntryState
    let onCodeChange: (String) -> Void
    let onSubmit: (String) -> Void
    let onDismiss: () -> Void

    @FocusState private var isInputFocused: Bool

    private var titleKey: String {
        switch state.mode {
        case .tool: return "code_entry_title_tool"
        case .cashier: return "code_entry_title_cashier"
        case .scanner: return "code_entry_title_scanner"
        case .liveStats: return "code_entry_title_live_stats"
        }
    }

    private var subtitleKey: String {
        switch state.mode {
        case .tool: return "code_entry_subtitle_tool"
        case .cashier: return "code_entry_subtitle_cashier"
        case .scanner: return "code_entry_subtitle_scanner"
        case .liveStats: return "code_entry_subtitle_live_stats"
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

                codeInput

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
                    .background(
                        state.code.count == 6 && !state.isValidating
                            ? AppColors.buttonPrimary
                            : AppColors.buttonPrimaryDisabled
                    )
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)
                .disabled(state.code.count != 6 || state.isValidating)

                Spacer()
            }
            .padding(20)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("button_cancel"), action: onDismiss)
                }
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    isInputFocused = true
                }
            }
        }
    }

    private var codeInput: some View {
        VStack(alignment: .leading, spacing: 8) {
            codeBoxes

            TextField(
                "",
                text: Binding(
                    get: { state.code },
                    set: { onCodeChange($0) }
                )
            )
            .textInputAutocapitalization(.characters)
            .autocorrectionDisabled()
            .keyboardType(.asciiCapable)
            .submitLabel(.done)
            .focused($isInputFocused)
            .onSubmit {
                if state.code.count == 6 {
                    onSubmit(state.code)
                }
            }
            .frame(width: 1, height: 1)
            .opacity(0.01)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            isInputFocused = true
        }
    }

    private var codeBoxes: some View {
        HStack(spacing: 8) {
            ForEach(0..<6, id: \.self) { index in
                if index == 3 {
                    Text("-")
                        .font(.headline)
                        .foregroundColor(AppColors.textSecondary)
                }

                CodeSlotView(
                    character: state.code.character(at: index).map(String.init) ?? "",
                    isFocused: state.code.count < 6 ? index == state.code.count : index == 5,
                    hasError: state.errorMessage != nil
                )
            }
        }
    }
}

private struct CodeSlotView: View {
    let character: String
    let isFocused: Bool
    let hasError: Bool

    var body: some View {
        Text(character == " " ? "" : character)
            .font(.headline.weight(.bold))
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(AppColors.inputBackground)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(borderColor, lineWidth: isFocused || hasError ? 2 : 1)
            )
            .cornerRadius(10)
    }

    private var borderColor: Color {
        if hasError { return AppColors.textError }
        if isFocused { return AppColors.inputBorderFocused }
        return AppColors.border
    }
}

private extension String {
    func character(at index: Int) -> Character? {
        guard index >= 0 && index < count else { return nil }
        return self[self.index(startIndex, offsetBy: index)]
    }
}
