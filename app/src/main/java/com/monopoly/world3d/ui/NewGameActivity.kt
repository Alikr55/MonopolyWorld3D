package com.monopoly.world3d.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.monopoly.world3d.R
import com.monopoly.world3d.MonopolyApp
import com.monopoly.world3d.engine.AiLevel
import com.monopoly.world3d.engine.AiPersonality
import com.monopoly.world3d.engine.GameRules
import com.monopoly.world3d.engine.GameState
import com.monopoly.world3d.engine.Names
import com.monopoly.world3d.engine.Player
import com.monopoly.world3d.engine.TokenKind

/**
 * تنظیم بازی جدید: تک‌نفره (۱ تا ۵ حریف رایانه با سطح دشواری) یا
 * چندنفره‌ی محلی (۲ تا ۶ بازیکن با گوشی مشترک).
 */
class NewGameActivity : android.app.Activity() {

    private var mode = "single"
    private var opponentCount = 3
    private var aiLevel = AiLevel.NORMAL
    private lateinit var playersContainer: LinearLayout
    private val humanNames = mutableListOf<String>()
    private var ruleParkingPot = true
    private var ruleAuctions = true
    private var ruleExactGoBonus = false
    private var ruleDoubleRent = true
    private val humanTokens = mutableListOf<Int>()
    private val humanColors = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra("mode") ?: "single"
        val app = application as MonopolyApp
        opponentCount = app.settings.lastOpponentCount
        aiLevel = AiLevel.entries[app.settings.lastAiLevel.coerceIn(0, 2)]

