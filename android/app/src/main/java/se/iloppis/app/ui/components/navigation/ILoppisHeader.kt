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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.ui.theme.AppColors

/**
 * App header with iLoppis logo, optional subtitle, and a subtle divider.
 *
 * Tinted background area to give the header visual weight,
 * with the logo centered and sized Large for prominence.
 *
 * @param subtitle Optional string resource ID for a subtitle below the logo (e.g. page name).
 *                 Pass `null` for the main home screen where only the logo is needed.
 */
@Composable
fun ILoppisHeader(subtitle: Int? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Primary.copy(alpha = 0.06f))
            .padding(top = 16.dp, bottom = 8.dp),
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

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(
            color = AppColors.Border,
            thickness = 0.5.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
