package com.sawan4art.noor

import com.sawan4art.noor.data.update.UpdateChecker
import com.sawan4art.noor.domain.location.ManualLocations
import com.sawan4art.noor.domain.khutbah.KhutbahLibrary
import com.sawan4art.noor.widget.WidgetCustomization
import com.sawan4art.noor.widget.WidgetPresets
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات دفعة 1.70 (بلاغات المالك 2026-11-11): الأشكال الستة
 * وإطارات العناصر والتحديد اليدوي واختيار ملف التحديث بنكهته
 * وقالبا المقدمة والخاتمة.
 */
class Release170Test {

    // ===== الأشكال الستة =====

    @Test
    fun `الأشكال الستة بأرقام متتالية وتخصيصات سليمة`() {
        assertEquals(6, WidgetPresets.ALL.size)
        assertEquals((1..6).toList(), WidgetPresets.ALL.map { it.index })
        WidgetPresets.ALL.forEach { preset ->
            // كل تخصيصٍ يمر دورة JSON سليمة
            val parsed = WidgetCustomization.fromJson(preset.customization.toJson())
            assertEquals(preset.customization, parsed)
            // وترتيب عناصره تبديلٌ كاملٌ لمفاتيح العناصر (المجموعة نفسها)
            assertEquals(
                WidgetCustomization.ELEMENT_KEYS.toSet(),
                WidgetCustomization.normalizeOrder(parsed.elementOrder).toSet(),
            )
        }
    }

    @Test
    fun `الشكل الثاني ساعة تناظرية والثالث بلا تواريخ والخامس داكن`() {
        assertEquals(
            WidgetCustomization.ClockStyle.ANALOG,
            WidgetPresets.byIndex(2).customization.clockStyle,
        )
        assertTrue(!WidgetPresets.byIndex(3).customization.showHijri)
        assertTrue(!WidgetPresets.byIndex(3).customization.showGregorian)
        assertEquals(
            WidgetCustomization.Theme.DARK,
            WidgetPresets.byIndex(5).customization.theme,
        )
        assertEquals(
            WidgetCustomization.Theme.LIGHT,
            WidgetPresets.byIndex(6).customization.theme,
        )
        // الشكل الغريب يعود للأول بأمان
        assertEquals(1, WidgetPresets.byIndex(99).index)
    }

    // ===== إطارات العناصر =====

    @Test
    fun `إطارات العناصر تمر دورة JSON وتقبل الألوان والبلا والغريب يرمى`() {
        val c = WidgetCustomization.DEFAULT.copy(
            elementFrames = mapOf(
                WidgetCustomization.ELEMENT_CLOCK to "#2FA48C",
                WidgetCustomization.ELEMENT_GRID to WidgetCustomization.FRAME_NONE,
            ),
        )
        val parsed = WidgetCustomization.fromJson(c.toJson())
        assertEquals("#2FA48C", parsed.elementFrames[WidgetCustomization.ELEMENT_CLOCK])
        assertEquals(
            WidgetCustomization.FRAME_NONE,
            parsed.elementFrames[WidgetCustomization.ELEMENT_GRID],
        )
        // الغريب و«auto» وغيرها يُرمى فلا إطار له
        val raw = """
            {"elementFrames":{"clock":"#GGGGGG","grid":"auto","next":"#B04A3A"}}
        """.trimIndent()
        val parsed2 = WidgetCustomization.fromJson(raw)
        assertNull(parsed2.elementFrames[WidgetCustomization.ELEMENT_CLOCK])
        assertNull(parsed2.elementFrames[WidgetCustomization.ELEMENT_GRID])
        assertEquals("#B04A3A", parsed2.elementFrames[WidgetCustomization.ELEMENT_NEXT])
    }

    // ===== التحديد اليدوي للموقع =====

    @Test
    fun `بيانات القوائم اليدوية سليمة البنية بلا شواذ`() {
        val problems = ManualLocations.integrityProblems()
        assertTrue("مشاكل البيانات: $problems", problems.isEmpty())
        // مصر ٢٧ محافظة والسعودية ١٣ منطقة — بأمر المالك «كل دوله حسب مسميتها»
        assertEquals(27, ManualLocations.country("مصر")?.divisions?.size)
        assertEquals(13, ManualLocations.country("السعودية")?.divisions?.size)
        assertEquals("ولاية", ManualLocations.country("أمريكا")?.divisionLabel)
        assertEquals("إمارة", ManualLocations.country("الإمارات")?.divisionLabel)
        assertTrue(ManualLocations.country("دولة غير موجودة") == null)
    }

