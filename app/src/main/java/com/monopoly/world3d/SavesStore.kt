package com.monopoly.world3d

import android.content.Context
import com.monopoly.world3d.engine.GameState
import com.monopoly.world3d.engine.SaveManager

/**
 * مدیریت اسلات‌های ذخیره: ۳ اسلت دستی + ذخیره‌ی خودکار.
 * هر اسلات یک فایل اصلی و یک نسخه‌ی پشتیبان (.bak) دارد؛
 * اگر فایل اصلی خراب باشد، از پشتیبان بازیابی می‌شود.
 */
class SavesStore(private val context: Context) {

    companion object {
        const val SLOTS = 3
        const val AUTO = -1

        fun slotFile(context: Context, slot: Int): java.io.File =
            java.io.File(context.filesDir, if (slot == AUTO) "save_auto.json" else "save_slot$slot.json")
    }

    /** ذخیره در اسلات (یا خودکار) با ساخت پشتیبان */
    fun save(slot: Int, state: GameState): Boolean {
        val file = slotFile(context, slot)
        return try {
            val json = SaveManager.serialize(state)
            // پشتیبان از نسخه‌ی قبلی
            if (file.exists()) {
                val bak = java.io.File(file.parentFile, file.name + ".bak")
                bak.writeText(file.readText())
            }
            file.writeText(json)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** بارگذاری با بازیابی از پشتیبان در صورت خرابی */
    fun load(slot: Int): GameState? {
        val file = slotFile(context, slot)
        if (!file.exists()) return null
        // تلاش با فایل اصلی
        try {
            return SaveManager.parse(file.readText())
        } catch (e: Exception) { /* ادامه به پشتیبان */ }
        // بازیابی از پشتیبان
        val bak = java.io.File(file.parentFile, file.name + ".bak")
        if (bak.exists()) {
            try {
                val state = SaveManager.parse(bak.readText())
                file.writeText(bak.readText()) // ترمیم فایل اصلی
                return state
            } catch (e: Exception) { /* پشتیبان هم خراب است */ }
        }
        return null
    }

    fun exists(slot: Int): Boolean = slotFile(context, slot).exists()

    fun delete(slot: Int): Boolean {
        val file = slotFile(context, slot)
        val bak = java.io.File(file.parentFile, file.name + ".bak")
        val a = if (file.exists()) file.delete() else true
        val b = if (bak.exists()) bak.delete() else true
        return a && b
    }

    fun summary(slot: Int): SaveManager.SaveSummary? {
        val file = slotFile(context, slot)
        if (!file.exists()) return null
        return try {
            SaveManager.summarize(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    /** بارگذاری ذخیره‌ی خودکار (بازسازی حراج نیمه‌کاره در GameActivity انجام می‌شود) */
    fun loadAuto(): GameState? = load(AUTO)
}
