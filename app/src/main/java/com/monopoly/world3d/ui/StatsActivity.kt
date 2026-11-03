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

/** صفحه‌ی آمار بازی */
class StatsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonopolyApp
        val s = app.stats.stats

        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val title = TextView(this)
        title.text = "آمار بازی"
        title.textSize = 24f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.gravity = android.view.Gravity.CENTER
        content.addView(title)

        fun row(label: String, value: String) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.HORIZONTAL
            card.background = getDrawable(R.drawable.panel_bg)
            card.setPadding(Ui.dp(this, 10), Ui.dp(this, 7), Ui.dp(this, 10), Ui.dp(this, 7))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Ui.dp(this, 5)
            val l = TextView(this)
            l.text = label
            l.textSize = 14f
            l.typeface = Ui.persianFont(this)
            l.setTextColor(0xFFC8BFA8.toInt())
            l.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            card.addView(l)
            val v = TextView(this)
            v.text = value
            v.textSize = 14f
            v.typeface = Ui.persianFont(this)
            v.setTextColor(Color.WHITE)
            card.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            content.addView(card, lp)
        }

        val winRate = if (s.gamesPlayed > 0) (s.gamesWon * 100 / s.gamesPlayed) else 0
        row("بازی‌های انجام‌شده", PersianNumbers.number(s.gamesPlayed))
        row("بردها", PersianNumbers.number(s.gamesWon))
        row("باخت‌ها", PersianNumbers.number(s.gamesLost))
        row("نرخ برد", "٪" + PersianNumbers.number(winRate))
        row("بردهای قاطع (۲+ ورشکستگی)", PersianNumbers.number(s.perfectWins))
        row("بردها در سطح سخت", PersianNumbers.number(s.hardWins))
        row("ملک‌های خریده‌شده", PersianNumbers.number(s.propertiesBought))
        row("خانه‌های ساخته‌شده", PersianNumbers.number(s.housesBuilt))
        row("هتل‌های ساخته‌شده", PersianNumbers.number(s.hotelsBuilt))
        row("گروه‌های رنگی کامل‌شده", PersianNumbers.number(s.groupsCompleted))
        row("اجاره‌ی دریافتی", PersianNumbers.moneyShort(s.rentCollected))
        row("اجاره‌ی پرداختی", PersianNumbers.moneyShort(s.rentPaid))
        row("مالیات پرداختی", PersianNumbers.moneyShort(s.taxesPaid))
        row("معامله‌ها", PersianNumbers.number(s.tradesDone))
        row("حراج‌های برده‌شده", PersianNumbers.number(s.auctionsWon))
        row("کارت‌های کشیده‌شده", PersianNumbers.number(s.cardsDrawn))
        row("بازدید از زندان", PersianNumbers.number(s.timesInJail))
        row("فرار از زندان", PersianNumbers.number(s.jailEscapes))
        row("تاس جفت", PersianNumbers.number(s.doublesRolled))
        row("گذر از شروع", PersianNumbers.number(s.goPasses))
        row("بیشترین ارزش خالص", PersianNumbers.moneyShort(s.maxNetWorth))
        row("سریع‌ترین برد", if (s.fastestWinMillis > 0) "${PersianNumbers.number(s.fastestWinMillis / 60000)} دقیقه" else "—")
        row("طولانی‌ترین بازی", if (s.longestGameMillis > 0) "${PersianNumbers.number(s.longestGameMillis / 60000)} دقیقه" else "—")
        row("مجموع زمان بازی", "${PersianNumbers.number(s.totalPlayMillis / 60000)} دقیقه")

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
