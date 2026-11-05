package com.sawan4art.noor

import com.sawan4art.noor.domain.quran.MushafLine
import com.sawan4art.noor.domain.quran.MushafMath
import com.sawan4art.noor.ui.quran.TajweedPalette
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات المصحف — الأصول المبنية سلفاً (طبعة المدينة بتلوين التجويد):
 * بنية ملف الأصول، وحسابات الرقم العالمي والأجزاء، ولوحة الألوان
 * (توجيه المالك 2026-11-06: «ابدا شغل في المصحف… عايز زيها»).
 */
class MushafAssetTest {

    private val asset: File by lazy {
        val candidates = listOf(
            File("src/main/assets/quran_mushaf.json"),
            File("app/src/main/assets/quran_mushaf.json"),
        )
        candidates.firstOrNull { it.isFile }
            ?: error("أصول المصحف غير موجودة عند: $candidates")
    }

    @Test
    fun `أصول المصحف موجودة بحجم معقول`() {
        assertTrue("حجم الأصول صغير جداً — الملف ناقص", asset.length() > 3_000_000)
    }

    @Test
    fun `كل سورة لها عنوان واحد - ١١٤ عنواناً في الأصول`() {
        val text = asset.readText()
        // كل عنوان سورة يظهر كسطر من نوع "t":"h" — واحد لكل سورة
        val headers = Regex("\"t\":\"h\"").findAll(text).count()
        assertEquals(114, headers)
    }

    @Test
    fun `نصوص الآيات verbatim موجودة في الأصول`() {
        val text = asset.readText()
        // مفاتيح نصوص الآيات الكاملة — آية الكرسي وأول الفاتحة
        assertTrue(
            "نص آية الكرسي مفقود من الأصول",
            text.contains("\"2:255\":\""),
        )
        assertTrue(
            "نص الفاتحة مفقود — أولها البسملة",
            text.contains("\"1:1\":\"بِسْمِ"),
        )
    }

    @Test
    fun `مدى التجويد مدمج في كلمات الأصول`() {
        val text = asset.readText()
        // قواعد التجويد بأسمائها داخل مدى الكلمات
        assertTrue(text.contains("\"hamzat_wasl\""))
        assertTrue(text.contains("\"madd_2\""))
        assertTrue(text.contains("\"qalqalah\""))
        assertTrue(text.contains("\"ghunnah\""))
        assertTrue(text.contains("\"lam_shamsiyyah\""))
    }

    // ===== الحسابات =====

    @Test
    fun `الرقم العالمي لآية الكرسي مئتان واثنان وستون`() {
        val surahs = listOf(
            com.sawan4art.noor.domain.quran.MushafSurah(1, "الفاتحة", 7),
            com.sawan4art.noor.domain.quran.MushafSurah(2, "البقرة", 286),
        )
        assertEquals(1, MushafMath.globalAyah(1, 1, surahs))
        assertEquals(7, MushafMath.globalAyah(1, 7, surahs))
        assertEquals(262, MushafMath.globalAyah(2, 255, surahs))
    }

    @Test
    fun `الجزء من الصفحة - عشرون صفحة لكل جزء`() {
        assertEquals(1, MushafMath.juzOf(1))
        assertEquals(1, MushafMath.juzOf(20))
        assertEquals(2, MushafMath.juzOf(21))
        assertEquals(30, MushafMath.juzOf(604))
    }

    @Test
    fun `أول صفحة للسورة من عنوانها`() {
        val pages = listOf(
            listOf(MushafLine.Header(1, "سورة الفاتحة"), MushafLine.Text(emptyList())),
            listOf(
                MushafLine.Header(2, "سورة البقرة"),
                MushafLine.Text(emptyList()),
                MushafLine.Text(emptyList()),
            ),
            listOf(MushafLine.Text(emptyList())),
        )
        assertEquals(1, MushafMath.firstPageOf(1, pages))
        assertEquals(2, MushafMath.firstPageOf(2, pages))
    }

    // ===== لوحة ألوان التجويد =====

    @Test
    fun `قواعد التجويد بألوانها المعتمدة`() {
        val madd = TajweedPalette.colorOf("madd_6")
        val ghunnah = TajweedPalette.colorOf("ghunnah")
        val qalqalah = TajweedPalette.colorOf("qalqalah")
        assertNotNull(madd)
        assertNotNull(ghunnah)
        assertNotNull(qalqalah)
        assertTrue("المد أحمر وفق الطبعة", madd == TajweedPalette.groups[0].color)
        assertTrue("الغنة خضراء وفق الطبعة", ghunnah == TajweedPalette.groups[1].color)
        assertTrue("القلقلة برتقالية وفق الطبعة", qalqalah == TajweedPalette.groups[2].color)
    }

    @Test
    fun `قاعدة غير معروفة تبقى بلا تلوين`() {
        assertEquals(null, TajweedPalette.colorOf("قاعدة_غير_موجودة"))
    }
}
