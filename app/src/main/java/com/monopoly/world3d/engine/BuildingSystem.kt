package com.monopoly.world3d.engine

/**
 * سیستم ساخت‌وساز — خانه و هتل با قواعد یکنواختی و ذخیره‌ی بانک.
 * سطح ۰..۴ = خانه، سطح ۵ = هتل.
 */
object BuildingSystem {

    /** املاک گروه که بازیکن مالک همه‌ی آن‌هاست و هیچ‌کدام رهن نیستند */
    fun ownsFullGroup(state: GameState, playerId: Int, group: Group): Boolean {
        val ids = state.properties.indices.filter { state.space(it).group == group }
        if (ids.isEmpty()) return false
        return ids.all { state.prop(it).owner == playerId && !state.prop(it).mortgaged }
    }

    /** آیا بازیکن می‌تواند در این ملک بسازد؟ دلیل خطا را برمی‌گرداند */
    fun canBuild(state: GameState, playerId: Int, spaceId: Int): String? {
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        val p = state.players[playerId]
        if (sp.type != SpaceType.STREET) return "فقط در خیابان‌ها می‌توان ساخت"
        if (ps.owner != playerId) return "این ملک متعلق به شما نیست"
        if (ps.mortgaged) return "ملک رهنی قابل ساخت نیست"
        val group = sp.group!!
        if (!ownsFullGroup(state, playerId, group))
            return "برای ساخت باید همه‌ی املاک «${group.faName}» را مالک باشید"
        // قاعده‌ی یکنواختی: فقط روی ملکی می‌توان ساخت که کمترین خانه‌ی گروه را دارد
        val groupIds = state.properties.indices.filter { state.space(it).group == group }
        val minHouses = groupIds.minOf { state.prop(it).houses }
        if (ps.houses >= 5) return "در این ملک هتل وجود دارد"
        if (ps.houses > minHouses) return "ساخت باید یکنواخت باشد؛ ابتدا در خیابان‌های کم‌ساخت‌تر بسازید"
        // ذخیره‌ی بانک
        if (ps.houses < 4) {
            if (state.bankHouses <= 0) return "خانه‌ی بانک تمام شده است"
        } else {
            if (state.bankHotels <= 0) return "هتل بانک تمام شده است"
        }
        if (p.balance < sp.houseCost) return "پول کافی برای ساخت ندارید (${PersianNumbers.money(sp.houseCost)})"
        return null
    }

    /** ساخت یک خانه (یا ارتقا به هتل در سطح ۴). مقدار برگشتی: پیام خطا یا null */
    fun build(state: GameState, playerId: Int, spaceId: Int): String? {
        val err = canBuild(state, playerId, spaceId)
        if (err != null) return err
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        if (!Bank.withdraw(state, playerId, sp.houseCost, "هزینه‌ی ساخت در ${sp.name}")) return "پول کافی نیست"
        if (ps.houses < 4) {
            ps.houses++
            state.bankHouses--
            state.statsFor(playerId).housesBuilt++
            state.addLog("${state.players[playerId].name} در «${sp.name}» خانه ساخت (سطح ${PersianNumbers.number(ps.houses)})", playerId, LogEntry.KIND_BUILD)
        } else {
            ps.houses = 5
            state.bankHotels--
            state.bankHouses += 4   // ۴ خانه به بانک بازمی‌گردد
            state.statsFor(playerId).hotelsBuilt++
            state.addLog("${state.players[playerId].name} در «${sp.name}» هتل ساخت!", playerId, LogEntry.KIND_BUILD)
        }
        return null
    }

    /** آیا می‌توان این ساختمان را فروخت؟ */
    fun canSell(state: GameState, playerId: Int, spaceId: Int): String? {
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        if (sp.type != SpaceType.STREET) return "این ملک ساختمان ندارد"
        if (ps.owner != playerId) return "این ملک متعلق به شما نیست"
        if (ps.houses == 0) return "در این ملک ساختمانی نیست"
        // فروش یکنواخت: فقط ملکی که بیشترین خانه را دارد
        val groupIds = state.properties.indices.filter { state.space(it).group == sp.group }
        val maxHouses = groupIds.maxOf { state.prop(it).houses }
        if (ps.houses < maxHouses) return "فروش باید یکنواخت باشد؛ ابتدا از پرساخت‌ترین ملک بفروشید"
        return null
    }

