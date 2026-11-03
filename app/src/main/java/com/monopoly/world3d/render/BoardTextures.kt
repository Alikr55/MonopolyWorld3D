package com.monopoly.world3d.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import com.monopoly.world3d.engine.Board
import com.monopoly.world3d.engine.Group
import com.monopoly.world3d.engine.Space
import com.monopoly.world3d.engine.SpaceType
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * تولید همه‌ی بافت‌های بازی با Canvas — شامل متن فارسی روی خانه‌ها،
 * چهره‌های تاس، چوب میز، لوگوی مرکزی و سایه‌ی نرم.
 */
class BoardTextures(context: Context, quality: Int) {

    companion object {
        const val QUALITY_LOW = 0
        const val QUALITY_MEDIUM = 1
        const val QUALITY_HIGH = 2

        fun tileSize(quality: Int) = when (quality) {
            QUALITY_LOW -> 96
            QUALITY_MEDIUM -> 160
            else -> 256
        }
    }

    val typeface: Typeface = try {
        Typeface.createFromAsset(context.assets, "fonts/Vazirmatn-Regular.ttf")
    } catch (e: Exception) {
        try {
            Typeface.createFromAsset(context.assets, "fonts/Vazirmatn-Bold.ttf")
        } catch (e2: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    val tileTexSize = tileSize(quality)

    private fun newBitmap(w: Int, h: Int = w): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

    // ---------------- بافت خانه‌ها ----------------

    /** بافت یک خانه: نوار رنگ گروه + نام فارسی */
    fun tileTexture(space: Space): Bitmap {
        val w = tileTexSize
        val h = w * 2
        val bmp = newBitmap(w, h)
        val c = Canvas(bmp)
        val rot = BoardLayout.tileRotation(space.id)
        c.save()
        c.rotate(rot, w / 2f, h / 2f)
        // پس‌زمینه کرم
        val bg = Paint().apply {
            color = Color.rgb(0xF3, 0xEA, 0xD3)
            style = Paint.Style.FILL
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        // نوار رنگ گروه در سمت داخلی (بالای بافت)
        val bandHeight = h * 0.28f
        val groupColor = space.group?.let { Color.rgb((it.colorHex shr 16 and 0xFF).toInt(), (it.colorHex shr 8 and 0xFF).toInt(), (it.colorHex and 0xFF).toInt()) }
            ?: Color.rgb(0xE0, 0xD5, 0xB8)
        c.drawRect(0f, 0f, w.toFloat(), bandHeight, Paint().apply { color = groupColor })
        // متن
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2B, 0x22, 0x14)
            typeface = this@BoardTextures.typeface
            textAlign = Paint.Align.CENTER
        }
        val name = space.name
        val isCorner = BoardLayout.isCorner(space.id)
        if (isCorner) {
            textPaint.textSize = w * 0.16f
            // متن گوشه‌ها در مرکز
            val lines = wrapText(c, name, textPaint, w * 0.9f)
            var y = h * 0.35f
            for (line in lines) {
                c.drawText(line, w / 2f, y, textPaint)
                y += textPaint.textSize * 1.25f
            }
        } else if (space.type == SpaceType.STREET || space.type == SpaceType.RAILROAD || space.type == SpaceType.UTILITY) {
            textPaint.textSize = w * 0.13f
            val lines = wrapText(c, name, textPaint, w * 0.88f)
            var y = bandHeight + h * 0.09f
            for (line in lines) {
                c.drawText(line, w / 2f, y, textPaint)
                y += textPaint.textSize * 1.2f
            }
            // قیمت
            if (space.price > 0) {
                val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(0x50, 0x3A, 0x18)
                    typeface = this@BoardTextures.typeface
                    textAlign = Paint.Align.CENTER
                    textSize = w * 0.12f
                }
                c.drawText("تومان", w / 2f, h - w * 0.28f, pricePaint)
                c.drawText(
                    com.monopoly.world3d.engine.PersianNumbers.toPersianDigits(
                        com.monopoly.world3d.engine.PersianNumbers.groupThousands(space.price)
                    ),
                    w / 2f, h - w * 0.08f, pricePaint
                )
            }
        } else {
            // خانه‌های ویژه: شانس/اتفاقات/مالیات/شروع/زندان...
            textPaint.textSize = w * 0.14f
            val lines = wrapText(c, name, textPaint, w * 0.85f)
            var y = h * 0.42f
            for (line in lines) {
                c.drawText(line, w / 2f, y, textPaint)
                y += textPaint.textSize * 1.3f
            }
            // آیکون ساده
            drawSpaceIcon(c, space, w, h)
        }
        c.restore()
        return bmp
    }

