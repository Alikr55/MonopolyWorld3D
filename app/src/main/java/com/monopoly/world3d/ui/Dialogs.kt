package com.monopoly.world3d.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.monopoly.world3d.MonopolyApp
import com.monopoly.world3d.R
import com.monopoly.world3d.engine.Board
import com.monopoly.world3d.engine.BuildingSystem
import com.monopoly.world3d.engine.Card
import com.monopoly.world3d.engine.Cards
import com.monopoly.world3d.engine.DecisionKind
import com.monopoly.world3d.engine.Engine
import com.monopoly.world3d.engine.GameState
import com.monopoly.world3d.engine.Group
import com.monopoly.world3d.engine.PersianNumbers
import com.monopoly.world3d.engine.Space
import com.monopoly.world3d.engine.SpaceType
import com.monopoly.world3d.engine.TradeOffer
import com.monopoly.world3d.engine.TradingSystem

/**
 * دیالوگ‌های داخل بازی: خرید، حراج، کارت، زندان، بدهی، معامله، مدیریت املاک.
 */
object Dialogs {

    private fun themedDialog(activity: Activity, title: String): AlertDialog.Builder {
        val b = AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_Dialog_NoActionBar)
        return b
    }

    /** ساخت پنل محتوای تیره برای دیالوگ */
    fun dialogPanel(context: Context): LinearLayout {
        val panel = LinearLayout(context)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(Ui.dp(context, 16), Ui.dp(context, 12), Ui.dp(context, 16), Ui.dp(context, 12))
        panel.background = context.getDrawable(R.drawable.panel_bg)
        return panel
    }

    fun dialogTitle(context: Context, text: String, size: Float = 18f): TextView {
        val t = TextView(context)
        t.text = text
        t.textSize = size
        t.setTextColor(0xFFFFD4A017.toInt())
        t.typeface = Ui.persianFont(context)
        t.gravity = Gravity.CENTER
        t.setPadding(0, Ui.dp(context, 4), 0, Ui.dp(context, 8))
        return t
    }

    fun dialogText(context: Context, text: String, size: Float = 15f, color: Int = 0xFFF5EFE0.toInt()): TextView {
        val t = TextView(context)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        t.typeface = Ui.persianFont(context)
        t.setPadding(0, Ui.dp(context, 3), 0, Ui.dp(context, 3))
        return t
    }

    fun dialogButton(context: Context, text: String, bgRes: Int = R.drawable.btn_primary, onClick: () -> Unit): Button {
        val b = Button(context)
        b.text = text
        b.textSize = 14f
        b.typeface = Ui.persianFont(context)
        b.setTextColor(Color.WHITE)
        b.background = context.getDrawable(bgRes)
        b.isAllCaps = false
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 44))
        lp.topMargin = Ui.dp(context, 5)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    fun show(activity: Activity, panel: LinearLayout, cancellable: Boolean = true): AlertDialog {
        val d = AlertDialog.Builder(activity)
            .setView(panel)
            .setCancelable(cancellable)
            .create()
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.show()
        d.window?.setLayout(Ui.dp(activity, 330), ViewGroup.LayoutParams.WRAP_CONTENT)
        return d
    }

    // ---------------- دیالوگ کارت ----------------

    fun showCard(activity: Activity, card: Card, onClose: () -> Unit): AlertDialog {
        val panel = dialogPanel(activity)
        val isChance = card.deck == Cards.DECK_CHANCE
        panel.background = activity.getDrawable(
            if (isChance) R.drawable.card_chance else R.drawable.card_chest
        )
        val title = TextView(activity)
        title.text = Cards.deckName(card.deck)
        title.textSize = 14f
        title.setTextColor(Color.WHITE)
        title.typeface = Ui.persianFont(activity)
        title.gravity = Gravity.CENTER
        panel.addView(title)
        val cardTitle = TextView(activity)
        cardTitle.text = card.title
        cardTitle.textSize = 22f
        cardTitle.setTextColor(Color.WHITE)
        cardTitle.typeface = Ui.persianFont(activity)
        cardTitle.gravity = Gravity.CENTER
        cardTitle.setPadding(0, Ui.dp(activity, 10), 0, Ui.dp(activity, 6))
        panel.addView(cardTitle)
        val desc = TextView(activity)
        desc.text = card.description
        desc.textSize = 15f
        desc.setTextColor(Color.WHITE)
        desc.typeface = Ui.persianFont(activity)
        desc.gravity = Gravity.CENTER
        desc.setPadding(0, 0, 0, Ui.dp(activity, 10))
        panel.addView(desc)
        panel.addView(dialogButton(activity, "تأیید", R.drawable.btn_primary) { onClose() })
        val d = show(activity, panel, cancellable = false)
        (panel.getChildAt(panel.childCount - 1) as Button).setOnClickListener {
            d.dismiss(); onClose()
        }
        return d
    }

    // ---------------- دیالوگ خرید ملک ----------------

    fun showPurchase(activity: Activity, engine: Engine, onDone: () -> Unit): AlertDialog {
        val state = engine.state
        // گارد: اگر فضای خرید نامعتبر بود (فایل ذخیره‌ی خراب)، دیالوگ باز نمی‌شود
        if (state.pendingPurchaseSpace !in 0..39) {
            state.awaiting = DecisionKind.NONE
            onDone()
            return AlertDialog.Builder(activity).create()
        }
        val sp = state.space(state.pendingPurchaseSpace)
        val p = state.currentPlayer
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "خرید ملک"))
        panel.addView(dialogText(activity, "«${sp.name}»", 19f, 0xFFFFFFFF.toInt()))
        val groupText = sp.group?.let { "گروه: ${it.faName}" } ?: ""
        if (groupText.isNotEmpty()) panel.addView(dialogText(activity, groupText, 13f, 0xFFC8BFA8.toInt()))
        panel.addView(dialogText(activity, "قیمت: ${PersianNumbers.money(sp.price)}"))
        panel.addView(dialogText(activity, "موجودی شما: ${PersianNumbers.money(p.balance)}"))
        if (Board.isStreetGroup(sp.group)) {
            panel.addView(dialogText(activity, "اجاره‌ی پایه: ${PersianNumbers.money(sp.rents[0])} | با کل گروه: ${PersianNumbers.money(sp.rents[0] * 2)}"))
            panel.addView(dialogText(activity, "اجاره با هتل: ${PersianNumbers.money(sp.rents.getOrElse(5) { 0 })}"))
            panel.addView(dialogText(activity, "هزینه‌ی هر خانه: ${PersianNumbers.money(sp.houseCost)}"))
        } else if (sp.type == SpaceType.RAILROAD) {
            panel.addView(dialogText(activity, "اجاره: ${Board.RAILROAD_RENTS.joinToString(" / ") { PersianNumbers.money(it) }} (۱ تا ۴ ایستگاه)"))
        }
        val canAfford = p.balance >= sp.price
        panel.addView(dialogButton(activity, "خرید کن — ${PersianNumbers.money(sp.price)}", R.drawable.btn_green) {
            engine.buyCurrentProperty()
            MonopolyApp.instance.audio.playSfx("buy")
            onDone()
        }.also { it.isEnabled = canAfford })
        panel.addView(dialogButton(activity, "خرید نمی‌کنم (حراج)", R.drawable.btn_danger) {
            engine.declinePurchase()
            onDone()
        })
        return show(activity, panel, cancellable = false)
    }

    // ---------------- دیالوگ زندان ----------------

    fun showJail(activity: Activity, engine: Engine, onDone: () -> Unit): AlertDialog {
        val state = engine.state
        val p = state.currentPlayer
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "زندان"))
        panel.addView(dialogText(activity, "${p.name} در زندان است (تلاش ${PersianNumbers.number(p.jailTurns + 1)} از ${PersianNumbers.number(state.rules.maxJailTurns)}).", 14f))
        panel.addView(dialogText(activity, "برای خروج: تاس جفت بیاورید، ${PersianNumbers.money(state.rules.jailFine)} بپردازید یا کارت آزادی استفاده کنید."))
        panel.addView(dialogButton(activity, "ریختن تاس (امید به جفت)", R.drawable.btn_primary) {
            engine.rollDice()
            onDone()
        })
        panel.addView(dialogButton(activity, "پرداخت جریمه — ${PersianNumbers.money(state.rules.jailFine)}", R.drawable.btn_action) {
            engine.payJailFine()
            onDone()
        }.also { it.isEnabled = p.balance >= state.rules.jailFine })
        panel.addView(dialogButton(activity, "استفاده از کارت آزادی (${PersianNumbers.number(p.jailCards)})", R.drawable.btn_action) {
            engine.useJailCard()
            onDone()
        }.also { it.isEnabled = p.jailCards > 0 })
        return show(activity, panel, cancellable = false)
    }

    // ---------------- دیالوگ حراج ----------------

    class AuctionDialogHolder(val dialog: AlertDialog, val info: TextView, val bidButton: Button)

    fun showAuction(activity: Activity, engine: Engine, onHumanBid: (Long) -> Unit, onHumanPass: () -> Unit): AuctionDialogHolder {
        val state = engine.state
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "حراج ملک"))
        val info = TextView(activity)
        info.textSize = 14f
        info.typeface = Ui.persianFont(activity)
        info.setTextColor(0xFFF5EFE0.toInt())
        panel.addView(info)
        val bidButton = Button(activity)
        bidButton.text = "پیشنهاد بده"
        bidButton.typeface = Ui.persianFont(activity)
        bidButton.setTextColor(Color.WHITE)
        bidButton.background = activity.getDrawable(R.drawable.btn_green)
        panel.addView(bidButton)
        panel.addView(dialogButton(activity, "انصراف از حراج", R.drawable.btn_danger) { onHumanPass() })
        val d = show(activity, panel, cancellable = false)
        return AuctionDialogHolder(d, info, bidButton)
    }

    fun updateAuctionInfo(holder: AuctionDialogHolder, engine: Engine, context: Context, onBidAmountChosen: (Long) -> Unit) {
        val state = engine.state
        val auction = state.auction ?: return
        val sp = state.space(auction.spaceId)
        val bidderId = auction.currentBidder()
        val bidder = if (bidderId >= 0) state.players[bidderId] else null
        val sb = StringBuilder()
        sb.appendLine("ملک: «${sp.name}» — قیمت پایه ${PersianNumbers.money(sp.price)}")
        if (auction.highestBidder == -1) sb.appendLine("هنوز پیشنهادی ثبت نشده است.")
        else {
            val hb = state.players[auction.highestBidder]
            sb.appendLine("بالاترین پیشنهاد: ${hb.name} — ${PersianNumbers.money(auction.highestBid)}")
        }
        if (bidder != null) {
            sb.appendLine("نوبت پیشنهاد: ${bidder.name} (موجودی ${PersianNumbers.money(bidder.balance)})")
            if (bidder.isHuman) {
                sb.appendLine("مبلغ پیشنهاد را انتخاب کنید:")
            } else {
                sb.appendLine("در انتظار تصمیم ${bidder.name}...")
            }
        }
        holder.info.text = sb.toString()
        val isHumanTurn = bidder != null && bidder.isHuman
        holder.bidButton.isEnabled = isHumanTurn
        if (isHumanTurn) {
            holder.bidButton.setOnClickListener {
                showBidSheet(context, engine) { amount -> onBidAmountChosen(amount) }
            }
        }
    }

    private fun showBidSheet(context: Context, engine: Engine, onChosen: (Long) -> Unit) {
        val state = engine.state
        val auction = state.auction ?: return
        val minBid = auction.minimumNextBid(state.space(auction.spaceId).price)
        val panel = dialogPanel(context)
        panel.addView(dialogTitle(context, "مبلغ پیشنهاد"))
        panel.addView(dialogText(context, "حداقل پیشنهاد: ${PersianNumbers.money(minBid)}"))
        val amounts = linkedSetOf<Long>()
        amounts.add(minBid)
        amounts.add(minBid + 10_000)
        amounts.add(minBid + 50_000)
        amounts.add(minBid + 100_000)
        val me = state.players[auction.currentBidder()]
        for (a in amounts.toList()) if (a > me.balance) amounts.remove(a)
        val grid = LinearLayout(context)
        grid.orientation = LinearLayout.VERTICAL
        for (a in amounts) {
            grid.addView(dialogButton(context, PersianNumbers.money(a), R.drawable.btn_action) {
                onChosen(a)
            })
        }
        panel.addView(grid)
        // ورودی دلخواه
        val input = EditText(context)
        input.hint = "مبلغ دلخواه (تومان)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.typeface = Ui.persianFont(context)
        input.setTextColor(Color.WHITE)
        panel.addView(input)
        panel.addView(dialogButton(context, "ثبت این مبلغ", R.drawable.btn_primary) {
            val v = PersianNumbers.toLatinDigits(input.text.toString()).toLongOrNull() ?: 0
            if (v > 0) onChosen(v)
        })
        val d = show(context as Activity, panel)
        onChosenCustomDismiss = d
    }

    var onChosenCustomDismiss: AlertDialog? = null

    // ---------------- دیالوگ بدهی ----------------

    fun showDebt(activity: Activity, engine: Engine, onAction: () -> Unit): AlertDialog {
        val state = engine.state
        val p = state.currentPlayer
        val creditorName = if (state.debtCreditor >= 0) state.players[state.debtCreditor].name else "بانک"
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "بدهی!", 20f))
        panel.addView(dialogText(activity, "بدهی ${p.name}: ${PersianNumbers.money(state.debtAmount)} به $creditorName", 16f, 0xFFFF6B6B.toInt()))
        panel.addView(dialogText(activity, "علت: ${state.debtReason}", 13f, 0xFFC8BFA8.toInt()))
        panel.addView(dialogText(activity, "موجودی: ${PersianNumbers.money(p.balance)}", 14f))
        panel.addView(dialogText(activity, "برای پرداخت باید ساختمان بفروشید یا ملک رهن بگذارید.", 13f))
        panel.addView(dialogButton(activity, "فروش ساختمان", R.drawable.btn_action) { onAction() })
        panel.addView(dialogButton(activity, "رهن گذاشتن ملک", R.drawable.btn_action) { onAction() })
        panel.addView(dialogButton(activity, "تلاش برای پرداخت", R.drawable.btn_primary) {
            engine.checkDebt()
            onAction()
        })
        panel.addView(dialogButton(activity, "اعلام ورشکستگی", R.drawable.btn_danger) {
            AlertDialog.Builder(activity)
                .setTitle("ورشکستگی")
                .setMessage("همه‌ی دارایی‌های شما از دست می‌رود. مطمئنید؟")
                .setPositiveButton("بله") { _, _ ->
                    engine.declareBankruptcy(p.id, state.debtCreditor, state.debtReason)
                    onAction()
                }
                .setNegativeButton("خیر", null)
                .show()
        })
        return show(activity, panel, cancellable = false)
    }

    // ---------------- مدیریت املاک (ساخت/فروش/رهن/آزادسازی) ----------------

    fun showManage(activity: Activity, engine: Engine, playerId: Int, mode: String, onAction: () -> Unit) {
        val state = engine.state
        val p = state.players[playerId]
        val panel = dialogPanel(activity)
        val titleText = when (mode) {
            "build" -> "ساخت خانه و هتل"
            "sell" -> "فروش ساختمان"
            "mortgage" -> "رهن گذاشتن ملک"
            "redeem" -> "آزادسازی رهن"
            else -> "املاک من"
        }
        panel.addView(dialogTitle(activity, titleText, 18f))
        panel.addView(dialogText(activity, "موجودی: ${PersianNumbers.money(p.balance)}", 13f, 0xFFC8BFA8.toInt()))

        val owned = state.ownedBy(playerId)
        if (owned.isEmpty()) {
            panel.addView(dialogText(activity, "هنوز ملکی ندارید.", 14f))
        }
        val scroll = ScrollView(activity)
        val list = LinearLayout(activity)
        list.orientation = LinearLayout.VERTICAL
        for (id in owned) {
            val sp = state.space(id)
            val ps = state.prop(id)
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.VERTICAL
            row.background = activity.getDrawable(R.drawable.btn_action)
            row.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 8), Ui.dp(activity, 10), Ui.dp(activity, 8))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = Ui.dp(activity, 6)
            row.layoutParams = lp

            val nameView = TextView(activity)
            nameView.text = "${sp.name} ${if (ps.mortgaged) "(رهن)" else ""} ${buildingLabel(ps.houses)}"
            nameView.textSize = 15f
            nameView.typeface = Ui.persianFont(activity)
            nameView.setTextColor(0xFFFFFFFF.toInt())
            row.addView(nameView)

            val info = TextView(activity)
            info.textSize = 12f
            info.typeface = Ui.persianFont(activity)
            info.setTextColor(0xFFC8BFA8.toInt())
            val rent = if (ps.mortgaged) 0 else BuildingSystem.computeRent(state, id, 7)
            info.text = "اجاره فعلی: ${PersianNumbers.money(rent)} | ارزش رهن: ${PersianNumbers.money(sp.mortgageValue)}"
            row.addView(info)

            // دکمه‌های عملیات
            val actions = LinearLayout(activity)
            actions.orientation = LinearLayout.HORIZONTAL
            fun addAct(label: String, bg: Int, enabled: Boolean, fn: () -> Unit) {
                val b = Button(activity)
                b.text = label
                b.textSize = 12f
                b.typeface = Ui.persianFont(activity)
                b.setTextColor(Color.WHITE)
                b.background = activity.getDrawable(bg)
                b.isEnabled = enabled
                b.isAllCaps = false
                val alp = LinearLayout.LayoutParams(0, Ui.dp(activity, 38), 1f)
                alp.setMargins(Ui.dp(activity, 2), 0, Ui.dp(activity, 2), 0)
                actions.addView(b, alp)
                b.setOnClickListener { fn() }
            }
            if (mode == "build") {
                val err = BuildingSystem.canBuild(state, playerId, id)
                addAct("ساخت (${PersianNumbers.moneyShort(sp.houseCost)})", R.drawable.btn_green, err == null) {
                    val e = engine.playerBuild(playerId, id)
                    if (e != null) Ui.toast(activity, e) else MonopolyApp.instance.audio.playSfx("build")
                    onAction()
                }
            }
            if (mode == "sell") {
                val err = BuildingSystem.canSell(state, playerId, id)
                addAct("فروش ساختمان", R.drawable.btn_danger, err == null) {
                    val e = engine.playerSellBuilding(playerId, id)
                    if (e != null) Ui.toast(activity, e)
                    onAction()
                }
            }
            if (mode == "mortgage") {
                val err = BuildingSystem.canMortgage(state, playerId, id)
                addAct("رهن (${PersianNumbers.moneyShort(sp.mortgageValue)})", R.drawable.btn_danger, err == null) {
                    val e = engine.playerMortgage(playerId, id)
                    if (e != null) Ui.toast(activity, e)
                    onAction()
                }
            }
            if (mode == "redeem") {
                val err = BuildingSystem.canRedeem(state, playerId, id)
                addAct("آزادسازی (${PersianNumbers.moneyShort(BuildingSystem.redeemCost(sp))})", R.drawable.btn_primary, err == null) {
                    val e = engine.playerRedeem(playerId, id)
                    if (e != null) Ui.toast(activity, e)
                    onAction()
                }
            }
            if (mode == "portfolio") {
                addAct("جزئیات", R.drawable.btn_primary, true) {
                    showPropertyInfo(activity, state, id)
                }
            }
            row.addView(actions)
            list.addView(row)
        }
        scroll.addView(list)
        val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 260))
        slp.topMargin = Ui.dp(activity, 6)
        panel.addView(scroll, slp)
        panel.addView(dialogButton(activity, "بستن", R.drawable.btn_action) { onAction() })
        show(activity, panel)
    }

    fun buildingLabel(houses: Int): String = when (houses) {
        0 -> ""
        5 -> "🏨"
        else -> "🏠".repeat(houses)
    }

    fun showPropertyInfo(activity: Activity, state: GameState, spaceId: Int) {
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, sp.name, 19f))
        val sb = StringBuilder()
        sb.appendLine("قیمت خرید: ${PersianNumbers.money(sp.price)}")
        sb.appendLine("ارزش رهن: ${PersianNumbers.money(sp.mortgageValue)}")
        if (Board.isStreetGroup(sp.group)) {
            sb.appendLine("هزینه‌ی ساخت: ${PersianNumbers.money(sp.houseCost)}")
            sb.appendLine()
            sb.appendLine("جدول اجاره:")
            val labels = listOf("بدون خانه", "۱ خانه", "۲ خانه", "۳ خانه", "۴ خانه", "هتل")
            for (i in 0..5) {
                sb.appendLine("${labels[i]}: ${PersianNumbers.money(sp.rents[i])}")
            }
            sb.appendLine()
            sb.appendLine("با داشتن کل گروه و بدون خانه، اجاره دو برابر می‌شود.")
        } else if (sp.type == SpaceType.RAILROAD) {
            sb.appendLine("اجاره بر اساس تعداد ایستگاه‌های مالک:")
            sb.appendLine(Board.RAILROAD_RENTS.joinToString(" / ") { PersianNumbers.money(it) })
        } else if (sp.type == SpaceType.UTILITY) {
            sb.appendLine("اجاره: ۴ برابر تاس با یک شرکت، ۱۰ برابر با هر دو.")
        }
        sb.appendLine()
        sb.appendLine("مالک: ${if (ps.owner >= 0) state.players[ps.owner].name else "بانک"}")
        if (ps.mortgaged) sb.appendLine("وضعیت: رهنی (اجاره تعلق نمی‌گیرد)")
        val t = TextView(activity)
        t.text = sb.toString()
        t.textSize = 14f
        t.typeface = Ui.persianFont(activity)
        t.setTextColor(0xFFF5EFE0.toInt())
        panel.addView(t)
        panel.addView(dialogButton(activity, "بستن", R.drawable.btn_action) { })
        show(activity, panel)
    }

    // ---------------- پاسخ به پیشنهاد معامله ----------------

    fun showTradeResponse(activity: Activity, engine: Engine, onDone: () -> Unit): AlertDialog {
        val state = engine.state
        val offer = state.pendingTrade ?: return AlertDialog.Builder(activity).create()
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "پیشنهاد معامله", 19f))
        panel.addView(dialogText(activity, TradingSystem.describe(state, offer), 14f))
        panel.addView(dialogButton(activity, "پذیرش معامله", R.drawable.btn_green) {
            engine.respondTrade(true)
            MonopolyApp.instance.audio.playSfx("cash")
            onDone()
        })
        panel.addView(dialogButton(activity, "رد پیشنهاد", R.drawable.btn_danger) {
            engine.respondTrade(false)
            onDone()
        })
        return show(activity, panel, cancellable = false)
    }

    // ---------------- سازنده‌ی معامله ----------------

    class TradeBuilderState(
        val fromId: Int,
        val toId: Int,
        val selectedGive: MutableSet<Int> = mutableSetOf(),
        val selectedGet: MutableSet<Int> = mutableSetOf(),
        var giveMoney: Long = 0,
        var getMoney: Long = 0,
        var giveCards: Int = 0,
        var getCards: Int = 0
    )

    fun showTradeBuilder(activity: Activity, engine: Engine, state: TradeBuilderState, onDone: () -> Unit) {
        val gs = engine.state
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "معامله با ${gs.players[state.toId].name}", 18f))
        val scroll = ScrollView(activity)
        val list = LinearLayout(activity)
        list.orientation = LinearLayout.VERTICAL

        fun section(label: String, props: List<Int>, selected: MutableSet<Int>) {
            list.addView(dialogText(activity, label, 14f, 0xFFFFD4A017.toInt()))
            for (id in props) {
                val sp = gs.space(id)
                val ps = gs.prop(id)
                if (!TradingSystem.isTradable(gs, id)) continue
                val chip = TextView(activity)
                chip.text = "${sp.name} — ${PersianNumbers.moneyShort(sp.price)}"
                chip.textSize = 13f
                chip.typeface = Ui.persianFont(activity)
                chip.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 6), Ui.dp(activity, 10), Ui.dp(activity, 6))
                chip.setTextColor(Color.WHITE)
                fun paint() {
                    chip.background = activity.getDrawable(
                        if (id in selected) R.drawable.btn_primary else R.drawable.btn_action)
                }
                paint()
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.topMargin = Ui.dp(activity, 3)
                chip.layoutParams = lp
                chip.setOnClickListener {
                    if (id in selected) selected.remove(id) else selected.add(id)
                    paint()
                }
                list.addView(chip)
            }
        }

        section("آنچه می‌دهید (ملک‌های شما):", gs.ownedBy(state.fromId), state.selectedGive)
        section("آنچه می‌خواهید (ملک‌های ${gs.players[state.toId].name}):", gs.ownedBy(state.toId), state.selectedGet)

        // پول
        fun moneyRow(label: String, initial: Long, setter: (Long) -> Unit) {
            list.addView(dialogText(activity, label, 14f, 0xFFFFD4A017.toInt()))
            val row = LinearLayout(activity)
            row.orientation = LinearLayout.HORIZONTAL
            val edit = EditText(activity)
            edit.setText(PersianNumbers.toPersianDigits(initial.toString()))
            edit.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            edit.setTextColor(Color.WHITE)
            edit.textSize = 13f
            edit.typeface = Ui.persianFont(activity)
            val elp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(edit, elp)
            val apply = Button(activity)
            apply.text = "ثبت"
            apply.textSize = 12f
            apply.typeface = Ui.persianFont(activity)
            apply.background = activity.getDrawable(R.drawable.btn_action)
            row.addView(apply, LinearLayout.LayoutParams(Ui.dp(activity, 70), Ui.dp(activity, 40)))
            apply.setOnClickListener {
                val v = PersianNumbers.toLatinDigits(edit.text.toString()).toLongOrNull() ?: 0
                setter(v.coerceAtLeast(0))
                Ui.toast(activity, "ثبت شد: ${PersianNumbers.money(v)}")
            }
            list.addView(row)
        }
        moneyRow("پولی که می‌دهید (تومان):", state.giveMoney) { state.giveMoney = it }
        moneyRow("پولی که می‌خواهید (تومان):", state.getMoney) { state.getMoney = it }

        scroll.addView(list)
        val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(activity, 300))
        slp.topMargin = Ui.dp(activity, 6)
        panel.addView(scroll, slp)

        panel.addView(dialogButton(activity, "ارسال پیشنهاد", R.drawable.btn_green) {
            val offer = TradeOffer(
                fromId = state.fromId, toId = state.toId,
                giveProperties = state.selectedGive.toList(),
                getProperties = state.selectedGet.toList(),
                giveMoney = state.giveMoney, getMoney = state.getMoney,
                giveJailCards = state.giveCards, getJailCards = state.getCards
            )
            val err = TradingSystem.validate(gs, offer)
            if (err != null) {
                Ui.toast(activity, err)
            } else {
                engine.proposeTrade(offer)
                onDone()
            }
        })
        panel.addView(dialogButton(activity, "انصراف", R.drawable.btn_action) { onDone() })
        show(activity, panel)
    }

    // ---------------- انتخاب حریف برای معامله ----------------

    fun showTradeTarget(activity: Activity, engine: Engine, fromId: Int, onPicked: (Int) -> Unit) {
        val state = engine.state
        val panel = dialogPanel(activity)
        panel.addView(dialogTitle(activity, "معامله با چه کسی؟", 18f))
        for (p in state.players) {
            if (p.id == fromId || p.bankrupt) continue
            panel.addView(dialogButton(activity, "${p.name} — موجودی ${PersianNumbers.moneyShort(p.balance)}", R.drawable.btn_action) {
                onPicked(p.id)
            })
        }
        panel.addView(dialogButton(activity, "انصراف", R.drawable.btn_danger) { })
        show(activity, panel)
    }

    // ---------------- انتخاب بازیکن بدهکار برای فروش (چندنفره) ----------------

    fun buildGradientCircle(color: Int): GradientDrawable {
        val gd = GradientDrawable()
        gd.shape = GradientDrawable.OVAL
        gd.setColor(color)
        return gd
    }
}
