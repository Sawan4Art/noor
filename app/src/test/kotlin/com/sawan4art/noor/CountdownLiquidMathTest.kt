package com.sawan4art.noor

import com.sawan4art.noor.ui.home.CountdownLiquidMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * عقود «مؤقت الصلاة السائل» — alpha31 توجيه المالك:
 * «الشريط الذي يحيط ب الوقت المتبقي اجعله يتقدم مع تقدم انتهاء الوقت
 * و اجعل الوقت المتبقي داخل سائل حي و يلتف حوله الشريط الدائري الاسبق»
 * و«ممكن نعمله شريط دائري عباره عن سائل حي برضو».
 *
 * العقود تحرس ثلاثة أشياء:
 * ١. السباحة تتقدم رتيباً مع الزمن وتكتمل ٣٦٠° عند بلوغ الأذان.
 * ٢. القطرة المركزية لا تعدّي الحلقة أبداً في أسوأ امتداد لها —
 *    في كل أقيس الصناديق المستخدمة فعلاً.
 * ٣. القيم الفاسدة تُستأمن ولا تُسقط الشاشة.
 */
class CountdownLiquidMathTest {

    @Test
    fun `السباحة من صفر إلى ٣٦٠ درجة وتكتمل عند بلوغ الأذان`() {
        assertEquals(0f, CountdownLiquidMath.sweepDegrees(0f), 1e-4f)
        assertEquals(90f, CountdownLiquidMath.sweepDegrees(0.25f), 1e-4f)
        assertEquals(180f, CountdownLiquidMath.sweepDegrees(0.5f), 1e-4f)
        assertEquals(360f, CountdownLiquidMath.sweepDegrees(1f), 1e-4f)
        // فور نزول الصلاة القادمة وإعادة العدّ — لا يتجاوز الدائرة أبداً
        assertEquals(360f, CountdownLiquidMath.sweepDegrees(1.7f), 1e-4f)
    }

    @Test
    fun `السباحة رتيبة الصعود مع كل ثانية من عمر الفترة`() {
        var prev = -1f
        for (i in 0..200) {
            val s = CountdownLiquidMath.sweepDegrees(i / 200f)
            assertTrue("السباحة تراجعت عند i=$i", s >= prev)
            prev = s
        }
    }

    @Test
    fun `القيم الفاسدة تعود صفرا بلا انهيار`() {
        assertEquals(0f, CountdownLiquidMath.sweepDegrees(Float.NaN), 1e-4f)
        assertEquals(0f, CountdownLiquidMath.sweepDegrees(Float.POSITIVE_INFINITY), 1e-4f)
        assertEquals(0f, CountdownLiquidMath.sweepDegrees(-0.4f), 1e-4f)
    }

    @Test
    fun `زاوية الرأس من الأعلى باتجاه عقارب الساعة`() {
        assertEquals(-90f, CountdownLiquidMath.headAngleDeg(0f), 1e-4f)
        assertEquals(0f, CountdownLiquidMath.headAngleDeg(90f), 1e-4f)
        assertEquals(90f, CountdownLiquidMath.headAngleDeg(180f), 1e-4f)
        assertEquals(270f, CountdownLiquidMath.headAngleDeg(360f), 1e-4f)
    }

    @Test
    fun `القطرة تنسع داخل الحلقة في كل أقيس الشاشات حتى أسوء امتداد`() {
        // أقيس الصناديق الفعلية: 186dp سقف الهيرو، ثم شاشات أقصر
        for (side in listOf(186f, 176f, 160f, 150f, 132f, 120f)) {
            val margin = 10f
            val bandHalf = 9f / 2f
            val track = CountdownLiquidMath.trackRadius(side, margin)
            val ringInner = track - bandHalf
            val drop = CountdownLiquidMath.dropRadius(ringInner, 11f)
            assertTrue(
                "side=$side: القطرة ($drop) لا تنسع داخل الحلقة ($ringInner)",
                CountdownLiquidMath.fitsInsideRing(drop, ringInner),
            )
            // وهل الأنبوب بأقصى تموج يبقى داخل الصندوق؟
            val wobble = 9f * 0.35f
            assertTrue(
                "side=$side: الأنبوب يخرج عن الصندوق",
                track + bandHalf + wobble < side / 2f,
            )
        }
    }

