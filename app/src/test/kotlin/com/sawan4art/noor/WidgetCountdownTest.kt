package com.sawan4art.noor

import com.sawan4art.noor.widget.COUNTDOWN_FORMAT
import com.sawan4art.noor.widget.WidgetRenderer
import com.sawan4art.noor.widget.chronometerBase
import com.sawan4art.noor.widget.countdownLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد العدّاد الحي في الويدجت (alpha58 ثم alpha64 — بلاغ المالك عبر
 * صورتَي شاشته ١٠/١١: العدّاد يظهر أرقاماً عارية «٩:٠٦:٥٨» بلا كلماتٍ
 * وإلى جانبها «الفجر» فيُقرأ كوقت الصلاة):
 *
 * الجذر الميداني: بيئة سامسونج تُسقط صيغة Chronometer داخل RemoteViews
 * فتبقى الأرقام وحدها. العقد الآن منقسم:
 *  - الكلمات («متبقي حتى أذان الفجر») TextView مستقل بالمنادى setText
 *    الذي ينجح ميدانياً في كل عناصر الويدجت، ويحمل اسم الصلاة القادمة.
 *  - صيغة العدّاد أرقامٌ خالصة «%s» لا شيء غيرها.
 *  - والقاعدة «زمن الإقلاع + المتبقي» تُمرَّر بمنادى started=true
 *    (عقد alpha58) فتعدّ ثانيةً بثانية تنازلياً مع `android:countDown`.
 */
class WidgetCountdownTest {

    @Test
    fun `الصيغة أرقام خالصة بلا أي كلمة`() {
        assertEquals("%s", COUNTDOWN_FORMAT)
    }

    @Test
    fun `الجملة تحمل اسم الصلاة القادمة`() {
        assertEquals("متبقي حتى أذان الفجر", countdownLabel("الفجر"))
        assertEquals("متبقي حتى أذان الجمعة", countdownLabel("الجمعة"))
        assertEquals("متبقي حتى أذان العشاء", countdownLabel("العشاء"))
    }

    @Test
    fun `الاسم الغائب يعود للصياغة العامة`() {
        assertEquals("متبقي حتى الأذان", countdownLabel(""))
        assertEquals("متبقي حتى الأذان", countdownLabel("   "))
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

    @Test
    fun `التحديثان المتتاليان في دقيقتين يتبادلان التوأم`() {
        // دقيقة واحدة = ٦٠٠٠٠ ملي ثانية — فهرس الدقيقة الزوجي أصليّ والفردي بديل
        val evenMinute = 4L * 60_000L
        val oddMinute = evenMinute + 60_000L
        assertEquals(false, WidgetRenderer.useAltMainLayout(evenMinute))
        assertEquals(true, WidgetRenderer.useAltMainLayout(oddMinute))
        assertNotEquals(
            WidgetRenderer.useAltMainLayout(evenMinute),
            WidgetRenderer.useAltMainLayout(oddMinute),
        )
        // وأي لحظةٍ داخل الدقيقة نفسها تقرر القرار نفسه فلا رقصة
        assertEquals(
            WidgetRenderer.useAltMainLayout(evenMinute),
            WidgetRenderer.useAltMainLayout(evenMinute + 59_000L),
        )
    }

    @Test
    fun `التبادل يشمل الدقيقة صفر واللحظات الأولى للنظام`() {
        // الدقيقة صفر (فهرس زوجي) = التوأم الأصلي، وأول ثانية في الدقيقة الأولى = البديل
        assertEquals(false, WidgetRenderer.useAltMainLayout(0L))
        assertEquals(false, WidgetRenderer.useAltMainLayout(59_999L))
        assertEquals(true, WidgetRenderer.useAltMainLayout(60_000L))
        assertEquals(true, WidgetRenderer.useAltMainLayout(1L * 119_999L))
    }
}