    /** فروش یک ساختمان به بانک با نصف قیمت */
    fun sell(state: GameState, playerId: Int, spaceId: Int): String? {
        val err = canSell(state, playerId, spaceId)
        if (err != null) return err
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        val refund = if (ps.houses == 5) sp.houseCost * 5 / 2 else sp.houseCost / 2
        if (ps.houses == 5) {
            ps.houses = 0
            state.bankHotels++
            state.bankHouses += 4
            state.statsFor(playerId).hotelsBuilt = (state.statsFor(playerId).hotelsBuilt - 1).coerceAtLeast(0)
        } else {
            ps.houses--
            state.bankHouses++
        }
        Bank.deposit(state, playerId, refund, "فروش ساختمان در ${sp.name}")
        state.addLog("${state.players[playerId].name} ساختمانی در «${sp.name}» فروخت", playerId, LogEntry.KIND_BUILD)
        return null
    }

    /** آیا رهن کردن ممکن است؟ */
    fun canMortgage(state: GameState, playerId: Int, spaceId: Int): String? {
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        if (ps.owner != playerId) return "این ملک متعلق به شما نیست"
        if (ps.mortgaged) return "این ملک قبلاً رهن شده است"
        if (Board.isStreetGroup(sp.group)) {
            val groupIds = state.properties.indices.filter { state.space(it).group == sp.group }
            if (groupIds.any { state.prop(it).houses > 0 })
                return "ابتدا ساختمان‌های گروه «${sp.group?.faName}» را بفروشید"
        }
        return null
    }

    /** رهن کردن ملک: دریافت ارزش رهن؛ تا آزادسازی اجاره تعلق نمی‌گیرد */
    fun mortgage(state: GameState, playerId: Int, spaceId: Int): String? {
        val err = canMortgage(state, playerId, spaceId)
        if (err != null) return err
        val sp = state.space(spaceId)
        state.prop(spaceId).mortgaged = true
        Bank.deposit(state, playerId, sp.mortgageValue, "رهن گذاشتن ${sp.name}")
        state.addLog("${state.players[playerId].name} «${sp.name}» را رهن گذاشت", playerId, LogEntry.KIND_MONEY_IN)
        return null
    }

    /** هزینه‌ی آزادسازی: ارزش رهن + ۱۰٪ بهره */
    fun redeemCost(sp: Space): Long = sp.mortgageValue + sp.mortgageValue / 10

    fun canRedeem(state: GameState, playerId: Int, spaceId: Int): String? {
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        if (ps.owner != playerId) return "این ملک متعلق به شما نیست"
        if (!ps.mortgaged) return "این ملک رهن نیست"
        if (state.players[playerId].balance < redeemCost(sp))
            return "پول کافی ندارید؛ آزادسازی ${PersianNumbers.money(redeemCost(sp))} است"
        return null
    }

    fun redeem(state: GameState, playerId: Int, spaceId: Int): String? {
        val err = canRedeem(state, playerId, spaceId)
        if (err != null) return err
        val sp = state.space(spaceId)
        if (!Bank.withdraw(state, playerId, redeemCost(sp), "آزادسازی رهن ${sp.name}")) return "پول کافی نیست"
        state.prop(spaceId).mortgaged = false
        state.addLog("${state.players[playerId].name} «${sp.name}» را از رهن آزاد کرد", playerId, LogEntry.KIND_MONEY_OUT)
        return null
    }

    /** محاسبه‌ی اجاره‌ی یک ملک برای مالک فعلی (بر اساس تاس) */
    fun computeRent(state: GameState, spaceId: Int, diceTotal: Int): Long {
        val sp = state.space(spaceId)
        val ps = state.prop(spaceId)
        if (ps.owner < 0 || ps.mortgaged) return 0
        return when (sp.type) {
            SpaceType.STREET -> {
                if (ps.houses > 0) sp.rents[ps.houses]  // rents[0] = بدون خانه، پس سطح n => rents[n]
                else {
                    val monopoly = ownsFullGroup(state, ps.owner, sp.group!!)
                    if (monopoly && state.rules.doubleRentOnMonopoly) sp.rents[0] * 2 else sp.rents[0]
                }
            }
            SpaceType.RAILROAD -> {
                val count = state.ownedBy(ps.owner).count { state.space(it).type == SpaceType.RAILROAD }
                RAIL_RENTS[count.coerceIn(1, 4) - 1]
            }
            SpaceType.UTILITY -> {
                val count = state.ownedBy(ps.owner).count { state.space(it).type == SpaceType.UTILITY }
                diceTotal * (if (count >= 2) 10L else 4L) * 1000
            }
            else -> 0
        }
    }

    private val RAIL_RENTS = Board.RAILROAD_RENTS
}