    @Test
    fun `حافتا الأنبوب داخل حدودهما في كل زمن وزاوية`() {
        val track = 83f
        val bandHalf = 4.5f
        val wobble = 9f * 0.35f
        var t = 0f
        while (t < 60f) {
            for (k in 0..11) {
                val a = (Math.PI * 2 * k / 12).toFloat()
                val outer = CountdownLiquidMath.outerEdge(track, bandHalf, a, t, 0.4f, wobble)
                val inner = CountdownLiquidMath.innerEdge(track, bandHalf, a, t, 2.7f, wobble)
                assertTrue(outer <= track + bandHalf + wobble + 1e-3f)
                assertTrue(outer >= track + bandHalf - wobble - 1e-3f)
                assertTrue(inner >= track - bandHalf - wobble - 1e-3f)
                assertTrue(inner <= track - bandHalf + wobble + 1e-3f)
                // الحافة الداخلية لا تغزو القطرة: أدنى حال لها فوق
                // مساحة الهوة (١١px) المخصوصة في dropRadius
                assertTrue(inner > track - bandHalf - wobble)
            }
            t += 0.37f
        }
    }

    @Test
    fun `نصف أقطار فاسدة لا تُسقط الحسابات`() {
        assertEquals(CountdownLiquidMath.MIN_RADIUS, CountdownLiquidMath.trackRadius(-10f, 10f), 1e-4f)
        assertEquals(CountdownLiquidMath.MIN_RADIUS, CountdownLiquidMath.dropRadius(-5f, 11f), 1e-4f)
        assertFalse(CountdownLiquidMath.fitsInsideRing(100f, 90f))
        assertTrue(CountdownLiquidMath.fitsInsideRing(60f, 70f))
    }

    @Test
    fun `خط العداد محكوم بسقف وأرضية`() {
        // alpha44 — النقطة تملأ الحلقة: سقف ٤٠ وأرضية ١٦ (بلاغ المالك:
        // «كبر النقطه خمس اضعاف … هديك صوره لحجم النقطه اللي انا عايزها»)
        assertTrue(CountdownLiquidMath.countdownFontSizeSp(500f) <= 40f)
        assertTrue(CountdownLiquidMath.countdownFontSizeSp(5f) >= 16f)
        // في الهيرو الفعلي (side=216dp) الخط بين ٢٤ و٣٢sp — بارز ومقروء
        val track = CountdownLiquidMath.trackRadius(216f, 10f)
        val drop = CountdownLiquidMath.dropRadius(track - 4.5f, 11f)
        val fs = CountdownLiquidMath.countdownFontSizeSp(drop)
        assertTrue("الخط في الهيرو صغير جداً: $fs", fs in 24f..32f)
        // ولا يتجاوز أرضية المتن في أصغر قطرة معقولة — قراءة لنظر ضعيف
        assertTrue(CountdownLiquidMath.countdownFontSizeSp(44.5f) >= 16f)
    }

    @Test
    fun `النقطة الجديدة تملأ الحلقة ولا تلمسها في أسوء امتداد`() {
        // alpha44 — هندسة النقطة البطلة: شريط ٧dp هامش ٢dp، والنقطة
        // تُقاس بحيث أقصى امتدادٍ لها (موجة + تنفس = ١٢٪) يلامس حافة
        // الشريط الداخلية تماماً دون تجاوز — في كل أقيس الصناديق
        for (side in listOf(300f, 260f, 216f, 176f, 150f, 132f, 120f)) {
            val stroke = 7f
            val edgeGap = 2f
            val track = CountdownLiquidMath.trackRadius(side, stroke / 2f + edgeGap)
            val ringInner = track - stroke / 2f
            val drop = CountdownLiquidMath.dropRadius(ringInner, 0f) /
                CountdownLiquidMath.MAX_BLOB_EXTENT
            assertTrue(
                "side=$side: النقطة تخرج عن الشريط",
                drop * CountdownLiquidMath.MAX_BLOB_EXTENT <= ringInner + 1e-3f,
            )
            // والشريط نفسه لا يخرج عن الصندوق
            assertTrue(
                "side=$side: الشريط يخرج عن الصندوق",
                track + stroke / 2f <= side / 2f + 1e-3f,
            )
            // والنقطة أكبر بوضوح من هندسة alpha43 القديمة (الخماس تقريبًا)
            val oldTrack = CountdownLiquidMath.trackRadius(side, 10f)
            val oldDrop = CountdownLiquidMath.dropRadius(oldTrack - 4.5f, 11f)
            assertTrue(
                "side=$side: النقطة الجديدة ($drop) ليست أكبر من القديمة ($oldDrop)",
                drop > oldDrop,
            )
        }
    }

