package com.githarshking.the_digital_munshi

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.json.JSONObject

object QrCodeUtils {

    /**
     * Generates a QR Code ImageBitmap from a String.
     */
    fun generateQrCode(content: String, size: Int = 512): ImageBitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1

            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
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
     * Creates the "Bank-Grade" JSON Payload (Schema 2.0)
     */
    fun createCreditProfileJson(
        userName: String,
        userOccupation: String,
        deviceName: String,
        income: Double,
        savings: Double,
        stabilityScore: Double,
        stabilityLabel: String,
        profitMargin: Int,
        signature: String,
        publicKey: String
    ): String {
        // Mask the name slightly for privacy in the QR (Optional, but good practice)
        // For the hackathon, we show full name to prove identity.
        val identity = "$userName ($userOccupation)"

        val root = JSONObject()
        root.put("ver", "2.0")
        root.put("uid", identity)

        val meta = JSONObject()
        meta.put("generated_at", System.currentTimeMillis() / 1000)
        meta.put("device_model", deviceName)
        meta.put("is_65b_compliant", true)
        root.put("meta", meta)

        val financials = JSONObject()
        financials.put("period", "All Time")
        financials.put("gross_income", income)
        financials.put("net_surplus", savings)
        financials.put("profit_margin_percent", profitMargin)
        financials.put("stability_score", stabilityScore)
        financials.put("stability_band", stabilityLabel)
        root.put("financials", financials)

        val security = JSONObject()
        security.put("signature", signature)
        security.put("public_key", publicKey)
        root.put("security", security)

        return root.toString()
    }
}