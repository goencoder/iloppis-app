package se.iloppis.app.ui.screens.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.R
import se.iloppis.app.ui.theme.AppColors

/**
 * Numeric keypad component for cashier input.
 * Supports digits 0-9, clear, backspace, space (for multiple prices), and OK.
 */
@Composable
fun NumericKeypad(
    onDigitPress: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton("1", onClick = { onDigitPress("1") }, modifier = Modifier.weight(1f))
            KeypadButton("2", onClick = { onDigitPress("2") }, modifier = Modifier.weight(1f))
            KeypadButton("3", onClick = { onDigitPress("3") }, modifier = Modifier.weight(1f))
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton("4", onClick = { onDigitPress("4") }, modifier = Modifier.weight(1f))
            KeypadButton("5", onClick = { onDigitPress("5") }, modifier = Modifier.weight(1f))
            KeypadButton("6", onClick = { onDigitPress("6") }, modifier = Modifier.weight(1f))
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton("7", onClick = { onDigitPress("7") }, modifier = Modifier.weight(1f))
            KeypadButton("8", onClick = { onDigitPress("8") }, modifier = Modifier.weight(1f))
            KeypadButton("9", onClick = { onDigitPress("9") }, modifier = Modifier.weight(1f))
        }

        // Row 4: Clear, 0, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(
                text = stringResource(R.string.keypad_clear),
                onClick = onClear,
                modifier = Modifier.weight(1f)
            )
            KeypadButton("0", onClick = { onDigitPress("0") }, modifier = Modifier.weight(1f))
            KeypadButton(
                text = "⌫",
                onClick = onBackspace,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 5: Space (2 columns), OK (1 column)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(
                text = "␣",
                onClick = onSpace,
                modifier = Modifier.weight(2f)
            )
            KeypadButton(
                text = stringResource(R.string.keypad_ok),
                onClick = onOk,
                modifier = Modifier.weight(1f),
                isPrimary = true
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val backgroundColor = if (isPrimary) AppColors.ButtonSuccess else AppColors.CardBackground
    val textColor = if (isPrimary) Color.White else AppColors.TextPrimary

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
