package com.sawan4art.noor.data.update

import android.content.Context
import com.sawan4art.noor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * منزّل التحديث — يكتب الـ APK في مجلد updates الخارجي الخاص بالتطبيق
 * (لا يطلب تخزيناً عاماً) ويعرض التقدم لحظياً. الملف يُبنى بلاحقة ‎.part
 * ثم يُنقل — انقطاع الشبكة لا يترك ملفاً تالفاً يثبَّت بالخطأ.
 */
object UpdateDownloader {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    /** مجلد التنزيل — خارجي خاص إن توفر وإلا داخلي (كلاهما مصرّح في file_paths) */
    fun updatesDir(context: Context): File {
        val external = context.getExternalFilesDir("updates")
        return external ?: File(context.filesDir, "updates").apply { mkdirs() }
    }

    fun apkFile(context: Context, info: UpdateInfo): File =
        File(updatesDir(context), "noor-${info.versionName}.apk")

    /** APK مكتمل مسبقاً لنفس النسخة (حجمه يطابق) → يُتخطى التنزيل */
    fun existingApk(context: Context, info: UpdateInfo): File? {
        val file = apkFile(context, info)
        if (!info.isCompleteApk(file)) return null
        return file
    }

    private fun UpdateInfo.isCompleteApk(file: File): Boolean =
        file.isFile && file.length() > 0 &&
            (apkSizeBytes <= 0 || file.length() == apkSizeBytes)

    /**
     * تنزيل الـ APK مع تقدم لحظي — يعيد الملف المكتمل.
     * GitHub يُعيد التوجيه إلى خادم الملفات وHttpURLConnection يتبعه تلقائياً.
     */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (receivedBytes: Long, totalBytes: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val target = apkFile(context, info)
        if (info.isCompleteApk(target)) return@withContext target

        val dir = updatesDir(context)
        if (!dir.exists()) dir.mkdirs()
        val partial = File(dir, target.name + ".part")
        partial.delete()

        val connection = URL(info.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "noor-almuslim/${BuildConfig.VERSION_NAME}")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw UpdateException("تعذر تنزيل التحديث (استجابة $code)")
            }

            val total = connection.contentLengthLong.takeIf { it > 0 }
                ?: if (info.apkSizeBytes > 0) info.apkSizeBytes else -1L
            var received = 0L
            var lastReported = -1L

            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        received += read
                        // التقدم يُبلَّغ كل ~256 ك.ب كي لا نغرق الـ StateFlow بإطارات فارغة
                        if (received - lastReported >= 256 * 1024 || received == total) {
                            lastReported = received
                            onProgress(received, if (total > 0) total else info.apkSizeBytes)
                        }
                    }
                    output.flush()
                }
            }

            // التحقق من الاكتمال: الحجم المعروف يجب أن يطابق
            if (info.apkSizeBytes > 0 && received != info.apkSizeBytes) {
                partial.delete()
                throw UpdateException("التنزيل غير مكتمل — حاول من جديد")
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            onProgress(received, if (total > 0) total else received)
            target
        } catch (t: Throwable) {
            partial.delete()
            throw t
        } finally {
            connection.disconnect()
        }
    }
}
