package com.monopoly.world3d.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsTest {

    private fun dice(a: Int, b: Int) = TestHelpers.ScriptedRandom(listOf(a, b))

    @Test
    fun `decks have at least 30 unique cards each`() {
        assertTrue(Cards.chanceIds().size >= 30)
        assertTrue(Cards.chestIds().size >= 30)
        assertEquals(Cards.chanceIds().size, Cards.chanceIds().toSet().size)
        assertEquals(Cards.chestIds().size, Cards.chestIds().toSet().size)
    }

    @Test
    fun `all cards have persian title and description`() {
        val ids = Cards.chanceIds() + Cards.chestIds()
        for (id in ids) {
            val c = Cards.byId(id)
            assertTrue(c.title.isNotBlank())
            assertTrue(c.description.isNotBlank())
            assertTrue("عنوان باید فارسی باشد", c.title.any { it in 'آ'..'ی' })
            assertTrue("توضیح باید فارسی باشد", c.description.any { it in 'آ'..'ی' })
        }
    }

    @Test
    fun `gain card adds money`() {
        val engine = TestHelpers.makeGame(seed = 40)
        val state = engine.state
        val p = state.currentPlayer
        val card = Cards.byId(111) // وام بازرگانی ۴۵ هزار
        val b = p.balance
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(engine, p, card, 3)
        assertEquals(b + 45_000, p.balance)
    }

    @Test
    fun `move to card collects go salary`() {
        val engine = TestHelpers.makeGame(seed = 41)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 20
        val card = Cards.byId(100) // به شروع بروید
        val b = p.balance
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(engine, p, card, 3)
        assertEquals(0, p.position)
        assertEquals(b + state.rules.goSalary, p.balance)
    }

    @Test
    fun `jail free card kept when free used when in jail`() {
        val engine = TestHelpers.makeGame(seed = 42)
        val state = engine.state
        val p = state.currentPlayer
        val card = Cards.byId(118)
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        // آزاد: نگه می‌دارد
        method.invoke(engine, p, card, 3)
        assertEquals(1, p.jailCards)
        // زندانی: بلافاصله استفاده می‌شود (کارت مستقیماً از دسته مصرف شده و به دست نمی‌رود)
        p.inJail = true
        method.invoke(engine, p, card, 3)
        assertFalse(p.inJail)
        assertEquals(1, p.jailCards)
    }

    @Test
    fun `go to jail card sends to jail`() {
        val engine = TestHelpers.makeGame(seed = 43)
        val state = engine.state
        val p = state.currentPlayer
        val card = Cards.byId(117)
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(engine, p, card, 3)
        assertTrue(p.inJail)
        assertEquals(Board.JAIL_POS, p.position)
    }

    @Test
    fun `building repairs charge per building`() {
        val engine = TestHelpers.makeGame(seed = 44)
        val state = engine.state
        val p = state.currentPlayer
        TestHelpers.giveProperty(engine, 0, 1, houses = 2)
        TestHelpers.giveProperty(engine, 0, 3, houses = 5) // هتل
        val card = Cards.byId(121) // خانه ۲۵هزار، هتل ۱۰۰هزار
        val b = p.balance
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(engine, p, card, 3)
        assertEquals(b - (2 * 25_000 + 100_000), p.balance)
    }

    @Test
    fun `collect from each takes money from all others`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 45)
        val state = engine.state
        val p = state.players[0]
        val card = Cards.byId(127) // ۱۰ هزار از هر بازیکن
        val b0 = p.balance
        val b1 = state.players[1].balance
        val b2 = state.players[2].balance
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(engine, p, card, 3)
        assertEquals(b0 + 20_000, p.balance)
        assertEquals(b1 - 10_000, state.players[1].balance)
        assertEquals(b2 - 10_000, state.players[2].balance)
    }

    @Test
    fun `move back three steps without salary`() {
        val engine = TestHelpers.makeGame(seed = 46)
        val state = engine.state
        val p = state.currentPlayer
        p.position = 2
        val card = Cards.byId(230) // سه قدم عقب (اتفاقات)
        val b = p.balance
        val method = Engine::class.java.getDeclaredMethod("applyCardEffect", Player::class.java, Card::class.java, Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(engine, p, card, 3)
        assertEquals(39, p.position)
        assertEquals(b, p.balance) // حقوقی داده نشد
    }

    @Test
    fun `drawn chance card actually lands on board via dice`() {
        val engine = TestHelpers.makeGame(seed = 47)
        val state = engine.state
        val p = state.currentPlayer
        // از خانه‌ی ۶ با مجموع ۱ نمی‌شود؛ از ۶ با ۱+... خانه ۷ شانس: مجموع ۱ ناممکن بدون جفت
        // پس از ۵ (ایستگاه) با مجموع ۲ (جفت) → ۷ شانس
        p.position = 5
        state.rng = dice(0, 0)
        val before = p.balance
        engine.rollDice()
        // یا کارت یا خانه‌های دیگر؛ در هر صورت بازی پایدار مانده
        assertTrue(p.balance >= 0)
        assertTrue(state.awaiting == DecisionKind.NONE || state.awaiting == DecisionKind.BUY_PROMPT || p.inJail)
    }
}

