package se.iloppis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.domain.model.EventState
import se.iloppis.app.ui.theme.AppColors

/**
 * Displays a badge showing the event's current state.
 */
@Composable
fun StateBadge(state: EventState) {
    val (bgColor, textColor) = when (state) {
        EventState.OPEN -> AppColors.BadgeOpenBackground to AppColors.BadgeOpenText
        EventState.UPCOMING -> AppColors.BadgeUpcomingBackground to AppColors.BadgeUpcomingText
        else -> AppColors.BadgeDefaultBackground to AppColors.BadgeDefaultText
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(state.stringResId),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Star icon
 */
@Composable
fun StarIcon() {
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = stringResource(R.string.store_event_locally),
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

/**
 * Individual code input box for the code entry dialog.
 */
@Composable
fun CodeBox(
    char: String,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    hasError: Boolean = false
) {
    val borderColor = when {
        hasError -> AppColors.TextError
        isFocused -> AppColors.InputBorderFocused
        else -> AppColors.InputBorder
    }
    val borderWidth = if (isFocused || hasError) 2.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = if (hasError) AppColors.InputBackground else AppColors.InputBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (hasError) AppColors.TextError else AppColors.TextPrimary
        )
    }
}

/**
 * Primary action button with consistent styling.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonPrimary,
            disabledContainerColor = AppColors.ButtonPrimaryDisabled
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Cancel/dismiss text button with consistent styling.
 */
@Composable
fun CancelTextButton(
    text: String = "Avbryt",
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = AppColors.TextError,
            fontSize = 14.sp
        )
    }
}
