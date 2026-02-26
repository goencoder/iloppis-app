package se.iloppis.app.ui.screens.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.ui.components.buttons.AppButton
import se.iloppis.app.ui.components.buttons.AppButtonSize
import se.iloppis.app.ui.components.buttons.AppButtonVariant
import se.iloppis.app.ui.theme.AppColors

/**
 * Payment section showing total, paid amount input, change, and payment buttons.
 */
@Composable
fun PaymentSection(
    total: Int,
    paidAmount: String,
    change: Int,
    isProcessing: Boolean,
    onPaidAmountChange: (String) -> Unit,
    onCashPayment: () -> Unit,
    onSwishPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cashier_total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "$total kr",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            HorizontalDivider(color = AppColors.Border)

            // Paid amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cashier_paid),
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Background)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    BasicTextField(
                        value = paidAmount,
                        onValueChange = onPaidAmountChange,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.End
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Change row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cashier_change),
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = if (change >= 0) "$change kr" else "-",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (change >= 0) AppColors.ButtonSuccess else AppColors.Error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = stringResource(R.string.cashier_button_cash),
                    onClick = onCashPayment,
                    enabled = !isProcessing && total > 0,
                    loading = isProcessing,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Large,
                    containerColor = AppColors.TextPrimary,
                    contentColor = AppColors.OnButtonPrimary
                )

                AppButton(
                    text = stringResource(R.string.cashier_button_swish),
                    onClick = onSwishPayment,
                    enabled = !isProcessing && total > 0,
                    loading = isProcessing,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Large,
                    containerColor = AppColors.SwishBlue,
                    contentColor = AppColors.OnButtonPrimary
                )
            }
        }
    }
}
