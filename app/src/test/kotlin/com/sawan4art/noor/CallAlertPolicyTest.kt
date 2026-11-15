package com.sawan4art.noor

import com.sawan4art.noor.alarm.CallAlertPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود حاكم التنبيهات وقت المكالمات — بلاغ المالك (2026-11-15):
 * «لما يجي وقت النبيه سواء الاذن او غيره اثناء وجود مكالمه لا يصدر صوت
 * يكتفي فقط باشعار على الشاشه بانه حان وقت الاذن ودي في حاله الاذان و
 * الاقامه اما باقي التنبيهات يصدر رنين التنبيه بعد انتهاء المكالمه ب ٥
 * ثواني كده اللي كأن التنبيه حصل لي غفوة ... اذا مضى وقت الاذان فهو مضى
 * لا يؤذن مره اخرى و يكتفي فقط باشعار في الشاشه يفيد بأنه حان وقت الصلاه
 * و يعرض المتبقي على الاقامه»
 */
class CallAlertPolicyTest {

    private val now: Long = 1_700_000_000_000L

    /* ============ قرار لحظة الدقّ ============ */

    @Test
    fun `لا مكالمة — الأذان والإقامة والتنبيهات كلها ترن الآن`() {
        assertEquals(CallAlertPolicy.Decision.SoundNow, CallAlertPolicy.decide(CallAlertPolicy.AlertClass.PRAYER, false, now))
        assertEquals(CallAlertPolicy.Decision.SoundNow, CallAlertPolicy.decide(CallAlertPolicy.AlertClass.GENERIC, false, now))
    }

    @Test
    fun `أذان في مكالمة — إشعار صامت فقط`() {
        assertEquals(CallAlertPolicy.Decision.SilentNotice, CallAlertPolicy.decide(CallAlertPolicy.AlertClass.PRAYER, true, now))
    }

    @Test
    fun `الإقامة في مكالمة — إشعار صامت فقط ففئتها صلاة مقدسة`() {
        assertEquals(CallAlertPolicy.Decision.SilentNotice, CallAlertPolicy.decide(CallAlertPolicy.AlertClass.PRAYER, true, now + 1))
    }

    @Test
    fun `تنبيه عام في مكالمة — غفوة بنبضة فحص بعد أربع ثوانٍ`() {
        val decision = CallAlertPolicy.decide(CallAlertPolicy.AlertClass.GENERIC, true, now)
        assertEquals(CallAlertPolicy.Decision.Defer(now + CallAlertPolicy.RECHECK_DELAY_MILLIS), decision)
    }

    /** «اذا مضى وقت الاذان فهو مضى لا يؤذن مره اخرى» — الأذان لا يُؤجَّل أبداً */
    @Test
    fun `عقد الأذان المقدس — في المكالمة لا صوتاً ولا غفوةً ولا إعادة`() {
        for (inCall in listOf(true, false)) {
            val decision = CallAlertPolicy.decide(CallAlertPolicy.AlertClass.PRAYER, inCall, now)
            assertTrue(decision !is CallAlertPolicy.Decision.Defer)
            if (inCall) {
                assertEquals(CallAlertPolicy.Decision.SilentNotice, decision)
            } else {
                assertEquals(CallAlertPolicy.Decision.SoundNow, decision)
            }
        }
    }

    /** التنبيهات العامة لا تصمت أبداً — فالغفوة لا إلغاء: ترنّ بعد المكالمة */
    @Test
    fun `عقد التنبيه العام — لا صمتاً نهائياً بل غفوةً ثم رنّ`() {
        val decision = CallAlertPolicy.decide(CallAlertPolicy.AlertClass.GENERIC, true, now)
        assertTrue(decision !is CallAlertPolicy.Decision.SilentNotice)
    }

    /* ============ نبضات الفحص أثناء الغفوة ============ */

    @Test
    fun `الفحص والمكالمة باقية — انتظارٌ لنبضة أخرى`() {
        val result = CallAlertPolicy.recheck(inCall = true, nowMillis = now)
        assertEquals(CallAlertPolicy.Recheck.KeepWaiting(now + CallAlertPolicy.RECHECK_DELAY_MILLIS), result)
    }

    @Test
    fun `الفحص وانتهت المكالمة — رنينٌ بعد خمس ثوانٍ بالضبط`() {
        val result = CallAlertPolicy.recheck(inCall = false, nowMillis = now)
        assertEquals(CallAlertPolicy.Recheck.Ring(now + CallAlertPolicy.RING_DELAY_MILLIS), result)
    }

    @Test
    fun `ثوابت البلاغ — خمس ثوانٍ بعد الانتهاء وأربع للنبضة`() {
        assertEquals(5_000L, CallAlertPolicy.RING_DELAY_MILLIS)
        assertEquals(4_000L, CallAlertPolicy.RECHECK_DELAY_MILLIS)
    }

    /* ============ لحظة الرنين بعد المهلة ============ */

    @Test
    fun `لحظة الرنين بلا مكالمة — رنّ الآن`() {
        assertEquals(CallAlertPolicy.Decision.SoundNow, CallAlertPolicy.ringDue(false, now))
    }

    @Test
    fun `مكالمةٌ جديدة دخلت في مهلة الخمس ثوانٍ — عادت الغفوة`() {
        val decision = CallAlertPolicy.ringDue(true, now)
        assertEquals(CallAlertPolicy.Decision.Defer(now + CallAlertPolicy.RECHECK_DELAY_MILLIS), decision)
    }

    /* ============ المتبقي على الإقامة في الإشعار الصامت ============ */

    @Test
    fun `المتبقي على الإقامة ساعةٌ كاملة — نصٌّ بالأرقام الهندية`() {
        val text = CallAlertPolicy.remainingIqamahText(iqamahAtMillis = now + 3_600_000L, nowMillis = now)
        assertEquals("المتبقي على الإقامة: ٠١:٠٠:٠٠", text)
    }

    @Test
    fun `المتبقي على الإقامة دقيقتان وثلاثون ثانية`() {
        val text = CallAlertPolicy.remainingIqamahText(iqamahAtMillis = now + 152_000L, nowMillis = now)
        assertEquals("المتبقي على الإقامة: ٠٠:٠٢:٣٢", text)
    }

    @Test
    fun `المتبقي على الإقامة فات وقته — فارغٌ بلا نصّ زائف`() {
        assertEquals("", CallAlertPolicy.remainingIqamahText(iqamahAtMillis = now, nowMillis = now))
        assertEquals("", CallAlertPolicy.remainingIqamahText(iqamahAtMillis = now - 60_000L, nowMillis = now))
    }
}
