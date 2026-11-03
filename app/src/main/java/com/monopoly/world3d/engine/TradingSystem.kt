package com.monopoly.world3d.engine

/**
 * پیشنهاد معامله‌ی دوطرفه بین دو بازیکن.
 * طرف اول این‌ها را می‌دهد: give* و این‌ها را می‌گیرد: get*
 */
class TradeOffer(
    val fromId: Int,
    val toId: Int,
    val giveProperties: List<Int> = emptyList(),
    val getProperties: List<Int> = emptyList(),
    val giveMoney: Long = 0,
    val getMoney: Long = 0,
    val giveJailCards: Int = 0,
    val getJailCards: Int = 0,
    var isCounter: Boolean = false
)

/**
 * سیستم معامله — اعتبارسنجی و اجرای امن معاملات.
 */
object TradingSystem {

    /** آیا این ملک قابل معامله است؟ (بدون ساختمان و بدون ساختمان در کل گروه) */
    fun isTradable(state: GameState, spaceId: Int): Boolean {
        val sp = state.space(spaceId)
        if (sp.price <= 0) return false
        if (sp.group == null) return false
        if (state.prop(spaceId).houses > 0) return false
        // کل گروه نباید ساختمان داشته باشد (قاعده‌ی فروش یکنواخت)
        for (id in state.properties.indices) {
            val other = state.space(id)
            if (other.group == sp.group && state.prop(id).houses > 0) return false
        }
        return true
    }

    /** اعتبارسنجی کامل پیشنهاد؛ در صورت نامعتبر بودن دلیل برمی‌گرداند */
    fun validate(state: GameState, offer: TradeOffer): String? {
        val from = state.players[offer.fromId]
        val to = state.players[offer.toId]
        if (from.bankrupt || to.bankrupt) return "بازیکن ورشکسته نمی‌تواند معامله کند"
        if (offer.fromId == offer.toId) return "معامله با خود ممکن نیست"
        if (offer.giveMoney < 0 || offer.getMoney < 0) return "مبلغ منفی مجاز نیست"
        if (offer.giveJailCards < 0 || offer.getJailCards < 0) return "تعداد کارت نامعتبر"
        if (offer.giveMoney > from.balance) return "موجودی ${from.name} کافی نیست"
        if (offer.getMoney > to.balance) return "موجودی ${to.name} کافی نیست"
        if (offer.giveJailCards > from.jailCards) return "${from.name} این تعداد کارت آزادی ندارد"
        if (offer.getJailCards > to.jailCards) return "${to.name} این تعداد کارت آزادی ندارد"
        for (id in offer.giveProperties) {
            if (state.prop(id).owner != offer.fromId) return "${state.space(id).name} متعلق به ${from.name} نیست"
            if (!isTradable(state, id)) return "${state.space(id).name} اکنون قابل معامله نیست (ساختمان دارد)"
        }
        for (id in offer.getProperties) {
            if (state.prop(id).owner != offer.toId) return "${state.space(id).name} متعلق به ${to.name} نیست"
            if (!isTradable(state, id)) return "${state.space(id).name} اکنون قابل معامله نیست (ساختمان دارد)"
        }
        if (offer.giveProperties.isEmpty() && offer.getProperties.isEmpty() &&
            offer.giveMoney == 0L && offer.getMoney == 0L &&
            offer.giveJailCards == 0 && offer.getJailCards == 0) return "معامله خالی است"
        return null
    }

    /** اجرای معامله؛ قبلاً باید validate شده باشد */
    fun execute(state: GameState, offer: TradeOffer): Boolean {
        val err = validate(state, offer)
        if (err != null) return false
        val from = state.players[offer.fromId]
        val to = state.players[offer.toId]

        if (offer.giveMoney > 0) {
            Bank.withdraw(state, offer.fromId, offer.giveMoney, "پرداخت در معامله با ${to.name}")
            Bank.deposit(state, offer.toId, offer.giveMoney, "دریافت از ${from.name}")
        }
        if (offer.getMoney > 0) {
            Bank.withdraw(state, offer.toId, offer.getMoney, "پرداخت در معامله با ${from.name}")
            Bank.deposit(state, offer.fromId, offer.getMoney, "دریافت از ${to.name}")
        }
        for (id in offer.giveProperties) state.prop(id).owner = offer.toId
        for (id in offer.getProperties) state.prop(id).owner = offer.fromId
        from.jailCards -= offer.giveJailCards
        to.jailCards += offer.giveJailCards
        to.jailCards -= offer.getJailCards
        from.jailCards += offer.getJailCards

        state.statsFor(offer.fromId).tradesDone++
        state.statsFor(offer.toId).tradesDone++
        state.addLog(
            "معامله انجام شد: ${from.name} و ${to.name} — ${describe(state, offer)}",
            offer.fromId, LogEntry.KIND_TRADE
        )
        return true
    }

    /** توضیح کوتاه معامله برای گزارش */
    fun describe(state: GameState, offer: TradeOffer): String {
        val parts = mutableListOf<String>()
        val from = state.players[offer.fromId]
        val to = state.players[offer.toId]
        if (offer.giveProperties.isNotEmpty())
            parts.add("${from.name} املاک «" + offer.giveProperties.joinToString("، ") { state.space(it).name } + "» را می‌دهد")
        if (offer.giveMoney > 0) parts.add("${from.name} ${PersianNumbers.money(offer.giveMoney)} می‌دهد")
        if (offer.giveJailCards > 0) parts.add("${from.name} ${PersianNumbers.number(offer.giveJailCards)} کارت آزادی می‌دهد")
        if (offer.getProperties.isNotEmpty())
            parts.add("${to.name} املاک «" + offer.getProperties.joinToString("، ") { state.space(it).name } + "» را می‌دهد")
        if (offer.getMoney > 0) parts.add("${to.name} ${PersianNumbers.money(offer.getMoney)} می‌دهد")
        if (offer.getJailCards > 0) parts.add("${to.name} ${PersianNumbers.number(offer.getJailCards)} کارت آزادی می‌دهد")
        return parts.joinToString(" | ")
    }
}
