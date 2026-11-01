package com.sawan4art.noor

import com.sawan4art.noor.domain.location.CityGeocoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد سطر الموقع (توجيه المالك alpha20: «انا عايز اسم مكانى الفعلى يتكتب
 * مش اسم المدينة، مثال انا في ابو زعبل — ابو زعبل دي تابعة لمدينة اي الخانكه
 * — اسم القريه او العزبه او الكفر اولا ثم اسم المدينه التابع لها»):
 * - **القرية أولاً ثم المدينة ثم المحافظة** (الثالوث كاملاً).
 * - الدمج المرجّح: القرية من Nominatim، والمدينة الأم تُسقط إن كانت هي
 *   القرية نفسها (فيلتر Photon الذي يضع القرية في خانة city).
 * - التطبيع للمقارنة فقط (ألفات + بادئة «مدينة») — العرض باسم المصدر.
 * - المحافظة تُنظّف من بادئة «محافظة »، وطابق الاسم المحافظة فلا يُكرر.
 * - لا قرية ولا مدينة؟ المحافظة، وإلا الدولة، وإلا «موقعي الحالي».
 */
class LocationLabelTest {

    @Test
    fun `قرية مع محافظة`() {
        assertEquals(
            "الخانكة، القليوبية",
            CityGeocoder.buildLocationLabel("الخانكة", "القليوبية", "مصر"),
        )
    }

    @Test
    fun `مدينة مع محافظة بدون دولة`() {
        assertEquals(
            "طنطا، الغربية",
            CityGeocoder.buildLocationLabel("طنطا", "الغربية", null),
        )
    }

    @Test
    fun `طابق الاسم والمحافظة يعرض الاسم وحده`() {
        assertEquals("القاهرة", CityGeocoder.buildLocationLabel("القاهرة", "القاهرة", "مصر"))
    }

    @Test
    fun `لا محافظة فتعرض الدولة كما في النسخ السابقة`() {
        assertEquals("إسطنبول، تركيا", CityGeocoder.buildLocationLabel("إسطنبول", null, "تركيا"))
    }

    @Test
    fun `لا محافظة ولا دولة يعرض الاسم وحده`() {
        assertEquals("موقعي الحالي", CityGeocoder.buildLocationLabel("موقعي الحالي", null, null))
    }

    @Test
    fun `محافظة فارغة تعامل كغائبة`() {
        assertEquals("عمّان، الأردن", CityGeocoder.buildLocationLabel("عمّان", "  ", "الأردن"))
    }

    @Test
    fun `تنظيف بادئة محافظة العربية`() {
        assertEquals("القليوبية", CityGeocoder.cleanGovernorate("محافظة القليوبية"))
    }

    @Test
    fun `تنظيف بادئة محافظة بلا مسافة`() {
        assertEquals("الجيزة", CityGeocoder.cleanGovernorate("محافظةالجيزة"))
    }

    @Test
    fun `تنظيف بادئة إنجليزية`() {
        assertEquals("Qalyubia", CityGeocoder.cleanGovernorate("Muhafazat Qalyubia"))
        assertEquals("Qalyubia", CityGeocoder.cleanGovernorate("Governorate of Qalyubia"))
    }

    @Test
    fun `اسم نظيف يمر كما هو`() {
        assertEquals("الإسكندرية", CityGeocoder.cleanGovernorate("الإسكندرية"))
    }

    @Test
    fun `نص فارغ يعيد null لا نصاً فارغاً`() {
        assertNull(CityGeocoder.cleanGovernorate(""))
        assertNull(CityGeocoder.cleanGovernorate("   "))
        assertNull(CityGeocoder.cleanGovernorate("محافظة "))
    }

    @Test
    fun `المدن المدمجة لها محافظات مصرية والتنسيق لا يكرر الاسم`() {
        val cairo = CityGeocoder.KNOWN_CITIES.first { it.name == "القاهرة" }
        assertEquals("القاهرة", cairo.region)
        assertEquals(
            "القاهرة",
            CityGeocoder.buildLocationLabel(cairo.name, cairo.region, null),
        )
        val giza = CityGeocoder.KNOWN_CITIES.first { it.name == "الجيزة" }
        assertEquals("الجيزة", giza.region)
    }

