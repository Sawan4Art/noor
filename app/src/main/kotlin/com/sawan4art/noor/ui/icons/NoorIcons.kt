package com.sawan4art.noor.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * أيقونات «نور» المخصصة — مرسومة هندسيًا بلا مكتبة الأيقونات الموسعة
 * (قرار المرحلة الأولى: core icons فقط لإبقاء الـ APK خفيفًا).
 * نفس اللغة الهندسية لأيقونة التطبيق: دائرة، هلال، نجمة رباعية.
 * التلوين يتم عبر tint في Icon() — مسار اللون هنا لا يهم.
 */
object NoorIcons {

    /** المواقيت — ساعة هندسية: حلقة + عقارب */
    val PrayerTimes: ImageVector by lazy {
        noorIcon("PrayerTimes") {
            filled(PathFillType.EvenOdd) {
                circle(12f, 12f, 9.4f)
                circle(12f, 12f, 7.4f)
            }
            filled {
                // العقرب العمودي (نحو 12)
                moveTo(11.1f, 6.9f)
                lineTo(12.9f, 6.9f)
                lineTo(12.9f, 12.9f)
                lineTo(11.1f, 12.9f)
                close()
                // العقرب الأفقي (نحو 3)
                moveTo(11.1f, 11.1f)
                lineTo(16.4f, 11.1f)
                lineTo(16.4f, 12.9f)
                lineTo(11.1f, 12.9f)
                close()
            }
        }
    }

    /** المصحف — مصحف مفتوح بصفحتين وشق الكعب الأوسط */
    val Mushaf: ImageVector by lazy {
        noorIcon("Mushaf") {
            filled(PathFillType.EvenOdd) {
                moveTo(12f, 6.2f)
                curveTo(10.2f, 4.8f, 7.4f, 4.4f, 4.9f, 4.9f)
                curveTo(4.4f, 5.0f, 4.0f, 5.4f, 4.0f, 5.9f)
                lineTo(4.0f, 17.5f)
                curveTo(4.0f, 17.95f, 4.35f, 18.3f, 4.8f, 18.25f)
                curveTo(7.2f, 17.9f, 10.1f, 18.3f, 12f, 19.6f)
                curveTo(13.9f, 18.3f, 16.8f, 17.9f, 19.2f, 18.25f)
                curveTo(19.65f, 18.3f, 20f, 17.95f, 20f, 17.5f)
                lineTo(20f, 5.9f)
                curveTo(20f, 5.4f, 19.6f, 5.0f, 19.1f, 4.9f)
                curveTo(16.6f, 4.4f, 13.8f, 4.8f, 12f, 6.2f)
                close()
                // شق الكعب الأوسط — يُفرّغ الصفحتين (EvenOdd)
                moveTo(11.5f, 7.0f)
                curveTo(11.8f, 6.8f, 12.2f, 6.8f, 12.5f, 7.0f)
                lineTo(12.5f, 18.6f)
                curveTo(12.2f, 18.8f, 11.8f, 18.8f, 11.5f, 18.6f)
                close()
            }
        }
    }

    /** القبلة — بوصلة: حلقة وإبرة معينية */
    val Qibla: ImageVector by lazy {
        noorIcon("Qibla") {
            filled(PathFillType.EvenOdd) {
                circle(12f, 12f, 9.4f)
                circle(12f, 12f, 7.6f)
            }
            filled {
                moveTo(12f, 6.8f)
                lineTo(14.3f, 12f)
                lineTo(12f, 17.2f)
                lineTo(9.7f, 12f)
                close()
            }
        }
    }

    /** الأذكار — نجمتا نور (نفس نجمة أيقونة التطبيق) */
    val Adhkar: ImageVector by lazy {
        noorIcon("Adhkar") {
            filled {
                star(10.2f, 9.4f, 7.2f)
            }
            filled {
                star(17.6f, 17.8f, 3.6f)
            }
        }
    }

