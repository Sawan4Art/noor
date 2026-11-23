package com.sawan4art.noor.domain.tafsir

/**
 * كتالوج طبقات التفسير — v1.81 (بلاغ المالك: «اضف التفاسير التاليه
 * الجلالين و القرطبي و ابن كثير الطبري السعدي و طبعا مع ابقاء التفسير
 * الميسر مع امكانيه تغيير طبعه التفسير من الاعدادات و حطلها قسم جديد
 * باسم التفاسير»).
 *
 * **التفسير الميسر باقٍ** — مضمّنٌ في قاعدة المصحف نفسها
 * (mushaf_ayahinfo.db / جدول tafsir_muyassar) فيعمل فورًا بلا إنترنت
 * وهو الطبعة الافتراضية. والخمس الأخرى قواعدُ sqlite موحّدة
 * (جدول tafsir: sura/ayah/text) تُنزَّل عند الطلب من فرع الأصول
 * ويتحقق تجزؤها sha256 قبل الفتح — والأرقام أدناه مولَّدة من خط
 * البيانات (scripts/build_tafsir_pipeline.py) وكلها بتغطيةٍ كاملة
 * ٦٢٣٦ آية وكل سورة بعدّها القياسي.
 *
 * هذا الملف نقيةٌ بلا أندرويد كي تُختبر عقودُه وحدةً.
 */
object TafsirEditions {

    /** معرّف الطبعة المضمّنة الافتراضية */
    const val MUYASSAR_ID = "ar.muyassar"

    data class Edition(
        val id: String,
        val arabicName: String,
        /** حجم الحزمة المضغوطة بالبايت — يُعرض للمستخدم قبل التنزيل */
        val sizeBytes: Long,
        /** بصمة الحزمة — يتحقق منها المكتبة بعد التنزيل قبل فتح القاعدة */
        val sha256: String,
        /** مضمّنةٌ في التطبيق فلا تحتاج تنزيلًا (الميسر وحده) */
        val bundled: Boolean,
    )

    private const val ASSETS_BASE =
        "https://raw.githubusercontent.com/Sawan4Art/noor/tafsir-assets"

    val JALALAYN = Edition(
        "ar.jalalayn", "تفسير الجلالين", 681_956L,
        "ec0b0cb98f8ab1fd25867827a906ce99244346810fe7c980611690c5a02b1cac",
        bundled = false,
    )
    val KATHEER = Edition(
        "ar.katheer", "تفسير ابن كثير", 3_780_219L,
        "b7204e5336f5c6f233f1a34ef65021c41bf4779ae165e8c208b6c4ab47998d7d",
        bundled = false,
    )
    val QURTUBI = Edition(
        "ar.qurtubi", "تفسير القرطبي", 5_295_462L,
        "cb5e12f8df18347f552b916efc09b2c569dcaa649bb49e9b0da356f55c899cdd",
        bundled = false,
    )
    val TABARI = Edition(
        "ar.tabari", "تفسير الطبري", 8_095_044L,
        "4c81b223c98437b34f436f11bf566c31d2febd88858c48bda82188d94fc701f0",
        bundled = false,
    )
    val SAADI = Edition(
        "ar.saadi", "تفسير السعدي", 1_825_956L,
        "ba80149415824d6fd66c57bcda425cfb5d88d048ec6df046d222bc0cefb12e25",
        bundled = false,
    )
    val MUYASSAR = Edition(
        MUYASSAR_ID, "التفسير الميسر", 0L, "", bundled = true,
    )

    /** الترتيب المعروض — الميسر أولًا ثم الكتب بالاعتماد */
    val ALL: List<Edition> = listOf(MUYASSAR, JALALAYN, KATHEER, QURTUBI, TABARI, SAADI)

    fun byId(id: String): Edition? = ALL.firstOrNull { it.id == id }

    /** رابط حزمة التنزيل — للمضمّنة null (لا حزمة لها) */
    fun downloadUrl(id: String): String? =
        byId(id)?.takeIf { !it.bundled }?.let { "$ASSETS_BASE/${it.id}.zip" }

    /**
     * اسم طبعةٍ للعرض مع سقوطٍ آمن — المعرّف المجهول يعود نصًّا خامًا
     * كي لا تُفقد الطبعة المختارة اسمًا لو اتسع الكتالوج لاحقًا.
     */
    fun displayName(id: String): String =
        byId(id)?.arabicName ?: id

    /**
     * صلاحية الطبعة المختارة المحفوظة — معرّفٌ غير موجودٍ في الكتالوج
     * يعيد الافتراضية المضمّنة (حماية من تخزينٍ قديمٍ أو تالفة).
     */
    fun resolveOrDefault(id: String?): String =
        if (id != null && byId(id) != null) id else MUYASSAR_ID

    /** نص الحجم للعرض — «٠٫٧ م.ب» بالأرقام العربية كعرف التطبيق */
    fun sizeLabel(bytes: Long): String {
        val mb = bytes / 1_048_576.0
        val arabic = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        val s = if (mb >= 1.0) {
            String.format("%.1f", mb)
        } else {
            val kb = bytes / 1024.0
            String.format("%.0f", kb) + " ك.ب"
        }
        return s.map { c -> if (c.isDigit()) arabic[c - '0'] else c }
            .joinToString("") + if (mb >= 1.0) " م.ب" else ""
    }
}
