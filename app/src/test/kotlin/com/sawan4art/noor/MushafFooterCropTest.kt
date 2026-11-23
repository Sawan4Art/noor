package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.MushafGeometry
import com.sawan4art.noor.domain.mushaf.MushafTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود قصّ شريط رقم الصفحة الأصفر — v1.83 (بلاغ المالك: «ازاله
 * الاجزء الاصفر في اخر الصفحه الذي يحتوي على رقم الصفه و استبداله
 * برقم الصفحه لاكن بشكل افضل»):
 *
 * ١. صفحات ٣+ تقص 36.2 بكسل فراغٍ مرجعيٍّ من أسفل (قياس محسوس من
 *    الصور: حد الإطار عند y≈1320–1321 والرقم بين 1332–1368 على
 *    ارتفاع 1369)، وصفحتا الفاتحة وبداية البقرة (١-٢) بلا شريطٍ
 *    أصلًا فلا قصَّ لهما.
 * ٢. القصّ السفلي لا يحرك الآيات: الرسم والإبراز من أعلى الصورة
 *    فإحداثيات الآيات (من فوق) تبقى مطابقةً قبل القصّ وبعده،
 *    ومعامل الامتداد الرأسي فقط يمتد لملء الشاشة بالقصّ الجديد.
 * ٣. التحويل العكسي سليمٌ تحت القصّ — الضغط على موضع الشاشة يعود
 *    بالآية نفسها قبل قصّ الشريط وبعده.
 */
class MushafFooterCropTest {

    /** صفحة منتصف المصحف بمقاسها الفعلي ١٠٠٩×١٣٦٩ */
    private fun t50(viewH: Float, cropY1: Float = MushafGeometry.cropY1(50)) = MushafTransform(
        imgW = 1009f, imgH = 1369f,
        cropX0 = 0f, cropX1 = MushafGeometry.REF_W,
        viewW = 1080f, viewH = viewH,
        cropY1 = cropY1,
    )

    @Test
    fun `صفحات ٣+ تقص الشريط وصفحتا ١-٢ لا`() {
        val crop = (MushafGeometry.REF_H - MushafGeometry.FOOTER_CROP_REF).toDouble()
        val full = MushafGeometry.REF_H.toDouble()
        assertEquals(crop, MushafGeometry.cropY1(50).toDouble(), 1e-6)
        assertEquals(crop, MushafGeometry.cropY1(3).toDouble(), 1e-6)
        assertEquals(crop, MushafGeometry.cropY1(604).toDouble(), 1e-6)
        assertEquals(full, MushafGeometry.cropY1(1).toDouble(), 1e-6)
        assertEquals(full, MushafGeometry.cropY1(2).toDouble(), 1e-6)
    }

    @Test
    fun `القص السفلي يمتد بالمحتوى ويبقى التحويل متسقا`() {
        val viewH = 2000f
        val cropped = t50(viewH)
        // القص يوسّع الامتداد: المعامل أكبر بنسبة الحد المقطوع —
        // المحتوى الباقي يملأ الشاشة كلها بعد سقوط الشريط (المقصود
        // من القصّ كما في القصّ الجانبي تماماً)
        assertEquals(
            (viewH / (1369f * (MushafGeometry.REF_H - 36.2f) / MushafGeometry.REF_H)).toDouble(),
            cropped.scaleY.toDouble(),
            1e-4,
        )
        // والارتفاع المرسوم فعلاً = حد القص بالبكسل الفعلي لا ارتفاع الصورة
        assertEquals(
            (1369f * (MushafGeometry.REF_H - 36.2f) / MushafGeometry.REF_H).toDouble(),
            cropped.srcY1.toDouble(),
            1e-3,
        )
    }

    @Test
    fun `التحويل العكسي سليم تحت القص — الضغط يعود بالآية نفسها`() {
        val t = t50(2000f)
        // نقطة شاشة داخل نصٍّ معروف: من إحداثي مرجعي ثم عكسه
        val (sx, sy) = t.toDisplay(388f, 600f)
        val (gx, gy) = t.toGlyph(sx, sy)
        assertEquals(388f, gx, 0.02f)
        assertEquals(600f, gy, 0.02f)
    }

    @Test
    fun `قص الشريط لا يلمس الإطار المزخرف — حد القص تحت حده`() {
        // حد الإطار بالفراغ المرجعي: 1321 بكسل صورةٍ ÷ 1.3001 ≈ 1016.0
        // والقص عند 1053 - 36.2 = 1016.8 — فوق حافة الإطار بأمانٍ
        val cropInImage = (MushafGeometry.REF_H - MushafGeometry.FOOTER_CROP_REF) *
            (1369f / MushafGeometry.REF_H)
        assertTrue(
            "القص قد يقطع خط الإطار!",
            cropInImage >= 1321.5f,
        )
        assertTrue(
            "القص أقل من اللازم — بقي جزء من الشريط",
            cropInImage <= 1324f,
        )
    }
}
