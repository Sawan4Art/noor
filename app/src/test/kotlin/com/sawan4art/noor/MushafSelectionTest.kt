package com.sawan4art.noor

import com.sawan4art.noor.domain.mushaf.MushafGeometry
import com.sawan4art.noor.domain.mushaf.MushafSegment
import com.sawan4art.noor.domain.mushaf.MushafSelection
import com.sawan4art.noor.domain.mushaf.MushafSelectionOps
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.MushafTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات تفاعل المصحف (alpha51) — التحديد المُدىّ والإحداثيات والقص،
 * كلها منطق نقي على إحداثيات الطبعة المرجعية ٧٧٦×١٠٥٣.
 */
class MushafSelectionTest {

    private fun seg(sura: Int, ayah: Int, minX: Float, maxX: Float, minY: Float, maxY: Float, page: Int = 3) =
        MushafSegment(page = page, sura = sura, ayah = ayah, minX = minX, maxX = maxX, minY = minY, maxY = maxY)

    /** نموذج صفحة ٣ الحقيقي: آية ٦ سطران وآية ٧ تبدأ في نهاية السطر الثاني */
    private val page3 = listOf(
        seg(2, 6, 28f, 644f, 39f, 99f),
        seg(2, 6, 494f, 644f, 99f, 160f),
        seg(2, 7, 28f, 493f, 99f, 160f),
        seg(2, 8, 28f, 149f, 160f, 220f),
    )

    @Test
    fun `الضغط داخل شريحة يحدد آيتها`() {
        val hit = MushafSelectionOps.hit(page3, 400f, 130f)
        assertEquals(2, hit?.sura)
        assertEquals(7, hit?.ayah)
    }

    @Test
    fun `فجوة بين آيتين في السطر الواحد تختار الأقرب أفقيا`() {
        // بين آية ٧ (تنتهي ٤٩٣) وآية ٨ (تبدأ ٢٨ على السطر الثالث) — الضغط
        // على فجوة السطر الثاني يسار نهاية ٧ يختار ٧ لا ٨
        assertEquals(7, MushafSelectionOps.hit(page3, 480f, 130f)?.ayah)
    }

    @Test
    fun `فجوة بين السطور تختار أقرب سطر ثم أقرب آية فيه`() {
        // بين سطر ٩٩..١٦٠ وسطر ١٦٠..٢٢٠ عند الضغط فوق ١٦٠ بقليل
        val hit = MushafSelectionOps.hit(page3, 400f, 158f)
        assertEquals(7, hit?.ayah)
    }

    @Test
    fun `الضغط في الهامش بعيدا عن السطور لا يحدد شيئا`() {
        // فوق أول سطر (٣٩) بأكثر من التقاط ١٤ — والنزول آخر الصفحة بعيداً
        assertNull(MushafSelectionOps.hit(page3, 400f, 20f))
        assertNull(MushafSelectionOps.hit(page3, 400f, 950f))
    }

    @Test
    fun `المدي بين آيتين يحدد ما بينهما آليا كما طلب المالك ٥٠ ثم ٥٥`() {
        val anchor = MushafSelectionOps.extend(null, com.sawan4art.noor.domain.mushaf.MushafAyah(2, 50))
        assertEquals(MushafSelection(2, 50, 50), anchor)
        val extended = MushafSelectionOps.extend(anchor, com.sawan4art.noor.domain.mushaf.MushafAyah(2, 55))
        assertEquals(MushafSelection(2, 50, 55), extended)
        assertEquals((50..55).toList(), extended.ayahs.toList())
    }

    @Test
    fun `المدي المعكوس ٥٥ ثم ٥٠ ينتظم تصاعديا`() {
        val anchor = MushafSelectionOps.extend(null, com.sawan4art.noor.domain.mushaf.MushafAyah(2, 55))
        val extended = MushafSelectionOps.extend(anchor, com.sawan4art.noor.domain.mushaf.MushafAyah(2, 50))
        assertEquals((50..55).toList(), extended.ayahs.toList())
    }

    @Test
    fun `سورة مختلفة تبدأ تحديدا جديدا لا تمتد المدي`() {
        val anchor = MushafSelectionOps.extend(null, com.sawan4art.noor.domain.mushaf.MushafAyah(2, 286))
        val extended = MushafSelectionOps.extend(anchor, com.sawan4art.noor.domain.mushaf.MushafAyah(3, 200))
        assertEquals(MushafSelection(3, 200, 200), extended)
    }

    @Test
    fun `أول آية في المدي هي الأصغر رقمين كان أم لا`() {
        val sel = MushafSelection(2, 55, 50)
        assertEquals(50, MushafSelectionOps.firstOf(sel).ayah)
    }

    @Test
    fun `عنوان التحديد آية واحدة بأرقام عربية`() {
        assertEquals("سورة البقرة · آية ١٠", MushafSurahs.caption(MushafSelection(2, 10, 10)))
    }

