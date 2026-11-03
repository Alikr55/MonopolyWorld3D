package com.monopoly.world3d.engine

/**
 * کتابخانه‌ی JSON مینیمال و بدون وابستگی، برای ذخیره‌سازی بازی و آمار.
 * پشتیبانی از: Map، List، String، Long/Int، Double، Boolean، null.
 */
object MiniJson {

    // ---------------- نوشتن ----------------

    fun write(value: Any?): String {
        val sb = StringBuilder()
        writeValue(value, sb)
        return sb.toString()
    }

    private fun writeValue(v: Any?, sb: StringBuilder) {
        when (v) {
            null -> sb.append("null")
            is String -> writeString(v, sb)
            is Boolean -> sb.append(if (v) "true" else "false")
            is Int -> sb.append(v.toString())
            is Long -> sb.append(v.toString())
            is Double -> {
                if (v.isNaN() || v.isInfinite()) sb.append("null") else sb.append(v.toString())
            }
            is Float -> writeValue(v.toDouble(), sb)
            is Map<*, *> -> writeObject(v, sb)
            is List<*> -> writeArray(v, sb)
            is Array<*> -> writeArray(v.toList(), sb)
            else -> writeString(v.toString(), sb)
        }
    }

    private fun writeObject(m: Map<*, *>, sb: StringBuilder) {
        sb.append('{')
        var first = true
        for ((k, v) in m) {
            if (!first) sb.append(',')
            first = false
            writeString(k.toString(), sb)
            sb.append(':')
            writeValue(v, sb)
        }
        sb.append('}')
    }

    private fun writeArray(a: List<*>, sb: StringBuilder) {
        sb.append('[')
        var first = true
        for (v in a) {
            if (!first) sb.append(',')
            first = false
            writeValue(v, sb)
        }
        sb.append(']')
    }

    private fun writeString(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    // ---------------- خواندن ----------------

    class JsonException(msg: String) : Exception(msg)

    fun parse(text: String): Any? {
        val p = Parser(text)
        val v = p.parseValue()
        p.skipWhitespace()
        if (!p.atEnd()) throw JsonException("متن اضافی بعد از JSON")
        return v
    }

    private class Parser(val s: String) {
        var i = 0

        fun atEnd() = i >= s.length

        fun skipWhitespace() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): Any? {
            skipWhitespace()
            if (atEnd()) throw JsonException("پایان ناگهانی متن")
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> { expect("true"); true }
                'f' -> { expect("false"); false }
                'n' -> { expect("null"); null }
                else -> parseNumber()
            }
        }

        fun expect(word: String) {
            if (i + word.length > s.length || s.substring(i, i + word.length) != word)
                throw JsonException("انتظار «$word» در موقعیت $i")
            i += word.length
        }

        fun parseObject(): LinkedHashMap<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            i++ // {
            skipWhitespace()
            if (!atEnd() && s[i] == '}') { i++; return map }
            while (true) {
                skipWhitespace()
                if (atEnd() || s[i] != '"') throw JsonException("کلید نامعتبر در $i")
                val key = parseString()
                skipWhitespace()
                if (atEnd() || s[i] != ':') throw JsonException("انتظار : در $i")
                i++
                map[key] = parseValue()
                skipWhitespace()
                if (atEnd()) throw JsonException("شی ناتمام")
                when (s[i]) {
                    ',' -> i++
                    '}' -> { i++; return map }
                    else -> throw JsonException("انتظار , یا } در $i")
                }
            }
        }

        fun parseArray(): ArrayList<Any?> {
            val list = ArrayList<Any?>()
            i++ // [
            skipWhitespace()
            if (!atEnd() && s[i] == ']') { i++; return list }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                if (atEnd()) throw JsonException("آرایه ناتمام")
                when (s[i]) {
                    ',' -> i++
                    ']' -> { i++; return list }
                    else -> throw JsonException("انتظار , یا ] در $i")
                }
            }
        }

        fun parseString(): String {
            val sb = StringBuilder()
            i++ // "
            while (true) {
                if (atEnd()) throw JsonException("رشته ناتمام")
                val c = s[i]
                when {
                    c == '"' -> { i++; return sb.toString() }
                    c == '\\' -> {
                        i++
                        if (atEnd()) throw JsonException("کاراکتر گریز ناتمام")
                        when (val e = s[i]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                            'u' -> {
                                if (i + 4 >= s.length) throw JsonException("یونیکد ناتمام")
                                val hex = s.substring(i + 1, i + 5)
                                sb.append(hex.toInt(16).toChar())
                                i += 4
                            }
                            else -> throw JsonException("کاراکتر گریز نامعتبر: \\$e")
                        }
                        i++
                    }
                    else -> { sb.append(c); i++ }
                }
            }
        }

        fun parseNumber(): Any {
            val start = i
            if (!atEnd() && (s[i] == '-' || s[i] == '+')) i++
            var isDouble = false
            while (!atEnd()) {
                val c = s[i]
                if (c.isDigit()) i++
                else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    if (c == '.' || c == 'e' || c == 'E') isDouble = true
                    i++
                } else break
            }
            val num = s.substring(start, i)
            if (num.isEmpty() || num == "-") throw JsonException("عدد نامعتبر در $start")
            return if (isDouble) num.toDouble() else (num.toLongOrNull() ?: num.toDouble())
        }
    }
}