    /** المسبحة — سبحة: حبات في حلقة والشاهد المعلق */
    val Tasbih: ImageVector by lazy {
        noorIcon("Tasbih") {
            filled {
                // سبع حبات في حلقة — الفراغ أسفل للشاهد
                bead(12f, 4.0f)
                bead(16.31f, 5.79f)
                bead(18.1f, 10.1f)
                bead(16.31f, 14.41f)
                bead(7.69f, 14.41f)
                bead(5.9f, 10.1f)
                bead(7.69f, 5.79f)
                // الشاهد المعلق أسفل
                moveTo(10.65f, 18.4f)
                curveTo(10.65f, 16.7f, 11.2f, 15.7f, 12f, 15.2f)
                curveTo(12.8f, 15.7f, 13.35f, 16.7f, 13.35f, 18.4f)
                curveTo(13.35f, 20.1f, 12.8f, 21.2f, 12f, 21.7f)
                curveTo(11.2f, 21.2f, 10.65f, 20.1f, 10.65f, 18.4f)
                close()
            }
        }
    }

    /** مبدّل المظهر — الشمس والقمر معاً (بطلب المالك 2026-10-30: هندسية شمس وقمر):
     *  هلال بنفس هندسة أيقونة التطبيق (فرق دائرتين بقوسين حقيقيين، الفتحة لأعلى-يمين
     *  نحو الشمس) + قرص شمس بثماني أشعة مثلثية خارج دائرة القمر تماماً.
     *  المسار مولّد بـ scripts/gen_theme_icon.py — لا تُحرَّر يدوياً. */
    val SunMoon: ImageVector by lazy {
        noorIcon("SunMoon") {
            filled {
                // الهلال — قوسان حقيقيان بين نقطتي تقاطع دائرتين (لغة أيقونة التطبيق نفسها)
                moveTo(17.35f, 16.53f)
                curveTo(16.24f, 20.12f, 12.75f, 22.44f, 9.01f, 22.06f)
                curveTo(5.27f, 21.68f, 2.31f, 18.72f, 1.94f, 14.98f)
                curveTo(1.57f, 11.24f, 3.89f, 7.75f, 7.48f, 6.65f)
                curveTo(5.8f, 8.42f, 5.17f, 10.95f, 5.83f, 13.31f)
                curveTo(6.48f, 15.67f, 8.32f, 17.51f, 10.68f, 18.17f)
                curveTo(13.04f, 18.83f, 15.57f, 18.2f, 17.35f, 16.52f)
                close()
            }
            filled {
                // قرص الشمس
                circle(SUN_CX, SUN_CY, SUN_DISC_R)
                // ثماني أشعة مثلثية كل 45°
                for (i in 0 until 8) ray(SUN_CX, SUN_CY, i * 45f)
            }
        }
    }

    /** مزلاج المظهر — القمر وحده (هلال أيقونة التطبيق ممركزاً — مولّد آلي scripts/gen_dial_icons.py) */
    val Moon: ImageVector by lazy {
        noorIcon("Moon") {
            filled {
                moveTo(19.7f, 14.18f)
                curveTo(18.6f, 17.77f, 15.11f, 20.09f, 11.36f, 19.71f)
                curveTo(7.62f, 19.33f, 4.66f, 16.37f, 4.29f, 12.63f)
                curveTo(3.92f, 8.89f, 6.24f, 5.4f, 9.84f, 4.3f)
                curveTo(8.16f, 6.07f, 7.53f, 8.6f, 8.18f, 10.96f)
                curveTo(8.83f, 13.32f, 10.68f, 15.16f, 13.04f, 15.82f)
                curveTo(15.4f, 16.48f, 17.93f, 15.85f, 19.71f, 14.17f)
                close()
            }
        }
    }

    /** مزلاج المظهر — الشمس وحدها (قرص + ثماني أشعة، الأولى نحو الشرق — مولّد آلي) */
    val Sun: ImageVector by lazy {
        noorIcon("Sun") {
            filled {
                // قرص الشمس الممركز
                circle(12f, 12f, 4.5f)
                // ثماني أشعة مثلثية — الأولى نحو اليمين (شرق)
                rayAt(21.2f, 12f, 18.06f, 12.7f, 18.06f, 11.3f)
                rayAt(18.51f, 18.51f, 15.79f, 16.78f, 16.78f, 15.79f)
                rayAt(12f, 21.2f, 11.3f, 18.06f, 12.7f, 18.06f)
                rayAt(5.49f, 18.51f, 7.22f, 15.79f, 8.21f, 16.78f)
                rayAt(2.8f, 12f, 5.94f, 11.3f, 5.94f, 12.7f)
                rayAt(5.49f, 5.49f, 8.21f, 7.22f, 7.22f, 8.21f)
                rayAt(12f, 2.8f, 12.7f, 5.94f, 11.3f, 5.94f)
                rayAt(18.51f, 5.49f, 16.78f, 8.21f, 15.79f, 7.22f)
            }
        }
    }

