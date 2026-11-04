package com.sawan4art.noor.domain.adhan

import android.content.Context

/**
 * أصوات الأذان والإقامة المدمجة — عشرة أصوات تعمل بلا إنترنت:
 * خمسة أذان وخمس إقامات، كلها مدمجة في موارد التطبيق.
 * وفضلاً عنها يستطيع المستخدم اختيار أي صوت من هاتفه، أو نغمة
 * إشعارات الهاتف بدلاً عن الأذان/الإقامة.
 */
object AdhanVoices {

    /** صوت مدمج بمعرّف واسم وعنوان */
    data class Voice(
        val id: String,
        val name: String,
        val subtitle: String,
    )

    val ADHAN: List<Voice> = listOf(
        Voice("main", "الأذان الكامل الخاشع", "صوت واضح ومؤثر"),
        Voice("fakhry", "أذان المؤذن صباح فخري", "أداء تقليدي عريق"),
        Voice("aqsa", "أذان المسجد الأقصى", "من باحات المسجد الأقصى"),
        Voice("traditional", "الأذان التقليدي", "مؤذن تقليدي"),
        Voice("beautiful", "أذان هادئ جميل", "صوت عذب مطمئن"),
    )

    val IQAMAH: List<Voice> = listOf(
        Voice("makkah", "إقامة الحرم المكي", "الشيخ أحمد بسناوي — مكة المكرمة"),
        Voice("haram", "إقامة حية من الحرم", "تسجيل حي من مسجد الحرام"),
        Voice("beautiful", "إقامة خاشعة جميلة", "أداء مؤثر مطول"),
        Voice("pure", "إقامة خاشعة قصيرة", "صوت هادئ نقّي"),
        Voice("soft", "إقامة عذبة سريعة", "أداء رنّان مريح"),
    )

    const val CUSTOM_ID = "custom"

    /** نغمة إشعارات الهاتف — الخيار الثالث بدل الأذان/الإقامة */
    const val NOTIFICATION_ID = "notification"

    fun adhanById(id: String): Voice = ADHAN.firstOrNull { it.id == id } ?: ADHAN.first()

    fun iqamahById(id: String): Voice = IQAMAH.firstOrNull { it.id == id } ?: IQAMAH.first()

    /** هل المعرّف من الأصوات المدمجة (وليس مخصص/إشعارات)؟ */
    fun isBuiltin(kind: AdhanSoundKind, id: String): Boolean =
        (if (kind == AdhanSoundKind.ADHAN) ADHAN else IQAMAH).any { it.id == id }

    /**
     * معرّف مورد الصوت المدمج — بأي معرّف غريب ترجع الافتراضي.
     * الصيغة: res/raw/adhan_<id>.ogg وres/raw/iqamah_<id>.ogg
     */
    fun builtinResId(context: Context, kind: AdhanSoundKind, id: String): Int {
        val prefix = if (kind == AdhanSoundKind.ADHAN) "adhan" else "iqamah"
        val known = isBuiltin(kind, id)
        val safeId = if (known) id else (if (kind == AdhanSoundKind.ADHAN) ADHAN else IQAMAH).first().id
        return context.resources.getIdentifier("${prefix}_$safeId", "raw", context.packageName)
    }
}

/** نوع الصوت المطلوب تشغيله — أذان أو إقامة */
enum class AdhanSoundKind { ADHAN, IQAMAH }
