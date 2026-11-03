package com.monopoly.world3d

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

/**
 * کلاس برنامه — نگهداری سرویس‌های سراسری: تنظیمات، صدا، آمار و دستاوردها.
 */
class MonopolyApp : Application() {

    lateinit var settings: SettingsManager
        private set
    lateinit var audio: GameAudio
        private set
    lateinit var stats: StatsStore
        private set
    lateinit var saves: SavesStore
        private set

    companion object {
        lateinit var instance: MonopolyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsManager(this)
        audio = GameAudio(this, settings)
        stats = StatsStore(this)
        saves = SavesStore(this)
    }
}

/** تنظیمات بازی — همه با مقادیر پیش‌فرض فارسی‌سازی‌شده */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("monopoly_settings", Context.MODE_PRIVATE)

    companion object {
        const val GFX_LOW = 0
        const val GFX_MEDIUM = 1
        const val GFX_HIGH = 2
    }

    var musicVolume: Int
        get() = prefs.getInt("music_volume", 60)
        set(v) { prefs.edit().putInt("music_volume", v.coerceIn(0, 100)).apply() }

    var sfxVolume: Int
        get() = prefs.getInt("sfx_volume", 80)
        set(v) { prefs.edit().putInt("sfx_volume", v.coerceIn(0, 100)).apply() }

    var vibration: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(v) { prefs.edit().putBoolean("vibration", v).apply() }

    var animations: Boolean
        get() = prefs.getBoolean("animations", true)
        set(v) { prefs.edit().putBoolean("animations", v).apply() }

    var graphicsQuality: Int
        get() = prefs.getInt("graphics_quality", GFX_MEDIUM)
        set(v) { prefs.edit().putInt("graphics_quality", v).apply() }

    var autoCamera: Boolean
        get() = prefs.getBoolean("auto_camera", true)
        set(v) { prefs.edit().putBoolean("auto_camera", v).apply() }

    var showDiceAnimation: Boolean
        get() = prefs.getBoolean("dice_animation", true)
        set(v) { prefs.edit().putBoolean("dice_animation", v).apply() }

    /** سرعت بازی: 0=آرام 1=معمولی 2=سریع */
    var gameSpeed: Int
        get() = prefs.getInt("game_speed", 1)
        set(v) { prefs.edit().putInt("game_speed", v).apply() }

    fun gameSpeedFactor(): Float = when (gameSpeed) {
        0 -> 1.5f
        1 -> 1.0f
        else -> 0.6f
    }

    var lastPlayerName: String
        get() = prefs.getString("last_player_name", "بازیکن") ?: "بازیکن"
        set(v) { prefs.edit().putString("last_player_name", v).apply() }

    var lastTokenIndex: Int
        get() = prefs.getInt("last_token", 0)
        set(v) { prefs.edit().putInt("last_token", v).apply() }

    var lastColorIndex: Int
        get() = prefs.getInt("last_color", 0)
        set(v) { prefs.edit().putInt("last_color", v).apply() }

    var lastOpponentCount: Int
        get() = prefs.getInt("last_opponents", 3)
        set(v) { prefs.edit().putInt("last_opponents", v).apply() }

    var lastAiLevel: Int
        get() = prefs.getInt("last_ai_level", 1)
        set(v) { prefs.edit().putInt("last_ai_level", v).apply() }
}
