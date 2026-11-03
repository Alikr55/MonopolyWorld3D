package com.monopoly.world3d.engine

/**
 * آمار ماندگار بازیکن و دستاوردها.
 * همه‌ی مقادیر با MiniJson ذخیره و بازیابی می‌شوند.
 */
class GlobalStats {
    var gamesPlayed: Int = 0
    var gamesWon: Int = 0
    var gamesLost: Int = 0
    var perfectWins: Int = 0          // برد با ورشکست‌کردن حداقل دو حریف
    var hardWins: Int = 0
    var wins6Players: Int = 0
    var propertiesBought: Int = 0
    var housesBuilt: Int = 0
    var hotelsBuilt: Int = 0
    var groupsCompleted: Int = 0
    var allGroupsCompleted: Boolean = false
    var rentCollected: Long = 0
    var rentPaid: Long = 0
    var taxesPaid: Long = 0
    var tradesDone: Int = 0
    var auctionsWon: Int = 0
    var cardsDrawn: Int = 0
    var timesInJail: Int = 0
    var jailEscapes: Int = 0
    var bankruptcies: Int = 0
    var doublesRolled: Int = 0
    var goPasses: Int = 0
    var potCollectedBest: Long = 0
    var maxNetWorth: Long = 0
    var fastestWinMillis: Long = 0
    var longestGameMillis: Long = 0
    var totalPlayMillis: Long = 0

    fun toMap(): Map<String, Any?> = linkedMapOf(
        "gamesPlayed" to gamesPlayed, "gamesWon" to gamesWon, "gamesLost" to gamesLost,
        "perfectWins" to perfectWins, "hardWins" to hardWins, "wins6Players" to wins6Players,
        "propertiesBought" to propertiesBought, "housesBuilt" to housesBuilt, "hotelsBuilt" to hotelsBuilt,
        "groupsCompleted" to groupsCompleted, "allGroupsCompleted" to allGroupsCompleted,
        "rentCollected" to rentCollected, "rentPaid" to rentPaid, "taxesPaid" to taxesPaid,
        "tradesDone" to tradesDone, "auctionsWon" to auctionsWon, "cardsDrawn" to cardsDrawn,
        "timesInJail" to timesInJail, "jailEscapes" to jailEscapes, "bankruptcies" to bankruptcies,
        "doublesRolled" to doublesRolled, "goPasses" to goPasses, "potCollectedBest" to potCollectedBest,
        "maxNetWorth" to maxNetWorth, "fastestWinMillis" to fastestWinMillis,
        "longestGameMillis" to longestGameMillis, "totalPlayMillis" to totalPlayMillis
    )

    fun fromMap(m: Map<String, Any?>) {
        fun i(k: String, d: Int = 0) = (m[k] as? Number)?.toInt() ?: d
        fun l(k: String, d: Long = 0) = (m[k] as? Number)?.toLong() ?: d
        fun b(k: String) = m[k] as? Boolean ?: false
        gamesPlayed = i("gamesPlayed"); gamesWon = i("gamesWon"); gamesLost = i("gamesLost")
        perfectWins = i("perfectWins"); hardWins = i("hardWins"); wins6Players = i("wins6Players")
        propertiesBought = i("propertiesBought"); housesBuilt = i("housesBuilt"); hotelsBuilt = i("hotelsBuilt")
        groupsCompleted = i("groupsCompleted"); allGroupsCompleted = b("allGroupsCompleted")
        rentCollected = l("rentCollected"); rentPaid = l("rentPaid"); taxesPaid = l("taxesPaid")
        tradesDone = i("tradesDone"); auctionsWon = i("auctionsWon"); cardsDrawn = i("cardsDrawn")
        timesInJail = i("timesInJail"); jailEscapes = i("jailEscapes"); bankruptcies = i("bankruptcies")
        doublesRolled = i("doublesRolled"); goPasses = i("goPasses"); potCollectedBest = l("potCollectedBest")
        maxNetWorth = l("maxNetWorth"); fastestWinMillis = l("fastestWinMillis")
        longestGameMillis = l("longestGameMillis"); totalPlayMillis = l("totalPlayMillis")
    }
}

/** نتیجه‌ی یک بازی تمام‌شده برای به‌روزرسانی آمار و دستاوردها */
class GameResultContext(
    val winnerId: Int,
    val humanWon: Boolean,
    val playerCount: Int,
    val hardestAiLevel: AiLevel,
    val durationMillis: Long,
    val bankruptCount: Int,
    val potCollected: Long,
    val humanNetWorth: Long,
    val humanStats: PlayerGameStats,
    val groupsCompletedByHuman: Int
)

/** تعریف یک دستاورد */
class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val check: (GlobalStats, GameResultContext) -> Boolean
)

object Achievements {

