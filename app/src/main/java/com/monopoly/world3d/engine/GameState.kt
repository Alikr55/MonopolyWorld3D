package com.monopoly.world3d.engine

import java.util.Random

/** حالت کامل بازی — تنها منبع حقیقت (Single Source of Truth) */
class GameState(
    val players: MutableList<Player>,
    val rules: GameRules,
    var seed: Long = System.currentTimeMillis()
) {
    val spaces: List<Space> = Board.createSpaces()
    val properties: MutableList<PropertyState> = MutableList(Board.BOARD_SIZE) { PropertyState() }

    /** صف کارت‌ها (شناسه کارت) */
    var chanceDeck: MutableList<Int> = mutableListOf()
    var chestDeck: MutableList<Int> = mutableListOf()

    var currentTurn: Int = 0            // ایندکس بازیکن نوبت‌دار
    var roundCount: Int = 1
    var doublesCount: Int = 0           // تاس جفت‌های متوالی در این نوبت
    var dice1: Int = 1
    var dice2: Int = 1

    var awaiting: DecisionKind = DecisionKind.NONE
    var pendingPurchaseSpace: Int = -1  // خانه‌ای که منتظر تصمیم خرید است
    var hasRolled: Boolean = false      // در این نوبت تاس ریخته شده؟
    var rollAgain: Boolean = false      // تاس جفت آمده و نوبت ادامه دارد
    var jailAfterDebtMove: Int = 0      // اگر پس از تسویه‌ی جریمه زندان باید حرکت کند

    /** آخرین حرکت برای انیمیشن رابط کاربری */
    var lastMovePath: List<Int> = emptyList()
    var lastDrawnCardId: Int = -1
    var lastDrawnCardDeck: Int = 0

    /** صف حراج املاکی که مالکشان به بانک ورشکسته شده‌اند */
    val auctionQueue: MutableList<Int> = mutableListOf()

    /** اگر پس از بارگذاری، حراج باید از نو آغاز شود، شناسه‌ی خانه در اینجا است */
    var pendingAuctionRestart: Int = -1

    /** وضعیت حراج جاری (اگر فعال باشد) */
    var auction: AuctionState? = null

    /** بدهی معوق: مبلغ و طلبکار (-1 = بانک) */
    var debtAmount: Long = 0
    var debtCreditor: Int = -1
    var debtReason: String = ""

    /** پیشنهاد معامله‌ی در انتظار پاسخ انسان */
    var pendingTrade: TradeOffer? = null

    /** صندوق پارکینگ آزاد (در صورت فعال بودن قانون) */
    var parkingPot: Long = 0

    /** خانه و هتل باقی‌مانده در بانک */
    var bankHouses: Int = rules.houseBankLimit
    var bankHotels: Int = rules.hotelBankLimit

    val log: MutableList<LogEntry> = mutableListOf()
    val playerStats: MutableList<PlayerGameStats> =
        MutableList(players.size) { PlayerGameStats() }

    var gameStartedAtMillis: Long = System.currentTimeMillis()
    var winnerId: Int = -1
    var turnCounter: Int = 0            // شمارنده‌ی کلی نوبت‌ها

    var rng: Random = Random(seed)

    val currentPlayer: Player get() = players[currentTurn]
    val isGameOver: Boolean get() = awaiting == DecisionKind.GAME_OVER

    fun activePlayers(): List<Player> = players.filter { !it.bankrupt }

    fun space(id: Int): Space = spaces[id]
    fun prop(id: Int): PropertyState = properties[id]

    fun ownedBy(playerId: Int): List<Int> =
        properties.indices.filter { properties[it].owner == playerId }

    /** ارزش خالص بازیکن: پول نقد + ملک‌های آزاد + رهنی‌ها (با کسر بدهی رهن) + نصف خانه‌ها */
    fun netWorth(playerId: Int): Long {
        val p = players[playerId]
        var total = if (p.bankrupt) 0 else p.balance
        for (id in ownedBy(playerId)) {
            val ps = properties[id]
            val sp = spaces[id]
            if (ps.mortgaged) {
                total += sp.mortgageValue // رهنی: ارزش رهن منهای ۱۰٪ بهره در هنگام آزادسازی
                total -= sp.mortgageValue / 10
            } else {
                total += sp.price
            }
            if (Board.isStreetGroup(sp.group)) {
                total += ps.houses * sp.houseCost / 2  // خانه‌ها در فروش نصف قیمت برمی‌گردند
            }
        }
        return total
    }

    fun addLog(text: String, player: Int = -1, kind: Int = LogEntry.KIND_INFO): LogEntry {
        val entry = LogEntry(turnCounter, player, text, kind)
        log.add(entry)
        if (log.size > 400) log.removeAt(0)
        return entry
    }

    fun statsFor(playerId: Int): PlayerGameStats = playerStats[playerId]

    /** آماده‌سازی دسته‌ی کارت‌ها با rng — لیست‌ها در جا پر می‌شوند تا اشاره‌گرهای موجود معتبر بمانند */
    fun shuffleDecks() {
        chanceDeck.clear()
        chanceDeck.addAll(Cards.chanceIds())
        chestDeck.clear()
        chestDeck.addAll(Cards.chestIds())
        chanceDeck.shuffle(rng)
        chestDeck.shuffle(rng)
    }
}

/** وضعیت حراج یک ملک */
class AuctionState(
    val spaceId: Int,
    val participants: MutableList<Int>,   // شناسه‌ی بازیکنان فعال در حراج
    var highestBid: Long = 0,
    var highestBidder: Int = -1,          // -1 = هنوز پیشنهادی نیست
    var currentBidderIndex: Int = 0       // ایندکس در participants
) {
    fun currentBidder(): Int = if (participants.isEmpty()) -1 else participants[currentBidderIndex]
    fun minimumNextBid(startPrice: Long): Long =
        if (highestBidder == -1) startPrice else highestBid + 10_000
}
