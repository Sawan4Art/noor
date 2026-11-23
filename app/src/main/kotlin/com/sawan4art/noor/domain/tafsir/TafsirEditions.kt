package com.sawan4art.noor.domain.tafsir

/**
 * كتالوج طبعات التفسير — v1.81 أدخلها بلاغ المالك، ثم **v1.82 أعاد
 * صياغتها**: «انا عايز التفاسير تكون مدمجه داخل التطبيق نفسه مش قابله
 * للتنزيل لا تكون موجوده بشكل افتراضي في التطبيق و المستخدم يختار
 * بينهم».
 *
 * الست كلها **مدمجةٌ في حزمة التطبيق** (assets/tafsir — ملفٌ نصيٌّ
 * لكل سورةٍ في كل طبعة) فتعمل فورًا بلا إنترنت ولا تنزيل ولا مساحةٍ
 * على الهاتف، والميسر هو الافتراضي والقارئ يبدّل بينها من الإعدادات
 * أو من لوحة التفسير نفسها. وحُذف كل أثر التنزيل (الأحجام والبصمات
 * والروابط) — ودالة نص الحجم القديمة كانت سبب انهيار زر التفاسير:
 * حوّلت الأرقام بافتراض أنها لاتينية، وعلى هاتفٍ بواجهةٍ عربية
 * يعيد التنسيق أرقامًا عربيةً (٠٫٧) فوقعت `c - '0'` خارج المصفوفة.
 * ما هنا اليوم لا تنسيق أرقامٍ إطلاقًا.
 *
 * هذا الملف نقيةٌ بلا أندرويد كي تُختبر عقودُه وحدةً.
 */
object TafsirEditions {

    /** معرّف الطبعة الافتراضية */
    const val MUYASSAR_ID = "ar.muyassar"

    data class Edition(
        val id: String,
        val arabicName: String,
    )

    val MUYASSAR = Edition(MUYASSAR_ID, "التفسير الميسر")
    val JALALAYN = Edition("ar.jalalayn", "تفسير الجلالين")
    val KATHEER = Edition("ar.katheer", "تفسير ابن كثير")
    val QURTUBI = Edition("ar.qurtubi", "تفسير القرطبي")
    val TABARI = Edition("ar.tabari", "تفسير الطبري")
    val SAADI = Edition("ar.saadi", "تفسير السعدي")

    /** الترتيب المعروض — الميسر أولًا ثم الكتب بالاعتماد */
    val ALL: List<Edition> = listOf(MUYASSAR, JALALAYN, KATHEER, QURTUBI, TABARI, SAADI)

    fun byId(id: String): Edition? = ALL.firstOrNull { it.id == id }

    /**
     * اسم طبعةٍ للعرض مع سقوطٍ آمن — المعرّف المجهول يعود نصًّا خامًا
     * كي لا تُفقد الطبعة المختارة اسمًا لو اتسع الكتالوج لاحقًا.
     */
    fun displayName(id: String): String = byId(id)?.arabicName ?: id

    /**
     * صلاحية الطبعة المختارة المحفوظة — معرّفٌ غير موجودٍ في الكتالوج
     * يعيد الافتراضية (حماية من تخزينٍ قديمٍ أو تالفة).
     */
    fun resolveOrDefault(id: String?): String =
        if (id != null && byId(id) != null) id else MUYASSAR_ID

    /**
     * مسار ملف السورة داخل أصول الحزمة — `tafsir/<المعرّف>/NNN.txt`.
     * الرقم بثلاث خاناتٍ لاتينيةٍ بترقيمٍ ثابت (مسارٌ لا نصُّ عرضٍ
     * فلا علاقة له بلغة الجهاز).
     */
    fun assetPath(editionId: String, sura: Int): String {
        val n = sura.coerceIn(1, 114).toString().padStart(3, '0')
        return "tafsir/$editionId/$n.txt"
    }
}
