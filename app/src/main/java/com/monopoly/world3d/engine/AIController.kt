package com.monopoly.world3d.engine

import kotlin.math.max
import kotlin.math.min

/**
 * نقطه‌ی دسترسی موتور به هوش مصنوعی بازیکن.
 */
object AIController {
    fun forPlayer(state: GameState, player: Player): AIPlayer = AIPlayer.forPlayer(state, player)
}

/**
 * هوش مصنوعی بازیکنان — سه سطح دشواری و شش شخصیت.
 * تصمیم‌ها بر پایه‌ی پول، قیمت، اجاره‌ی بالقوه، گروه رنگی، وضعیت حریفان و ریسک ورشکستگی است.
 */
class AIPlayer private constructor(
    val level: AiLevel,
    val personality: AiPersonality
) {

    companion object {
        fun forPlayer(state: GameState, player: Player): AIPlayer =
            AIPlayer(player.aiLevel, player.personality)

        /** سقف پول نقدی که AI برای خودش نگه می‌دارد */
        private fun cashReserve(state: GameState, personality: AiPersonality, level: AiLevel): Long {
            val base = state.rules.startMoney * personality.cashReserve
            return when (level) {
                AiLevel.EASY -> (base * 0.4).toLong()
                AiLevel.NORMAL -> base.toLong()
                AiLevel.HARD -> base.toLong()
            }
        }
    }

    // ---------------- ارزش‌گذاری املاک ----------------

    /** ارزش راهبردی یک ملک برای بازیکن (به تومان) */
    fun propertyValue(state: GameState, playerId: Int, spaceId: Int): Long {
        val sp = state.space(spaceId)
        if (sp.price <= 0) return 0
        val group = sp.group ?: return 0
        var value = sp.price.toDouble()

        if (Board.isStreetGroup(group)) {
            val groupIds = state.properties.indices.filter { state.space(it).group == group }
            val mine = groupIds.count { state.prop(it).owner == playerId }
            val others = groupIds.filter { state.prop(it).owner != playerId }
            val ownedByOneOpponent = others.size == 1 && state.prop(others.first()).owner >= 0
            // پیشرفت به سمت مونوپولی
            value += sp.price * (mine.toDouble() / groupIds.size) * 0.8
            // بازدارندگی: نگذار حریف مونوپولی بسازد
            if (ownedByOneOpponent && others.first().let { state.prop(it).owner } != playerId) {
                value += sp.price * 0.5 * personality.aggression
            }
            // اجاره‌ی بالقوه
            value += sp.rents.getOrElse(2) { 0 } * 0.6
        } else if (group == Group.RAILROAD) {
            val mine = state.ownedBy(playerId).count { state.space(it).type == SpaceType.RAILROAD }
            value += mine * sp.price * 0.25
        } else if (group == Group.UTILITY) {
            val mine = state.ownedBy(playerId).count { state.space(it).type == SpaceType.UTILITY }
            value += mine * sp.price * 0.2
        }
        return value.toLong()
    }

    // ---------------- تصمیم‌ها ----------------

    /** ۰ = پرداخت جریمه، ۱ = کارت، ۲ = تلاش با تاس */
    fun jailDecision(state: GameState, playerId: Int): Int {
        val p = state.players[playerId]
        if (p.jailCards > 0) return 1
        return when (level) {
            AiLevel.EASY -> if (p.balance > state.rules.jailFine * 4) 0 else 2
            AiLevel.NORMAL -> if (state.roundCount <= 2) 2 else 0
            AiLevel.HARD -> if (state.roundCount <= 3 && p.balance > state.rules.jailFine * 3) 2 else 0
        }
    }

    fun wantBuy(state: GameState, playerId: Int, spaceId: Int): Boolean {
        val p = state.players[playerId]
        val sp = state.space(spaceId)
        if (p.balance < sp.price) return false
        val cashAfter = p.balance - sp.price
        val reserve = cashReserve(state, personality, level)
        return when (level) {
            AiLevel.EASY -> {
                val noise = 0.7 + state.rng.nextDouble() * 0.6
                cashAfter >= reserve * 0.5 && sp.price <= state.rules.startMoney * noise
            }
            AiLevel.NORMAL -> {
                cashAfter >= reserve * 0.6 && propertyValue(state, playerId, spaceId) >= sp.price * 0.85
            }
            AiLevel.HARD -> {
                val value = propertyValue(state, playerId, spaceId)
                if (value >= sp.price * 1.1) cashAfter >= reserve * 0.4
                else cashAfter >= reserve && value >= sp.price * 0.9
            }
        }
    }

    /** بیشترین مبلغی که در حراج حاضر است بدهد */
    fun auctionMaxBid(state: GameState, playerId: Int, spaceId: Int): Long {
        val p = state.players[playerId]
        val value = propertyValue(state, playerId, spaceId).toDouble()
        val reserve = cashReserve(state, personality, level)
        val budget = (p.balance - reserve).coerceAtLeast(0)
        return when (level) {
            AiLevel.EASY -> min(budget, (state.space(spaceId).price * (0.5 + state.rng.nextDouble() * 0.3)).toLong())
            AiLevel.NORMAL -> min(budget, (value * (0.85 + personality.aggression * 0.15)).toLong())
            AiLevel.HARD -> min(budget, (value * (0.95 + personality.aggression * 0.25)).toLong())
        }
    }

    /** ملکی که می‌خواهد در آن بسازد یا null */
    fun wantBuild(state: GameState, playerId: Int): Int? {
        val p = state.players[playerId]
        if (state.hasRolled) return null // بعد از ریختن تاس فقط در نوبت بعد
        val reserve = cashReserve(state, personality, level)
        val candidates = mutableListOf<Pair<Int, Long>>() // spaceId, firstEstateRent
        for (id in state.ownedBy(playerId)) {
            val sp = state.space(id)
            if (sp.type != SpaceType.STREET) continue
            if (!BuildingSystem.ownsFullGroup(state, playerId, sp.group!!)) continue
            val err = BuildingSystem.canBuild(state, playerId, id)
            if (err != null) continue
            if (p.balance - sp.houseCost < reserve) continue
            val monopolyBonus = if (state.rules.doubleRentOnMonopoly) sp.rents[0] else 0
            candidates.add(id to sp.rents.getOrElse(1) { 0 } + monopolyBonus)
        }
        if (candidates.isEmpty()) return null
        return when (level) {
            AiLevel.EASY -> if (state.rng.nextDouble() < 0.5) candidates[state.rng.nextInt(candidates.size)].first else null
            AiLevel.NORMAL -> candidates.maxByOrNull { it.second }?.first
            AiLevel.HARD -> candidates.maxByOrNull { it.second }?.first
        }
    }

    /** برای نقدسازی: ساختمانی که بهتر است بفروشد */
    fun wantSellBuilding(state: GameState, playerId: Int): Int? =
        state.ownedBy(playerId)
            .filter { state.prop(it).houses > 0 }
            .maxByOrNull { state.prop(it).houses }

    /** برای نقدسازی: ملکی که بهتر است رهن گذاشته شود (ارزان‌ترین غیرمونوپولی) */
    fun wantMortgage(state: GameState, playerId: Int): Int? {
        val owned = state.ownedBy(playerId).filter { !state.prop(it).mortgaged }
        val nonMonopoly = owned.filter {
            val g = state.space(it).group
            g == null || !BuildingSystem.ownsFullGroup(state, playerId, g)
        }
        val pool = nonMonopoly.ifEmpty { owned }
        return pool.minByOrNull { state.space(it).price }
    }

    // ---------------- معامله ----------------

    /** ارزش کل یک پیشنهاد برای گیرنده (مثبت = به نفع من) */
    private fun tradeValue(state: GameState, playerId: Int, offer: TradeOffer, asReceiver: Boolean): Long {
        // از دید گیرنده: آنچه می‌دهد get* پیشنهاد است و آنچه می‌گیرد give* پیشنهاد
        val give = if (asReceiver) offer.getProperties else offer.giveProperties
        val get = if (asReceiver) offer.giveProperties else offer.getProperties
        val giveMoney = if (asReceiver) offer.getMoney else offer.giveMoney
        val getMoney = if (asReceiver) offer.giveMoney else offer.getMoney
        var value = getMoney - giveMoney
        for (id in get) value += propertyValue(state, playerId, id)
        for (id in give) value -= propertyValue(state, playerId, id)
        value += offer.getJailCards * 50_000L - offer.giveJailCards * 50_000L
        return value
    }

    fun evaluateTrade(state: GameState, playerId: Int, offer: TradeOffer): TradeResponse {
        val p = state.players[playerId]
        if (p.bankrupt) return TradeResponse.REJECT
        if (offer.getMoney > p.balance) return TradeResponse.REJECT
        val value = tradeValue(state, playerId, offer, asReceiver = true)
        val margin = when (level) {
            AiLevel.EASY -> 1.0 - 0.25 + state.rng.nextDouble() * 0.3   // بی‌دقت
            AiLevel.NORMAL -> 1.0 + (1.0 - personality.tradeWillingness) * 0.15
            AiLevel.HARD -> 1.0 + (1.0 - personality.tradeWillingness) * 0.10
        }
        val threshold = (state.rules.startMoney * 0.02).toLong()
        return when {
            value >= threshold * margin -> TradeResponse.ACCEPT
            value >= -threshold -> TradeResponse.COUNTER
            else -> TradeResponse.REJECT
        }
    }

    fun makeCounterOffer(state: GameState, playerId: Int, offer: TradeOffer): TradeOffer? {
        // پیشنهاد متقابل: همان املاک با جبران اختلاف پول
        val deficit = -tradeValue(state, playerId, offer, asReceiver = true)
        val p = state.players[playerId]
        val extra = (deficit * 1.2).toLong().coerceAtMost(p.balance / 2)
        if (extra <= 0) return null
        return TradeOffer(
            fromId = offer.toId,
            toId = offer.fromId,
            giveProperties = offer.getProperties,
            getProperties = offer.giveProperties,
            giveMoney = offer.getMoney + extra,
            getMoney = offer.giveMoney,
            giveJailCards = offer.getJailCards,
            getJailCards = offer.giveJailCards,
            isCounter = true
        )
    }

    /** پیشنهاد معامله‌ی فعال از سوی AI (برای تکمیل مونوپولی) */
    fun maybeProposeTrade(state: GameState, playerId: Int): TradeOffer? {
        val rate = if (level == AiLevel.EASY) 0.15 else 0.35
        if (state.rng.nextDouble() > personality.tradeWillingness * rate) return null
        val p = state.players[playerId]
        // گروه‌هایی که فقط یک ملکش را ندارم
        for (group in listOf(Group.BROWN, Group.LIGHT_BLUE, Group.PINK, Group.ORANGE, Group.RED, Group.YELLOW, Group.GREEN, Group.DARK_BLUE)) {
            val ids = state.properties.indices.filter { state.space(it).group == group }
            val mine = ids.filter { state.prop(it).owner == playerId }
            if (mine.isEmpty() || mine.size != ids.size - 1) continue
            val missing = ids.first { state.prop(it).owner != playerId }
            val owner = state.prop(missing).owner
            if (owner < 0) continue
            val target = state.players[owner]
            val myMoney = (p.balance - cashReserve(state, personality, level)).coerceAtLeast(0)
            val offerMoney = myMoney.coerceAtMost(propertyValue(state, playerId, missing) + state.rules.startMoney / 4)
            if (offerMoney <= 0) continue
            val offer = TradeOffer(
                fromId = playerId, toId = owner,
                getProperties = listOf(missing),
                giveMoney = offerMoney
            )
            if (TradingSystem.validate(state, offer) == null) return offer
        }
        return null
    }

    enum class TradeResponse { ACCEPT, REJECT, COUNTER }
}

