package com.monopoly.world3d.engine

/**
 * ذخیره و بازیابی کامل وضعیت بازی به صورت JSON.
 * مستقل از اندروید؛ لایه‌ی UI مدیریت فایل‌ها و پشتیبان (.bak) را انجام می‌دهد.
 */
object SaveManager {

    const val FORMAT_VERSION = 1

    class SaveException(msg: String) : Exception(msg)

    fun serialize(state: GameState): String {
        val root = linkedMapOf<String, Any?>(
            "version" to FORMAT_VERSION,
            "savedAt" to System.currentTimeMillis(),
            "seed" to state.seed,
            "currentTurn" to state.currentTurn,
            "roundCount" to state.roundCount,
            "doublesCount" to state.doublesCount,
            "dice1" to state.dice1,
            "dice2" to state.dice2,
            "awaiting" to state.awaiting.name,
            "hasRolled" to state.hasRolled,
            "rollAgain" to state.rollAgain,
            "pendingPurchaseSpace" to state.pendingPurchaseSpace,
            "debtAmount" to state.debtAmount,
            "debtCreditor" to state.debtCreditor,
            "debtReason" to state.debtReason,
            "jailAfterDebtMove" to state.jailAfterDebtMove,
            "parkingPot" to state.parkingPot,
            "bankHouses" to state.bankHouses,
            "bankHotels" to state.bankHotels,
            "turnCounter" to state.turnCounter,
            "winnerId" to state.winnerId,
            "gameStartedAtMillis" to state.gameStartedAtMillis,
            "chanceDeck" to state.chanceDeck,
            "chestDeck" to state.chestDeck,
            "auctionQueue" to state.auctionQueue,
            "auctionSpaceId" to state.auction?.spaceId,
            "rules" to linkedMapOf(
                "startMoney" to state.rules.startMoney,
                "goSalary" to state.rules.goSalary,
                "doubleSalaryOnExactGo" to state.rules.doubleSalaryOnExactGo,
                "freeParkingPot" to state.rules.freeParkingPot,
                "auctionsEnabled" to state.rules.auctionsEnabled,
                "jailFine" to state.rules.jailFine,
                "maxJailTurns" to state.rules.maxJailTurns,
                "incomeTax" to state.rules.incomeTax,
                "luxuryTax" to state.rules.luxuryTax,
                "doubleRentOnMonopoly" to state.rules.doubleRentOnMonopoly,
                "houseBankLimit" to state.rules.houseBankLimit,
                "hotelBankLimit" to state.rules.hotelBankLimit
            ),
            "players" to state.players.map { p ->
                linkedMapOf(
                    "name" to p.name, "token" to p.token.name, "colorHex" to p.colorHex,
                    "isHuman" to p.isHuman, "aiLevel" to p.aiLevel.name,
                    "personality" to p.personality.name,
                    "balance" to p.balance, "position" to p.position,
                    "inJail" to p.inJail, "jailTurns" to p.jailTurns,
                    "jailCards" to p.jailCards, "bankrupt" to p.bankrupt
                )
            },
            "properties" to state.properties.map { ps ->
                linkedMapOf("owner" to ps.owner, "houses" to ps.houses, "mortgaged" to ps.mortgaged)
            },
            "playerStats" to state.playerStats.map { st ->
                linkedMapOf(
                    "rentCollected" to st.rentCollected, "rentPaid" to st.rentPaid,
                    "taxesPaid" to st.taxesPaid, "propertiesBought" to st.propertiesBought,
                    "housesBuilt" to st.housesBuilt, "hotelsBuilt" to st.hotelsBuilt,
                    "tradesDone" to st.tradesDone, "auctionsWon" to st.auctionsWon,
                    "cardsDrawn" to st.cardsDrawn, "timesInJail" to st.timesInJail,
                    "goPasses" to st.goPasses, "biggestBalance" to st.biggestBalance,
                    "totalEarned" to st.totalEarned, "totalSpent" to st.totalSpent,
                    "doublesRolled" to st.doublesRolled
                )
            },
            "log" to state.log.takeLast(120).map {
                linkedMapOf("turn" to it.turn, "player" to it.player, "text" to it.text, "kind" to it.kind)
            }
        )
        return MiniJson.write(root)
    }

