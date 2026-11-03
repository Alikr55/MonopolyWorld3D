package com.monopoly.world3d.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.monopoly.world3d.R
import com.monopoly.world3d.MonopolyApp
import com.monopoly.world3d.engine.PersianNumbers
import com.monopoly.world3d.engine.Engine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** صفحه‌ی ذخیره/بارگذاری: ۳ اسلات دستی با حذف و بارگذاری */
class SavesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonopolyApp
        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val title = TextView(this)
        title.text = "ذخیره و بارگذاری"
        title.textSize = 22f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.gravity = android.view.Gravity.CENTER
        content.addView(title)

        val fmt = SimpleDateFormat("yy/MM/dd — HH:mm", Locale.getDefault())
        for (slot in 0 until com.monopoly.world3d.SavesStore.SLOTS) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.background = getDrawable(R.drawable.panel_bg)
            card.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Ui.dp(this, 8)
            val summary = app.saves.summary(slot)
            val t = TextView(this)
            if (summary == null) {
                t.text = "اسلات ${PersianNumbers.number(slot + 1)} — خالی"
            } else {
                val humans = summary.humanNames.joinToString("، ")
                t.text = "اسلات ${PersianNumbers.number(slot + 1)} — دور ${PersianNumbers.number(summary.round)}\n" +
                        (if (humans.isNotEmpty()) "$humans — " else "") +
                        fmt.format(Date(summary.savedAt)) + "\n" +
                        summary.players.joinToString("، ")
            }
            t.textSize = 13f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(Color.WHITE)
            card.addView(t)

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val load = Button(this)
            load.text = "بارگذاری"
            load.textSize = 12f
            load.typeface = Ui.persianFont(this)
            load.setTextColor(Color.WHITE)
            load.background = getDrawable(R.drawable.btn_green)
            load.isEnabled = summary != null
            load.setOnClickListener {
                val state = app.saves.load(slot)
                if (state == null) {
                    Ui.toast(this, "فایل ذخیره خراب است یا وجود ندارد")
                } else {
                    GameSession.state = state
                    GameSession.isSinglePlayer = state.players.any { !it.isHuman }
                    Engine(state) // بررسی سلامت
                    startActivity(Intent(this, GameActivity::class.java))
                }
            }
            val dlp = LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f)
            dlp.setMargins(Ui.dp(this, 3), Ui.dp(this, 4), Ui.dp(this, 3), 0)
            row.addView(load, dlp)

            val del = Button(this)
            del.text = "حذف"
            del.textSize = 12f
            del.typeface = Ui.persianFont(this)
            del.setTextColor(Color.WHITE)
            del.background = getDrawable(R.drawable.btn_danger)
            del.isEnabled = summary != null
            del.setOnClickListener {
                if (app.saves.delete(slot)) {
                    Ui.toast(this, "حذف شد")
                    recreate()
                }
            }
            row.addView(del, dlp)
            card.addView(row)
            content.addView(card, lp)
        }

        val back = Button(this)
        back.text = "بازگشت"
        back.typeface = Ui.persianFont(this)
        back.setTextColor(Color.WHITE)
        back.background = getDrawable(R.drawable.btn_action)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46))
        lp.topMargin = Ui.dp(this, 14)
        content.addView(back, lp)
        back.setOnClickListener { finish() }
        Ui.applyFont(root)
        setContentView(root)
    }
}
