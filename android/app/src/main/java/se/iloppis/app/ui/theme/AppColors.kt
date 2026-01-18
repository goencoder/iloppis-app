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
    // Brand colors
    val Background = Color(0xFFF5E8E9)
    val CardBackground = Color(0xFFFAF3F4)
    val DialogBackground = Color.White
    val Primary = Color(0xFFB71C1C)  // Brand red - primary actions
    val PrimaryLight = Color(0xFFDF5931)  // Orange accent from web
    val Border = Color(0xFFE2E8F0)

    // Text colors
    val TextPrimary = Color(0xFF2D3748)
    val TextSecondary = Color(0xFF4A5568)
    val TextMuted = Color.Gray
    val TextDark = Color.DarkGray
    val TextError = Color(0xFFE53E3E)

    // Button semantic colors
    val ButtonPrimary = Primary  // Main action buttons
    val ButtonPrimaryDisabled = Color(0xFFCBD5E0)
    val ButtonSecondary = Color(0xFF718096)  // Secondary actions
    
    // Status colors (for result sheets, badges, feedback)
    val Success = Color(0xFF4CAF50)  // Green - success states
    val Warning = Color(0xFFF6AD55)  // Amber - caution/attention
    val Error = Color(0xFFE53E3E)    // Red - error states
    val Info = Color(0xFF2196F3)     // Blue - informational

    // Legacy aliases (for existing code compatibility)
    val ButtonSuccess = Success
    val ButtonInfo = Info
    val ButtonDanger = Error
    val SwishBlue = Color(0xFF007ACC)  // Swish brand color

    // Input colors
    val InputBackground = Color(0xFFF7FAFC)
    val InputBorder = Color(0xFFE2E8F0)
    val InputBorderFocused = Color(0xFF718096)

    // Badge colors - Open state
    val BadgeOpenBackground = Color(0xFFC8E6C9)
    val BadgeOpenText = Color(0xFF388E3C)

    // Badge colors - Upcoming state
    val BadgeUpcomingBackground = Color(0xFFE8C8CA)
    val BadgeUpcomingText = Color(0xFF6D4C41)

    // Badge colors - Default/Closed state
    val BadgeDefaultBackground = Color(0xFFE0E0E0)
    val BadgeDefaultText = Color(0xFF757575)
}
