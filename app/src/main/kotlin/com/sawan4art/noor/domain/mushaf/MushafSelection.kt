package com.sawan4art.noor.domain.mushaf

import kotlin.math.max
import kotlin.math.min

/**
 * هندسة صفحات المصحف الملوّن — توصيف محسوس من الطبعة نفسها.
 *
 * إحداثيات قاعدة glyphs (databases/mushaf_ayahinfo.db) مبنية على صورة
 * مرجعية ٧٧٦×١٠٥٣ بكسل، وصور التحميل إما بالمقاس المرجعي نفسه (صفحة ٣+)
 * أو مكبَّرة منه (صفحتا الفاتحة وبداية البقرة ٨٢٧×١١٥٨) — فالتحويل خطي
 * ساذج على كل محور من فراغ الإحداثيات إلى بكسل الصورة الفعلية.
 *
 * قصّ التفاسير الجانبية (توجيه المالك ٢٠٢٦-١١-٠٦: «في جزء في الصفحات
 * جانبي في تفاسير شيل الجزء ده من كل الصفحات و ابقي فقط على اطار الصفحه
 * المزغرف مع طبعا الجزء السفلي الذي ينبه معنى الالوان») — العمود الجانبي
 * يقلب ضِعه حسب الصفحة (يمين صفحة ٣، يسار صفحة ٥٠) فيتحرك الإطار المزخرف
 * معه؛ قياس الطبعة: حد الإطار = حد النص ∓ ١٣ بكسل، وهامش القص الآمن
 * بعد الإطار ١٦ بكسل، وشريط دليل الألوان أسفل الصفحة داخل الإطار فيبقى
 * كما هو. الصفحتا ١ و٢ (فاتحة وبداية البقرة) بلا عمود تفاسير أصلاً
 * وزخرفهما يعانق حدود الصفحة كاملة فلا تُقصان.
 */
object MushafGeometry {

    /** عرض فراغ الإحداثيات المرجعي بالبكسل */
    const val REF_W = 776f

    /** ارتفاع فراغ الإحداثيات المرجعي بالبكسل */
    const val REF_H = 1053f

    /** مسافة حد الإطار المزخرف عن حدود النص (قياس الطبعة) */
    const val FRAME_PAD = 13f

    /** هامش القص خارج الإطار — يُبقي خطَّي الإطار كاملين */
    const val CROP_PAD = 16f

    /** ارتفاع منطقة تنظيف الزوايا العلوية خارج الإطار (بكسل فراغ مرجعي) */
    const val CORNER_CLEAN_H = 40f

    /** لون ورق الطبعة — يقيس ثابتاً عبر الصفحات (٢٥٥، ٢٥٢، ٢١٥) */
    const val PAPER_RGB = 0xFFFFFCD7

    /** أول صفحة تُقص — ما قبلهما (الفاتحة وبداية البقرة) صفحتان كاملتان */
    const val FIRST_CROPPED_PAGE = 3

    /**
     * مدى القص الجانبي بالفراغ المرجعي — null للصفحتين الكاملتين ١ و٢.
     * الحدود: حد النص ∓ هامش القص (١٦) — الإطار المزخرف على ١٣ من النص
     * فيبقى كاملاً داخل القص بثلاثة بكسل رحمة، ويسقط عمود التفاسير
     * الجانبي خارج القصّ. مقيَّد بحدود الفراغ المرجعي فلا قص سالب.
     */
    fun cropX(page: Int, minX: Float, maxX: Float): Pair<Float, Float>? =
        if (page < FIRST_CROPPED_PAGE) {
            null
        } else {
            (minX - CROP_PAD).coerceAtLeast(0f) to (maxX + CROP_PAD).coerceAtMost(REF_W)
        }

    /** حدود الإطار المزخرف بالفراغ المرجعي — لتنظيف زوايا الإشارات العلوية */
    fun frameX(minX: Float, maxX: Float): Pair<Float, Float> =
        (minX - FRAME_PAD) to (maxX + FRAME_PAD)
}

