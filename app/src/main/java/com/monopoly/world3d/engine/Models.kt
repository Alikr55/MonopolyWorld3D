package com.monopoly.world3d.engine

/**
 * مدل‌های هسته‌ای بازی «دنیای روپولی».
 * این فایل هیچ وابستگی به اندروید ندارد تا روی JVM قابل تست باشد.
 */

enum class SpaceType(val faName: String) {
    GO("شروع"),
    STREET("ملک"),
    RAILROAD("حمل‌ونقل"),
    UTILITY("خدمات"),
    CHANCE("شانس"),
    CHEST("اتفاقات عمومی"),
    TAX("مالیات"),
    JAIL("زندان"),
    FREE_PARKING("پارکینگ آزاد"),
    GO_TO_JAIL("رفتن به زندان")
}

enum class Group(val faName: String, val colorHex: Long) {
    BROWN("قهوه‌ای", 0x8D5524),
    LIGHT_BLUE("آبی روشن", 0x7EC8E3),
    PINK("صورتی", 0xE86A9A),
    ORANGE("نارنجی", 0xF39C12),
    RED("قرمز", 0xE74C3C),
    YELLOW("زرد", 0xF7DC6F),
    GREEN("سبز", 0x27AE60),
    DARK_BLUE("آبی تیره", 0x2C3E80),
    RAILROAD("حمل‌ونقل", 0x4A4A4A),
    UTILITY("خدمات", 0x8E9AAF)
}

/** ده مهره‌ی اصلی بازی */
enum class TokenKind(val faName: String, val colorHint: Long) {
    SPORT_CAR("ماشین اسپرت", 0xE74C3C),
    RACE_CAR("ماشین مسابقه", 0x2E86DE),
    SHIP("کشتی", 0x16A085),
    PLANE("هواپیما", 0xF39C12),
    CROWN("تاج", 0xF1C40F),
    ROCKET("موشک", 0x8E44AD),
    ROBOT("ربات", 0x5D6D7E),
    DIAMOND("الماس", 0x48DBFB),
    CAT("گربه", 0xE67E22),
    TROPHY("جام طلایی", 0xFFD700)
}

enum class AiLevel(val faName: String) {
    EASY("آسان"),
    NORMAL("متوسط"),
    HARD("سخت")
}

/** شخصیت‌های متفاوت هوش مصنوعی */
enum class AiPersonality(
    val faName: String,
    val aggression: Double,      // سرسختی در حراج و مسدودسازی حریفان
    val builderBias: Double,     // علاقه به ساخت خانه و هتل
    val tradeWillingness: Double,// تمایل به معامله
    val cashReserve: Double      // ضریب نگه‌داشت پول نقد
) {
    AGGRESSIVE("جاه‌طلب", 0.95, 0.75, 0.35, 0.45),
    CONSERVATIVE("محتاط", 0.35, 0.55, 0.30, 0.90),
    TRADER("معامله‌گر", 0.55, 0.40, 0.95, 0.55),
    DEVELOPER("سازنده", 0.60, 0.95, 0.30, 0.50),
    BALANCED("متعادل", 0.60, 0.60, 0.55, 0.60),
    LANDLORD("زمین‌دار", 0.80, 0.65, 0.45, 0.40);

    companion object {
        fun byName(n: String): AiPersonality =
            entries.firstOrNull { it.name == n } ?: BALANCED
    }
}

/** یک خانه‌ی صفحه‌ی بازی */
class Space(
    val id: Int,
    val type: SpaceType,
    val name: String,
    val group: Group? = null,
    val price: Long = 0,
    val houseCost: Long = 0,
    /** اجاره با ۰ خانه، ۱ تا ۴ خانه و هتل (اجاره‌ی ۰ خانه در صورت داشتن کل گروه دو برابر می‌شود) */
    val rents: List<Long> = emptyList(),
    val taxAmount: Long = 0
) {
    val mortgageValue: Long = price / 2
}

/** وضعیت مالکیت یک ملک */
class PropertyState(
    var owner: Int = -1,        // -1 = بانک
    var houses: Int = 0,        // 0..4 خانه، 5 = هتل
    var mortgaged: Boolean = false
)

/** بازیکن */
class Player(
    val id: Int,
    var name: String,
    val token: TokenKind,
    val colorHex: Long,
    val isHuman: Boolean,
    var aiLevel: AiLevel = AiLevel.NORMAL,
    var personality: AiPersonality = AiPersonality.BALANCED
) {
    var balance: Long = 0
    var position: Int = 0
    var inJail: Boolean = false
    var jailTurns: Int = 0
    var jailCards: Int = 0
    var bankrupt: Boolean = false

    val netWorth: Long
        get() = balance // محاسبه‌ی کامل در engine (با احتساب املاک) انجام می‌شود
}

/** قوانین قابل تنظیم بازی */
class GameRules(
    var startMoney: Long = 1_500_000,
    var goSalary: Long = 200_000,
    var doubleSalaryOnExactGo: Boolean = false,
    var freeParkingPot: Boolean = true,
    var auctionsEnabled: Boolean = true,
    var jailFine: Long = 50_000,
    var maxJailTurns: Int = 3,
    var incomeTax: Long = 200_000,
    var luxuryTax: Long = 100_000,
    var doubleRentOnMonopoly: Boolean = true,
    var houseBankLimit: Int = 32,
    var hotelBankLimit: Int = 12
)

/** انواع تصمیمی که موتور از کنترل‌کننده (انسان یا AI) انتظار دارد */
enum class DecisionKind {
    NONE,           // ادامه‌ی عادی؛ دکمه‌ی تاس فعال است
    BUY_PROMPT,     // بازیکن روی ملک آزاد است؛ خرید یا رد
    JAIL_DECISION,  // تصمیم برای خروج از زندان
    AUCTION,        // حراج در جریان است؛ پیشنهاد یا انصراف
    DEBT,           // بازیکن بدهکار است؛ باید دارایی بفروشد
    TRADE_RESPONSE, // پیشنهاد معامله در انتظار پاسخ انسان
    GAME_OVER       // بازی تمام شده
}

/** یک رخداد برای نمایش در گزارش و رابط کاربری */
class LogEntry(
    val turn: Int,
    val player: Int,       // شناسه‌ی بازیکن مرتبط یا -1
    val text: String,
    val kind: Int = KIND_INFO
) {
    companion object {
        const val KIND_INFO = 0
        const val KIND_MONEY_IN = 1
        const val KIND_MONEY_OUT = 2
        const val KIND_CARD = 3
        const val KIND_JAIL = 4
        const val KIND_BUILD = 5
        const val KIND_TRADE = 6
        const val KIND_AUCTION = 7
        const val KIND_BANKRUPT = 8
        const val KIND_WIN = 9
    }
}

/** آمار لحظه‌ای یک بازی برای صفحه‌ی نتیجه */
class PlayerGameStats {
    var rentCollected: Long = 0
    var rentPaid: Long = 0
    var taxesPaid: Long = 0
    var propertiesBought: Int = 0
    var housesBuilt: Int = 0
    var hotelsBuilt: Int = 0
    var tradesDone: Int = 0
    var auctionsWon: Int = 0
    var cardsDrawn: Int = 0
    var timesInJail: Int = 0
    var goPasses: Int = 0
    var biggestBalance: Long = 0
    var totalEarned: Long = 0
    var totalSpent: Long = 0
    var doublesRolled: Int = 0
}
