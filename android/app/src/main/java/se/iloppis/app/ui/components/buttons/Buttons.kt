package se.iloppis.app.ui.components.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Button component with an icon and text object
 */
@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    text: Int,
    icon: ImageVector,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Small,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: () -> Unit
) {
    AppButton(
        text = stringResource(text),
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        containerColor = containerColor,
        contentColor = contentColor,
        leadingIcon = {
            Icon(
                imageVector = icon,
                modifier = Modifier.size(20.dp),
                contentDescription = stringResource(text)
            )
        }
    )
}
