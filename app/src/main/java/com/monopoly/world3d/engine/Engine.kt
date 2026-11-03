package com.monopoly.world3d.engine

/**
 * موتور قوانین «دنیای روپولی».
 * مستقل از گرافیک؛ رابط کاربری و هوش مصنوعی هر دو با همین متدها بازی را پیش می‌برند.
 *
 * جریان نوبت:
 *  startTurn → (تصمیم زندان؟) → rollDice → حرکت → حل تبعات خانه (خرید/اجاره/کارت/مالیات)
 *  → [حراج یا بدهی در صورت لزوم] → تاس جفت؟ دوباره بریز : endTurn
 */
class Engine(val state: GameState) {

    // ---------------- شروع بازی ----------------

    fun startGame(): List<LogEntry> {
        val events = mutableListOf<LogEntry>()
        state.players.forEachIndexed { i, p ->
            p.balance = state.rules.startMoney
            p.position = Board.GO_POS
            p.inJail = false
            p.jailTurns = 0
            p.jailCards = 0
            p.bankrupt = false
        }
        state.properties.forEach { it.owner = -1; it.houses = 0; it.mortgaged = false }
        state.currentTurn = 0
        state.roundCount = 1
        state.doublesCount = 0
        state.hasRolled = false
        state.rollAgain = false
        state.pendingPurchaseSpace = -1
        state.auction = null
        state.auctionQueue.clear()
        state.pendingTrade = null
        state.debtAmount = 0
        state.debtCreditor = -1
        state.parkingPot = 0
        state.bankHouses = state.rules.houseBankLimit
        state.bankHotels = state.rules.hotelBankLimit
        state.winnerId = -1
        state.turnCounter = 0
        state.awaiting = DecisionKind.NONE
        state.shuffleDecks()
        events.add(state.addLog("بازی شروع شد! هر بازیکن ${PersianNumbers.money(state.rules.startMoney)} دارد.", -1, LogEntry.KIND_INFO))
        events.add(state.addLog("نوبت ${state.currentPlayer.name} است.", state.currentTurn, LogEntry.KIND_INFO))
        return events
    }

    // ---------------- توابع کمکی وضعیت ----------------

    fun canRoll(): Boolean =
        state.awaiting == DecisionKind.NONE && !state.isGameOver && (!state.hasRolled || state.rollAgain)

    fun canEndTurn(): Boolean =
        state.awaiting == DecisionKind.NONE && !state.isGameOver && state.hasRolled && !state.rollAgain

    private fun tailEvents(beforeSize: Int): List<LogEntry> =
        state.log.drop(beforeSize)

    // ---------------- نوبت ----------------

    private fun startTurn(): List<LogEntry> {
        val p = state.currentPlayer
        state.hasRolled = false
        state.rollAgain = false
        state.doublesCount = 0
        val ev = mutableListOf<LogEntry>()
        ev.add(state.addLog("— نوبت ${p.name} (دور ${PersianNumbers.number(state.roundCount)}) —", p.id, LogEntry.KIND_INFO))
        if (p.inJail) {
            state.awaiting = DecisionKind.JAIL_DECISION
        } else {
            state.awaiting = DecisionKind.NONE
        }
        return ev
    }

    fun rollDice(): List<LogEntry> {
        val before = state.log.size
        if (!canRoll() && state.awaiting != DecisionKind.JAIL_DECISION) return emptyList()
        val p = state.currentPlayer
        val d1 = 1 + state.rng.nextInt(6)
        val d2 = 1 + state.rng.nextInt(6)
        state.dice1 = d1
        state.dice2 = d2
        state.hasRolled = true
        val isDouble = d1 == d2
        if (isDouble) state.statsFor(p.id).doublesRolled++

        if (state.awaiting == DecisionKind.JAIL_DECISION) {
            rollDiceInJail(p, d1, d2, isDouble)
        } else {
            if (isDouble) {
                state.doublesCount++
                if (state.doublesCount >= 3) {
                    state.addLog("${p.name} سه بار پشت‌سرهم تاس جفت آورد و به زندان رفت!", p.id, LogEntry.KIND_JAIL)
                    sendToJail(p)
                    state.awaiting = DecisionKind.NONE
                    return tailEvents(before)
                }
                state.rollAgain = true
            } else {
                state.doublesCount = 0
                state.rollAgain = false
            }
            state.addLog("${p.name} تاس انداخت: ${PersianNumbers.number(d1)} و ${PersianNumbers.number(d2)}${if (isDouble) " (جفت!)" else ""}", p.id, LogEntry.KIND_INFO)
            movePlayer(p, d1 + d2)
        }
        checkVictory()
        return tailEvents(before)
    }

