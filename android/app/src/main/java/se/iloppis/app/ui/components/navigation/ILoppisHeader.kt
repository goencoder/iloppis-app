package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.ui.theme.AppColors

/**
 * Unified app header — "Soft Band" design.
 *
 * Warm neutral background with logo centered at the top.
 * Accepts an optional [content] slot for toolbar elements (buttons, search,
 * filter chips) that should visually belong to the header zone.
 * A single 1px warm gray divider separates the entire header from scrollable content.
 *
 * @param subtitle Optional page subtitle below the logo.
 * @param content  Optional composable slot for toolbar items (buttons, search, filters).
 */
@Composable
fun ILoppisHeader(
    subtitle: Int? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.HeaderBackground)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.iloppis_logo_black),
            contentDescription = stringResource(R.string.app_title),
            modifier = Modifier
                .height(40.dp)
                .padding(horizontal = 16.dp)
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitle),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary
            )
        }

        if (content != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                content()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(
            color = AppColors.HeaderDivider,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
