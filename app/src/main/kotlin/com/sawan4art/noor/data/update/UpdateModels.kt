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
 * alpha46: حالتا «جارٍ التنزيل» و«جاهز للتثبيت» صارا بلا معنى — التنزيل
 * والتثبيت في المتصفح مباشرة (بلاغ المالك: فحص جوجل كان يصنّف التنزيل
 * والتثبيت داخل التطبيق ضمن الأنماط الخطرة) — فالزبان فحصٌ فقط + فتح
 * ملف الـ APK في المتصفح.
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
