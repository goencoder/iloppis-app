import SwiftUI

/// Central color palette mirroring Android AppColors.
struct AppColors {
    // Brand colors
    static let background = Color(red: 0.960, green: 0.910, blue: 0.913) // #F5E8E9
    static let cardBackground = Color(red: 0.980, green: 0.953, blue: 0.957) // #FAF3F4
    static let dialogBackground = Color.white
    static let primary = Color(red: 0.718, green: 0.110, blue: 0.110) // #B71C1C
    static let primaryLight = Color(red: 0.875, green: 0.349, blue: 0.192) // #DF5931
    static let border = Color(red: 0.886, green: 0.909, blue: 0.941) // #E2E8F0

    // Text colors
    static let textPrimary = Color(red: 0.176, green: 0.216, blue: 0.282) // #2D3748
    static let textSecondary = Color(red: 0.290, green: 0.333, blue: 0.408) // #4A5568
    static let textMuted = Color.gray
    static let textDark = Color(.darkGray)
    static let textError = Color(red: 0.898, green: 0.243, blue: 0.243) // #E53E3E

    // Button semantic colors
    static let buttonPrimary = primary
    static let buttonPrimaryDisabled = Color(red: 0.796, green: 0.835, blue: 0.878) // #CBD5E0
    static let buttonSecondary = Color(red: 0.443, green: 0.502, blue: 0.588) // #718096

    // Status colors
    static let success = Color(red: 0.298, green: 0.686, blue: 0.314) // #4CAF50
    static let warning = Color(red: 0.965, green: 0.678, blue: 0.333) // #F6AD55
    static let error = Color(red: 0.898, green: 0.243, blue: 0.243) // #E53E3E
    static let info = Color(red: 0.129, green: 0.588, blue: 0.953) // #2196F3

    // Input colors
    static let inputBackground = Color(red: 0.969, green: 0.980, blue: 0.988) // #F7FAFC
    static let inputBorder = border
    static let inputBorderFocused = buttonSecondary

    // Badge colors
    static let badgeOpenBackground = Color(red: 0.784, green: 0.902, blue: 0.792) // #C8E6C9
    static let badgeOpenText = Color(red: 0.220, green: 0.557, blue: 0.235) // #388E3C
    static let badgeUpcomingBackground = Color(red: 0.910, green: 0.784, blue: 0.792) // #E8C8CA
    static let badgeUpcomingText = Color(red: 0.427, green: 0.298, blue: 0.255) // #6D4C41
    static let badgeDefaultBackground = Color(red: 0.878, green: 0.878, blue: 0.878) // #E0E0E0
    static let badgeDefaultText = Color(red: 0.459, green: 0.459, blue: 0.459) // #757575

    // Legacy aliases
    static let buttonSuccess = success
    static let buttonInfo = info
    static let buttonDanger = error
    static let swishBlue = Color(red: 0.0, green: 0.478, blue: 0.800) // #007ACC
}
