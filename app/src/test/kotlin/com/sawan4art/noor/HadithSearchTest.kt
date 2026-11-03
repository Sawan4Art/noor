package com.sawan4art.noor

import com.sawan4art.noor.domain.hadith.HadithSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود مكتبة الأحاديث النقية — المُطبِّع وبانِد الاستعلام:
 *
 * **توافق التطبيع**: HadithNormalizationFixtures مولَّد آلياً من
 * scripts/build_hadith_db.py (مولّد الفهرس نفسه) — ٣١٧ زوجاً من نصوص
 * الكوربس الحقيقية وحالات مصاغة يدوياً؛ أي انحراف في HadithSearch.normalize
 * عن منطق المولّد يفشل البناء فوراً كي لا يصمت البحث عن المستخدم.
 */
class HadithSearchTest {

    @Test
    fun `normalization matches the corpus fixtures exactly`() {
        assertTrue(
            "عدد العقود يجب أن يبقى مئات — ملف مولّد آلياً",
            HadithNormalizationFixtures.size >= 300,
        )
        HadithNormalizationFixtures.forEach { (raw, expected) ->
            assertEquals(
                "انحراف في تطبيع: $raw",
                expected,
                HadithSearch.normalize(raw),
            )
        }
    }

    @Test
    fun `normalization is idempotent`() {
        HadithNormalizationFixtures.forEach { (raw, _) ->
            val once = HadithSearch.normalize(raw)
            assertEquals(once, HadithSearch.normalize(once))
        }
    }

    @Test
    fun `diacritics hamza forms taa marbuta and alef maqsura unify`() {
        // التشكيل يسقط — الصورتان تلتقيان في مكان واحد
        assertEquals(
            HadithSearch.normalize("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ"),
            HadithSearch.normalize("انما الاعمال بالنيات"),
        )
        // الهمزات كلها ألف
        assertEquals("الايمه", HadithSearch.normalize("الأَئِمَّةُ"))
        assertEquals("الامام", HadithSearch.normalize("الإِمَامُ"))
        assertEquals("امن", HadithSearch.normalize("آمَنَّ"))
        assertEquals("احمد", HadithSearch.normalize("أَحْمَدُ"))
        // التاء المربوطة هاء والألف المقصورة ياء — والهمزة المفردة ثابتة
        assertEquals("صلاه", HadithSearch.normalize("صَلَاةٌ"))
        assertEquals("علي", HadithSearch.normalize("عَلَىٰ"))
        assertEquals("شيء", HadithSearch.normalize("شَيْءٌ"))
        // التطويل وألف الخنجرية
        assertEquals("كتب", HadithSearch.normalize("كِتَـٰبٌ"))
        assertEquals("حسبنا", HadithSearch.normalize("حَسْــبُــنَا"))
    }

    @Test
    fun `blessings and basmala expand via nfkc`() {
        assertEquals("قال رسول صلي الله عليه وسلم", HadithSearch.normalize("قَالَ رَسُولُ ﷺ"))
        assertEquals("بسم الله الرحمن الرحيم", HadithSearch.normalize("﷽"))
        assertEquals("الله", HadithSearch.normalize("ﷲ"))
    }

    @Test
    fun `arabic and persian digits unify to latin`() {
        assertEquals("0123", HadithSearch.normalize("٠١٢٣"))
        assertEquals("0123", HadithSearch.normalize("۰۱۲۳"))
        assertEquals("حديث 40", HadithSearch.normalize("حديث ٤٠"))
    }

    @Test
    fun `direction and zero width marks vanish`() {
        assertEquals("بسم الله", HadithSearch.normalize("بِسْمِ\u200F اللَّهِ"))
        assertEquals("بسم الله", HadithSearch.normalize("\u200Eبسم\u200B الله\uFEFF"))
        assertEquals("قال", HadithSearch.normalize("\u202Bقال\u202C"))
    }

