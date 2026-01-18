import SwiftUI

struct EventDetailDialog: View {
    let event: Event
    let onDismiss: () -> Void
    let onCashierClick: () -> Void
    let onScannerClick: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(event.name)
                        .font(.title3.weight(.bold))
                        .foregroundColor(AppColors.textPrimary)
                    Text(locationText)
                        .font(.subheadline)
                        .foregroundColor(AppColors.textSecondary)
                }
                Spacer()
                statusBadge
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundColor(AppColors.textSecondary)
                        .padding(8)
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 8) {
                Image(systemName: "clock")
                    .foregroundColor(AppColors.textSecondary)
                Text(timeRangeText)
                    .foregroundColor(AppColors.textSecondary)
                    .font(.subheadline)
            }

            Text(LocalizedStringKey("select_mode"))
                .font(.headline)
                .foregroundColor(AppColors.textPrimary)

            VStack(spacing: 10) {
                Button(action: onCashierClick) {
                    Text(LocalizedStringKey("button_open_cashier"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(.white)
                        .background(AppColors.buttonPrimary)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)

                Button(action: onScannerClick) {
                    Text(LocalizedStringKey("button_ticket_scanner"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(.white)
                        .background(AppColors.buttonPrimary)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)

                Button(action: onDismiss) {
                    Text(LocalizedStringKey("button_cancel"))
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundColor(AppColors.textSecondary)
                        .background(AppColors.cardBackground)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(20)
        .background(AppColors.dialogBackground)
    }

    private var locationText: String {
        let parts = [event.addressStreet, event.addressCity]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        if parts.isEmpty {
            return NSLocalizedString("location_not_specified", comment: "")
        }
        return parts.joined(separator: ", ")
    }

    private var timeRangeText: String {
        let start = event.startTime?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let end = event.endTime?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if start.isEmpty && end.isEmpty {
            return NSLocalizedString("time_not_specified", comment: "")
        }
        if end.isEmpty { return start }
        if start.isEmpty { return end }
        return "\(start) - \(end)"
    }

    private var statusBadge: some View {
        let (textKey, background, foreground): (String, Color, Color) = {
            switch (event.lifecycleState ?? "").uppercased() {
            case "OPEN":
                return ("state_open", AppColors.badgeOpenBackground, AppColors.badgeOpenText)
            case "UPCOMING":
                return ("state_upcoming", AppColors.badgeUpcomingBackground, AppColors.badgeUpcomingText)
            case "CLOSED":
                return ("state_closed", AppColors.badgeDefaultBackground, AppColors.badgeDefaultText)
            default:
                return ("state_unknown", AppColors.badgeDefaultBackground, AppColors.badgeDefaultText)
            }
        }()

        return Text(LocalizedStringKey(textKey))
            .font(.caption2.weight(.bold))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .foregroundColor(foreground)
            .background(background)
            .clipShape(Capsule())
    }
}
