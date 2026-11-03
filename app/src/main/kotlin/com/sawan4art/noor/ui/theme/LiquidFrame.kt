package com.sawan4art.noor.ui.theme

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * الإطار السائل الحي المستطيل (alpha39 — بلاغ المالك: «عايز اسم المطور فقط
 * يكون داخل اطار سائل حي و ليس نقطه او هي شبه النقطه لاكن مستطيله»):
 *
 * نفس لغة السوائل الحية في الشريط والهلال — حافة تتموج ولا تسكن — لكن الشكل
 * هنا **مستطيل دائري الزوايا** لا نقطة: المحيط يتكون من أربعة حواف مستقيمة
 * وأربعة أرباع أقواس، وكل نقطة على المحيط تُزاح على العمودي الخارجي بموجات
 * جيبية تجري على المحيط مع الزمن، ثم يُنعّم المسار بمنحنيات بيزيه (الصيغة
 * المجرَّبة في addLiquidBlob) فيبدو الإطار ماءً حياً يتنفس حول النص.
 */
object LiquidFrameMath {

    /** سقف النقاط المعبأة في المصفوفة (زوجان لكل نقطة) — حماية من تجاوز الطول */
    const val MAX_POINTS = 1024

    /** كثافة المعاينة: نقطة كل ~7px على المحيط — نعومة تكفي بلا تكلفة زائدة */
    private const val STEP_PX = 7f

    /** سقف سعة الموجة الأماني (نسبة من ارتفاع الإطار) — يمنع التقاطع الذاتي */
    const val MAX_AMPLITUDE_RATIO = 0.10f

    /**
     * موجة الحافة الحية — مجموع جيبيين: واحدة تجري مع المحيط والأخرى
     * معاكسة السير أبطأ، والزمن يحرّكهما فيموج الإطار في الثبات ذاته.
     * [s] موقع القوس على المحيط (مسافة قوسية بالبكسل)، الناتج (−1..1).
     */
    fun edgeWave(s: Float, time: Float, phase: Float): Float =
        0.62f * sin(0.045f * s + 0.9f * time + phase) +
            0.38f * sin(0.021f * s - 0.6f * time + 2.3f + phase)

    /**
     * يملأ [out] بنقاط محيط الإطار السائل (x,y بالتناوب) ويُرجع عددها.
     * المسار يبدأ من أعلى منتصف الحافة العلوية ويسير باتجاه عقارب الساعة.
     * [w],[h] بُعدا الإطار، [corner] نصف قطر الزوايا، [amp] سعة الموجة بالبكسل.
     */
    fun sampleFrame(
        w: Float,
        h: Float,
        corner: Float,
        time: Float,
        amp: Float,
        phase: Float,
        out: FloatArray,
    ): Int {
        val cw = w.coerceAtLeast(2f)
        val ch = h.coerceAtLeast(2f)
        val r = corner.coerceIn(0f, minOf(cw, ch) / 2f - 0.5f).coerceAtLeast(0f)
        val ew = cw - 2f * r
        val eh = ch - 2f * r
        val perimeter = 2f * (ew + eh) + 2f * Math.PI.toFloat() * r
        var count = 0

        fun emit(x: Float, y: Float, nx: Float, ny: Float, arcPos: Float) {
            if (count + 2 > out.size.coerceAtMost(MAX_POINTS * 2)) return
            val off = amp * edgeWave(arcPos, time, phase)
            out[count++] = x + nx * off
            out[count++] = y + ny * off
        }

        // مواقع المحيط بالمسافة القوسية من نقطة البداية (أعلى المنتصف)
        val halfTop = ew / 2f
        var s = 0f
        // 1) النصف الأيمن من الحافة العلوية: من المنتصف نحو الزاوية اليمنى
        val topSteps = ceil(halfTop / STEP_PX).toInt().coerceAtLeast(1)
        for (i in 0..topSteps) {
            val t = i.toFloat() / topSteps
            emit(r + halfTop + t * halfTop, 0f, 0f, -1f, s + t * halfTop)
        }
        s += halfTop
        // 2) ربع قوس الزاوية اليمنى العليا: 270° ← 360° حول (cw−r, r)
        val arcSteps = ceil((Math.PI.toFloat() * r / 2f / STEP_PX).toDouble()).toInt().coerceAtLeast(2)
        for (i in 1..arcSteps) {
            val ang = (270f + 90f * i / arcSteps) * (Math.PI.toFloat() / 180f)
            val nx = cos(ang)
            val ny = sin(ang)
            emit(cw - r + r * nx, r + r * ny, nx, ny, s + Math.PI.toFloat() * r / 2f * i / arcSteps)
        }
        s += Math.PI.toFloat() * r / 2f
        // 3) الحافة اليمنى
        val rightSteps = ceil(eh / STEP_PX).toInt().coerceAtLeast(1)
        for (i in 1..rightSteps) {
            val t = i.toFloat() / rightSteps
            emit(cw, r + t * eh, 1f, 0f, s + t * eh)
        }
        s += eh
        // 4) ربع قوس اليمنى السفلى: 0° ← 90° حول (cw−r, ch−r)
        for (i in 1..arcSteps) {
            val ang = 90f * i / arcSteps * (Math.PI.toFloat() / 180f)
            val nx = cos(ang)
            val ny = sin(ang)
            emit(cw - r + r * nx, ch - r + r * ny, nx, ny, s + Math.PI.toFloat() * r / 2f * i / arcSteps)
        }
        s += Math.PI.toFloat() * r / 2f
        // 5) الحافة السفلية (من اليمين إلى اليسار)
        val bottomSteps = ceil(ew / STEP_PX).toInt().coerceAtLeast(1)
        for (i in 1..bottomSteps) {
            val t = i.toFloat() / bottomSteps
            emit(cw - r - t * ew, ch, 0f, 1f, s + t * ew)
        }
        s += ew
        // 6) ربع قوس اليسرى السفلى: 90° ← 180° حول (r, ch−r)
        for (i in 1..arcSteps) {
            val ang = (90f + 90f * i / arcSteps) * (Math.PI.toFloat() / 180f)
            val nx = cos(ang)
            val ny = sin(ang)
            emit(r + r * nx, ch - r + r * ny, nx, ny, s + Math.PI.toFloat() * r / 2f * i / arcSteps)
        }
        s += Math.PI.toFloat() * r / 2f
        // 7) الحافة اليسرى (من الأسفل إلى الأعلى)
        val leftSteps = ceil(eh / STEP_PX).toInt().coerceAtLeast(1)
        for (i in 1..leftSteps) {
            val t = i.toFloat() / leftSteps
            emit(0f, ch - r - t * eh, -1f, 0f, s + t * eh)
        }
        s += eh
        // 8) ربع قوس اليسرى العليا: 180° ← 270° حول (r, r)
        for (i in 1..arcSteps) {
            val ang = (180f + 90f * i / arcSteps) * (Math.PI.toFloat() / 180f)
            val nx = cos(ang)
            val ny = sin(ang)
            emit(r + r * nx, r + r * ny, nx, ny, s + Math.PI.toFloat() * r / 2f * i / arcSteps)
        }
        s += Math.PI.toFloat() * r / 2f
        // 9) النصف الأيسر من الحافة العلوية — العودة لنقطة البداية
        for (i in 1..topSteps) {
            val t = i.toFloat() / topSteps
            emit(r + halfTop - t * halfTop, 0f, 0f, -1f, s + t * halfTop)
        }
        return count / 2
    }

