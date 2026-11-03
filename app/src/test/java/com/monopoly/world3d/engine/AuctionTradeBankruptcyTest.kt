package com.monopoly.world3d.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuctionTest {

    private fun dice(a: Int, b: Int) = TestHelpers.ScriptedRandom(listOf(a, b))

    @Test
    fun `bid below minimum is rejected`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 60)
        val state = engine.state
        engine.startAuction(3)
        val auction = state.auction!!
        val bidder = auction.currentBidder()
        // پیشنهاد کمتر از قیمت پایه
        val events = engine.auctionBid(bidder, 10_000)
        assertEquals(-1, auction.highestBidder)
        // پیشنهاد معتبر
        engine.auctionBid(bidder, state.space(3).price)
        assertEquals(bidder, auction.highestBidder)
        assertEquals(state.space(3).price, auction.highestBid)
    }

    @Test
    fun `bid above balance is rejected`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 61)
        val state = engine.state
        engine.startAuction(3)
        val auction = state.auction!!
        val bidder = auction.currentBidder()
        val balance = state.players[bidder].balance
        val before = auction.highestBidder
        engine.auctionBid(bidder, balance + 1_000_000)
        assertEquals(before, auction.highestBidder)
    }

    @Test
    fun `passing until one left awards property to highest bidder`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 62)
        val state = engine.state
        engine.startAuction(3)
        var auction = state.auction!!
        // بازیکن اول پیشنهاد می‌دهد
        val first = auction.currentBidder()
        engine.auctionBid(first, 60_000)
        auction = state.auction!!
        // بقیه انصراف می‌دهند
        while (state.auction != null) {
            val a = state.auction!!
            val bidder = a.currentBidder()
            engine.auctionPass(bidder)
        }
        assertEquals(first, state.prop(3).owner)
        val balance = state.players[first].balance
        assertEquals(state.rules.startMoney - 60_000, balance)
        assertEquals(DecisionKind.NONE, state.awaiting)
    }

    @Test
    fun `no bids leaves property with bank`() {
        val engine = TestHelpers.makeGame(players = 2, seed = 63)
        val state = engine.state
        engine.startAuction(3)
        while (state.auction != null) {
            val a = state.auction!!
            engine.auctionPass(a.currentBidder())
        }
        assertEquals(-1, state.prop(3).owner)
    }

    @Test
    fun `bankruptcy to bank queues auction`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 64)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        TestHelpers.giveProperty(engine, 0, 3)
        engine.declareBankruptcy(0, -1, "تست")
        assertTrue(state.players[0].bankrupt)
        assertEquals(-1, state.prop(1).owner)
        assertEquals(2, state.auctionQueue.size)
        // بازی ادامه دارد با ۲ بازیکن
        assertFalse(state.isGameOver)
    }

    @Test
    fun `winner is last active player`() {
        val engine = TestHelpers.makeGame(players = 2, seed = 65)
        val state = engine.state
        engine.declareBankruptcy(1, -1, "تست")
        assertTrue(state.isGameOver)
        assertEquals(0, state.winnerId)
        assertEquals(DecisionKind.GAME_OVER, state.awaiting)
    }
}

class TradeTest {

