package com.sawan4art.noor

import com.sawan4art.noor.domain.adhan.PrayerReminder
import com.sawan4art.noor.domain.adhan.PrayerVoiceMaps
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود صوت كل صلاة والتنبيه قبل/بعد الأذان (alpha61) — بلاغا المالك:
 * «عايز ظبط اصوات الاذان و الاقامه يكون اما لكل صلاه على حده او لكل
 * الصلوات» و«عايز اضيف في الاعادات زر جديد خاص بالتنبيه قبل او بعد
 * اذان اي صلاه … بوقت محدد لكل صلاه».
 */
class PrayerVoicesAndReminderTest {

    /* ============ صوت لكل صلاة ============ */

    @Test
    fun `تحليل فارغ وغريب يعود فارغا بأمان`() {
        assertTrue(PrayerVoiceMaps.parse(null).isEmpty())
        assertTrue(PrayerVoiceMaps.parse("").isEmpty())
        assertTrue(PrayerVoiceMaps.parse("weird:main,JUNK:x").isEmpty())
    }

    @Test
    fun `جولة كاملة تحليل وتنسيق تحفظ الاختيارات`() {
        val raw = "FAJR:fakhry,MAGHRIB:notification"
        val map = PrayerVoiceMaps.parse(raw)
        assertEquals("fakhry", map["FAJR"])
        assertEquals("notification", map["MAGHRIB"])
        assertEquals(raw, PrayerVoiceMaps.format(map))
    }

    @Test
    fun `صوت الصلاة الفعلي يرجع للكل عند غياب مفتاحها`() {
        val prefs = com.sawan4art.noor.data.AdhanPrefs(
            adhanVoice = "main",
            perPrayerVoices = true,
            perPrayerAdhanVoices = mapOf("FAJR" to "aqsa"),
        )
        assertEquals("aqsa", prefs.adhanVoiceFor(PrayerTimeKey.FAJR))
        // الظهر بلا اختيار خاص — صوت الكل
        assertEquals("main", prefs.adhanVoiceFor(PrayerTimeKey.DHUHR))
        // الوضع مطفأ — صوت الكل حتى للمفاتيح المحفوظة
        val off = prefs.copy(perPrayerVoices = false)
        assertEquals("main", off.adhanVoiceFor(PrayerTimeKey.FAJR))
    }

    @Test
    fun `المسار المخصص يتبع صوت الصلاة نفسها`() {
        val prefs = com.sawan4art.noor.data.AdhanPrefs(
            customAdhanPath = "/x/a.mp3",
            perPrayerVoices = true,
            perPrayerAdhanVoices = mapOf("ISHA" to "custom"),
        )
        assertEquals("/x/a.mp3", prefs.adhanCustomPathFor(PrayerTimeKey.ISHA))
        assertEquals("", prefs.adhanCustomPathFor(PrayerTimeKey.FAJR))
    }

    /* ============ التنبيه قبل/بعد الأذان ============ */

    @Test
    fun `قواعد CSV جولة كاملة مع رمي الشاذ`() {
        val raw = "FAJR:BEFORE:10,MAGHRIB:AFTER:5,JUNK:X:1,FAJR:OFF:3"
        val rules = PrayerReminder.parseRules(raw)
        // OFF يُرمى (لا يزيل سابقه — المسح بأمر المستخدم من الواجهة)
        // وJUNK يُرمى — فيبقى قبل الفجر وبعد المغرب
        assertEquals(2, rules.size)
        assertEquals(PrayerReminder.Mode.BEFORE, rules["FAJR"]?.mode)
        assertEquals(PrayerReminder.Mode.AFTER, rules["MAGHRIB"]?.mode)
        assertEquals(5, rules["MAGHRIB"]?.minutes)
        assertEquals(
            setOf("FAJR:BEFORE:10", "MAGHRIB:AFTER:5").joinToString(","),
            PrayerReminder.formatRules(rules),
        )
    }

