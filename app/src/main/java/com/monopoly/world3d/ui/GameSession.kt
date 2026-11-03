package com.monopoly.world3d.ui

import com.monopoly.world3d.engine.GameState

/** حامل وضعیت بازی بین فعالیت‌ها (بدون سریال‌سازی سنگین) */
object GameSession {
    var state: GameState? = null
    var isSinglePlayer: Boolean = true
    var cameFromLoad: Boolean = false
}
