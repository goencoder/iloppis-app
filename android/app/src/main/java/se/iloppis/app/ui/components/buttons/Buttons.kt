package se.iloppis.app.ui.components.buttons

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Button component with an icon and text object
 */
@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    text: Int,
    icon: ImageVector,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onclick: () -> Unit
) {
    Button(
        onClick = onclick,
        modifier = modifier,
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            modifier = Modifier.size(20.dp),
            contentDescription = stringResource(text)
        )
        Text(
            text = stringResource(text),
            modifier = Modifier.padding(start = 5.dp),
            fontSize = 12.sp
        )
    }
}