    @Test
    fun `الدقائق تُحصّن داخل الحدود`() {
        assertEquals(1, PrayerReminder.clampMinutes(0))
        assertEquals(120, PrayerReminder.clampMinutes(500))
        assertEquals(15, PrayerReminder.clampMinutes(15))
        assertFalse(PrayerReminder.isValid(PrayerReminder.Rule(PrayerReminder.Mode.OFF, 10)))
        assertFalse(PrayerReminder.isValid(PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 0)))
        assertTrue(PrayerReminder.isValid(PrayerReminder.Rule(PrayerReminder.Mode.AFTER, 120)))
    }

    @Test
    fun `لحظة التنبيه قبلها مطروحا وبعدها مضافا`() {
        val at = ZonedDateTime.parse("2026-11-09T17:30:00+02:00[Africa/Cairo]")
        val before = PrayerReminder.eventAt(at, PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 15))!!
        assertEquals(17 * 60 + 15, before.hour * 60 + before.minute)
        val after = PrayerReminder.eventAt(at, PrayerReminder.Rule(PrayerReminder.Mode.AFTER, 30))!!
        assertEquals(18 * 60, after.hour * 60 + after.minute)
        assertNull(PrayerReminder.eventAt(at, PrayerReminder.Rule(PrayerReminder.Mode.OFF, 10)))
        assertNull(PrayerReminder.eventAt(at, PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 0)))
    }

    @Test
    fun `نص التنبيه عربي مفهوم بالدقائق العربية`() {
        val note = PrayerReminder.note("الفجر", PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 10))
        assertTrue(note.contains("قبل"))
        assertTrue(note.contains("الفجر"))
        val after = PrayerReminder.note("المغرب", PrayerReminder.Rule(PrayerReminder.Mode.AFTER, 20))
        assertTrue(after.contains("بعد"))
        assertTrue(after.contains("المغرب"))
    }

    @Test
    fun `قاعدة التنبيه الفعلية لا تعمل بغير المفتاح الرئيسي`() {
        val prefs = com.sawan4art.noor.data.AdhanPrefs(
            reminders = mapOf("FAJR" to PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 10)),
        )
        assertNull(prefs.reminderFor(PrayerTimeKey.FAJR))
        val on = prefs.copy(reminderEnabled = true)
        assertEquals(10, on.reminderFor(PrayerTimeKey.FAJR)?.minutes)
        assertNull(on.reminderFor(PrayerTimeKey.DHUHR))
    }

    // ===== اسم المنبه المخصص — alpha63 =====
    // بلاغ المالك: «ميزه التنبيه قبل او بعد الاذان عايز يكون فيها اسماء
    // المنبهات المظبوطه يعني مثلا اذا اراد المستخدم ظبط تنبيه السحور
    // قبل اذان الفجر و هكذا اي تنبيه لاز يكون ليه اسم».

    @Test
    fun `الاسم المخصص هو ما يظهر في المنبه`() {
        val rule = PrayerReminder.Rule(
            PrayerReminder.Mode.BEFORE, 15, "تنبيه السحور",
        )
        assertEquals("تنبيه السحور", PrayerReminder.note("الفجر", rule))
    }

    @Test
    fun `الاسم الفارغ يعود للنص التلقائي`() {
        val rule = PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 15)
        assertTrue(PrayerReminder.note("الفجر", rule).contains("الفجر"))
        assertTrue(PrayerReminder.note("الفجر", rule).contains("قبل"))
    }

    @Test
    fun `الاسم في سجل CSV يعود كما هو والقديم بلا اسم يعمل`() {
        // سجل حديث بالاسم
        val round = PrayerReminder.parseRules("FAJR:BEFORE:15:تنبيه السحور,MAGHRIB:AFTER:5")
        assertEquals("تنبيه السحور", round["FAJR"]?.name)
        assertEquals(15, round["FAJR"]?.minutes)
        assertEquals("", round["MAGHRIB"]?.name)
        // ونسخة قديمة ثلاثية بلا اسمٍ تُقرأ كما هي فلا يفقد المستخدم تخصيصه
        val legacy = PrayerReminder.parseRules("FAJR:BEFORE:15")
        assertEquals(PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 15), legacy["FAJR"])
        // والتنسيق يحفظ الاسم ويقصده
        val csv = PrayerReminder.formatRules(
            mapOf("FAJR" to PrayerReminder.Rule(PrayerReminder.Mode.BEFORE, 10, "تنبيه الإمساك")),
        )
        assertEquals("FAJR:BEFORE:10:تنبيه الإمساك", csv)
    }

    @Test
    fun `تحصين الاسم يرمي الفواصل التي تكسر سجل القواعد`() {
        assertEquals("تنبيه أ، ب", PrayerReminder.sanitizeName("تنبيه أ, ب"))
        assertEquals("ساعة-٣", PrayerReminder.sanitizeName("ساعة:٣"))
        assertEquals(60, PrayerReminder.sanitizeName("ط".repeat(100)).length)
    }
}
