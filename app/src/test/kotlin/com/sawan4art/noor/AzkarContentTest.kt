package com.sawan4art.noor

import com.sawan4art.noor.domain.azkar.AzkarContent
import com.sawan4art.noor.domain.azkar.AzkarGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود محتوى الأذكار — النصوص المشكولة ومصادرها وأعدادها المسنونة
 * (alpha27): كل ذكرٍ بنصٍ غير خالٍ ومصدرٍ صريح وعددٍ من العادات
 * المسنونة، ومعرّفات فريدة عبر المجموعات كلها، والصلاة على النبي
 * نصوصها موثقة حرفياً (ذكرٌ مستقل لا يدخل تسبيح ما بعد الصلاة).
 */
class AzkarContentTest {

    // ===== عقود الصلاة على النبي — ذكر مستقل =====

    @Test
    fun `نص الصلاة القصير داخل النقطة حرفياً`() {
        assertEquals("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", AzkarContent.PRAISE_TEXT)
    }

    @Test
    fun `الصلاة الإبراهيمية كاملة تنعقد في نصين صلاة وبركة`() {
        val full = AzkarContent.PRAISE_FULL
        assertTrue(full.contains("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ"))
        assertTrue(full.contains("اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ"))
        // الخاتمة المشهورة ترد مرتين — مرة في كل نص
        assertEquals(2, Regex("إِنَّكَ حَمِيدٌ مَجِيدٌ").findAll(full).count())
        assertEquals("متفق عليه", AzkarContent.PRAISE_SOURCE)
        // فضل الذكر بمسند مسلم حرفياً
        assertEquals(
            "«مَنْ صَلَّى عَلَيَّ صَلاةً صَلَّى اللَّهُ عَلَيْهِ بِهَا عَشْرًا»",
            AzkarContent.PRAISE_VIRTUE,
        )
    }

    // ===== عقود المجموعات الثلاث =====

    @Test
    fun `المجموعات ثلاثة بالترتيب الصباح ثم المساء ثم النوم`() {
        assertEquals(listOf("morning", "evening", "sleep"), AzkarContent.GROUPS.map { it.id })
    }

    @Test
    fun `كل ذكر بنص مشكول غير خالٍ ومصدر صريح وعدد مسنون صحيح`() {
        val validCounts = setOf(1, 3, 4, 7, 10, 33, 100)
        AzkarContent.GROUPS.forEach { group ->
            assertTrue(group.items.isNotEmpty())
            group.items.forEach { item ->
                assertTrue("نص ${item.id} خالٍ!", item.text.isNotBlank())
                assertTrue("المصدر ${item.id} خالٍ!", item.source.isNotBlank())
                assertTrue(
                    "العدد ${item.count} لـ ${item.id} ليس من العادات المسنونة",
                    item.count in validCounts,
                )
                assertTrue(
                    "النص ${item.id} بلا تشكيل؟",
                    Regex("[\\u064B-\\u0652]").containsMatchIn(item.text),
                )
            }
        }
    }

