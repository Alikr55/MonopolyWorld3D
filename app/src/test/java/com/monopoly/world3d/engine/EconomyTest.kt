package com.monopoly.world3d.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyTest {

    private fun dice(a: Int, b: Int) = TestHelpers.ScriptedRandom(listOf(a, b))

    @Test
    fun `street rent without houses`() {
        val engine = TestHelpers.makeGame(seed = 20)
        val state = engine.state
        // خانه‌ی ۳ به بازیکن ۱
        TestHelpers.giveProperty(engine, 1, 3)
        val rent = BuildingSystem.computeRent(state, 3, 7)
        assertEquals(4_000, rent) // اجاره پایه خیابان طالقانی = ۴ هزار
    }

    @Test
    fun `monopoly doubles base rent`() {
        val engine = TestHelpers.makeGame(seed = 21)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 1)
        TestHelpers.giveProperty(engine, 1, 3)
        val rent = BuildingSystem.computeRent(state, 3, 7)
        assertEquals(8_000, rent)
    }

    @Test
    fun `house rent uses rent table`() {
        val engine = TestHelpers.makeGame(seed = 22)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 3, houses = 2)
        val rent = BuildingSystem.computeRent(state, 3, 7)
        assertEquals(60_000, rent)
        // هتل
        engine.state.prop(3).houses = 5
        assertEquals(450_000, BuildingSystem.computeRent(state, 3, 7))
    }

    @Test
    fun `mortgaged property collects no rent`() {
        val engine = TestHelpers.makeGame(seed = 23)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 3)
        state.prop(3).mortgaged = true
        assertEquals(0, BuildingSystem.computeRent(state, 3, 7))
    }

    @Test
    fun `railroad rent scales with count`() {
        val engine = TestHelpers.makeGame(seed = 24)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 5)
        assertEquals(25_000, BuildingSystem.computeRent(state, 5, 7))
        TestHelpers.giveProperty(engine, 1, 15)
        assertEquals(50_000, BuildingSystem.computeRent(state, 5, 7))
        TestHelpers.giveProperty(engine, 1, 25)
        TestHelpers.giveProperty(engine, 1, 35)
        assertEquals(200_000, BuildingSystem.computeRent(state, 5, 7))
    }

    @Test
    fun `utility rent multiplies dice`() {
        val engine = TestHelpers.makeGame(seed = 25)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 12)
        assertEquals(4 * 7 * 1000, BuildingSystem.computeRent(state, 12, 7))
        TestHelpers.giveProperty(engine, 1, 28)
        assertEquals(10 * 7 * 1000, BuildingSystem.computeRent(state, 12, 7))
    }

    @Test
    fun `rent payment on landing`() {
        val engine = TestHelpers.makeGame(seed = 26)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 3) // اجاره ۴ هزار
        val p = state.players[0]
        val owner = state.players[1]
        p.position = 0
        state.rng = dice(0, 1) // مجموع ۳
        val bP = p.balance; val bO = owner.balance
        engine.rollDice()
        assertEquals(bP - 4_000, p.balance)
        assertEquals(bO + 4_000, owner.balance)
    }

    @Test
    fun `insufficient funds creates debt and settle works`() {
        val engine = TestHelpers.makeGame(seed = 27)
        val state = engine.state
        TestHelpers.giveProperty(engine, 1, 3, houses = 5) // اجاره ۴۵۰ هزار
        val p = state.players[0]
        p.balance = 100_000
        TestHelpers.giveProperty(engine, 0, 5) // ایستگاه برای رهن
        p.position = 0
        state.rng = dice(0, 1)
        engine.rollDice()
        assertEquals(DecisionKind.DEBT, state.awaiting)
        assertEquals(450_000, state.debtAmount)
        // رهن ایستگاه: ۱۰۰ هزار می‌گیرد → ۲۰۰ هزار؛ هنوز کم است → ورشکستگی اجتناب‌ناپذیر
        engine.playerMortgage(0, 5)
        // ورشکستگی خودکار چون حداکثر نقدسازی کمتر از بدهی است
        assertTrue(p.bankrupt)
    }

    @Test
    fun `debt settled by selling buildings`() {
        val engine = TestHelpers.makeGame(seed = 28)
        val state = engine.state
        // حریف هتلی دارد که اجاره‌ی ۴۵۰ هزار می‌سازد
        TestHelpers.giveProperty(engine, 1, 3, houses = 5)
        val p = state.players[0]
        p.balance = 0
        // بازیکن دارایی برای نقدسازی دارد: خانه‌های خیابان ری + ایستگاه
        TestHelpers.giveProperty(engine, 0, 1, houses = 4)
        TestHelpers.giveProperty(engine, 0, 5)
        p.position = 0
        state.rng = dice(0, 1) // مجموع ۳ → خانه ۳
        engine.rollDice()
        assertEquals(DecisionKind.DEBT, state.awaiting)
        // فروش خانه‌ها: ۴ × ۲۵ هزار = ۱۰۰ هزار + رهن ۱ (۳۰ هزار) + رهن ۵ (۱۰۰ هزار) = ۲۳۰ هزار
        engine.playerSellBuilding(0, 1)
        engine.playerSellBuilding(0, 1)
        engine.playerSellBuilding(0, 1)
        engine.playerSellBuilding(0, 1)
        if (!p.bankrupt) engine.playerMortgage(0, 1)
        if (!p.bankrupt) engine.playerMortgage(0, 5)
        // مجموع نقدسازی از بدهی کمتر است → ورشکستگی
        assertTrue(p.bankrupt || p.balance >= 450_000)
    }

    @Test
    fun `tax landing on income tax pays to pot`() {
        val rules = GameRules(freeParkingPot = true)
        val engine = TestHelpers.makeGame(seed = 29, rules = rules)
        val state = engine.state
        val p = state.players[0]
        p.position = 1
        state.rng = dice(0, 1) // مجموع ۳ → خانه ۴ مالیات درآمد
        val before = p.balance
        val potBefore = state.parkingPot
        engine.rollDice()
        assertEquals(4, p.position)
        assertEquals(before - 200_000, p.balance)
        assertEquals(potBefore + 200_000, state.parkingPot)
    }
}

