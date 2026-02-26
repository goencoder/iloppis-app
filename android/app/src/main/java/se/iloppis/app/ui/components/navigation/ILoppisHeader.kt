package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.ui.theme.AppColors

/**
 * App header — "Soft Band" design.
 *
 * Warm neutral background (#F7F6F4) avoids green-on-green with the logo.
 * 1px warm gray border provides clear separation from content.
 * Logo centered, sized Large for prominence.
 *
 * @param subtitle Optional string resource ID for a subtitle below the logo.
 */
@Composable
fun ILoppisHeader(subtitle: Int? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F6F4))
            .padding(top = 16.dp, bottom = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ILoppisLogo(size = LogoSize.Large)

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitle),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(
            color = Color(0xFFE8E5E0),
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
