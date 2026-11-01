package com.sawan4art.noor

import com.sawan4art.noor.domain.qibla.QiblaMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود رياضيات بوصلة القبلة (alpha29 توجيه المالك: النقطة السائلة
 * الحية بسهمها ودرجتها وتناثرها عند الاستقامة) — نقية قابلة للاختبار.
 */
class QiblaMathTest {

    // ===== التطبيع =====

    @Test
    fun `التطبيع يرجع الزوايا السالبة والماكسية للدائرة`() {
        assertEquals(270f, QiblaMath.normalise(-90f), 1e-4f)
        assertEquals(90f, QiblaMath.normalise(450f), 1e-4f)
        assertEquals(0f, QiblaMath.normalise(360f), 1e-4f)
        assertEquals(0f, QiblaMath.normalise(0f), 1e-4f)
        assertEquals(359.5f, QiblaMath.normalise(-0.5f), 1e-4f)
    }

    // ===== الفارق الموقَّع =====

    @Test
    fun `الفارق الموقع يقطع الدائرة من أقصر طريق`() {
        // القبلة ١٠ والمستخدم ٣٥٠ — القبلة على يمينه بعشرين درجة
        assertEquals(20f, QiblaMath.signedDiff(10f, 350f), 1e-4f)
        // والعكس يساراً بعشرين
        assertEquals(-20f, QiblaMath.signedDiff(350f, 10f), 1e-4f)
        assertEquals(47f, QiblaMath.signedDiff(137f, 90f), 1e-4f)
        assertEquals(180f, QiblaMath.signedDiff(180f, 0f), 1e-4f)
        assertEquals(0f, QiblaMath.signedDiff(137f, 137f), 1e-4f)
    }

    // ===== الاستقامة =====

    @Test
    fun `الاستقامة داخل اربع درجات من كل الجهات`() {
        assertTrue(QiblaMath.isAligned(4f))
        assertTrue(QiblaMath.isAligned(-4f))
        assertTrue(QiblaMath.isAligned(0f))
        assertFalse(QiblaMath.isAligned(4.1f))
        assertFalse(QiblaMath.isAligned(-35f))
    }

    @Test
    fun `اتجاه الاستدرار — يمين للموجب ويسار للسالب وصفر للمستقيم`() {
        assertEquals(0, QiblaMath.rotateDirection(2f))
        assertEquals(1, QiblaMath.rotateDirection(10f))
        assertEquals(-1, QiblaMath.rotateDirection(-10f))
    }

    // ===== السهم =====

    @Test
    fun `زاوية السهم فارق القبلة عن اتجاه الجهاز`() {
        assertEquals(0f, QiblaMath.arrowAngle(137f, 137f), 1e-4f)
        assertEquals(47f, QiblaMath.arrowAngle(137f, 90f), 1e-4f)
        assertEquals(-113f, QiblaMath.arrowAngle(137f, 250f), 1e-4f)
    }

    @Test
    fun `رأس السهم يبدأ من الأعلى ويدور مع عقارب الساعة`() {
        val (x0, y0) = QiblaMath.arrowTip(0f, 100f)
        assertEquals(0f, x0, 1e-3f)
        assertEquals(-100f, y0, 1e-3f)
        val (x90, y90) = QiblaMath.arrowTip(90f, 100f)
        assertEquals(100f, x90, 1e-3f)
        assertEquals(0f, y90, 1e-3f)
    }

    // ===== التنعيم الدائري =====

    @Test
    fun `القراءة الأولى تتبنى كما هي`() {
        assertEquals(137f, QiblaMath.smoothHeading(-1f, 137f), 1e-4f)
    }

    @Test
    fun `التنعيم يمر بخط الشمال بسلاسة لا قفزة`() {
        // من ٣٥٨ إلى ٢ — العبرة عبر الصفر مركّز على الأقواس القصيرة
        assertEquals(358f + 4f * 0.25f, QiblaMath.smoothHeading(358f, 2f), 1e-4f)
        // ومن ٢ إلى ٣٥٨ بالعكس
        assertEquals(2f - 4f * 0.25f, QiblaMath.smoothHeading(2f, 358f), 1e-4f)
    }

    @Test
    fun `التنعيم بعامل كامل يتبنى القراءة الجديدة فورا`() {
        assertEquals(137f, QiblaMath.smoothHeading(90f, 137f, 1f), 1e-4f)
        // وعامل الصفر يجمّد على القديمة
        assertEquals(90f, QiblaMath.smoothHeading(90f, 137f, 0f), 1e-4f)
    }

    // ===== التناثر عند الاستقامة =====

    @Test
    fun `دخول الاستقامة رشة اقصى والبقاء عليها تناثر مستمر وتركها هدوء`() {
        assertEquals(QiblaMath.ALIGN_ENTRY_PARTICLES, QiblaMath.alignedParticles(false, true))
        assertEquals(QiblaMath.ALIGN_HOLD_PARTICLES, QiblaMath.alignedParticles(true, true))
        assertEquals(0, QiblaMath.alignedParticles(true, false))
        assertEquals(0, QiblaMath.alignedParticles(false, false))
    }
}
