package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CommentItem
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun formatCommentsAsText(comments: List<CommentItem>, sessionTitle: String): String {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("📌 $sessionTitle\n")
        val commentsCount = comments.count { !it.isReply }
        val repliesCount = comments.count { it.isReply }
        sb.append("📊 إجمالي التعليقات: $commentsCount | إجمالي الردود: $repliesCount\n")
        sb.append("========================================\n\n")

        // Group into root comments and their replies
        var commentIndex = 1
        for (item in comments) {
            if (!itIsReply(item)) {
                sb.append("[$commentIndex] ${item.author}:\n")
                sb.append("${item.content}\n\n")
                commentIndex++
            } else {
                sb.append("   ↳ [رد] ${item.author}:\n")
                sb.append("     ${item.content}\n\n")
            }
        }

        return sb.toString()
    }

    private fun itIsReply(item: CommentItem): Boolean = item.isReply

    fun copyToClipboard(context: Context, comments: List<CommentItem>, sessionTitle: String) {
        val text = formatCommentsAsText(comments, sessionTitle)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Facebook Comments", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ جميع التعليقات والردود إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    fun exportToTxt(context: Context, comments: List<CommentItem>, sessionTitle: String): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportsDir, "comments_$timestamp.txt")

        val text = formatCommentsAsText(comments, sessionTitle)
        file.writeText(text, Charsets.UTF_8)
        return file
    }

    fun exportToCsv(context: Context, comments: List<CommentItem>, sessionTitle: String): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportsDir, "comments_$timestamp.csv")

        FileWriter(file, Charsets.UTF_8).use { writer ->
            // UTF-8 BOM for Excel Arabic compatibility
            writer.write("\uFEFF")
            writer.write("النوع,اسم الكاتب,نص التعليق,الكاتب الأصلي,التاريخ\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (item in comments) {
                val type = if (item.isReply) "رد" else "تعليق"
                val author = escapeCsv(item.author)
                val content = escapeCsv(item.content)
                val parentAuthor = escapeCsv(item.parentAuthor)
                val date = dateFormat.format(Date(item.timestamp))

                writer.write("$type,$author,$content,$parentAuthor,$date\n")
            }
        }

        return file
    }

    private fun escapeCsv(value: String): String {
        var res = value.replace("\"", "\"\"")
        if (res.contains(",") || res.contains("\"") || res.contains("\n") || res.contains("\r")) {
            res = "\"$res\""
        }
        return res
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "مشاركة الملف") {
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

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
