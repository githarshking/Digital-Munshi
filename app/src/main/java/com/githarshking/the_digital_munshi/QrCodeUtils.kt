package com.githarshking.the_digital_munshi

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeUtils {

    /**
     * Generates a QR Code ImageBitmap from a String.
     */
    fun generateQrCode(content: String, size: Int = 512): ImageBitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1 // Small white border

            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    // Black for 1, White for 0
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a Compact JSON string for the QR Code.
     * We construct this manually to avoid adding a heavy JSON library dependency.
     */
    fun createSharePayload(
        userId: String,
        income: Double,
        score: Double,
        signature: String,
        publicKey: String
    ): String {
        // We truncate the keys to save QR space
        return """
            {
              "id": "$userId",
              "inc": $income,
              "cv": $score,
              "sig": "$signature",
              "pk": "$publicKey"
            }
        """.trimIndent()
    }
}