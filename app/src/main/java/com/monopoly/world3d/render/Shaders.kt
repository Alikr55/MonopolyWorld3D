package com.monopoly.world3d.render

import android.opengl.GLES20

/**
 * شیدرهای OpenGL ES 2.0 — نور جهت‌دار ساده با رنگ رأس و بافت.
 */
object Shaders {

    const val VERTEX_SRC = """
        uniform mat4 uMVP;
        uniform mat4 uModel;
        uniform mat3 uNormalMat;
        attribute vec3 aPos;
        attribute vec3 aNormal;
        attribute vec3 aColor;
        attribute vec2 aUV;
        varying vec3 vNormal;
        varying vec3 vColor;
        varying vec2 vUV;
        varying vec3 vWorldPos;
        void main() {
            vNormal = normalize(uNormalMat * aNormal);
            vColor = aColor;
            vUV = aUV;
            vec4 world = uModel * vec4(aPos, 1.0);
            vWorldPos = world.xyz;
            gl_Position = uMVP * world;
        }
    """

    const val FRAGMENT_SRC = """
        precision mediump float;
        uniform vec3 uLightDir;
        uniform vec3 uAmbient;
        uniform float uTextureMix;   // 0 = فقط رنگ رأس، 1 = فقط بافت
        uniform sampler2D uTexture;
        uniform float uAlpha;
        uniform vec3 uTint;
        uniform float uHighlight;    // پالس هایلایت
        varying vec3 vNormal;
        varying vec3 vColor;
        varying vec2 vUV;
        varying vec3 vWorldPos;
        void main() {
            vec3 n = normalize(vNormal);
            float diff = max(dot(n, normalize(uLightDir)), 0.0);
            float rim = pow(1.0 - max(dot(n, vec3(0.0, 1.0, 0.0)), 0.0), 3.0) * 0.05;
            vec3 lit = uAmbient + diff * 0.85 + rim;
            vec4 tex = texture2D(uTexture, vUV);
            vec3 base = mix(vColor * uTint, tex.rgb * uTint, uTextureMix);
            vec3 color = base * lit;
            color += vec3(0.35, 0.35, 0.1) * uHighlight * (0.5 + 0.5 * sin(uHighlight * 8.0));
            gl_FragColor = vec4(color, tex.a * uAlpha);
        }
    """

    var program = 0
    var uMVP = 0
    var uModel = 0
    var uNormalMat = 0
    var uLightDir = 0
    var uAmbient = 0
    var uTextureMix = 0
    var uTexture = 0
    var uAlpha = 0
    var uTint = 0
    var uHighlight = 0
    var aPos = 0
    var aNormal = 0
    var aColor = 0
    var aUV = 0

    fun init() {
        program = buildProgram(VERTEX_SRC, FRAGMENT_SRC)
        uMVP = GLES20.glGetUniformLocation(program, "uMVP")
        uModel = GLES20.glGetUniformLocation(program, "uModel")
        uNormalMat = GLES20.glGetUniformLocation(program, "uNormalMat")
        uLightDir = GLES20.glGetUniformLocation(program, "uLightDir")
        uAmbient = GLES20.glGetUniformLocation(program, "uAmbient")
        uTextureMix = GLES20.glGetUniformLocation(program, "uTextureMix")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        uAlpha = GLES20.glGetUniformLocation(program, "uAlpha")
        uTint = GLES20.glGetUniformLocation(program, "uTint")
        uHighlight = GLES20.glGetUniformLocation(program, "uHighlight")
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        aColor = GLES20.glGetAttribLocation(program, "aColor")
        aUV = GLES20.glGetAttribLocation(program, "aUV")
    }

    fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        val status = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(prog)
            GLES20.glDeleteProgram(prog)
            throw RuntimeException("خطای لینک شیدر: $log")
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("خطای کامپایل شیدر: $log")
        }
        return shader
    }
}
