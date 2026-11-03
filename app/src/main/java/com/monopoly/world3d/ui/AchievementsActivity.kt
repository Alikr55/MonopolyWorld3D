package com.monopoly.world3d.ui

import android.app.Activity
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
import com.monopoly.world3d.engine.Achievements

/** صفحه‌ی دستاوردها */
class AchievementsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonopolyApp
        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val unlockedCount = app.stats.unlocked.size
        val title = TextView(this)
        title.text = "دستاوردها (${PersianNumbers.number(unlockedCount)} از ${PersianNumbers.number(Achievements.ALL.size)})"
        title.textSize = 22f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.gravity = android.view.Gravity.CENTER
        content.addView(title)

        for (a in Achievements.ALL) {
            val got = a.id in app.stats.unlocked
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.background = getDrawable(R.drawable.panel_bg)
            card.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Ui.dp(this, 6)
            val t = TextView(this)
            t.text = (if (got) "🏆 " else "🔒 ") + a.title
            t.textSize = 15f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(if (got) 0xFFFFD700.toInt() else 0xFFAAAAAA.toInt())
            card.addView(t)
            val d = TextView(this)
            d.text = a.description
            d.textSize = 12f
            d.typeface = Ui.persianFont(this)
            d.setTextColor(0xFFC8BFA8.toInt())
            card.addView(d)
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
