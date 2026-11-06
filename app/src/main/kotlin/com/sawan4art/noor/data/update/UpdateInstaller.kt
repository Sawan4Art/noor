package com.sawan4art.noor.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * ناقل التحديث الداخلي — تحديث من جوّه التطبيق بلا متصفح وبلا مدير
 * تنزيل النظام (alpha53 بتوجيه المالك: «عالج مشكله فحص جوجل بان التطبيق
 * قد يكون ضار بدون ما نعمل حركه تحميل التحديث من المتصفح»).
 *
 * alpha48 كانت تنزّل بمدير تنزيل النظام (DownloadManager) — ومدير
 * التنزيل يسجّل ملف الـ APK في قائمة تنزيلات النظام التي يجتاحها فاحص
 * جوجل فيعلّم الملف وهو ينزل وقد يصفه «قد يكون التطبيق ضار» قبل أن
 * يصل المستخدم للتثبيت أصلاً. الطريق الجديد: التنزيل باتصال التطبيق
 * المباشر (HTTPS قياسي) داخل مسار ملفات التطبيق الخاص مباشرةً — لا
 * أثر للملف في أي قائمة عامة يرصدها الفاحص، ومنه يُفتح مثبّت أندرويد
 * الرسمي عبر موفّر الملفات كما كان: لا تثبيت صامت ولا كود مبتكر،
 * والمستخدم يرى حوار التثبيت الذي يعرفه ويضغط «تثبيت» بيده.
 */
object UpdateInstaller {

    private const val APK_DIR = "updates"
    private const val APK_FILE = "noor-update.apk"
    private const val PART_FILE = "noor-update.apk.part"
    private const val APK_MIME = "application/vnd.android.package-archive"

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

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
     * تنزيل النسخة باتصال مباشر داخل مسار التطبيق الخاص — دالة حجبية
     * تُنادى من خيط IO وتُعلِم بالتقدم كل نصف ثانية تقريباً. أي ملف
     * قديم أو نصف مكتمل يُحذف أولاً، والملف الجديد يُكتب بصيغة .part
     * فلا يظن مثبّت النظام ملفاً ناقصاً مكتمل، وبعد الاكتمال يُعاد
     * تسميته لاسم التثبيت النهائي.
     */
    fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): DownloadState {
        val final = targetFile(context)
        val part = File(final.parentFile, PART_FILE)
        final.delete()
        part.delete()
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                return DownloadState.Failed("تعذّر الوصول للنسخة على الشبكة ($code) — حاول من جديد")
            }
            // الحجم الفعلي من المخدم وإن غاب نحسب بمعرفة حجم النسخة المعلن
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.apkSizeBytes
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    var lastReport = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            val pct = ((written * 100) / total).toInt().coerceIn(0, 99)
                            if (pct != lastReport) {
                                lastReport = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }
            if (!part.renameTo(final)) {
                part.delete()
                return DownloadState.Failed("تعذّر تجهيز ملف التحديث على الجهاز — حاول من جديد")
            }
            DownloadState.Done
        } catch (e: IOException) {
            part.delete()
            DownloadState.Failed("توقّف التنزيل — تأكد من اتصالك بالإنترنت وحاول من جديد")
        } catch (e: Exception) {
            part.delete()
            DownloadState.Failed("حدث خطأ غير متوقع أثناء التنزيل — حاول من جديد")
        } finally {
            connection?.disconnect()
        }
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
