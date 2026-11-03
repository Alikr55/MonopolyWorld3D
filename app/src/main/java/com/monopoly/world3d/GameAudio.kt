package com.monopoly.world3d

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * مدیریت صدا: جلوه‌های صوتی (SoundPool) و موسیقی پس‌زمینه (MediaPlayer).
 * همه‌ی فایل‌های صوتی به صورت رویه‌ای تولید شده‌اند و در assets قرار دارند.
 */
class GameAudio(private val context: Context, private val settings: SettingsManager) {

    companion object {
        const val SFX_CLICK = "click"
        const val SFX_DICE = "dice"
        const val SFX_CASH = "cash"
        const val SFX_BUY = "buy"
        const val SFX_BUILD = "build"
        const val SFX_CARD = "card"
        const val SFX_JAIL = "jail"
        const val SFX_AUCTION = "auction"
        const val SFX_WIN = "win"
        const val SFX_LOSE = "lose"
        const val SFX_STEP = "step"
        const val SFX_ERROR = "error"
    }

    private var soundPool: SoundPool? = null
    private val soundIds = HashMap<String, Int>()
    private var music: MediaPlayer? = null

    fun load() {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        for (name in listOf(SFX_CLICK, SFX_DICE, SFX_CASH, SFX_BUY, SFX_BUILD, SFX_CARD,
                SFX_JAIL, SFX_AUCTION, SFX_WIN, SFX_LOSE, SFX_STEP, SFX_ERROR)) {
            try {
                val afd = context.assets.openFd("sfx/$name.wav")
                val id = soundPool!!.load(afd, 1)
                soundIds[name] = id
                afd.close()
            } catch (e: Exception) {
                // اگر فایلی نبود، بی‌صدا ادامه می‌دهیم
            }
        }
    }

    fun playSfx(name: String) {
        val sp = soundPool ?: return
        val id = soundIds[name] ?: return
        val vol = settings.sfxVolume / 100f
        if (vol <= 0f) return
        sp.play(id, vol, vol, 1, 0, 1f)
    }

    fun startMusic() {
        if (music != null) return
        try {
            val afd = context.assets.openFd("music/theme.wav")
            music = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setVolume(settings.musicVolume / 100f * 0.5f, settings.musicVolume / 100f * 0.5f)
                prepare()
                start()
            }
            afd.close()
        } catch (e: Exception) {
            music = null
        }
    }

    fun updateMusicVolume() {
        val v = settings.musicVolume / 100f * 0.5f
        music?.setVolume(v, v)
    }

    fun pauseMusic() { music?.takeIf { it.isPlaying }?.pause() }

    fun resumeMusic() { music?.takeIf { !it.isPlaying }?.start() }

    fun vibrate(ms: Long = 30) {
        if (!settings.vibration) return
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!v.hasVibrator()) return
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) { /* بی‌اهمیت */ }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        music?.release()
        music = null
    }
}
