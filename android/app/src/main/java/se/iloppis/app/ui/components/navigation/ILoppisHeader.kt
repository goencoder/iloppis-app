package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.ui.theme.AppColors

/**
 * App header with iLoppis logo, optional subtitle, and a subtle divider.
 *
 * @param subtitle Optional string resource ID for a subtitle below the logo (e.g. page name).
 *                 Pass `null` for the main home screen where only the logo is needed.
 */
@Composable
fun ILoppisHeader(subtitle: Int? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        ILoppisLogo(
            modifier = Modifier.padding(start = 4.dp),
            size = LogoSize.Medium
        )

        if (subtitle != null) {
            Text(
                text = stringResource(subtitle),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            color = AppColors.Border,
            thickness = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
