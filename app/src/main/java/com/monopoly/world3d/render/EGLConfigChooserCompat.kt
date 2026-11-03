package com.monopoly.world3d.render

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * انتخاب پیکربندی EGL با چندنمونه‌گیری (MSAA 4x) برای کیفیت بالا.
 * اگر هیچ‌کدام از پیکربندی‌های خواسته‌شده پشتیبانی نشود، اولین پیکربندی
 * موجود دستگاه برگردانده می‌شود تا هرگز NullPointerException رخ ندهد.
 */
class EGLConfigChooserCompat : GLSurfaceView.EGLConfigChooser {

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        // ۱) MSAA 4x با عمق ۲۴
        val attempts = listOf(
            intArrayOf(
                EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE, 5,
                EGL10.EGL_ALPHA_SIZE, 0, EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, 4, EGL10.EGL_NONE
            ),
            intArrayOf(
                EGL10.EGL_RED_SIZE, 5, EGL10.EGL_GREEN_SIZE, 6, EGL10.EGL_BLUE_SIZE, 5,
                EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE
            ),
            intArrayOf(
                EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                EGL10.EGL_DEPTH_SIZE, 8, EGL10.EGL_NONE
            ),
            intArrayOf(EGL10.EGL_NONE)
        )
        val configs = arrayOfNulls<EGLConfig>(64)
        val num = IntArray(1)
        for (attribs in attempts) {
            if (egl.eglChooseConfig(display, attribs, configs, configs.size, num) && num[0] > 0) {
                // در حالت MSAA ترجیح: اولین پیکربندی؛ در بقیه: اولین پیکربندی با عمق
                return configs[0]!!
            }
        }
        // ۲) آخرین راه: پیمایش همه‌ی پیکربندی‌های موجود
        if (egl.eglGetConfigs(display, configs, configs.size, num) && num[0] > 0) {
            for (i in 0 until num[0]) configs[i]?.let { return it }
        }
        // ۳) هیچ راهی نبود — همان رفتار پیش‌فرض GLSurfaceView (RuntimeException صریح و قابل‌فهم)
        throw IllegalArgumentException("هیچ پیکربندی EGL مناسبی روی این دستگاه یافت نشد")
    }
}
