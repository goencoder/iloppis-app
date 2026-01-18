import SwiftUI

struct EventCard: View {
    let event: Event
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(event.name)
                        .font(.headline)
                        .foregroundColor(AppColors.textPrimary)
                    Spacer()
                    statusBadge
                }
                Text(locationText)
                    .font(.subheadline)
                    .foregroundColor(AppColors.textSecondary)
                Text(timeRangeText)
                    .font(.footnote)
                    .foregroundColor(AppColors.textMuted)
            }
            .padding(16)
            .background(AppColors.cardBackground)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(AppColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
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
}
