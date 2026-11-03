package com.monopoly.world3d.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementAndTurnTest {

    /** تاس اسکریپت‌شده: مقدار n به معنی تاس n+1 است */
    private fun dice(a: Int, b: Int) = TestHelpers.ScriptedRandom(listOf(a, b))

    @Test
    fun `movement passes go and collects salary`() {
        val engine = TestHelpers.makeGame(seed = 5)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 37
        state.rng = dice(0, 1) // ۱+۲ = ۳ → ۳۷+۳ = ۰ (شروع)
        val balanceBefore = p.balance
        engine.rollDice()
        assertEquals(0, p.position)
        assertEquals(balanceBefore + state.rules.goSalary, p.balance)
    }

    @Test
    fun `exact go landing gives double salary when rule on`() {
        val rules = GameRules(doubleSalaryOnExactGo = true)
        val engine = TestHelpers.makeGame(seed = 6, rules = rules)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 36
        state.rng = dice(2, 0) // ۳+۱ = ۴ → ۳۶+۴ = ۰
        val balanceBefore = p.balance
        engine.rollDice()
        assertEquals(0, p.position)
        assertEquals(balanceBefore + state.rules.goSalary * 2, p.balance)
    }

    @Test
    fun `no double salary when rule off`() {
        val engine = TestHelpers.makeGame(seed = 6)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 36
        state.rng = dice(2, 0)
        val balanceBefore = p.balance
        engine.rollDice()
        assertEquals(0, p.position)
        assertEquals(balanceBefore + state.rules.goSalary, p.balance)
    }

    @Test
    fun `doubles grant extra roll`() {
        val engine = TestHelpers.makeGame(seed = 8)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 4
        // خانه‌های مسیر به حریف داده می‌شود تا پرامپت خرید باز نشود
        TestHelpers.giveProperty(engine, 1, 8)
        TestHelpers.giveProperty(engine, 1, 12)
        state.rng = dice(1, 1) // ۲+۲ = ۴
        engine.rollDice()
        assertEquals(8, p.position)
        assertTrue(state.rollAgain)
        assertFalse(engine.canEndTurn())
        // نوبت اضافه
        state.rng = dice(1, 1)
        engine.rollDice()
        assertEquals(12, p.position)
    }

    @Test
    fun `three consecutive doubles send to jail`() {
        val engine = TestHelpers.makeGame(seed = 9)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 4
        TestHelpers.giveProperty(engine, 1, 8)
        TestHelpers.giveProperty(engine, 1, 12)
        TestHelpers.giveProperty(engine, 1, 16)
        state.rng = dice(1, 1)
        engine.rollDice()
        state.rng = dice(1, 1)
        engine.rollDice()
        state.rng = dice(1, 1)
        engine.rollDice()
        assertTrue("سه جفت پیاپی باید به زندان برود", p.inJail)
        assertEquals(Board.JAIL_POS, p.position)
        assertFalse(state.rollAgain)
    }

    @Test
    fun `turn advances to next player and wraps around`() {
        val rules = GameRules(auctionsEnabled = false)
        val engine = TestHelpers.makeGame(players = 3, seed = 10, rules = rules)
        val state = engine.state
        // بازیکن ۰: تاس می‌ریزد (خانه ۳ → رد خرید → بی‌حراج)
        state.rng = dice(0, 1)
        engine.rollDice()
        engine.declinePurchase()
        engine.endTurn()
        assertEquals(1, state.currentTurn)
        // بازیکن ۱
        state.rng = dice(0, 1)
        engine.rollDice()
        engine.declinePurchase()
        engine.endTurn()
        assertEquals(2, state.currentTurn)
        // بازیکن ۲ → دور جدید
        state.rng = dice(0, 1)
        engine.rollDice()
        engine.declinePurchase()
        engine.endTurn()
        assertEquals(0, state.currentTurn)
        assertEquals(2, state.roundCount)
    }

    @Test
    fun `go to jail from dice`() {
        val engine = TestHelpers.makeGame(seed = 11)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 28
        state.rng = dice(0, 0) // ۱+۱ = ۲ → خانه ۳۰
        engine.rollDice()
        assertTrue(p.inJail)
        assertEquals(Board.JAIL_POS, p.position)
        assertFalse(state.rollAgain)
    }

    @Test
    fun `landing on tax pays to pot`() {
        val rules = GameRules(freeParkingPot = true)
        val engine = TestHelpers.makeGame(seed = 12, rules = rules)
        val state = engine.state
        val p = state.currentPlayer
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

class PurchaseTest {

    private fun dice(a: Int, b: Int) = TestHelpers.ScriptedRandom(listOf(a, b))

    @Test
    fun `buying property sets owner and deducts price`() {
        val engine = TestHelpers.makeGame(seed = 11)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 0
        state.rng = dice(0, 1) // مجموع ۳ → خانه ۳
        engine.rollDice()
        assertEquals(3, p.position)
        assertEquals(DecisionKind.BUY_PROMPT, state.awaiting)
        assertEquals(3, state.pendingPurchaseSpace)
        val balanceBefore = p.balance
        engine.buyCurrentProperty()
        assertEquals(0, state.prop(3).owner)
        assertEquals(balanceBefore - state.space(3).price, p.balance)
        assertEquals(DecisionKind.NONE, state.awaiting)
    }

    @Test
    fun `declining purchase starts auction when enabled`() {
        val rules = GameRules(auctionsEnabled = true)
        val engine = TestHelpers.makeGame(players = 3, seed = 12, rules = rules)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 0
        state.rng = dice(0, 1)
        engine.rollDice()
        engine.declinePurchase()
        assertEquals(DecisionKind.AUCTION, state.awaiting)
        assertEquals(3, state.auction!!.participants.size)
    }

    @Test
    fun `declining without auctions just passes`() {
        val rules = GameRules(auctionsEnabled = false)
        val engine = TestHelpers.makeGame(players = 3, seed = 13, rules = rules)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 0
        state.rng = dice(0, 1)
        engine.rollDice()
        engine.declinePurchase()
        assertEquals(DecisionKind.NONE, state.awaiting)
        assertEquals(-1, state.prop(3).owner)
    }

    @Test
    fun `cannot buy without enough cash`() {
        val engine = TestHelpers.makeGame(seed = 14)
        val state = engine.state
        val p = state.currentPlayer
        p.balance = 10_000
        p.position = 0
        state.rng = dice(0, 1)
        engine.rollDice()
        assertEquals(DecisionKind.BUY_PROMPT, state.awaiting)
        engine.buyCurrentProperty()
        assertEquals(-1, state.prop(3).owner)
    }
}