    private fun rollDiceInJail(p: Player, d1: Int, d2: Int, isDouble: Boolean) {
        if (isDouble) {
            p.inJail = false
            p.jailTurns = 0
            state.awaiting = DecisionKind.NONE
            state.addLog("${p.name} با تاس جفت (${PersianNumbers.number(d1)} و ${PersianNumbers.number(d2)}) از زندان آزاد شد و حرکت کرد.", p.id, LogEntry.KIND_JAIL)
            movePlayer(p, d1 + d2)
        } else {
            p.jailTurns++
            if (p.jailTurns >= state.rules.maxJailTurns) {
                // پس از سه تلاش ناموفق: پرداخت اجباری جریمه و حرکت
                state.addLog("${p.name} سه بار نتوانست جفت بیاورد؛ باید جریمه را بپردازد.", p.id, LogEntry.KIND_JAIL)
                val fine = state.rules.jailFine
                p.inJail = false
                p.jailTurns = 0
                val paid = Bank.payToBank(state, p.id, fine, "پرداخت اجباری جریمه‌ی زندان", toPot = true)
                if (!paid) {
                    // بدهی جریمه؛ پس از تسویه حرکت می‌کند
                    state.debtAmount = fine
                    state.debtCreditor = -1
                    state.debtReason = "جریمه‌ی زندان"
                    state.jailAfterDebtMove = d1 + d2
                    state.awaiting = DecisionKind.DEBT
                } else {
                    state.awaiting = DecisionKind.NONE
                    movePlayer(p, d1 + d2)
                    state.hasRolled = true
                    state.rollAgain = false
                    if (state.awaiting == DecisionKind.NONE) endTurnInternal()
                }
            } else {
                state.addLog("${p.name} نتوانست جفت بیاورد و در زندان ماند (تلاش ${PersianNumbers.number(p.jailTurns)} از ${PersianNumbers.number(state.rules.maxJailTurns)}).", p.id, LogEntry.KIND_JAIL)
                state.awaiting = DecisionKind.NONE
                state.rollAgain = false
                endTurnInternal()
            }
        }
    }

    fun payJailFine(): List<LogEntry> {
        val before = state.log.size
        if (state.awaiting != DecisionKind.JAIL_DECISION) return emptyList()
        val p = state.currentPlayer
        val fine = state.rules.jailFine
        if (Bank.payToBank(state, p.id, fine, "جریمه‌ی خروج از زندان", toPot = true)) {
            p.inJail = false
            p.jailTurns = 0
            state.awaiting = DecisionKind.NONE
            state.addLog("${p.name} با پرداخت ${PersianNumbers.money(fine)} از زندان آزاد شد.", p.id, LogEntry.KIND_JAIL)
        } else {
            state.debtAmount = fine
            state.debtCreditor = -1
            state.debtReason = "جریمه‌ی زندان"
            state.awaiting = DecisionKind.DEBT
            state.jailAfterDebtMove = -1  // پس از تسویه فقط آزاد می‌شود، حرکت ندارد
        }
        return tailEvents(before)
    }

    fun useJailCard(): List<LogEntry> {
        val before = state.log.size
        if (state.awaiting != DecisionKind.JAIL_DECISION) return emptyList()
        val p = state.currentPlayer
        if (p.jailCards <= 0) return emptyList()
        p.jailCards--
        p.inJail = false
        p.jailTurns = 0
        state.awaiting = DecisionKind.NONE
        state.addLog("${p.name} کارت خروج از زندان را استفاده کرد.", p.id, LogEntry.KIND_JAIL)
        return tailEvents(before)
    }

    fun endTurn(): List<LogEntry> {
        val before = state.log.size
        if (!canEndTurn()) return emptyList()
        endTurnInternal()
        return tailEvents(before)
    }

    private fun endTurnInternal() {
        if (state.isGameOver) return
        // پاک‌سازی وضعیت نوبت
        state.pendingPurchaseSpace = -1
        state.debtAmount = 0
        state.debtCreditor = -1
        state.debtReason = ""
        state.jailAfterDebtMove = 0
        // بازیکن بعدی فعال
        var next = state.currentTurn
        for (i in 1..state.players.size) {
            next = (next + 1) % state.players.size
            if (!state.players[next].bankrupt) break
        }
        if (next <= state.currentTurn) state.roundCount++
        state.currentTurn = next
        state.turnCounter++
        state.doublesCount = 0
        state.hasRolled = false
        state.rollAgain = false
        startTurn()
    }

    // ---------------- حرکت ----------------

