package com.monopoly.world3d.render

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * دوربین مداری لمسی: چرخش با کشیدن، بزرگ‌نمایی با دو انگشت، دنبال‌کردن نرم هدف.
 */
class Camera3D {

    var azimuth = 0f          // درجه — 0 = نگاه از سمت بازیکن (جنوب)
    var polar = 52f           // درجه — زاویه از افق
    var distance = 15f
    var minDistance = 8f
    var maxDistance = 22f
    var minPolar = 28f
    var maxPolar = 78f

    private val target = floatArrayOf(0f, 0f, 0f)
    private val desiredTarget = floatArrayOf(0f, 0f, 0f)

    val viewMatrix = FloatArray(16)
    val projMatrix = FloatArray(16)
    val viewProjMatrix = FloatArray(16)
    private val tmpModel = FloatArray(16)
    private val tmpNormal = FloatArray(16)

    var aspect = 1f

    fun rotateBy(dx: Float, dy: Float) {
        azimuth += dx * 0.25f
        polar = (polar - dy * 0.2f).coerceIn(minPolar, maxPolar)
        azimuth = ((azimuth + 180) % 360 + 360) % 360 - 180
    }

    fun zoomBy(scale: Float) {
        distance = (distance / scale).coerceIn(minDistance, maxDistance)
    }

    fun followTo(x: Float, y: Float, z: Float, strength: Float = 0.12f) {
        desiredTarget[0] = x * 0.35f
        desiredTarget[1] = y
        desiredTarget[2] = z * 0.35f
        this.strength = strength
    }

    fun resetTarget() {
        desiredTarget[0] = 0f; desiredTarget[1] = 0f; desiredTarget[2] = 0f
    }

    private var strength = 0.12f

    fun update() {
        for (i in 0..2) target[i] += (desiredTarget[i] - target[i]) * strength
        val eye = eyePosition()
        Matrix.setLookAtM(viewMatrix, 0,
            eye[0], eye[1], eye[2],
            target[0], target[1], target[2],
            0f, 1f, 0f)
        Matrix.perspectiveM(projMatrix, 0, 42f, aspect, 0.5f, 120f)
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    fun eyePosition(): FloatArray {
        val az = Math.toRadians(azimuth.toDouble())
        val po = Math.toRadians(polar.toDouble())
        val x = target[0] + distance * cos(po).toFloat() * sin(az).toFloat()
        val y = target[1] + distance * sin(po).toFloat()
        val z = target[2] + distance * cos(po).toFloat() * cos(az).toFloat()
        return floatArrayOf(x, y, z)
    }

    /** ماتریس مدل و نرمال برای رندر یک جسم */
    fun computeModelMatrices(outModel: FloatArray, outNormal: FloatArray,
                            x: Float, y: Float, z: Float, rotYDeg: Float = 0f, scale: Float = 1f) {
        Matrix.setIdentityM(outModel, 0)
        Matrix.translateM(outModel, 0, x, y, z)
        if (rotYDeg != 0f) Matrix.rotateM(outModel, 0, rotYDeg, 0f, 1f, 0f)
        if (scale != 1f) Matrix.scaleM(outModel, 0, scale, scale, scale)
        Matrix.setIdentityM(tmpModel, 0)
        // ماتریس نرمال = معکوس ترانهاده‌ی بخش چرخش (فقط چرخش+مقیاس یکنواخت)
        Matrix.setIdentityM(tmpNormal, 0)
        System.arraycopy(tmpModel, 0, tmpNormal, 0, 16)
    }
}
