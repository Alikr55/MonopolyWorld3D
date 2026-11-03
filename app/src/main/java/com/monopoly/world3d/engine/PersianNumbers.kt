package com.monopoly.world3d.engine

/** تبدیل اعداد و مبالغ به فرمت فارسی */
object PersianNumbers {

    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** تبدیل ارقام لاتین به فارسی */
    fun toPersianDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c in '0'..'9') sb.append(faDigits[c - '0']) else sb.append(c)
        }
        return sb.toString()
    }

    /** تبدیل ارقام فارسی به لاتین (برای ورودی کاربر) */
    fun toLatinDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '۰' -> sb.append('0'); '۱' -> sb.append('1'); '۲' -> sb.append('2')
                '۳' -> sb.append('3'); '۴' -> sb.append('4'); '۵' -> sb.append('5')
                '۶' -> sb.append('6'); '۷' -> sb.append('7'); '۸' -> sb.append('8')
                '۹' -> sb.append('9'); else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** جداکننده هزارگان */
    fun groupThousands(n: Long): String {
        val s = n.toString()
        if (n < 0) return "-" + groupThousands(-n)
        val sb = StringBuilder()
        for (i in s.indices) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('٬')
            sb.append(s[i])
        }
        return sb.toString()
    }

    /** مبلغ به تومان با ارقام فارسی: ۱٬۵۰۰٬۰۰۰ تومان */
    fun money(amount: Long): String = toPersianDigits(groupThousands(amount)) + " تومان"

    /** مبلغ کوتاه با ارقام فارسی: ۱.۵ میلیون */
    fun moneyShort(amount: Long): String {
        val abs = if (amount < 0) -amount else amount
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000 -> {
                val m = abs / 100_000.0
                val v = if (m % 10.0 == 0.0) (m / 10).toInt().toString() else String.format("%.1f", m / 10)
                sign + toPersianDigits(v) + " میلیون"
            }
            abs >= 1_000 -> sign + toPersianDigits((abs / 1000).toString()) + " هزار"
            else -> sign + toPersianDigits(abs.toString())
        }
    }

    /** عدد صحیح با ارقام فارسی */
    fun number(n: Int): String = toPersianDigits(n.toString())

    fun number(n: Long): String = toPersianDigits(n.toString())
}
