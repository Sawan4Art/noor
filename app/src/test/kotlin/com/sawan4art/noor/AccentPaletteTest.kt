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
 * عقد لوحة الألوان (alpha18 العشرة الهادئة + alpha28 العشرة الجريئة —
 * توجيه المالك: «عايز اضيف ١٠ الوان اضافين بس يكونوا الوان جريئه»).
 */
class AccentPaletteTest {

    @Test
    fun `اللوحة عشرون لونا بالضبط — العشرة الهادئة ثم العشرة الجريئة`() {
        assertEquals(20, AccentPalette.All.size)
    }

    @Test
    fun `الألوان الجريئة العشرة كاملة بأسمائها العربية`() {
        val bold = AccentPalette.All.drop(10)
        assertEquals(
            listOf("electric", "turquoise", "crimson", "blaze", "lime", "fuchsia", "hotpink", "vividpurple", "solar", "navy"),
            bold.map { it.id },
        )
        assertEquals(
            listOf("كهربائي", "تركوازي", "قرمزي", "ناري", "ليموني", "فوشيا", "زهري", "أرجواني", "أصفر", "كحلي"),
            bold.map { it.arabicName },
        )
    }

    @Test
    fun `الجريئة تشبع عال نهارا وحية لا باهتة ليلا`() {
        // التشبّع الأقصى في HSV: الجريئة نهاراً أساس عميق عالي التشبّع (≥0.7 —
        // الهادئة القديمة كلها تحت 0.68 ما عدا الكهرماني والنحاسي الترابيين)،
        // وليلاً أساس فاتح حيّ اللون لا رمادي (≥0.3 — الباتيستة الخالصة أقل منه)
        val saturation: (Color) -> Float = { c ->
            val max = maxOf(c.red, c.green, c.blue)
            val min = minOf(c.red, c.green, c.blue)
            if (max == 0f) 0f else (max - min) / max
        }
        AccentPalette.All.drop(10).forEach { bold ->
            assertTrue(
                "النهاري للجريء ${bold.id} باهت: تشبعه ${saturation(bold.day.primary)}",
                saturation(bold.day.primary) >= 0.7f,
            )
            assertTrue(
                "الليلي للجريء ${bold.id} رمادي: تشبعه ${saturation(bold.night.primary)}",
                saturation(bold.night.primary) >= 0.3f,
            )
        }
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