/**
 * درایور اجرای AI — یک گام از تصمیم AI را اجرا می‌کند.
 * هم رابط کاربری اندروید و هم تست‌های JVM از این استفاده می‌کنند.
 */
object AiDriver {

    /** آیا اکنون تصمیمی بر عهده‌ی AI است؟ */
    fun isAiTurn(engine: Engine): Boolean {
        val state = engine.state
        if (state.isGameOver) return false
        return when (state.awaiting) {
            DecisionKind.NONE -> !state.currentPlayer.isHuman
            DecisionKind.BUY_PROMPT, DecisionKind.JAIL_DECISION, DecisionKind.DEBT -> !state.currentPlayer.isHuman
            DecisionKind.AUCTION -> {
                val a = state.auction
                a != null && a.currentBidder() >= 0 && !state.players[a.currentBidder()].isHuman
            }
            DecisionKind.TRADE_RESPONSE -> false
            DecisionKind.GAME_OVER -> false
        }
    }

    /** یک گام AI را اجرا می‌کند؛ اگر اقدامی انجام شد true برمی‌گرداند */
    fun step(engine: Engine): Boolean {
        val state = engine.state
        if (state.isGameOver || !isAiTurn(engine)) return false
        val p = state.currentPlayer
        val brain = AIPlayer.forPlayer(state, p)
        when (state.awaiting) {
            DecisionKind.NONE -> {
                if (engine.canEndTurn()) {
                    // پیش از پایان نوبت شانس معامله‌ی آزادسازی رهن
                    if (engine.autoRedeemCheapest(p.id)) return true
                    engine.endTurn()
                    return true
                }
                // مدیریت پیش از تاس: ساخت
                val buildTarget = brain.wantBuild(state, p.id)
                if (buildTarget != null) {
                    engine.playerBuild(p.id, buildTarget)
                    return true
                }
                // پیشنهاد معامله
                val offer = brain.maybeProposeTrade(state, p.id)
                if (offer != null) {
                    engine.proposeTrade(offer)
                    return true
                }
                engine.rollDice()
                return true
            }
            DecisionKind.BUY_PROMPT -> {
                val spaceId = state.pendingPurchaseSpace
                if (spaceId >= 0 && brain.wantBuy(state, p.id, spaceId)) engine.buyCurrentProperty()
                else engine.declinePurchase()
                return true
            }
            DecisionKind.JAIL_DECISION -> {
                when (brain.jailDecision(state, p.id)) {
                    0 -> engine.payJailFine()
                    1 -> engine.useJailCard()
                    else -> engine.rollDice()
                }
                return true
            }
            DecisionKind.AUCTION -> {
                val auction = state.auction ?: return false
                val bidder = auction.currentBidder()
                if (bidder < 0 || state.players[bidder].isHuman) return false
                val minBid = auction.minimumNextBid(state.space(auction.spaceId).price)
                val maxBid = brain.auctionMaxBid(state, bidder, auction.spaceId)
                // مبلغ واقعی هرگز از موجودی بیشتر نیست؛ اگر از حداقل کمتر شد باید انصراف دهد
                // وگرنه auctionBid بی‌صدا رد می‌شود و بازی هنگ می‌کند
                val amount = min(maxBid, state.players[bidder].balance)
                if (amount >= minBid && state.rng.nextDouble() < 0.85) {
                    engine.auctionBid(bidder, amount)
                } else {
                    engine.auctionPass(bidder)
                }
                return true
            }
            DecisionKind.DEBT -> {
                if (state.debtAmount <= 0) {
                    engine.checkDebt()
                    return true
                }
                val sell = brain.wantSellBuilding(state, p.id)
                if (sell != null) {
                    engine.playerSellBuilding(p.id, sell)
                    return true
                }
                val mortgage = brain.wantMortgage(state, p.id)
                if (mortgage != null) {
                    engine.playerMortgage(p.id, mortgage)
                    return true
                }
                engine.checkDebt()
                return true
            }
            else -> return false
        }
    }