        val root = ScrollView(this)
        root.setBackgroundColor(0xFF101828.toInt())
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 24))
        root.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        fun title(text: String, size: Float = 20f): TextView {
            val t = TextView(this)
            t.text = text
            t.textSize = size
            t.setTextColor(0xFFFFD4A017.toInt())
            t.typeface = Ui.persianFont(this)
            t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6))
            return t
        }
        fun label(text: String): TextView {
            val t = TextView(this)
            t.text = text
            t.textSize = 15f
            t.setTextColor(0xFFF5EFE0.toInt())
            t.typeface = Ui.persianFont(this)
            t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4))
            return t
        }

        // عنوان
        val header = TextView(this)
        header.text = if (mode == "single") "بازی تک‌نفره" else "بازی چندنفره"
        header.textSize = 24f
        header.setTextColor(Color.WHITE)
        header.typeface = Ui.persianFont(this)
        header.gravity = Gravity.CENTER
        content.addView(header)

        if (mode == "single") {
            content.addView(label("تعداد حریفان رایانه:"))
            val oppRow = LinearLayout(this)
            oppRow.orientation = LinearLayout.HORIZONTAL
            var oppButtons = mutableListOf<Button>()
            for (n in 1..5) {
                val b = Button(this)
                b.text = com.monopoly.world3d.engine.PersianNumbers.number(n)
                b.typeface = Ui.persianFont(this)
                b.background = resources.getDrawable(R.drawable.btn_action, theme)
                b.setTextColor(Color.WHITE)
                val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f)
                lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
                oppRow.addView(b, lp)
                oppButtons.add(b)
                b.setOnClickListener {
                    opponentCount = n
                    refreshOppButtons(oppButtons)
                    rebuildPlayers(content)
                }
            }
            content.addView(oppRow)

            content.addView(label("سطح دشواری رایانه:"))
            val diffRow = LinearLayout(this)
            var diffButtons = mutableListOf<Button>()
            val levels = listOf(AiLevel.EASY to "آسان", AiLevel.NORMAL to "متوسط", AiLevel.HARD to "سخت")
            for ((lvl, name) in levels) {
                val b = Button(this)
                b.text = name
                b.typeface = Ui.persianFont(this)
                b.background = resources.getDrawable(R.drawable.btn_action, theme)
                b.setTextColor(Color.WHITE)
                val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f)
                lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
                diffRow.addView(b, lp)
                diffButtons.add(b)
                b.setOnClickListener {
                    aiLevel = lvl
                    app.settings.lastAiLevel = AiLevel.entries.indexOf(lvl)
                    refreshDiffButtons(diffButtons, levels)
                }
            }
            refreshDiffButtons(diffButtons, levels)
            content.addView(diffRow)
        } else {
            content.addView(label("تعداد بازیکنان:"))
            val countRow = LinearLayout(this)
            var countButtons = mutableListOf<Button>()
            for (n in 2..6) {
                val b = Button(this)
                b.text = com.monopoly.world3d.engine.PersianNumbers.number(n)
                b.typeface = Ui.persianFont(this)
                b.background = resources.getDrawable(R.drawable.btn_action, theme)
                b.setTextColor(Color.WHITE)
                val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f)
                lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
                countRow.addView(b, lp)
                countButtons.add(b)
                b.setOnClickListener {
                    humanCount = n
                    refreshCountButtons(countButtons)
                    rebuildPlayers(content)
                }
            }
            refreshCountButtons(countButtons)
            content.addView(countRow)
        }

        // قوانین سفارشی
        content.addView(label("قوانین سفارشی:"))
        val rulesPanel = LinearLayout(this)
        rulesPanel.orientation = LinearLayout.VERTICAL
        rulesPanel.background = getDrawable(R.drawable.panel_bg)
        rulesPanel.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8))
        fun ruleToggle(title: String, initial: Boolean, setter: (Boolean) -> Unit) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val t = TextView(this)
            t.text = title
            t.textSize = 13f
            t.typeface = Ui.persianFont(this)
            t.setTextColor(0xFFF5EFE0.toInt())
            t.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(t)
            val sw = android.widget.Switch(this)
            sw.isChecked = initial
            sw.setOnCheckedChangeListener { _, checked -> setter(checked) }
            row.addView(sw)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Ui.dp(this, 4)
            rulesPanel.addView(row, lp)
        }
        ruleToggle("صندوق پارکینگ آزاد (جریمه‌ها جمع می‌شود)", ruleParkingPot) { ruleParkingPot = it }
        ruleToggle("حراج ملک‌های ردشده", ruleAuctions) { ruleAuctions = it }
        ruleToggle("پاداش دوبرابر فرود دقیق روی شروع", ruleExactGoBonus) { ruleExactGoBonus = it }
        ruleToggle("اجاره‌ی دوبرابر با کل گروه رنگی", ruleDoubleRent) { ruleDoubleRent = it }
        content.addView(rulesPanel)

        content.addView(label("بازیکنان:"))
        playersContainer = LinearLayout(this)
        playersContainer.orientation = LinearLayout.VERTICAL
        content.addView(playersContainer)

        // دکمه شروع
        val start = Button(this)
        start.text = "شروع بازی"
        start.textSize = 18f
        start.typeface = Ui.persianFont(this)
        start.background = resources.getDrawable(R.drawable.btn_green, theme)
        start.setTextColor(Color.WHITE)
        val slp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 54))
        slp.topMargin = Ui.dp(this, 16)
        start.setOnClickListener { startGame() }
        content.addView(start, slp)

        val back = Button(this)
        back.text = "بازگشت"
        back.typeface = Ui.persianFont(this)
        back.background = resources.getDrawable(R.drawable.btn_action, theme)
        back.setTextColor(Color.WHITE)
        content.addView(back, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)))
        back.setOnClickListener { finish() }

        humanCount = if (mode == "single") 1 else 2
        rebuildPlayers(content)
        Ui.applyFont(root)
        setContentView(root)
    }

    private var humanCount = 1

    private fun refreshOppButtons(buttons: List<Button>) {
        buttons.forEachIndexed { i, b ->
            b.background = resources.getDrawable(
                if (i + 1 == opponentCount) R.drawable.btn_primary else R.drawable.btn_action, theme)
        }
        (application as MonopolyApp).settings.lastOpponentCount = opponentCount
    }

    private fun refreshCountButtons(buttons: List<Button>) {
        buttons.forEachIndexed { i, b ->
            b.background = resources.getDrawable(
                if (i + 2 == humanCount) R.drawable.btn_primary else R.drawable.btn_action, theme)
        }
    }

    private fun refreshDiffButtons(buttons: List<Button>, levels: List<Pair<AiLevel, String>>) {
        buttons.forEachIndexed { i, b ->
            b.background = resources.getDrawable(
                if (levels[i].first == aiLevel) R.drawable.btn_primary else R.drawable.btn_action, theme)
        }
    }

    private fun rebuildPlayers(content: LinearLayout) {
        playersContainer.removeAllViews()
        while (humanNames.size < humanCount) {
            humanNames.add(if (humanNames.isEmpty()) (application as MonopolyApp).settings.lastPlayerName else "")
            humanTokens.add((humanNames.size - 1) % TokenKind.entries.size)
            humanColors.add((humanNames.size - 1) % Names.COLORS.size)
        }
        while (humanNames.size > humanCount) {
            humanNames.removeAt(humanNames.size - 1)
            humanTokens.removeAt(humanTokens.size - 1)
            humanColors.removeAt(humanColors.size - 1)
        }
        for (i in 0 until humanCount) {
            playersContainer.addView(playerEditor(i))
        }
    }

    private fun playerEditor(index: Int): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setBackgroundResource(R.drawable.panel_bg)
        card.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = Ui.dp(this, 6)
        card.layoutParams = lp

        val nameLabel = TextView(this)
        nameLabel.text = "بازیکن ${index + 1}"
        nameLabel.textSize = 14f
        nameLabel.setTextColor(0xFFFFD4A017.toInt())
        nameLabel.typeface = Ui.persianFont(this)
        card.addView(nameLabel)

        val nameEdit = EditText(this)
        nameEdit.hint = "نام بازیکن ${index + 1}"
        nameEdit.setText(humanNames[index].ifEmpty { "" })
        nameEdit.textSize = 14f
        nameEdit.typeface = Ui.persianFont(this)
        nameEdit.setTextColor(Color.WHITE)
        nameEdit.setSingleLine(true)
        nameEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                humanNames[index] = s?.toString() ?: ""
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        card.addView(nameEdit)

        // انتخاب مهره
        val tokenLabel = TextView(this)
        tokenLabel.text = "مهره:"
        tokenLabel.textSize = 13f
        tokenLabel.typeface = Ui.persianFont(this)
        card.addView(tokenLabel)
        val tokenRow = HorizontalScrollView(this)
        val tokenInner = LinearLayout(this)
        tokenInner.orientation = LinearLayout.HORIZONTAL
        TokenKind.entries.forEachIndexed { ti, kind ->
            val chip = TextView(this)
            chip.text = kind.faName
            chip.textSize = 12f
            chip.typeface = Ui.persianFont(this)
            chip.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6))
            chip.setTextColor(Color.WHITE)
            chip.background = resources.getDrawable(
                if (humanTokens[index] == ti) R.drawable.btn_primary else R.drawable.btn_action, theme)
            val clp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            clp.marginEnd = Ui.dp(this, 4)
            chip.layoutParams = clp
            chip.setOnClickListener {
                humanTokens[index] = ti
                rebuildPlayersInline()
            }
            tokenInner.addView(chip)
        }
        tokenRow.addView(tokenInner)
        card.addView(tokenRow)

        // انتخاب رنگ
        val colorLabel = TextView(this)
        colorLabel.text = "رنگ:"
        colorLabel.textSize = 13f
        colorLabel.typeface = Ui.persianFont(this)
        card.addView(colorLabel)
        val colorRow = LinearLayout(this)
        colorRow.orientation = LinearLayout.HORIZONTAL
        Names.COLORS.forEachIndexed { ci, colorLong ->
            val dot = ImageView(this)
            val c = colorLong.toInt()
            dot.setBackgroundColor(c)
            dot.setOnClickListener {
                humanColors[index] = ci
                rebuildPlayersInline()
            }
            val dlp = LinearLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30))
            dlp.marginEnd = Ui.dp(this, 6)
            dot.layoutParams = dlp
            if (humanColors[index] == ci) {
                dot.background = resources.getDrawable(R.drawable.btn_primary, theme)
            }
            colorRow.addView(dot)
        }
        card.addView(colorRow)
        return card
    }

    private fun rebuildPlayersInline() {
        playersContainer.removeAllViews()
        for (i in 0 until humanCount) {
            playersContainer.addView(playerEditor(i))
        }
    }

    private fun startGame() {
        val app = application as MonopolyApp
        val players = mutableListOf<Player>()
        val usedTokens = mutableSetOf<Int>()
        val usedColors = mutableSetOf<Int>()
        for (i in 0 until humanCount) {
            val name = humanNames[i].trim().ifEmpty { "بازیکن ${i + 1}" }
            var token = humanTokens[i]
            while (token in usedTokens) token = (token + 1) % TokenKind.entries.size
            usedTokens.add(token)
            var color = humanColors[i]
            while (color in usedColors) color = (color + 1) % Names.COLORS.size
            usedColors.add(color)
            players.add(Player(i, name, TokenKind.entries[token], Names.COLORS[color].toLong(), isHuman = true))
        }
        if (mode == "single") {
            val aiNames = Names.AI_NAMES.shuffled(java.util.Random())
            for (j in 0 until opponentCount) {
                val id = players.size
                players.add(Player(
                    id, aiNames[j],
                    TokenKind.entries[(id * 3 + 2) % TokenKind.entries.size],
                    Names.COLORS[(id * 2 + 3) % Names.COLORS.size].toLong(),
                    isHuman = false, aiLevel = aiLevel,
                    personality = Names.personalityFor(id)
                ))
            }
            app.settings.lastPlayerName = players[0].name
        }
        if (players.size < 2) {
            Ui.toast(this, "حداقل دو بازیکن لازم است")
            return
        }

        val rules = GameRules(
            freeParkingPot = ruleParkingPot,
            auctionsEnabled = ruleAuctions,
            doubleSalaryOnExactGo = ruleExactGoBonus,
            doubleRentOnMonopoly = ruleDoubleRent
        )
        val state = GameState(players.toMutableList(), rules)
        // مقداردهی اولیه‌ی کامل بازی: موجودی ۱٫۵ میلیون تومانی، دسته‌ی کارت‌ها و وضعیت آغازین
        com.monopoly.world3d.engine.Engine(state).startGame()
        GameSession.state = state
        GameSession.isSinglePlayer = (mode == "single")
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }
}
