package com.monopoly.world3d.render

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * سازنده‌ی هندسه‌ی رویه‌ای — همه‌ی مدل‌های سه‌بعدی بازی با کد ساخته می‌شوند
 * تا حجم APK حداقلی بماند و نیازی به فایل مدل نباشد.
 */
class MeshBuilder {
    private val data = ArrayList<Float>(2048)

    val vertexCount: Int get() = data.size / STRIDE

    private fun vertex(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, u: Float, v: Float) {
        data.add(px); data.add(py); data.add(pz)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b)
        data.add(u); data.add(v)
    }

    /** رأس سفارشی با UV دلخواه (برای اطلس‌ها) */
    fun addVertex(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, u: Float, v: Float) =
        vertex(px, py, pz, nx, ny, nz, r, g, b, u, v)

    /** چهارضلعی با دو مثلث */
    fun quad(
        p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        vertex(p1[0], p1[1], p1[2], nx, ny, nz, r, g, b, 0f, 0f)
        vertex(p2[0], p2[1], p2[2], nx, ny, nz, r, g, b, 1f, 0f)
        vertex(p3[0], p3[1], p3[2], nx, ny, nz, r, g, b, 1f, 1f)
        vertex(p1[0], p1[1], p1[2], nx, ny, nz, r, g, b, 0f, 0f)
        vertex(p3[0], p3[1], p3[2], nx, ny, nz, r, g, b, 1f, 1f)
        vertex(p4[0], p4[1], p4[2], nx, ny, nz, r, g, b, 0f, 1f)
    }

    /** چهارضلعی افقی (y ثابت) با UV کامل */
    fun groundQuad(x1: Float, z1: Float, x2: Float, z2: Float, y: Float, r: Float, g: Float, b: Float) {
        val p1 = floatArrayOf(x1, y, z1); val p2 = floatArrayOf(x2, y, z1)
        val p3 = floatArrayOf(x2, y, z2); val p4 = floatArrayOf(x1, y, z2)
        vertex(p1[0], p1[1], p1[2], 0f, 1f, 0f, r, g, b, 0f, 0f)
        vertex(p2[0], p2[1], p2[2], 0f, 1f, 0f, r, g, b, 1f, 0f)
        vertex(p3[0], p3[1], p3[2], 0f, 1f, 0f, r, g, b, 1f, 1f)
        vertex(p1[0], p1[1], p1[2], 0f, 1f, 0f, r, g, b, 0f, 0f)
        vertex(p3[0], p3[1], p3[2], 0f, 1f, 0f, r, g, b, 1f, 1f)
        vertex(p4[0], p4[1], p4[2], 0f, 1f, 0f, r, g, b, 0f, 1f)
    }

    /** جعبه با مرکز و ابعاد — با چرخش اختیاری حول Y */
    fun box(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, yawDeg: Float = 0f) {
        val hx = sx / 2; val hy = sy / 2; val hz = sz / 2
        val yaw = Math.toRadians(yawDeg.toDouble())
        val c = cos(yaw).toFloat(); val s = sin(yaw).toFloat()
        fun tp(x: Float, y: Float, z: Float): FloatArray {
            val rx = x * c + z * s
            val rz = -x * s + z * c
            return floatArrayOf(cx + rx, cy + y, cz + rz)
        }
        val nn = fun(nx0: Float, ny0: Float, nz0: Float): FloatArray {
            val rx = nx0 * c + nz0 * s
            val rz = -nx0 * s + nz0 * c
            return floatArrayOf(rx, ny0, rz)
        }
        // بالا و پایین
        val top = nn(0f, 1f, 0f)
        quad(tp(-hx, hy, -hz), tp(hx, hy, -hz), tp(hx, hy, hz), tp(-hx, hy, hz), top[0], top[1], top[2], r, g, b)
        val nb = nn(0f, -1f, 0f)
        quad(tp(-hx, -hy, hz), tp(hx, -hy, hz), tp(hx, -hy, -hz), tp(-hx, -hy, -hz), nb[0], nb[1], nb[2], r, g, b)
        // جلو (z+)
        val nf = nn(0f, 0f, 1f)
        quad(tp(-hx, -hy, hz), tp(hx, -hy, hz), tp(hx, hy, hz), tp(-hx, hy, hz), nf[0], nf[1], nf[2], r, g, b)
        // عقب
        val nbk = nn(0f, 0f, -1f)
        quad(tp(hx, -hy, -hz), tp(-hx, -hy, -hz), tp(-hx, hy, -hz), tp(hx, hy, -hz), nbk[0], nbk[1], nbk[2], r, g, b)
        // راست (x+)
        val nr = nn(1f, 0f, 0f)
        quad(tp(hx, -hy, hz), tp(hx, -hy, -hz), tp(hx, hy, -hz), tp(hx, hy, hz), nr[0], nr[1], nr[2], r, g, b)
        // چپ
        val nl = nn(-1f, 0f, 0f)
        quad(tp(-hx, -hy, -hz), tp(-hx, -hy, hz), tp(-hx, hy, hz), tp(-hx, hy, -hz), nl[0], nl[1], nl[2], r, g, b)
    }

    /** استوانه با محور Y */
    fun cylinder(cx: Float, cy: Float, cz: Float, radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float) {
        val y0 = cy; val y1 = cy + height
        for (i in 0 until segments) {
            val a0 = (i * 2 * Math.PI / segments)
            val a1 = ((i + 1) * 2 * Math.PI / segments)
            val x0 = cx + radius * cos(a0).toFloat(); val z0 = cz + radius * sin(a0).toFloat()
            val x1 = cx + radius * cos(a1).toFloat(); val z1 = cz + radius * sin(a1).toFloat()
            val nx0 = cos(a0).toFloat(); val nz0 = sin(a0).toFloat()
            val nx1 = cos(a1).toFloat(); val nz1 = sin(a1).toFloat()
            // دیواره
            vertex(x0, y0, z0, nx0, 0f, nz0, r, g, b, 0f, 0f)
            vertex(x1, y0, z1, nx1, 0f, nz1, r, g, b, 1f, 0f)
            vertex(x1, y1, z1, nx1, 0f, nz1, r, g, b, 1f, 1f)
            vertex(x0, y0, z0, nx0, 0f, nz0, r, g, b, 0f, 0f)
            vertex(x1, y1, z1, nx1, 0f, nz1, r, g, b, 1f, 1f)
            vertex(x0, y1, z0, nx0, 0f, nz0, r, g, b, 0f, 1f)
            // درپوش بالا و پایین
            vertex(cx, y1, cz, 0f, 1f, 0f, r, g, b, 0f, 0f)
            vertex(x1, y1, z1, 0f, 1f, 0f, r, g, b, 0f, 0f)
            vertex(x0, y1, z0, 0f, 1f, 0f, r, g, b, 0f, 0f)
            vertex(cx, y0, cz, 0f, -1f, 0f, r, g, b, 0f, 0f)
            vertex(x0, y0, z0, 0f, -1f, 0f, r, g, b, 0f, 0f)
            vertex(x1, y0, z1, 0f, -1f, 0f, r, g, b, 0f, 0f)
        }
    }

    /** مخروط با محور Y */
    fun cone(cx: Float, cy: Float, cz: Float, radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float) {
        val y1 = cy + height
        for (i in 0 until segments) {
            val a0 = (i * 2 * Math.PI / segments)
            val a1 = ((i + 1) * 2 * Math.PI / segments)
            val x0 = cx + radius * cos(a0).toFloat(); val z0 = cz + radius * sin(a0).toFloat()
            val x1 = cx + radius * cos(a1).toFloat(); val z1 = cz + radius * sin(a1).toFloat()
            val mx = cos((a0 + a1) / 2).toFloat(); val mz = sin((a0 + a1) / 2).toFloat()
            val ny = radius / height
            vertex(x0, cy, z0, mx, ny, mz, r, g, b, 0f, 0f)
            vertex(x1, cy, z1, mx, ny, mz, r, g, b, 1f, 0f)
            vertex(cx, y1, cz, mx, ny, mz, r, g, b, 0.5f, 1f)
        }
    }

    /** هرم چهارضلعی (سقف خانه) */
    fun pyramid(cx: Float, cy: Float, cz: Float, sx: Float, sz: Float, h: Float, r: Float, g: Float, b: Float, yawDeg: Float = 0f) {
        val hx = sx / 2; val hz = sz / 2
        val yaw = Math.toRadians(yawDeg.toDouble())
        val c = cos(yaw).toFloat(); val s = sin(yaw).toFloat()
        fun tp(x: Float, z: Float): FloatArray = floatArrayOf(cx + x * c + z * s, cy, cz - x * s + z * c)
        val b0 = tp(-hx, -hz); val b1 = tp(hx, -hz); val b2 = tp(hx, hz); val b3 = tp(-hx, hz)
        val apex = floatArrayOf(cx, cy + h, cz)
        fun tri(p0: FloatArray, p1: FloatArray, p2: FloatArray) {
            val u = floatArrayOf(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2])
            val v = floatArrayOf(p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2])
            var nx = u[1] * v[2] - u[2] * v[1]
            var ny = u[2] * v[0] - u[0] * v[2]
            var nz = u[0] * v[1] - u[1] * v[0]
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            nx /= len; ny /= len; nz /= len
            if (ny < 0) { nx = -nx; ny = -ny; nz = -nz }
            vertex(p0[0], p0[1], p0[2], nx, ny, nz, r, g, b, 0f, 0f)
            vertex(p1[0], p1[1], p1[2], nx, ny, nz, r, g, b, 1f, 0f)
            vertex(p2[0], p2[1], p2[2], nx, ny, nz, r, g, b, 1f, 1f)
        }
        tri(b0, b1, apex); tri(b1, b2, apex); tri(b2, b3, apex); tri(b3, b0, apex)
    }

    /** هشت‌وجهی (الماس) */
    fun octahedron(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        val hx = sx / 2; val hy = sy / 2; val hz = sz / 2
        val top = floatArrayOf(cx, cy + hy, cz)
        val bot = floatArrayOf(cx, cy - hy, cz)
        val v = arrayOf(
            floatArrayOf(cx - hx, cy, cz), floatArrayOf(cx, cy, cz - hz),
            floatArrayOf(cx + hx, cy, cz), floatArrayOf(cx, cy, cz + hz)
        )
        fun tri(p0: FloatArray, p1: FloatArray, p2: FloatArray) {
            val u = floatArrayOf(p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2])
            val w = floatArrayOf(p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2])
            var nx = u[1] * w[2] - u[2] * w[1]
            var ny = u[2] * w[0] - u[0] * w[2]
            var nz = u[0] * w[1] - u[1] * w[0]
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            nx /= len; ny /= len; nz /= len
            vertex(p0[0], p0[1], p0[2], nx, ny, nz, r, g, b, 0f, 0f)
            vertex(p1[0], p1[1], p1[2], nx, ny, nz, r, g, b, 1f, 0f)
            vertex(p2[0], p2[1], p2[2], nx, ny, nz, r, g, b, 1f, 1f)
        }
        for (i in 0..3) {
            val a = v[i]; val b2 = v[(i + 1) % 4]
            tri(a, b2, top)
            tri(b2, a, bot)
        }
    }

    /** کره‌ی ساده‌ی کم‌وجه */
    fun sphere(cx: Float, cy: Float, cz: Float, radius: Float, rows: Int, cols: Int, r: Float, g: Float, b: Float) {
        for (i in 0 until rows) {
            val th0 = Math.PI * i / rows; val th1 = Math.PI * (i + 1) / rows
            for (j in 0 until cols) {
                val ph0 = 2 * Math.PI * j / cols; val ph1 = 2 * Math.PI * (j + 1) / cols
                fun p(th: Double, ph: Double): FloatArray = floatArrayOf(
                    cx + radius * kotlin.math.sin(th).toFloat() * kotlin.math.cos(ph).toFloat(),
                    cy + radius * kotlin.math.cos(th).toFloat(),
                    cz + radius * kotlin.math.sin(th).toFloat() * kotlin.math.sin(ph).toFloat()
                )
                val a = p(th0, ph0); val b2 = p(th0, ph1); val c2 = p(th1, ph1); val d = p(th1, ph0)
                val ny = kotlin.math.cos((th0 + th1) / 2).toFloat()
                quad(d, c2, b2, a, (a[0] - cx), ny, (a[2] - cz), r, g, b)
            }
        }
    }

    fun buildArray(): FloatArray = data.toFloatArray()

    companion object {
        const val STRIDE = 11 // pos3 + normal3 + color3 + uv2
        const val FLOATS_PER_VERTEX = STRIDE
    }
}

