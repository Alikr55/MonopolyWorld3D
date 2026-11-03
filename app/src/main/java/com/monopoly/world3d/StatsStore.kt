package com.monopoly.world3d

import android.content.Context
import com.monopoly.world3d.engine.Achievements
import com.monopoly.world3d.engine.AchievementDef
import com.monopoly.world3d.engine.GameResultContext
import com.monopoly.world3d.engine.GlobalStats
import com.monopoly.world3d.engine.MiniJson

/**
 * ذخیره‌ی آمار و دستاوردها در فایل‌های محلی (JSON).
 */
class StatsStore(context: Context) {

    private val statsFile = java.io.File(context.filesDir, "stats.json")
    private val achFile = java.io.File(context.filesDir, "achievements.json")

    val stats = GlobalStats()
    val unlocked = LinkedHashSet<String>()

    /** شنونده برای اطلاع‌رسانی دستاورد تازه */
    var onAchievementUnlocked: ((AchievementDef) -> Unit)? = null

    init { load() }

    fun load() {
        try {
            if (statsFile.exists()) {
                stats.fromMap(MiniJson.parse(statsFile.readText()) as? Map<String, Any?> ?: emptyMap())
            }
        } catch (e: Exception) { /* فایل خراب: از نو شروع می‌کنیم */ }
        try {
            if (achFile.exists()) {
                val arr = MiniJson.parse(achFile.readText()) as? List<*> ?: return
                for (id in arr) (id as? String)?.let { unlocked.add(it) }
            }
        } catch (e: Exception) { /* همان بالا */ }
    }

    fun save() {
        try {
            statsFile.writeText(MiniJson.write(stats.toMap()))
            achFile.writeText(MiniJson.write(unlocked.toList()))
        } catch (e: Exception) { /* حافظه پر؟ بی‌خیال */ }
    }

    /** به‌روزرسانی آمار بعد از پایان یک بازی */
    fun applyGameResult(ctx: GameResultContext): List<AchievementDef> {
        val s = stats
        s.gamesPlayed++
        if (ctx.humanWon) {
            s.gamesWon++
            if (ctx.bankruptCount >= 2) s.perfectWins++
            if (ctx.hardestAiLevel == com.monopoly.world3d.engine.AiLevel.HARD) s.hardWins++
            if (ctx.playerCount == 6) s.wins6Players++
            if (s.fastestWinMillis == 0L || ctx.durationMillis < s.fastestWinMillis) s.fastestWinMillis = ctx.durationMillis
        } else {
            s.gamesLost++
        }
        if (ctx.durationMillis > s.longestGameMillis) s.longestGameMillis = ctx.durationMillis
        s.totalPlayMillis += ctx.durationMillis

        val hs = ctx.humanStats
        s.propertiesBought += hs.propertiesBought
        s.housesBuilt += hs.housesBuilt
        s.hotelsBuilt += hs.hotelsBuilt
        s.rentCollected += hs.rentCollected
        s.rentPaid += hs.rentPaid
        s.taxesPaid += hs.taxesPaid
        s.tradesDone += hs.tradesDone
        s.auctionsWon += hs.auctionsWon
        s.cardsDrawn += hs.cardsDrawn
        s.timesInJail += hs.timesInJail
        s.doublesRolled += hs.doublesRolled
        s.goPasses += hs.goPasses
        s.bankruptcies += if (ctx.humanNetWorth == 0L && !ctx.humanWon) 1 else 0
        if (ctx.potCollected > s.potCollectedBest) s.potCollectedBest = ctx.potCollected
        if (ctx.humanNetWorth > s.maxNetWorth) s.maxNetWorth = ctx.humanNetWorth
        s.groupsCompleted += ctx.groupsCompletedByHuman
        if (ctx.groupsCompletedByHuman > 0) s.allGroupsCompleted = s.groupsCompleted >= 8

        val newly = mutableListOf<AchievementDef>()
        for (a in Achievements.ALL) {
            if (a.id in unlocked) continue
            if (a.check(s, ctx)) {
                unlocked.add(a.id)
                newly.add(a)
            }
        }
        save()
        return newly
    }

    /** فرار از زندان و رفتن به زندان در حین بازی */
    fun recordJailVisit() { stats.timesInJail++; save() }
    fun recordJailEscape() { stats.jailEscapes++; save() }
}
