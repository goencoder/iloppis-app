package se.iloppis.app.ui.screens.live_stats

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import se.iloppis.app.network.config.clientConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal const val META_ROTATE_MS = 12_000L
internal const val META_FLIP_ANIM_MS = 700

// QR scanners expect high contrast, so keep the matrix pure black on white.
private const val QR_FOREGROUND_ARGB = 0xFF000000.toInt()
private const val QR_BACKGROUND_ARGB = 0xFFFFFFFF.toInt()

internal fun createQrBitmap(value: String, sizePx: Int): Bitmap? = runCatching {
    val hints = mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val matrix = MultiFormatWriter().encode(
        value,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        hints
    )

    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        val rowOffset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[rowOffset + x] = if (matrix[x, y]) {
                QR_FOREGROUND_ARGB
            } else {
                QR_BACKGROUND_ARGB
            }
        }
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    bitmap
}.getOrNull()

internal fun visitUrl(eventId: String): String {
    val baseUrl = runCatching { clientConfig().url }
        .getOrDefault("https://iloppis.se/")
        .trimEnd('/')
    val encodedEventId = URLEncoder.encode(eventId, StandardCharsets.UTF_8.toString())
    return "$baseUrl/visit?event_id=$encodedEventId"
}
