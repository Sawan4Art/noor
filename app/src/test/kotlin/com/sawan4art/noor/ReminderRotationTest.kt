package com.sawan4art.noor

import com.sawan4art.noor.ui.home.HomeReminders
import com.sawan4art.noor.ui.home.ReminderRotation
import com.sawan4art.noor.ui.home.SadaqahReminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود جمل التذكير الدوّارة (توجيه المالك alpha24):
 * «مع كل تغيير لمظهر الوضع الليلي او النهاري تتغير جمله التذكير
 * و زود عددهم اكثر» — الدوران حتميّ فتظهر جملة مختلفة مع كل قلب،
 * والصياغة الجديدة للمالك أول جمل الصدقة تظهر عند الفتح.
 */
class ReminderRotationTest {

    @Test
    fun `الصياغة الجديدة للمالك أول جمل الصدقة حرفياً`() {
        assertEquals(
            "صدقة جارية لي ولأبي وأمي.. نسألكم صالح الدعاء",
            SadaqahReminders.first(),
        )
    }

    @Test
    fun `عدد الجمل مُكبَّر عن السابق (١٤ صدقة و١٨ تذكيراً على الأقل)`() {
        assertTrue("تجمع الصدقة لا يقل عن ١٤", SadaqahReminders.size >= 14)
        assertTrue("تجمع التذكير لا يقل عن ١٨", HomeReminders.size >= 18)
    }

    @Test
    fun `الدوران يتقدم دائماً فيظهر جملة مختلفة مع كل قلب`() {
        for (size in listOf(SadaqahReminders.size, HomeReminders.size)) {
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
        val last = SadaqahReminders.size - 1
        assertEquals(0, ReminderRotation.nextIndex(last, SadaqahReminders.size))
    }

    @Test
    fun `مؤشر خارج الحدود يُطبَّع لا يكسر`() {
        val size = SadaqahReminders.size
        val next = ReminderRotation.nextIndex(size + 7, size)
        assertTrue(next in 0 until size)
        assertEquals((size - 1 + 1) % size, next)
    }

    @Test
    fun `قائمة بعنصر واحد أو أقل لا تدور فتعيد الصفر`() {
        assertEquals(0, ReminderRotation.nextIndex(0, 1))
        assertEquals(0, ReminderRotation.nextIndex(0, 0))
    }

    @Test
    fun `لا جملة فارغة ولا تكرار في تجمع الصدقة`() {
        assertTrue(SadaqahReminders.all { it.isNotBlank() })
        assertEquals(SadaqahReminders.size, SadaqahReminders.toSet().size)
    }
}