    private fun movePlayer(p: Player, steps: Int, collectGo: Boolean = true) {
        val path = mutableListOf<Int>()
        var pos = p.position
        repeat(steps) {
            pos = (pos + 1) % Board.BOARD_SIZE
            path.add(pos)
            if (pos == Board.GO_POS && collectGo) {
                giveGoSalary(p, landedExactly = false)
            }
        }
        p.position = pos
        state.lastMovePath = path
        // پاداش فرود دقیق روی شروع
        if (p.position == Board.GO_POS && collectGo && state.rules.doubleSalaryOnExactGo) {
            Bank.deposit(state, p.id, state.rules.goSalary, "پاداش فرود دقیق روی شروع")
        }
        resolveLanding(p, depth = 0)
    }

    private fun teleportPlayer(p: Player, target: Int, collectGo: Boolean) {
        val path = mutableListOf<Int>()
        if (collectGo) {
            var pos = p.position
            while (true) {
                pos = (pos + 1) % Board.BOARD_SIZE
                path.add(pos)
                if (pos == Board.GO_POS) giveGoSalary(p, landedExactly = target == 0)
                if (pos == target) break
            }
        } else {
            path.add(target)
        }
        p.position = target
        state.lastMovePath = path
        resolveLanding(p, depth = 0)
    }

    private fun giveGoSalary(p: Player, landedExactly: Boolean) {
        var salary = state.rules.goSalary
        if (landedExactly && state.rules.doubleSalaryOnExactGo) salary *= 2
        Bank.deposit(state, p.id, salary, "حقوق گذر از شروع")
        state.statsFor(p.id).goPasses++
    }

    /** حرکت عقب (برای کارت‌ها) — حقوق شروع داده نمی‌شود */
    private fun moveBackward(p: Player, steps: Int) {
        val path = mutableListOf<Int>()
        var pos = p.position
        repeat(steps) {
            pos = (pos - 1 + Board.BOARD_SIZE) % Board.BOARD_SIZE
            path.add(pos)
        }
        p.position = pos
        state.lastMovePath = path
        resolveLanding(p, depth = 0)
    }

    private fun sendToJail(p: Player) {
        p.position = Board.JAIL_POS
        p.inJail = true
        p.jailTurns = 0
        state.doublesCount = 0
        state.rollAgain = false
        state.lastMovePath = listOf(Board.JAIL_POS)
        state.statsFor(p.id).timesInJail++
    }

    // ---------------- حل تبعات خانه ----------------

    private fun resolveLanding(p: Player, depth: Int) {
        if (state.isGameOver || p.bankrupt) return
        if (depth > 4) return
        val sp = state.space(p.position)
        when (sp.type) {
            SpaceType.GO -> { /* حقوق قبلاً داده شده */ }
            SpaceType.STREET, SpaceType.RAILROAD, SpaceType.UTILITY -> resolveProperty(p, sp)
            SpaceType.CHANCE -> drawCard(p, Cards.DECK_CHANCE, depth)
            SpaceType.CHEST -> drawCard(p, Cards.DECK_CHEST, depth)
            SpaceType.TAX -> {
                state.addLog("${p.name} روی «${sp.name}» رفت و باید ${PersianNumbers.money(sp.taxAmount)} بپردازد.", p.id, LogEntry.KIND_MONEY_OUT)
                payDebtable(p, sp.taxAmount, "پرداخت ${sp.name}", toPot = true)
            }
            SpaceType.JAIL -> state.addLog("${p.name} به دیدن زندان آمد.", p.id, LogEntry.KIND_INFO)
            SpaceType.FREE_PARKING -> {
                if (state.rules.freeParkingPot && state.parkingPot > 0) {
                    Bank.deposit(state, p.id, state.parkingPot, "برداشت صندوق پارکینگ آزاد")
                    state.addLog("${p.name} صندوق پارکینگ آزاد را برداشت: ${PersianNumbers.money(state.parkingPot)}!", p.id, LogEntry.KIND_MONEY_IN)
                    state.parkingPot = 0
                } else {
                    state.addLog("${p.name} در پارکینگ آزاد استراحت کرد.", p.id, LogEntry.KIND_INFO)
                }
            }
            SpaceType.GO_TO_JAIL -> {
                state.addLog("${p.name} دستور رفتن به زندان را دریافت کرد!", p.id, LogEntry.KIND_JAIL)
                sendToJail(p)
            }
        }
    }

