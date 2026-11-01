package com.sawan4art.noor.domain.qibla

import kotlin.math.abs

/**
 * رياضيات بوصلة القبلة — نقية بلا أندرويد كي تُختبر آلياً بعقود صارمة
 * (نمط AzkarMath وMisbahaMath).
 *
 * سلوك النقطة السائلة الحية (توجيه المالك alpha29: «شكل البوصله هيكون
 * نقطه سائله حيه مثل نقطه التسبيح و يكون بداخلها سهم و اتجاهات و رقم
 * درجه الاتجاه .. لما المستخدم يتجه الى الاتجاه الصحيح تبدا النقطه
 * الحيه بتناثر اجزاء منها مثل الموجوده في المسبحه»):
 *
 * - السهم يدور داخل النقطة بزاوية = اتجاه القبلة − اتجاه الجهاز،
 *   فيستقيم للأعلى تحديداً عند مواجهة القبلة.
 * - بلوغ الاستقامة (± ٤°) يفتح التفريج: النقطة تنثر قطراتها كالمسبحة.
 * - اتجاه الجهاز الوارد من المستشعر يُناعَم دائرياً كي لا يقفز العرض
 *   عند خط ٣٦٠→٠.
 */
object QiblaMath {

    /** سماحية الاستقامة — أقل من هذه الزاوية تعتبر مواجهاً للقبلة */
    const val ALIGN_TOLERANCE_DEG = 4f

    /** سماحية التنعيم الدائري — كل قراءة مستشعر تمتزج بهذه النسبة */
    const val HEADING_SMOOTH_FACTOR = 0.25f

    /** قطرات التفريج عند دخول الاستقامة — رشّة أقصى كاكتمال قسم المسبحة */
    const val ALIGN_ENTRY_PARTICLES = 12

    /** قطرات التناثر المستمر أثناء البقاء على القبلة */
    const val ALIGN_HOLD_PARTICLES = 3

    /**
     * تطبيع زاوية إلى [0، 360) — السالب يعود موجباً والماكس يعود صفراً.
     */
    fun normalise(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }

    /**
     * الفارق الموقَّع من [heading] إلى [target] على دائرة الاتجاهات —
     * في (-180، 180]: موجب يعني القبلة على يمين المستخدم (استدر يميناً)
     * وسالب يعني على يساره.
     */
    fun signedDiff(target: Float, heading: Float): Float {
        var d = (target - heading) % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        return d
    }

    /** هل المستخدم مواجٍ للقبلة الآن؟ داخل ± سماحية الاستقامة */
    fun isAligned(signedDiff: Float, tolerance: Float = ALIGN_TOLERANCE_DEG): Boolean =
        abs(signedDiff) <= tolerance

    /** زاوية دوران السهم داخل النقطة — صفر يعني السهم للأعلى */
    fun arrowAngle(qiblaBearing: Float, heading: Float): Float =
        signedDiff(qiblaBearing, heading)

    /**
     * اتجاه الاستدرار النصي: ١ يعني استدر يميناً، و−١ يساراً، وصفر
     * أنت على القبلة (وفق الفارق الموقَّع).
     */
    fun rotateDirection(signedDiff: Float): Int =
        if (isAligned(signedDiff)) 0 else if (signedDiff > 0f) 1 else -1

    /**
     * تنعيم دائري لقراءة البوصلة — مزج زاوية جديدة مع السابقة على
     * الدائرة نفسها (لا يهرب عبر خط ٣٥٩→١): كل قراءة تبتلع النسبة
     * [factor] من المسافة القوسية القصيرة نحو القراءة الجديدة.
     * قراءة أولى (prev سالبة) تُتّبنى كما هي.
     */
    fun smoothHeading(prev: Float, next: Float, factor: Float = HEADING_SMOOTH_FACTOR): Float {
        val f = factor.coerceIn(0f, 1f)
        if (prev < 0f) return normalise(next)
        val diff = signedDiff(normalise(next), normalise(prev))
        return normalise(prev + diff * f)
    }

    /** إحداثيات السهم على قرص — زاوية بالدرجات من الأعلى باتجاه عقارب الساعة */
    fun arrowTip(angleDeg: Float, radius: Float): Pair<Float, Float> {
        val a = Math.toRadians(angleDeg.toDouble())
        return Pair((radius * sinOf(a)).toFloat(), (-radius * cosOf(a)).toFloat())
    }

    private fun sinOf(a: Double): Double = kotlin.math.sin(a)
    private fun cosOf(a: Double): Double = kotlin.math.cos(a)

    /**
     * قطرات التناثر للإطار الحالي: البقاء على القبلة يرشّ باستمرار
     * [ALIGN_HOLD_PARTICLES]، ودخولُها الأول رشّة أقصى — وتركها لا رشّ.
     */
    fun alignedParticles(wasAligned: Boolean, nowAligned: Boolean): Int = when {
        nowAligned && !wasAligned -> ALIGN_ENTRY_PARTICLES
        nowAligned -> ALIGN_HOLD_PARTICLES
        else -> 0
    }
}
