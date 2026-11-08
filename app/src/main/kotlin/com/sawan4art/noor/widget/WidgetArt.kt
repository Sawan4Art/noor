package com.sawan4art.noor.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.sawan4art.noor.ui.nav.GooeyMath
import kotlin.math.cos
import kotlin.math.sin

/**
 * رسّام فنّ الويدجت (alpha59) — بلاغ المالك في النقطة السائلة والساعة:
 *
 * «عايزك تعدل شكل السائل فهو يظهر كنقطه ماء مخالفه تماماً للسائل الحي
 * الموجود في التطبيق فهذا ما اريده و ليس مجرد نقطه» — فالفقاعة هنا
 * **سائلٌ حيٌّ بنفس رياضيات التطبيق**: سطح متموج بثلاث موجات سفر
 * ([GooeyMath.surfaceWave] نفسها المعملة في الرئيسية والمسبحة) ومنعّم
 * كاتمول-روم مغلق كالقطرة الكبيرة، مع لمعةٍ زجاجيةٍ وقطرةٍ تابعةٍ
 * صغيرةٍ تجاورها — تُرسم في صورةٍ نقطيةٍ بلون كل مقامٍ كما كان.
 *
 * و«يتم ايضا اضافه ساعه تناظريه» — فميناء الساعة التناظرية يُرسم
 * بألوان التخصيص: ميناء هادئ وعقارب بلون الخط ومحور بلون التطبيق.
 *
 * كل الصور صغيرةٌ عمداً (خلفية ٣٦٠×٢٦٠ وفقاعات ≤ ٢٠٠×٧٠ وميناء ١١٠×١١٠)
 * — درس alpha58: مجموع البايتات داخل باكِت RemoteViews لا يجاوز مخزن
 * البايندر (~١MB) وإلا فشل التحديث وبدت الويدجت ميتة.
 */
object WidgetArt {

    /** حجم صورة الفقاعة خلف العدّاد — عرض×ارتفاع بالبكسل */
    const val COUNTDOWN_BUBBLE_W = 200
    const val COUNTDOWN_BUBBLE_H = 70

    /** حجم صورة فقاعة خلية المواقيت */
    const val GRID_BUBBLE_W = 96
    const val GRID_BUBBLE_H = 46

    /** حجم ميناء الساعة التناظرية (مربع) */
    const val ANALOG_SIZE = 110

