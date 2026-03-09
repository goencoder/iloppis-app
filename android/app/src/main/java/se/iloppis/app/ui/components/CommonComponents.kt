package se.iloppis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.domain.model.EventDisplayStatus
import se.iloppis.app.ui.theme.AppColors

/**
 * Displays a badge showing the computed display status.
 * Uses ONGOING (green), UPCOMING (blue), PAST (gray).
 */
@Composable
fun DisplayStatusBadge(status: EventDisplayStatus) {
    val (bgColor, textColor) = when (status) {
        EventDisplayStatus.ONGOING -> AppColors.BadgeOngoingBackground to AppColors.BadgeOngoingText
        EventDisplayStatus.UPCOMING -> AppColors.BadgeInfoBackground to AppColors.BadgeInfoText
        EventDisplayStatus.PAST -> AppColors.BadgeDefaultBackground to AppColors.BadgeDefaultText
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(status.stringResId),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Individual code input box for the code entry screen.
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
                color = AppColors.InputBackground,
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
