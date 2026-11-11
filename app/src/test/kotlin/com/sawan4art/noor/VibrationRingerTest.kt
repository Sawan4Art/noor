package com.sawan4art.noor

import com.sawan4art.noor.data.AdhanPrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات اهتزاز أثناء الرنين — بلاغ المالك v1.71: «اضف ميزه الاهتزاز
 * اثنار الرنين لكل الاصوات الصادره من التطبيق و يكون كل صوت قابل
 * لتفعيل الميزه او ايقافها» — عقد المفاتيح الثلاثة المستقلة والافتراضي
 * المفعّل وسقوط القراءة الآمنة.
 */
class VibrationRingerTest {

    @Test
    fun `الافتراضي مفعّل للأصوات الثلاثة`() {
        val prefs = AdhanPrefs()
        assertTrue(prefs.adhanVibrate)
        assertTrue(prefs.iqamahVibrate)
        assertTrue(prefs.reminderVibrate)
    }

    @Test
    fun `كل صوتٍ بمفتاحه — الأذان ثم الإقامة ثم التنبيه`() {
        val prefs = AdhanPrefs(
            adhanVibrate = false,
            iqamahVibrate = true,
            reminderVibrate = false,
        )
        assertFalse("الأذان بمفتاحه", prefs.vibrateFor(isReminder = false, isIqamah = false))
        assertTrue("الإقامة بمفتاحها", prefs.vibrateFor(isReminder = false, isIqamah = true))
        assertFalse("التنبيه بمفتاحه", prefs.vibrateFor(isReminder = true, isIqamah = false))
    }

    @Test
    fun `إطفاء الأذان لا يمس الإقامة والتنبيه`() {
        val prefs = AdhanPrefs(adhanVibrate = false)
        assertFalse(prefs.vibrateFor(false, false))
        assertTrue(prefs.vibrateFor(false, true))
        assertTrue(prefs.vibrateFor(true, false))
    }
}
