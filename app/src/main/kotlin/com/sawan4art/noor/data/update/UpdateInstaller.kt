package com.sawan4art.noor.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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
 *
 * **alpha61 — بلاغ المالك: «تحميل التحديث صار بطيء»**: النسخة تُنزَّل
 * من جت هب دفعةً واحدةً على اتصالٍ واحد، وقطعُ الاتصال وسط الطريق
 * كان يعيد النزيل من الصفر. فالآن:
 * - **تنزيل متوازٍ بالتقطيع**: النسخ الأكبر من حدٍّ معين تُقسَّم أربعة
 *   قطعٍ تنزّل في وقتٍ واحد (كلٌّ على اتصاله) فتتضاعف السرعة على
 *   الروابط البطيئة، ثم تُلَمّ القطع بترتيبها في ملفٍّ واحد.
 * - **استئناف حقيقي**: كل قطعةٍ تُكتب ملفاً مستقلاً، وفشلُ اتصالٍ
 *   يعيد المحاولة من حيث توقف (طلب Range) لا من الصفر — وضغطة
 *   «تحديث الآن» التالية تكمل ما تبقى كذلك.
 * - **متابعة التحويلات**: روابط جت هب تحوّل (٣٠٢) إلى مخزن الأصول —
 *   التحويلات تُتابَع يدوياً (حتى ٥) مع حمل الطلب نفسه فلا يضيع Range.
 *
 * [UpdateDownloadPlan] الوحدة النقية للتقطيع — مختبرةٌ آلياً بلا شبكة.
 */
object UpdateInstaller {

    private const val APK_DIR = "updates"
    private const val APK_FILE = "noor-update.apk"
    private const val PART_FILE = "noor-update.apk.part"
    private const val APK_MIME = "application/vnd.android.package-archive"

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000

    /** حد التنزيل المتوازي — الأصغر من هذا ينزل باتصالٍ واحد أهدأ */
    internal const val PARALLEL_MIN_BYTES = 8L * 1024 * 1024

    /** عدد الاتصالات المتوازية القصوى */
    internal const val MAX_CONNECTIONS = 4

    /** محاولات كل قطعةٍ قبل الاستسلام — والاستئناف من الموجود في كلٍّ منها */
    private const val CHUNK_ATTEMPTS = 4

