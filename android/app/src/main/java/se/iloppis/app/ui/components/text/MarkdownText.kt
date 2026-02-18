package se.iloppis.app.ui.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            h2 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            h3 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            h4 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            h5 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            h6 = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            paragraph = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    )
}
