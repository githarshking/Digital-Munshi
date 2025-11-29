package com.githarshking.the_digital_munshi

import android.content.Context
import android.net.Uri
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor

object PdfUtils {
    fun extractTextFromPdf(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)

                // Limit to first 5 pages to save AI tokens
                val pages = minOf(pdfDocument.numberOfPages, 5)

                for (i in 1..pages) {
                    val text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))
                    stringBuilder.append(text).append("\n")
                }
                pdfDocument.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error extracting text: ${e.message}"
        }
        return stringBuilder.toString()
    }
}