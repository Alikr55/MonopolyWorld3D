package com.monopoly.world3d.render

/**
 * مدل‌های مهره‌های بازی — همه با هندسه‌ی رویه‌ای ساخته می‌شوند.
 * مهره‌ها سفید ساخته می‌شوند و رنگ بازیکن در زمان رندر با uTint اعمال می‌شود.
 */
object Tokens {

    /** ارتفاع نگه‌داشتن مهره روی زمین (پایه در y=0) */
    fun yOffset(kind: com.monopoly.world3d.engine.TokenKind): Float = when (kind) {
        com.monopoly.world3d.engine.TokenKind.SPORT_CAR -> 0.02f
        com.monopoly.world3d.engine.TokenKind.RACE_CAR -> 0.02f
        com.monopoly.world3d.engine.TokenKind.SHIP -> 0.02f
        com.monopoly.world3d.engine.TokenKind.PLANE -> 0.02f
        com.monopoly.world3d.engine.TokenKind.CROWN -> 0.0f
        com.monopoly.world3d.engine.TokenKind.ROCKET -> 0.0f
        com.monopoly.world3d.engine.TokenKind.ROBOT -> 0.0f
        com.monopoly.world3d.engine.TokenKind.DIAMOND -> 0.0f
        com.monopoly.world3d.engine.TokenKind.CAT -> 0.0f
        com.monopoly.world3d.engine.TokenKind.TROPHY -> 0.0f
    }

    fun mesh(kind: com.monopoly.world3d.engine.TokenKind): FloatArray {
        val m = MeshBuilder()
        val white = 1f
        when (kind) {
            com.monopoly.world3d.engine.TokenKind.SPORT_CAR -> {
                m.box(0f, 0.10f, 0f, 0.44f, 0.10f, 0.22f, white, white, white)
                m.box(-0.02f, 0.19f, 0f, 0.22f, 0.08f, 0.19f, white, white, white)
                for (wx in listOf(-0.15f, 0.15f)) for (wz in listOf(-0.11f, 0.11f))
                    m.box(wx, 0.045f, wz, 0.08f, 0.09f, 0.05f, white, white, white)
            }
            com.monopoly.world3d.engine.TokenKind.RACE_CAR -> {
                m.box(0f, 0.08f, 0f, 0.48f, 0.07f, 0.16f, white, white, white)
                m.box(0.02f, 0.14f, 0f, 0.16f, 0.07f, 0.14f, white, white, white)
                m.box(-0.20f, 0.15f, 0f, 0.05f, 0.06f, 0.20f, white, white, white) // اسپویلر
                for (wx in listOf(-0.16f, 0.16f)) for (wz in listOf(-0.09f, 0.09f))
                    m.box(wx, 0.04f, wz, 0.07f, 0.08f, 0.05f, white, white, white)
            }
            com.monopoly.world3d.engine.TokenKind.SHIP -> {
                m.box(0f, 0.09f, 0f, 0.40f, 0.12f, 0.20f, white, white, white)
                m.pyramid(0.20f, 0.03f, 0f, 0.16f, 0.20f, 0.12f, white, white, white) // دماغه
                m.cylinder(-0.02f, 0.15f, 0f, 0.015f, 0.38f, 6, white, white, white) // دکل
                m.box(-0.02f, 0.30f, 0.06f, 0.02f, 0.16f, 0.12f, white, white, white) // بادبان
            }
            com.monopoly.world3d.engine.TokenKind.PLANE -> {
                m.box(0f, 0.12f, 0f, 0.10f, 0.10f, 0.46f, white, white, white) // بدنه
                m.box(0f, 0.13f, 0.04f, 0.52f, 0.03f, 0.11f, white, white, white) // بال
                m.box(0f, 0.18f, -0.20f, 0.03f, 0.10f, 0.10f, white, white, white) // دم
                m.box(0f, 0.13f, -0.19f, 0.18f, 0.02f, 0.06f, white, white, white) // بال دم
            }
            com.monopoly.world3d.engine.TokenKind.CROWN -> {
                m.cylinder(0f, 0.0f, 0f, 0.19f, 0.05f, 12, white, white, white) // پایه
                m.cylinder(0f, 0.05f, 0f, 0.15f, 0.14f, 12, white, white, white) // بدنه
                for (i in 0 until 6) {
                    val a = Math.PI * 2 * i / 6
                    m.cone(kotlin.math.cos(a).toFloat() * 0.13f, 0.19f, kotlin.math.sin(a).toFloat() * 0.13f,
                        0.035f, 0.12f, 6, white, white, white)
                }
            }
            com.monopoly.world3d.engine.TokenKind.ROCKET -> {
                m.cylinder(0f, 0.0f, 0f, 0.09f, 0.30f, 12, white, white, white)
                m.cone(0f, 0.30f, 0f, 0.09f, 0.16f, 12, white, white, white)
                for (i in 0..2) {
                    val a = Math.PI * 2 * i / 3
                    val fx = kotlin.math.cos(a).toFloat() * 0.08f
                    val fz = kotlin.math.sin(a).toFloat() * 0.08f
                    m.box(fx, 0.08f, fz, 0.05f, 0.16f, 0.03f, white, white, white)
                }
                m.cone(0f, -0.001f, 0f, 0.05f, -0.08f, 8, white, white, white) // شعله
            }
            com.monopoly.world3d.engine.TokenKind.ROBOT -> {
                m.box(0f, 0.17f, 0f, 0.20f, 0.22f, 0.14f, white, white, white) // بدن
                m.box(0f, 0.34f, 0f, 0.14f, 0.12f, 0.12f, white, white, white) // سر
                m.cylinder(0f, 0.40f, 0f, 0.01f, 0.07f, 6, white, white, white) // آنتن
                m.sphere(0f, 0.48f, 0f, 0.025f, 4, 6, white, white, white)
                for (ax in listOf(-0.14f, 0.14f)) m.box(ax, 0.16f, 0f, 0.04f, 0.16f, 0.04f, white, white, white)
                for (lx in listOf(-0.06f, 0.06f)) m.box(lx, 0.03f, 0f, 0.06f, 0.06f, 0.06f, white, white, white)
            }
            com.monopoly.world3d.engine.TokenKind.DIAMOND -> {
                m.octahedron(0f, 0.20f, 0f, 0.30f, 0.40f, 0.30f, white, white, white)
            }
            com.monopoly.world3d.engine.TokenKind.CAT -> {
                m.cylinder(0f, 0.0f, 0f, 0.10f, 0.20f, 10, white, white, white) // بدن
                m.sphere(0f, 0.26f, 0f, 0.095f, 5, 8, white, white, white) // سر
                m.cone(-0.05f, 0.33f, 0f, 0.03f, 0.07f, 5, white, white, white) // گوش
                m.cone(0.05f, 0.33f, 0f, 0.03f, 0.07f, 5, white, white, white)
                m.cylinder(0f, 0.04f, -0.10f, 0.02f, 0.20f, 6, white, white, white) // دم
            }
            com.monopoly.world3d.engine.TokenKind.TROPHY -> {
                m.box(0f, 0.02f, 0f, 0.20f, 0.05f, 0.20f, white, white, white) // پایه
                m.cylinder(0f, 0.05f, 0f, 0.04f, 0.10f, 8, white, white, white) // ساقه
                m.cylinder(0f, 0.15f, 0f, 0.12f, 0.18f, 12, white, white, white) // کاسه
                m.cylinder(0f, 0.15f, 0f, 0.15f, 0.02f, 12, white, white, white) // لبه
            }
        }
        return m.buildArray()
    }

