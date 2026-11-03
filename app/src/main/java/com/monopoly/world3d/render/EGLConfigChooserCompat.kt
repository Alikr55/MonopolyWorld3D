package com.monopoly.world3d.render

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * انتخاب پیکربندی EGL با چندنمونه‌گیری (MSAA 4x) برای کیفیت بالا.
 */
class EGLConfigChooserCompat : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val attribs = intArrayOf(
            EGL10.EGL_RED_SIZE, 5,
            EGL10.EGL_GREEN_SIZE, 6,
            EGL10.EGL_BLUE_SIZE, 5,
            EGL10.EGL_ALPHA_SIZE, 0,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_SAMPLE_BUFFERS, 1,
            EGL10.EGL_SAMPLES, 4,
            EGL10.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        if (egl.eglChooseConfig(display, attribs, configs, 1, num) && num[0] > 0) {
            return configs[0]!!
        }
        // بازگشت به پیکربندی عادی
        val fallback = intArrayOf(
            EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE, 5,
            EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE
        )
        egl.eglChooseConfig(display, fallback, configs, 1, num)
        return configs[0]!!
    }
}
