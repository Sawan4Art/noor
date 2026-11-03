package com.sawan4art.noor

import com.sawan4art.noor.domain.azkar.AzkarGroup
import com.sawan4art.noor.domain.azkar.AzkarItem
import com.sawan4art.noor.domain.azkar.AzkarLibrary
import com.sawan4art.noor.domain.azkar.CustomDhikr
import com.sawan4art.noor.domain.azkar.CustomGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود «المكتبة الحيّة» للأذكار (alpha44 — بلاغ المالك: «اضف زر في قسم
 * الاذكار يمكن المستخدم من انشاء ذكر اضافي و او حذف اذكار موجوده بالفعل
 * مع طبعا امكانيه استرجاع الاذكار الافتراضيه»):
 *
 * ١. الإضافات تظهر في نهاية قسمها والافتراضي المحذوف يُستثنى من العرض.
 * ٢. زر الاسترجاع يفرّغ المحذوف فتعود الافتراضية كلها.
 * ٣. ترميز الحفظ يحمل الذكر كاملاً ويحل التالف بتسامح لا انهيار.
 */
class AzkarLibraryTest {

    private val group = AzkarGroup(
        id = "morning",
        title = "أذكار الصباح",
        subtitle = "",
        items = listOf(
            AzkarItem("morning_a", "ذكرٌ أول", 1, "مسلم"),
            AzkarItem("morning_b", "ذكرٌ ثانٍ", 3, "البخاري"),
        ),
    )

    // ===== المحتوى الفعلي =====

    @Test
    fun `بدون تعديلات يعود المحتوى الافتراضي كما هو`() {
        assertEquals(group.items, AzkarLibrary.effectiveItems(group, emptyList(), emptySet()))
    }

    @Test
    fun `الافتراضي المحذوف يختفي من العرض والمضاف يظهر في النهاية`() {
        val customs = listOf(
            CustomDhikr(
                "morning",
                AzkarItem("custom_1", "ذكرٌ مضاف", 3, "ذكر مضاف"),
            ),
        )
        val items = AzkarLibrary.effectiveItems(group, customs, setOf("morning_a"))
        assertEquals(listOf(group.items[1], customs[0].item), items)
    }

    @Test
    fun `المضاف لقسم آخر لا يتسرب إلى هذا القسم`() {
        val customs = listOf(
            CustomDhikr(
                "evening",
                AzkarItem("custom_x", "ذكر المساء", 1, "ذكر مضاف"),
            ),
        )
        assertEquals(group.items, AzkarLibrary.effectiveItems(group, customs, emptySet()))
    }

    @Test
    fun `الاسترجاع يفرغ المحذوف فتعود الافتراضية كلها`() {
        val customs = listOf(
            CustomDhikr("morning", AzkarItem("custom_1", "ذكرٌ مضاف", 1, "ذكر مضاف")),
        )
        val afterRestore = AzkarLibrary.effectiveGroup(group, customs, emptySet())
        assertEquals(group.items + customs[0].item, afterRestore.items)
        assertEquals(group.title, afterRestore.title)
    }

    @Test
    fun `مقدر التقدم يتبع المحتوى الفعلي لا الافتراضي`() {
        // يُختبر عبر AzkarMath على القسم الفعلي — هنا عقد الشكل فقط
        val effective = AzkarLibrary.effectiveGroup(group, emptyList(), setOf("morning_a", "morning_b"))
        assertEquals(0, effective.items.size)
    }

    // ===== التمييز بين المضاف والافتراضي =====

    @Test
    fun `معرفات المضافة بادئتها custom والافتراضي غير ذلك`() {
        assertTrue(AzkarLibrary.isCustom("custom_abc"))
        assertFalse(AzkarLibrary.isCustom("morning_a"))
        assertFalse(AzkarLibrary.isCustom(""))
    }

    // ===== ترميز الحفظ =====

    @Test
    fun `الترميز يحمل الذكر كاملا ويعود كما هو`() {
        val custom = CustomDhikr(
            "morning",
            AzkarItem("custom_1", "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى", 3, "مسلم"),
        )
        val encoded = AzkarLibrary.encodeCustom(custom)
        assertEquals(custom, AzkarLibrary.decodeCustom(encoded!!))
    }

    @Test
    fun `النص المحتوي فواصل وأسطرا وشرطات مائلة يُهرَّب ويصح`() {
        val custom = CustomDhikr(
            "home",
            AzkarItem("custom_2", "سطرٌ أول|سطرٌ ثانٍ\nسطرٌ ثالث\\خاص", 7, "مصدر|مع قائم"),
        )
        val decoded = AzkarLibrary.decodeCustom(AzkarLibrary.encodeCustom(custom)!!)
        assertEquals(custom, decoded)
    }

    @Test
    fun `المصدر الفارغ مسموح والنص الخالي يرفض`() {
        val ok = CustomDhikr("morning", AzkarItem("custom_3", "ذكرٌ بلا مصدر", 1, ""))
        assertEquals(ok, AzkarLibrary.decodeCustom(AzkarLibrary.encodeCustom(ok)!!))

        val blank = CustomDhikr("morning", AzkarItem("custom_4", "  ", 1, "مصدر"))
        assertNull(AzkarLibrary.encodeCustom(blank))
    }

