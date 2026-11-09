package com.sawan4art.noor.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/**
 * رسّام فنّ الويدجت (alpha59) — الساعة التناظرية:
 *
 * «يتم ايضا اضافه ساعه تناظريه» — فميناء الساعة التناظرية يُرسم
 * بألوان التخصيص: ميناء هادئ وعقارب بلون الخط ومحور بلون التطبيق.
 *
 * والنقطةُ السائلة انتهت من هنا (alpha62 — بلاغ المالك: «الغي
 * السائل الحي من الويدجت و استبدله باطار حوافه دائريه») — والإطار
 * يُرسم في الخلفية نفسها [widgetBackgroundBitmap] فلا صورةً إضافية
 * ولا بايتاً زائداً في باكِت البايندر.
 *
 * كل الصور صغيرةٌ عمداً (خلفية ≤ ٣٦٠×٢٦٠ وميناء ≤ ٢٦٠×٢٦٠)
 * — درس alpha58: مجموع البايتات داخل باكِت RemoteViews لا يجاوز
 * مخزن البايندر (~١MB) وإلا فشل التحديث وبدت الويدجت ميتة.
 */
object WidgetArt {

    /** حجم ميناء الساعة التناظرية (مربع) — alpha63 كبّرها بلاغ
     *  المالك: «شكل الساعه التناظريه صغير جدا» — من ١١٠ إلى ١٥٠ */
    const val ANALOG_SIZE = 150

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
        val s = size.coerceIn(64, 260)
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