    private fun resolveProperty(p: Player, sp: Space) {
        val ps = state.prop(sp.id)
        if (ps.owner == -1) {
            state.pendingPurchaseSpace = sp.id
            state.awaiting = DecisionKind.BUY_PROMPT
            return
        }
        if (ps.owner == p.id) {
            state.addLog("${p.name} به ملک خودش «${sp.name}» رسید.", p.id, LogEntry.KIND_INFO)
            return
        }
        if (ps.mortgaged) {
            state.addLog("«${sp.name}» رهن است؛ اجاره‌ای پرداخت نمی‌شود.", p.id, LogEntry.KIND_INFO)
            return
        }
        val rent = BuildingSystem.computeRent(state, sp.id, state.dice1 + state.dice2)
        if (rent <= 0) return
        val owner = state.players[ps.owner]
        state.addLog(
            "${p.name} اجاره‌ی «${sp.name}» را به ${owner.name} پرداخت کرد: ${PersianNumbers.money(rent)}",
            p.id, LogEntry.KIND_MONEY_OUT
        )
        state.statsFor(p.id).rentPaid += rent
        payDebtableToPlayer(p, ps.owner, rent, "اجاره‌ی ${sp.name}")
    }

    // ---------------- خرید و حراج ----------------

    fun buyCurrentProperty(): List<LogEntry> {
        val before = state.log.size
        if (state.awaiting != DecisionKind.BUY_PROMPT) return emptyList()
        val p = state.currentPlayer
        val sp = state.space(state.pendingPurchaseSpace)
        if (Bank.withdraw(state, p.id, sp.price, "خرید ${sp.name}")) {
            state.prop(sp.id).owner = p.id
            state.pendingPurchaseSpace = -1
            state.awaiting = DecisionKind.NONE
            state.statsFor(p.id).propertiesBought++
            state.addLog("${p.name} «${sp.name}» را به قیمت ${PersianNumbers.money(sp.price)} خرید!", p.id, LogEntry.KIND_MONEY_OUT)
        } else {
            state.addLog("${p.name} پول کافی برای خرید «${sp.name}» ندارد.", p.id, LogEntry.KIND_INFO)
            state.pendingPurchaseSpace = -1
            if (state.rules.auctionsEnabled) startAuction(sp.id) else state.awaiting = DecisionKind.NONE
        }
        return tailEvents(before)
    }

    fun declinePurchase(): List<LogEntry> {
        val before = state.log.size
        if (state.awaiting != DecisionKind.BUY_PROMPT) return emptyList()
        val sp = state.space(state.pendingPurchaseSpace)
        state.pendingPurchaseSpace = -1
        state.addLog("${state.currentPlayer.name} خرید «${sp.name}» را رد کرد.", state.currentTurn, LogEntry.KIND_INFO)
        if (state.rules.auctionsEnabled) {
            startAuction(sp.id)
        } else {
            state.awaiting = DecisionKind.NONE
        }
        return tailEvents(before)
    }

    fun startAuction(spaceId: Int) {
        val participants = state.activePlayers().map { it.id }.toMutableList()
        if (participants.size < 2) {
            // فقط یک بازیکن باقی است؛ ملک به او پیشنهاد نمی‌شود
            state.awaiting = DecisionKind.NONE
            return
        }
        // شروع پیشنهادها از بازیکن بعدیِ نوبت‌دار
        val startIdx = participants.indexOfFirst { it == state.currentTurn }.let {
            if (it == -1) 0 else (it + 1) % participants.size
        }
        state.auction = AuctionState(spaceId, participants, 0, -1, startIdx)
        state.awaiting = DecisionKind.AUCTION
        state.addLog("حراج «${state.space(spaceId).name}» آغاز شد!", -1, LogEntry.KIND_AUCTION)
    }

    fun auctionBid(playerId: Int, amount: Long): List<LogEntry> {
        val before = state.log.size
        val auction = state.auction
        if (state.awaiting != DecisionKind.AUCTION || auction == null) return emptyList()
        if (auction.currentBidder() != playerId) return emptyList()
        if (amount <= 0) return emptyList()
        if (amount > state.players[playerId].balance) {
            state.addLog("پیشنهاد بیش از موجودی مجاز نیست.", playerId, LogEntry.KIND_INFO)
            return tailEvents(before)
        }
        if (amount < auction.minimumNextBid(state.space(auction.spaceId).price)) return emptyList()
        auction.highestBid = amount
        auction.highestBidder = playerId
        advanceAuction(auction)
        return tailEvents(before)
    }

    fun auctionPass(playerId: Int): List<LogEntry> {
        val before = state.log.size
        val auction = state.auction
        if (state.awaiting != DecisionKind.AUCTION || auction == null) return emptyList()
        if (auction.currentBidder() != playerId) return emptyList()
        auction.participants.remove(playerId)
        if (auction.currentBidderIndex >= auction.participants.size) auction.currentBidderIndex = 0
        finishAuctionIfDone(auction)
        return tailEvents(before)
    }

    private fun advanceAuction(auction: AuctionState) {
        auction.currentBidderIndex = (auction.currentBidderIndex + 1) % auction.participants.size
        finishAuctionIfDone(auction)
    }

