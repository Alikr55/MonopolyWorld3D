package com.monopoly.world3d.render

/**
 * چیدمان هندسی صفحه در فضای سه‌بعدی.
 * صفحه ۱۳×۱۳ واحد است: گوشه‌ها ۲×۲، خانه‌های کناری ۱ عرض و ۲ عمق.
 */
object BoardLayout {

    const val HALF = 6.5f
    const val CORNER = 2.0f
    const val TILE_W = 1.0f   // عرض مماسی
    const val TILE_L = 2.0f   // عمق شعاعی

    // جهت‌های ضلع
    const val SIDE_BOTTOM = 0
    const val SIDE_LEFT = 1
    const val SIDE_TOP = 2
    const val SIDE_RIGHT = 3

    fun sideOf(spaceId: Int): Int = when (spaceId) {
        0, 10, 20, 30 -> -1  // گوشه‌ها
        in 1..9 -> SIDE_BOTTOM
        in 11..19 -> SIDE_LEFT
        in 21..29 -> SIDE_TOP
        else -> SIDE_RIGHT
    }

    /** مرکز خانه بر روی صفحه (y=0) */
    fun tileCenter(spaceId: Int): FloatArray {
        return when {
            spaceId == 0 -> floatArrayOf(5.5f, 5.5f)       // شروع — گوشه پایین-راست
            spaceId == 10 -> floatArrayOf(-5.5f, 5.5f)     // زندان — گوشه پایین-چپ
            spaceId == 20 -> floatArrayOf(-5.5f, -5.5f)    // پارکینگ — گوشه بالا-چپ
            spaceId == 30 -> floatArrayOf(5.5f, -5.5f)     // رفتن به زندان — گوشه بالا-راست
            spaceId in 1..9 -> floatArrayOf(4.5f - spaceId, 5.5f)
            spaceId in 11..19 -> floatArrayOf(-5.5f, 4.5f - (spaceId - 10))
            spaceId in 21..29 -> floatArrayOf(-4.5f + (spaceId - 20), -5.5f)
            else -> floatArrayOf(5.5f, -4.5f + (spaceId - 30))
        }
    }

    /** چرخش بافت خانه (درجه) تا متن از مرکز صفحه خوانا باشد */
    fun tileRotation(spaceId: Int): Float = when (sideOf(spaceId)) {
        SIDE_BOTTOM -> 0f
        SIDE_LEFT -> 90f
        SIDE_TOP -> 180f
        SIDE_RIGHT -> 270f
        else -> 0f
    }

    /** موقعیت مهره روی خانه: جایگاه بر اساس شماره بازیکن (تا ۶ نفر) */
    fun tokenPosition(spaceId: Int, laneIndex: Int, laneCount: Int): FloatArray {
        val c = tileCenter(spaceId)
        val side = sideOf(spaceId)
        // شبکه ۲×۳ درون خانه
        val row = laneIndex / 3
        val col = laneIndex % 3
        val perp = when (laneCount) {
            1 -> 0f
            2 -> (col - 0.5f) * 0.42f
            else -> (col - 1f) * 0.4f
        }
        val inward = 0.55f + row * 0.45f  // فاصله از لبه بیرونی به سمت مرکز
        return when (side) {
            SIDE_BOTTOM -> floatArrayOf(c[0] + perp, 0f, c[1] - inward + 0.4f)
            SIDE_LEFT -> floatArrayOf(c[0] + inward - 0.4f, 0f, c[1] - perp)
            SIDE_TOP -> floatArrayOf(c[0] - perp, 0f, c[1] + inward - 0.4f)
            SIDE_RIGHT -> floatArrayOf(c[0] - inward + 0.4f, 0f, c[1] + perp)
            else -> { // گوشه‌ها
                val dx = if (laneCount <= 1) 0f else (col - (laneCount - 1) / 2f) * 0.5f
                val dz = if (laneCount <= 1) 0f else (row - 0.5f) * 0.5f
                floatArrayOf(c[0] + dx, 0f, c[1] + dz)
            }
        }
    }

    /** موقعیت‌های ۴ خانه و ۱ هتل روی ملک (نزدیک لبه‌ی داخلی) */
    fun housePositions(spaceId: Int): Array<FloatArray> {
        val c = tileCenter(spaceId)
        val side = sideOf(spaceId)
        val spots = arrayOfNulls<FloatArray>(5)
        for (i in 0..3) {
            val t = (i - 1.5f) * 0.42f
            spots[i] = when (side) {
                SIDE_BOTTOM -> floatArrayOf(c[0] + t, 0f, c[1] - TILE_L / 2 + 0.55f)
                SIDE_LEFT -> floatArrayOf(c[0] + TILE_L / 2 - 0.55f, 0f, c[1] + t)
                SIDE_TOP -> floatArrayOf(c[0] - t, 0f, c[1] + TILE_L / 2 - 0.55f)
                else -> floatArrayOf(c[0] - TILE_L / 2 + 0.55f, 0f, c[1] - t)
            }
        }
        spots[4] = when (side) { // هتل مرکز لبه داخلی
            SIDE_BOTTOM -> floatArrayOf(c[0], 0f, c[1] - TILE_L / 2 + 0.55f)
            SIDE_LEFT -> floatArrayOf(c[0] + TILE_L / 2 - 0.55f, 0f, c[1])
            SIDE_TOP -> floatArrayOf(c[0], 0f, c[1] + TILE_L / 2 - 0.55f)
            else -> floatArrayOf(c[0] - TILE_L / 2 + 0.55f, 0f, c[1])
        }
        @Suppress("UNCHECKED_CAST")
        return spots as Array<FloatArray>
    }

    /** اندازه‌ی خانه‌های کوچک برای گوشه‌ها */
    fun isCorner(spaceId: Int): Boolean = sideOf(spaceId) == -1
}