    @Test
    fun `موضع الزاوية حول المركز صحيح في المحورين`() {
        assertEquals(103f, CountdownLiquidMath.pointX(100f, 3f, 0f), 1e-4f)
        assertEquals(100f, CountdownLiquidMath.pointX(100f, 3f, (Math.PI / 2).toFloat()), 1e-4f)
        assertEquals(112f, CountdownLiquidMath.pointY(100f, 12f, (Math.PI / 2).toFloat()), 1e-4f)
        assertEquals(100f, CountdownLiquidMath.pointY(100f, 12f, 0f), 1e-4f)
    }

    // ===== رقبة الاتصال — الشريط يخرج من النقطة ويلتحق بها (alpha32) =====

    @Test
    fun `رقبة الاتصال بين سطح النقطة وسط الأنبوب — تغمر الطرفين`() {
        // في أقيس الصناديق الفعلية: الرقبة الممتدة تغطي سطح النقطة الأقصى
        // وحافة الأنبوب الداخلية معاً — لا فجوة بين الشريط والنقطة أبداً
        for (side in listOf(186f, 176f, 160f, 150f, 132f, 120f)) {
            val bandHalf = 9f / 2f
            val track = CountdownLiquidMath.trackRadius(side, 10f)
            val drop = CountdownLiquidMath.dropRadius(track - bandHalf, 11f)
            val neckCenter = CountdownLiquidMath.neckCenterRadius(drop, track)
            val neckR = CountdownLiquidMath.neckRadius(bandHalf)
            // تغمر سطح النقطة (مع أسوأ امتداد له)
            assertTrue(
                "side=$side: الرقبة لا تلمس النقطة",
                neckCenter - neckR < drop * CountdownLiquidMath.MAX_BLOB_EXTENT,
            )
            // وتغمر حافة الأنبوب الداخلية
            assertTrue(
                "side=$side: الرقبة لا تلمس الأنبوب",
                neckCenter + neckR > track - bandHalf,
            )
        }
    }

    @Test
    fun `مركز الرقبة هو منتصف المسافة — ونصف قطرها أكبر من نصف الأنبوب`() {
        assertEquals(75f, CountdownLiquidMath.neckCenterRadius(67f, 83f), 1e-4f)
        assertEquals(6.75f, CountdownLiquidMath.neckRadius(4.5f), 1e-4f)
        assertTrue(
            CountdownLiquidMath.neckRadius(4.5f) > 4.5f,
        )
    }

    @Test
    fun `دفعة الالتصاق صفر حتى اللوب شبه مكتمل ثم تنهض إلى واحد`() {
        assertEquals(0f, CountdownLiquidMath.attachSurge(0f), 1e-4f)
        assertEquals(0f, CountdownLiquidMath.attachSurge(180f), 1e-4f)
        assertEquals(0f, CountdownLiquidMath.attachSurge(349.9f), 1e-4f)
        assertEquals(0.5f, CountdownLiquidMath.attachSurge(355f), 1e-4f)
        assertEquals(1f, CountdownLiquidMath.attachSurge(360f), 1e-4f)
        assertEquals(1f, CountdownLiquidMath.attachSurge(400f), 1e-4f)
        // القيم الفاسدة تُستأمن
        assertEquals(0f, CountdownLiquidMath.attachSurge(Float.NaN), 1e-4f)
        assertEquals(0f, CountdownLiquidMath.attachSurge(Float.NEGATIVE_INFINITY), 1e-4f)
    }

    @Test
    fun `دفعة الالتصاق لا تتراجع مع تقدم السباحة`() {
        var prev = -1f
        for (i in 0..360) {
            val s = CountdownLiquidMath.attachSurge(i.toFloat())
            assertTrue("الدفعة تراجعت عند $i", s >= prev)
            prev = s
        }
    }
}

// ===== حاكم تباين قوس الامتلاء — بلاغ المالك 2026-11-13 =====
// «شريط التقديم الخاص بالوقت المتبقي في بعض الوان الثيمات و خصوصا
// الثيمات التي في الوضع الليلي الشريط لا يظهر بشكل جيد بسبب تشابه
// الالوان يكاد يكون غير مرئى»

