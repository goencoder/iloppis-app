package se.iloppis.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Central color palette for the iLoppis app.
 * All colors used in the app should be referenced from here.
 * 
 * Button Color Guidelines:
 * - Primary (Orange): Main actions (Scan, Verify, Submit, Continue)
 * - Secondary (Gray): Less important actions (Back, Cancel as button)
 * - TextSecondary: Dismiss/Cancel as text link
 * - Success (Green): Positive confirmations (in result sheets only)
 * - Warning (Amber): Caution states (duplicate, offline)
 * - Error (Red): Error states and destructive actions
 */
object AppColors {
    // Skog (Woods) theme - source: iloppis/frontend/src/styles/themes.js (Themes.Woods)
    val Background = Color(0xFFF9FAFB)
    val CardBackground = Color(0xFFFFFFFF)
    val DialogBackground = Color(0xFFFFFFFF)
    val Primary = Color(0xFF2F6A6A)
    val PrimaryLight = Color(0xFF346E79)  // Woods accent
    val Border = Color(0xFFE5E7EB)

    // Text colors
    val TextPrimary = Color(0xFF132A2F)
    val TextSecondary = Color(0xFF5B6B7A)
    val TextMuted = Color(0xFF5B6B7A)
    val TextDark = Color(0xFF132A2F)
    val TextError = Color(0xFFD32F2F)

    // Button semantic colors
    val ButtonPrimary = Primary  // Main action buttons
    val ButtonPrimaryDisabled = Color(0xFFCBD5E0)
    val ButtonSecondary = Border  // Secondary actions
    
    // Status colors (for result sheets, badges, feedback)
    val Success = Color(0xFF2E7D6B)  // Woods success
    val Warning = Color(0xFFDAA000)  // Woods warning
    val Error = Color(0xFFD32F2F)    // Woods danger
    val Info = Color(0xFF346E79)     // Woods accent

    // Legacy aliases (for existing code compatibility)
    val ButtonSuccess = Success
    val ButtonInfo = Info
    val ButtonDanger = Error
    val SwishBlue = Color(0xFF007ACC)  // Swish brand color

    // Input colors
    val InputBackground = Background
    val InputBorder = Border
    val InputBorderFocused = Primary

    // Badge colors - Open state
    val BadgeOpenBackground = Success.copy(alpha = 0.18f)
    val BadgeOpenText = Success

    // Badge colors - Upcoming state
    val BadgeUpcomingBackground = Warning.copy(alpha = 0.18f)
    val BadgeUpcomingText = Warning

    // Badge colors - Default/Closed state
    val BadgeDefaultBackground = Border
    val BadgeDefaultText = TextSecondary

    // Badge colors - Ongoing (Pågående) state - same as Open
    val BadgeOngoingBackground = BadgeOpenBackground
    val BadgeOngoingText = BadgeOpenText

    // Badge colors - Info/Upcoming (Kommande) state
    val BadgeInfoBackground = Info.copy(alpha = 0.18f)
    val BadgeInfoText = Info

    // Splash screen
    val SplashBackground = Success
    val OnSplashBackground = Color.White

    // On-button text colors
    val OnButtonPrimary = Color.White
    val OnButtonSecondary = TextPrimary

    // Accent / decoration
    val Gold = Warning

    // Surface variants (for containers, backgrounds)
    val SurfaceVariant = Border
    val ErrorContainer = Error.copy(alpha = 0.12f)
    val OnErrorContainer = Error
    val WarningContainer = Warning.copy(alpha = 0.12f)
    val OnWarningContainer = Warning

    // Top-level text on surface
    val OnBackground = TextPrimary
    val NavigatorOverlay = TextPrimary  // onSurface equivalent for scrim
}
