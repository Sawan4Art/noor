package com.sawan4art.noor.data.mushaf

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
import com.sawan4art.noor.data.update.UpdateDownloadPlan

/**
 * ناقل ضغطات السور (alpha62) — بلاغ المالك: «مازال هناك اصوات للشيخ
 * لا يتم تحميلها» و«الذي يعمل بطئ في التحميل ايضا».
 *
 * السورة على مرآة جت هب أصلٌ واحدٌ قد يبلغ عشرات الميغابايتات
 * (روايات ١٢٨–١٩٢kbps) وكانت تُنزَّل على اتصالٍ واحدٍ بمهلة قراءةٍ
 * ثلاثين ثانية — فالاتصال البطيء يُقطَع وسط الطريق ويعيد التنزيل
 * من الصفر فيبدو «لا يُحمَّل» أبداً.
 *
 * هنا نفس علاج ناقل التحديث (alpha61):
 * - **استطلاع** بح نطاق `0-0`: الحجم الكلي وهل المخدم يخدم النطاقات.
 * - **تنزيل متوازٍ**: ما زاد عن ٨ ميجابايت يُقسَّم أربعة قطعٍ على
 *   اتصالاتٍ متوازية [UpdateDownloadPlan.plan] فتتضاعف السرعة.
 * - **استئناف حقيقي**: كل قطعةٍ ملفٌ مستقلٌ يُستأنف من موجده (Range)،
 *   وفشلُ الاتصال يعيد المحاولة من حيث توقف لا من الصفر.
 * - **متابعة التحويلات يدوياً**: روابط جت هب تحوّل (٣٠٢) إلى مخزن
 *   الأصول والتحويل الآلي يُضيّع رأس Range — فالمتابعة يدوية.
 */
object SurahZipFetch {

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

    /**
     * تنزيل ضغط السورة إلى الملف الهدف — true اكتمل الطول مطابقاً.
     * [onPct] يُبلَّغ بنسبة التقدم ٠..٩٩ أثناء العمل.
     */
    fun fetch(url: String, target: File, onPct: (Int) -> Unit = {}): Boolean = runCatching {
        val dir = target.parentFile ?: return@runCatching false
        if (!dir.exists()) dir.mkdirs()
        target.delete()

        // (١) استطلاع: الحجم الكلي وهل يخدم المخدم نطاقات Range؟
        val probe = probe(url)
        val total = probe.total
        val parallel = probe.ranges && total >= PARALLEL_MIN_BYTES
        val part = File(dir, "${target.name}.part")

        val failedState: String? = if (parallel && total > 0) {
            downloadParallel(url, dir, "${target.name}.part", total, onPct)
        } else {
            downloadSingle(url, part, total, onPct)
        }
        if (failedState != null) {
            return@runCatching false
        }

        // (٢) التحقق النهائي: الطول مطابق؟ ثم التسمية النهائية
        if (total > 0 && part.length() != total) {
            part.delete()
            return@runCatching false
        }
        if (part.length() <= 0L) {
            part.delete()
            return@runCatching false
        }
        target.delete()
        part.renameTo(target)
    }.getOrDefault(false)

    /** نتيجة الاستطلاع — الحجم الكلي وهل النطاقات مدعومة */
    private class Probe(val total: Long, val ranges: Boolean)

    /** استطلاع المخدم بنطاق `bytes=0-0` — ٢٠٦ نطاقاتٌ و٢٠٠ بلاها */
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
                    }
                    HttpURLConnection.HTTP_PARTIAL -> return Probe(
                        total = contentRangeTotal(conn.getHeaderField("Content-Range")),
                        ranges = true,
                    )
                    HttpURLConnection.HTTP_OK -> return Probe(
                        conn.contentLengthLong.takeIf { it > 0 } ?: -1L,
                        ranges = false,
                    )
                    else -> throw IOException("استجابة غير متوقعة (${conn.responseCode})")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("تعذّر الوصول للأصل — تحويلات كثيرة")
    }

    /**
     * التنزيل المتوازي: تقطيعٌ بـ[UpdateDownloadPlan.plan]، كل قطعةٍ على
     * اتصالٍ مستقلٍ بملفٍ مستقلٍ وإعادة محاولةٍ مستأنفة، ثم لمٌّ بالترتيب.
     * null نجاح، وغير null فشلٌ (والقطع الباقية تُستأنف في المحاولة القادمة).
     */
    private fun downloadParallel(
        url: String,
        dir: File,
        partName: String,
        total: Long,
        onPct: (Int) -> Unit,
    ): String? {
        val chunks = UpdateDownloadPlan.plan(total, PARALLEL_MIN_BYTES, MAX_CONNECTIONS)
        val parts = chunks.map { File(dir, "$partName.p${it.index}") }
        val written = AtomicLong(0L)
        var lastReport = -1

        fun reportProgress() {
            if (total <= 0) return
            val pct = ((written.get() * 100) / total).toInt().coerceIn(0, 99)
            if (pct != lastReport) {
                lastReport = pct
                onPct(pct)
            }
        }

        // استئنافٌ مبدئي: القطع الموجودة من محاولةٍ سابقة تُحسب فوراً
        for ((i, part) in parts.withIndex()) {
            val already = part.length()
            if (already > 0) {
                written.addAndGet(minOf(already, chunks[i].length()))
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
                return "تعذّر إتمام جزء من التنزيل — أعد المحاولة وسيكمل من حيث توقف"
            }
        } finally {
            pool.shutdown()
            runCatching { pool.awaitTermination(10, TimeUnit.SECONDS) }
        }

        reportProgress()
        // اللمّ: القطع بترتيبها في ملف الضغط المؤقت
        val part = File(dir, partName)
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

    /** تنزيل قطعةٍ واحدة — من موجدها إن استُؤنفت (Range من الطول الحالي) */
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

    /** الطريق الواحد — للمخدمات بلا نطاقات أو الأصول الصغيرة، باستئنافٍ كذلك */
    private fun downloadSingle(
        url: String,
        part: File,
        total: Long,
        onPct: (Int) -> Unit,
    ): String? {
        var lastReport = -1
        fun report() {
            if (total <= 0) return
            val pct = ((part.length() * 100) / total).toInt().coerceIn(0, 99)
            if (pct != lastReport) {
                lastReport = pct
                onPct(pct)
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
                    }
                    HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL -> {
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            from = 0L // تجاهل النطاق — من الصفر
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
            setRequestProperty("User-Agent", "NoorAlMuslim/0.5 (Android audio)")
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

    /** الحجم الكلي من رأس `Content-Range: bytes 0-0/12345678` — دالةٌ نقية تُختبر */
    internal fun contentRangeTotal(header: String?): Long {
        if (header.isNullOrBlank()) return -1L
        val slash = header.lastIndexOf('/')
        if (slash < 0 || slash == header.length - 1) return -1L
        return header.substring(slash + 1).trim().toLongOrNull() ?: -1L
    }
}