    @Test
    fun `المعرفات فريدة عبر كل المجموعات`() {
        val ids = AzkarContent.GROUPS.flatMap { it.items }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `معرفات الأذكار تعود بنطاق مجموعتها لا غير`() {
        AzkarContent.GROUPS.forEach { group ->
            val prefix = when (group.id) {
                "morning" -> "morning_"
                "evening" -> "evening_"
                else -> "sleep_"
            }
            assertTrue(
                "معرّف خارج نطاق ${group.id}",
                group.items.all { it.id.startsWith(prefix) },
            )
        }
    }

    // ===== عقود الأعداد المسنونة المشهورة =====

    @Test
    fun `سبحان الله وبحمده مائة صباحاً ومساءً`() {
        val morning = AzkarContent.MORNING.items.first { it.id == "morning_subhan_100" }
        val evening = AzkarContent.EVENING.items.first { it.id == "evening_subhan_100" }
        assertEquals(100, morning.count)
        assertEquals(100, evening.count)
        assertEquals("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", morning.text)
        assertEquals("مسلم", morning.source)
    }

    @Test
    fun `تسبيح فاطمة عند النوم ثلاثا وثلاثين في الثلاثة — بأمر المالك`() {
        // أمر المالك alpha28: «هناك ذكر من المفترض انه ٣٣ و هو معمول الان
        // ٣٤ و هو الله اكبر» — التكبير ثلاث وثلاثين كالمسبحة نفسها
        // (عقد MisbahaDhikrList ٣٣/٣٣/٣٣) وكرواية دُبر الصلوات في مسلم
        val sleep = AzkarContent.SLEEP.items
        assertEquals(33, sleep.first { it.id == "sleep_subhan_33" }.count)
        assertEquals(33, sleep.first { it.id == "sleep_hamd_33" }.count)
        assertEquals(33, sleep.first { it.id == "sleep_akbar_33" }.count)
    }

    @Test
    fun `الأذكار المضافة alpha28 موثقة بعددها المسنون`() {
        // الصباح: فطرة الإسلام + الشهادة الأربع + العفو والعافية الثلاث +
        // عالم الغيب + التوحيد عشراً
        val morning = AzkarContent.MORNING.items
        assertTrue(morning.first { it.id == "morning_fitrah" }.text.startsWith("أَصْبَحْنَا عَلَى فِطْرَةِ الْإِسْلَامِ"))
        assertEquals(4, morning.first { it.id == "morning_shahida_4" }.count)
        assertEquals(3, morning.first { it.id == "morning_afwa_afiya" }.count)
        assertEquals(1, morning.first { it.id == "morning_alim_ghayb" }.count)
        assertEquals(10, morning.first { it.id == "morning_tawhid_10" }.count)
        // المساء: الصيغ المسائية نفسها
        val evening = AzkarContent.EVENING.items
        assertTrue(evening.first { it.id == "evening_fitrah" }.text.startsWith("أَمْسَيْنَا عَلَى فِطْرَةِ الْإِسْلَامِ"))
        assertEquals(4, evening.first { it.id == "evening_shahida_4" }.count)
        assertEquals(3, evening.first { it.id == "evening_afwa_afiya" }.count)
        assertEquals(1, evening.first { it.id == "evening_alim_ghayb" }.count)
        // النوم: وضع الجنب + قني عذابك ثلاثاً
        val sleep = AzkarContent.SLEEP.items
        assertEquals(1, sleep.first { it.id == "sleep_wadaat_janbi" }.count)
        assertEquals(3, sleep.first { it.id == "sleep_qini_adhab" }.count)
        assertTrue(sleep.first { it.id == "sleep_qini_adhab" }.text.contains("قِنِي عَذَابَكَ"))
    }

    @Test
    fun `المجموعات صارت أغنى — أذكار أكثر من المرحلة الأولى`() {
        // alpha28: المزيد من الأذكار (توجيه المالك) — الصباح أربعة عشر+
        // والمساء أحد عشر+ والنوم أحد عشر+
        assertTrue(AzkarContent.MORNING.items.size >= 14)
        assertTrue(AzkarContent.EVENING.items.size >= 11)
        assertTrue(AzkarContent.SLEEP.items.size >= 11)
    }

    @Test
    fun `سيد الاستغفار مرة واحدة بكل من الصباح والمساء بنص البخاري`() {
        val expected = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، " +
            "وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، " +
            "أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، " +
            "فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ"
        val morning = AzkarContent.MORNING.items.first { it.id == "morning_sayyid_istighfar" }
        val evening = AzkarContent.EVENING.items.first { it.id == "evening_sayyid_istighfar" }
        assertEquals(expected, morning.text)
        assertEquals(expected, evening.text)
        assertEquals(1, morning.count)
        assertEquals(1, evening.count)
        assertTrue(morning.source.contains("البخاري"))
    }

    @Test
    fun `حسبي الله سبع مرات في الصباح والمساء`() {
        assertEquals(7, AzkarContent.MORNING.items.first { it.id == "morning_hasbi" }.count)
        assertEquals(7, AzkarContent.EVENING.items.first { it.id == "evening_hasbi" }.count)
    }

    @Test
    fun `أذكار الصباح تختلف لفظ الصباح عن المساء في صيغتي اليوم`() {
        val morning = AzkarContent.MORNING.items.first { it.id == "morning_mulk_azim" }
        val evening = AzkarContent.EVENING.items.first { it.id == "evening_mulk_azim" }
        assertTrue(morning.text.contains("أَصْبَحْنَا"))
        assertTrue(evening.text.contains("أَمْسَيْنَا"))
        assertFalse(morning.text.contains("اللَّيْلَةِ"))
        assertFalse(evening.text.contains("الْيَوْمِ"))
    }

    @Test
    fun `البحث بالمعرف يعود بمجموعة سليمة لغير المعرّف`() {
        assertEquals("morning", AzkarContent.groupById("morning").id)
        assertEquals("evening", AzkarContent.groupById("evening").id)
        assertEquals("sleep", AzkarContent.groupById("sleep").id)
        // معرّف غريب يعود سالمًا بمجموعة حقيقية لا انهياراً
        val fallback: AzkarGroup = AzkarContent.groupById("whatever")
        assertTrue(fallback.items.isNotEmpty())
    }
}