    /** أقصى تحويلاتٍ تُتابَع (جت هب يحوّل مرةً إلى مخزن الأصول غالباً) */
    private const val MAX_REDIRECTS = 5

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
     * تنزيل النسخة — متوازٍ بالتقطيع ما أمكن ومستأنفٌ دائماً، وتقدمه
     * يُبلَّغ كل نصف ثانية تقريباً. الملف الجديد يُكتب بصيغة .part
     * فلا يظن مثبّت النظام ملفاً ناقصاً مكتمل، وبعد الاكتمال يُعاد
     * تسميته لاسم التثبيت النهائي.
     */
    fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): DownloadState {
        val dir = File(context.getExternalFilesDir(null), APK_DIR)
        if (!dir.exists()) dir.mkdirs()
        val final = File(dir, APK_FILE)
        val part = File(dir, PART_FILE)
        // ملفٌ قديمٌ مكتملٌ لم يُثبَّت يُمسح — سنبني النسخة من جديد
        final.delete()
        part.delete()

        return try {
            // (١) استطلاع: الحجم الكلي وهل يخدم المخدم نطاقات Range؟
            val probe = probe(info.apkUrl)
            val total = probe.total.takeIf { it > 0 } ?: info.apkSizeBytes
            val parallel = probe.ranges && total >= PARALLEL_MIN_BYTES

            val result = if (parallel && total > 0) {
                downloadParallel(info.apkUrl, dir, total, onProgress)
            } else {
                downloadSingle(info.apkUrl, part, total, onProgress)
            }
            if (result != null) return result

            // (٣) التحقق النهائي: الطول مطابق؟ ثم التسمية النهائية
            if (total > 0 && part.length() != total) {
                part.delete()
                return DownloadState.Failed(
                    "اكتمل التنزيل بناقص (${part.length()} من $total) — أعد المحاولة سيكمل الباقي",
                )
            }
            if (!part.renameTo(final)) {
                part.delete()
                return DownloadState.Failed("تعذّر تجهيز ملف التحديث على الجهاز — حاول من جديد")
            }
            DownloadState.Done
        } catch (e: IOException) {
            DownloadState.Failed("توقّف التنزيل — تأكد من اتصالك بالإنترنت وحاول من جديد")
        } catch (e: Exception) {
            DownloadState.Failed("حدث خطأ غير متوقع أثناء التنزيل — حاول من جديد")
        }
    }

    /** نتيجة الاستطلاع — الحجم الكلي وهل النطاقات مدعومة */
    private class Probe(val total: Long, val ranges: Boolean)

    /**
     * استطلاع المخدم: طلبٌ بنطاق `bytes=0-0` — ٢٠٦ يعني نطاقاتٌ مدعومة
     * والحجم من رأس Content-Range، و٢٠٠ يعني دعماً بلا نطاقات والحجم
     * من Content-Length. التحويلات تُتابَع يدوياً.
     */
    private fun probe(url: String): Probe {
        var current = url
        repeat(MAX_REDIRECTS) {
            val conn = open(current, from = 0L, to = 0L)
            try {
                when (conn.responseCode) {
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location")
                        if (location.isNullOrBlank()) throw IOException("تحويل بلا وجهة")
                        current = absolute(current, location)
                        return@repeat
                    }
                    HttpURLConnection.HTTP_PARTIAL -> {
                        return Probe(
                            total = totalFromContentRange(conn.getHeaderField("Content-Range")),
                            ranges = true,
                        )
                    }
                    HttpURLConnection.HTTP_OK -> {
                        return Probe(conn.contentLengthLong.takeIf { it > 0 } ?: -1L, ranges = false)
                    }
                    else -> throw IOException("استجابة غير متوقعة (${conn.responseCode})")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("تعذّر الوصول للنسخة — تحويلات كثيرة")
    }

    /**
     * التنزيل المتوازٍ: تقطيعٌ خُططه [UpdateDownloadPlan]، كل قطعةٍ على
     * اتصالٍ مستقلٍ بملفٍ مستقل وإعادة محاولةٍ مستأنفة، ثم لمٌّ بالترتيب.
     * null يعني النجاح (الملف .part جاهز)، وغير null حالة فشلٍ جاهزة العرض.
     */
    private fun downloadParallel(
        url: String,
        dir: File,
        total: Long,
        onProgress: (Int) -> Unit,
    ): DownloadState? {
        val chunks = UpdateDownloadPlan.plan(total, PARALLEL_MIN_BYTES, MAX_CONNECTIONS)
        val parts = chunks.map { File(dir, "$PART_FILE.p${it.index}") }
        val written = AtomicLong(0L)
        var lastReport = -1

        fun reportProgress() {
            if (total <= 0) return
            val pct = ((written.get() * 100) / total).toInt().coerceIn(0, 99)
            if (pct != lastReport) {
                lastReport = pct
                onProgress(pct)
            }
        }

        // استئنافٌ مبدئي: القطع الموجودة من محاولةٍ سابقة تُحسب فوراً
        for ((i, part) in parts.withIndex()) {
            val already = part.length()
            if (already > 0) {
                val cap = chunks[i].length()
                written.addAndGet(minOf(already, cap))
            }
        }
        reportProgress()

        val pool = Executors.newFixedThreadPool(chunks.size)
        try {
            val failed = chunks.map { chunk ->
                pool.submit<Boolean> {
                    val part = parts[chunk.index]
                    for (attempt in 1..CHUNK_ATTEMPTS) {
                        try {
                            if (downloadChunk(url, chunk, part, written)) return@submit true
                        } catch (_: Exception) {
                            // محاولة أخرى بعد مهلةٍ قصيرة — والقطعة مستأنفة من موجدها
                        }
                        Thread.sleep(1_500L * attempt)
                    }
                    false
                }
            }.map { it.get() }
            if (failed.any { !it }) {
                // القطع الباقية على القرص — الضغطة القادمة تكملها
                return DownloadState.Failed(
                    "تعذّر إتمام جزء من التنزيل — أعد المحاولة وسيكمل من حيث توقف",
                )
            }
        } finally {
            pool.shutdown()
            runCatching { pool.awaitTermination(10, TimeUnit.SECONDS) }
        }

        reportProgress()
        // اللمّ: القطع بترتيبها في ملف التثبيت المؤقت
        val part = File(dir, PART_FILE)
        BufferedOutputStream(FileOutputStream(part)).use { out ->
            for (p in parts) {
                BufferedInputStream(FileInputStream(p)).use { input ->
                    input.copyTo(out, 512 * 1024)
                }
            }
            out.flush()
        }
        for (p in parts) p.delete()
        return null
    }

    /**
     * تنزيل قطعةٍ واحدة — من موجدها إن استُؤنفت (Range من الطول الحالي).
     * true اكتملت القطعة كاملةً بحجمها المطلوب.
     */
    private fun downloadChunk(
        url: String,
        chunk: UpdateDownloadPlan.Chunk,
        part: File,
        written: AtomicLong,
    ): Boolean {
        part.parentFile?.mkdirs()
        var from = chunk.start + part.length()
        if (from > chunk.endInclusive + 1) return true // مكتملة سلفاً
        var current = url
        repeat(MAX_REDIRECTS) {
            val conn = open(current, from = from, to = chunk.endInclusive)
            try {
                when (conn.responseCode) {
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location") ?: throw IOException("تحويل بلا وجهة")
                        current = absolute(current, location)
                        return@repeat
                    }
                    HttpURLConnection.HTTP_PARTIAL, HttpURLConnection.HTTP_OK -> {
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            // المخدم تجاهل النطاق — نبدا القطعة من صفر
                            written.addAndGet(-part.length())
                            part.delete()
                            from = chunk.start
                        }
                        conn.inputStream.use { input ->
                            FileOutputStream(part, part.length() > 0L && from > chunk.start).use { out ->
                                val buffer = ByteArray(128 * 1024)
                                var remaining = chunk.endInclusive + 1 - from
                                while (remaining > 0) {
                                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                                    if (read <= 0) break
                                    out.write(buffer, 0, read)
                                    written.addAndGet(read.toLong())
                                    remaining -= read
                                }
                                out.flush()
                            }
                        }
                        return part.length() == chunk.length()
                    }
                    else -> throw IOException("استجابة غير متوقعة (${conn.responseCode})")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("تحويلات كثيرة")
    }

    /**
     * الطريق الواحد — للمخدمات بلا نطاقات أو النسخ الصغيرة، وباستئنافٍ
     * من الموجود كذلك. null نجاح، غيرها حالة فشل.
     */
    private fun downloadSingle(
        url: String,
        part: File,
        total: Long,
        onProgress: (Int) -> Unit,
    ): DownloadState? {
        var lastReport = -1
        fun report() {
            if (total <= 0) return
            val pct = ((part.length() * 100) / total).toInt().coerceIn(0, 99)
            if (pct != lastReport) {
                lastReport = pct
                onProgress(pct)
            }
        }
        var from = part.length()
        var current = url
        repeat(MAX_REDIRECTS) {
            val conn = open(current, from = from, to = -1L)
            try {
                when (conn.responseCode) {
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location") ?: throw IOException("تحويل بلا وجهة")
                        current = absolute(current, location)
                        return@repeat
                    }
                    HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL -> {
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            // تجاهل النطاق — من الصفر
                            from = 0L
                        }
                        val append = from > 0L
                        conn.inputStream.use { input ->
                            FileOutputStream(part, append).use { out ->
                                val buffer = ByteArray(128 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    out.write(buffer, 0, read)
                                    report()
                                }
                                out.flush()
                            }
                        }
                        return null
                    }
                    else -> throw IOException("استجابة غير متوقعة (${conn.responseCode})")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("تحويلات كثيرة")
    }

    /** فتح اتصالٍ بنطاق اختياري — `to = -1` يعني «حتى النهاية» */
    private fun open(url: String, from: Long, to: Long): HttpURLConnection {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false // التحويل يدوياً كي يبقى نطاق الطلب
            setRequestProperty("User-Agent", "noor-almuslim/0.5 (Android update)")
            if (from > 0L || to >= 0L) {
                setRequestProperty(
                    "Range",
                    if (to >= from) "bytes=$from-$to" else "bytes=$from-",
                )
            }
        }
        conn.connect()
        return conn
    }

    /** ربط وجهة التحويل مع عنوانٍ نسبي */
    private fun absolute(base: String, location: String): String =
        if (location.startsWith("http")) location else URL(URL(base), location).toString()

    /** الحجم الكلي من رأس `Content-Range: bytes 0-0/12345678` */
    private fun totalFromContentRange(header: String?): Long {
        if (header.isNullOrBlank()) return -1L
        val slash = header.lastIndexOf('/')
        if (slash < 0 || slash == header.length - 1) return -1L
        return header.substring(slash + 1).trim().toLongOrNull() ?: -1L
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

/**
 * خطة تقطيع التنزيل — وحدةٌ نقية تُختبر آلياً: تقسيم [totalBytes] إلى
 * [maxConnections] قطعٍ متساوية تقريباً (الفارق بايتٌ واحد على أكثرها)
 * بشرط ألا يُقسَّم ما دون [minTotal] أصلاً.
 */
object UpdateDownloadPlan {

    /** قطعةٌ واحدة من الخطة — من [start] إلى [endInclusive] ضمنتاً */
    data class Chunk(val index: Int, val start: Long, val endInclusive: Long) {
        /** طول القطعة بالبايت */
        fun length(): Long = endInclusive - start + 1
    }

    /**
     * الخطة: صفر بايت = بلا قطع، وما دون [minTotal] قطعةٌ واحدة، وما فوق
     * يُقسَّم إلى [maxConnections] قطعٍ متتالٍ تغطي كل بايتٍ مرةً واحدة.
     */
    fun plan(totalBytes: Long, minTotal: Long, maxConnections: Int): List<Chunk> {
        if (totalBytes <= 0) return emptyList()
        if (totalBytes < minTotal || maxConnections <= 1) {
            return listOf(Chunk(0, 0, totalBytes - 1))
        }
        val count = maxConnections
        val base = totalBytes / count
        val remainder = totalBytes % count
        val chunks = mutableListOf<Chunk>()
        var start = 0L
        for (i in 0 until count) {
            val length = base + if (i < remainder) 1L else 0L
            if (length <= 0) continue
            chunks.add(Chunk(chunks.size, start, start + length - 1))
            start += length
        }
        return chunks
    }
}
