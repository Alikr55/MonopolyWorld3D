package com.monopoly.world3d.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.monopoly.world3d.MonopolyApp
import com.monopoly.world3d.R
import com.monopoly.world3d.render.GameSurfaceView

/**
 * منوی اصلی — با پس‌زمینه‌ی شهر سه‌بعدی متحرک و فونت فارسی.
 */
class MainActivity : android.app.Activity() {

    private lateinit var surface: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonopolyApp
        app.audio.load()
        app.audio.startMusic()

        val root = FrameLayout(this)
        root.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // پس‌زمینه‌ی سه‌بعدی شهر
        surface = GameSurfaceView(this, app.settings.graphicsQuality)
        surface.setTouchEnabled(true)
        surface.renderer.setMenuMode(true)
        root.addView(surface, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // پنل منو
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setBackgroundResource(R.drawable.panel_bg)
        panel.gravity = Gravity.CENTER
        val pw = Ui.dp(this, 210)
        val params = FrameLayout.LayoutParams(pw, FrameLayout.LayoutParams.WRAP_CONTENT)
        when {
            resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL ->
                params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            else -> params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        params.marginStart = Ui.dp(this, 18)
        panel.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14))

        val title = TextView(this)
        title.text = getString(R.string.app_name)
        title.textSize = 26f
        title.setTextColor(0xFFF5EFE0.toInt())
        title.gravity = Gravity.CENTER
        title.typeface = Ui.persianFont(this)
        panel.addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val subtitle = TextView(this)
        subtitle.text = getString(R.string.app_subtitle)
        subtitle.textSize = 11f
        subtitle.setTextColor(0xFFC8A96A.toInt())
        subtitle.gravity = Gravity.CENTER
        subtitle.typeface = Ui.persianFont(this)
        panel.addView(subtitle, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        panel.addView(View(this), LinearLayout.LayoutParams(1, Ui.dp(this, 10)))

        fun addBtn(text: String, action: (android.content.Context) -> Unit) {
            val b = Ui.menuButton(this, text) { action(this) }
            panel.addView(b)
        }

        addBtn(getString(R.string.menu_new_game)) { ctx ->
            app.audio.playSfx("click")
            startActivity(Intent(ctx, NewGameActivity::class.java).putExtra("mode", "single"))
        }
        addBtn(getString(R.string.menu_continue)) { ctx ->
            if (app.saves.exists(com.monopoly.world3d.SavesStore.AUTO)) {
                startActivity(Intent(ctx, GameActivity::class.java).putExtra("loadAuto", true))
            } else {
                Ui.toast(ctx, "بازی ذخیره‌شده‌ای وجود ندارد")
            }
        }
        addBtn("بارگذاری از اسلات") { ctx ->
            startActivity(Intent(ctx, SavesActivity::class.java))
        }
        addBtn(getString(R.string.menu_multi)) { ctx ->
            startActivity(Intent(ctx, NewGameActivity::class.java).putExtra("mode", "multi"))
        }
        addBtn(getString(R.string.menu_rules)) { ctx -> startActivity(Intent(ctx, RulesActivity::class.java)) }
        addBtn(getString(R.string.menu_achievements)) { ctx -> startActivity(Intent(ctx, AchievementsActivity::class.java)) }
        addBtn(getString(R.string.menu_stats)) { ctx -> startActivity(Intent(ctx, StatsActivity::class.java)) }
        addBtn(getString(R.string.menu_settings)) { ctx -> startActivity(Intent(ctx, SettingsActivity::class.java)) }
        addBtn(getString(R.string.menu_about)) { ctx -> startActivity(Intent(ctx, AboutActivity::class.java)) }
        addBtn(getString(R.string.menu_exit)) { _ ->
            AlertDialog.Builder(this)
                .setTitle("خروج")
                .setMessage("از بازی خارج می‌شوید؟")
                .setPositiveButton("بله") { _, _ -> finishAffinity() }
                .setNegativeButton("خیر", null)
                .show()
        }

        root.addView(panel)
        Ui.applyFont(root)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        surface.onResume()
        (application as MonopolyApp).audio.resumeMusic()
    }

    override fun onPause() {
        surface.onPause()
        (application as MonopolyApp).audio.pauseMusic()
        super.onPause()
    }
}