/**
 * تحويل نقطي بين فراغ الإحداثيات وشاشة العرض — صفحة واحدة ظاهرة واحدة.
 *
 * توجيه المالك ٢٠٢٦-١١-٠٦ بعد alpha51: «صفحه المصحف نفسها في فراغ فوقها و
 * تحتها يعني عايز الصفحه تتسع لكامل الشاشه من اولها من فوق الى اخر حدود شريط
 * التنقل» — طبعة المصحف أعرض نسبياً من شاشات الهواتف، والملاءمة Fit كانت
 * تترك فراغين أعلى وأسفل، فالامتداد هنا على كل محور على حدة (كسحِب السائل
 * على قطرة): الصفحة تملأ المساحة من أولها فوق إلى آخر حدود شريط التنقل بلا
 * فراغ، عين ما تفعل تطبيقات المصحف المرجعية — والتحويلان متعاكسان دائماً
 * (ضغط ← آية، آية ← إبراز) فالمحاذاة تظل مليمترية رغم الامتداد.
 *
 * @param imgW عرض الصورة الفعلية بالبكسل (٧٧٦ عادة، ٨٢٧ للصفحتين ١-٢)
 * @param imgH ارتفاع الصورة الفعلية بالبكسل
 * @param cropX0 حد القص الأيسر بالفراغ المرجعي — 0 للصفحة الكاملة
 * @param cropX1 حد القص الأيمن بالفراغ المرجعي — عرض الفراغ للصفحة الكاملة
 * @param viewW عرض مساحة العرض بالبكسل الشاشي
 * @param viewH ارتفاع مساحة العرض بالبكسل الشاشي
 */
class MushafTransform(
    imgW: Float,
    imgH: Float,
    cropX0: Float,
    cropX1: Float,
    viewW: Float,
    viewH: Float,
) {
    /** تحويل فراغ الإحداثيات ← بكسل الصورة، على كل محور */
    private val sx = imgW / MushafGeometry.REF_W
    private val sy = imgH / MushafGeometry.REF_H

    /** حدود القص بالبكسل الفعلي */
    val srcX0 = cropX0 * sx
    val srcX1 = cropX1 * sx

    /** مساحة القص بالبكسل الفعلي */
    private val srcW = srcX1 - srcX0
    private val srcH = imgH

    /** معامل الامتداد الأفقي — كامل العرض بلا هوامش */
    val scaleX = viewW / srcW

    /** معامل الامتداد الرأسي — من أول الشاشة فوق إلى آخر شريط التنقل */
    val scaleY = viewH / srcH

    /** الصفحة تبدأ من حافة مساحة العرض نفسها */
    val dstLeft = 0f
    val dstTop = 0f

    /** كامل عرض مساحة العرض */
    val dstWidth = viewW

    /** كامل ارتفاع مساحة العرض — من فوق إلى آخر حدود شريط التنقل */
    val dstHeight = viewH

    /** نقطة فراغ مرجعية ← نقطة شاشة */
    fun toDisplay(x: Float, y: Float): Pair<Float, Float> =
        Pair((x * sx - srcX0) * scaleX, y * sy * scaleY)

    /** نقطة شاشة ← نقطة فراغ مرجعي (لضغط الإصبع) */
    fun toGlyph(x: Float, y: Float): Pair<Float, Float> =
        Pair((x / scaleX + srcX0) / sx, y / scaleY / sy)
}

/**
 * شريحة آية على الصفحة — سطر واحد من أسطر الآية كما طبعت، بإحداثياته
 * بالفراغ المرجعي. الآية الطويلة عدة شرائح (سطر لكل شريحة) والقصيرة واحدة.
 */
data class MushafSegment(
    val page: Int,
    val sura: Int,
    val ayah: Int,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)

/** آية مفردة بسورةها */
data class MushafAyah(val sura: Int, val ayah: Int)

/**
 * تحديد مُدى آيات — من ضغطة أولى (أو ضغط مطوّل) إلى ضغطة ثانية: كل الآيات
 * بينهما تُحدَّد آلياً بتوجيه المالك «اذا حدد المستخدم مثلا ايه رقم ٥٠ و
 * جيه يدوس على ايه ٥٥ يبقى اوتماتك الايات التي في المنتصف تحدد».
 */
data class MushafSelection(
    val sura: Int,
    val from: Int,
    val to: Int,
) {
    /** مدى الآيات المنتظب — الضغط قبل البداية يقلب المُدى هادئاً */
    val ayahs: IntRange get() = min(from, to)..max(from, to)
}

object MushafSelectionOps {

    /** أقصى التقاط رأسي بين السطور (بكسل فراغ مرجعي) — فوقه الضغط هامش فارغ */
    private const val SNAP = 14f

