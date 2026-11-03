package com.monopoly.world3d.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

/**
 * نمای OpenGL با کنترل لمسی: کشیدن = چرخش دوربین، دو انگشت = بزرگ‌نمایی.
 */
class GameSurfaceView(context: Context, quality: Int) : GLSurfaceView(context) {

    val renderer: GlBoardRenderer = GlBoardRenderer(context, quality)

    private var lastX = 0f
    private var lastY = 0f
    private var pinchDist = 0f
    private var touchEnabled = true

    init {
        setEGLContextClientVersion(2)
        if (quality == GlBoardRenderer.QUALITY_HIGH) {
            setEGLConfigChooser(EGLConfigChooserCompat())
        }
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    fun setTouchEnabled(enabled: Boolean) { touchEnabled = enabled }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!touchEnabled) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = e.x; lastY = e.y
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (e.pointerCount >= 2) {
                    pinchDist = distance(e)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (e.pointerCount >= 2) {
                    val d = distance(e)
                    if (pinchDist > 0f) {
                        renderer.camera.zoomBy(d / pinchDist)
                    }
                    pinchDist = d
                } else {
                    renderer.camera.rotateBy(e.x - lastX, e.y - lastY)
                    lastX = e.x; lastY = e.y
                }
                return true
            }
        }
        return false
    }

    private fun distance(e: MotionEvent): Float {
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
