package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.AccentPalette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد لوحة الألوان العشرة (alpha18) — توجيه المالك:
 * «نضيف قسم جديد في الاعدادات لتغيير ألوان التطبيق وعايز ١٠ ألوان».
 */
class AccentPaletteTest {

    @Test
    fun `اللوحة عشرة ألوان بالضبط`() {
        assertEquals(10, AccentPalette.All.size)
    }

    @Test
    fun `المعرفات عربية الأسماء وفريدة`() {
        val ids = AccentPalette.All.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val names = AccentPalette.All.map { it.arabicName }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.isNotBlank() })
    }

    @Test
    fun `الافتراضي هو الزمردي وهو أول القائمة — ألوان الهوية الأصلية`() {
        val first = AccentPalette.All.first()
        assertSame(first, AccentPalette.default)
        assertEquals("emerald", AccentPalette.DEFAULT_ID)
        // حماية الهوية: الزمردي يحمل ألوان الثيم الأصلية حرفياً
        assertEquals(0xFF2E6B5E.toInt(), first.day.primary.toArgbInt())
        assertEquals(0xFF82C9B0.toInt(), first.night.primary.toArgbInt())
    }

    @Test
    fun `الاسترجاع بالمعرف متسامح — المفقود والغريب يرجعان للافتراضي`() {
        assertSame(AccentPalette.default, AccentPalette.byId("emerald"))
        assertSame(AccentPalette.default, AccentPalette.byId("ghost-color"))
        assertSame(AccentPalette.default, AccentPalette.byId(null))
        assertNotEquals(AccentPalette.default, AccentPalette.byId("indigo"))
    }

    @Test
    fun `كل لون يوفر وضعيه النهاري والليلي بكل التوكينات`() {
        AccentPalette.All.forEach { accent ->
            listOf(accent.day, accent.night).forEach { set ->
                assertTrue("تدرج الهيرو أعمق من الأساس ${accent.id}", set.heroBottom != set.heroTop)
                assertTrue("محتوى الهيرو موجود ${accent.id}", set.heroContent != set.heroTop)
            }
        }
    }

    private fun Color.toArgbInt(): Int = this.toArgb()
}