class BuildingSystemTest {

    @Test
    fun `cannot build without full group`() {
        val engine = TestHelpers.makeGame(seed = 30)
        TestHelpers.giveProperty(engine, 0, 1)
        val err = BuildingSystem.canBuild(engine.state, 0, 1)
        assertTrue(err != null)
    }

    @Test
    fun `build respects even rule and hotel rule`() {
        val engine = TestHelpers.makeGame(seed = 31)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        TestHelpers.giveProperty(engine, 0, 3)
        val p = state.players[0]
        TestHelpers.giveMoney(engine, 0, 10_000_000)

        // ساخت در ۱ وقتی ۳ ساختمان ندارد؟ قاعده: فقط کمترین → هر دو صفرند، مجاز
        assertEquals(null, BuildingSystem.canBuild(state, 0, 1))
        assertEquals(null, BuildingSystem.build(state, 0, 1))
        assertEquals(1, state.prop(1).houses)
        // حالا ساخت در ۳ مجاز است (تفاوت ۱) اما دوباره در ۱ ممنوع
        assertTrue(BuildingSystem.canBuild(state, 0, 3) == null)
        assertTrue(BuildingSystem.canBuild(state, 0, 1) != null)
        BuildingSystem.build(state, 0, 3)
        // تا ۴ خانه در هر دو
        for (i in 0 until 3) {
            BuildingSystem.build(state, 0, 1)
            BuildingSystem.build(state, 0, 3)
        }
        assertEquals(4, state.prop(1).houses)
        assertEquals(4, state.prop(3).houses)
        // هتل
        assertEquals(null, BuildingSystem.build(state, 0, 1))
        assertEquals(5, state.prop(1).houses)
        // ذخیره‌ی بانک: ۸ خانه مصرف و ۴ خانه با هتل برگشته است
        assertEquals(state.rules.houseBankLimit - 8 + 4, state.bankHouses)
    }

    @Test
    fun `sell refunds half and must be even`() {
        val engine = TestHelpers.makeGame(seed = 32)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1, houses = 2)
        TestHelpers.giveProperty(engine, 0, 3, houses = 2)
        TestHelpers.giveMoney(engine, 0, 0)
        val b = state.players[0].balance
        // فروش از ۱ مجاز (بیشترین ۲)، از ۳ هم مجاز؛ فروش نایک‌نواخت ممنوع نیست چون هر دو ۲ هستند
        assertTrue(BuildingSystem.canSell(state, 0, 1) == null)
        BuildingSystem.sell(state, 0, 1)
        assertEquals(b + 25_000, state.players[0].balance)
        assertEquals(1, state.prop(1).houses)
        // حالا فروش از ۳ مجاز، از ۱ ممنوع
        assertTrue(BuildingSystem.canSell(state, 0, 1) != null)
        assertTrue(BuildingSystem.canSell(state, 0, 3) == null)
    }

    @Test
    fun `mortgage and redeem with interest`() {
        val engine = TestHelpers.makeGame(seed = 33)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 5) // ایستگاه ۲۰۰ هزار؛ رهن ۱۰۰ هزار
        val p = state.players[0]
        val b = p.balance
        BuildingSystem.mortgage(state, 0, 5)
        assertTrue(state.prop(5).mortgaged)
        assertEquals(b + 100_000, p.balance)
        // اجاره صفر
        assertEquals(0, BuildingSystem.computeRent(state, 5, 7))
        // آزادسازی: ۱۱۰ هزار
        val cost = BuildingSystem.redeemCost(state.space(5))
        assertEquals(110_000, cost)
        TestHelpers.giveMoney(engine, 0, 200_000)
        assertEquals(null, BuildingSystem.redeem(state, 0, 5))
        assertFalse(state.prop(5).mortgaged)
    }

    @Test
    fun `cannot mortgage street with buildings`() {
        val engine = TestHelpers.makeGame(seed = 34)
        TestHelpers.giveProperty(engine, 0, 1, houses = 1)
        TestHelpers.giveProperty(engine, 0, 3)
        assertTrue(BuildingSystem.canMortgage(engine.state, 0, 1) != null)
        assertTrue(BuildingSystem.canMortgage(engine.state, 0, 3) != null) // گروه ساختمان دارد
    }

    @Test
    fun `bank house supply limit`() {
        val rules = GameRules(houseBankLimit = 2, hotelBankLimit = 1)
        val engine = TestHelpers.makeGame(seed = 35, rules = rules)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        TestHelpers.giveProperty(engine, 0, 3)
        TestHelpers.giveMoney(engine, 0, 10_000_000)
        BuildingSystem.build(state, 0, 1)
        BuildingSystem.build(state, 0, 3)
        // ذخیره تمام شد
        assertTrue(BuildingSystem.canBuild(state, 0, 1) != null)
    }
}