    private fun finishAuctionIfDone(auction: AuctionState) {
        if (auction.participants.size > 1) return
        val sp = state.space(auction.spaceId)
        if (auction.highestBidder != -1) {
            val winner = state.players[auction.highestBidder]
            if (Bank.withdraw(state, winner.id, auction.highestBid, "بردن حراج ${sp.name}")) {
                state.prop(sp.id).owner = winner.id
                state.statsFor(winner.id).auctionsWon++
                state.addLog("${winner.name} «${sp.name}» را در حراج با ${PersianNumbers.money(auction.highestBid)} برد!", winner.id, LogEntry.KIND_AUCTION)
            }
        } else {
            state.addLog("کسی در حراج «${sp.name}» پیشنهادی نداد.", -1, LogEntry.KIND_AUCTION)
        }
        state.auction = null
        state.awaiting = DecisionKind.NONE
        processAuctionQueue()
    }

    private fun processAuctionQueue() {
        if (state.isGameOver) return
        if (state.auctionQueue.isNotEmpty() && state.rules.auctionsEnabled) {
            val next = state.auctionQueue.removeAt(0)
            startAuction(next)
        }
    }

    // ---------------- بدهی و ورشکستگی ----------------

    /** پرداخت به بانک با امکان ایجاد بدهی برای بازیکن نوبت‌دار */
    private fun payDebtable(p: Player, amount: Long, reason: String, toPot: Boolean = false) {
        if (Bank.payToBank(state, p.id, amount, reason, toPot)) return
        if (p.id == state.currentTurn && state.awaiting != DecisionKind.DEBT) {
            state.debtAmount = amount
            state.debtCreditor = -1
            state.debtReason = reason
            state.awaiting = DecisionKind.DEBT
        }
    }

    /** پرداخت به بازیکن با امکان ایجاد بدهی برای بازیکن نوبت‌دار */
    private fun payDebtableToPlayer(p: Player, payeeId: Int, amount: Long, reason: String) {
        if (Bank.payToPlayer(state, p.id, payeeId, amount, reason)) return
        if (p.id == state.currentTurn && state.awaiting != DecisionKind.DEBT) {
            state.debtAmount = amount
            state.debtCreditor = payeeId
            state.debtReason = reason
            state.awaiting = DecisionKind.DEBT
        }
    }

    /** پس از هر اقدام نقدسازی، بدهی بررسی می‌شود */
    fun checkDebt(): List<LogEntry> {
        val before = state.log.size
        if (state.awaiting != DecisionKind.DEBT) return emptyList()
        val p = state.currentPlayer
        if (p.balance >= state.debtAmount) {
            val amount = state.debtAmount
            val creditor = state.debtCreditor
            val reason = state.debtReason
            if (creditor == -1) {
                Bank.payToBank(state, p.id, amount, reason, toPot = true)
            } else {
                Bank.payToPlayer(state, p.id, creditor, amount, reason)
            }
            state.addLog("${p.name} بدهی ${PersianNumbers.money(amount)} را تسویه کرد.", p.id, LogEntry.KIND_MONEY_OUT)
            state.debtAmount = 0
            state.debtCreditor = -1
            state.debtReason = ""
            if (state.jailAfterDebtMove != 0) {
                if (state.jailAfterDebtMove > 0) movePlayer(p, state.jailAfterDebtMove)
                state.jailAfterDebtMove = 0
                if (state.awaiting == DecisionKind.DEBT) {
                    // حرکت بعدی خودش بدهی تازه ساخته است؛ ابتدا تسویه می‌شود
                    state.awaiting = DecisionKind.DEBT
                } else {
                    state.awaiting = DecisionKind.NONE
                    endTurnInternal()
                }
            } else {
                state.awaiting = DecisionKind.NONE
            }
        } else if (Bank.maxRaisableFunds(state, p.id) < state.debtAmount) {
            // حتی با فروش همه‌چیز هم نمی‌تواند بپردازد → ورشکستگی
            declareBankruptcy(p.id, state.debtCreditor, state.debtReason)
        }
        checkVictory()
        return tailEvents(before)
    }

    /** نقدسازی خودکار: فروش خانه‌ها و رهن املاک تا رسیدن به مبلغ هدف */
    fun autoLiquidate(playerId: Int, target: Long) {
        val p = state.players[playerId]
        var guard = 0
        while (p.balance < target && guard++ < 200) {
            // ۱) فروش ساختمان‌ها (پرساخت‌ترین اول)
            val withHouses = state.ownedBy(playerId)
                .filter { state.prop(it).houses > 0 }
                .sortedByDescending { state.prop(it).houses }
            if (withHouses.isNotEmpty()) {
                BuildingSystem.sell(state, playerId, withHouses.first())
                continue
            }
            // ۲) رهن کردن ارزان‌ترین ملک آزاد
            val unmortgaged = state.ownedBy(playerId)
                .filter { !state.prop(it).mortgaged }
                .sortedBy { state.space(it).price }
            if (unmortgaged.isNotEmpty()) {
                BuildingSystem.mortgage(state, playerId, unmortgaged.first())
                continue
            }
            break
        }
    }

