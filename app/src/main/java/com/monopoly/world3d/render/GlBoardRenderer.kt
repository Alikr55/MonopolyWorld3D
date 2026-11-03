package com.monopoly.world3d.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.monopoly.world3d.engine.Engine
import com.monopoly.world3d.engine.GameState
import com.monopoly.world3d.engine.SpaceType
import com.monopoly.world3d.engine.TokenKind
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min
import kotlin.math.sin

/**
 * رندرر OpenGL ES صفحه‌ی بازی — تخته‌ی سه‌بعدی، مهره‌ها، تاس، خانه و هتل.
 * منطق بازی هرگز در این فایل اجرا نمی‌شود؛ فقط آینه‌ی وضعیت نمایش داده می‌شود.
 */
class GlBoardRenderer(
    context: Context,
    private val quality: Int
) : GLSurfaceView.Renderer {

    companion object {
        const val QUALITY_LOW = BoardTextures.QUALITY_LOW
        const val QUALITY_MEDIUM = BoardTextures.QUALITY_MEDIUM
        const val QUALITY_HIGH = BoardTextures.QUALITY_HIGH
    }

    private val appContext = context.applicationContext
    private val commands = ConcurrentLinkedQueue<Runnable>()
    var menuMode = false
        private set

    // دوربین و شیدر
    val camera = Camera3D()
    private lateinit var textures: BoardTextures

    // مش‌ها
    private lateinit var tableMesh: GlMesh
    private lateinit var baseMesh: GlMesh
    private lateinit var centerMesh: GlMesh
    private lateinit var tileQuads: MutableList<GlMesh>
    private lateinit var houseMesh: GlMesh
    private lateinit var hotelMesh: GlMesh
    private lateinit var diceMesh: GlMesh
    private lateinit var shadowMesh: GlMesh
    private lateinit var flagMesh: GlMesh
    private lateinit var highlightQuad: GlMesh
    private lateinit var tokenMeshes: MutableMap<TokenKind, GlMesh>
    private lateinit var cityGround: GlMesh
    private lateinit var cityBuildings: MutableList<Pair<GlMesh, FloatArray>> // مش و موقعیت

    // بافت‌ها
    private var tableTex = 0
    private var centerTex = 0
    private var diceTex = 0
    private var shadowTex = 0
    private var cityWindowTex = 0
    private var buildingSideTex = 0
    private val tileTex = IntArray(40)

    // آینه‌ی وضعیت (از رشته‌ی UI با syncState پر می‌شود)
    private var playerCount = 2
    private val playerPositions = Array(6) { floatArrayOf(0f, 0f, 5.5f) }
    private val playerActive = BooleanArray(6)
    private val playerColors = FloatArray(6) { 1f }
    private val playerKinds = arrayOfNulls<TokenKind>(6)
    private val tileOwner = IntArray(40) { -1 }
    private val tileHouses = IntArray(40)
    private val tileMortgaged = BooleanArray(40)
    private var activePlayer = 0
    private var highlightSpace = -1
    private var pulseSpace = -1
    private var pulseTime = 0f

    // انیمیشن مهره
    private var movePath: MutableList<FloatArray> = mutableListOf()
    private var movePlayer = -1
    private var moveT = 0f
    private var moveFrom = FloatArray(3)
    private var moveDuration = 0.18f
    private var moveDoneCallback: Runnable? = null

    // انیمیشن تاس
    private var diceT = -1f // -1 = خنثی
    private val diceTarget = intArrayOf(1, 1)
    private var diceDoneCallback: Runnable? = null
    private val dicePos = arrayOf(floatArrayOf(-0.9f, 0.30f, 1.6f), floatArrayOf(0.9f, 0.30f, 1.6f))
    private val diceRot = arrayOf(FloatArray(3), FloatArray(3))
    private val diceSpin = arrayOf(FloatArray(3), FloatArray(3))
    private var diceSettled = arrayOf(FloatArray(3), FloatArray(3))

    private var time = 0f
    private val model = FloatArray(16)
    private val normalMat = FloatArray(16)
    private val lightDir = floatArrayOf(0.35f, 1f, 0.45f)

    // ---------------- چرخه‌ی حیات GL ----------------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.13f, 0.17f, 0.24f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Shaders.init()
        textures = BoardTextures(appContext, quality)
        buildGeometry()
        buildTextures()
        diceSettled[0] = faceRotation(diceTarget[0])
        diceSettled[1] = faceRotation(diceTarget[1])
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        camera.aspect = width.toFloat() / maxOf(1, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // اجرای فرمان‌های صف‌شده از رشته‌ی UI
        while (true) {
            val cmd = commands.poll() ?: break
            try { cmd.run() } catch (e: Exception) { /* جلوگیری از کرش رندر */ }
        }
        time += 1f / 60f
        updateAnimations()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        camera.update()
        if (menuMode) { drawCity(); return }
        drawBoard()
        drawFlagsAndBuildings()
        drawTokens()
        drawDice()
        drawHighlight()
    }

    // ---------------- ساخت هندسه و بافت ----------------

    private fun buildGeometry() {
        var m = MeshBuilder()
        // میز چوبی بزرگ
        m.groundQuad(-22f, -22f, 22f, 22f, -0.02f, 1f, 1f, 1f)
        tableMesh = GlMesh(m.buildArray())

        // پایه‌ی صفحه
        m = MeshBuilder()
        m.box(0f, 0.125f, 0f, 13.2f, 0.25f, 13.2f, 0.92f, 0.90f, 0.82f)
        baseMesh = GlMesh(m.buildArray())

        // مرکز صفحه
        m = MeshBuilder()
        m.groundQuad(-6.45f, -6.45f, 6.45f, 6.45f, 0.254f, 1f, 1f, 1f)
        centerMesh = GlMesh(m.buildArray())

        // چهارضلعی خانه‌ها (مستطیل ۱×۲ با نوار رنگ در سمت شمال)
        tileQuads = mutableListOf()
        for (id in 0..39) {
            val corner = BoardLayout.isCorner(id)
            val s = if (corner) BoardLayout.CORNER / 2 else BoardLayout.TILE_W / 2
            val l = if (corner) BoardLayout.CORNER / 2 else BoardLayout.TILE_L / 2
            val q = MeshBuilder()
            // v=1 در شمال (z-)؛ پیکسل‌های بالای بیت‌مپ (نوار رنگ) در v=1
            val p1 = floatArrayOf(-s, 0f, l); val p2 = floatArrayOf(s, 0f, l)
            val p3 = floatArrayOf(s, 0f, -l); val p4 = floatArrayOf(-s, 0f, -l)
            q.quad(p1, p2, p3, p4, 0f, 1f, 0f, 1f, 1f, 1f)
            tileQuads.add(GlMesh(q.buildArray()))
        }

        houseMesh = GlMesh(Tokens.houseMesh())
        hotelMesh = GlMesh(Tokens.hotelMesh())
        diceMesh = GlMesh(Tokens.diceMesh(0.6f))

        // سایه‌ی نرم
        val sh = MeshBuilder()
        sh.groundQuad(-0.30f, -0.30f, 0.30f, 0.30f, 0.0f, 1f, 1f, 1f)
        shadowMesh = GlMesh(sh.buildArray())

        // پرچم مالکیت
        val fl = MeshBuilder()
        fl.cylinder(0f, 0f, 0f, 0.015f, 0.35f, 5, 1f, 1f, 1f)
        fl.box(0.09f, 0.30f, 0f, 0.15f, 0.09f, 0.005f, 1f, 1f, 1f)
        flagMesh = GlMesh(fl.buildArray())

        // چهارضلعی هایلایت
        val hq = MeshBuilder()
        hq.groundQuad(-0.5f, -0.5f, 0.5f, 0.5f, 0f, 1f, 1f, 1f)
        highlightQuad = GlMesh(hq.buildArray())

        // مهره‌ها
        tokenMeshes = mutableMapOf()
        for (kind in TokenKind.entries) {
            tokenMeshes[kind] = GlMesh(Tokens.mesh(kind))
        }

        // شهر منو
        m = MeshBuilder()
        m.groundQuad(-14f, -14f, 14f, 14f, 0f, 1f, 1f, 1f)
        cityGround = GlMesh(m.buildArray())
        cityBuildings = mutableListOf()
        val rnd = kotlin.random.Random(11)
        for (i in 0 until 46) {
            val bm = MeshBuilder()
            val w = 0.8f + rnd.nextFloat() * 1.4f
            val d = 0.8f + rnd.nextFloat() * 1.4f
            val h = 1.2f + rnd.nextFloat() * 4.2f
            bm.box(0f, h / 2, 0f, w, h, d, 1f, 1f, 1f)
            val x = -12f + rnd.nextFloat() * 24f
            val z = -12f + rnd.nextFloat() * 24f
            if (kotlin.math.abs(x) < 2.2f && kotlin.math.abs(z) < 2.2f) continue
            cityBuildings.add(GlMesh(bm.buildArray()) to floatArrayOf(x, 0f, z))
        }
    }

    private fun buildTextures() {
        tableTex = textures.upload(textures.tableTexture(), mipmap = quality > QUALITY_LOW)
        centerTex = textures.upload(textures.centerTexture(), mipmap = quality > QUALITY_LOW)
        diceTex = textures.upload(textures.diceAtlas())
        shadowTex = textures.upload(textures.blobShadow())
        cityWindowTex = textures.upload(textures.cityWindowTexture())
        buildingSideTex = textures.upload(textures.buildingSideTexture(0xFF5D8AA8.toInt()))
        for (id in 0..39) {
            val space = com.monopoly.world3d.engine.Board.createSpaces()[id]
            tileTex[id] = textures.upload(textures.tileTexture(space), mipmap = quality > QUALITY_LOW)
        }
    }

    // ---------------- همگام‌سازی وضعیت ----------------

    /** کپی وضعیت از موتور (رشته‌ی UI) به آینه‌ی رندرر */
    fun syncState(state: GameState) {
        commands.add(Runnable {
            playerCount = state.players.size
            for (p in state.players) {
                val idx = p.id.coerceIn(0, 5)
                playerActive[idx] = !p.bankrupt
                playerKinds[idx] = p.token
                val rgb = p.colorHex
                playerColors[idx * 3] = ((rgb shr 16 and 0xFF).toInt()) / 255f
                playerColors[idx * 3 + 1] = ((rgb shr 8 and 0xFF).toInt()) / 255f
                playerColors[idx * 3 + 2] = ((rgb and 0xFF).toInt()) / 255f
                if (movePlayer != p.id) {
                    val tp = BoardLayout.tokenPosition(p.position, idx, playerCount)
                    playerPositions[idx][0] = tp[0]; playerPositions[idx][1] = 0f; playerPositions[idx][2] = tp[2]
                }
            }
            for (i in 0..39) {
                tileOwner[i] = state.properties[i].owner
                tileHouses[i] = state.properties[i].houses
                tileMortgaged[i] = state.properties[i].mortgaged
            }
            activePlayer = state.currentTurn
        })
    }

    fun animateMove(pathSpaces: List<Int>, playerId: Int, laneCount: Int, onDone: Runnable) {
        commands.add(Runnable {
            val pts = mutableListOf<FloatArray>()
            for (space in pathSpaces) {
                val tp = BoardLayout.tokenPosition(space, playerId.coerceIn(0, 5), laneCount)
                pts.add(floatArrayOf(tp[0], 0f, tp[2]))
            }
            movePath = pts
            movePlayer = playerId
            moveT = 0f
            if (pts.isNotEmpty()) {
                moveFrom = playerPositions[playerId.coerceIn(0, 5)].copyOf()
            }
            moveDoneCallback = onDone
        })
    }

    fun animateDice(d1: Int, d2: Int, onDone: Runnable) {
        commands.add(Runnable {
            diceTarget[0] = d1.coerceIn(1, 6)
            diceTarget[1] = d2.coerceIn(1, 6)
            diceT = 0f
            for (i in 0..1) {
                diceSpin[i][0] = 300f + kotlin.random.Random.nextFloat() * 500f
                diceSpin[i][1] = 260f + kotlin.random.Random.nextFloat() * 500f
                diceSpin[i][2] = 200f + kotlin.random.Random.nextFloat() * 400f
            }
            diceSettled[0] = faceRotation(diceTarget[0])
            diceSettled[1] = faceRotation(diceTarget[1])
            diceDoneCallback = onDone
        })
    }

    fun pulseProperty(spaceId: Int) {
        commands.add(Runnable {
            pulseSpace = spaceId
            pulseTime = 0f
        })
    }

    fun setHighlightSpace(spaceId: Int) {
        commands.add(Runnable { highlightSpace = spaceId })
    }

    fun snapToPlayer(playerId: Int) {
        commands.add(Runnable {
            val p = playerPositions[playerId.coerceIn(0, 5)]
            camera.followTo(p[0], p[1], p[2])
        })
    }

    fun resetCamera() {
        commands.add(Runnable { camera.resetTarget() })
    }

    fun setMenuMode(value: Boolean) {
        commands.add(Runnable {
            menuMode = value
            if (value) {
                camera.minDistance = 10f; camera.maxDistance = 26f
                camera.distance = 17f; camera.polar = 46f
            } else {
                camera.minDistance = 8f; camera.maxDistance = 22f
                camera.distance = 15f; camera.polar = 52f
            }
        })
    }

    // ---------------- انیمیشن ----------------

    private fun updateAnimations() {
        // حرکت مهره
        if (movePlayer >= 0 && movePath.isNotEmpty()) {
            moveT += 1f / 60f / moveDuration
            if (moveT >= 1f) {
                moveT = 0f
                val next = movePath.removeAt(0)
                moveFrom = next.copyOf()
                playerPositions[movePlayer][0] = next[0]
                playerPositions[movePlayer][1] = 0f
                playerPositions[movePlayer][2] = next[2]
                if (movePath.isEmpty()) {
                    movePlayer = -1
                    camera.resetTarget()
                    moveDoneCallback?.let { postToMain(it) }
                    moveDoneCallback = null
                }
            } else {
                val target = movePath[0]
                val t = moveT
                val idx = movePlayer.coerceIn(0, 5)
                playerPositions[idx][0] = moveFrom[0] + (target[0] - moveFrom[0]) * t
                playerPositions[idx][2] = moveFrom[2] + (target[2] - moveFrom[2]) * t
                // پرش پارابولیک
                playerPositions[idx][1] = sin(t * Math.PI).toFloat() * 0.38f
            }
        }
        // تاس
        if (diceT >= 0f) {
            diceT += 1f / 60f / 0.95f
            if (diceT >= 1f) {
                diceT = -1f
                diceDoneCallback?.let { postToMain(it) }
                diceDoneCallback = null
            }
        }
        // پالس
        if (pulseTime > 0f) {
            pulseTime += 1f / 60f
            if (pulseTime > 1.4f) pulseTime = 0f
        }
    }

    private fun postToMain(r: Runnable) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(r)
    }

    private fun faceRotation(face: Int): FloatArray = when (face) {
        1 -> floatArrayOf(0f, 0f, 90f)   // +X به بالا
        2 -> floatArrayOf(0f, 0f, -90f)  // -X به بالا
        3 -> floatArrayOf(0f, 0f, 0f)    // +Y
        4 -> floatArrayOf(180f, 0f, 0f)  // -Y
        5 -> floatArrayOf(-90f, 0f, 0f)  // +Z به بالا
        else -> floatArrayOf(90f, 0f, 0f)// -Z به بالا
    }

    // ---------------- ترسیم ----------------

    private fun setCommonUniforms() {
        GLES20.glUniform3fv(Shaders.uLightDir, 1, lightDir, 0)
        GLES20.glUniform3f(Shaders.uAmbient, 0.34f, 0.34f, 0.38f)
        GLES20.glUniform1f(Shaders.uAlpha, 1f)
        GLES20.glUniform1f(Shaders.uHighlight, 0f)
        GLES20.glUniform3f(Shaders.uTint, 1f, 1f, 1f)
        GLES20.glUniformMatrix4fv(Shaders.uMVP, 1, false, camera.viewProjMatrix, 0)
    }

    private fun bindTexture(tex: Int, unit: Int = 0) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glUniform1i(Shaders.uTexture, unit)
    }

    private fun drawMesh(mesh: GlMesh, x: Float, y: Float, z: Float, rotY: Float, scale: Float,
                         texture: Int, texMix: Float, tintR: Float = 1f, tintG: Float = 1f, tintB: Float = 1f,
                         alpha: Float = 1f, highlight: Float = 0f, rotX: Float = 0f, rotZ: Float = 0f) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(model, 0, rotY, 0f, 1f, 0f)
        if (rotX != 0f) Matrix.rotateM(model, 0, rotX, 1f, 0f, 0f)
        if (rotZ != 0f) Matrix.rotateM(model, 0, rotZ, 0f, 0f, 1f)
        if (scale != 1f) Matrix.scaleM(model, 0, scale, scale, scale)
        GLES20.glUniformMatrix4fv(Shaders.uModel, 1, false, model, 0)
        // ماتریس نرمال: بخش چرخش مدل (مقیاس یکنواخت)
        normalMat[0] = model[0]; normalMat[1] = model[1]; normalMat[2] = model[2]
        normalMat[3] = model[4]; normalMat[4] = model[5]; normalMat[5] = model[6]
        normalMat[6] = model[8]; normalMat[7] = model[9]; normalMat[8] = model[10]
        GLES20.glUniformMatrix3fv(Shaders.uNormalMat, 1, false, normalMat, 0)
        bindTexture(texture)
        GLES20.glUniform1f(Shaders.uTextureMix, texMix)
        GLES20.glUniform3f(Shaders.uTint, tintR, tintG, tintB)
        GLES20.glUniform1f(Shaders.uAlpha, alpha)
        GLES20.glUniform1f(Shaders.uHighlight, highlight)
        mesh.draw(Shaders.program, Shaders.aPos, Shaders.aNormal, Shaders.aColor, Shaders.aUV)
    }

    private fun tileRotationY(id: Int): Float = when {
        BoardLayout.sideOf(id) == BoardLayout.SIDE_LEFT -> -90f
        BoardLayout.sideOf(id) == BoardLayout.SIDE_TOP -> 180f
        BoardLayout.sideOf(id) == BoardLayout.SIDE_RIGHT -> 90f
        BoardLayout.isCorner(id) -> if (id >= 20) 180f else 0f
        else -> 0f
    }

    private fun drawBoard() {
        setCommonUniforms()
        drawMesh(tableMesh, 0f, 0f, 0f, 0f, 1f, tableTex, 1f)
        drawMesh(baseMesh, 0f, 0f, 0f, 0f, 1f, 0, 0f, tintR = 1f, tintG = 1f, tintB = 1f)
        drawMesh(centerMesh, 0f, 0f, 0f, 0f, 1f, centerTex, 1f)
        for (id in 0..39) {
            val c = BoardLayout.tileCenter(id)
            val rot = tileRotationY(id)
            drawMesh(tileQuads[id], c[0], 0.256f, c[1], rot, 1f, tileTex[id], 1f)
        }
    }

    private fun drawFlagsAndBuildings() {
        setCommonUniforms()
        for (id in 0..39) {
            val owner = tileOwner[id]
            val c = BoardLayout.tileCenter(id)
            // پرچم مالکیت
            if (owner >= 0 && owner < 6 && playerActive[owner]) {
                val side = BoardLayout.sideOf(id)
                var fx = c[0]; var fz = c[1]
                when (side) {
                    BoardLayout.SIDE_BOTTOM -> fz -= 1.45f
                    BoardLayout.SIDE_LEFT -> fx += 1.45f
                    BoardLayout.SIDE_TOP -> fz += 1.45f
                    BoardLayout.SIDE_RIGHT -> fx -= 1.45f
                    else -> {}
                }
                drawMesh(flagMesh, fx, 0.254f, fz, 0f, 1f, 0, 0f,
                    tintR = playerColors[owner * 3], tintG = playerColors[owner * 3 + 1], tintB = playerColors[owner * 3 + 2])
            }
            // خانه و هتل
            val houses = tileHouses[id]
            if (houses in 1..4) {
                val spots = BoardLayout.housePositions(id)
                for (i in 0 until houses) {
                    val sp = spots[i]
                    drawMesh(houseMesh, sp[0], 0.256f, sp[2], tileRotationY(id), 1f, 0, 0f,
                        tintR = 0.35f, tintG = 0.75f, tintB = 0.40f)
                }
            } else if (houses == 5) {
                val sp = BoardLayout.housePositions(id)[4]
                drawMesh(hotelMesh, sp[0], 0.256f, sp[2], tileRotationY(id), 1f, 0, 0f,
                    tintR = 0.90f, tintG = 0.25f, tintB = 0.25f)
            }
        }
    }

    private fun drawTokens() {
        setCommonUniforms()
        for (i in 0 until playerCount) {
            if (!playerActive[i]) continue
            val kind = playerKinds[i] ?: TokenKind.CROWN
            val pos = playerPositions[i]
            val isActive = i == activePlayer
            val highlight = if (isActive) 0.35f + 0.25f * sin(time * 5f) else 0f
            // سایه
            if (quality > QUALITY_LOW) {
                GLES20.glDepthMask(false)
                drawMesh(shadowMesh, pos[0], 0.258f, pos[2], 0f, 1.1f, shadowTex, 1f, alpha = 0.8f)
                GLES20.glDepthMask(true)
            }
            drawMesh(tokenMeshes[kind]!!, pos[0], pos[1] + Tokens.yOffset(kind) + 0.256f, pos[2],
                0f, 1f, 0, 0f,
                tintR = playerColors[i * 3], tintG = playerColors[i * 3 + 1], tintB = playerColors[i * 3 + 2],
                highlight = highlight)
        }
    }

    private fun drawDice() {
        setCommonUniforms()
        for (i in 0..1) {
            var rx = diceSettled[i][0]
            var ry = diceSettled[i][1]
            var rz = diceSettled[i][2]
            var y = dicePos[i][1]
            if (diceT >= 0f) {
                val t = diceT
                rx = diceSettled[i][0] + diceSpin[i][0] * (1f - t) * (1f - t)
                ry = diceSettled[i][1] + diceSpin[i][1] * (1f - t) * (1f - t)
                rz = diceSettled[i][2] + diceSpin[i][2] * (1f - t) * (1f - t)
                y += sin(t * Math.PI).toFloat() * 1.1f
            }
            drawMesh(diceMesh, dicePos[i][0], y, dicePos[i][2], ry, 1f, diceTex, 1f,
                rotX = rx, rotZ = rz)
        }
    }

    private fun drawHighlight() {
        if (highlightSpace in 0..39) {
            val c = BoardLayout.tileCenter(highlightSpace)
            setCommonUniforms()
            GLES20.glDepthMask(false)
            drawMesh(highlightQuad, c[0], 0.262f, c[1], 0f, 1.35f, 0, 0f,
                tintR = 1f, tintG = 0.9f, tintB = 0.3f, alpha = 0.35f,
                highlight = 0.4f + 0.3f * sin(time * 6f))
            GLES20.glDepthMask(true)
        }
        if (pulseSpace in 0..39 && pulseTime > 0f) {
            val c = BoardLayout.tileCenter(pulseSpace)
            val a = (1f - pulseTime / 1.4f).coerceIn(0f, 1f)
            setCommonUniforms()
            GLES20.glDepthMask(false)
            drawMesh(highlightQuad, c[0], 0.264f, c[1], 0f, 1f + pulseTime * 0.6f, 0, 0f,
                tintR = 1f, tintG = 0.85f, tintB = 0.25f, alpha = a * 0.6f)
            GLES20.glDepthMask(true)
            if (pulseTime >= 1.4f) pulseSpace = -1
        }
    }

    private fun drawCity() {
        setCommonUniforms()
        // چرخش آرام دوربین در حالت منو
        camera.azimuth += 0.06f
        drawMesh(cityGround, 0f, 0f, 0f, 0f, 1f, cityWindowTex, 0f, tintR = 0.25f, tintG = 0.27f, tintB = 0.35f)
        for ((mesh, pos) in cityBuildings) {
            drawMesh(mesh, pos[0], 0f, pos[2], 0f, 1f, buildingSideTex, 1f,
                tintR = 0.8f, tintG = 0.8f, tintB = 0.9f)
        }
    }

    fun dispose() {
        // آزادسازی بافت‌ها در صورت تخریب (اختیاری)
    }
}
