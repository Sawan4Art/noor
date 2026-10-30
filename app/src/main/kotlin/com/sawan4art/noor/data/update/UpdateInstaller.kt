package com.sawan4art.noor.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * مثبّت التحديث — التثبيت داخل التطبيق كما طلب المالك:
 * الحوار هو حوار النظام للتثبيت يظهر فوق التطبيق (بلا متصفح إطلاقاً)
 * عبر FileProvider + ACTION_VIEW بنوع «حزمة أندرويد».
 *
 * أول مرة يحتاج الجهاز إذن «تثبيت التطبيقات من هذا المصدر» — نكتشفه
 * ونفتح صفحة الإعداد الخاصة بتطبيقنا مباشرة ثم يعود المستخدم ويكمل بضغطة.
 */
object UpdateInstaller {

    private const val APK_MIME = "application/vnd.android.package-archive"

    /** هل يستطيع هذا المصدر التثبيت الآن؟ (أقل من 26 لا يملك الإذن لكل تطبيق) */
    fun canRequestInstall(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 26 ||
            context.packageManager.canRequestPackageInstalls()

    /** فتح صفحة الإعداد «تثبيت تطبيقات غير معروفة» لهذا التطبيق تحديداً */
    fun openInstallPermissionSettings(context: Context): Boolean {
        val intent = if (android.os.Build.VERSION.SDK_INT >= 26) {
            Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
        } else {
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** تشغيل حوار تثبيت النظام على الـ APK المُنزّل — يعيد false إن لم يُفتح */
    fun installApk(context: Context, apk: File): Boolean {
        if (!apk.isFile || apk.length() == 0L) return false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