class CountdownRingContrastTest {

    /** إضاءة لونٍ سداسي عشري بمعادلة luminance نفسها — لقياساتٍ حيةٍ هنا */
    private fun lumOf(hex: Long): Float {
        fun lin(c: Int): Float {
            val v = c / 255f
            return if (v > 0.04045f) (((v + 0.055f) / 1.055f).toDouble().pow(2.4)).toFloat()
            else v / 12.92f
        }
        val r = ((hex shr 16) and 0xFF).toInt()
        val g = ((hex shr 8) and 0xFF).toInt()
        val b = (hex and 0xFF).toInt()
        return 0.2126f * lin(r) + 0.7152f * lin(g) + 0.0722f * lin(b)
    }

    @Test
    fun `حاويات الوضع الليلي تتشابه مع الهيرو فيلزم الزوج اللامع`() {
        // قياسات فعلية لعشرين لوناً ليلياً: الحاويات الداكنة فرقها عن
        // خلفية الهيرو دون الحد (أقصاها ٠٫٠٦٣) — والحد ٠٫١٦
        val nightContainer = lumOf(0x24463D)   // زمردي ليليًّا
        val nightSecondary = lumOf(0x3D3218)   // ثابت الهوية الليلي
        val heroNight = (lumOf(0x17332C) + lumOf(0x0B1A16)) / 2f
        assertTrue(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = nightContainer, midLum = nightSecondary, heroLum = heroNight,
            ),
        )
        // وأسوأ حالات الليل كلها دون الحد — القوس كان يكاد يختفي فعلاً
        for (container in listOf(0x1F5236L, 0x7A2424L, 0x2C2C2CL, 0x66491AL)) {
            assertTrue(
                CountdownLiquidMath.ringFillNeedsBrightFallback(
                    fillLum = lumOf(container), midLum = nightSecondary, heroLum = heroNight,
                ),
            )
        }
    }

    @Test
    fun `حاويات الوضع النهاري تبان فتبقى على عينها المعتمدة`() {
        // قياسات فعلية: الحاويات الفاتحة فرقها عن الهيرو الداكن فوق الحد
        // بمرتين على الأقل (أدنى فجوةٍ نهاريةٍ قيست ٠٫٥٥٧) — فالنهاري
        // لا يتغير أبداً
        val heroDay = (lumOf(0x35655B) + lumOf(0x22453D)) / 2f
        assertFalse(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = lumOf(0xBFE5D8), midLum = lumOf(0xF2E3BC), heroLum = heroDay,
            ),
        )
        // ولونٌ داكنٌ على خلفيةٍ فاتحةٍ يبان كذلك (الفرق يحكم لا الاتجاه)
        assertFalse(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = 0.05f, midLum = 0.06f, heroLum = 0.70f,
            ),
        )
    }

    @Test
    fun `الحد نفسه يكفي ودونه يلزم البديل — وأحد اللونين يكفي للحكم`() {
        val g = CountdownLiquidMath.MIN_RING_CONTRAST
        assertTrue(g == 0.16f)
        // الفجوة بالحد نفسه (٠٫١٦) تُعدَّ ظاهرة
        assertFalse(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = 0.30f, midLum = 0.30f, heroLum = 0.14f,
            ),
        )
        // ودون الحدٍ بفجوةٍ واحدةٍ من الاثنتين يُستبدل الزوجان معاً
        assertTrue(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = 0.30f, midLum = 0.29f, heroLum = 0.14f,
            ),
        )
        assertTrue(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = 0.15f, midLum = 0.30f, heroLum = 0.14f,
            ),
        )
    }

    @Test
    fun `القيم الفاسدة تحكم بالزوج اللامع بأمان`() {
        // إضاءةٌ فاسدةٌ لا تُقاس — الاحتياط الأهدأ: الزوج اللامع المضمون
        assertTrue(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = Float.NaN, midLum = 0.5f, heroLum = 0.1f,
            ),
        )
        assertTrue(
            CountdownLiquidMath.ringFillNeedsBrightFallback(
                fillLum = 0.5f, midLum = Float.POSITIVE_INFINITY, heroLum = 0.1f,
            ),
        )
    }
}
