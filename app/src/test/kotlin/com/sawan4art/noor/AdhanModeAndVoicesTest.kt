package com.sawan4art.noor

import com.sawan4art.noor.domain.adhan.AdhanMode
import com.sawan4art.noor.domain.adhan.AdhanVoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات موحّد وضع الصوت (alpha60) — بلاغ المالك: «صوت الاقامه لا
 * يعمل بل بالعكس يتم تشغيل صوت من اصوات الاذان» — الجذر أن نية
 * المستقبِل كانت تحمل «IQAMAH» بحروف كبيرة وعقد الخدمة «iqamah»
 * بالصغيرة فسقطت المقارنة دائماً على الأذان. والعقد الآن: أي نصٍّ
 * يعود وضعاً متعارفاً عليه.
 */
class AdhanModeTest {

    @Test
    fun `الوضعان المتعارف عليهما بالصغيرة`() {
        assertEquals("adhan", AdhanMode.ADHAN)
        assertEquals("iqamah", AdhanMode.IQAMAH)
    }

    @Test
    fun `اسم العَلَم بحروفه الكبيرة يعود إقامة`() {
        // هذا عين ما كان يرسله المستقبِل فكان يُقرأ أذاناً
        assertEquals("iqamah", AdhanMode.canonical("IQAMAH"))
        assertEquals("iqamah", AdhanMode.canonical("iqamah"))
        assertEquals("iqamah", AdhanMode.canonical("Iqamah"))
    }

    @Test
    fun `الأذان وأي غريب يعودان أذاناً`() {
        assertEquals("adhan", AdhanMode.canonical("ADHAN"))
        assertEquals("adhan", AdhanMode.canonical("adhan"))
        assertEquals("adhan", AdhanMode.canonical(null))
        assertEquals("adhan", AdhanMode.canonical(""))
        assertEquals("adhan", AdhanMode.canonical("ghost"))
    }

    @Test
    fun `عقد أصوات الأذان والإقامة — أفضل ثلاثة وأقابلها ثلاثة`() {
        // توجيه المالك alpha60: «عايز افضل ثلاث اصوات للاذان و مقابلهم
        // افضل ثلاث اصوات للاقامه بدل الخمسه»
        assertEquals(3, AdhanVoices.ADHAN.size)
        assertEquals(3, AdhanVoices.IQAMAH.size)
        // الأفضل الثلاثة باقية بالأسماء المعروفة
        assertEquals(
            listOf("main", "fakhry", "aqsa"),
            AdhanVoices.ADHAN.map { it.id },
        )
        assertEquals(
            listOf("makkah", "haram", "beautiful"),
            AdhanVoices.IQAMAH.map { it.id },
        )
        // ومعرّفات النسخ السابقة المحذوفة تسير بأمان إلى أول قائمتها
        assertEquals("main", AdhanVoices.adhanById("traditional").id)
        assertEquals("main", AdhanVoices.adhanById("beautiful").id)
        assertEquals("makkah", AdhanVoices.iqamahById("pure").id)
        assertEquals("makkah", AdhanVoices.iqamahById("soft").id)
        // والمعرّفات الستة فريدة داخل كل قائمة
        assertEquals(3, AdhanVoices.ADHAN.map { it.id }.toSet().size)
        assertEquals(3, AdhanVoices.IQAMAH.map { it.id }.toSet().size)
        // وكل مدمجٍ له اسمٌ وعنوان غير فارغين
        assertTrue(
            (AdhanVoices.ADHAN + AdhanVoices.IQAMAH).all {
                it.name.isNotBlank() && it.subtitle.isNotBlank()
            },
        )
    }
}
