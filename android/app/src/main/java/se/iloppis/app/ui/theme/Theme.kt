package se.iloppis.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * Light-only color scheme sourced entirely from AppColors.
 * No dynamic colors, no dark theme.
 */
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    secondary = AppColors.Success,
    error = AppColors.Error,

    onPrimary = AppColors.OnButtonPrimary,
    primaryContainer = AppColors.BadgeUpcomingBackground,
    onPrimaryContainer = AppColors.OnBackground,
    onSecondary = AppColors.OnButtonPrimary,
    background = AppColors.Background,
    surface = AppColors.CardBackground,
    onBackground = AppColors.OnBackground,
    onSurface = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextDark,
    surfaceDim = AppColors.TextMuted,
    surfaceVariant = AppColors.SurfaceVariant,
    tertiary = AppColors.TextPrimary,
    onTertiaryFixed = AppColors.Gold,
    errorContainer = AppColors.ErrorContainer,
    onErrorContainer = AppColors.OnErrorContainer,
    tertiaryContainer = AppColors.WarningContainer,
)

@Composable
fun ILoppisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