    // ===== عقد alpha20: القرية أولاً ثم المدينة ثم المحافظة =====

    /** نتيجة Nominatim النمطية لقرية (zoom=14): قرية + محافظة بلا مدينة أم */
    private val nom = CityGeocoder.CityResult(
        village = "ابو زعبل البلد", city = null,
        region = "القليوبية", country = "مصر",
    )

    @Test
    fun `الثالوث الكامل - قرية ثم مدينة ثم محافظة`() {
        assertEquals(
            "ابو زعبل البلد، الخانكة، القليوبية",
            CityGeocoder.buildLocationLabel("ابو زعبل البلد", "الخانكة", "القليوبية", "مصر"),
        )
    }

    @Test
    fun `قرية بلا مدينة فالمحافظة تليها مباشرة`() {
        assertEquals(
            "أبو زعبل، القليوبية",
            CityGeocoder.buildLocationLabel("أبو زعبل", null, "القليوبية", null),
        )
    }

    @Test
    fun `المدينة المطابقة للقرية تطبعا تُسقط`() {
        // Photon يضع القرية نفسها في خانة city — يجب ألا يصير «أبو زعبل، أبو زعبل»
        assertEquals(
            "ابو زعبل البلد، القليوبية",
            CityGeocoder.buildLocationLabel(
                "ابو زعبل البلد", "ابو زعبل البلد", "القليوبية", null,
            ),
        )
    }

    @Test
    fun `توحيد الألفات وبادئة مدينة في مقارنة التكرار`() {
        assertEquals(
            "أبو زعبل، القليوبية",
            CityGeocoder.buildLocationLabel("أبو زعبل", "ابو زعبل", "القليوبية", null),
        )
        // التطبيع للمقارنة فقط — العرض يحفظ اسم المصدر كما هو
        assertEquals(
            "مدينة بلبيس، الشرقية",
            CityGeocoder.buildLocationLabel(null, "مدينة بلبيس", "الشرقية", null),
        )
        assertEquals(
            CityGeocoder.normalizeForCompare("مدينة بلبيس"),
            CityGeocoder.normalizeForCompare("بلبيس"),
        )
    }

    @Test
    fun `الدمج المرجح - القرية من نوميناتيم والمدينة الأم من بيج داتا كلاود`() {
        val photon = CityGeocoder.CityResult(
            village = null, city = "ابو زعبل البلد",
            region = "القليوبية", country = "مصر",
        )
        val bdc = CityGeocoder.CityResult(
            village = "الخانكة", city = "الخانكة",
            region = "القليوبية", country = "مصر",
        )
        val merged = CityGeocoder().mergeResults(listOf(0 to nom, 1 to photon, 2 to bdc))
        assertEquals("ابو زعبل البلد", merged?.village)
        // مدينة photon هي القرية نفسها فتُسقط، والخانكة من بيج داتا كلاود تلحق
        assertEquals("الخانكة", merged?.city)
        assertEquals("القليوبية", merged?.region)
        assertEquals("مصر", merged?.country)
        assertTrue("الثالوث مكتمل يسمح بالعودة المبكرة", merged?.isComplete == true)
        assertEquals(
            "ابو زعبل البلد، الخانكة، القليوبية",
            CityGeocoder.buildLocationLabel(
                merged?.village, merged?.city, merged?.region, merged?.country,
            ),
        )
    }

    @Test
    fun `الدمج المرجح - ما توفر يُعرض إن لم يكتمل الثالوث`() {
        val photon = CityGeocoder.CityResult(city = "بنها", region = null, country = null)
        val merged = CityGeocoder().mergeResults(listOf(1 to photon, 0 to null))
        assertEquals("بنها", merged?.city)
        assertFalse("بلا قرية ومحافظة: غير مكتمل", merged?.isComplete == true)
        assertTrue("فيه اسم يُعرض", merged?.hasAny == true)
    }

    @Test
    fun `الدمج بلا نتائج يعيد null`() {
        assertNull(CityGeocoder().mergeResults(listOf(0 to null, 1 to null, 2 to null)))
    }
}