    /**
     * مسار قطرةٍ سائلةٍ حيّة — التوأم المباشر لـ addLiquidBlob في
     * الرئيسية: ٤٤ نقطةً حول المركز بنصف قطرٍ متموج بثلاث موجات،
     * ومنعّمة بكاتمول-روم مغلق — على android.graphics.Path بدل مسار
     * Compose كي تُرسم في صورة الويدجت.
     */
    internal fun addBlobPath(
        path: Path,
        cx: Float,
        cy: Float,
        baseRadius: Float,
        time: Float,
        phase: Float,
    ) {
        val n = 44
        val xs = FloatArray(n)
        val ys = FloatArray(n)
        val twoPi = (2.0 * Math.PI).toFloat()
        for (i in 0 until n) {
            val angle = twoPi * i / n - (Math.PI / 2f).toFloat()
            val radius = baseRadius * (1f + GooeyMath.surfaceWave(angle, time, 0f, phase))
            xs[i] = cx + radius * cos(angle)
            ys[i] = cy + radius * sin(angle)
        }
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
     * صورة الفقاعة السائلة الحية — القطرة المتموجة تملأ الارتفاع، ولمعة
     * زجاجية في رأسها الأيسر، وقطرةٌ تابعةٌ صغيرةٌ عند طرفها الأيمن
     * تجعلها «سائلاً» لا نقطة — والزمن مجمدٌ عند طورٍ مختلفٍ لكل مقامٍ
     * فتبدو كل فقاعةٍ بملامحها.
     */
    fun liquidBubbleBitmap(width: Int, height: Int, color: Int, phase: Float): Bitmap {
        val w = width.coerceIn(24, 400)
        val h = height.coerceIn(16, 200)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setColor(color)
            alpha = 200
        }
        val radius = h * 0.42f
        val blob = Path()
        addBlobPath(blob, w * 0.42f, h * 0.54f, radius, 0.8f, phase)
        canvas.drawPath(blob, paint)

        // القطرة التابعة الصغيرة — تجاور الكبيرة بطورٍ آخر فتؤكد السيلان
        val small = Path()
        addBlobPath(small, w * 0.80f, h * 0.72f, radius * 0.24f, 1.7f, phase + 2.9f)
        canvas.drawPath(small, paint)

        // اللمعة الزجاجية — بيضاوان شفافان في رأس القطرة
        val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setColor(Color.WHITE)
            alpha = 70
        }
        canvas.drawOval(
            w * 0.30f, h * 0.30f, w * 0.44f, h * 0.44f,
            gloss,
        )
        canvas.drawOval(
            w * 0.46f, h * 0.62f, w * 0.52f, h * 0.70f,
            gloss,
        )
        return bmp
    }

    /**
     * زاوية عقارب الساعة بالدرجات من الثانية عشر باتجاه العقارب — دالة
     * نقية تُختبر: العقرب الصاع نصف زاوية الدقيقة زائداً محصلة الساعة،
     * والدقيقة دورة كاملة كل ستين.
     */
    internal fun hourAngleDeg(hour12: Int, minute: Int): Float =
        ((hour12 % 12) * 30f + minute * 0.5f).let { if (it >= 360f) it - 360f else it }

    internal fun minuteAngleDeg(minute: Int): Float = (minute % 60) * 6f

    /**
     * ميناء الساعة التناظرية — حلقةُ ميناءٍ هادئة من لون الإبراز
     * بلفافية منخفضة، اثنتا عشرة وتداً (أربعة رئيسية أسمك)، عقرب ساعةٍ
     * قصيرٌ سمين وعقرب دقيقةٍ أطول أنحف بلون الخط، ومحورٌ بلون التطبيق.
     */
    fun analogClockBitmap(
        size: Int,
        hour12: Int,
        minute: Int,
        dialColor: Int,
        handColor: Int,
        accentColor: Int,
        onDialColor: Int,
    ): Bitmap {
        val s = size.coerceIn(48, 240)
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = s / 2f
        val r = s * 0.46f

        // الميناء — قرص بلون الخط بلفافية خفيفة وحلقة إبراز
        val dial = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setColor(dialColor)
            alpha = 46
        }
        canvas.drawCircle(cx, cx, r, dial)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.025f
            setColor(accentColor)
            alpha = 160
        }
        canvas.drawCircle(cx, cx, r, ring)

        // الوتد الاثنا عشر — الرئيسي عند ١٢ و٣ و٦ و٩ أسمك وأطول
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            setColor(onDialColor)
            alpha = 170
        }
        for (i in 0 until 12) {
            val major = i % 3 == 0
            tick.strokeWidth = if (major) s * 0.030f else s * 0.016f
            tick.alpha = if (major) 190 else 120
            val a = Math.toRadians((i * 30 - 90).toDouble())
            val outer = r * 0.92f
            val inner = if (major) r * 0.78f else r * 0.84f
            canvas.drawLine(
                cx + (outer * cos(a)).toFloat(), cx + (outer * sin(a)).toFloat(),
                cx + (inner * cos(a)).toFloat(), cx + (inner * sin(a)).toFloat(),
                tick,
            )
        }

        // العقارب — من المركز بلون الخط
        val hourA = Math.toRadians((hourAngleDeg(hour12, minute) - 90).toDouble())
        val minuteA = Math.toRadians((minuteAngleDeg(minute) - 90).toDouble())
        val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            setColor(handColor)
            strokeCap = Paint.Cap.ROUND
        }
        hand.strokeWidth = s * 0.050f
        canvas.drawLine(cx, cx, cx + (r * 0.52f * cos(hourA)).toFloat(), cx + (r * 0.52f * sin(hourA)).toFloat(), hand)
        hand.strokeWidth = s * 0.034f
        canvas.drawLine(cx, cx, cx + (r * 0.74f * cos(minuteA)).toFloat(), cx + (r * 0.74f * sin(minuteA)).toFloat(), hand)

        // المحور بلون التطبيق
        val hub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            setColor(accentColor)
        }
        canvas.drawCircle(cx, cx, s * 0.035f, hub)
        return bmp
    }
}