    fun declareBankruptcy(playerId: Int, creditorId: Int, reason: String) {
        val p = state.players[playerId]
        if (p.bankrupt) return
        // پول نقد باقی‌مانده به طلبکار می‌رود
        if (p.balance > 0 && creditorId >= 0) {
            Bank.deposit(state, creditorId, p.balance, "دارایی نقد ورشکسته")
        }
        p.bankrupt = true
        p.balance = 0
        val creditor = if (creditorId >= 0) state.players[creditorId] else null
        state.addLog(
            if (creditor != null) "${p.name} ورشکست شد! همه‌ی دارایی‌ها به ${creditor.name} منتقل شد."
            else "${p.name} ورشکست شد! دارایی‌ها به بانک بازگشت.",
            p.id, LogEntry.KIND_BANKRUPT
        )
        // فروش ساختمان‌ها به بانک با نصف قیمت؛ پول به طلبکار
        var buildingMoney = 0L
        for (id in state.ownedBy(playerId)) {
            val ps = state.prop(id)
            if (ps.houses > 0) {
                val sp = state.space(id)
                buildingMoney += if (ps.houses == 5) sp.houseCost * 5 / 2 else ps.houses * sp.houseCost / 2
                state.bankHotels += if (ps.houses == 5) 1 else 0
                state.bankHouses += if (ps.houses == 5) 4 else ps.houses
                ps.houses = 0
            }
        }
        if (creditor != null) Bank.deposit(state, creditor.id, buildingMoney, "دارایی ورشکسته")
        // انتقال املاک
        for (id in state.ownedBy(playerId).toList()) {
            val ps = state.prop(id)
            if (creditor != null) {
                ps.owner = creditor.id
                if (ps.mortgaged) {
                    // طلبکار ۱۰٪ بهره‌ی رهن به بانک می‌پردازد
                    val fee = state.space(id).mortgageValue / 10
                    if (creditor.balance >= fee) Bank.withdraw(state, creditor.id, fee, "بهره‌ی رهن ${state.space(id).name}")
                }
            } else {
                ps.owner = -1
                if (state.rules.auctionsEnabled) state.auctionQueue.add(id)
            }
        }
        // انتقال کارت‌های آزادی
        if (creditor != null) {
            creditor.jailCards += p.jailCards
        }
        p.jailCards = 0
        // اگر نوبت‌دار خودش بود، نوبت تمام می‌شود
        if (p.id == state.currentTurn && !state.isGameOver) {
            state.debtAmount = 0
            state.debtCreditor = -1
            state.jailAfterDebtMove = 0
            endTurnInternal()
        }
        checkVictory()
    }

    private fun checkVictory() {
        val active = state.activePlayers()
        if (active.size == 1 && !state.isGameOver) {
            state.winnerId = active[0].id
            state.awaiting = DecisionKind.GAME_OVER
            state.addLog("🏆 ${active[0].name} برنده‌ی بازی شد!", active[0].id, LogEntry.KIND_WIN)
        }
    }

    // ---------------- کارت‌ها ----------------

    private fun drawCard(p: Player, deck: Int, depth: Int) {
        val deckRef = if (deck == Cards.DECK_CHANCE) state.chanceDeck else state.chestDeck
        if (deckRef.isEmpty()) {
            state.shuffleDecks()
        }
        val cardId = deckRef.removeAt(0)
        // کارت‌های نگه‌داشتنی به دست برنمی‌گردند؛ بقیه به ته دسته برمی‌گردند
        val card = Cards.byId(cardId)
        state.lastDrawnCardId = cardId
        state.lastDrawnCardDeck = deck
        state.statsFor(p.id).cardsDrawn++
        state.addLog("${p.name} «${card.title}» کشید: ${card.description}", p.id, LogEntry.KIND_CARD)
        val keepable = card.effect is CardEffect.JailFree && !p.inJail
        if (!keepable) deckRef.add(cardId)
        applyCardEffect(p, card, depth)
    }