    /** نصف العرض الأقصى الذي قد يصل إليه المسار عن يمين ويسار الشكل (للاختبارات) */
    fun maxReach(amp: Float): Float = amp

    /** نطاق الموجة الكلي (−1..1) — قمة مجموع الجيبين معاً */
    fun edgeWavePeak(): Float = 1f
}

/** يبني مسار الإطار السائل المنعّم داخل [path] — منعّم بيزيه كقطرة addLiquidBlob */
fun addLiquidFrame(
    path: Path,
    w: Float,
    h: Float,
    corner: Float,
    time: Float,
    amp: Float,
    phase: Float = 0f,
) {
    val out = FloatArray(LiquidFrameMath.MAX_POINTS * 2)
    val n = LiquidFrameMath.sampleFrame(w, h, corner, time, amp, phase, out)
    if (n < 3) return
    val xs = FloatArray(n)
    val ys = FloatArray(n)
    System.arraycopy(out, 0, xs, 0, n)
    System.arraycopy(out, n, ys, 0, n)
    for (i in 0 until n) {
        val prev = (i + n - 1) % n
        val next = (i + 1) % n
        val next2 = (i + 2) % n
        if (i == 0) path.moveTo(xs[i], ys[i])
        path.cubicTo(
            xs[i] + (xs[next] - xs[prev]) / 6f,
            ys[i] + (ys[next] - ys[prev]) / 6f,
            xs[next] - (xs[next2] - xs[i]) / 6f,
            ys[next] - (ys[next2] - ys[i]) / 6f,
            xs[next],
            ys[next],
        )
    }
    path.close()
}

/**
 * شارة اسم المطور — الاسم داخل إطار سائل حي مستطيل بحواف دائرية:
 * حافته تتموج ولا تسكن (LiquidFrameMath)، وتدرّج عائلة السوائل نفسه
 * primaryContainer ← secondaryContainer والنص على الحاوي onPrimaryContainer.
 *
 * alpha44 (بلاغ المالك: «اسم المطور في صفحه الاعدادات عايزه داخل سائل
 * مستطيل حوافه دائريه و يكون سائل حي بنفس النمط و الالوان») — السائل
 * صار **حيّاً بوضوح**: زوايا أدورّ (٤٦٪ من الارتفاع)، وموجة أمضخم
 * (٨٪ من الارتفاع داخل سقف الأمان ١٠٪ بدل ٥٫٥٪ الخفية)، وتنفّسٌ بطيء
 * يكبر الإطار ويصغّره كقطرةٍ تتنفس، وانسيابٌ رأسي يعوم به الإطار كله
 * كما يعوم الهلال في قطرة التذكير — نفس نمط سوائل التطبيق وألوانها.
 */
@Composable
fun LiquidDeveloperBadge(
    name: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // زمن حي لا يتوقف — السائل لا يسكن (لغة الشريط والهلال ذاتها)
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos = it }
        }
    }
    Box(
        modifier = modifier
            .drawBehind {
                val t = frameNanos / 1_000_000_000f
                // زوايا ٤٦٪ من الارتفاع — مستديرة بوضوح وتبقى مستطيلاً لا كبسولة
                val corner = size.height * 0.46f
                // موجة حيّة أوضح — ٨٪ من الارتفاع داخل سقف الأمان ١٠٪،
                // وتتنفس سعتها ببطء (الموج تتمدد وتنكمش كنفسٍ حية)
                val amp = size.height * 0.08f * (1f + 0.18f * sin(t * 0.8f))
                // انسياب رأسي بطيء — الإطار كله يعوم كما يعوم الهلال في قطرة التذكير
                val drift = size.height * 0.045f * sin(t * 0.55f)
                val path = Path()
                addLiquidFrame(path, size.width, size.height, corner, t, amp)
                translate(top = drift) {
                    drawPath(
                        path,
                        Brush.verticalGradient(
                            listOf(scheme.primaryContainer, scheme.secondaryContainer),
                        ),
                    )
                }
            }
            .padding(horizontal = 30.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer,
        )
    }
}
