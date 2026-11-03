package com.monopoly.world3d.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.monopoly.world3d.MonopolyApp
import com.monopoly.world3d.R
import com.monopoly.world3d.engine.Cards
import com.monopoly.world3d.engine.DecisionKind
import com.monopoly.world3d.engine.Engine
import com.monopoly.world3d.engine.GameState
import com.monopoly.world3d.engine.PersianNumbers
import com.monopoly.world3d.render.GameSurfaceView

/**
 * صفحه‌ی اصلی بازی: صحنه‌ی سه‌بعدی + HUD فارسی + همه‌ی دیالوگ‌ها.
 * جریان نوبت: انسان با دکمه‌ها و هوش مصنوعی با گام‌های زمان‌بندی‌شده پیش می‌رود.
 */
class GameActivity : Activity() {

    private lateinit var state: GameState
    private lateinit var engine: Engine
    private lateinit var surface: GameSurfaceView
    private val handler = Handler(Looper.getMainLooper())
    private val app get() = application as MonopolyApp

    private var currentDialog: AlertDialog? = null
    private var auctionHolder: Dialogs.AuctionDialogHolder? = null
    private var animating = false
    private var finished = false

    // HUD
    private lateinit var playersRow: LinearLayout
    private lateinit var turnLabel: TextView
    private lateinit var logTicker: TextView
    private lateinit var diceButton: Button
    private lateinit var endTurnButton: Button
    private lateinit var tradeButton: Button
    private lateinit var buildButton: Button
    private lateinit var manageButton: Button
    private lateinit var pauseButton: Button
    private lateinit var jailButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loaded = if (intent.getBooleanExtra("loadAuto", false)) app.saves.loadAuto() else GameSession.state
        if (loaded == null) {
            Ui.toast(this, "بازی‌ای برای بارگذاری وجود ندارد")
            finish()
            return
        }
        state = loaded
        engine = Engine(state)
        GameSession.state = state
        GameSession.isSinglePlayer = state.players.any { !it.isHuman }
        buildUi()
        surface.renderer.syncState(state)
        updateHud()
        handler.postDelayed({
            surface.renderer.animateDice(state.dice1, state.dice2) {}
            handlePending()
            scheduleAi()
        }, 350)
    }

    // ---------------- ساخت رابط ----------------

    private fun buildUi() {
        val root = FrameLayout(this)
        surface = GameSurfaceView(this, app.settings.graphicsQuality)
        surface.setTouchEnabled(true)
        root.addView(surface, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // پنل بازیکنان (بالا)
        val topPanel = LinearLayout(this)
        topPanel.orientation = LinearLayout.VERTICAL
        topPanel.background = getDrawable(R.drawable.panel_bg)
        topPanel.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6))
        val tp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tp.gravity = Gravity.TOP
        tp.marginStart = Ui.dp(this, 8); tp.marginEnd = Ui.dp(this, 8); tp.topMargin = Ui.dp(this, 6)
        root.addView(topPanel, tp)

        turnLabel = TextView(this)
        turnLabel.textSize = 14f
        turnLabel.typeface = Ui.persianFont(this)
        turnLabel.setTextColor(0xFFFFD4A017.toInt())
        turnLabel.gravity = Gravity.CENTER
        topPanel.addView(turnLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        playersRow = LinearLayout(this)
        playersRow.orientation = LinearLayout.HORIZONTAL
        playersRow.gravity = Gravity.CENTER
        topPanel.addView(playersRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // گزارش رویدادها (پایین نوار)
        logTicker = TextView(this)
        logTicker.textSize = 12f
        logTicker.typeface = Ui.persianFont(this)
        logTicker.setTextColor(0xFFF5EFE0.toInt())
        logTicker.background = getDrawable(R.drawable.panel_bg)
        logTicker.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4))
        logTicker.maxLines = 2
        val lt = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lt.gravity = Gravity.BOTTOM
        lt.marginStart = Ui.dp(this, 8); lt.marginEnd = Ui.dp(this, 8)
        lt.bottomMargin = Ui.dp(this, 108)
        root.addView(logTicker, lt)

        // نوار پایین: دو ردیف دکمه
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.VERTICAL
        bar.background = getDrawable(R.drawable.panel_bg)
        bar.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6))
        val bp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bp.gravity = Gravity.BOTTOM
        bp.marginStart = Ui.dp(this, 6); bp.marginEnd = Ui.dp(this, 6); bp.bottomMargin = Ui.dp(this, 6)
        root.addView(bar, bp)

        fun barButton(label: String, bg: Int, onClick: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.textSize = 13f
            b.typeface = Ui.persianFont(this)
            b.setTextColor(Color.WHITE)
            b.background = getDrawable(bg)
            b.isAllCaps = false
            b.setPadding(0, 0, 0, 0)
            val lp = LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f)
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
            b.layoutParams = lp
            b.setOnClickListener { if (!animating) { app.audio.playSfx("click"); onClick() } }
            return b
        }

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        diceButton = barButton("🎲 بریز تاس", R.drawable.btn_primary) { onDicePressed() }
        diceButton.textSize = 16f
        row1.addView(diceButton)
        endTurnButton = barButton("پایان نوبت", R.drawable.btn_green) { onEndTurnPressed() }
        row1.addView(endTurnButton)
        jailButton = barButton("زندان", R.drawable.btn_action) { if (state.awaiting == DecisionKind.JAIL_DECISION) handlePending(true) }
        row1.addView(jailButton)
        pauseButton = barButton("مکث ⏸", R.drawable.btn_action) { showPauseDialog() }
        row1.addView(pauseButton)
        bar.addView(row1)

        val row2 = LinearLayout(this)
        row2.orientation = LinearLayout.HORIZONTAL
        tradeButton = barButton("معامله", R.drawable.btn_action) { openTradeFlow() }
        row2.addView(tradeButton)
        buildButton = barButton("ساخت 🏠", R.drawable.btn_action) {
            Dialogs.showManage(this, engine, state.currentTurn, "build") { refreshAfterDialog() }
        }
        row2.addView(buildButton)
        manageButton = barButton("مدیریت املاک", R.drawable.btn_action) {
            showManagePicker()
        }
        row2.addView(manageButton)
        bar.addView(row2)

        Ui.applyFont(root)
        setContentView(root)
    }

    private fun showManagePicker() {
        val p = state.currentPlayer
        val panel = Dialogs.dialogPanel(this)
        panel.addView(Dialogs.dialogTitle(this, "مدیریت املاک", 18f))
        panel.addView(Dialogs.dialogText(this, "موجودی: ${PersianNumbers.money(p.balance)}", 13f, 0xFFC8BFA8.toInt()))
        panel.addView(Dialogs.dialogButton(this, "نمایش املاک و جزئیات", R.drawable.btn_action) {
            Dialogs.showManage(this, engine, state.currentTurn, "portfolio") { refreshAfterDialog() }
        })
        panel.addView(Dialogs.dialogButton(this, "فروش ساختمان", R.drawable.btn_danger) {
            Dialogs.showManage(this, engine, state.currentTurn, "sell") { refreshAfterDialog() }
        })
        panel.addView(Dialogs.dialogButton(this, "رهن گذاشتن ملک", R.drawable.btn_danger) {
            Dialogs.showManage(this, engine, state.currentTurn, "mortgage") { refreshAfterDialog() }
        })
        panel.addView(Dialogs.dialogButton(this, "آزادسازی رهن", R.drawable.btn_primary) {
            Dialogs.showManage(this, engine, state.currentTurn, "redeem") { refreshAfterDialog() }
        })
        panel.addView(Dialogs.dialogButton(this, "بستن", R.drawable.btn_action) { })
        Dialogs.show(this, panel)
    }

    // ---------------- نوار وضعیت ----------------

    private fun updateHud() {
        // بازیکنان
        playersRow.removeAllViews()
        for (p in state.players) {
            val chip = LinearLayout(this)
            chip.orientation = LinearLayout.VERTICAL
            chip.gravity = Gravity.CENTER
            chip.setPadding(Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2))
            chip.background = getDrawable(
                if (p.id == state.currentTurn && !p.bankrupt) R.drawable.btn_primary else R.drawable.panel_bg)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
            val dot = View(this)
            dot.background = Dialogs.buildGradientCircle(p.colorHex.toInt())
            dot.minimumWidth = Ui.dp(this, 12); dot.minimumHeight = Ui.dp(this, 12)
            chip.addView(dot, LinearLayout.LayoutParams(Ui.dp(this, 12), Ui.dp(this, 12)))
            val name = TextView(this)
            name.text = p.name + if (p.isHuman) "" else " (ر)"
            name.textSize = 11f
            name.typeface = Ui.persianFont(this)
            name.setTextColor(if (p.bankrupt) 0xFF888888.toInt() else Color.WHITE)
            name.gravity = Gravity.CENTER
            chip.addView(name)
            val bal = TextView(this)
            bal.text = PersianNumbers.moneyShort(p.balance)
            bal.textSize = 11f
            bal.typeface = Ui.persianFont(this)
            bal.setTextColor(if (p.bankrupt) 0xFF888888.toInt() else 0xFFB8E986.toInt())
            bal.gravity = Gravity.CENTER
            chip.addView(bal)
            playersRow.addView(chip, lp)
        }
        // نوبت
        val phase = when (state.awaiting) {
            DecisionKind.BUY_PROMPT -> "— تصمیم خرید"
            DecisionKind.JAIL_DECISION -> "— در زندان"
            DecisionKind.AUCTION -> "— حراج!"
            DecisionKind.DEBT -> "— بدهکاری!"
            DecisionKind.TRADE_RESPONSE -> "— پیشنهاد معامله"
            DecisionKind.GAME_OVER -> "— پایان بازی"
            DecisionKind.NONE -> ""
        }
        turnLabel.text = "نوبت ${state.currentPlayer.name} — دور ${PersianNumbers.number(state.roundCount)} $phase" +
                if (state.rollAgain) " (تاس جفت! دوباره بریز)" else ""

        // دکمه‌ها
        val human = state.currentPlayer.isHuman
        diceButton.isEnabled = human && engine.canRoll() && !animating
        endTurnButton.isEnabled = human && engine.canEndTurn() && !animating
        val manageAllowed = human && !state.isGameOver && !animating &&
                (state.awaiting == DecisionKind.NONE || state.awaiting == DecisionKind.DEBT)
        buildButton.isEnabled = manageAllowed
        manageButton.isEnabled = manageAllowed
        tradeButton.isEnabled = human && !state.isGameOver && !animating && state.awaiting == DecisionKind.NONE
        jailButton.isEnabled = human && state.awaiting == DecisionKind.JAIL_DECISION

        // گزارش
        val recent = state.log.takeLast(2).joinToString("\n") { it.text }
        logTicker.text = recent
    }

    private fun refreshAfterDialog() {
        currentDialog = null
        syncAndAutosave()
        updateHud()
        handlePending()
        scheduleAi()
    }

    private fun syncAndAutosave() {
        surface.renderer.syncState(state)
        app.saves.save(com.monopoly.world3d.SavesStore.AUTO, state)
    }

    // ---------------- جریان نوبت انسان ----------------

    private fun onDicePressed() {
        if (!engine.canRoll()) return
        app.audio.playSfx("dice")
        val events = engine.rollDice()
        onRollResolved(events)
    }

    private fun onRollResolved(events: List<com.monopoly.world3d.engine.LogEntry>) {
        animating = true
        updateHud()
        val diceEnabled = app.settings.showDiceAnimation && app.settings.animations
        val runMove = {
            val path = state.lastMovePath
            val mover = state.currentPlayer
            if (path.isNotEmpty() && app.settings.animations) {
                surface.renderer.animateMove(path, mover.id, state.players.size) {
                    animating = false
                    afterMoveSegment()
                }
            } else {
                animating = false
                afterMoveSegment()
            }
        }
        if (diceEnabled) {
            surface.renderer.animateDice(state.dice1, state.dice2) { runMove() }
        } else runMove()
    }

    private fun afterMoveSegment() {
        syncAndAutosave()
        val cardId = state.lastDrawnCardId
        if (cardId >= 0) {
            state.lastDrawnCardId = -1
            val card = Cards.byId(cardId)
            app.audio.playSfx("card")
            surface.renderer.pulseProperty(state.currentPlayer.position)
            Dialogs.showCard(this, card) {
                refreshAfterDialog()
            }
            return
        }
        refreshAfterDialog()
    }

    private fun onEndTurnPressed() {
        engine.endTurn()
        syncAndAutosave()
        updateHud()
        handlePending()
        scheduleAi()
    }

    // ---------------- جریان هوش مصنوعی ----------------

    private val aiStepRunnable = Runnable { aiStep() }

    private fun scheduleAi() {
        if (finished || state.isGameOver) return
        if (state.awaiting == DecisionKind.TRADE_RESPONSE) return
        if (!com.monopoly.world3d.engine.AiDriver.isAiTurn(engine)) return
        if (animating) {
            // بعد از پایان انیمیشن دوباره برنامه‌ریزی می‌شود
            handler.postDelayed(aiStepRunnable, 150)
            return
        }
        if (currentDialog?.isShowing == true) return
        handler.removeCallbacks(aiStepRunnable)
        val delay = (550 * app.settings.gameSpeedFactor()).toLong()
        handler.postDelayed(aiStepRunnable, delay)
    }

    private fun aiStep() {
        if (finished || state.isGameOver) return
        if (!com.monopoly.world3d.engine.AiDriver.isAiTurn(engine)) {
            updateHud()
            return
        }
        val diceSigBefore = diceSignature()
        val cardBefore = state.lastDrawnCardId
        com.monopoly.world3d.engine.AiDriver.step(engine)
        val diceChanged = diceSignature() != diceSigBefore

        if (diceChanged && app.settings.animations) {
            animating = true
            app.audio.playSfx("dice")
            surface.renderer.animateDice(state.dice1, state.dice2) {
                val path = state.lastMovePath
                val mover = state.currentPlayer
                if (path.isNotEmpty()) {
                    surface.renderer.animateMove(path, mover.id, state.players.size) {
                        animating = false
                        aiAfterStep(cardBefore)
                    }
                } else {
                    animating = false
                    aiAfterStep(cardBefore)
                }
            }
        } else {
            aiAfterStep(cardBefore)
        }
    }

    private fun aiAfterStep(cardBefore: Int) {
        syncAndAutosave()
        // نمایش کارت کشیده‌شده توسط AI
        val cardId = state.lastDrawnCardId
        if (cardId >= 0 && cardId != cardBefore) {
            state.lastDrawnCardId = -1
            val card = Cards.byId(cardId)
            surface.renderer.pulseProperty(state.currentPlayer.position)
            Dialogs.showCard(this, card) {
                updateHud()
                handlePending()
                scheduleAi()
            }
            return
        }
        updateHud()
        handlePending()
        scheduleAi()
    }

    private fun diceSignature(): String =
        "${state.hasRolled}-${state.dice1}-${state.dice2}-${state.rollAgain}-${state.lastMovePath.size}-${state.currentPlayer.position}"

    // ---------------- مدیریت دیالوگ‌ها بر اساس وضعیت ----------------

    private fun handlePending(force: Boolean = false) {
        if (finished) return
        if (state.isGameOver) {
            showResults()
            return
        }
        // حراج: دیالوگ پایدار
        if (state.awaiting == DecisionKind.AUCTION && state.auction != null) {
            ensureAuctionDialog()
            Dialogs.updateAuctionInfo(auctionHolder!!, engine, this) { amount ->
                auctionHolder?.dialog?.dismiss()
                onChosenBid(amount)
            }
            return
        } else if (auctionHolder != null && state.auction == null) {
            auctionHolder?.dialog?.dismiss()
            auctionHolder = null
        }
        if (currentDialog?.isShowing == true && !force) return

        val human = state.currentPlayer.isHuman
        when (state.awaiting) {
            DecisionKind.BUY_PROMPT -> if (human) {
                currentDialog?.dismiss()
                currentDialog = Dialogs.showPurchase(this, engine) { refreshAfterDialog() }
            }
            DecisionKind.JAIL_DECISION -> if (human) {
                currentDialog?.dismiss()
                currentDialog = Dialogs.showJail(this, engine) {
                    // پس از تصمیم زندان، اگر تاس ریخته شد انیمیشن بده
                    refreshAfterDialogWithMove()
                }
            }
            DecisionKind.DEBT -> if (human) {
                currentDialog?.dismiss()
                currentDialog = Dialogs.showDebt(this, engine) { refreshAfterDialog() }
            }
            DecisionKind.TRADE_RESPONSE -> {
                currentDialog?.dismiss()
                currentDialog = Dialogs.showTradeResponse(this, engine) { refreshAfterDialog() }
            }
            else -> {}
        }
    }

    private fun refreshAfterDialogWithMove() {
        syncAndAutosave()
        updateHud()
        val path = state.lastMovePath
        if (path.isNotEmpty() && app.settings.animations) {
            animating = true
            surface.renderer.animateMove(path, state.currentPlayer.id, state.players.size) {
                animating = false
                afterMoveSegment()
            }
        } else {
            handlePending()
            scheduleAi()
        }
    }

    private fun onChosenBid(amount: Long) {
        val auction = state.auction ?: return
        val bidder = auction.currentBidder()
        if (bidder >= 0) {
            engine.auctionBid(bidder, amount)
            app.audio.playSfx("cash")
        }
        refreshAfterDialog()
    }

    private fun ensureAuctionDialog() {
        if (auctionHolder?.dialog?.isShowing == true) return
        currentDialog?.dismiss()
        auctionHolder = Dialogs.showAuction(this, engine,
            onHumanBid = { amount -> onChosenBid(amount) },
            onHumanPass = {
                val a = state.auction
                if (a != null) {
                    val bidder = a.currentBidder()
                    if (bidder >= 0) engine.auctionPass(bidder)
                }
                refreshAfterDialog()
            })
        currentDialog = auctionHolder?.dialog
    }

    // ---------------- معامله ----------------

    private fun openTradeFlow() {
        val me = state.currentTurn
        Dialogs.showTradeTarget(this, engine, me) { target ->
            Dialogs.showTradeBuilder(this, engine, Dialogs.TradeBuilderState(me, target)) {
                refreshAfterDialog()
            }
        }
    }

    // ---------------- مکث و ذخیره ----------------

    private fun showPauseDialog() {
        val panel = Dialogs.dialogPanel(this)
        panel.addView(Dialogs.dialogTitle(this, "مکث", 20f))
        panel.addView(Dialogs.dialogButton(this, "ادامه بازی", R.drawable.btn_primary) { })
        panel.addView(Dialogs.dialogButton(this, "ذخیره در اسلات", R.drawable.btn_action) { showSaveSlots() })
        panel.addView(Dialogs.dialogButton(this, "تنظیمات صدا", R.drawable.btn_action) {
            app.audio.updateMusicVolume()
            Ui.toast(this, "صدا از صفحه‌ی تنظیمات قابل تغییر است")
        })
        panel.addView(Dialogs.dialogButton(this, "خروج به منوی اصلی", R.drawable.btn_danger) {
            AlertDialog.Builder(this)
                .setTitle("خروج")
                .setMessage("بازی به‌صورت خودکار ذخیره می‌شود. خارج می‌شوید؟")
                .setPositiveButton("بله") { _, _ ->
                    app.saves.save(com.monopoly.world3d.SavesStore.AUTO, state)
                    finished = true
                    finish()
                }
                .setNegativeButton("خیر", null)
                .show()
        })
        Dialogs.show(this, panel)
    }

    private fun showSaveSlots() {
        val panel = Dialogs.dialogPanel(this)
        panel.addView(Dialogs.dialogTitle(this, "ذخیره‌ی بازی", 19f))
        for (slot in 0 until com.monopoly.world3d.SavesStore.SLOTS) {
            val summary = app.saves.summary(slot)
            val label = if (summary == null) "اسلات ${PersianNumbers.number(slot + 1)} — خالی"
            else "اسلات ${PersianNumbers.number(slot + 1)} — دور ${PersianNumbers.number(summary.round)} — ${summary.humanNames.joinToString("، ").ifEmpty { summary.players.first() }}"
            panel.addView(Dialogs.dialogButton(this, label, R.drawable.btn_action) {
                if (app.saves.save(slot, state)) {
                    Ui.toast(this, "در اسلات ${PersianNumbers.number(slot + 1)} ذخیره شد")
                } else {
                    Ui.toast(this, "ذخیره‌سازی ناموفق بود")
                }
            })
        }
        panel.addView(Dialogs.dialogButton(this, "بستن", R.drawable.btn_action) { })
        Dialogs.show(this, panel)
    }

    // ---------------- پایان بازی ----------------

    private fun showResults() {
        if (finished) return
        finished = true
        app.audio.playSfx(if (state.winnerId == 0 && state.players[0].isHuman) "win" else "lose")
        handler.postDelayed({
            startActivity(android.content.Intent(this, ResultsActivity::class.java))
            finish()
        }, 900)
    }

    // ---------------- چرخه‌ی حیات ----------------

    override fun onResume() {
        super.onResume()
        surface.onResume()
        scheduleAi()
    }

    override fun onPause() {
        super.onPause()
        surface.onPause()
        handler.removeCallbacks(aiStepRunnable)
        app.saves.save(com.monopoly.world3d.SavesStore.AUTO, state)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        showPauseDialog()
    }

    companion object {
        const val MSG_AI_STEP = 1001
    }
}
