package com.sawan4art.noor

import com.sawan4art.noor.data.update.UpdateNotifyPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * عقود سياسة تكرار إشعار النسخ الجديدة — alpha37 بلاغ المالك:
 * «الاشعار بتاع التحديث وصل مره واحده — انا عايز الاشعار ده يوصل
 * لو كتير لو المستخدم محدثش التطبيق».
 *
 * العقد: إشعار أول فوري لكل نسخة، ثم تذكير كل ١٢ ساعة للنسخة نفسها
 * ما دامت أحدث من النسخة المثبتة، وأبدأ من جديد فوراً مع أي نسخة
 * أحدث، ولا إشعار لمرشحٍ أقدم مما أُشعر عنه.
 */
class UpdateNotifyPolicyTest {

    private val hour = TimeUnit.HOURS.toMillis(1)
    private val now = 1_800_000_000_000L

    @Test
    fun `أول نسخة بلا أي إشعار سابق تُشعَر فوراً`() {
        assertTrue(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = null,
                lastNotifiedAt = 0L,
                candidateTag = "v0.5.0-alpha37",
                now = now,
            ),
        )
    }

    @Test
    fun `النسخة نفسها داخل فترة التذكير لا تُشعَر ثانية`() {
        assertFalse(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha37",
                lastNotifiedAt = now - 5 * hour,
                candidateTag = "v0.5.0-alpha37",
                now = now,
            ),
        )
    }

    @Test
    fun `النسخة نفسها بعد فترة التذكير تُشعَر من جديد ما دام لم يحدّث`() {
        // ١٣ ساعة منذ آخر إشعار — الفحص عند الفتح أو الدوري يعيد التذكير
        assertTrue(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha37",
                lastNotifiedAt = now - 13 * hour,
                candidateTag = "v0.5.0-alpha37",
                now = now,
            ),
        )
    }

    @Test
    fun `حد الفترة تماماً اثنتا عشرة ساعة يُشعِر — العقد شامل الحد`() {
        assertTrue(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha37",
                lastNotifiedAt = now - UpdateNotifyPolicy.REPEAT_INTERVAL_MS,
                candidateTag = "v0.5.0-alpha37",
                now = now,
            ),
        )
    }

    @Test
    fun `دقيقة واحدة قبل تمام الفترة لا تُشعِر`() {
        assertFalse(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha37",
                lastNotifiedAt = now - UpdateNotifyPolicy.REPEAT_INTERVAL_MS + TimeUnit.MINUTES.toMillis(1),
                candidateTag = "v0.5.0-alpha37",
                now = now,
            ),
        )
    }

    @Test
    fun `نسخة أحدث من آخر إشعار تُشعَر فوراً حتى لو التذكير لم يحن`() {
        // أُشعر عن alpha37 قبل دقيقة، وصدرت alpha38 — إشعارها فوري
        assertTrue(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha37",
                lastNotifiedAt = now - TimeUnit.MINUTES.toMillis(1),
                candidateTag = "v0.5.0-alpha38",
                now = now,
            ),
        )
    }

    @Test
    fun `مرشح أقدم من آخر إشعار لا يُشعَر أبداً`() {
        assertFalse(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha37",
                lastNotifiedAt = now - 20 * hour,
                candidateTag = "v0.5.0-alpha36",
                now = now,
            ),
        )
    }

    @Test
    fun `قفزة قناة كاملة أحدث من أي تذكير — إشعار فوري`() {
        // أُشعر عن alpha99 وصدرت 0.6.0 — الأساس الأعلى يسقط فترة الانتظار
        assertTrue(
            UpdateNotifyPolicy.shouldNotify(
                notifiedVersion = "v0.5.0-alpha99",
                lastNotifiedAt = now,
                candidateTag = "v0.6.0",
                now = now,
            ),
        )
    }
}