    fun parse(json: String): GameState {
        val root = try {
            MiniJson.parse(json) as? Map<*, *> ?: throw SaveException("ساختار فایل نامعتبر است")
        } catch (e: MiniJson.JsonException) {
            throw SaveException("فایل ذخیره خراب است: ${e.message}")
        }
        fun num(k: String, d: Long = 0): Long = (root[k] as? Number)?.toLong() ?: d
        fun int(k: String, d: Int = 0): Int = (root[k] as? Number)?.toInt() ?: d
        fun bool(k: String, d: Boolean = false): Boolean = root[k] as? Boolean ?: d
        fun str(k: String, d: String = ""): String = root[k] as? String ?: d

        val version = int("version", 0)
        if (version < 1 || version > FORMAT_VERSION) throw SaveException("نسخه‌ی فایل ذخیره پشتیبانی نمی‌شود")

        val rulesMap = root["rules"] as? Map<*, *> ?: throw SaveException("قوانین بازی یافت نشد")
        val rules = GameRules().apply {
            startMoney = ((rulesMap["startMoney"] as? Number)?.toLong() ?: startMoney)
            goSalary = ((rulesMap["goSalary"] as? Number)?.toLong() ?: goSalary)
            doubleSalaryOnExactGo = rulesMap["doubleSalaryOnExactGo"] as? Boolean ?: false
            freeParkingPot = rulesMap["freeParkingPot"] as? Boolean ?: true
            auctionsEnabled = rulesMap["auctionsEnabled"] as? Boolean ?: true
            jailFine = ((rulesMap["jailFine"] as? Number)?.toLong() ?: jailFine)
            maxJailTurns = ((rulesMap["maxJailTurns"] as? Number)?.toInt() ?: 3)
            incomeTax = ((rulesMap["incomeTax"] as? Number)?.toLong() ?: incomeTax)
            luxuryTax = ((rulesMap["luxuryTax"] as? Number)?.toLong() ?: luxuryTax)
            doubleRentOnMonopoly = rulesMap["doubleRentOnMonopoly"] as? Boolean ?: true
            houseBankLimit = ((rulesMap["houseBankLimit"] as? Number)?.toInt() ?: 32)
            hotelBankLimit = ((rulesMap["hotelBankLimit"] as? Number)?.toInt() ?: 12)
        }

        val playersArr = root["players"] as? List<*> ?: throw SaveException("فهرست بازیکنان یافت نشد")
        if (playersArr.size < 2 || playersArr.size > 6) throw SaveException("تعداد بازیکنان نامعتبر است")
        val fixedPlayers = mutableListOf<Player>()
        playersArr.forEachIndexed { i, pm ->
            val m = pm as? Map<*, *> ?: throw SaveException("داده‌ی بازیکن نامعتبر")
            val p = Player(
                id = i,
                name = m["name"] as? String ?: "بازیکن ${i + 1}",
                token = try { TokenKind.valueOf(m["token"] as? String ?: "CROWN") } catch (e: Exception) { TokenKind.CROWN },
                colorHex = ((m["colorHex"] as? Number)?.toLong() ?: 0xFFD32F2F),
                isHuman = m["isHuman"] as? Boolean ?: false,
                aiLevel = try { AiLevel.valueOf(m["aiLevel"] as? String ?: "NORMAL") } catch (e: Exception) { AiLevel.NORMAL },
                personality = AiPersonality.byName(m["personality"] as? String ?: "BALANCED")
            )
            p.balance = ((m["balance"] as? Number)?.toLong() ?: 0).coerceAtLeast(0)
            p.position = ((m["position"] as? Number)?.toInt() ?: 0).coerceIn(0, 39)
            p.inJail = m["inJail"] as? Boolean ?: false
            p.jailTurns = ((m["jailTurns"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            p.jailCards = ((m["jailCards"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            p.bankrupt = m["bankrupt"] as? Boolean ?: false
            fixedPlayers.add(p)
        }

        val seed = num("seed", 1L)
        val state = GameState(fixedPlayers, rules, seed)
        state.rng = java.util.Random(seed + state.turnCounter)

        val propsArr = root["properties"] as? List<*>
        if (propsArr != null && propsArr.size == Board.BOARD_SIZE) {
            propsArr.forEachIndexed { i, pm ->
                val m = pm as? Map<*, *> ?: return@forEachIndexed
                state.properties[i].owner = ((m["owner"] as? Number)?.toInt() ?: -1).coerceIn(-1, fixedPlayers.size - 1)
                state.properties[i].houses = ((m["houses"] as? Number)?.toInt() ?: 0).coerceIn(0, 5)
                state.properties[i].mortgaged = m["mortgaged"] as? Boolean ?: false
            }
        }

        fun readDeck(k: String, valid: List<Int>): MutableList<Int> {
            val arr = root[k] as? List<*> ?: return valid.toMutableList()
            val ids = arr.mapNotNull { (it as? Number)?.toInt() }.filter { valid.contains(it) }
            return if (ids.size == valid.size) ids.toMutableList() else valid.toMutableList()
        }
        state.chanceDeck = readDeck("chanceDeck", Cards.chanceIds())
        state.chestDeck = readDeck("chestDeck", Cards.chestIds())

        state.auctionQueue.clear()
        (root["auctionQueue"] as? List<*>)?.forEach { qid ->
            (qid as? Number)?.toInt()?.let { if (it in 0..39) state.auctionQueue.add(it) }
        }

        state.currentTurn = int("currentTurn").coerceIn(0, fixedPlayers.size - 1)
        if (state.players[state.currentTurn].bankrupt) {
            state.currentTurn = state.players.indexOfFirst { !it.bankrupt }.let { if (it < 0) 0 else it }
        }
        state.roundCount = int("roundCount", 1).coerceAtLeast(1)
        state.doublesCount = int("doublesCount").coerceIn(0, 2)
        state.dice1 = int("dice1", 1).coerceIn(1, 6)
        state.dice2 = int("dice2", 1).coerceIn(1, 6)
        state.hasRolled = bool("hasRolled")
        state.rollAgain = bool("rollAgain")
        state.pendingPurchaseSpace = int("pendingPurchaseSpace", -1).let { if (it in 0..39) it else -1 }
        state.debtAmount = num("debtAmount")
        state.debtCreditor = int("debtCreditor", -1)
        state.debtReason = str("debtReason")
        state.jailAfterDebtMove = int("jailAfterDebtMove")
        state.parkingPot = num("parkingPot").coerceAtLeast(0)
        state.bankHouses = int("bankHouses", rules.houseBankLimit).coerceIn(0, rules.houseBankLimit)
        state.bankHotels = int("bankHotels", rules.hotelBankLimit).coerceIn(0, rules.hotelBankLimit)
        state.turnCounter = int("turnCounter")
        state.winnerId = int("winnerId", -1)
        state.gameStartedAtMillis = num("gameStartedAtMillis", System.currentTimeMillis())

        val statsArr = root["playerStats"] as? List<*>
        if (statsArr != null && statsArr.size == state.playerStats.size) {
            statsArr.forEachIndexed { i, sm ->
                val m = sm as? Map<*, *> ?: return@forEachIndexed
                fun li(k: String): Long = (m[k] as? Number)?.toLong() ?: 0
                fun ii(k: String): Int = (m[k] as? Number)?.toInt() ?: 0
                val st = state.playerStats[i]
                st.rentCollected = li("rentCollected"); st.rentPaid = li("rentPaid")
                st.taxesPaid = li("taxesPaid"); st.propertiesBought = ii("propertiesBought")
                st.housesBuilt = ii("housesBuilt"); st.hotelsBuilt = ii("hotelsBuilt")
                st.tradesDone = ii("tradesDone"); st.auctionsWon = ii("auctionsWon")
                st.cardsDrawn = ii("cardsDrawn"); st.timesInJail = ii("timesInJail")
                st.goPasses = ii("goPasses"); st.biggestBalance = li("biggestBalance")
                st.totalEarned = li("totalEarned"); st.totalSpent = li("totalSpent")
                st.doublesRolled = ii("doublesRolled")
            }
        }

        (root["log"] as? List<*>)?.forEach { lm ->
            val m = lm as? Map<*, *> ?: return@forEach
            state.log.add(
                LogEntry(
                    (m["turn"] as? Number)?.toInt() ?: 0,
                    (m["player"] as? Number)?.toInt() ?: -1,
                    m["text"] as? String ?: "",
                    (m["kind"] as? Number)?.toInt() ?: 0
                )
            )
        }

        // وضعیت انتظار — با بازسازی ایمن
        val awaitingName = str("awaiting", "NONE")
        state.awaiting = when {
            state.winnerId >= 0 -> DecisionKind.GAME_OVER
            awaitingName == "AUCTION" && root["auctionSpaceId"] != null -> DecisionKind.NONE // حراج با StartAuction مجدداً آغاز می‌شود
            awaitingName == "BUY_PROMPT" -> DecisionKind.BUY_PROMPT
            awaitingName == "JAIL_DECISION" -> DecisionKind.JAIL_DECISION
            awaitingName == "DEBT" -> DecisionKind.DEBT
            else -> DecisionKind.NONE
        }
        // اگر وسط حراج ذخیره شده، حراج از نو شروع می‌شود
        val auctionSpace = (root["auctionSpaceId"] as? Number)?.toInt()
        if (awaitingName == "AUCTION" && auctionSpace != null && auctionSpace in 0..39) {
            state.auction = null
            state.awaiting = DecisionKind.NONE
            state.pendingAuctionRestart = auctionSpace
        }
        return state
    }

    /** خلاصه‌ی یک فایل ذخیره برای نمایش در فهرست اسلات‌ها */
    fun summarize(json: String): SaveSummary? {
        return try {
            val root = MiniJson.parse(json) as? Map<*, *> ?: return null
            val players = root["players"] as? List<*> ?: return null
            val names = players.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
            val humans = players.mapNotNull { pm ->
                val m = pm as? Map<*, *> ?: return@mapNotNull null
                if (m["isHuman"] == true) m["name"] as? String else null
            }
            SaveSummary(
                round = ((root["roundCount"] as? Number)?.toInt() ?: 1),
                turn = ((root["turnCounter"] as? Number)?.toInt() ?: 0),
                savedAt = ((root["savedAt"] as? Number)?.toLong() ?: 0L),
                players = names,
                humanNames = humans,
                winnerId = ((root["winnerId"] as? Number)?.toInt() ?: -1)
            )
        } catch (e: Exception) {
            null
        }
    }

    class SaveSummary(
        val round: Int,
        val turn: Int,
        val savedAt: Long,
        val players: List<String>,
        val humanNames: List<String>,
        val winnerId: Int
    )
}
