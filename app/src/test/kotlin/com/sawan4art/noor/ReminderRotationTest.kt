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
    fun `التجمع يحوي خمسين حديثا صحيحا على الأقل — بأمر المالك alpha32`() {
        val hadiths = HomeReminders.filter { !it.startsWith("﴿") }
        assertTrue(
            "الأحاديث ${hadiths.size} أقل من الخمسين المطلوبة",
            hadiths.size >= 50,
        )
    }

    @Test
    fun `التجمع يحوي آيات تذكر بالطاعة والذكر`() {
        val verses = HomeReminders.filter { it.startsWith("﴿") }
        assertTrue(
            "الآيات ${verses.size} — المطلوب آيات كثيرة تذكر بالطاعة",
            verses.size >= 12,
        )
        // كل آية موثقة برقم سورتها
        assertTrue(verses.all { "— " in it })
    }

    @Test
    fun `كل حديث موثق بمصدره — البخاري أو مسلم أو المتفق عليه أو ما صححه الألباني`() {
        val hadiths = HomeReminders.filter { !it.startsWith("﴿") }
        val sources = listOf(
            "متفق عليه", "رواه البخاري", "رواه مسلم", "رواه الترمذي",
            "رواه النسائي", "رواه أبو داود", "رواه ابن ماجه", "رواه الحاكم",
        )
        hadiths.forEach { h ->
            assertTrue(
                "حديث بلا تسند: $h",
                sources.any { it in h },
            )
        }
    }

    @Test
    fun `صيغ التدوير الصحيحة الثابتة — تصحيحات المراجعة alpha32`() {
        // «الكلمة الطيبة صدقة» بصيغة البخاري ومسلم الثابتة
        assertTrue(HomeReminders.any { "الكلمة الطيبة صدقة" in it })
        // لا صيغة «الكلمات الطيبات» المرفوضة
        assertTrue(HomeReminders.none { "الكلمات الطيبات" in it })
        // لفظ المتفق عليه: «تفضل» لا «تفوق»
        assertTrue(HomeReminders.any { "تفضُل صلاةَ الفذ بسبعٍ وعشرين درجة" in it })
        assertTrue(HomeReminders.none { "تفوق صلاة" in it })
        // لفظ مسلم: «مكفرات ما بينهن» بصيغته الموثقة
        assertTrue(HomeReminders.any { "مكفّرات لما بينهن إذا اجتنبت الكبائر" in it })
        // لا حديث بلا إطار في صيغة «لا إله إلا الله»
        assertTrue(HomeReminders.any { "«من قال: لا إله إلا الله" in it })
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
