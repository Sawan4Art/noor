package com.sawan4art.noor.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

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

    // ===== أدوات الرسم =====

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
