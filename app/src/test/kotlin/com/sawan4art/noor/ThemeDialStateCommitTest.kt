package com.sawan4art.noor

import com.sawan4art.noor.ui.theme.ThemeDialState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد قاتل الومضة (بلاغ المالك alpha12: «ومضة بتحصل بعد ما الوضع يتفعل سواء
 * المظلم أو الفاتح»):
 *
 * 1) نهاية الرحلة تطلب التثبيت لكنها **لا تُسقط** غطاء المعاينة — الغطاء صورة
 *    كاملة للثيم الجديد ويبقى معلقاً حتى يلتقط الثيم المثبت هدفه (وصوله عبر
 *    DataStore متأخر إطاراً أو أكثر) ثم يُسقط بـ dropPreview في اللحظة نفسها.
 *    الإسقاط الفوري القديم كان يكشف الثيم القديم خلال الفجوة = الومضة.
 * 2) مهلة أمان قصيرة تُسقط الغطاء لو تأخر الوصول عنها كي لا يتجمّد المزلاج.
 * 3) أي تفاعل جديد يُبطل مهلة رحلة أقدم (جيل الرحلة) كي لا تُسقط غطاءً هو
 *    لرحلة جديدة كلياً.
 * 4) الإحباط (مغادرة الرئيسية) أثناء الانتظار ينظّف كل شيء بأمان.
 *
 * نطاق الاختبار يبتلع أخطاء الأنيميشن (لا ساعة إطارات في اختبار الوحدة) —
 * كل نقاط العقد تحت الاختبار delay/snapTo لا تتأثر بحركة الإطارات.
 */
class ThemeDialStateCommitTest {

    private fun newDial(): ThemeDialState = ThemeDialState().apply {
        placeTrack(0f, 100f, 1080f, 46f)
        configure(1080f, 2000f, 40f, 64f)
    }

    private fun scope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> },
    )

    @Test
    fun `نهاية الرحلة تطلب التثبيت ولا تُسقط الغطاء قبل وصول الثيم المثبت`() = runBlocking {
        val dial = newDial()
        val scope = scope()
        dial.syncTo(true, scope) // مظلم مثبت — أول قياس
        var requested: Boolean? = null
        dial.toggle(true, { requested = it }, scope) // نقرة → هدف فاتح
        delay(ThemeDialState.SWEEP_MS + 120L) // الرحلة انتهت والتثبيت طُلب
        assertTrue("طلب التثبيت صدر فوراً عند نهاية الرحلة", requested == true)
        assertTrue(
            "الغطاء يبقى معلقاً حتى وصول الثيم المثبت — عقد الومضة",
            dial.splitActive,
        )
        assertTrue("المزلاج يبقى مشغولاً خلال انتظار الوصول", dial.busy)
        dial.dropPreview() // الثيم المثبت وصل — الإسقاط الآمن في اللحظة نفسها
        assertFalse("الغطاء سقط بعد الوصول", dial.splitActive)
        assertFalse("المزلاج حرّ بعد الإسقاط", dial.busy)
    }

    @Test
    fun `مهلة الأمان تُسقط الغطاء لو تأخر الثيم المثبت عنها`() = runBlocking {
        val dial = newDial()
        val scope = scope()
        dial.syncTo(false, scope) // فاتح مثبت
        dial.toggle(false, { }, scope) // نقرة → هدف مظلم
        delay(
            ThemeDialState.SWEEP_MS + 40L +
                ThemeDialState.COMMIT_LANDING_GRACE_MS + 150L,
        )
        assertFalse("شبكة الأمان أسقطت الغطاء", dial.splitActive)
        assertFalse("المزلاج حرّ بعد شبكة الأمان — لا تجميد أبداً", dial.busy)
    }

    @Test
    fun `مهلة الرحلة القديمة لا تُسقط غطاء رحلة أحدث`() = runBlocking {
        val dial = newDial()
        val scope = scope()
        dial.syncTo(true, scope)
        dial.toggle(true, { }, scope) // رحلة ١ — هدف فاتح
        delay(ThemeDialState.SWEEP_MS + 60L)
        dial.dropPreview() // الثيم وصل بسرعة والغطاء سقط
        delay(200L)
        dial.toggle(false, { }, scope) // رحلة ٢ جديدة كلياً — هدف مظلم
        delay(ThemeDialState.COMMIT_LANDING_GRACE_MS) // مهلة الرحلة ١ تنفخ الآن
        assertTrue("غطاء الرحلة الجديدة لم يُمس بمهلة القديمة", dial.splitActive)
        assertTrue(dial.busy)
        dial.dropPreview()
        assertFalse(dial.splitActive)
    }

    @Test
    fun `مغادرة الرئيسية أثناء الانتظار تُحبط كل شيء بأمان`() = runBlocking {
        val dial = newDial()
        val scope = scope()
        dial.syncTo(true, scope)
        dial.toggle(true, { }, scope)
        delay(ThemeDialState.SWEEP_MS + 60L)
        dial.abort(true)
        assertFalse(dial.splitActive)
        assertFalse(dial.busy)
        delay(ThemeDialState.COMMIT_LANDING_GRACE_MS + 100L) // مهلة قديمة لا تعيد شيئاً
        assertFalse("الإحباط نهائي — لا عودة للغطاء", dial.splitActive)
    }
}
