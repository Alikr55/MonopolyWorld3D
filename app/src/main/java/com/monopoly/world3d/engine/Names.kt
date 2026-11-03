package com.monopoly.world3d.engine

/** نام‌ها و رنگ‌های پیش‌فرض بازیکنان */
object Names {

    val AI_NAMES = listOf(
        "آرش", "سارا", "نیما", "مینا", "کاوه", "رزا",
        "بهرام", "لیلا", "فرهاد", "شیرین", "بابک", "نگار",
        "کیان", "پریسا", "سامان", "ترانه"
    )

    /** ده رنگ شاد برای مهره‌ها */
    val COLORS = listOf(
        0xFFD32F2F, // قرمز
        0xFF1976D2, // آبی
        0xFF388E3C, // سبز
        0xFFFBC02D, // زرد
        0xFF7B1FA2, // بنفش
        0xFFF57C00, // نارنجی
        0xFF00796B, // سبزآبی
        0xFFC2185B, // سرخابی
        0xFF455A64, // خاکستری آبی
        0xFF5D4037  // قهوه‌ای
    )

    val COLOR_NAMES = listOf(
        "قرمز", "آبی", "سبز", "زرد", "بنفش", "نارنجی", "سبزآبی", "سرخابی", "دودی", "قهوه‌ای"
    )

    fun personalityFor(index: Int): AiPersonality =
        AiPersonality.entries[index % AiPersonality.entries.size]
}