    /** التحديثات — سهم تنزيل فوق صينية تركيب (نفس لغة المستطيلات المستديرة) */
    val Update: ImageVector by lazy {
        noorIcon("Update") {
            filled {
                // عمود السهم
                moveTo(11.1f, 3.6f)
                lineTo(12.9f, 3.6f)
                lineTo(12.9f, 12.0f)
                lineTo(11.1f, 12.0f)
                close()
                // رأس السهم مثلث لأسفل
                moveTo(6.9f, 11.2f)
                lineTo(17.1f, 11.2f)
                lineTo(12f, 16.9f)
                close()
                // قدمتا الصينية والقاعدة
                moveTo(4.2f, 15.4f)
                lineTo(5.9f, 15.4f)
                lineTo(5.9f, 18.3f)
                lineTo(18.1f, 18.3f)
                lineTo(18.1f, 15.4f)
                lineTo(19.8f, 15.4f)
                lineTo(19.8f, 20.0f)
                lineTo(4.2f, 20.0f)
                close()
            }
        }
    }

    // ===== أدوات الرسم =====

    /** هندسة شمس مبدّل المظهر — قرص وأشعة (نفس أرقام المولّد) */
    private const val SUN_CX = 19.2f
    private const val SUN_CY = 4.8f
    private const val SUN_DISC_R = 1.9f
    private const val RAY_BASE_R = 2.55f
    private const val RAY_TIP_R = 3.8f
    private const val RAY_HALF_DEG = 3.4

    private fun noorIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "Noor.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.filled(
        fillType: PathFillType = PathFillType.NonZero,
        block: PathBuilder.() -> Unit,
    ) = path(
        name = "path",
        fill = SolidColor(Color.Black),
        pathFillType = fillType,
        pathBuilder = block,
    )

    /** دائرة كاملة بأربع منحنيات بيزيه (معامل دائرة 0.5523) */
    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        val k = 0.552284749831f * r
        moveTo(cx - r, cy)
        curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        close()
    }

    /** حبة سبحة صغيرة */
    private fun PathBuilder.bead(cx: Float, cy: Float) = circle(cx, cy, 1.45f)

    /** شعاع شمس مثلثي بنقاط جاهزة — مولّد آلي (مزلاج المظهر) */
    private fun PathBuilder.rayAt(
        tipX: Float, tipY: Float,
        b1X: Float, b1Y: Float,
        b2X: Float, b2Y: Float,
    ) {
        moveTo(tipX, tipY)
        lineTo(b1X, b1Y)
        lineTo(b2X, b2Y)
        close()
    }

    /** شعاع شمس مثلثي — رأس على RAY_TIP_R وقاعدة حول الزاوية على RAY_BASE_R */
    private fun PathBuilder.ray(cx: Float, cy: Float, angleDeg: Float) {
        val a = Math.toRadians(angleDeg.toDouble())
        val ha = Math.toRadians(RAY_HALF_DEG.toDouble())
        moveTo(
            (cx + RAY_TIP_R * cos(a)).toFloat(),
            (cy + RAY_TIP_R * sin(a)).toFloat(),
        )
        lineTo(
            (cx + RAY_BASE_R * cos(a + ha)).toFloat(),
            (cy + RAY_BASE_R * sin(a + ha)).toFloat(),
        )
        lineTo(
            (cx + RAY_BASE_R * cos(a - ha)).toFloat(),
            (cy + RAY_BASE_R * sin(a - ha)).toFloat(),
        )
        close()
    }

    /** نجمة نور الرباعية بأضلاع مقعّرة — نفس نجمة أيقونة التطبيق */
    private fun PathBuilder.star(cx: Float, cy: Float, r: Float) {
        val a = 0.12f * r
        val b = 0.18f * r
        moveTo(cx, cy - r)
        curveTo(cx + a, cy - b, cx + b, cy - a, cx + r, cy)
        curveTo(cx + b, cy + a, cx + a, cy + b, cx, cy + r)
        curveTo(cx - a, cy + b, cx - b, cy + a, cx - r, cy)
        curveTo(cx - b, cy - a, cx - a, cy - b, cx, cy - r)
        close()
    }
}