    @Test
    fun `قري أبو زعبل تجد في محافظة القليوبية`() {
        val country = ManualLocations.country("مصر")!!
        val qalyubia = ManualLocations.division(country, "القليوبية")!!
        assertTrue(qalyubia.cities.contains("أبو زعبل"))
    }

    @Test
    fun `نص الترميز وسطر الموقع بترتيب الدقة من الأدق فالأعم`() {
        assertEquals(
            "أبو زعبل، الخانكة، القليوبية، مصر",
            ManualLocations.geocodeQuery("مصر", "القليوبية", "الخانكة", "أبو زعبل"),
        )
        assertEquals(
            "القاهرة، مصر",
            ManualLocations.geocodeQuery("مصر", null, "القاهرة", null),
        )
        assertEquals(
            "أبو زعبل، الخانكة، القليوبية، مصر",
            ManualLocations.locationLabel("مصر", "القليوبية", "الخانكة", "أبو زعبل"),
        )
        assertEquals("القاهرة، مصر", ManualLocations.locationLabel("مصر", null, "القاهرة"))
    }

    // ===== قالبا المقدمة والخاتمة =====

    @Test
    fun `قالبا المقدمة والخاتمة جاهزان غير فارغين وموثقان`() {
        assertTrue(KhutbahLibrary.INTRO_TEMPLATE.contains("﴿"))
        assertTrue(KhutbahLibrary.CONCLUSION_TEMPLATE.isNotBlank())
        assertTrue(KhutbahLibrary.ORDERED.size == 50)
    }

    // ===== اختيار ملف التحديث — القناة العادية الواحدة =====

    /** JSON إصدارٍ بأصلين — مستقرة وألفا (إصدارات 1.70→1.72 القديمة) */
    private fun dualAssetRelease(tag: String, alphaFirst: Boolean): String {
        val stable = org.json.JSONObject()
            .put("name", "noor-almuslim-$tag.apk")
            .put("browser_download_url", "https://x/$tag-stable.apk")
            .put("size", 111L)
        val alpha = org.json.JSONObject()
            .put("name", "noor-almuslim-$tag-alpha.apk")
            .put("browser_download_url", "https://x/$tag-alpha.apk")
            .put("size", 222L)
        val assets = JSONArray()
        if (alphaFirst) {
            assets.put(alpha).put(stable)
        } else {
            assets.put(stable).put(alpha)
        }
        return JSONArray()
            .put(
                org.json.JSONObject()
                    .put("tag_name", tag)
                    .put("name", "نور المسلم $tag")
                    .put("body", "x")
                    .put("prerelease", false)
                    .put("draft", false)
                    .put("assets", assets),
            )
            .toString()
    }

    @Test
    fun `إصدار بأصلين يتخذ أصل المستقرة أياً كان ترتيبه`() {
        // بلاغ المالك v1.73: تطبيقٌ واحدٌ بملفٍ واحدٍ — من إصدارٍ قديمٍ
        // بأصلين يُعرض دائماً أصل المستقرة فلا يُنزَّل ملفُّ حزمةٍ منفصلة
        val stableFirst = UpdateChecker.parseReleases(dualAssetRelease("v1.71", alphaFirst = false))
        assertEquals("noor-almuslim-v1.71.apk", stableFirst.first().apkName)
        val alphaFirst = UpdateChecker.parseReleases(dualAssetRelease("v1.71", alphaFirst = true))
        assertEquals("noor-almuslim-v1.71.apk", alphaFirst.first().apkName)
    }

    @Test
    fun `إصدار بملف واحد يتخذ به كما هو`() {
        val json = """
            [{"tag_name":"v1.73","name":"x","body":"x","prerelease":false,
              "draft":false,"assets":[{"name":"noor-almuslim-v1.73.apk",
              "browser_download_url":"https://x/a.apk","size":1}]}]
        """.trimIndent()
        assertEquals(
            "noor-almuslim-v1.73.apk",
            UpdateChecker.parseReleases(json).first().apkName,
        )
    }

    @Test
    fun `إصدار قديم بملف ألفا وحيد يتخذ به احتياطاً`() {
        val json = """
            [{"tag_name":"v0.5.0-alpha64","name":"x","body":"x","prerelease":true,
              "draft":false,"assets":[{"name":"noor-almuslim-v0.5.0-alpha64.apk",
              "browser_download_url":"https://x/a.apk","size":1}]}]
        """.trimIndent()
        assertEquals(
            "noor-almuslim-v0.5.0-alpha64.apk",
            UpdateChecker.parseReleases(json).first().apkName,
        )
    }
}
