package com.sawan4art.noor

import com.sawan4art.noor.domain.tasbih.MisbahaSplashMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود تناثر السائل (توجيه المالك alpha25): «مع كل ضغطه يتناثر نقاط
 * صغير و متوسطه و باحجام مختلفه و عشوائيا من النقطه السائله و كلما زادت
 * سرعه الضغط تزداد معها كميه السوائل المتناثره منها» — كثافة رتيبة
 * بسقف، وأحجام عشوائية محصورة، وفيزياء قطرات سليمة.
 */
class MisbahaSplashMathTest {

    // ===== كثافة التناثر دالة في سرعة النقر =====

    @Test
    fun `أبطأ نقرة لها قطرات أساس لا صمت`() {
        assertEquals(
            MisbahaSplashMath.BASE_PARTICLES,
            MisbahaSplashMath.tapParticles(0f),
        )
        assertTrue(MisbahaSplashMath.BASE_PARTICLES >= 1)
    }

    @Test
    fun `الكثافة رتيبة تصاعدية مع سرعة النقر`() {
        var prev = -1
        for (rate in listOf(0f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f)) {
            val n = MisbahaSplashMath.tapParticles(rate)
            assertTrue("رتيبة لا تنقص مع السرعة", n >= prev)
            prev = n
        }
    }

    @Test
    fun `سقف القطرات للتسبيحة الواحدة محفوظ مهما بلغت السرعة`() {
        assertEquals(
            MisbahaSplashMath.MAX_TAP_PARTICLES,
            MisbahaSplashMath.tapParticles(100f),
        )
        // معدل سالب (مدخل شاذ) يعامَل كالصفر — قطرات الأساس لا السقف
        assertEquals(
            MisbahaSplashMath.BASE_PARTICLES,
            MisbahaSplashMath.tapParticles(-5f),
        )
    }

    @Test
    fun `معدل النقر داخل النافذة يحصي الحديثة ويهمل القديمة`() {
        val now = 10_000L
        val times = listOf(now - 100, now - 800, now - 1900, now - 2100, now - 9000)
        val rate = MisbahaSplashMath.tapsPerSecond(times, now)
        // ثلاث داخل نافذة الثانيتين → 3/2 = 1.5 نقرة/ثانية
        assertEquals(1.5f, rate, 1e-4f)
    }

    // ===== توليد القطرات =====

    @Test
    fun `القطرات المولدة بأحجام ومواضع محصورة حول النقطة`() {
        val blobR = 120f
        val drops = MisbahaSplashMath.spawn(12, 0f, 0f, blobR)
        assertEquals(12, drops.size)
        for (p in drops) {
            // حجم صغير أو متوسط فقط — بين ٤٪ و١١٪ من نصف قطر النقطة
            assertTrue("حجم محصور", p.radius in blobR * 0.04f..blobR * 0.11f)
            // تولد من سطح النقطة لا من بعيد ولا من داخلها العميق
            val dist = kotlin.math.sqrt(p.x * p.x + p.y * p.y)
            assertTrue("من السطح", dist <= blobR * 1.05f && dist >= blobR * 0.8f)
            assertEquals(1f, p.life, 1e-6f)
        }
    }

    @Test
    fun `سقف القطرات الحية يحمي من الطوفان`() {
        val many = MisbahaSplashMath.spawn(999, 0f, 0f, 100f)
        assertEquals(MisbahaSplashMath.MAX_LIVE_PARTICLES, many.size)
    }

    @Test
    fun `صفر قطرات ونقطة معدومة يعيدان قائمة فارغة بلا انهيار`() {
        assertTrue(MisbahaSplashMath.spawn(0, 0f, 0f, 100f).isEmpty())
        assertTrue(MisbahaSplashMath.spawn(5, 0f, 0f, 0f).isEmpty())
    }

    // ===== فيزياء القطرة =====

    @Test
    fun `القطرة تتقدم بموضعها وسرعتها الرأسية تزداد بالجاذبية`() {
        val blobR = 100f
        val p = MisbahaSplashMath.Particle(
            x = 0f, y = 0f, vx = 50f, vy = -80f,
            radius = 5f, life = 1f, decayPerSecond = 1f,
        )
        val dt = 0.1f
        MisbahaSplashMath.advance(p, dt, blobR)
        assertEquals(50f * dt, p.x, 1e-3f)
        // أويلر شبه الضمني: الجاذبية تُحدّث السرعة أولاً ثم الموضع بها
        // — ترتيب أكثر ثباتاً عددياً لقطرات الماء
        assertEquals(-80f + blobR * MisbahaSplashMath.GRAVITY_FACTOR * dt, p.vy, 1e-3f)
        assertEquals((-80f + blobR * MisbahaSplashMath.GRAVITY_FACTOR * dt) * dt, p.y, 1e-3f)
        assertEquals(0.9f, p.life, 1e-4f)
    }

    @Test
    fun `العمر لا ينزل عن الصفر والصفر زمن لا يحرك شيئاً`() {
        val p = MisbahaSplashMath.Particle(0f, 0f, 0f, 0f, 1f, 0.01f, 5f)
        MisbahaSplashMath.advance(p, 1f, 100f)
        assertEquals(0f, p.life, 1e-6f)
        val q = MisbahaSplashMath.Particle(10f, 10f, 30f, 30f, 1f, 1f, 1f)
        MisbahaSplashMath.advance(q, 0f, 100f)
        assertEquals(10f, q.x, 1e-6f)
        assertEquals(1f, q.life, 1e-6f)
    }

    // ===== موجة نشاط السائل =====

    @Test
    fun `النشاط يُضخّ مع الضغط بسقف واحد ويتذبل نحو الصفر`() {
        var e = 0f
        e = MisbahaSplashMath.bumpEnergy(e)
        assertEquals(0.4f, e, 1e-6f)
        repeat(10) { e = MisbahaSplashMath.bumpEnergy(e) }
        assertEquals("سقف النشاط", 1f, e, 1e-6f)
        e = MisbahaSplashMath.decayEnergy(e, 0.5f)
        assertTrue(e < 1f && e > 0f)
        e = MisbahaSplashMath.decayEnergy(e, 100f)
        assertEquals("أرضية الصفر", 0f, e, 1e-6f)
    }

    @Test
    fun `الجاذبية نسبة ثابتة معقولة من نصف القطر`() {
        assertTrue(
            "جاذبية ملموسة لا طائشة",
            MisbahaSplashMath.GRAVITY_FACTOR in 0.5f..4f,
        )
        assertEquals(1.6f, MisbahaSplashMath.GRAVITY_FACTOR, 1e-6f)
    }
}
