package com.memento.app.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ShareCardContent(
    val title: String,
    val subtitle: String,
    val body: String,
    val imageUrl: String? = null,
    val attribution: String = "Memento",
)

class ShareCardRenderer(private val context: Context) {
    suspend fun render(content: ShareCardContent, fileName: String): Uri = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas, loadBitmap(content.imageUrl))
        drawText(canvas, content)

        val directory = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val safeName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "-")
        val file = File(directory, "$safeName.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun drawBackground(canvas: Canvas, image: Bitmap?) {
        canvas.drawColor(Color.rgb(43, 35, 38))
        image?.let { source ->
            val scale = maxOf(WIDTH.toFloat() / source.width, HEIGHT.toFloat() / source.height)
            val cropWidth = (WIDTH / scale).toInt()
            val cropHeight = (HEIGHT / scale).toInt()
            val left = ((source.width - cropWidth) / 2).coerceAtLeast(0)
            val top = ((source.height - cropHeight) / 2).coerceAtLeast(0)
            canvas.drawBitmap(source, Rect(left, top, left + cropWidth, top + cropHeight), Rect(0, 0, WIDTH, HEIGHT), null)
            source.recycle()
        }
        val overlay = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                HEIGHT.toFloat(),
                intArrayOf(Color.argb(45, 0, 0, 0), Color.argb(115, 12, 8, 10), Color.argb(245, 20, 14, 17)),
                floatArrayOf(0f, 0.46f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), overlay)
    }

    private fun drawText(canvas: Canvas, content: ShareCardContent) {
        val white = Color.WHITE
        val muted = Color.argb(220, 255, 255, 255)
        val margin = 92f
        val maxWidth = WIDTH - margin * 2
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = 76f; isFakeBoldText = true }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 38f }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; textSize = 52f }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 28f; letterSpacing = 0.08f }

        var y = 980f
        y = canvas.drawWrapped(content.title, margin, y, maxWidth, titlePaint, 1.1f, maxLines = 3)
        y += 34f
        y = canvas.drawWrapped(content.subtitle, margin, y, maxWidth, subtitlePaint, 1.25f, maxLines = 2)
        y += 70f
        canvas.drawWrapped("“${content.body}”", margin, y, maxWidth, bodyPaint, 1.3f, maxLines = 8)
        canvas.drawText(content.attribution, margin, HEIGHT - 74f, brandPaint)
    }

    private fun loadBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = true
            connection.inputStream.use(BitmapFactory::decodeStream).also { connection.disconnect() }
        }.getOrNull()
    }

    companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1920
    }
}

private fun Canvas.drawWrapped(
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    paint: Paint,
    lineSpacing: Float,
    maxLines: Int,
): Float {
    val words = text.trim().split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) current = candidate
        else {
            if (current.isNotEmpty()) lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    val visible = lines.take(maxLines).toMutableList()
    if (lines.size > maxLines && visible.isNotEmpty()) visible[visible.lastIndex] = visible.last().trimEnd('…') + "…"
    var y = startY
    visible.forEach { line ->
        drawText(line, x, y, paint)
        y += paint.textSize * lineSpacing
    }
    return y
}

fun sharePng(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "Memento", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(com.memento.app.R.string.share)))
}
