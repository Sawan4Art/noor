package com.sawan4art.noor

import com.sawan4art.noor.widget.COUNTDOWN_FORMAT
import com.sawan4art.noor.widget.chronometerBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد العدّاد الحي في الويدجت (alpha58 — بلاغ المالك: «الويدجت لا تعمل
 * في تظهر في التطبيقات المصغره لاكن لا تعمل»):
 *
 * الجذر المُصلَح: الرسم كان يضبط قاعدة Chronometer بـ setBase وحدها
 * فلا يُستدعى start() إطلاقاً — فبقي العدّاد واقفاً على ٠٠:٠٠ والويدجت
 * تبدو ميتة. العقد الآن: صيغة العدّاد واحدةٌ ثابتة تحمل العنصر %s
 * (تُمرَّر مع منادى started=true في setChronometer)، والقاعدة «زمن
 * الإقلاع + المتبقي» فتعدّ ثانيةً بثانية تنازلياً مع `android:countDown`.
 */
class WidgetCountdownTest {

    @Test
    fun `الصيغة تحمل مقام القيمة`() {
        assertTrue(COUNTDOWN_FORMAT.contains("%s"))
        assertTrue(COUNTDOWN_FORMAT.startsWith("متبقي"))
    }

    @Test
    fun `القاعدة زمن الإقلاع زائد المتبقي`() {
        assertEquals(1_000_000L + 37_000L, chronometerBase(37, 1_000_000L))
        assertEquals(500L, chronometerBase(0, 500L))
    }

    @Test
    fun `المتبقي السالب لا يرجع القاعدة للخلف`() {
        // لو فات الأذان والحساب جاء بسالبٍ فالقاعدة زمن الإقلاع نفسه
        assertEquals(900_000L, chronometerBase(-5, 900_000L))
    }
}
