package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

    fun exportCsvAndShare(
        context: Context,
        periodTitle: String,
        transactions: List<TransactionRecord>,
        totalIncome: Double,
        totalExpense: Double
    ): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = "hisab_khata_report_${System.currentTimeMillis()}.csv"
        val file = File(dir, safeName)
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM for Excel Bengali support
        sb.appendLine("Hisab Khata Report,$periodTitle")
        sb.appendLine("Total Income,${totalIncome},Total Expense,${totalExpense},Net Balance,${totalIncome - totalExpense}")
        sb.appendLine()
        sb.appendLine("Date,Type,Category,Payment Method,Amount (BDT),Note")
        transactions.forEach { tx ->
            val dateStr = Formatters.formatDate(tx.dateEpochMillis)
            val typeStr = if (tx.parsedType == TransactionType.INCOME) "Income" else "Expense"
            val cat = escapeCsv(tx.categoryName)
            val pm = escapeCsv(tx.paymentMethodName)
            val note = escapeCsv(tx.note)
            sb.appendLine("$dateStr,$typeStr,$cat,$pm,${tx.amount},$note")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, file, "text/csv", "CSV রিপোর্ট শেয়ার করুন")
    }

    fun exportPdfAndShare(
        context: Context,
        periodTitle: String,
        transactions: List<TransactionRecord>,
        totalIncome: Double,
        totalExpense: Double
    ): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "hisab_khata_report_${System.currentTimeMillis()}.pdf")

        val bengaliBaseFont = runCatching {
            ResourcesCompat.getFont(context, R.font.hind_siliguri)
        }.getOrNull() ?: Typeface.DEFAULT
        val bengaliBoldFont = Typeface.create(bengaliBaseFont, Typeface.BOLD)

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.rgb(8, 91, 75)
            textSize = 20f
            typeface = bengaliBoldFont
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = bengaliBaseFont
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = bengaliBoldFont
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 40, 38)
            textSize = 10.5f
            typeface = bengaliBaseFont
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas
        var y = 52f

        canvas.drawText("হিসাব খাতা - আর্থিক বিবরণী", margin, y, titlePaint)
        y += 22f
        canvas.drawText("সময়কাল: $periodTitle", margin, y, subtitlePaint)
        y += 18f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 22f

        canvas.drawText("মোট আয়: ${Formatters.formatTaka(totalIncome)}", margin, y, headerPaint)
        canvas.drawText("মোট ব্যয়: ${Formatters.formatTaka(totalExpense)}", margin + 175f, y, headerPaint)
        canvas.drawText("ব্যালেন্স: ${Formatters.formatTaka(totalIncome - totalExpense)}", margin + 350f, y, headerPaint)
        y += 20f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 22f

        canvas.drawText("তারিখ", margin, y, headerPaint)
        canvas.drawText("ক্যাটাগরি ও মেথড", margin + 95f, y, headerPaint)
        canvas.drawText("ধরন", margin + 310f, y, headerPaint)
        canvas.drawText("পরিমাণ", margin + 410f, y, headerPaint)
        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 18f

        transactions.forEach { tx ->
            if (y > pageHeight - 60f) {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 50f
            }
            val dateStr = Formatters.formatDate(tx.dateEpochMillis)
            val catPm = "${tx.categoryName} (${tx.paymentMethodName})".take(36)
            val typeStr = tx.parsedType.labelBn
            val amtStr = Formatters.formatTaka(tx.amount)

            canvas.drawText(dateStr, margin, y, bodyPaint)
            canvas.drawText(catPm, margin + 95f, y, bodyPaint)
            canvas.drawText(typeStr, margin + 310f, y, bodyPaint)
            canvas.drawText(amtStr, margin + 410f, y, bodyPaint)
            y += 16f
            if (tx.note.isNotBlank()) {
                canvas.drawText("নোট: ${tx.note.take(65)}", margin + 95f, y, subtitlePaint)
                y += 15f
            }
        }

        pdf.finishPage(page)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()

        shareFile(context, file, "application/pdf", "PDF রিপোর্ট শেয়ার করুন")
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
