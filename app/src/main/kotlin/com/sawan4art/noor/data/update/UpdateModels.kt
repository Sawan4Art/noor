package com.sawan4art.noor.data.update

import java.util.Locale

/** ملخص إصدار مستخرج من واجهة GitHub — طبقة نقية قابلة للاختبار بلا JSON */
data class ReleaseSummary(
    val tag: String,
    val title: String,
    val body: String,
    val isPrerelease: Boolean,
    val releaseUrl: String,
    val publishedAt: String,
    val apkName: String?,
    val apkUrl: String?,
    val apkSizeBytes: Long,
    /** قناة الإصدار من الوسم — مستقرة/بيتا/تجريبية (بلاغ المالك v1.71) */
    val channel: UpdateChannel = UpdateChannel.STABLE,
)

/** بيانات تحديث متاح — ما تظهره الواجهة وتحتاجه سلسلة التنزيل/التثبيت */
data class UpdateInfo(
    val tag: String,
    val versionName: String,
    val title: String,
    val body: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val releaseUrl: String,
    val publishedAt: String,
    /** قناة الإصدار — تُعرض في الحوار وقسم التحديثات (بلاغ v1.71) */
    val channel: UpdateChannel = UpdateChannel.STABLE,
) {
    /** نص الحجم: «7.1 م.ب» */
    val sizeText: String
        get() {
            val mb = apkSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format(Locale.US, "%.1f م.ب", mb)
            else String.format(Locale.US, "%.0f ك.ب", apkSizeBytes / 1024.0)
        }
}

/** حالة نظام التحديث — تُجمع من الواجهة (الحوار + بطاقة الإعدادات)
 *
 * alpha48: حالتا «جارٍ التنزيل» و«جاهز للتثبيت» عادتا — التحديث من
 * جوّه التطبيق تاني (توجيه المالك: «التحديث بقا يفتح المتصفح انا
 * مش عايز كده») بحالتها الجديدة في DownloadState.
 */
sealed interface UpdateUiState {
    /** بلا فحص جارٍ ولا تحديث معروض */
    data object Idle : UpdateUiState

    /** جارٍ سؤال GitHub */
    data object Checking : UpdateUiState

    /** تحديث جديد متاح — «ما الجديد» جاهز للعرض */
    data class Available(val info: UpdateInfo) : UpdateUiState

    /** فشل (شبكة) — مع ما يمكن إعادة المحاولة عليه */
    data class Failed(val message: String, val info: UpdateInfo?) : UpdateUiState

    /** فحص انتهى ولا يوجد أحدث — تظهر في الإعدادات فقط */
    data class UpToDate(val currentVersion: String) : UpdateUiState
}

/**
 * حالة تنزيل التحديث داخل التطبيق (alpha48 — عودة التحديث من جوّه
 * التطبيق بتوجيه المالك بدل فتح المتصفح).
 */
sealed interface DownloadState {

    /** لا تنزيل جارٍ */
    data object Idle : DownloadState

    /** التطبيق بلا حق «التثبيت من هذا المصدر» — يُفتح بيان الإذن ثم يعيد المستخدم الضغط */
    data object NeedsPermission : DownloadState

    /** جارٍ التنزيل من جوّه التطبيق مباشرةً — بالنسبة المئوية (alpha53) */
    data class Running(val percent: Int) : DownloadState

    /** اكتمل التنزيل — مثبّت النظام فُتح على الملف (ويُعاد فتحه بضغطة أخرى) */
    data object Done : DownloadState

    /** فشل التنزيل (شبكة/مساحة) — بإعادة المحاولة من الزر نفسه */
    data class Failed(val message: String) : DownloadState
}
