package com.monopoly.world3d.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumbersAndJsonTest {

    @Test
    fun `persian digits conversion`() {
        assertEquals("۱۲۳", PersianNumbers.toPersianDigits("123"))
        assertEquals("123", PersianNumbers.toLatinDigits("۱۲۳"))
        assertEquals("1٬500٬000", PersianNumbers.groupThousands(1_500_000))
        assertEquals("۱٬۵۰۰٬۰۰۰ تومان", PersianNumbers.money(1_500_000))
        assertTrue(PersianNumbers.money(50_000).startsWith("۵۰"))
        assertTrue(PersianNumbers.moneyShort(1_500_000).contains("میلیون"))
        assertTrue(PersianNumbers.moneyShort(50_000).contains("هزار"))
    }

    @Test
    fun `json roundtrip with persian text`() {
        val data = linkedMapOf<String, Any?>(
            "name" to "دنیای روپولی «ویژه»",
            "money" to 1_500_000L,
            "flag" to true,
            "nothing" to null,
            "list" to listOf(1, 2, 3),
            "nested" to linkedMapOf("a" to "متن\nچندخطی\"نقل\"", "b" to -42)
        )
        val json = MiniJson.write(data)
        val back = MiniJson.parse(json) as Map<*, *>
        assertEquals(data["name"], back["name"])
        assertEquals(1_500_000L, back["money"])
        assertEquals(true, back["flag"])
        assertEquals(null, back["nothing"])
        assertEquals(listOf(1L, 2L, 3L), back["list"])
        val nested = back["nested"] as Map<*, *>
        assertEquals("متن\nچندخطی\"نقل\"", nested["a"])
        assertEquals(-42L, nested["b"])
    }

    @Test
    fun `json rejects malformed input`() {
        val bad = listOf("{", "[1,2", "{\"a\":}", "\"unterminated", "extra\" text\"")
        for (b in bad) {
            try {
                MiniJson.parse(b)
                org.junit.Assert.fail("باید خطا می‌داد: $b")
            } catch (e: Exception) {
                // انتظار می‌رود
            }
        }
    }
}

class BoardTest {

    @Test
    fun `board has 40 spaces with correct corners`() {
        val spaces = Board.createSpaces()
        assertEquals(40, spaces.size)
        assertEquals(SpaceType.GO, spaces[0].type)
        assertEquals(SpaceType.JAIL, spaces[10].type)
        assertEquals(SpaceType.FREE_PARKING, spaces[20].type)
        assertEquals(SpaceType.GO_TO_JAIL, spaces[30].type)
    }

    @Test
    fun `all streets have complete rent tables`() {
        for (sp in Board.createSpaces()) {
            if (sp.type == SpaceType.STREET) {
                assertEquals("اجاره‌ی ناقص در ${sp.name}", 6, sp.rents.size)
                assertTrue(sp.price > 0)
                assertTrue(sp.houseCost > 0)
            }
            if (sp.type == SpaceType.RAILROAD || sp.type == SpaceType.UTILITY) {
                assertTrue(sp.price > 0)
            }
        }
    }

    @Test
    fun `property groups are balanced`() {
        val spaces = Board.createSpaces()
        val streetGroups = spaces.filter { it.type == SpaceType.STREET }.map { it.group }.toSet()
        assertEquals(8, streetGroups.size)
        for (g in streetGroups) {
            val count = spaces.count { it.group == g }
            assertTrue(count == 2 || count == 3)
        }
        assertEquals(4, spaces.count { it.type == SpaceType.RAILROAD })
        assertEquals(2, spaces.count { it.type == SpaceType.UTILITY })
        assertEquals(3, spaces.count { it.type == SpaceType.CHANCE })
        assertEquals(3, spaces.count { it.type == SpaceType.CHEST })
    }
}
