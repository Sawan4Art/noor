package com.sawan4art.noor

import com.sawan4art.noor.domain.location.LocationGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد بوابات تحديد الموقع (alpha18) — توجيه المالك: عند غلق الإنترنت
 * وخدمة الموقع يجب أن تظهر رسالة تفيدهما فوراً بلا لاج ولا صمت.
 */
class LocationGateTest {

    @Test
    fun `كل البوابات مفتوحة — لا حجب ولا رسالة`() {
        val gate = LocationGate(hasPermission = true, serviceOn = true, online = true)
        assertFalse(gate.blocked)
        assertEquals(null, gate.message)
        assertEquals(LocationGate.Action.NONE, gate.action)
    }

    @Test
    fun `الموقع والانترنت مقفولان — رسالة مزدوجة وإجراء الموقع`() {
        val gate = LocationGate(hasPermission = true, serviceOn = false, online = false)
        assertTrue(gate.blocked)
        assertTrue("الرسالة تذكر الاثنين", gate.message!!.contains("الموقع") && gate.message!!.contains("الإنترنت"))
        assertEquals(LocationGate.Action.OPEN_LOCATION_SETTINGS, gate.action)
    }

    @Test
    fun `خدمة الموقع مقفولة وحدها — رسالة خاصة بها`() {
        val gate = LocationGate(hasPermission = true, serviceOn = false, online = true)
        assertTrue(gate.message!!.contains("خدمة الموقع"))
        assertFalse(gate.message!!.contains("الإنترنت"))
        assertEquals(LocationGate.Action.OPEN_LOCATION_SETTINGS, gate.action)
    }

    @Test
    fun `الانترنت مقفول وحده — رسالة خاصة به`() {
        val gate = LocationGate(hasPermission = true, serviceOn = true, online = false)
        assertTrue(gate.message!!.contains("الإنترنت"))
        assertEquals(LocationGate.Action.OPEN_INTERNET_SETTINGS, gate.action)
    }

    @Test
    fun `الاذن مفقود — إجراء إعدادات التطبيق`() {
        val gate = LocationGate(hasPermission = false, serviceOn = true, online = true)
        assertTrue(gate.message!!.contains("إذن الموقع"))
        assertEquals(LocationGate.Action.OPEN_APP_SETTINGS, gate.action)
    }

    @Test
    fun `الاذن مفقود وكل شيء مقفول — الإذن أولاً في الرسالة والإجراء`() {
        val gate = LocationGate(hasPermission = false, serviceOn = false, online = false)
        assertTrue(gate.message!!.contains("إذن الموقع"))
        assertEquals(LocationGate.Action.OPEN_APP_SETTINGS, gate.action)
    }

    @Test
    fun `فشل القراءة رغم فتح البوابات — الرسالة القسرية بلا إجراء`() {
        val gate = LocationGate.READING_FAILED
        assertTrue(gate.message!!.contains("لم يتمكن التطبيق"))
        assertEquals(LocationGate.Action.NONE, gate.action)
        assertEquals("", gate.actionLabel)
    }

    @Test
    fun `لكل حالة محجوبة زر إجراء بتسمية عربية`() {
        listOf(
            LocationGate(false, false, false),
            LocationGate(true, false, false),
            LocationGate(true, false, true),
            LocationGate(true, true, false),
            LocationGate(false, true, true),
        ).forEach { gate ->
            assertTrue(gate.blocked)
            assertTrue(gate.message!!.isNotBlank())
            assertTrue("تسمية الإجراء ${gate.action}", gate.actionLabel.isNotBlank())
        }
    }
}
