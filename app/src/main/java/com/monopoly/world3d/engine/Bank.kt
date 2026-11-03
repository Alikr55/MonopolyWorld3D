package com.monopoly.world3d.engine

/**
 * سیستم بانکی — همه‌ی انتقال پول از اینجا عبور می‌کند تا از کسری/تکرار جلوگیری شود.
 * همه‌ی مبالغ صحیح و بر حسب تومان هستند.
 */
object Bank {

    /** واریز پول به بازیکن (درآمد) */
    fun deposit(state: GameState, playerId: Int, amount: Long, reason: String): LogEntry {
        val p = state.players[playerId]
        p.balance += amount
        val st = state.statsFor(playerId)
        st.totalEarned += amount
        if (p.balance > st.biggestBalance) st.biggestBalance = p.balance
        val entry = LogEntry(state.turnCounter, playerId, "${p.name} $reason: +${PersianNumbers.money(amount)}", LogEntry.KIND_MONEY_IN)
        state.log.add(entry)
        return entry
    }

    /** برداشت پول از بازیکن — فقط در صورت موجودی کافی */
    fun withdraw(state: GameState, playerId: Int, amount: Long, reason: String): Boolean {
        val p = state.players[playerId]
        if (p.balance < amount) return false
        p.balance -= amount
        val st = state.statsFor(playerId)
        st.totalSpent += amount
        state.log.add(
            LogEntry(state.turnCounter, playerId, "${p.name} $reason: -${PersianNumbers.money(amount)}", LogEntry.KIND_MONEY_OUT)
        )
        return true
    }

    /** پرداخت به بانک (خرید، جریمه، مالیات). با قانون صندوق، جریمه‌ها به پارکینگ آزاد می‌روند. */
    fun payToBank(state: GameState, playerId: Int, amount: Long, reason: String, toPot: Boolean = false): Boolean {
        val p = state.players[playerId]
        if (p.balance < amount) return false
        withdraw(state, playerId, amount, reason)
        if (toPot && state.rules.freeParkingPot) state.parkingPot += amount
        return true
    }

    /** پرداخت از بازیکن به بازیکن دیگر — در صورت شکست، موتور بدهی ثبت می‌کند */
    fun payToPlayer(state: GameState, payerId: Int, payeeId: Int, amount: Long, reason: String): Boolean {
        val payer = state.players[payerId]
        if (payer.balance < amount) return false
        withdraw(state, payerId, amount, reason)
        deposit(state, payeeId, amount, "دریافت بابت $reason")
        return true
    }

    /** حداکثر وجهی که بازیکن می‌تواند با فروش خانه‌ها و رهن کردن املاک تهیه کند */
    fun maxRaisableFunds(state: GameState, playerId: Int): Long {
        val p = state.players[playerId]
        if (p.bankrupt) return 0
        var total = p.balance
        for (id in state.ownedBy(playerId)) {
            val ps = state.prop(id)
            val sp = state.space(id)
            if (Board.isStreetGroup(sp.group)) total += ps.houses * sp.houseCost / 2
            if (!ps.mortgaged) total += sp.mortgageValue
        }
        return total
    }
}
