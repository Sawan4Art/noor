package com.sawan4art.noor

import com.sawan4art.noor.domain.permissions.PermissionGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات بوابة الصلاحيات (alpha57 — بلاغ المالك: «طالما الاوذنات كلها
 * تعمل لا يطلب التطبيق الاذن مره اخرى الا اذا تم الغاء اذونات منها …
 * ألا اذا اضفنا اذن اخر مستقبلا») — عقل القرار كله نقي هنا: المعطي لا
 * يُزاح حتى بعد التحديث، وأولُ مرةٍ يُطلب، والممنوح الفُقد يُطلب مرة،
 * والمرفوض بلا سابقة منح لا يُلحّ — وكان عدم الإزعاج يُطلب كل فتحٍ
 * قبل هذه البوابة.
 */
class PermissionGateTest {

    private val allGranted = PermissionGate.KEYS.associateWith { true }
    private val allDenied = PermissionGate.KEYS.associateWith { false }

    @Test
    fun `أول فتح يطلب الكل بلا استثناء`() {
        val toAsk = PermissionGate.decide(emptyMap(), allDenied)
        assertEquals(PermissionGate.KEYS, toAsk)
    }

    @Test
    fun `الممنوح كله لا يُزاح أبداً حتى لو لم تُحفظ حالته`() {
        assertTrue(PermissionGate.decide(emptyMap(), allGranted).isEmpty())
        val state = PermissionGate.KEYS.associateWith { PermissionGate.PermState(asked = true, granted = false) }
        assertTrue(PermissionGate.decide(state, allGranted).isEmpty())
    }

    @Test
    fun `بلاغ المالك نفسه — عدم الإزعاج المرفوض لا يُطلب في كل فتح`() {
        // سُئل كل شيء ورفض عدم الإزعاج بينما مُنح الباقي
        val state = mapOf(
            "location" to PermissionGate.PermState(asked = true, granted = true),
            "notifications" to PermissionGate.PermState(asked = true, granted = true),
            "exact_alarm" to PermissionGate.PermState(asked = true, granted = true),
            "full_screen" to PermissionGate.PermState(asked = true, granted = true),
            "dnd" to PermissionGate.PermState(asked = true, granted = false),
        )
        val now = allGranted + ("dnd" to false)
        // فتحات لا حصر لها — القرار ثابت: لا شيء يُطلب
        repeat(10) {
            assertTrue(PermissionGate.decide(state, now).isEmpty())
        }
    }

    @Test
    fun `الممنوح الذي فُقد يُطلب مرة واحدة ثم يسكت إن رُفض`() {
        val state = mapOf(
            "dnd" to PermissionGate.PermState(asked = true, granted = true),
        )
        val now = allGranted + ("dnd" to false)
        assertEquals(listOf("dnd"), PermissionGate.decide(state, now))
        // سُئل بعد الفقد ولم يُمنح — لا إلحاح بعده
        val after = PermissionGate.markAsked(state, listOf("dnd"), now)
        assertTrue(PermissionGate.decide(after, now).isEmpty())
        assertFalse(after["dnd"]!!.granted)
        assertTrue(after["dnd"]!!.asked)
    }

    @Test
    fun `إذن جديد أضيف مستقبلاً يُطلب مرة واحدة لمن لديه الباقي`() {
        val state = PermissionGate.KEYS.associateWith { PermissionGate.PermState(asked = true, granted = true) }
        // المستخدم لديه كل أذونات اليوم وتعمل
        assertTrue(PermissionGate.decide(state, allGranted).isEmpty())
        // وُلّد إذنٌ جديد مستقبلاً = مفتاحٌ لا حالة له في المخزن
        // (غير مسأل قطّ) والواقع أنّه غير ممنوح ← يُطلب مرةً واحدة
        val now = allGranted + ("dnd" to false)
        val withoutDndState = state - "dnd"
        assertEquals(listOf("dnd"), PermissionGate.decide(withoutDndState, now))
        // وبعد السلسلة يسكت ما دام غير ممنوح
        val after = PermissionGate.markAsked(withoutDndState, listOf("dnd"), now)
        assertTrue(PermissionGate.decide(after, now).isEmpty())
    }

    @Test
    fun `التحديث لا يفتح سلسلة الطلب من جديد لمن يعمل`() {
        // قبل البوابة كانت ذاكرة النسخة تفتح السلسلة بعد كل تحديث —
        // الآن الحالة بالمفتاح لا بالنسخة: المعطي كلّه يمضي بلا حوار
        val state = PermissionGate.KEYS.associateWith { PermissionGate.PermState(asked = true, granted = true) }
        assertTrue(PermissionGate.decide(state, allGranted).isEmpty())
    }

    @Test
    fun `التدوير JSON يفقد شيئا والفاسد يسير بأمان`() {
        val state = mapOf(
            "location" to PermissionGate.PermState(true, true),
            "dnd" to PermissionGate.PermState(true, false),
        )
        val parsed = PermissionGate.fromJson(PermissionGate.toJson(state))
        assertEquals(state, parsed.filterKeys { it in state.keys })
        assertTrue(PermissionGate.fromJson(null).isEmpty())
        assertTrue(PermissionGate.fromJson("").isEmpty())
        assertTrue(PermissionGate.fromJson("بلا معنى").isEmpty())
    }

    @Test
    fun `markAsked يسجّل المعطي ممنوحاً والرافض مرفوضاً`() {
        val now = allGranted + ("dnd" to false)
        val after = PermissionGate.markAsked(emptyMap(), PermissionGate.KEYS, now)
        assertTrue(after["location"]!!.granted)
        assertTrue(after["dnd"]!!.asked)
        assertFalse(after["dnd"]!!.granted)
        // واكتمال السلسلة يعني: الفتح القادم بلا أي طلبٍ ما دام الوضع كما هو
        assertTrue(PermissionGate.decide(after, now).isEmpty())
    }
}