    /** خانه‌ی سبز کوچک */
    fun houseMesh(): FloatArray {
        val m = MeshBuilder()
        m.box(0f, 0.05f, 0f, 0.16f, 0.10f, 0.16f, 1f, 1f, 1f)
        m.pyramid(0f, 0.10f, 0f, 0.20f, 0.20f, 0.08f, 1f, 1f, 1f)
        return m.buildArray()
    }

    /** هتل بزرگ قرمز */
    fun hotelMesh(): FloatArray {
        val m = MeshBuilder()
        m.box(0f, 0.09f, 0f, 0.30f, 0.18f, 0.20f, 1f, 1f, 1f)
        m.pyramid(0f, 0.18f, 0f, 0.36f, 0.26f, 0.10f, 1f, 1f, 1f)
        m.box(0f, 0.19f, 0.05f, 0.05f, 0.05f, 0.05f, 1f, 1f, 1f)
        return m.buildArray()
    }

    /** تاس با UV اطلس ۳×۲ برای ۶ چهره */
    fun diceMesh(size: Float): FloatArray {
        val m = MeshBuilder()
        val h = size / 2
        // هر وجه: سلول اطلس (f-1)%3 , (f-1)/3
        fun face(p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, n: FloatArray, face: Int) {
            val u0 = ((face - 1) % 3) / 3f
            val v0 = ((face - 1) / 3) / 2f
            val u1 = u0 + 1f / 3f
            val v1 = v0 + 1f / 2f
            val white = 1f
            fun vert(p: FloatArray, u: Float, v: Float) {
                m.customQuadVert(p, n, white, white, white, u, v)
            }
            vert(p1, u0, v0); vert(p2, u1, v0); vert(p3, u1, v1)
            vert(p1, u0, v0); vert(p3, u1, v1); vert(p4, u0, v1)
        }
        val c = floatArrayOf(0f, 0f, 0f)
        // +X چهره ۱
        face(floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), floatArrayOf(1f, 0f, 0f), 1)
        // -X چهره ۲
        face(floatArrayOf(-h, -h, h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(-h, h, h), floatArrayOf(-1f, 0f, 0f), 2)
        // +Y چهره ۳
        face(floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h), floatArrayOf(0f, 1f, 0f), 3)
        // -Y چهره ۴
        face(floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(0f, -1f, 0f), 4)
        // +Z چهره ۵
        face(floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h), floatArrayOf(0f, 0f, 1f), 5)
        // -Z چهره ۶
        face(floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h), floatArrayOf(0f, 0f, -1f), 6)
        return m.buildArray()
    }
}

/** توابع کمکی برای ساخت رأس‌های سفارشی */
private fun MeshBuilder.customQuadVert(p: FloatArray, n: FloatArray, r: Float, g: Float, b: Float, u: Float, v: Float) {
    addVertex(p[0], p[1], p[2], n[0], n[1], n[2], r, g, b, u, v)
}
