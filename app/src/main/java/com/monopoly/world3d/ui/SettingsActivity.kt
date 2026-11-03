package com.monopoly.world3d.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import com.monopoly.world3d.R
import com.monopoly.world3d.MonopolyApp

/** تنظیمات: گرافیک، صدا، لرزش، انیمیشن، دوربین، سرعت بازی */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonopolyApp
        val s = app.settings

        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val title = TextView(this)
        title.text = "تنظیمات"
        title.textSize = 24f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.gravity = android.view.Gravity.CENTER
        content.addView(title)

        fun label(text: String): TextView {
            val t = TextView(this)
            t.text = text
            t.textSize = 15f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(0xFFF5EFE0.toInt())
            t.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4))
            return t
        }
        fun optionRow(options: List<String>, selected: Int, onPick: (Int) -> Unit): LinearLayout {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            options.forEachIndexed { i, name ->
                val b = Button(this)
                b.text = name
                b.textSize = 13f
                b.typeface = Ui.persianFont(this)
                b.setTextColor(Color.WHITE)
                b.isAllCaps = false
                b.background = getDrawable(if (i == selected) R.drawable.btn_primary else R.drawable.btn_action)
                val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f)
                lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
                row.addView(b, lp)
                b.setOnClickListener { onPick(i) }
            }
            return row
        }

        // گرافیک
        content.addView(label("کیفیت گرافیک (برای اعمال، از بازی خارج و دوباره وارد شوید):"))
        content.addView(optionRow(listOf("کم", "متوسط", "زیاد"), s.graphicsQuality) {
            s.graphicsQuality = it
            Toast.makeText(this, "کیفیت گرافیک ذخیره شد", Toast.LENGTH_SHORT).show()
        })

        // صدا
        content.addView(label("صدای موسیقی: ${s.musicVolume}٪"))
        val music = SeekBar(this)
        music.max = 100; music.progress = s.musicVolume
        music.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                s.musicVolume = p
                app.audio.updateMusicVolume()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        content.addView(music)

        content.addView(label("جلوه‌های صوتی: ${s.sfxVolume}٪"))
        val sfx = SeekBar(this)
        sfx.max = 100; sfx.progress = s.sfxVolume
        sfx.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                s.sfxVolume = p
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        content.addView(sfx)

        // لرزش و انیمیشن
        content.addView(label("لرزش:"))
        val vib = ToggleButton(this)
        vib.textOn = "روشن"; vib.textOff = "خاموش"
        vib.isChecked = s.vibration
        vib.typeface = Ui.persianFont(this)
        vib.setOnCheckedChangeListener { _, checked -> s.vibration = checked }
        content.addView(vib, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44)))

        content.addView(label("انیمیشن‌ها:"))
        val anim = ToggleButton(this)
        anim.textOn = "روشن"; anim.textOff = "خاموش"
        anim.isChecked = s.animations
        anim.typeface = Ui.persianFont(this)
        anim.setOnCheckedChangeListener { _, checked -> s.animations = checked }
        content.addView(anim, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44)))

        content.addView(label("دوربین خودکار (دنبال‌کردن مهره):"))
        val cam = ToggleButton(this)
        cam.textOn = "روشن"; cam.textOff = "خاموش"
        cam.isChecked = s.autoCamera
        cam.typeface = Ui.persianFont(this)
        cam.setOnCheckedChangeListener { _, checked -> s.autoCamera = checked }
        content.addView(cam, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44)))

        content.addView(label("انیمیشن تاس:"))
        val dice = ToggleButton(this)
        dice.textOn = "روشن"; dice.textOff = "خاموش"
        dice.isChecked = s.showDiceAnimation
        dice.typeface = Ui.persianFont(this)
        dice.setOnCheckedChangeListener { _, checked -> s.showDiceAnimation = checked }
        content.addView(dice, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44)))

        // سرعت
        content.addView(label("سرعت بازی:"))
        content.addView(optionRow(listOf("آرام", "معمولی", "سریع"), s.gameSpeed) { s.gameSpeed = it })

        // زبان
        content.addView(label("زبان بازی:"))
        val lang = TextView(this)
        lang.text = "فارسی (پیش‌فرض و اصلی)"
        lang.textSize = 14f
        lang.typeface = Ui.persianFont(this)
        lang.setTextColor(0xFFC8BFA8.toInt())
        content.addView(lang)

        val back = Button(this)
        back.text = "بازگشت"
        back.typeface = Ui.persianFont(this)
        back.setTextColor(Color.WHITE)
        back.background = getDrawable(R.drawable.btn_action)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46))
        lp.topMargin = Ui.dp(this, 16)
        content.addView(back, lp)
        back.setOnClickListener { finish() }
        Ui.applyFont(root)
        setContentView(root)
    }
}
