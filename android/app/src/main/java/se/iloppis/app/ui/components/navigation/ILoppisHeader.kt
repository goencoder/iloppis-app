package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.ui.theme.AppColors

/**
 * iLoppis header component
 */
@Composable
fun ILoppisHeader() {
    Text(
        text = stringResource(R.string.app_title),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextPrimary,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}
