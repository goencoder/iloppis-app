import SwiftUI

struct ScannerScreen: View {
    let event: Event
    let apiKey: String
    let onBack: () -> Void

    @StateObject private var viewModel: ScannerViewModel

    @State private var cameraState: CameraAuthorizationState = CameraPermission.currentState()
    @State private var isScanningActive: Bool = false

    private static let ticketDetailDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.setLocalizedDateFormatFromTemplate("d MMM HH:mm")
        return formatter
    }()

    private static let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

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
                                .foregroundColor(AppColors.dialogBackground)
                                .background(AppColors.buttonPrimary)
                                .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                        .opacity(cameraState == .notDetermined || cameraState == .denied ? 1 : 0)
                        .disabled(!(cameraState == .notDetermined || cameraState == .denied))

                        Button {
                            viewModel.onAction(.requestTicketSearch)
                        } label: {
                            Text(LocalizedStringKey("scanner_button_search"))
                                .frame(maxWidth: .infinity)
                                .padding()
                                .foregroundColor(AppColors.dialogBackground)
                                .background(AppColors.buttonPrimary)
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
        .sheet(item: Binding(
            get: { viewModel.state.activeResult },
            set: { _ in viewModel.onAction(.dismissResult) }
        )) { result in
            scanResultSheet(result)
                .presentationDetents([.medium])
        }
        .sheet(isPresented: Binding(
            get: { viewModel.state.ticketSearchVisible && viewModel.state.searchDetailTicket == nil },
            set: { if !$0 { viewModel.onAction(.dismissTicketSearch) } }
        )) {
            ticketSearchSheet
                .presentationDetents([.large])
        }
        .sheet(item: Binding(
            get: { viewModel.state.ticketSearchVisible ? nil : viewModel.state.searchDetailTicket },
            set: { _ in viewModel.onAction(.dismissSearchDetail) }
        )) { ticket in
            ticketDetailSheet(ticket)
                .presentationDetents([.medium, .large])
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
        ZStack {
            cameraPlaceholder

            if viewModel.state.isProcessing {
                RoundedRectangle(cornerRadius: 14)
                    .fill(AppColors.navigatorOverlay.opacity(0.18))

                ProgressView()
                    .tint(AppColors.dialogBackground)
                    .scaleEffect(1.2)
            }
        }
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
                    .foregroundColor(AppColors.dialogBackground)
                    .background(AppColors.buttonPrimary)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
    }

    private var ticketSearchSheet: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text(LocalizedStringKey("scanner_search_description"))
                    .font(.subheadline)
                    .foregroundColor(AppColors.textSecondary)

                TextField(
                    NSLocalizedString("scanner_search_email_placeholder", comment: ""),
                    text: Binding(
                        get: { viewModel.state.searchQuery },
                        set: { viewModel.onAction(.updateTicketSearchQuery($0)) }
                    )
                )
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)

                if !viewModel.state.ticketTypes.isEmpty {
                    Picker(
                        NSLocalizedString("scanner_search_ticket_type_all", comment: ""),
                        selection: Binding(
                            get: { viewModel.state.selectedTicketTypeId },
                            set: { viewModel.onAction(.updateTicketSearchType($0)) }
                        )
                    ) {
                        Text(LocalizedStringKey("scanner_search_ticket_type_all")).tag(nil as String?)
                        ForEach(viewModel.state.ticketTypes) { type in
                            Text(type.name).tag(type.id as String?)
                        }
                    }
                    .pickerStyle(.menu)
                }

                Button {
                    viewModel.onAction(
                        .submitTicketSearch(
                            query: viewModel.state.searchQuery,
                            ticketTypeId: viewModel.state.selectedTicketTypeId
                        )
                    )
                } label: {
                    Text(LocalizedStringKey("scanner_search_button"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(AppColors.dialogBackground)
                        .background(
                            viewModel.state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                ? AppColors.textMuted
                                : AppColors.buttonPrimary
                        )
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
                .disabled(viewModel.state.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                if viewModel.state.isSearching {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else if let error = viewModel.state.searchError {
                    Text(String(format: NSLocalizedString("scanner_search_error", comment: ""), error))
                        .font(.footnote)
                        .foregroundColor(AppColors.error)
                } else if !viewModel.state.searchResults.isEmpty {
                    List(viewModel.state.searchResults) { ticket in
                        Button {
                            viewModel.onAction(.selectSearchResult(ticket))
                        } label: {
                            ticketSearchRow(ticket)
                        }
                        .listRowBackground(AppColors.cardBackground)
                    }
                    .listStyle(.plain)
                } else if viewModel.state.hasSubmittedTicketSearch {
                    Text(LocalizedStringKey("scanner_search_no_results"))
                        .font(.footnote)
                        .foregroundColor(AppColors.textMuted)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 20)
                }

                Spacer()
            }
            .padding(16)
            .navigationTitle(LocalizedStringKey("scanner_search_title"))
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func ticketSearchRow(_ ticket: VisitorTicket) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if let email = ticket.email, !email.isEmpty {
                Text(email)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(AppColors.textPrimary)
            }
            HStack(spacing: 8) {
                if let type = ticket.ticketType, !type.isEmpty {
                    Text(type)
                        .font(.caption)
                        .foregroundColor(AppColors.textSecondary)
                }
                Text(ticketStatusLabel(ticket.status))
                    .font(.caption.weight(.semibold))
                    .foregroundColor(ticketStatusColor(ticket.status))
            }
        }
    }

    private func ticketDetailSheet(_ ticket: VisitorTicket) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(LocalizedStringKey("scanner_ticket_details_title"))
                .font(.title3.weight(.bold))
                .foregroundColor(AppColors.textPrimary)

            VStack(alignment: .leading, spacing: 8) {
                detailField(
                    label: NSLocalizedString("scanner_field_status", comment: ""),
                    value: ticketStatusLabel(ticket.status),
                    color: ticketStatusColor(ticket.status)
                )
                if let type = ticket.ticketType, !type.isEmpty {
                    detailField(
                        label: NSLocalizedString("scanner_field_ticket_type_label", comment: ""),
                        value: type
                    )
                }
                if let email = ticket.email, !email.isEmpty {
                    detailField(
                        label: NSLocalizedString("scanner_field_email_label", comment: ""),
                        value: email
                    )
                }
                if let scannedAt = ticket.scannedAt, !scannedAt.isEmpty {
                    detailField(
                        label: NSLocalizedString("scanner_field_scanned_at_label", comment: ""),
                        value: formatTicketDate(scannedAt)
                    )
                }
                if let validWindow = ticketValidityWindowText(ticket) {
                    detailField(
                        label: NSLocalizedString("scanner_field_valid_window_label", comment: ""),
                        value: validWindow
                    )
                }
                detailField(
                    label: NSLocalizedString("scanner_field_ticket_id", comment: ""),
                    value: ticket.id
                )
            }
            .padding(12)
            .background(AppColors.cardBackground)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(AppColors.border, lineWidth: 1)
            )

            Spacer()

            if ticket.status == .issued {
                Button {
                    viewModel.onAction(.scanFromDetail(ticketId: ticket.id))
                } label: {
                    Text(LocalizedStringKey("scanner_button_mark_scanned"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(AppColors.dialogBackground)
                        .background(AppColors.success)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
            }

            Button {
                viewModel.onAction(.dismissSearchDetail)
            } label: {
                Text(LocalizedStringKey("scanner_button_close_result"))
                    .frame(maxWidth: .infinity)
                    .padding()
                    .foregroundColor(AppColors.textPrimary)
                    .background(AppColors.cardBackground)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(AppColors.background.ignoresSafeArea())
    }

    private func detailField(label: String, value: String, color: Color? = nil) -> some View {
        HStack(alignment: .top) {
            Text(label)
                .font(.footnote)
                .foregroundColor(AppColors.textMuted)
                .frame(width: 100, alignment: .leading)
            Text(value)
                .font(.footnote.weight(.medium))
                .foregroundColor(color ?? AppColors.textPrimary)
        }
    }

    private func ticketValidityWindowText(_ ticket: VisitorTicket) -> String? {
        let validFrom = ticket.validFrom?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let validUntil = ticket.validUntil?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        switch (validFrom.isEmpty, validUntil.isEmpty) {
        case (false, false):
            return "\(formatTicketDate(validFrom)) – \(formatTicketDate(validUntil))"
        case (false, true):
            return formatTicketDate(validFrom)
        case (true, false):
            return formatTicketDate(validUntil)
        case (true, true):
            return nil
        }
    }

    private func ticketStatusLabel(_ status: VisitorTicketStatus) -> String {
        switch status {
        case .scanned:
            return NSLocalizedString("scanner_search_result_scanned", comment: "")
        case .issued:
            return NSLocalizedString("scanner_search_result_not_scanned", comment: "")
        case .unknown:
            return NSLocalizedString("scanner_status_unknown", comment: "")
        }
    }

    private func ticketStatusColor(_ status: VisitorTicketStatus) -> Color {
        switch status {
        case .scanned:
            return AppColors.info
        case .issued:
            return AppColors.success
        case .unknown:
            return AppColors.textMuted
        }
    }

    private func formatTicketDate(_ raw: String) -> String {
        if let date = Self.isoFormatter.date(from: raw) {
            return Self.ticketDetailDateFormatter.string(from: date)
        }

        let fallbackFormatter = ISO8601DateFormatter()
        fallbackFormatter.formatOptions = [.withInternetDateTime]
        if let date = fallbackFormatter.date(from: raw) {
            return Self.ticketDetailDateFormatter.string(from: date)
        }

        return raw
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

            let message = (result.message?.isEmpty == false)
                ? result.message!
                : statusMessage(result.status)
            Text(message)
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
                    .foregroundColor(AppColors.dialogBackground)
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
