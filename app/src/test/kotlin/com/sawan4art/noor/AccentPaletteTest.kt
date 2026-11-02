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
 * عقد لوحة الألوان (alpha32 توجيه المالك: «امسح اللون الابيض من قائمه
 * الالوان و واضف الوان جديده جريئه و مختلفه عن الالوان الموجوده و خلي
 * مجموع الالوان كلها ٢٠ لون») — عشرون لوناً متمايزاً: تسعة عشر لوناً
 * عائلياً بتباعد واضح (لا زوج يكاد يُرى الفرق بينه) + الأسود المحايد.
 */
class AccentPaletteTest {

    /** ألوان ألفا المزالة — لا يجوز أن تعود، ومستخدموها ينتقلون للافتراضي */
    private val removedGhostIds =
        listOf(
            "electric", "royal", "crimson", "copper", "lime", "fuchsia",
            "hotpink", "vividpurple", "solar", "navy",
            "white", // الأبيض أُزيل بأمر المالك alpha32
        )

    @Test
    fun `اللوحة عشرون لونا بالضبط — بتسعة ألوان جريئة جديدة`() {
        assertEquals(20, AccentPalette.All.size)
        assertEquals(
            listOf(
                "emerald", "grass", "chartreuse", "olive", "amber", "coffee",
                "blaze", "scarlet", "ruby", "rose", "magenta", "radiant",
                "violet", "ultraviolet", "indigo", "cobalt", "sky", "cyan",
                "turquoise", "black",
            ),
            AccentPalette.All.map { it.id },
        )
        assertEquals(
            listOf(
                "زمردي", "أخضر عشبي", "لايم", "زيتوني", "كهرماني", "بندقي",
                "ناري", "قرمزي", "عنابي", "واردي", "ماجنتا", "أرجواني",
                "بنفسجي", "أزرق بنفسجي", "نيلي", "كوبالت", "سماوي", "سيان",
                "تركوازي", "أسود",
            ),
            AccentPalette.All.map { it.arabicName },
        )
    }

    @Test
    fun `الالوان المزالة زالت كلها — ومنها الأبيض بأمر المالك`() {
        removedGhostIds.forEach { ghost ->
            assertTrue(
                "اللون المزال $ghost ما زال في اللوحة!",
                AccentPalette.All.none { it.id == ghost },
            )
        }
    }

    @Test
    fun `الجريئة التسع الجديدة موجودة بأسمائها`() {
        listOf(
            "grass" to "أخضر عشبي",
            "chartreuse" to "لايم",
            "coffee" to "بندقي",
            "scarlet" to "قرمزي",
            "magenta" to "ماجنتا",
            "radiant" to "أرجواني",
            "ultraviolet" to "أزرق بنفسجي",
            "cobalt" to "كوبالت",
            "cyan" to "سيان",
        ).forEach { (id, name) ->
            val accent = AccentPalette.byId(id)
            assertEquals("اسم اللون الجديد $id", name, accent.arabicName)
            assertNotEquals("اللون الجديد $id غير موجود!", AccentPalette.default, accent)
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
    fun `الالوان العائلية التسعة عشر متباعدة نهارا — لا زوج يكاد يرى الفرق بينه`() {
        val colored = AccentPalette.All.dropLast(1) // الأسود محايد خارج الفحص
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
    fun `الالوان العائلية التسعة عشر متباعدة ليلا أيضا`() {
        val colored = AccentPalette.All.dropLast(1)
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
    fun `الجريئة تشبع عال نهارا وحية ليلا`() {
        listOf("chartreuse", "scarlet", "magenta", "cobalt", "cyan", "blaze", "turquoise").forEach { id ->
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
    fun `الأسود محايد بلا تشبع يذكر في الوضعين — والأبيض لم يعد موجودا`() {
        val black = AccentPalette.byId("black")
        listOf(black.day.primary, black.night.primary).forEach { c ->
            assertTrue("المحايد الأسود مشبع؟ ${toHsv(c).second}", toHsv(c).second <= 0.2f)
        }
        assertEquals(null, AccentPalette.All.firstOrNull { it.id == "white" })
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
        // ألوان ألفا المزالة — مستخدموها القديمون ينتقلون للزمردي سالمين
        removedGhostIds.forEach { ghost ->
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