    @Test
    fun `العدد المعدوم أو السالب يرفض في الترميز`() {
        assertNull(
            AzkarLibrary.encodeCustom(
                CustomDhikr("morning", AzkarItem("custom_5", "ذكرٌ", 0, "")),
            ),
        )
        assertNull(
            AzkarLibrary.encodeCustom(
                CustomDhikr("morning", AzkarItem("custom_6", "ذكرٌ", -3, "")),
            ),
        )
    }

    @Test
    fun `المدخلات التالفة تعود بلا ذكر ولا انهيار`() {
        assertNull(AzkarLibrary.decodeCustom(""))
        assertNull(AzkarLibrary.decodeCustom("morning"))
        assertNull(AzkarLibrary.decodeCustom("||||"))
        assertNull(AzkarLibrary.decodeCustom("morning|custom_7|لا_عدد|مصدر|نص"))
        assertNull(AzkarLibrary.decodeCustom("morning|custom_7|0|مصدر|نص"))
        assertNull(AzkarLibrary.decodeCustom("|custom_7|3|مصدر|نص"))
        // معرفات غير مضافة (بلا بادئة) ترفض — حماية من تعديل الافتراضي بالحفظ
        assertNull(AzkarLibrary.decodeCustom("morning|morning_a|3|مصدر|نص"))
    }

    // ===== الأقسام المضافة (alpha45) =====

    private val defaults = listOf(
        AzkarGroup("morning", "أذكار الصباح", "", listOf(AzkarItem("morning_a", "ذكرٌ", 1, "مسلم"))),
        AzkarGroup("evening", "أذكار المساء", "", listOf(AzkarItem("evening_a", "ذكرٌ", 1, "مسلم"))),
    )

    @Test
    fun `القسم المضاف يظهر بطاقته في نهاية الشبكة`() {
        val customGroups = listOf(CustomGroup("cg_1", "أذكار سفري", "قسم من إضافتك"))
        val groups = AzkarLibrary.effectiveGroups(defaults, customGroups, emptySet())
        assertEquals(3, groups.size)
        assertEquals("أذكار سفري", groups[2].title)
        assertEquals("cg_1", groups[2].id)
        // صفحة القسم المضاف تبدأ فارغة وأذكاره من مضافات المستخدم
        assertTrue(groups[2].items.isEmpty())
    }

    @Test
    fun `القسم الافتراضي المحذوف يختفي والاسترجاع يعيده`() {
        val hidden = AzkarLibrary.effectiveGroups(defaults, emptyList(), setOf("morning"))
        assertEquals(listOf(defaults[1]), hidden)
        val restored = AzkarLibrary.effectiveGroups(defaults, emptyList(), emptySet())
        assertEquals(defaults, restored)
    }

    @Test
    fun `معرفات الأقسام المضافة بادئتها cg_ وغير ذلك يرفض`() {
        assertTrue(AzkarLibrary.isCustomGroup("cg_abc"))
        assertFalse(AzkarLibrary.isCustomGroup("morning"))
        assertFalse(AzkarLibrary.isCustomGroup(""))
        // بادئة أذكار المستخدم لا تصلح لأقسامها والعكس
        assertFalse(AzkarLibrary.isCustomGroup("custom_1"))
    }

    @Test
    fun `ترميز القسم المضاف يحمله كاملا ويعود كما هو`() {
        val group = CustomGroup("cg_9", "أذكار السفر", "قسم من إضافتك — أضف أذكاره من داخل صفحته")
        assertEquals(group, AzkarLibrary.decodeCustomGroup(AzkarLibrary.encodeCustomGroup(group)!!))
    }

    @Test
    fun `عنوان القسم المضاف بفواصل وأسطر يُهرَّب ويصح`() {
        val group = CustomGroup("cg_10", "أذكار|خاصة\nبيتي", "وصف\\خاص")
        val decoded = AzkarLibrary.decodeCustomGroup(AzkarLibrary.encodeCustomGroup(group)!!)
        assertEquals(group, decoded)
    }

    @Test
    fun `القسم بلا بادئة أو بعنوان خالٍ يرفض في الترميز`() {
        assertNull(AzkarLibrary.encodeCustomGroup(CustomGroup("morning", "قسم", "")))
        assertNull(AzkarLibrary.encodeCustomGroup(CustomGroup("cg_11", "  ", "")))
    }

    @Test
    fun `مدخلات الأقسام التالفة تعود بلا قسم ولا انهيار`() {
        assertNull(AzkarLibrary.decodeCustomGroup(""))
        assertNull(AzkarLibrary.decodeCustomGroup("morning"))
        assertNull(AzkarLibrary.decodeCustomGroup("cg_12|وصف|  "))
        // بادئة ذكر المستخدم لا تصلح لقسم
        assertNull(AzkarLibrary.decodeCustomGroup("custom_1|وصف|عنوان"))
    }
}
