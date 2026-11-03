package com.monopoly.world3d.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های رگرسیون باگ‌های نسخه ۱٫۰٫۰:
 * ۱) کرش IndexOutOfBoundsException در کشیدن کارت از دسته‌ی خالی
 * ۲) بن‌بست AI در حراج با موجودی ناکافی
 * ۳) خرابی ذخیره/بارگذاری با نویسه‌های خاص
 * ۴) بازیابی حراج نیمه‌کاره
 */
class CrashRegressionTest {

    private fun newGame(seed: Long, players: Int = 3): Engine {
        val list = mutableListOf<Player>()
        list.add(Player(0, "انسان", TokenKind.CROWN, 0xFFD32F2F, isHuman = true))
        for (i in 1 until players) {
            list.add(Player(i, "رایانه$i", TokenKind.entries[i], 0xFF1976D2, isHuman = false,
                aiLevel = AiLevel.NORMAL, personality = Names.personalityFor(i)))
        }
        val state = GameState(list, GameRules(), seed)
        val engine = Engine(state)
        engine.startGame()
        return engine
    }

    @Test
    fun `کشیدن کارت از دسته خالی هرگز کرش نمی‌زند`() {
        val engine = newGame(seed = 5)
        val state = engine.state
        // بازتولید دقیق وضعیت باگ نسخه ۱٫۰٫۰: دسته‌های کارت خالی‌اند (شبه‌حالت قبل از startGame)
        state.chanceDeck.clear()
        state.chestDeck.clear()

        val p = state.currentPlayer
        p.position = 5
        p.balance = 1_500_000
        // تاس ۱ و ۱ (nextInt=0 → تاس ۱) → حرکت ۲ قدم → فرود روی خانه ۷ (کارت شانس)
        state.rng = TestHelpers.ScriptedRandom(listOf(0, 0))

        val events = engine.rollDice() // قبل از تعمیر: IndexOutOfBoundsException
        assertTrue("باید کارت کشیده می‌شد", state.lastDrawnCardId >= 0)
        assertTrue("دسته پس از شافل باید پر شود", state.chanceDeck.size >= 36)
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun `شافل دسته‌ها در جا انجام می‌شود و اشاره‌گرهای قدیمی معتبر می‌مانند`() {
        val engine = newGame(seed = 6)
        val state = engine.state
        val chanceRef: MutableList<Int> = state.chanceDeck
        val chestRef: MutableList<Int> = state.chestDeck
        state.chanceDeck.clear()
        state.chestDeck.clear()
        state.shuffleDecks()
        assertTrue("شافل باید همان نمونه‌ی لیست را پر کند", chanceRef === state.chanceDeck)
        assertTrue(chestRef === state.chestDeck)
        assertEquals(Cards.chanceIds().size, state.chanceDeck.size)
        assertEquals(Cards.chestIds().size, state.chestDeck.size)
    }

    @Test
    fun `AI با موجودی ناکافی در حراج انصراف می‌دهد نه بن‌بست`() {
        val engine = newGame(seed = 11, players = 4)
        // همه‌ی بازیکنان را تقریباً بی‌پول می‌کنیم
        engine.state.players.forEach { it.balance = 5_000 }
        engine.startAuction(1) // خیابان ری — قیمت پایه ۶۰٬۰۰۰
        val state = engine.state
        assertTrue(state.awaiting == DecisionKind.AUCTION)

        var steps = 0
        while (state.auction != null && steps < 200) {
            assertTrue("AI باید بتواند گامی بردارد", AiDriver.step(engine))
            steps++
        }
        assertNull("حراج باید بدون بن‌بست تمام می‌شد", state.auction)
        assertTrue(steps < 200)
    }

    @Test
    fun `ذخیره و بارگذاری با نویسه‌های خاص و فارسی سالم می‌ماند`() {
        val engine = newGame(seed = 21)
        val state = engine.state
        state.players[0].name = "«طراح\"بازی\"\nنسخه\t۲"
        state.players[1].name = "بازیکن %100 با ٬ جداکننده"
        // وضعیت غنی: خرید، رهن و کارت
        Bank.deposit(state, 0, 500_000, "تست")
        assertTrue(Bank.withdraw(state, 0, state.space(1).price, "خرید"))
        state.prop(1).owner = 0
        BuildingSystem.mortgage(state, 0, 1)
        state.statsFor(0).rentCollected = 123_456

        val json = SaveManager.serialize(state)
        val loaded = SaveManager.parse(json)
        assertEquals("«طراح\"بازی\"\nنسخه\t۲", loaded.players[0].name)
        assertEquals("بازیکن %100 با ٬ جداکننده", loaded.players[1].name)
        assertEquals(0, loaded.prop(1).owner)
        assertTrue(loaded.prop(1).mortgaged)
        assertEquals(state.players[0].balance, loaded.players[0].balance)
        assertEquals(123_456L, loaded.statsFor(0).rentCollected)
        assertEquals(state.chanceDeck.size, loaded.chanceDeck.size)
    }

    @Test
    fun `ذخیره در وسط حراج با پرچم شروع مجدد بازیابی می‌شود`() {
        val engine = newGame(seed = 31)
        engine.startAuction(5)
        val json = SaveManager.serialize(engine.state)
        val loaded = SaveManager.parse(json)
        assertEquals(5, loaded.pendingAuctionRestart)
        assertNull(loaded.auction)
        // GameActivity پس از بارگذاری حراج را از نو آغاز می‌کند
        Engine(loaded).startAuction(loaded.pendingAuctionRestart)
        assertEquals(DecisionKind.AUCTION, loaded.awaiting)
    }

    @Test
    fun `بازی کامل چهار AI تا پایان بدون استثنا و بن‌بست`() {
        val engine = TestHelpers.makeAiGame(players = 4, seed = 77, level = AiLevel.NORMAL)
        val steps = AiDriver.runFullGame(engine)
        assertTrue("بازی باید تمام می‌شد (گام‌ها: $steps)", engine.state.isGameOver)
        assertFalse(engine.state.activePlayers().isEmpty())
    }

    @Test
    fun `پس از ورشکستگی، بازیکن ورشکسته نوبت نمی‌گیرد`() {
        val engine = newGame(seed = 41, players = 3)
        val state = engine.state
        // انسان ورشکست می‌شود؛ نوبت باید به AI بعدی برود
        engine.declareBankruptcy(0, -1, "تست")
        assertTrue(state.players[0].bankrupt)
        if (!state.isGameOver) {
            assertFalse("نوبت‌دار نباید ورشکسته باشد", state.currentPlayer.bankrupt)
        }
    }
}
