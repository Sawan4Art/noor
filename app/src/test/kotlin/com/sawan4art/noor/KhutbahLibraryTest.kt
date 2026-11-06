package com.sawan4art.noor

import com.sawan4art.noor.domain.khutbah.KhutbahLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات مكتبة خطب الجمعة الجاهزة (alpha54) — توجيه المالك:
 * «اكبر كميه من الخطب الجاهزه و يكونوا مرتبين» — عقد الكمية
 * والترتيب وسلامة التركيب (افتتاح وخاتمة وشواهد موثقة).
 */
class KhutbahLibraryTest {

    @Test
    fun `الخطبات أربعة وعشرون مرتبة بمعرفات متتالية`() {
        assertEquals(24, KhutbahLibrary.ORDERED.size)
        assertEquals((1..24).toList(), KhutbahLibrary.ORDERED.map { it.id })
    }

    @Test
    fun `كل خطبة لها عنوان وفقرات غير فارغة`() {
        KhutbahLibrary.ORDERED.forEach { khutbah ->
            assertTrue("عنوان فارغ في الخطبة ${khutbah.id}", khutbah.title.isNotBlank())
            assertTrue(
                "فقرات قليلة في الخطبة ${khutbah.id}: ${khutbah.body.size}",
                khutbah.body.size >= 3,
            )
            khutbah.body.forEach { paragraph ->
                assertTrue(
                    "فقرة فارغة في الخطبة ${khutbah.id}",
                    paragraph.isNotBlank(),
                )
            }
        }
    }

    @Test
    fun `كل خطبة تحمل شاهدا موثقا من القرآن أو السنة`() {
        KhutbahLibrary.ORDERED.forEach { khutbah ->
            val full = khutbah.body.joinToString(" ")
            val hasQuran = full.contains("﴿") && full.contains("﴾")
            val hasHadith = full.contains("رواه")
            assertTrue(
                "الخطبة ${khutbah.id} بلا شاهد موثق",
                hasQuran || hasHadith,
            )
        }
    }

    @Test
    fun `النص الكامل يفتتح بالتشهد ويختم بالاستغفار`() {
        val khutbah = KhutbahLibrary.byId(8)!!
        val full = KhutbahLibrary.fullText(khutbah)
        assertTrue(full.startsWith(KhutbahLibrary.OPENING))
        assertTrue(full.trimEnd().endsWith(KhutbahLibrary.CLOSING))
        // الترتيب: الافتتاح ثم الفقرات ثم الخاتمة
        assertTrue(full.indexOf(KhutbahLibrary.OPENING) < full.indexOf(khutbah.body.first()))
        assertTrue(full.indexOf(KhutbahLibrary.CLOSING) > full.indexOf(khutbah.body.last()))
    }

    @Test
    fun `الخطبة بالمعرف تعيد المطلوب والغريب يعيد لا شيء`() {
        assertEquals("تقوى الله أوجز الوصايا", KhutbahLibrary.byId(1)?.title)
        assertEquals("الصلاة على النبي ﷺ", KhutbahLibrary.byId(24)?.title)
        assertEquals(null, KhutbahLibrary.byId(0))
        assertEquals(null, KhutbahLibrary.byId(25))
    }

    @Test
    fun `الخطبة المخصصة للجمعة بين العناوين والعناوين فريدة`() {
        val titles = KhutbahLibrary.ORDERED.map { it.title }
        assertEquals(titles.size, titles.toSet().size)
        assertTrue(titles.any { it.contains("الجمعة") })
    }
}