    @Test
    fun `valid trade transfers money and properties`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 70)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        TestHelpers.giveProperty(engine, 1, 5)
        val offer = TradeOffer(
            fromId = 0, toId = 1,
            giveProperties = listOf(1), getProperties = listOf(5),
            giveMoney = 50_000, getMoney = 0
        )
        assertEquals(null, TradingSystem.validate(state, offer))
        val b0 = state.players[0].balance
        val b1 = state.players[1].balance
        assertTrue(TradingSystem.execute(state, offer))
        assertEquals(0, state.prop(5).owner)   // ایستگاه به انسان رفت
        assertEquals(1, state.prop(1).owner)   // خیابان به رایانه رفت
        assertEquals(b0 - 50_000, state.players[0].balance)
        assertEquals(b1 + 50_000, state.players[1].balance)
    }

    @Test
    fun `trade with unowned property is invalid`() {
        val engine = TestHelpers.makeGame(seed = 71)
        val state = engine.state
        val offer = TradeOffer(fromId = 0, toId = 1, giveProperties = listOf(3))
        assertTrue(TradingSystem.validate(state, offer) != null)
    }

    @Test
    fun `trade of property with buildings is invalid`() {
        val engine = TestHelpers.makeGame(seed = 72)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1, houses = 1)
        val offer = TradeOffer(fromId = 0, toId = 1, giveProperties = listOf(1))
        assertTrue(TradingSystem.validate(state, offer) != null)
    }

    @Test
    fun `trade with insufficient money is invalid`() {
        val engine = TestHelpers.makeGame(seed = 73)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        val offer = TradeOffer(fromId = 0, toId = 1, giveProperties = listOf(1), getMoney = 10_000_000)
        assertTrue(TradingSystem.validate(state, offer) != null)
    }

    @Test
    fun `empty trade is invalid`() {
        val engine = TestHelpers.makeGame(seed = 74)
        val state = engine.state
        val offer = TradeOffer(fromId = 0, toId = 1)
        assertTrue(TradingSystem.validate(state, offer) != null)
    }

    @Test
    fun `trade to bankrupt player is invalid`() {
        val engine = TestHelpers.makeGame(seed = 75)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        engine.declareBankruptcy(1, -1, "تست")
        val offer = TradeOffer(fromId = 0, toId = 1, giveProperties = listOf(1))
        assertTrue(TradingSystem.validate(state, offer) != null)
    }

    @Test
    fun `ai responds to trade offers`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 76)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        TestHelpers.giveProperty(engine, 0, 3) // گروه کامل انسان
        TestHelpers.giveProperty(engine, 1, 5)
        val brain = AIController.forPlayer(state, state.players[1])
        // پیشنهاد خوب برای AI (پول زیاد) باید پذیرفته شود
        val good = TradeOffer(fromId = 0, toId = 1, getProperties = listOf(5), giveMoney = 400_000)
        val r1 = brain.evaluateTrade(state, 1, good)
        assertTrue(r1 == AIPlayer.TradeResponse.ACCEPT || r1 == AIPlayer.TradeResponse.COUNTER)
        // پیشنهاد بد باید رد شود
        val bad = TradeOffer(fromId = 0, toId = 1, getProperties = listOf(5))
        val r2 = brain.evaluateTrade(state, 1, bad)
        assertTrue(r2 == AIPlayer.TradeResponse.REJECT || r2 == AIPlayer.TradeResponse.COUNTER)
    }
}

class BankruptcyTest {

    @Test
    fun `assets transfer to creditor player`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 80)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1)
        TestHelpers.giveProperty(engine, 0, 5)
        state.players[0].jailCards = 2
        state.players[0].balance = 30_000
        engine.declareBankruptcy(0, 1, "تست")
        assertTrue(state.players[0].bankrupt)
        assertEquals(1, state.prop(1).owner)
        assertEquals(1, state.prop(5).owner)
        assertEquals(2, state.players[1].jailCards)
        assertEquals(1_530_000, state.players[1].balance)
    }

    @Test
    fun `mortgaged transfer charges creditor fee`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 81)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 5)
        BuildingSystem.mortgage(state, 0, 5)
        val creditorBalance = state.players[1].balance
        engine.declareBankruptcy(0, 1, "تست")
        assertEquals(1, state.prop(5).owner)
        assertTrue(state.prop(5).mortgaged)
        // بهره‌ی ۱۰٪ رهن (۱۰ هزار کسر) + پول نقد ورشکسته (۱٫۵ میلیون + ۱۰۰ هزار رهن)
        assertEquals(creditorBalance - 10_000 + 1_600_000, state.players[1].balance)
    }

    @Test
    fun `buildings sold to bank at half price to creditor`() {
        val engine = TestHelpers.makeGame(players = 3, seed = 82)
        val state = engine.state
        TestHelpers.giveProperty(engine, 0, 1, houses = 3)
        TestHelpers.giveProperty(engine, 0, 3, houses = 3)
        val creditorBalance = state.players[1].balance
        engine.declareBankruptcy(0, 1, "تست")
        // ۶ خانه × ۲۵ هزار = ۱۵۰ هزار + پول نقد ورشکسته (۱٫۵ میلیون)
        assertEquals(creditorBalance + 150_000 + 1_500_000, state.players[1].balance)
        assertEquals(0, state.prop(1).houses)
        assertEquals(0, state.prop(3).houses)
    }
}
