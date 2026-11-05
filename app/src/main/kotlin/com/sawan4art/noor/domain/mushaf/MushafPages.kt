package com.sawan4art.noor.domain.mushaf

/**
 * صفحات المصحف — رواية حفص مجوّد بالتشكيل الملون للتجويد.
 *
 * توجيه المالك (٢٠٢٦-١١-٠٥): «غير المصحف تماما انا عايز راويه حفص مجود
 * زي الصوره كده و يكون عرض صفحات المصحف بكامل الشاشه مع زر فوق على
 * اليمين للرجوع و يكون زر صغير» — المصحف الجديد كلُّه صور صفحات
 * المصحف الملون بالتجويد نفسه بترقيم المدنة القياسي (٦٠٤ صفحة،
 * الصفحة ٥٠ مطلع آل عمران مثلاً)، فلا يُرسم نصٌّ داخل التطبيق بل
 * تُعرض صفحة المصحف المطبوع عينها كما في الصورة.
 *
 * الصفحات تُحمَّل من الشبكة عند أول مشاهدة وتُحفظ محلياً في ذاكرة
 * قرص التطبيق (ImageLoader في NoorApplication) فتصبح متاحة بلا
 * إنترنت بعد ذلك — وهذا هو نمط تطبيقات المصحف المعروفة.
 *
 * المصدر — مستودع عام مفتوح لصور «مصحف التجويد الملون»:
 * github.com/murtraja/quran-android-images-helper (المجلد images_1053).
 * الرابط الأساسي عبر شبكة jsDelivr العالمية (سريعة ومستقرة في مصر)،
 * والرابط الاحتياطي أرشيف GitHub الخام للصورة نفسها في تعطل الأول.
 */
object MushafPages {

    /** عدد صفحات المصحف المدني القياسي — المصحف كله */
    const val PAGE_COUNT = 604

    /** قاعدة روابط الصفحات الأساسية — شبكة jsDelivr العالمية */
    const val IMAGE_BASE_URL =
        "https://cdn.jsdelivr.net/gh/murtraja/quran-android-images-helper@master/static/images_1053"

    /** المصدر الاحتياطي — أرشيف GitHub الخام للصورة نفسها */
    private const val FALLBACK_BASE_URL =
        "https://raw.githubusercontent.com/murtraja/quran-android-images-helper/master/static/images_1053"

    /**
     * تقييد رقم الصفحة داخل ١..٦٠٤ — يُستخدم عند القراءة والكتابة معاً
     * فلا يعود قيمة مخزنة من نسخة قديمة أو إدخال خاطئ خارج النطاق.
     */
    fun clampPage(page: Int): Int = page.coerceIn(1, PAGE_COUNT)

    /**
     * رابط الصفحة الأساسي (jsDelivr) — رقم الصفحة ١..٦٠٤ كما هو في
     * المصحف المطبوع، وأسماء ملفات المصدر تبدأ من 1.jpg حتى 604.jpg.
     */
    fun primaryUrl(page: Int): String = "$IMAGE_BASE_URL/${clampPage(page)}.jpg"

    /**
     * رابط الصفحة الاحتياطي (GitHub الخام) — الصورة نفسها بالبايت،
     * يُجرب بعد فشل الأساسي فتصل الصفحة ما دام الجهاز متصلاً بأي طريق.
     */
    fun fallbackUrl(page: Int): String = "$FALLBACK_BASE_URL/${clampPage(page)}.jpg"
}
