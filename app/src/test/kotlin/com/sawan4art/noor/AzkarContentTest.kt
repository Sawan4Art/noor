package com.sawan4art.noor

import com.sawan4art.noor.domain.azkar.AzkarContent
import com.sawan4art.noor.domain.azkar.AzkarGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود محتوى الأذكار — النصوص المشكولة ومصادرها وأعدادها المسنونة
 * (alpha29 توجيه المالك: «خفف الاذكار و اعرض الاذكار الخاصه بكل قسم ..
 * عايز اضيف حتى ٣٠ نوع من الاذكار ان وجد») — ثلاثون قسماً
 * خفيفاً + الصلاة على النبي عدّادها الحر = واحد وثلاثون نوعاً.
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
        assertEquals(
            "«مَنْ صَلَّى عَلَيَّ صَلاةً صَلَّى اللَّهُ عَلَيْهِ بِهَا عَشْرًا»",
            AzkarContent.PRAISE_VIRTUE,
        )
    }

    // ===== عقود الأقسام الثلاثين =====

    @Test
    fun `ثلاثون قسما مع الصلاة على النبي تصير واحد وثلاثين نوعا`() {
        assertEquals(30, AzkarContent.GROUPS.size)
        // مع بطاقة الصلاة على النبي المستقلة = ٣١ نوعاً — المالك وسّع بالطلب
        assertEquals(31, AzkarContent.GROUPS.size + 1)
    }

    @Test
    fun `ترتيب الأقسام — اليومية أولا ثم المكانية ثم الأدعية`() {
        val ids = AzkarContent.GROUPS.map { it.id }
        assertEquals("morning", ids.first())
        assertEquals("evening", ids[1])
        assertEquals("sleep", ids[2])
        // الأقسام التي طلبها المالك بالاسم موجودة
        assertTrue("قسم السفر مفقود!", "travel" in ids)
        assertTrue("قسم الثياب الجديدة مفقود!", "clothes" in ids)
        assertTrue("قسم بعد الصلاة مفقود!", "after_prayer" in ids)
        assertTrue("قسم الاستيقاظ مفقود!", "wake_up" in ids)
    }

    @Test
    fun `كل ذكر بنص مشكول غير خال ومصدر صريح وعدد مسنون صحيح`() {
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
    fun `المعرفات فريدة عبر كل الأقسام وبنطاق مجموعتها`() {
        val all = AzkarContent.GROUPS.flatMap { it.items }
        val ids = all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // كل معرّف يعود بنطاق قسمه — ما عدا المشترك المعلن (سيد الاستغفار
        // والكلمات الثامات تُعرَّف بنفس بادئة قسمها دوماً)
        AzkarContent.GROUPS.forEach { group ->
            assertTrue(
                "معرّف خارج نطاق ${group.id}",
                group.items.all { it.id.startsWith(group.id + "_") },
            )
        }
    }

    @Test
    fun `كل قسم خفيف — المحتوى المختار لا أكواد الحصن الكاملة`() {
        // أمر المالك: «خفف الاذكار» — لا قسم يتجاوز اثني عشر ذكراً،
        // والأقسام الموقتية تبقى قصيرة (١-٤ أذكار)
        AzkarContent.GROUPS.forEach { group ->
            assertTrue("القسم ${group.id} ثقيل: ${group.items.size}", group.items.size <= 12)
        }
        assertEquals(1, AzkarContent.WUDU.items.size)
        assertEquals(1, AzkarContent.CRESCENT.items.size)
        assertEquals(3, AzkarContent.TRAVEL.items.size)
        assertEquals(2, AzkarContent.CLOTHES.items.size)
    }

    @Test
    fun `الثراء الكلي — أكثر من سبعين ذكرا موثقا في الأقسام`() {
        val total = AzkarContent.GROUPS.sumOf { it.items.size }
        assertTrue("المجموع ${total} أقل من المتوقع", total >= 80)
    }

    // ===== عقود الأعداد المسنونة المشهورة =====

    @Test
    fun `سبحان الله وبحمده مائة صباحا ومساء`() {
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
        val sleep = AzkarContent.SLEEP.items
        assertEquals(33, sleep.first { it.id == "sleep_subhan_33" }.count)
        assertEquals(33, sleep.first { it.id == "sleep_hamd_33" }.count)
        assertEquals(33, sleep.first { it.id == "sleep_akbar_33" }.count)
        // ودُبر الصلاة ثلاثتها ثلاث وثلاثين أيضاً
        val after = AzkarContent.AFTER_PRAYER.items
        assertEquals(33, after.first { it.id == "after_prayer_subhan_33" }.count)
        assertEquals(33, after.first { it.id == "after_prayer_hamd_33" }.count)
        assertEquals(33, after.first { it.id == "after_prayer_akbar_33" }.count)
    }

    @Test
    fun `سيد الاستغفار بنص البخاري في الصباح والمساء والاستغفار`() {
        val expected = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، " +
            "وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، " +
            "أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، " +
            "فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ"
        listOf("morning", "evening", "istighfar").forEach { gid ->
            val item = AzkarContent.groupById(gid).items.first { it.id.endsWith("_sayyid_istighfar") || it.id == "${gid}_sayyid" }
            assertEquals(expected, item.text)
            assertEquals(1, item.count)
        }
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
    fun `أذكار السفر موثقة — الركوب والنزول والعودة`() {
        val travel = AzkarContent.TRAVEL
        assertTrue(travel.items.first { it.id == "travel_ride" }.text.contains("سَخَّرَ لَنَا هَذَا"))
        assertTrue(travel.items.first { it.id == "travel_ride" }.text.contains("الْبِرَّ وَالتَّقْوَى"))
        assertEquals(3, travel.items.first { it.id == "travel_lodge" }.count)
        assertTrue(travel.items.first { it.id == "travel_return" }.text.contains("آيِبُونَ تَائِبُونَ"))
    }

    @Test
    fun `الثياب الجديدة والوضوء والخلاء بنصوصها المسنونة`() {
        assertTrue(
            AzkarContent.CLOTHES.items.first { it.id == "clothes_new" }.text
                .contains("أَنْتَ كَسَوْتَنِيهِ"),
        )
        assertTrue(
            AzkarContent.WUDU.items.first { it.id == "wudu_shahada" }.text
                .contains("مِنَ التَّوَّابِينَ"),
        )
        assertTrue(
            AzkarContent.KHILAA.items.first { it.id == "khilaa_enter" }.text
                .contains("الْخُبْثِ وَالْخَبَائِثِ"),
        )
        assertEquals("غُفْرَانَكَ", AzkarContent.KHILAA.items.first { it.id == "khilaa_leave" }.text)
    }

    @Test
    fun `أذكار الزلازل قسم خفيف موثق بأمر المالك`() {
        // توجيه المالك alpha30: «اضف اذكار الزلال» — أي الزلازل
        val zalzala = AzkarContent.ZALZAL
        assertEquals("zalzala", zalzala.id)
        assertEquals("أذكار الزلازل", zalzala.title)
        assertTrue("قسم الزلازل خفيف (خفف الاذكار)", zalzala.items.size <= 6)
        assertTrue("zalzala" in AzkarContent.GROUPS.map { it.id })
        // دعاء الرعد الصحيح الذي يُسن عند الزلازل
        val thunder = zalzala.items.first { it.id == "zalzala_la_taqtulna" }
        assertTrue(thunder.text.contains("لَا تَقْتُلْنَا بِغَضَبِكَ"))
        assertTrue(thunder.text.contains("وَعَافِنَا قَبْلَ ذَلِكَ"))
        // ودعاء المصاب من مسلم و«حسبي الله» من البخاري
        assertTrue(
            zalzala.items.first { it.id == "zalzala_istirja" }.text
                .contains("إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ"),
        )
        assertTrue(
            zalzala.items.first { it.id == "zalzala_hasbuna" }.text
                .contains("حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ"),
        )
    }

    @Test
    fun `أذكار البيت والحمام بأسمائها الواضحة ومحتواها الموسع`() {
        // alpha31 توجيه المالك: «اضف اذكار دخول و خروج من البيت و اذكار
        // دخول الحمام» — القسمان موجودان بأسماء أصبحت ظاهرة وبمحتوى أوفى
        val home = AzkarContent.HOME
        assertEquals("home", home.id)
        // alpha44 أمر المالك: «اعد تسميتها ل اذكار دخول البيت و الخروج»
        assertEquals("أذكار دخول البيت والخروج", home.title)
        assertTrue("عنوان البيت غير ظاهر: ${home.title}", home.title.contains("البيت"))
        assertTrue(home.title.contains("دخول"))
        assertTrue(home.title.contains("الخروج"))
        assertEquals(4, home.items.size)
        assertTrue(home.items.first { it.id == "home_enter" }.text.contains("وَلَجْنَا"))
        assertTrue(
            home.items.first { it.id == "home_greet" }.text
                .contains("السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللهِ"),
        )
        assertTrue(
            home.items.first { it.id == "home_leave" }.text
                .contains("تَوَكَّلْتُ عَلَى اللَّهِ"),
        )
        assertTrue(
            home.items.first { it.id == "home_leave_refuge" }.text
                .contains("اللَّهُمَّ إِنِّي أَعُوذُ"),
        )
        assertTrue(
            home.items.first { it.id == "home_leave_refuge" }.text
                .contains("أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ"),
        )

        val khilaa = AzkarContent.KHILAA
        assertEquals("khilaa", khilaa.id)
        assertTrue("عنوان الحمام غير ظاهر: ${khilaa.title}", khilaa.title.contains("الحمام"))
        assertEquals(3, khilaa.items.size)
        assertTrue(
            khilaa.items.first { it.id == "khilaa_enter" }.text
                .contains("الْخُبْثِ وَالْخَبَائِثِ"),
        )
        assertEquals("غُفْرَانَكَ", khilaa.items.first { it.id == "khilaa_leave" }.text)
        assertTrue(
            khilaa.items.first { it.id == "khilaa_bath" }.text
                .contains("الْحَمْدُ لِلَّهِ الَّذِي أَذْهَبَ"),
        )
        // الأقسام الثلاثون قائمة بلا زيادة — الإثراء داخل القسمين لا إضافة قسم
        assertEquals(30, AzkarContent.GROUPS.size)
    }

    @Test
    fun `خاتمة القنوت التامة من الترمذي موجودة`() {
        // مراجعة النص واللغة (alpha30): «تباركت ربنا وتعاليت» خاتمة الحديث
        assertTrue(
            AzkarContent.QUNUT.items.first { it.id == "qunut_dua" }.text
                .contains("تَبَارَكْتَ رَبَّنَا وَتَعَالَيْتَ"),
        )
    }

    @Test
    fun `البحث بالمعرف يعود بقسم سليم لغير المعرف`() {
        assertEquals("morning", AzkarContent.groupById("morning").id)
        assertEquals("travel", AzkarContent.groupById("travel").id)
        assertEquals("qunut", AzkarContent.groupById("qunut").id)
        // معرّف غريب يعود سالمًا بقسم حقيقي لا انهياراً
        val fallback: AzkarGroup = AzkarContent.groupById("whatever")
        assertTrue(fallback.items.isNotEmpty())
    }
}