    /** AI تا جایی که تصمیمش است پیش می‌رود (برای تست‌ها) */
    fun runUntilHumanOrBlocked(engine: Engine, maxSteps: Int = 5000): Int {
        var steps = 0
        while (steps < maxSteps) {
            if (state_isTradePendingForHuman(engine)) break
            if (!step(engine)) break
            steps++
        }
        return steps
    }

    private fun state_isTradePendingForHuman(engine: Engine): Boolean =
        engine.state.awaiting == DecisionKind.TRADE_RESPONSE

    /** کامل تا پایان بازی (فقط AI ها) — برای تست خودکار */
    fun runFullGame(engine: Engine, maxSteps: Int = 200_000): Int {
        var steps = 0
        while (!engine.state.isGameOver && steps < maxSteps) {
            if (!step(engine)) {
                // بن‌بست؟ برای جلوگیری از حلقه‌ی بی‌پایان، نوبت را می‌بندیم
                if (engine.state.awaiting == DecisionKind.TRADE_RESPONSE) engine.respondTrade(false)
                else if (engine.canEndTurn()) engine.endTurn()
                else if (engine.state.awaiting == DecisionKind.NONE && engine.state.hasRolled) engine.endTurn()
                else break
            }
            steps++
        }
        return steps
    }
}
