package com.sawan4art.noor.domain.tasbih

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * رياضيات تناثر قطرات المسبحة — نقيّ بلا أندرويد كي يُختبر آلياً.
 *
 * سلوك السائل التفاعلي (توجيه المالك alpha25): «مع كل ضغطه يتناثر نقاط
 * صغير و متوسطه و باحجام مختلفه و عشوائيا من النقطه السائله و كلما زادت
 * سرعه الضغط تزداد معها كميه السوائل المتناثره منها» — فعدد القطرات
 * دالة رتيبة في معدل النقر بسقفٍ محدود، وأحجامها عشوائية صغيرة ومتوسطة.
 */
object MisbahaSplashMath {

    /** سقف القطرات الحية في اللحظة — حماية للأداء لا للشكل */
    const val MAX_LIVE_PARTICLES = 48

    /** قطرات أبطأ نقرة — السائل لا يبقى صامتاً مع الضغط */
    const val BASE_PARTICLES = 3

    /** سقف قطرات التسبيحة الواحدة مهما بلغت سرعة النقر */
    const val MAX_TAP_PARTICLES = 12

    /** نافذة قياس سرعة النقر بالميلي ثانية */
    const val RATE_WINDOW_MS = 2000L

    /**
     * معدل النقر داخل النافذة الأخيرة — نقرة/ثانية.
     * نقاط أقدم من النافذة تُهمل، والقسمة على الثواني الحقيقية للنافذة.
     */
    fun tapsPerSecond(times: Collection<Long>, nowMs: Long, windowMs: Long = RATE_WINDOW_MS): Float {
        if (windowMs <= 0) return 0f
        val recent = times.count { nowMs - it in 0..windowMs }
        return recent * 1000f / windowMs
    }

    /**
     * عدد قطرات التناثر لتسبيحة — رتيب تصاعدي مع معدل النقر:
     * ٣ قطرات أساس + كلفة السرعة حتى سقف ١٢، فكلما زادت سرعة الضغط
     * زادت كمية السوائل المتناثرة (بطلب المالك حرفياً).
     */
    fun tapParticles(tapsPerSecond: Float): Int {
        val rate = tapsPerSecond.coerceIn(0f, 6f)
        val extra = (rate * 1.5f).toInt()
        return (BASE_PARTICLES + extra).coerceAtMost(MAX_TAP_PARTICLES)
    }

    /** قطرة تناثر حية — موضع وسرعة وحجم وعمر (1 → 0) */
    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val radius: Float,
        var life: Float,
        val decayPerSecond: Float,
    )

    /**
     * توليد قطرات التناثر من سطح النقطة السائلة: زوايا واتجاهات عشوائية
     * كاملة، بأحجام عشوائية صغيرة ومتوسطة (4.5%..10.5% من نصف القطر)،
     * وسرعة خارجية مع انحيازٍ رأسي طفوفاً كالماء المرشوش، وعمر 0.6..1.1 ثانية.
     */
    fun spawn(
        count: Int,
        cx: Float,
        cy: Float,
        blobR: Float,
        random: Random = Random.Default,
    ): List<Particle> {
        val n = count.coerceIn(0, MAX_LIVE_PARTICLES)
        if (n == 0 || blobR <= 0f) return emptyList()
        return List(n) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = blobR * (0.9f + random.nextFloat() * 0.8f)
            val r = blobR * (0.045f + random.nextFloat() * 0.06f)
            Particle(
                x = cx + cos(angle) * blobR * 0.95f,
                y = cy + sin(angle) * blobR * 0.95f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - blobR * 0.35f, // طفو أولي لأعلى ثم جاذبية
                radius = r,
                life = 1f,
                decayPerSecond = 1f / (0.6f + random.nextFloat() * 0.5f),
            )
        }
    }

    /** جاذبية القطرات كنسبة من نصف قطر النقطة — قوس ماء يعود للأسفل */
    const val GRAVITY_FACTOR = 1.6f

    /** تقدم القطرة زمنياً: موضع وسرعة وعمر — [dt] بالثواني */
    fun advance(p: Particle, dtSeconds: Float, blobR: Float) {
        if (dtSeconds <= 0f) return
        p.vy += blobR * GRAVITY_FACTOR * dtSeconds
        p.x += p.vx * dtSeconds
        p.y += p.vy * dtSeconds
        p.life = (p.life - p.decayPerSecond * dtSeconds).coerceAtLeast(0f)
    }

    /** موجة نشاط السائل بعد ضغطة — تُضخّ موجات energy في addLiquidBlob (سقف 1) */
    fun bumpEnergy(current: Float, amount: Float = 0.4f): Float =
        (current + amount).coerceAtMost(1f)

    /** ذبول النشاط بين الإطارات — يهبط نحو الصفر ولا ينزل عنه */
    fun decayEnergy(current: Float, dtSeconds: Float, rate: Float = 0.45f): Float =
        (current - rate * dtSeconds).coerceAtLeast(0f)
}