    /**
     * ضغط نقطة بالفراغ المرجعي ← آيتها، أو null إن كان الضغط في الهوامش:
     * ١) شريحة تحتوي النقطة تماماً، ٢) وإلا أقرب شريحة: السطر أولاً
     * (الفرق الرأسي يوزَّن ألفاً فالفجوة بين السطرين تُقرر السطر) ثم
     * الأفقي داخل السطر (فجوة بين آيتين في سطر واحد تُقرر أقربهما).
     */
    fun hit(segments: List<MushafSegment>, x: Float, y: Float): MushafAyah? {
        segments.firstOrNull {
            y >= it.minY && y <= it.maxY && x >= it.minX && x <= it.maxX
        }?.let { return MushafAyah(it.sura, it.ayah) }
        val nearest = segments
            .filter { y >= it.minY - SNAP && y <= it.maxY + SNAP }
            .minByOrNull { seg ->
                val vertical = if (y < seg.minY) seg.minY - y else y - seg.maxY
                val horizontal = when {
                    x < seg.minX -> seg.minX - x
                    x > seg.maxX -> x - seg.maxX
                    else -> 0f
                }
                vertical * 1000f + horizontal
            }
            ?: return null
        return MushafAyah(nearest.sura, nearest.ayah)
    }

    /**
     * توسيع التحديد بضغطة جديدة: نفس السورة ← مُدى من ثابت التحديد الحالي
     * إلى الآية المضغوطة (الأوسط آلياً)، وسورة أخرى ← بداية تحديد جديدة.
     */
    fun extend(current: MushafSelection?, tapped: MushafAyah): MushafSelection =
        if (current != null && current.sura == tapped.sura) {
            MushafSelection(tapped.sura, current.from, tapped.ayah)
        } else {
            MushafSelection(tapped.sura, tapped.ayah, tapped.ayah)
        }

    /** أول آية في التحديد — للعلامة المرجعية والتلاوة */
    fun firstOf(selection: MushafSelection): MushafAyah =
        MushafAyah(selection.sura, min(selection.from, selection.to))

    /**
     * هل الآية داخل المُدى المحدَّد؟ — لمسح التحديد بضغطةٍ مطوّلةٍ
     * ثانيةٍ على آيةٍ من المُدى (alpha63 — بلاغ المالك: «انا عايز
     * التحديد يزال عند الضغط المطول عليه مره اخرى»).
     */
    fun contains(selection: MushafSelection?, ayah: MushafAyah): Boolean {
        if (selection == null) return false
        if (selection.sura != ayah.sura) return false
        return ayah.ayah in selection.ayahs
    }
}

/**
 * آيات التلاوة الثلاثية — توجيه المالك ٢٠٢٦-١١-٠٦: «عايز لما ادوس على زر
 * تشغيل الصوت يظهر قائمه اختار منها تشغيل السوره كامله من بدايه التحديد او
 * تشغيل المحدد فقط او تشغيل السوره كامله من بدايتها بغض النظر عن التحديد».
 * كله تشغيل متتابع آية وراء آية حتى آخر المُدى المختار.
 */
object MushafPlaylist {

    /** المحدد فقط — المُدى المنتظب من التحديد */
    fun selected(selection: MushafSelection): List<Int> = selection.ayahs.toList()

    /** السورة من بداية التحديد إلى آخر آية فيها */
    fun surahFromSelection(selection: MushafSelection, surahAyahCount: Int): List<Int> {
        val start = min(selection.from, selection.to)
        return if (start <= surahAyahCount) (start..surahAyahCount).toList() else emptyList()
    }

    /** السورة كاملة من بدايتها بغض النظر عن التحديد */
    fun surahFull(surahAyahCount: Int): List<Int> = (1..surahAyahCount.coerceAtLeast(0)).toList()

    /**
     * تلاوة الحفظ الميسر — كل آية تُكرَّر عِدة مرات (٣ عادةً) ثم تليها
     * الأخرى، ويعيد المشغّل الدورة على المُدى حتى يقف المستخدم — طريقة
     * الحفظ بالتكرار والاستمرار.
     */
    fun hifzRepeats(ayahs: List<Int>, repeats: Int): List<Int> =
        ayahs.flatMap { ayah -> List(repeats.coerceAtLeast(1)) { ayah } }
}
