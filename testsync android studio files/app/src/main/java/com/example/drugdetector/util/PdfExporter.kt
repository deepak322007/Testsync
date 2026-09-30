package com.example.drugdetector.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.drugdetector.model.TestRecord
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generateAndSharePdf(context: Context, record: TestRecord, capturedBitmap: Bitmap?) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 20f
                isFakeBoldText = true
            }
            val subTitlePaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                textSize = 12f
                isFakeBoldText = true
            }
            val boldPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 11f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 10f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            var y = 40f

            // Top Header Banner
            canvas.drawText("TestSync — Forensic Evidence Certificate", 40f, y, titlePaint)
            y += 20f
            canvas.drawText("Official Chain-of-Custody Digital Record", 40f, y, subTitlePaint)
            y += 15f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 25f

            // Record & Verdict Overview
            canvas.drawText("Record ID: ${record.id}", 40f, y, boldPaint)
            canvas.drawText("Timestamp: ${record.formattedTimestamp}", 300f, y, textPaint)
            y += 20f

            canvas.drawText("Result Verdict: PRESUMPTIVE ${record.resultCategory.name}", 40f, y, boldPaint)
            canvas.drawText("Confidence Match: ${record.confidencePercent}%", 300f, y, textPaint)
            y += 20f

            canvas.drawText("Kit Name: ${record.kitName}", 40f, y, textPaint)
            canvas.drawText("Target Agent: ${record.targetSubstance}", 300f, y, textPaint)
            y += 25f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 25f

            // Officer & Agency Details
            canvas.drawText("Officer & Agency Identity", 40f, y, subTitlePaint)
            y += 20f
            canvas.drawText("Operator Name: ${record.operatorName.ifBlank { "N/A" }}", 40f, y, textPaint)
            canvas.drawText("Badge / ID: ${record.operatorId.ifBlank { "N/A" }}", 300f, y, textPaint)
            y += 18f
            canvas.drawText("Agency / Branch: ${record.agency.ifBlank { "N/A" }}", 40f, y, textPaint)
            y += 25f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 25f

            // GPS & Location
            canvas.drawText("Geospatial Chain of Custody", 40f, y, subTitlePaint)
            y += 20f
            canvas.drawText("Location: ${record.locationName}", 40f, y, textPaint)
            y += 18f
            canvas.drawText("Coordinates: Lat ${record.latitude}°, Lon ${record.longitude}° (±${record.locationAccuracy}m)", 40f, y, textPaint)
            y += 25f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 25f

            // Cryptographic Verification & QR Code
            canvas.drawText("Cryptographic Seal & Verification", 40f, y, subTitlePaint)
            y += 20f
            canvas.drawText("Image SHA-256 Hash:", 40f, y, boldPaint)
            y += 15f
            canvas.drawText(record.imageHashSha256.take(50) + "...", 40f, y, textPaint)
            y += 20f

            canvas.drawText("Digital Signature (HMAC-SHA256):", 40f, y, boldPaint)
            y += 15f
            canvas.drawText(record.digitalSignature.take(50) + "...", 40f, y, textPaint)
            y += 25f

            // QR Code for Instant Verification
            val qrUrl = "https://testsyncbackend.onrender.com/api/records/verify/${record.id}"
            val qrBitmap = QrCodeGenerator.generateQrCode(qrUrl, 100)
            if (qrBitmap != null) {
                canvas.drawBitmap(qrBitmap, 440f, 620f, paint)
                canvas.drawText("Scan to Verify Cloud Record", 410f, 735f, textPaint)
            }

            // Captured Image
            if (capturedBitmap != null) {
                val scaledBmp = Bitmap.createScaledBitmap(capturedBitmap, 180, 130, true)
                canvas.drawBitmap(scaledBmp, 40f, 620f, paint)
                canvas.drawText("Captured Sample Photo", 40f, 760f, textPaint)
            }

            // Footer Notice
            y = 790f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 15f
            canvas.drawText("Generated by TestSync Field App • Powered by NewGenRev", 40f, y, textPaint)

            pdfDocument.finishPage(page)

            // Save PDF File
            val pdfFile = File(context.cacheDir, "Evidence_Certificate_${record.id}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            // Trigger Share Intent
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Forensic Evidence Certificate"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