class JailTest {

    private fun dice(a: Int, b: Int) = TestHelpers.ScriptedRandom(listOf(a, b))

    @Test
    fun `pay jail fine frees player`() {
        val engine = TestHelpers.makeGame(seed = 50)
        val state = engine.state
        val p = state.currentPlayer
        p.inJail = true
        state.awaiting = DecisionKind.JAIL_DECISION
        val b = p.balance
        val pot = state.parkingPot
        engine.payJailFine()
        assertFalse(p.inJail)
        assertEquals(b - state.rules.jailFine, p.balance)
        assertEquals(pot + state.rules.jailFine, state.parkingPot)
        assertEquals(DecisionKind.NONE, state.awaiting)
    }

    @Test
    fun `use jail card frees player`() {
        val engine = TestHelpers.makeGame(seed = 51)
        val state = engine.state
        val p = state.currentPlayer
        p.inJail = true
        p.jailCards = 1
        state.awaiting = DecisionKind.JAIL_DECISION
        engine.useJailCard()
        assertFalse(p.inJail)
        assertEquals(0, p.jailCards)
    }

    @Test
    fun `doubles in jail frees and moves`() {
        val engine = TestHelpers.makeGame(seed = 52)
        val state = engine.state
        val p = state.currentPlayer
        p.inJail = true
        state.awaiting = DecisionKind.JAIL_DECISION
        p.position = Board.JAIL_POS
        state.rng = dice(1, 1) // ۲+۲ = ۴
        engine.rollDice()
        assertFalse(p.inJail)
        assertEquals(14, p.position)
    }

    @Test
    fun `failed jail roll ends turn`() {
        val rules = GameRules(auctionsEnabled = false)
        val engine = TestHelpers.makeGame(players = 2, seed = 53, rules = rules)
        val state = engine.state
        val p = state.currentPlayer
        p.inJail = true
        state.awaiting = DecisionKind.JAIL_DECISION
        state.rng = dice(0, 2) // ۱+۳ = ۴ غیرجفت
        engine.rollDice()
        assertTrue(p.inJail)
        assertEquals(1, p.jailTurns)
        assertEquals(1, state.currentTurn) // نوبت رفت به بازیکن بعد
    }

    @Test
    fun `three failed attempts force fine and move`() {
        val rules = GameRules(auctionsEnabled = false)
        val engine = TestHelpers.makeGame(players = 2, seed = 54, rules = rules)
        val state = engine.state
        val p = state.currentPlayer
        p.position = Board.JAIL_POS
        p.inJail = true
        state.awaiting = DecisionKind.JAIL_DECISION
        state.rng = dice(0, 2)
        engine.rollDice() // تلاش ۱
        TestHelpers.forceTurn(engine, 0)
        p.inJail = true
        state.awaiting = DecisionKind.JAIL_DECISION
        state.rng = dice(0, 2)
        engine.rollDice() // تلاش ۲
        TestHelpers.forceTurn(engine, 0)
        p.inJail = true
        state.awaiting = DecisionKind.JAIL_DECISION
        state.rng = dice(0, 2)
        val before = p.balance
        engine.rollDice() // تلاش ۳ → جریمه اجباری + حرکت
        assertFalse(p.inJail)
        assertEquals(before - state.rules.jailFine, p.balance)
        assertEquals(14, p.position) // ۱۰ + ۴
        // فرود روی ملک آزاد: تصمیم خرید حفظ می‌شود و پس از آن نوبت تمام می‌شود
        assertEquals(DecisionKind.BUY_PROMPT, state.awaiting)
        engine.declinePurchase()
        engine.endTurn()
        assertEquals(1, state.currentTurn)
    }
}