    private fun applyCardEffect(p: Player, card: Card, depth: Int) {
        when (val e = card.effect) {
            is CardEffect.Gain -> Bank.deposit(state, p.id, e.amount, "اثر کارت «${card.title}»")
            is CardEffect.PayFine -> payDebtable(p, e.amount, "اثر کارت «${card.title}»", toPot = true)
            is CardEffect.MoveTo -> {
                if (p.position != e.spaceId) teleportPlayer(p, e.spaceId, e.collectGo)
                else resolveLanding(p, depth + 1)
            }
            is CardEffect.MoveToNearestRailroad -> {
                val rails = listOf(5, 15, 25, 35)
                val target = rails.firstOrNull { it > p.position } ?: rails.first()
                teleportPlayer(p, target, true)
                val ps = state.prop(target)
                if (ps.owner >= 0 && ps.owner != p.id && !ps.mortgaged) {
                    val rent = BuildingSystem.computeRent(state, target, state.dice1 + state.dice2) * 2
                    val owner = state.players[ps.owner]
                    state.addLog("${p.name} دو برابر اجاره‌ی «${state.space(target).name}» را به ${owner.name} پرداخت کرد: ${PersianNumbers.money(rent)}", p.id, LogEntry.KIND_MONEY_OUT)
                    state.statsFor(p.id).rentPaid += rent
                    payDebtableToPlayer(p, ps.owner, rent, "اجاره‌ی دوبرابر ${state.space(target).name}")
                }
            }
            is CardEffect.MoveToNearestUtility -> {
                val utils = listOf(12, 28)
                val target = utils.firstOrNull { it > p.position } ?: utils.first()
                teleportPlayer(p, target, true)
                val ps = state.prop(target)
                if (ps.owner >= 0 && ps.owner != p.id && !ps.mortgaged) {
                    val rent = (state.dice1 + state.dice2) * 10_000L
                    val owner = state.players[ps.owner]
                    state.addLog("${p.name} ده برابر تاس را به ${owner.name} پرداخت کرد: ${PersianNumbers.money(rent)}", p.id, LogEntry.KIND_MONEY_OUT)
                    state.statsFor(p.id).rentPaid += rent
                    payDebtableToPlayer(p, ps.owner, rent, "اجاره‌ی خدمات")
                }
            }
            is CardEffect.MoveBack -> moveBackward(p, e.steps)
            is CardEffect.MoveForward -> movePlayer(p, e.steps)
            is CardEffect.GoToJail -> sendToJail(p)
            is CardEffect.JailFree -> {
                if (p.inJail) {
                    p.inJail = false
                    p.jailTurns = 0
                    state.addLog("${p.name} با کارت از زندان آزاد شد!", p.id, LogEntry.KIND_JAIL)
                } else {
                    p.jailCards++
                    state.addLog("${p.name} کارت خروج از زندان را نگه داشت.", p.id, LogEntry.KIND_CARD)
                }
            }
            is CardEffect.BuildingRepairs -> {
                var houses = 0
                var hotels = 0
                for (id in state.ownedBy(p.id)) {
                    val h = state.prop(id).houses
                    if (h == 5) hotels++ else houses += h
                }
                val cost = houses * e.perHouse + hotels * e.perHotel
                if (cost > 0) {
                    state.addLog("تعمیرات: ${PersianNumbers.number(houses)} خانه و ${PersianNumbers.number(hotels)} هتل — مجموع ${PersianNumbers.money(cost)}", p.id, LogEntry.KIND_MONEY_OUT)
                    payDebtable(p, cost, "تعمیرات ساختمان‌ها", toPot = true)
                }
            }
            is CardEffect.CollectFromEach -> {
                for (other in state.activePlayers()) {
                    if (other.id == p.id || other.bankrupt) continue
                    autoLiquidate(other.id, e.amount)
                    if (other.balance >= e.amount) {
                        Bank.payToPlayer(state, other.id, p.id, e.amount, "اثر کارت «${card.title}»")
                    } else {
                        declareBankruptcy(other.id, p.id, "ناتوانی در پرداخت کارت")
                        if (state.isGameOver) return
                    }
                }
            }
            is CardEffect.PayEachPlayer -> {
                for (other in state.activePlayers()) {
                    if (other.id == p.id || other.bankrupt) continue
                    autoLiquidate(p.id, e.amount)
                    if (p.balance >= e.amount) {
                        Bank.payToPlayer(state, p.id, other.id, e.amount, "اثر کارت «${card.title}»")
                    } else {
                        declareBankruptcy(p.id, other.id, "ناتوانی در پرداخت کارت")
                        return
                    }
                }
            }
        }
    }

    // ---------------- ساخت، رهن، آزادسازی (پوشش برای UI و AI) ----------------

    fun playerBuild(playerId: Int, spaceId: Int): String? {
        if (state.isGameOver) return "بازی تمام شده است"
        val err = BuildingSystem.build(state, playerId, spaceId)
        if (err == null) checkDebt()
        return err
    }

