package com.sawan4art.noor.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * ناقل التحديث الداخلي — عودة «تحديث الآن» من داخل التطبيق بلا متصفح
 * (alpha48 بتوجيه المالك: «التحديث بقا يفتح المتصفح انا مش عايز كده»).
 *
 * الطريق أهدأ ما يمكن مع فحص جوجل في اليوم نفسه:
 * - **التنزيل بمدير تنزيل النظام نفسه** (DownloadManager): التطبيق لا
 *   يلمس بايتات الملف إطلاقاً — التنزيل بعمل النظام المعروف بإشعاره
 *   المألوف، ويكمل ولو أُغلق التطبيق.
 * - **الفتح بمثبّت أندرويد الرسمي** عبر موفّر الملفات: لا تثبيت صامت
 *   ولا كود تحميل ديناميكي — المستخدم يرى حوار التثبيت الذي يعرفه
 *   من كل تطبيق، وهو يضغط «تثبيت» بيده.
 */
object UpdateInstaller {

    private const val APK_DIR = "updates"
    private const val APK_FILE = "noor-update.apk"
    private const val APK_MIME = "application/vnd.android.package-archive"

    /** هل يملك التطبيق حق «التثبيت من هذا المصدر» (٢٦+) — الأقدم يسأل عند التثبيت نفسه */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    /** فتح صفحة «السماح من هذا المصدر» — خطوة واحدة تُمنح مرة واحدة فقط */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** مسار ملف النسخة داخل ملفات التطبيق الخارجية — بلا أذونات تخزين */
    private fun targetFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), APK_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, APK_FILE)
    }

    /**
     * بدء التنزيل بمدير النظام — يعود بمعرّف العملية أو null إن تعذّر
     * البدء. حُذف أي ملف قديم أولاً كي لا يبقى APK نصف مكتمل.
     */
    fun enqueue(context: Context, info: UpdateInfo): Long? = runCatching {
        targetFile(context).delete()
        val request = DownloadManager.Request(Uri.parse(info.apkUrl)).apply {
            setTitle("تحديث نور المسلم ${info.versionName}")
            setDescription("جارٍ تنزيل النسخة الجديدة…")
            setMimeType(APK_MIME)
            setDestinationUri(Uri.fromFile(targetFile(context)))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }.getOrNull()

    /**
     * سؤال مدير التنزيل عن حالة العملية — null تعني «لا معلومة الآن»
     * فيُبقى على آخر حالة معروفة بدل قفزات الواجهة.
     */
    fun queryState(context: Context, id: Long): DownloadState? {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return null
        val cursor: Cursor = runCatching {
            manager.query(DownloadManager.Query().setFilterById(id))
        }.getOrNull() ?: return null
        cursor.use { c ->
            if (!c.moveToFirst()) return null
            val statusIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            when (c.getInt(statusIndex)) {
                DownloadManager.STATUS_SUCCESSFUL -> return DownloadState.Done
                DownloadManager.STATUS_FAILED -> return DownloadState.Failed(
                    "توقّف التنزيل — تأكد من اتصالك بالإنترنت وحاول من جديد",
                )
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING ->
                    return DownloadState.Running(percentOf(c))
                else -> return DownloadState.Running(percentOf(c))
            }
        }
    }

    /** النسبة المئوية من أعمدة مدير التنزيل — 0..99 حتى تكتمل فيصير 100 */
    private fun percentOf(c: Cursor): Int {
        val downloadedIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val totalIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        val downloaded = c.getLong(downloadedIndex)
        val total = c.getLong(totalIndex)
        if (total <= 0) return 0
        return ((downloaded * 100) / total).toInt().coerceIn(0, 99)
    }

    /** فتح ملف النسخة عند مثبّت أندرويد الرسمي — حوار التثبيت المألوف */
    fun openApk(context: Context): Boolean {
        val file = targetFile(context)
        if (!file.exists() || file.length() == 0L) return false
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK,
            )
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
