package se.iloppis.app.ui.components.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.ui.theme.AppColors

/** Visual treatment for an [AppButton]. */

// ── Enums ────────────────────────────────────

enum class AppButtonVariant {
    /** Main call to action. */
    Primary,
    /** Successful or confirming action. */
    Success,
    /** Neutral alternative action. */
    Secondary,
    /** Destructive or high-risk action. */
    Danger,
    /** Low-emphasis outlined action. */
    Outlined,
    /** Text-only action. */
    Text
}

/** Supported [AppButton] dimensions. */
enum class AppButtonSize(val height: Dp, val fontSize: Int, val paddingH: Dp) {
    Small(32.dp, 12, 12.dp),
    Medium(40.dp, 14, 16.dp),
    Large(48.dp, 16, 20.dp),
    XLarge(56.dp, 16, 24.dp)
}

// ── Component ────────────────────────────────

/**
 * Displays a consistently styled app button.
 *
 * While [loading] is true, the button is disabled and shows a progress indicator.
 * Explicit color arguments override the selected [variant].
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val contentPadding = PaddingValues(horizontal = size.paddingH)

    when (variant) {
        AppButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(size.height),
                enabled = enabled && !loading,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: AppColors.ButtonPrimary,
                    contentColor = contentColor ?: AppColors.OnButtonPrimary,
                    disabledContainerColor = AppColors.ButtonPrimaryDisabled,
                )
            ) { ButtonContent(text, size, loading, leadingIcon) }
        }

        AppButtonVariant.Success -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(size.height),
                enabled = enabled && !loading,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: AppColors.Success,
                    contentColor = contentColor ?: AppColors.OnButtonPrimary,
                    disabledContainerColor = AppColors.ButtonPrimaryDisabled,
                )
            ) { ButtonContent(text, size, loading, leadingIcon) }
        }

        AppButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(size.height),
                enabled = enabled && !loading,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: AppColors.ButtonSecondary,
                    contentColor = contentColor ?: AppColors.OnButtonSecondary,
                    disabledContainerColor = AppColors.ButtonPrimaryDisabled,
                )
            ) { ButtonContent(text, size, loading, leadingIcon) }
        }

        AppButtonVariant.Danger -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(size.height),
                enabled = enabled && !loading,
                shape = shape,
                contentPadding = contentPadding,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(borderColor ?: AppColors.Error)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = contentColor ?: AppColors.Error,
                )
            ) { ButtonContent(text, size, loading, leadingIcon) }
        }

        AppButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(size.height),
                enabled = enabled && !loading,
                shape = shape,
                contentPadding = contentPadding,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(borderColor ?: AppColors.Primary)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = contentColor ?: AppColors.Primary,
                )
            ) { ButtonContent(text, size, loading, leadingIcon) }
        }

        AppButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(size.height),
                enabled = enabled && !loading,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = contentColor ?: AppColors.TextSecondary
                )
            ) { ButtonContent(text, size, loading, leadingIcon) }
        }
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    size: AppButtonSize,
    loading: Boolean,
    leadingIcon: @Composable (() -> Unit)?,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
        Spacer(modifier = Modifier.width(8.dp))
    } else {
        leadingIcon?.let {
            it()
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
    Text(text = text, fontSize = size.fontSize.sp)
}
