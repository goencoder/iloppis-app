package se.iloppis.app.ui.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import se.iloppis.app.ui.theme.AppColors

/**
 * iLoppis styled markdown text object
 *
 * Displays text with markdown data.
 * The markdown has custom iLoppis
 * configuration for styling.
 */
@Composable
fun MarkdownText(text: String) {
    Markdown(
        content = text,
        typography = markdownTypography(
            h1 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 38.sp,
                color = AppColors.TextSecondary
            ),
            h2 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 34.sp,
                color = AppColors.TextSecondary
            ),
            h3 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 30.sp,
                color = AppColors.TextSecondary
            ),
            h4 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 26.sp,
                color = AppColors.TextSecondary
            ),
            h5 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 22.sp,
                color = AppColors.TextSecondary
            ),
            h6 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                color = AppColors.TextSecondary
            ),
            paragraph = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
        )
    )
}