/** مش آماده‌ی رندر با VBO */
class GlMesh(vertices: FloatArray) {
    private val buffer: FloatBuffer
    val vertexCount: Int = vertices.size / MeshBuilder.STRIDE

    init {
        buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(vertices)
        buffer.position(0)
    }

    fun draw(program: Int, aPos: Int, aNormal: Int, aColor: Int, aUV: Int) {
        buffer.position(0)
        GLES20Bridge.glVertexAttribPointer(aPos, 3, buffer)
        buffer.position(3)
        GLES20Bridge.glVertexAttribPointer(aNormal, 3, buffer)
        buffer.position(6)
        GLES20Bridge.glVertexAttribPointer(aColor, 3, buffer)
        buffer.position(9)
        GLES20Bridge.glVertexAttribPointer(aUV, 2, buffer)
        GLES20Bridge.glDrawArrays(vertexCount)
    }
}

/** پل به GLES20 برای جداسازی تمیز */
object GLES20Bridge {
    fun glVertexAttribPointer(index: Int, size: Int, buffer: FloatBuffer) {
        buffer.position(buffer.position()) // موقعیت حفظ می‌شود
        android.opengl.GLES20.glVertexAttribPointer(
            index, size, android.opengl.GLES20.GL_FLOAT, false, MeshBuilder.STRIDE * 4, buffer
        )
        android.opengl.GLES20.glEnableVertexAttribArray(index)
    }

    fun glDrawArrays(vertexCount: Int) {
        android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLES, 0, vertexCount)
    }
}