    val ALL: List<AchievementDef> = listOf(
        AchievementDef("first_buy", "اولین خرید", "اولین ملک خود را بخرید",
            { s, _ -> s.propertiesBought >= 1 }),
        AchievementDef("trader10", "تاجر واقعی", "در مجموع ۱۰ ملک بخرید",
            { s, _ -> s.propertiesBought >= 10 }),
        AchievementDef("empire50", "امپراتور املاک", "در مجموع ۵۰ ملک بخرید",
            { s, _ -> s.propertiesBought >= 50 }),
        AchievementDef("first_house", "اولین خانه", "اولین خانه‌ی خود را بسازید",
            { s, _ -> s.housesBuilt >= 1 }),
        AchievementDef("architect25", "معمار", "در مجموع ۲۵ خانه بسازید",
            { s, _ -> s.housesBuilt >= 25 }),
        AchievementDef("builder100", "برج‌ساز", "در مجموع ۱۰۰ خانه بسازید",
            { s, _ -> s.housesBuilt >= 100 }),
        AchievementDef("first_hotel", "اولین هتل", "اولین هتل خود را بسازید",
            { s, _ -> s.hotelsBuilt >= 1 }),
        AchievementDef("hotelier10", "هتل‌دار", "در مجموع ۱۰ هتل بسازید",
            { s, _ -> s.hotelsBuilt >= 10 }),
        AchievementDef("monopoly1", "مونوپولی کامل", "یک گروه رنگی را کامل کنید",
            { s, _ -> s.groupsCompleted >= 1 }),
        AchievementDef("rainbow", "رنگین‌کمان", "هر ۸ گروه رنگی خیابانی را در طول بازی‌ها کامل کنید",
            { s, _ -> s.allGroupsCompleted }),
        AchievementDef("rich5m", "ثروتمند", "به ارزش خالص ۵٬۰۰۰٬۰۰۰ تومان برسید",
            { s, _ -> s.maxNetWorth >= 5_000_000 }),
        AchievementDef("rich20m", "میلیاردر", "به ارزش خالص ۲۰٬۰۰۰٬۰۰۰ تومان برسید",
            { s, _ -> s.maxNetWorth >= 20_000_000 }),
        AchievementDef("beat5", "شکست دادن پنج حریف", "یک بازی ۶ نفره را ببرید",
            { _, ctx -> ctx.humanWon && ctx.playerCount == 6 }),
        AchievementDef("decisive", "برد قاطع", "بازی را ببرید در حالی که دست‌کم دو حریف ورشکست شده‌اند",
            { _, ctx -> ctx.humanWon && ctx.bankruptCount >= 2 }),
        AchievementDef("lucky50", "خوش‌شانس", "در مجموع ۵۰ کارت بکشید",
            { s, _ -> s.cardsDrawn >= 50 }),
        AchievementDef("cardmaster200", "استاد کارت‌ها", "در مجموع ۲۰۰ کارت بکشید",
            { s, _ -> s.cardsDrawn >= 200 }),
        AchievementDef("trade10", "معامله‌گر حرفه‌ای", "۱۰ معامله انجام دهید",
            { s, _ -> s.tradesDone >= 10 }),
        AchievementDef("auction10", "پادشاه حراج", "۱۰ حراج را ببرید",
            { s, _ -> s.auctionsWon >= 10 }),
        AchievementDef("jailbreak10", "فرار از زندان", "۱۰ بار از زندان آزاد شوید",
            { s, _ -> s.jailEscapes >= 10 }),
        AchievementDef("visitor25", "مهمان مکرر زندان", "۲۵ بار به زندان بروید",
            { s, _ -> s.timesInJail >= 25 }),
        AchievementDef("taxpayer", "مالیات‌ساز", "در مجموع ۱٬۰۰۰٬۰۰۰ تومان مالیات بپردازید",
            { s, _ -> s.taxesPaid >= 1_000_000 }),
        AchievementDef("goldenpot", "صندوق طلایی", "صندوق پارکینگ آزاد به ارزش دست‌کم ۵۰۰٬۰۰۰ تومان را بردارید",
            { s, _ -> s.potCollectedBest >= 500_000 }),
        AchievementDef("speedrun", "سرعت نور", "یک بازی را در کمتر از ۱۵ دقیقه ببرید",
            { _, ctx -> ctx.humanWon && ctx.durationMillis in 1..(15 * 60_000L) }),
        AchievementDef("marathon", "ماراتن", "در بازی‌ای دست‌کم ۴۵ دقیقه‌ای شرکت کنید",
            { _, ctx -> ctx.durationMillis >= 45 * 60_000L }),
        AchievementDef("hard_win", "قهرمان سخت", "بازی را در سطح دشواری «سخت» ببرید",
            { _, ctx -> ctx.humanWon && ctx.hardestAiLevel == AiLevel.HARD }),
        AchievementDef("salary50", "حقوق‌بگیر ممتاز", "۵۰ بار از خانه‌ی شروع بگذرید",
            { s, _ -> s.goPasses >= 50 }),
        AchievementDef("champion25", "قهرمان جهان", "۲۵ بازی را ببرید",
            { s, _ -> s.gamesWon >= 25 }),
        AchievementDef("first_win", "آغاز درخشان", "اولین بازی خود را ببرید",
            { s, _ -> s.gamesWon >= 1 })
    )

    fun byId(id: String): AchievementDef? = ALL.firstOrNull { it.id == id }
}
