package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.AccentPalette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * عقد لوحة الألوان (alpha29 توجيه المالك: «الالوان فيها متشابه كثير
 * انا عايز الوان غير متشابه .. و كمان عايز لونين ابيض و اسود دول
 * اضافيين») — اثنا عشر لوناً متمايزاً: عشرة عائلية بتباعد واضح
 * (لا زوج يكاد يُرى الفرق بينه) + محايدَي الأبيض والأسود.
 */
class AccentPaletteTest {

    private val removedSimilarIds =
        listOf("electric", "royal", "crimson", "copper", "lime", "fuchsia", "hotpink", "vividpurple", "solar", "navy")

    @Test
    fun `اللوحة اثنا عشر لونا بالضبط — عشرة عائلية والمحايدان`() {
        assertEquals(12, AccentPalette.All.size)
        assertEquals(
            listOf("emerald", "turquoise", "sky", "indigo", "violet", "rose", "ruby", "blaze", "amber", "olive", "white", "black"),
            AccentPalette.All.map { it.id },
        )
        assertEquals(
            listOf("زمردي", "تركوازي", "سماوي", "نيلي", "بنفسجي", "واردي", "عنابي", "ناري", "كهرماني", "زيتوني", "أبيض", "أسود"),
            AccentPalette.All.map { it.arabicName },
        )
    }

    @Test
    fun `الوان المتشابهة السابقة زالت كلها — لون واحد لكل عائلة درجية`() {
        removedSimilarIds.forEach { ghost ->
            assertTrue(
                "اللون المتشابه $ghost ما زال في اللوحة!",
                AccentPalette.All.none { it.id == ghost },
            )
        }
    }

    // ===== حسابات الفصل اللوني — بلا مكتبات خارجية =====

    private fun toHsv(c: Color): Triple<Float, Float, Float> {
        val max = maxOf(c.red, c.green, c.blue)
        val min = minOf(c.red, c.green, c.blue)
        val v = max
        val s = if (max == 0f) 0f else (max - min) / max
        val d = max - min
        val h = when {
            d == 0f -> 0f
            max == c.red -> 60f * (((c.green - c.blue) / d) % 6f)
            max == c.green -> 60f * (((c.blue - c.red) / d) + 2f)
            else -> 60f * (((c.red - c.green) / d) + 4f)
        }
        return Triple(if (h < 0f) h + 360f else h, s, v)
    }

    private fun hueDistance(h1: Float, h2: Float): Float {
        val d = abs(h1 - h2) % 360f
        return minOf(d, 360f - d)
    }

    /** لونان «متشابهان» إذا اقتربت صفتهما الثلاث كلها — العقد يمنع ذلك */
    private fun assertSeparated(a: Color, b: Color, names: String) {
        val (h1, s1, v1) = toHsv(a)
        val (h2, s2, v2) = toHsv(b)
        val separated = hueDistance(h1, h2) >= 15f ||
            abs(s1 - s2) >= 0.28f ||
            abs(v1 - v2) >= 0.22f
        assertTrue(
            "زوج متشابه: $names — صبغة ${hueDistance(h1, h2)}° وتشبع ${abs(s1 - s2)} وقيمة ${abs(v1 - v2)}",
            separated,
        )
    }

    @Test
    fun `الالعائلية العشرة متباعدة نهارا — لا زوج يكاد يرى الفرق بينه`() {
        val colored = AccentPalette.All.dropLast(2)
        for (i in colored.indices) {
            for (j in i + 1 until colored.size) {
                assertSeparated(
                    colored[i].day.primary, colored[j].day.primary,
                    "${colored[i].id} النهاري × ${colored[j].id}",
                )
            }
        }
    }

    @Test
    fun `الالعائلية العشرة متباعدة ليلا أيضا`() {
        val colored = AccentPalette.All.dropLast(2)
        for (i in colored.indices) {
            for (j in i + 1 until colored.size) {
                assertSeparated(
                    colored[i].night.primary, colored[j].night.primary,
                    "${colored[i].id} الليلي × ${colored[j].id}",
                )
            }
        }
    }

    @Test
    fun `الجريئة الباقية تشبع عال نهارا وحية ليلا`() {
        listOf("turquoise", "blaze").forEach { id ->
            val accent = AccentPalette.byId(id)
            assertTrue(
                "النهاري للجريء $id باهت",
                toHsv(accent.day.primary).second >= 0.7f,
            )
            assertTrue(
                "الليلي للجريء $id رمادي",
                toHsv(accent.night.primary).second >= 0.3f,
            )
        }
    }

    @Test
    fun `الابيض والاسود محايدان بلا تشبع يذكر في الوضعين`() {
        listOf("white", "black").forEach { id ->
            val accent = AccentPalette.byId(id)
            listOf(accent.day.primary, accent.night.primary).forEach { c ->
                assertTrue("المحايد $id مشبع؟ ${toHsv(c).second}", toHsv(c).second <= 0.2f)
            }
        }
    }

    // ===== تباين نصوص الأزرار =====

    private fun luminance(c: Color): Float {
        fun channel(x: Float): Float =
            if (x <= 0.04045f) x / 12.92f else Math.pow(((x + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        return 0.2126f * channel(c.red) + 0.7152f * channel(c.green) + 0.0722f * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Float {
        val l1 = maxOf(luminance(a), luminance(b))
        val l2 = minOf(luminance(a), luminance(b))
        return (l1 + 0.05f) / (l2 + 0.05f)
    }

    @Test
    fun `كل اساسي يحمل نصه بتباين مقروء في الوضعين`() {
        AccentPalette.All.forEach { accent ->
            listOf(accent.day, accent.night).forEach { set ->
                assertTrue(
                    "تباين ${accent.id} ضعيف: ${contrast(set.primary, set.onPrimary)}",
                    contrast(set.primary, set.onPrimary) >= 4.0f,
                )
            }
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
        assertEquals(0xFF2E6B5E.toInt(), first.day.primary.toArgb())
        assertEquals(0xFF82C9B0.toInt(), first.night.primary.toArgb())
    }

    @Test
    fun `الاسترجاع بالمعرف متسامح — المفقود وغريب المعرّف وألوان ألفا القديمة ترجع للافتراضي`() {
        assertSame(AccentPalette.default, AccentPalette.byId("emerald"))
        assertSame(AccentPalette.default, AccentPalette.byId("ghost-color"))
        assertSame(AccentPalette.default, AccentPalette.byId(null))
        // ألوان alpha28 المزالة — مستخدموها القديمون ينتقلون للزمردي سالمين
        removedSimilarIds.forEach { ghost ->
            assertSame(AccentPalette.default, AccentPalette.byId(ghost))
        }
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
}
