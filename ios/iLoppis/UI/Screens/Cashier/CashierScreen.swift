import SwiftUI

struct CashierScreen: View {
    let event: Event
    let apiKey: String
    let onBack: () -> Void

    @StateObject private var viewModel: CashierViewModel

    init(event: Event, apiKey: String, onBack: @escaping () -> Void) {
        self.event = event
        self.apiKey = apiKey
        self.onBack = onBack
        _viewModel = StateObject(
            wrappedValue: CashierViewModel(
                eventId: event.id,
                eventName: event.name,
                apiKey: apiKey
            )
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if viewModel.state.isLoading {
                        Text(LocalizedStringKey("cashier_loading"))
                            .foregroundColor(AppColors.textSecondary)
                    }

                    inputSection
                    itemsSection
                    totalsSection
                    paymentSection
                }
                .padding(16)
            }
        }
        .background(AppColors.background.ignoresSafeArea())
        .alert(
            LocalizedStringKey("app_title"),
            isPresented: Binding(
                get: { viewModel.state.warningMessage != nil },
                set: { if !$0 { viewModel.onAction(.dismissWarning) } }
            ),
            actions: {
                Button(LocalizedStringKey("button_cancel")) {
                    viewModel.onAction(.dismissWarning)
                }
            },
            message: {
                Text(viewModel.state.warningMessage ?? "")
            }
        )
        .alert(
            LocalizedStringKey("app_title"),
            isPresented: Binding(
                get: { viewModel.state.errorMessage != nil },
                set: { if !$0 { viewModel.onAction(.dismissError) } }
            ),
            actions: {
                Button(LocalizedStringKey("button_cancel")) {
                    viewModel.onAction(.dismissError)
                }
            },
            message: {
                Text(viewModel.state.errorMessage ?? "")
            }
        )
        .overlay {
            if viewModel.state.isProcessingPayment {
                ZStack {
                    Color.black.opacity(0.3).ignoresSafeArea()
                    ProgressView()
                        .tint(.white)
                        .scaleEffect(1.2)
                }
            }
        }
    }

    private var header: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.backward")
                    .foregroundColor(AppColors.textPrimary)
            }
            Text(LocalizedStringKey("cashier_title"))
                .font(.title2.weight(.bold))
                .foregroundColor(AppColors.textPrimary)
            Spacer()
        }
        .padding(16)
        .background(AppColors.background)
    }

    private var inputSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(format: NSLocalizedString("cashier_event_label", comment: ""), event.name))
                .font(.footnote)
                .foregroundColor(AppColors.textMuted)

            HStack(spacing: 12) {
                fieldButton(
                    titleKey: "cashier_seller_label",
                    value: viewModel.state.sellerNumber,
                    isActive: viewModel.state.activeField == .seller
                ) {
                    viewModel.onAction(.setActiveField(.seller))
                }
                fieldButton(
                    titleKey: "cashier_price_label",
                    value: viewModel.state.priceString,
                    isActive: viewModel.state.activeField == .price
                ) {
                    viewModel.onAction(.setActiveField(.price))
                }
            }

            keypad
        }
        .padding(16)
        .background(AppColors.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppColors.border, lineWidth: 1)
        )
    }

    private func fieldButton(titleKey: String, value: String, isActive: Bool, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 4) {
                Text(LocalizedStringKey(titleKey))
                    .font(.caption)
                    .foregroundColor(AppColors.textMuted)
                Text(value.isEmpty ? "—" : value)
                    .font(.headline)
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(isActive ? AppColors.badgeUpcomingBackground : AppColors.background)
            .cornerRadius(10)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isActive ? AppColors.buttonPrimary : AppColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var keypad: some View {
        VStack(spacing: 10) {
            ForEach([["1","2","3"],["4","5","6"],["7","8","9"],["C","0","OK"]], id: \.self) { row in
                HStack(spacing: 10) {
                    ForEach(row, id: \.self) { key in
                        keypadButton(title: key)
                    }
                }
            }
            HStack(spacing: 10) {
                keypadButton(title: "⌫")
                keypadButton(title: "␣")
                keypadButton(title: "+")
            }
        }
    }

    private func keypadButton(title: String) -> some View {
        Button {
            switch title {
            case "C":
                viewModel.onAction(.keypadClear)
            case "OK":
                viewModel.onAction(.keypadOk)
            case "⌫":
                viewModel.onAction(.keypadBackspace)
            case "␣":
                viewModel.onAction(.keypadSpace)
            case "+":
                viewModel.onAction(.keypadPress(" "))
            default:
                viewModel.onAction(.keypadPress(title))
            }
        } label: {
            Text(title)
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .foregroundColor(AppColors.textPrimary)
                .background(AppColors.background)
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(AppColors.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    private var itemsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(LocalizedStringKey("cashier_items_title"))
                    .font(.headline)
                    .foregroundColor(AppColors.textPrimary)
                Spacer()
                Button(action: { viewModel.onAction(.clearAllItems) }) {
                    Text(LocalizedStringKey("cashier_clear_all"))
                        .foregroundColor(AppColors.textSecondary)
                }
                .buttonStyle(.plain)
                .disabled(viewModel.state.transactions.isEmpty)
            }

            if viewModel.state.transactions.isEmpty {
                Text(LocalizedStringKey("cashier_no_items"))
                    .foregroundColor(AppColors.textSecondary)
            } else {
                VStack(spacing: 8) {
                    ForEach(viewModel.state.transactions) { item in
                        HStack {
                            Text("\(NSLocalizedString("cashier_seller_header", comment: "")) \(item.sellerNumber)")
                                .foregroundColor(AppColors.textPrimary)
                            Spacer()
                            Text("\(item.price)")
                                .foregroundColor(AppColors.textPrimary)
                            Button(action: { viewModel.onAction(.removeItem(item.id)) }) {
                                Text(LocalizedStringKey("cashier_remove_item"))
                                    .font(.caption)
                                    .foregroundColor(AppColors.textMuted)
                                    .padding(.leading, 8)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(12)
                        .background(AppColors.cardBackground)
                        .cornerRadius(10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(AppColors.border, lineWidth: 1)
                        )
                    }
                }
            }
        }
        .padding(16)
        .background(AppColors.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppColors.border, lineWidth: 1)
        )
    }

    private var totalsSection: some View {
        VStack(spacing: 10) {
            row(labelKey: "cashier_total", value: "\(viewModel.state.total)")
            HStack {
                Text(LocalizedStringKey("cashier_paid"))
                    .foregroundColor(AppColors.textSecondary)
                Spacer()
                TextField("0", text: Binding(
                    get: { viewModel.state.paidAmount },
                    set: { viewModel.onAction(.setPaidAmount($0)) }
                ))
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 100)
                .padding(8)
                .background(AppColors.background)
                .cornerRadius(8)
            }
            row(labelKey: "cashier_change", value: "\(viewModel.state.change)")
        }
        .padding(16)
        .background(AppColors.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppColors.border, lineWidth: 1)
        )
    }

    private func row(labelKey: String, value: String) -> some View {
        HStack {
            Text(LocalizedStringKey(labelKey))
                .foregroundColor(AppColors.textSecondary)
            Spacer()
            Text(value)
                .foregroundColor(AppColors.textPrimary)
        }
    }

    private var paymentSection: some View {
        HStack(spacing: 12) {
            Button {
                viewModel.onAction(.checkout(.cash))
            } label: {
                Text(LocalizedStringKey("cashier_button_cash"))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.white)
                    .background(AppColors.buttonPrimary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
            .disabled(viewModel.state.transactions.isEmpty)

            Button {
                viewModel.onAction(.checkout(.swish))
            } label: {
                Text(LocalizedStringKey("cashier_button_swish"))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.white)
                    .background(AppColors.buttonPrimary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
            .disabled(viewModel.state.transactions.isEmpty)
        }
        .padding(.bottom, 8)
    }
}
import SwiftUI