    @Test
    fun `عنوان المدي بأرقام عربية ومشطتين`() {
        assertEquals("سورة آل عمران · الآيات ٥٠–٥٥", MushafSurahs.caption(MushafSelection(3, 50, 55)))
        assertEquals("سورة آل عمران · الآيات ٥٠–٥٥", MushafSurahs.caption(MushafSelection(3, 55, 50)))
    }

    @Test
    fun `أسماء السور مكتملة ومعروفة عند المرسى`() {
        assertEquals(114, MushafSurahs.NAMES.size)
        assertEquals("الفاتحة", MushafSurahs.name(1))
        assertEquals("البقرة", MushafSurahs.name(2))
        assertEquals("آل عمران", MushafSurahs.name(3))
        assertEquals("الناس", MushafSurahs.name(114))
        assertEquals("", MushafSurahs.name(115))
    }

    @Test
    fun `الأرقام العربية الهندية مطابقة`() {
        assertEquals("٥٠", MushafSurahs.arabicDigits(50))
        assertEquals("٦٠٤", MushafSurahs.arabicDigits(604))
        assertEquals("١", MushafSurahs.arabicDigits(1))
    }
}

/**
 * اختبارات الهندسة — القص الجانبي والإطار والتحويل البكسلي، وعينة
 * موثّقة من إحداثيات قاعدة الطبعة (صفحة ٣ قياساً تراكبياً).
 */
class MushafGeometryTest {

    /** حدود صفحة ٣ كما في القاعدة: نص ٢٨..٦٤٤ */
    private val minX = 28f
    private val maxX = 644f

    @Test
    fun `القص يبقي الإطار المزخرف ويقصي عمود التفاسير`() {
        val (x0, x1) = MushafGeometry.cropX(3, minX, maxX)!!
        // حد الإطار على ١٣ من النص — القص على ١٦ فيشمل الإطار برحمة ٣
        assertEquals(12f, x0, 0.01f)
        assertEquals(660f, x1, 0.01f)
        assertTrue(x0 < minX - MushafGeometry.FRAME_PAD)
        assertTrue(x1 > maxX + MushafGeometry.FRAME_PAD)
    }

    @Test
    fun `القص مقيّد بحدود الفراغ المرجعي فلا سالب`() {
        val (x0, x1) = MushafGeometry.cropX(3, 10f, 770f)!!
        assertEquals(0f, x0, 0.01f)
        assertEquals(MushafGeometry.REF_W, x1, 0.01f)
    }

    @Test
    fun `صفحتا الفاتحة والبقرة كاملتان بلا قص`() {
        assertNull(MushafGeometry.cropX(1, minX, maxX))
        assertNull(MushafGeometry.cropX(2, minX, maxX))
        assertTrue(MushafGeometry.cropX(3, minX, maxX) != null)
    }

    @Test
    fun `الإطار يقيس ١٣ من حد النص`() {
        val (fx0, fx1) = MushafGeometry.frameX(minX, maxX)
        assertEquals(15f, fx0, 0.01f)
        assertEquals(657f, fx1, 0.01f)
    }

    @Test
    fun `التحويل البكسلي صفحة قياسية مطابق للفراغ المرجعي`() {
        // صورة ٧٧٦×١٠٥٣ نفس الفراغ المرجعي، عرض ٧٧٦ وارتفاع ١٠٥٣ شاشة
        val t = MushafTransform(776f, 1053f, 12f, 660f, 776f, 1053f)
        val (dx, dy) = t.toDisplay(400f, 500f)
        val (gx, gy) = t.toGlyph(dx, dy)
        assertEquals(400f, gx, 0.01f)
        assertEquals(500f, gy, 0.01f)
    }

    @Test
    fun `صفحات الفاتحة المكبرة ٨٢٧×١١٥٨ تحافظ على المحاذاة`() {
        val t = MushafTransform(827f, 1158f, 0f, 776f, 827f, 1158f)
        val (gx, gy) = t.toGlyph(171f * 827f / 776f, 269f * 1158f / 1053f)
        assertEquals(171f, gx, 0.05f)
        assertEquals(269f, gy, 0.05f)
    }

    @Test
    fun `الملاءمة داخل مساحة أصغر توسّط الصفحة لا تشوهها`() {
        val t = MushafTransform(776f, 1053f, 12f, 660f, 400f, 900f)
        // نسبة القص ٦٤٨×١٠٥٣ أعرض من ٤٠٠×٩٠٠ → العرض يربط
        assertEquals(400f, t.dstWidth, 0.5f)
        assertTrue(t.dstHeight < 900f)
        assertEquals((900f - t.dstHeight) / 2f, t.dy, 0.5f)
        assertEquals(0f, t.dx, 0.5f)
    }
}
