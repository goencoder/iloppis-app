import SwiftUI

struct ScannerScreen: View {
    let event: Event
    let apiKey: String
    let onBack: () -> Void

    @StateObject private var viewModel: ScannerViewModel

    @State private var cameraState: CameraAuthorizationState = CameraPermission.currentState()
    @State private var isScanningActive: Bool = false
    @State private var manualCode: String = ""

    init(event: Event, apiKey: String, onBack: @escaping () -> Void) {
        self.event = event
        self.apiKey = apiKey
        self.onBack = onBack
        _viewModel = StateObject(
            wrappedValue: ScannerViewModel(
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
                    if viewModel.state.pendingCount > 0 {
                        Text(String(format: NSLocalizedString("scanner_offline_banner", comment: ""), viewModel.state.pendingCount))
                            .font(.footnote.weight(.semibold))
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .foregroundColor(AppColors.badgeUpcomingText)
                            .background(AppColors.badgeUpcomingBackground)
                            .cornerRadius(10)
                    }

                    cameraSection

                    HStack(spacing: 12) {
                        Button {
                            Task { await ensureCameraPermission() }
                        } label: {
                            Text(LocalizedStringKey("scanner_button_grant_permission"))
                                .frame(maxWidth: .infinity)
                                .padding()
                                .foregroundColor(.white)
                                .background(AppColors.buttonPrimary)
                                .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                        .opacity(cameraState == .notDetermined || cameraState == .denied ? 1 : 0)
                        .disabled(!(cameraState == .notDetermined || cameraState == .denied))

                        Button {
                            viewModel.onAction(.requestManualEntry)
                        } label: {
                            Text(LocalizedStringKey("scanner_button_manual_entry"))
                                .frame(maxWidth: .infinity)
                                .padding()
                                .foregroundColor(AppColors.textPrimary)
                                .background(AppColors.cardBackground)
                                .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                    }

                    historySection
                }
                .padding(16)
            }
        }
        .background(AppColors.background.ignoresSafeArea())
        .onAppear {
            cameraState = CameraPermission.currentState()
        }
        .sheet(isPresented: Binding(
            get: { viewModel.state.manualEntryVisible },
            set: { if !$0 { viewModel.onAction(.dismissManualEntry) } }
        )) {
            manualEntrySheet
                .presentationDetents([.medium])
        }
        .sheet(item: Binding(
            get: { viewModel.state.activeResult },
            set: { _ in viewModel.onAction(.dismissResult) }
        )) { result in
            scanResultSheet(result)
                .presentationDetents([.medium])
        }
        .overlay {
            if viewModel.state.isProcessing {
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
            Text(LocalizedStringKey("scanner_title"))
                .font(.title2.weight(.bold))
                .foregroundColor(AppColors.textPrimary)
            Spacer()
        }
        .padding(16)
    }

    private var cameraSection: some View {
        cameraPlaceholder
    }

    private var cameraPlaceholder: some View {
        RoundedRectangle(cornerRadius: 14)
            .fill(AppColors.cardBackground)
            .frame(height: 220)
            .overlay(
                VStack(spacing: 8) {
                    Image(systemName: "camera.viewfinder")
                        .font(.system(size: 42))
                        .foregroundColor(AppColors.textSecondary)
                    Text(LocalizedStringKey("scanner_preview_placeholder"))
                        .font(.footnote)
                        .multilineTextAlignment(.center)
                        .foregroundColor(AppColors.textMuted)
                    Text(LocalizedStringKey("scanner_manual_hint"))
                        .font(.footnote)
                        .foregroundColor(AppColors.textMuted)
                }
                .padding(16)
            )
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(LocalizedStringKey("scanner_history_title"))
                .font(.headline)
                .foregroundColor(AppColors.textPrimary)

            if viewModel.state.history.isEmpty {
                Text(LocalizedStringKey("scanner_history_empty"))
                    .font(.footnote)
                    .foregroundColor(AppColors.textMuted)
            } else {
                ForEach(viewModel.state.history) { item in
                    historyRow(item)
                }
            }
        }
        .padding(.top, 4)
    }

    private var manualEntrySheet: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(LocalizedStringKey("scanner_manual_title"))
                .font(.headline)
                .foregroundColor(AppColors.textPrimary)

            Text(LocalizedStringKey("scanner_manual_description"))
                .foregroundColor(AppColors.textSecondary)

            TextField(LocalizedStringKey("scanner_manual_placeholder"), text: $manualCode)
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)
                .padding(12)
                .background(AppColors.background)
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(AppColors.border, lineWidth: 1)
                )

            if let error = viewModel.state.manualEntryError {
                Text(manualErrorText(error))
                    .foregroundColor(AppColors.textError)
                    .font(.footnote)
            }

            HStack(spacing: 12) {
                Button {
                    viewModel.onAction(.dismissManualEntry)
                } label: {
                    Text(LocalizedStringKey("button_cancel"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(AppColors.textPrimary)
                        .background(AppColors.cardBackground)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)

                Button {
                    viewModel.onAction(.submitCode(manualCode))
                } label: {
                    Text(LocalizedStringKey("scanner_manual_confirm"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(.white)
                        .background(AppColors.buttonPrimary)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
            }

            Spacer()
        }
        .padding(16)
        .onAppear { manualCode = "" }
    }

    private func scanResultSheet(_ result: ScanResult) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(resultTitle(result.status))
                .font(.title3.weight(.bold))
                .foregroundColor(AppColors.textPrimary)

            Text(resultMessage(result.status))
                .foregroundColor(AppColors.textSecondary)

            if let ticket = result.ticket {
                VStack(alignment: .leading, spacing: 6) {
                    if let type = ticket.ticketType {
                        Text(String(format: NSLocalizedString("scanner_field_ticket_type", comment: ""), type))
                            .foregroundColor(AppColors.textMuted)
                    }
                    if let email = ticket.email {
                        Text(String(format: NSLocalizedString("scanner_field_email", comment: ""), email))
                            .foregroundColor(AppColors.textMuted)
                    }
                    if let scannedAt = ticket.scannedAt {
                        Text(String(format: NSLocalizedString("scanner_field_scanned_at", comment: ""), scannedAt))
                            .foregroundColor(AppColors.textMuted)
                    }
                }
                .padding(.top, 6)
            }

            Spacer()

            Button {
                viewModel.onAction(.dismissResult)
                // Resume scanning after dismiss.
                if cameraState == .authorized {
                    isScanningActive = true
                }
            } label: {
                Text(LocalizedStringKey("scanner_button_close_result"))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.white)
                    .background(AppColors.buttonPrimary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
    }

    private func resultTitle(_ status: ScanStatus) -> String {
        statusTitle(status)
    }

    private func resultMessage(_ status: ScanStatus) -> String {
        statusMessage(status)
    }

    private func ensureCameraPermission() async {
        cameraState = CameraPermission.currentState()
        if cameraState == .notDetermined {
            cameraState = await CameraPermission.request()
        }
        if cameraState == .authorized {
            isScanningActive = true
        }
    }

    private func manualErrorText(_ error: ManualEntryError) -> String {
        switch error {
        case .emptyInput:
            return NSLocalizedString("scanner_manual_error_empty", comment: "")
        case .wrongEvent:
            return String(format: NSLocalizedString("scanner_manual_error_event", comment: ""), event.name)
        case .invalidFormat:
            return NSLocalizedString("scanner_manual_error_format", comment: "")
        }
    }
    

    private func historyRow(_ item: ScanResult) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Circle()
                .fill(statusColor(item.status))
                .frame(width: 10, height: 10)
                .padding(.top, 6)

            VStack(alignment: .leading, spacing: 2) {
                Text(statusTitle(item.status))
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(AppColors.textPrimary)
                if let ticket = item.ticket {
                    if let email = ticket.email, !email.isEmpty {
                        Text(String(format: NSLocalizedString("scanner_field_email", comment: ""), email))
                            .font(.footnote)
                            .foregroundColor(AppColors.textSecondary)
                    }
                    if let type = ticket.ticketType, !type.isEmpty {
                        Text(String(format: NSLocalizedString("scanner_field_ticket_type", comment: ""), type))
                            .font(.footnote)
                            .foregroundColor(AppColors.textSecondary)
                    }
                }
                if let message = item.message, !message.isEmpty {
                    Text(message)
                        .font(.footnote)
                        .foregroundColor(AppColors.textMuted)
                }
            }
            Spacer()
        }
        .padding(12)
        .background(AppColors.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppColors.border, lineWidth: 1)
        )
    }

    private func resultSheet(_ result: ScanResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(statusTitle(result.status))
                .font(.title3.weight(.bold))
                .foregroundColor(AppColors.textPrimary)

            Text(statusMessage(result.status))
                .foregroundColor(AppColors.textSecondary)

            if let ticket = result.ticket {
                VStack(alignment: .leading, spacing: 6) {
                    if let type = ticket.ticketType, !type.isEmpty {
                        Text(String(format: NSLocalizedString("scanner_field_ticket_type", comment: ""), type))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    if let email = ticket.email, !email.isEmpty {
                        Text(String(format: NSLocalizedString("scanner_field_email", comment: ""), email))
                            .foregroundColor(AppColors.textSecondary)
                    }
                    if let scannedAt = ticket.scannedAt, !scannedAt.isEmpty {
                        Text(String(format: NSLocalizedString("scanner_field_scanned_at", comment: ""), scannedAt))
                            .foregroundColor(AppColors.textSecondary)
                    }
                }
                .padding(12)
                .background(AppColors.cardBackground)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(AppColors.border, lineWidth: 1)
                )
            }

            Button {
                viewModel.onAction(.dismissResult)
            } label: {
                Text(LocalizedStringKey("scanner_button_close_result"))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(.white)
                    .background(AppColors.buttonPrimary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)

            Spacer()
        }
        .padding(16)
        .background(AppColors.background.ignoresSafeArea())
    }

    private func statusTitle(_ status: ScanStatus) -> String {
        switch status {
        case .success:
            return NSLocalizedString("scanner_result_title_success", comment: "")
        case .duplicate:
            return NSLocalizedString("scanner_result_title_duplicate", comment: "")
        case .invalid:
            return NSLocalizedString("scanner_result_title_invalid", comment: "")
        case .offline:
            return NSLocalizedString("scanner_result_title_offline", comment: "")
        case .error:
            return NSLocalizedString("scanner_result_title_error", comment: "")
        }
    }

    private func statusMessage(_ status: ScanStatus) -> String {
        switch status {
        case .success:
            return NSLocalizedString("scanner_result_message_success", comment: "")
        case .duplicate:
            return NSLocalizedString("scanner_result_message_duplicate", comment: "")
        case .invalid:
            return NSLocalizedString("scanner_result_message_invalid", comment: "")
        case .offline:
            return NSLocalizedString("scanner_result_message_offline", comment: "")
        case .error:
            return NSLocalizedString("scanner_result_message_error", comment: "")
        }
    }

    private func statusColor(_ status: ScanStatus) -> Color {
        switch status {
        case .success:
            return AppColors.success
        case .duplicate:
            return AppColors.info
        case .invalid:
            return AppColors.error
        case .offline:
            return AppColors.badgeUpcomingText
        case .error:
            return AppColors.error
        }
    }
}
