package se.iloppis.app.ui.components.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.iloppis.app.ui.theme.AppColors

/**
 * iLoppis logo matching the frontend design:
 * Shop icon + "iLoppis" text in a pill-shaped container.
 *
 * The "i" is colored with [AppColors.Success] (green) and
 * "Loppis" with [AppColors.Primary] (teal), matching the web frontend.
 */
@Composable
fun ILoppisLogo(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Medium,
) {
    Row(
        modifier = modifier
            .background(
                color = AppColors.Primary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = size.paddingH, vertical = size.paddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        ShopIcon(
            size = size.iconSize,
            color = AppColors.Primary
        )
        Spacer(modifier = Modifier.width(size.gap))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = AppColors.Success)) {
                    append("i")
                }
                withStyle(SpanStyle(color = AppColors.Primary)) {
                    append("Loppis")
                }
            },
            fontSize = size.fontSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).sp
        )
    }
}

enum class LogoSize(
    val iconSize: Dp,
    val fontSize: TextUnit,
    val paddingH: Dp,
    val paddingV: Dp,
    val gap: Dp
) {
    Small(iconSize = 18.dp, fontSize = 16.sp, paddingH = 8.dp, paddingV = 4.dp, gap = 4.dp),
    Medium(iconSize = 26.dp, fontSize = 22.sp, paddingH = 14.dp, paddingV = 6.dp, gap = 6.dp),
    Large(iconSize = 34.dp, fontSize = 28.sp, paddingH = 16.dp, paddingV = 8.dp, gap = 8.dp)
}

/**
 * Shop/storefront icon matching the frontend SVG.
 * viewBox 0 0 24 24, stroke-only paths.
 */
@Composable
private fun ShopIcon(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Scale from 24x24 viewBox to actual size
        fun sx(x: Float) = x / 24f * w
        fun sy(y: Float) = y / 24f * h

        val stroke = Stroke(
            width = 1.5f / 24f * w,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Path 1: Shop body (walls)
        val body = Path().apply {
            moveTo(sx(3.01f), sy(11.22f))
            lineTo(sx(3.01f), sy(15.71f))
            cubicTo(sx(3.01f), sy(20.2f), sx(4.81f), sy(22f), sx(9.3f), sy(22f))
            lineTo(sx(14.69f), sy(22f))
            cubicTo(sx(19.18f), sy(22f), sx(20.98f), sy(20.2f), sx(20.98f), sy(15.71f))
            lineTo(sx(20.98f), sy(11.22f))
        }
        drawPath(body, color, style = stroke)

        // Path 2: Center awning
        val center = Path().apply {
            moveTo(sx(12f), sy(12f))
            cubicTo(sx(13.83f), sy(12f), sx(15.18f), sy(10.51f), sx(15f), sy(8.68f))
            lineTo(sx(14.34f), sy(2f))
            lineTo(sx(9.67f), sy(2f))
            lineTo(sx(9f), sy(8.68f))
            cubicTo(sx(8.82f), sy(10.51f), sx(10.17f), sy(12f), sx(12f), sy(12f))
            close()
        }
        drawPath(center, color, style = stroke)

        // Path 3: Right awning
        val right = Path().apply {
            moveTo(sx(18.31f), sy(12f))
            cubicTo(sx(20.33f), sy(12f), sx(21.81f), sy(10.36f), sx(21.61f), sy(8.35f))
            lineTo(sx(21.33f), sy(5.6f))
            cubicTo(sx(20.97f), sy(3f), sx(19.97f), sy(2f), sx(17.35f), sy(2f))
            lineTo(sx(14.3f), sy(2f))
            lineTo(sx(15f), sy(9.01f))
            cubicTo(sx(15.17f), sy(10.66f), sx(16.66f), sy(12f), sx(18.31f), sy(12f))
            close()
        }
        drawPath(right, color, style = stroke)

        // Path 4: Left awning
        val left = Path().apply {
            moveTo(sx(5.64f), sy(12f))
            cubicTo(sx(7.29f), sy(12f), sx(8.78f), sy(10.66f), sx(8.94f), sy(9.01f))
            lineTo(sx(9.16f), sy(6.8f))
            lineTo(sx(9.64f), sy(2f))
            lineTo(sx(6.59f), sy(2f))
            cubicTo(sx(3.97f), sy(2f), sx(2.97f), sy(3f), sx(2.61f), sy(5.6f))
            lineTo(sx(2.34f), sy(8.35f))
            cubicTo(sx(2.14f), sy(10.36f), sx(3.62f), sy(12f), sx(5.64f), sy(12f))
            close()
        }
        drawPath(left, color, style = stroke)

        // Path 5: Door
        val door = Path().apply {
            moveTo(sx(12f), sy(17f))
            cubicTo(sx(10.33f), sy(17f), sx(9.5f), sy(17.83f), sx(9.5f), sy(19.5f))
            lineTo(sx(9.5f), sy(22f))
            lineTo(sx(14.5f), sy(22f))
            lineTo(sx(14.5f), sy(19.5f))
            cubicTo(sx(14.5f), sy(17.83f), sx(13.67f), sy(17f), sx(12f), sy(17f))
            close()
        }
        drawPath(door, color, style = stroke)
    }
}