    fun playerSellBuilding(playerId: Int, spaceId: Int): String? {
        if (state.isGameOver) return "بازی تمام شده است"
        val err = BuildingSystem.sell(state, playerId, spaceId)
        if (err == null) checkDebt()
        return err
    }

    fun playerMortgage(playerId: Int, spaceId: Int): String? {
        if (state.isGameOver) return "بازی تمام شده است"
        val err = BuildingSystem.mortgage(state, playerId, spaceId)
        if (err == null) checkDebt()
        return err
    }

    fun playerRedeem(playerId: Int, spaceId: Int): String? {
        if (state.isGameOver) return "بازی تمام شده است"
        val err = BuildingSystem.redeem(state, playerId, spaceId)
        if (err == null) checkDebt()
        return err
    }

    /** آزادسازی خودکار ارزان‌ترین رهن وقتی بازیکن پول اضافه دارد (برای AI) */
    fun autoRedeemCheapest(playerId: Int): Boolean {
        val p = state.players[playerId]
        if (p.bankrupt) return false
        val threshold = state.rules.startMoney / 3
        val candidates = state.ownedBy(playerId)
            .filter { state.prop(it).mortgaged }
            .sortedBy { BuildingSystem.redeemCost(state.space(it)) }
        for (id in candidates) {
            val cost = BuildingSystem.redeemCost(state.space(id))
            if (p.balance - cost >= threshold) {
                return BuildingSystem.redeem(state, playerId, id) == null
            }
        }
        return false
    }

    // ---------------- معامله ----------------

    fun proposeTrade(offer: TradeOffer): List<LogEntry> {
        val before = state.log.size
        val err = TradingSystem.validate(state, offer)
        if (err != null) {
            state.addLog("معامله نامعتبر: $err", offer.fromId, LogEntry.KIND_TRADE)
            return tailEvents(before)
        }
        val target = state.players[offer.toId]
        if (target.isHuman) {
            state.pendingTrade = offer
            state.awaiting = DecisionKind.TRADE_RESPONSE
            state.addLog("${state.players[offer.fromId].name} به ${target.name} پیشنهاد معامله داد.", offer.fromId, LogEntry.KIND_TRADE)
        } else {
            // پاسخ فوری هوش مصنوعی
            val brain = AIController.forPlayer(state, target)
            val response = brain.evaluateTrade(state, target.id, offer)
            when (response) {
                AIPlayer.TradeResponse.ACCEPT -> {
                    if (TradingSystem.execute(state, offer))
                        state.addLog("${target.name} پیشنهاد معامله را پذیرفت.", target.id, LogEntry.KIND_TRADE)
                }
                AIPlayer.TradeResponse.REJECT ->
                    state.addLog("${target.name} پیشنهاد معامله را رد کرد.", target.id, LogEntry.KIND_TRADE)
                AIPlayer.TradeResponse.COUNTER -> {
                    val counter = brain.makeCounterOffer(state, target.id, offer)
                    if (counter != null) {
                        if (offer.fromId == state.currentTurn && state.players[offer.fromId].isHuman) {
                            state.pendingTrade = counter
                            state.awaiting = DecisionKind.TRADE_RESPONSE
                            state.addLog("${target.name} پیشنهاد متقابل داد.", target.id, LogEntry.KIND_TRADE)
                        } else {
                            val err2 = TradingSystem.validate(state, counter)
                            if (err2 == null && TradingSystem.execute(state, counter)) {
                                state.addLog("${target.name} پیشنهاد متقابل داد و معامله انجام شد.", target.id, LogEntry.KIND_TRADE)
                            } else {
                                state.addLog("${target.name} پیشنهاد متقابل داد که پذیرفته نشد.", target.id, LogEntry.KIND_TRADE)
                            }
                        }
                    } else {
                        state.addLog("${target.name} پیشنهاد معامله را رد کرد.", target.id, LogEntry.KIND_TRADE)
                    }
                }
            }
        }
        return tailEvents(before)
    }

    fun respondTrade(accept: Boolean): List<LogEntry> {
        val before = state.log.size
        val offer = state.pendingTrade ?: return emptyList()
        if (accept) {
            if (!TradingSystem.execute(state, offer)) {
                state.addLog("اجرای معامله ممکن نشد.", offer.fromId, LogEntry.KIND_TRADE)
            }
        } else {
            state.addLog("${state.players[offer.toId].name} پیشنهاد معامله را رد کرد.", offer.toId, LogEntry.KIND_TRADE)
        }
        state.pendingTrade = null
        if (state.awaiting == DecisionKind.TRADE_RESPONSE) state.awaiting = DecisionKind.NONE
        checkDebt()
        return tailEvents(before)
    }
}
