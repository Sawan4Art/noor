package com.sawan4art.noor

import com.sawan4art.noor.domain.location.CityGeocoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * عقد سطر الموقع (توجيه المالك alpha19: «الموقع بيجيب اسم المدينة بس انا
 * عايزه يجيب اسم القريه ان توفر و اسم المحافظه»):
 * - القرية/المدينة أولاً ثم المحافظة.
 * - المحافظة تُنظّف من بادئة «محافظة ».
 * - طابق الاسم المحافظة فلا يُكرر («القاهرة، القاهرة» قبيحة — «القاهرة» تكفي).
 * - لا محافظة؟ الدولة تلعب دورها كما في النسخ السابقة.
 * - لا شيء من الاثنين؟ الاسم وحده.
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
}