    private fun drawSpaceIcon(c: Canvas, space: Space, w: Int, h: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = w / 2f
        val cy = h * 0.68f
        val s = w * 0.18f
        when (space.type) {
            SpaceType.CHANCE -> { // علامت سوال
                p.color = Color.rgb(0xE8, 0x5D, 0x75)
                c.drawCircle(cx, cy, s, p)
                p.color = Color.WHITE
                p.textSize = s * 1.6f
                p.textAlign = Paint.Align.CENTER
                p.typeface = typeface
                c.drawText("؟", cx, cy + s * 0.55f, p)
            }
            SpaceType.CHEST -> { // صندوق
                p.color = Color.rgb(0x3D, 0x8B, 0x6E)
                c.drawRoundRect(cx - s, cy - s * 0.7f, cx + s, cy + s * 0.7f, s * 0.2f, s * 0.2f, p)
                p.color = Color.rgb(0xF3, 0xEA, 0xD3)
                c.drawRect(cx - s, cy - s * 0.1f, cx + s, cy + s * 0.15f, p)
            }
            SpaceType.TAX -> {
                p.color = Color.rgb(0x8E, 0x5B, 0x3A)
                c.drawCircle(cx, cy, s * 0.85f, p)
                p.color = Color.WHITE
                p.textSize = s * 1.1f
                p.textAlign = Paint.Align.CENTER
                p.typeface = typeface
                c.drawText("٪", cx, cy + s * 0.4f, p)
            }
            SpaceType.GO -> { // فلش قرمز
                p.color = Color.rgb(0xD8, 0x3A, 0x2B)
                val path = Path()
                path.moveTo(cx - s, cy - s * 0.6f)
                path.lineTo(cx + s * 0.9f, cy)
                path.lineTo(cx - s, cy + s * 0.6f)
                path.close()
                c.drawPath(path, p)
            }
            SpaceType.JAIL -> {
                p.color = Color.rgb(0x5A, 0x5A, 0x5A)
                for (i in -1..1) c.drawRect(cx + i * s * 0.55f - s * 0.08f, cy - s, cx + i * s * 0.55f + s * 0.08f, cy + s, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.12f
                c.drawRect(cx - s, cy - s, cx + s, cy + s, p)
            }
            SpaceType.FREE_PARKING -> {
                p.color = Color.rgb(0x4E, 0x7A, 0xC9)
                c.drawCircle(cx, cy, s * 0.9f, p)
                p.color = Color.rgb(0xF3, 0xEA, 0xD3)
                c.drawCircle(cx, cy, s * 0.5f, p)
            }
            SpaceType.GO_TO_JAIL -> {
                p.color = Color.rgb(0xB0, 0x3A, 0x2B)
                val path = Path()
                path.moveTo(cx + s, cy - s * 0.6f)
                path.lineTo(cx - s * 0.9f, cy)
                path.lineTo(cx + s, cy + s * 0.6f)
                path.close()
                c.drawPath(path, p)
            }
            else -> {}
        }
    }

    private fun wrapText(c: Canvas, text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        for (word in words) {
            val test = if (cur.isEmpty()) word else cur.toString() + " " + word
            if (paint.measureText(test) > maxWidth && cur.isNotEmpty()) {
                lines.add(cur.toString())
                cur = StringBuilder(word)
            } else {
                cur = StringBuilder(test)
            }
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return lines.take(3)
    }

    // ---------------- چوب میز ----------------

    fun tableTexture(): Bitmap {
        val size = 512
        val bmp = newBitmap(size)
        val c = Canvas(bmp)
        val paint = Paint()
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(),
            Color.rgb(0x8B, 0x5A, 0x2B), Color.rgb(0x6E, 0x44, 0x1E), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        val rnd = Random(42)
        paint.shader = null
        for (i in 0 until 220) {
            val y = rnd.nextInt(size)
            val alpha = 14 + rnd.nextInt(20)
            paint.color = Color.argb(alpha, if (rnd.nextBoolean()) 0x50 else 0xC8, 0x3A, if (rnd.nextBoolean()) 0x18 else 0x58)
            paint.strokeWidth = 1.5f + rnd.nextInt(3)
            c.drawLine(0f, y.toFloat(), size.toFloat(), y + rnd.nextInt(14) - 7f, paint)
        }
        return bmp
    }

    // ---------------- مرکز صفحه ----------------

    fun centerTexture(): Bitmap {
        val size = 512
        val bmp = newBitmap(size)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(0xF3, 0xEA, 0xD3))
        // قاب
        val frame = Paint().apply {
            color = Color.rgb(0x9C, 0x7A, 0x48)
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        c.drawRect(20f, 20f, size - 20f, size - 20f, frame)
        // لوگو
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xC0, 0x39, 0x2B)
            typeface = this@BoardTextures.typeface
            textAlign = Paint.Align.CENTER
            textSize = size * 0.14f
        }
        c.drawText("دنیای روپولی", size / 2f, size * 0.32f, titlePaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x2B, 0x22, 0x14)
            typeface = this@BoardTextures.typeface
            textAlign = Paint.Align.CENTER
            textSize = size * 0.07f
        }
        c.drawText("MONOPOLY WORLD 3D", size / 2f, size * 0.42f, subPaint)
        // دو دسته کارت
        drawDeck(c, size * 0.18f, size * 0.52f, size * 0.34f, size * 0.24f, Color.rgb(0x2E, 0x86, 0xC1), "شانس")
        drawDeck(c, size * 0.48f, size * 0.52f, size * 0.34f, size * 0.24f, Color.rgb(0xE8, 0x7D, 0x3C), "اتفاقات عمومی")
        // مربع راهنمای رنگ
        val areaPaint = Paint().apply {
            color = Color.argb(28, 0, 0, 0)
        }
        c.drawRect(size * 0.1f, size * 0.86f, size * 0.9f, size * 0.93f, areaPaint)
        val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x60, 0x4C, 0x2A)
            typeface = this@BoardTextures.typeface
            textAlign = Paint.Align.CENTER
            textSize = size * 0.045f
        }
        c.drawText("با دو انگشت بچرخانید و بزرگ‌نمایی کنید", size / 2f, size * 0.915f, hint)
        return bmp
    }

    private fun drawDeck(c: Canvas, x: Float, y: Float, w: Float, h: Float, deckColor: Int, label: String) {
        val shadow = Paint()
        shadow.color = Color.argb(70, 0, 0, 0)
        c.drawRoundRect(x + 6f, y + 8f, x + w + 6f, y + h + 8f, 12f, 12f, shadow)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = deckColor
        c.drawRoundRect(x, y, x + w, y + h, 12f, 12f, p)
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            typeface = this@BoardTextures.typeface
            textAlign = Paint.Align.CENTER
            textSize = h * 0.3f
        }
        c.drawText(label, x + w / 2f, y + h / 2f + h * 0.1f, t)
    }

    // ---------------- چهره‌های تاس (اطلس ۳×۲) ----------------

    fun diceAtlas(): Bitmap {
        val cell = 128
        val bmp = newBitmap(cell * 3, cell * 2)
        val c = Canvas(bmp)
        for (face in 1..6) {
            val col = (face - 1) % 3
            val row = (face - 1) / 3
            drawDiceFace(c, (col * cell).toFloat(), (row * cell).toFloat(), cell, face)
        }
        return bmp
    }

    private fun drawDiceFace(c: Canvas, ox: Float, oy: Float, size: Int, face: Int) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0xFA, 0xF6, 0xEC)
        }
        c.drawRect(ox, oy, ox + size, oy + size, bg)
        // حاشیه گرد
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        c.drawRect(ox + 3f, oy + 3f, ox + size - 3f, oy + size - 3f, border)
        val pip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x1B, 0x1B, 0x1B) }
        val m = size * 0.24f
        val pts = when (face) {
            1 -> listOf(0.5f to 0.5f)
            2 -> listOf(0.25f to 0.25f, 0.75f to 0.75f)
            3 -> listOf(0.25f to 0.25f, 0.5f to 0.5f, 0.75f to 0.75f)
            4 -> listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.25f to 0.75f, 0.75f to 0.75f)
            5 -> listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.5f to 0.5f, 0.25f to 0.75f, 0.75f to 0.75f)
            else -> listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.25f to 0.5f, 0.75f to 0.5f, 0.25f to 0.75f, 0.75f to 0.75f)
        }
        val r = size * 0.085f
        for ((fx, fy) in pts) {
            c.drawCircle(ox + size * fx, oy + size * fy, r, pip)
        }
    }

    // ---------------- سایه‌ی نرم ----------------

    fun blobShadow(): Bitmap {
        val size = 128
        val bmp = newBitmap(size)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            size / 2f, size / 2f, size / 2f,
            intArrayOf(Color.argb(120, 0, 0, 0), Color.argb(40, 0, 0, 0), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bmp
    }

    // ---------------- شهر منو ----------------

    fun cityWindowTexture(): Bitmap {
        val size = 128
        val bmp = newBitmap(size)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(0x2C, 0x3E, 0x60))
        val lit = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(7)
        val cols = 4
        val rows = 6
        val cw = size / cols
        val ch = size / rows
        for (i in 0 until cols) for (j in 0 until rows) {
            if (rnd.nextDouble() < 0.45) {
                lit.color = when {
                    rnd.nextDouble() < 0.5 -> Color.argb(255, 0xFF, 0xE0, 0x82)
                    else -> Color.argb(255, 0x90, 0xCA, 0xF9)
                }
                c.drawRect(i * cw + cw * 0.2f, j * ch + ch * 0.18f, (i + 1) * cw - cw * 0.2f, (j + 1) * ch - ch * 0.18f, lit)
            }
        }
        return bmp
    }

    fun buildingSideTexture(baseColor: Int): Bitmap {
        val size = 128
        val bmp = newBitmap(size)
        val c = Canvas(bmp)
        c.drawColor(baseColor)
        val win = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(230, 0x2C, 0x3E, 0x60) }
        val cols = 4; val rows = 5
        val cw = size / cols; val ch = size / rows
        for (i in 0 until cols) for (j in 0 until rows) {
            c.drawRect(i * cw + cw * 0.22f, j * ch + ch * 0.2f, (i + 1) * cw - cw * 0.22f, (j + 1) * ch - ch * 0.24f, win)
        }
        return bmp
    }

    // ---------------- بارگذاری GL ----------------

    fun upload(bmp: Bitmap, mipmap: Boolean = false): Int {
        val handles = IntArray(1)
        GLES20.glGenTextures(1, handles, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handles[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            if (mipmap) GLES20.GL_LINEAR_MIPMAP_LINEAR else GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        if (mipmap) GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bmp.recycle()
        return handles[0]
    }
}