    @Test
    fun `match expression builds prefix phrases for every word`() {
        assertEquals("\"الصلاه\"*", HadithSearch.matchExpression("الصَّلَاةَ"))
        // كلمتان فأكثر: العبارة الحرفية أولاً ثم الكلمات المتفرقة ببادئات
        assertEquals(
            "(\"قل الحمد\"*) OR (\"قل\"* \"الحمد\"*)",
            HadithSearch.matchExpression("قل الحمد"),
        )
        // التطبيع يمر عبر التعبير — أي صورة للكلمة تصير نفس البادئة
        assertEquals(
            HadithSearch.matchExpression("الأئمة"),
            HadithSearch.matchExpression("الائمة"),
        )
        // بادئة: الاستعلام القصير يصيد الكلمات الممتدة
        assertEquals(
            "(\"الطهور شطر\"*) OR (\"الطهور\"* \"شطر\"*)",
            HadithSearch.matchExpression("الطُّهُور شطر"),
        )
    }

    @Test
    fun `match expression returns null for empty or marks only`() {
        assertNull(HadithSearch.matchExpression(""))
        assertNull(HadithSearch.matchExpression("   "))
        assertNull(HadithSearch.matchExpression("، ؟ ! ــ"))
        assertNull(HadithSearch.matchExpression("\u064B\u064C")) // تشكيل فقط
    }

    @Test
    fun `match expression survives fts special characters`() {
        // علامات FTS5 الخاصة لا تتسرب إلى التعبير — كل كلمة داخل اقتباس
        assertEquals("\"الحديث\"*", HadithSearch.matchExpression("الحديث\""))
        assertEquals(
            "(\"حديث نبوي\"*) OR (\"حديث\"* \"نبوي\"*)",
            HadithSearch.matchExpression("حديث* نبوي?"),
        )
        // «NEAR» كلمة نصية مقتبسة لا صيغة جوار — والعبارات الحرفية تظهر
        val nearExpr = HadithSearch.matchExpression("عشرة NEAR")
        assertTrue(nearExpr.orEmpty().contains(" OR "))
        assertTrue(nearExpr.orEmpty().contains("\"NEAR\"*"))
        assertFalse(nearExpr.orEmpty().contains(" NEAR ")) // لا «NEAR» عارية بين مسافات
        // «OR» كلمة نصية مقتبسة حرفياً في الفرعين — لا مؤثِّر جوار كصيغة
        assertEquals(
            "(\"حديث OR\"*) OR (\"حديث\"* \"OR\"*)",
            HadithSearch.matchExpression("حديث OR"),
        )
    }

    @Test
    fun `pure number catches plain digits and arabic digits`() {
        assertEquals(5L, HadithSearch.pureNumber("٥"))
        assertEquals(7563L, HadithSearch.pureNumber("7563"))
        assertEquals(40L, HadithSearch.pureNumber("٤٠"))
        assertEquals(12L, HadithSearch.pureNumber(" 12 "))
    }

    @Test
    fun `pure number strips polite words like hadith and raqm`() {
        assertEquals(5L, HadithSearch.pureNumber("حديث ٥"))
        assertEquals(14L, HadithSearch.pureNumber("الحديث ١٤"))
        assertEquals(1200L, HadithSearch.pureNumber("رقم 1200"))
        assertEquals(7L, HadithSearch.pureNumber("الرقم ٧"))
    }

    @Test
    fun `pure number rejects mixed words long numbers and empty`() {
        assertNull(HadithSearch.pureNumber(""))
        assertNull(HadithSearch.pureNumber("   "))
        assertNull(HadithSearch.pureNumber("انما الاعمال"))
        assertNull(HadithSearch.pureNumber("12345")) // خمسة أرقام — فوق أكبر رقم في الكتب
        assertNull(HadithSearch.pureNumber("١٢ سنة")) // رقم مع كلمة غير عطف
        assertNull(HadithSearch.pureNumber("حديث")) // كلمة العطف وحدها ليست رقماً
        assertNull(HadithSearch.pureNumber("12a"))
    }
}
