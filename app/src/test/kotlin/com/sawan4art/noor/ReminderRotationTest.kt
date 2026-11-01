package com.sawan4art.noor

import com.sawan4art.noor.ui.home.HomeReminders
import com.sawan4art.noor.ui.home.ReminderRotation
import com.sawan4art.noor.ui.home.SADAQAH_LINE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود جمل التذكير (توجيه المالك alpha30):
 * «عايز تذكيرات اكثر و التذكيرات التي في الاسفل نستبدلها بجمله ثابته
 * غير متغيره الجمله التاليه هي هذا العمل صدقة جارية لي و لأبي و أمي
 * نسألكم صالح الدعاء».
 *
 * - السطر السفلي **جملة ثابتة** حرفياً كما أماها المالك — لا دوران.
 * - شريط التذكير فوق الشبكة يبقى دوّاراً من تجمعٍ أوسع، والدوران
 *   حتميّ فتظهر جملة مختلفة مع كل قلبٍ للثيم.
 */
class ReminderRotationTest {

    // ===== السطر السفلي الثابت =====

    @Test
    fun `السطر السفلي هو جملة المالك الثابتة حرفياً`() {
        assertEquals(
            "هذا العمل صدقة جارية لي ولأبي وأمي، نسألكم صالح الدعاء",
            SADAQAH_LINE,
        )
    }

    @Test
    fun `الجملة الثابتة تحمل مضمون صدقة جارية للمالك ووالديه ودعاءً صالحاً`() {
        assertTrue(SADAQAH_LINE.contains("صدقة جارية"))
        assertTrue(SADAQAH_LINE.contains("لأبي وأمي"))
        assertTrue(SADAQAH_LINE.contains("نسألكم صالح الدعاء"))
    }

    // ===== تجمع تذكيرات الهيدر =====

    @Test
    fun `تذكيرات الهيدر تكبر ثانية عن السابق (ثلاثون على الأقل)`() {
        assertTrue(
            "تجمع التذكير لا يقل عن ٣٠ (كان ١٨ ثم طلب المالك المزيد)",
            HomeReminders.size >= 30,
        )
    }

    @Test
    fun `لا جملة فارغة ولا تكرار ولا خطأ لغوي مشهور في تجمع التذكير`() {
        assertTrue(HomeReminders.all { it.isNotBlank() })
        assertEquals(HomeReminders.size, HomeReminders.toSet().size)
        // مراجعة اللغة (alpha30): لا «ارحمكما» — الوجه الصحيح «ارحمهما»
        HomeReminders.forEach { assertTrue("لفظ خطأ في: $it", "ارحمكما" !in it) }
    }

    // ===== دوران الهيدر =====

    @Test
    fun `الدوران يتقدم دائماً فيظهر جملة مختلفة مع كل قلب`() {
        for (size in listOf(HomeReminders.size, 2, 7)) {
            for (i in 0 until size) {
                val next = ReminderRotation.nextIndex(i, size)
                assertNotEquals("القلب لا يُعيد الجملة نفسها", i, next)
                // التقدم خطوة واحدة ملتفّاً حول القائمة
                assertEquals((i + 1) % size, next)
                assertTrue(next in 0 until size)
            }
        }
    }

    @Test
    fun `الالتفاف حول القائمة يعود لأولها بعد آخرها`() {
        val last = HomeReminders.size - 1
        assertEquals(0, ReminderRotation.nextIndex(last, HomeReminders.size))
    }

    @Test
    fun `مؤشر خارج الحدود يُطبَّع لا يكسر`() {
        val size = HomeReminders.size
        val next = ReminderRotation.nextIndex(size + 7, size)
        assertTrue(next in 0 until size)
        assertEquals((size - 1 + 1) % size, next)
    }

    @Test
    fun `قائمة بعنصر واحد أو أقل لا تدور فتعيد الصفر`() {
        assertEquals(0, ReminderRotation.nextIndex(0, 1))
        assertEquals(0, ReminderRotation.nextIndex(0, 0))
    }
}
