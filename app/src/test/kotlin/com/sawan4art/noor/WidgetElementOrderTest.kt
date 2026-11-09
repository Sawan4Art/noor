package com.sawan4art.noor

import com.sawan4art.noor.widget.WidgetCustomization
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود ترتيب عناصر الويدجت (alpha61 — المحرر التفاعلي: «قابل لتحريك
 * كل العناصر») — التطبيع يضمن أن الترتيب دائماً تبديلٌ كاملٌ لمفاتيح
 * العناصر، ودورة JSON تحفظ ما رتّبه المالك في المحرر.
 */
class WidgetElementOrderTest {

    @Test
    fun `الترتيب الافتراضي كامل وتبديل صحيح`() {
        val order = WidgetCustomization.DEFAULT_ORDER
        assertEquals(WidgetCustomization.ELEMENT_KEYS.size, order.size)
        assertEquals(order.toSet(), WidgetCustomization.ELEMENT_KEYS.toSet())
    }

    @Test
    fun `تطبيع ترتيب مقلوب يحفظه كما هو`() {
        val reversed = WidgetCustomization.DEFAULT_ORDER.reversed()
        assertEquals(reversed, WidgetCustomization.normalizeOrder(reversed))
    }

    @Test
    fun `التطبيع يرمي الغريب والمكرر ويلحق الغائب بترتيبه الافتراضي`() {
        val messy = listOf(
            "clock", "UNKNOWN", "misbaha", "clock",
        )
        val normalized = WidgetCustomization.normalizeOrder(messy)
        // ١) لا غريب ولا تكرار
        assertEquals(normalized.size, normalized.toSet().size)
        assertTrue(normalized.all { it in WidgetCustomization.ELEMENT_KEYS })
        // ٢) الترتيب المختار أولٌ ثم الغائب بترتيبه الافتراضي
        assertEquals("clock", normalized.first())
        // alpha62: مفتاح المسبحة انتهى — «الغى المسبحه في الويدجت» —
        // فيُرمى كأي مفتاحٍ غريبٍ ولا يُلحق بالقائمة
        assertTrue("misbaha" !in normalized)
        // ٣) الكل حاضر في النهاية
        assertEquals(WidgetCustomization.ELEMENT_KEYS.toSet(), normalized.toSet())
    }

    @Test
    fun `جولة JSON تحفظ الترتيب والحجم واللون`() {
        val c = WidgetCustomization.DEFAULT.copy(
            elementOrder = WidgetCustomization.normalizeOrder(
                listOf("countdown", "clock", "next", "grid", "hijri", "greg", "header"),
            ),
            elementSizes = mapOf(
                WidgetCustomization.ELEMENT_CLOCK to 250,
                WidgetCustomization.ELEMENT_GRID to 50,
            ),
            elementColors = mapOf(WidgetCustomization.ELEMENT_NEXT to "#123456"),
        )
        val back = WidgetCustomization.fromJson(c.toJson())
        assertEquals(c.elementOrder, back.elementOrder)
        assertEquals(200, back.elementSizes[WidgetCustomization.ELEMENT_CLOCK])
        assertEquals(50, back.elementSizes[WidgetCustomization.ELEMENT_GRID])
        assertEquals("#123456", back.elementColors[WidgetCustomization.ELEMENT_NEXT])
    }

    @Test
    fun `JSON بلا ترتيب يعود بالافتراضي والقديم سليم`() {
        // نسخة قديمة بلا مفتاح الترتيب — تسير بأمانٍ للافتراضي
        val legacy = JSONObject()
            .put("clock", true)
            .put("opacity", 70)
            .toString()
        val c = WidgetCustomization.fromJson(legacy)
        assertEquals(WidgetCustomization.DEFAULT_ORDER, c.elementOrder)
        assertEquals(70, c.opacityPercent)
    }

    @Test
    fun `عنصر الترويسة مفتاح معروف`() {
        assertTrue(WidgetCustomization.ELEMENT_HEADER in WidgetCustomization.ELEMENT_KEYS)
        assertTrue(WidgetCustomization.ELEMENT_HEADER in WidgetCustomization.DEFAULT_ORDER)
    }
}
