package com.monopoly.world3d.engine

import java.util.Random

/** ابزارهای مشترک تست */
object TestHelpers {

    /** Random اسکریپت‌شده: مقادیر داده‌شده را به ترتیب برمی‌گرداند (برای تاس قطعی) */
    class ScriptedRandom(script: List<Int>) : Random() {
        private val values = script.toMutableList()
        private var idx = 0
        var history = mutableListOf<Int>()

        override fun nextInt(bound: Int): Int {
            if (values.isEmpty()) return super.nextInt(bound)
            val v = values[idx % values.size]
            idx++
            history.add(v)
            return v % bound
        }

        override fun nextDouble(): Double = nextInt(1000) / 1000.0

        fun remaining(): List<Int> = values.drop(idx)
    }

    fun makeGame(players: Int = 3, seed: Long = 42, rules: GameRules = GameRules()): Engine {
        val list = mutableListOf<Player>()
        list.add(Player(0, "انسان", TokenKind.CROWN, 0xFFD32F2F, isHuman = true))
        for (i in 1 until players) {
            list.add(Player(i, "رایانه$i", TokenKind.entries[i], 0xFF1976D2, isHuman = false,
                aiLevel = AiLevel.NORMAL, personality = Names.personalityFor(i)))
        }
        val state = GameState(list, rules, seed)
        val engine = Engine(state)
        engine.startGame()
        return engine
    }

    fun makeAiGame(players: Int = 4, seed: Long = 7, level: AiLevel = AiLevel.NORMAL): Engine {
        val list = mutableListOf<Player>()
        for (i in 0 until players) {
            list.add(Player(i, "AI$i", TokenKind.entries[i], 0xFF388E3C, isHuman = false,
                aiLevel = level, personality = Names.personalityFor(i)))
        }
        val state = GameState(list, GameRules(), seed)
        val engine = Engine(state)
        engine.startGame()
        return engine
    }

    /** نوبت را به بازیکن مشخص می‌برد (برای تست مستقیم) */
    fun forceTurn(engine: Engine, playerId: Int) {
        engine.state.currentTurn = playerId
        engine.state.hasRolled = false
        engine.state.rollAgain = false
        engine.state.awaiting = DecisionKind.NONE
    }

    fun giveMoney(engine: Engine, playerId: Int, amount: Long) {
        Bank.deposit(engine.state, playerId, amount, "کمک تست")
    }

    fun giveProperty(engine: Engine, playerId: Int, spaceId: Int, houses: Int = 0) {
        engine.state.prop(spaceId).owner = playerId
        engine.state.prop(spaceId).houses = houses
        engine.state.prop(spaceId).mortgaged = false
    }
}
