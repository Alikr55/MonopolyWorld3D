package com.monopoly.world3d.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.monopoly.world3d.R
import com.monopoly.world3d.MonopolyApp
import com.monopoly.world3d.engine.AiLevel
import com.monopoly.world3d.engine.GameResultContext
import com.monopoly.world3d.engine.PersianNumbers

/**
 * صفحه‌ی پایان بازی: برنده، رتبه‌بندی نهایی، آمار بازیکنان و دکمه‌های ادامه.
 */
class ResultsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MonopolyApp
        val state = GameSession.state
        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 20))
        root.addView(content)

        if (state == null) {
            val t = TextView(this)
            t.text = "اطلاعاتی برای نمایش نیست"
            t.setTextColor(Color.WHITE)
            t.typeface = Ui.persianFont(this)
            content.addView(t)
            setContentView(root)
            return
        }

        val winner = if (state.winnerId >= 0) state.players[state.winnerId] else state.activePlayers().firstOrNull()
        val humanWon = winner != null && winner.isHuman && GameSession.isSinglePlayer

        val header = TextView(this)
        header.text = if (humanWon) "🏆 پیروزی!" else if (GameSession.isSinglePlayer) "💀 شکست" else "پایان بازی"
        header.textSize = 30f
        header.typeface = Ui.persianFont(this)
        header.setTextColor(if (humanWon) 0xFFFFD700.toInt() else 0xFFFF6B6B.toInt())
        header.gravity = Gravity.CENTER
        content.addView(header)

        val winnerLabel = TextView(this)
        winnerLabel.text = "برنده: ${winner?.name ?: "—"} ${winner?.let { "با دارایی ${PersianNumbers.money(it.balance)}" } ?: ""}"
        winnerLabel.textSize = 17f
        winnerLabel.typeface = Ui.persianFont(this)
        winnerLabel.setTextColor(Color.WHITE)
        winnerLabel.gravity = Gravity.CENTER
        winnerLabel.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 6))
        content.addView(winnerLabel)

        // مدت بازی
        val duration = System.currentTimeMillis() - state.gameStartedAtMillis
        val minutes = duration / 60000
        val seconds = (duration % 60000) / 1000
        content.addView(infoCard("مدت بازی", "${PersianNumbers.number(minutes)} دقیقه و ${PersianNumbers.number(seconds)} ثانیه — ${PersianNumbers.number(state.roundCount)} دور"))

        // رتبه‌بندی
        val title = TextView(this)
        title.text = "رتبه‌بندی نهایی (بر اساس ارزش خالص)"
        title.textSize = 16f
        title.typeface = Ui.persianFont(this)
        title.setTextColor(0xFFFFD4A017.toInt())
        title.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4))
        content.addView(title)

        val ranked = state.players.filter { !it.bankrupt }.sortedByDescending { state.netWorth(it.id) } +
                state.players.filter { it.bankrupt }
        for ((i, p) in ranked.withIndex()) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.background = getDrawable(R.drawable.panel_bg)
            card.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Ui.dp(this, 4)
            val rank = TextView(this)
            val st = state.statsFor(p.id)
            rank.text = "${PersianNumbers.number(i + 1)}. ${p.name}${if (p.bankrupt) " (ورشکسته)" else ""} — ارزش خالص: ${PersianNumbers.money(state.netWorth(p.id))}"
            rank.textSize = 14f
            rank.typeface = Ui.persianFont(this)
            rank.setTextColor(if (winner?.id == p.id) 0xFFFFD700.toInt() else Color.WHITE)
            card.addView(rank)
            val detail = TextView(this)
            detail.text = "ملک: ${PersianNumbers.number(st.propertiesBought + state.ownedBy(p.id).size)} | خانه: ${PersianNumbers.number(st.housesBuilt)} | هتل: ${PersianNumbers.number(st.hotelsBuilt)} | اجاره‌ی دریافتی: ${PersianNumbers.moneyShort(st.rentCollected)} | معامله: ${PersianNumbers.number(st.tradesDone)}"
            detail.textSize = 11f
            detail.typeface = Ui.persianFont(this)
            detail.setTextColor(0xFFC8BFA8.toInt())
            card.addView(detail)
            content.addView(card, lp)
        }

        // ثبت آمار و دستاوردها (فقط تک‌نفره)
        if (GameSession.isSinglePlayer) {
            val human = state.players[0]
            val hardestAi = state.players.filter { !it.isHuman }.maxOfOrNull { it.aiLevel } ?: AiLevel.NORMAL
            var groupsCompleted = 0
            for (g in listOf(
                com.monopoly.world3d.engine.Group.BROWN, com.monopoly.world3d.engine.Group.LIGHT_BLUE,
                com.monopoly.world3d.engine.Group.PINK, com.monopoly.world3d.engine.Group.ORANGE,
                com.monopoly.world3d.engine.Group.RED, com.monopoly.world3d.engine.Group.YELLOW,
                com.monopoly.world3d.engine.Group.GREEN, com.monopoly.world3d.engine.Group.DARK_BLUE)) {
                val ids = state.properties.indices.filter { state.space(it).group == g }
                if (ids.isNotEmpty() && ids.all { state.prop(it).owner == 0 }) groupsCompleted++
            }
            val ctx = GameResultContext(
                winnerId = winner?.id ?: -1,
                humanWon = humanWon,
                playerCount = state.players.size,
                hardestAiLevel = hardestAi,
                durationMillis = duration,
                bankruptCount = state.players.count { it.bankrupt },
                potCollected = 0,
                humanNetWorth = state.netWorth(0),
                humanStats = state.statsFor(0),
                groupsCompletedByHuman = groupsCompleted
            )
            val newly = app.stats.applyGameResult(ctx)
            if (newly.isNotEmpty()) {
                val achTitle = TextView(this)
                achTitle.text = "🎖 دستاوردهای تازه:"
                achTitle.textSize = 15f
                achTitle.typeface = Ui.persianFont(this)
                achTitle.setTextColor(0xFFFFD4A017.toInt())
                achTitle.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4))
                content.addView(achTitle)
                for (a in newly) {
                    content.addView(infoCard(a.title, a.description))
                }
            }
        }

        // دکمه‌ها
        content.addView(View(this), LinearLayout.LayoutParams(1, Ui.dp(this, 14)))
        fun bigButton(text: String, bg: Int, onClick: () -> Unit) {
            val b = Button(this)
            b.text = text
            b.textSize = 16f
            b.typeface = Ui.persianFont(this)
            b.setTextColor(Color.WHITE)
            b.background = getDrawable(bg)
            b.isAllCaps = false
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48))
            lp.topMargin = Ui.dp(this, 6)
            content.addView(b, lp)
            b.setOnClickListener { onClick() }
        }
        bigButton("آمار بازی", R.drawable.btn_action) {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        bigButton("بازی دوباره", R.drawable.btn_green) {
            startActivity(Intent(this, NewGameActivity::class.java))
            finish()
        }
        bigButton("منوی اصلی", R.drawable.btn_primary) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        Ui.applyFont(root)
        setContentView(root)
    }

    private fun infoCard(title: String, value: String): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = getDrawable(R.drawable.panel_bg)
        card.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = Ui.dp(this, 6)
        val t = TextView(this)
        t.text = title
        t.textSize = 14f
        t.typeface = Ui.persianFont(this)
        t.setTextColor(Color.WHITE)
        card.addView(t)
        val v = TextView(this)
        v.text = value
        v.textSize = 12f
        v.typeface = Ui.persianFont(this)
        v.setTextColor(0xFFC8BFA8.toInt())
        card.addView(v)
        card.layoutParams = lp
        return card
    }
}
