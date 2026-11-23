package com.sawan4art.noor.data.tafsir

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.sawan4art.noor.data.mushaf.MushafDb
import com.sawan4art.noor.domain.tafsir.TafsirEditions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

/**
 * مكتبة التفاسير — v1.81 (بلاغ المالك: «اضف التفاسير التاليه الجلالين
 * و القرطبي و ابن كثير الطبري السعدي و طبعا مع ابقاء التفسير الميسر»):
 *
 * • **القراءة** — الميسر من قاعدة المصحف المضمّنة نفسها عبر
 *   [MushafDb.tafsirRange] (يعمل فورًا بلا إنترنت)، وغيره من قاعدة
 *   sqlite الموحّدة المنزَّلة في filesDir/tafsir.
 * • **التنزيل** — حزمة zip من فرع الأصول بخصائص استئنافٍ من موجدها
 *   (رأس Range كدرس SurahZipFetch: التحويل اليدوي كي لا يضيع الرأس)،
 *   وتحقق sha256 قبل فتح القاعدة (بصمة كل طبعة في كتالوج
 *   [TafsirEditions]) ثم فحص عدد الآيات (٦٢٣٦) قبل التسمية مكتملةً.
 * • **الحالة** — تدفق [downloads] يصير للواجهة (القسم الجديد
 *   «التفاسير» في الإعدادات) ويعمل ضمن نشاط التطبيق بلا خدمةٍ أمامية
 *   — الحزمة الأكبر ٨ م.ب تنزيلٌ من الدقائق، والملف الجزئي يبقى
 *   للإكمال لو ماتت العملية.
 */
object TafsirLibrary {

    /** حالة تنزيل طبعةٍ واحدة — fraction بين ٠ و١ للشريط */
    data class DownloadState(
        val editionId: String,
        val running: Boolean,
        val fraction: Float,
        val failedMessage: String? = null,
    )

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads

    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val running = AtomicBoolean(false)

    /** مجلد القواعد المنزَّلة */
    private fun dir(context: Context): File = File(context.filesDir, "tafsir").apply { mkdirs() }

    fun dbFile(context: Context, editionId: String): File = File(dir(context), "$editionId.db")

    /** هل الطبعة جاهزة للقراءة؟ (المضمّنة دائماً جاهزة) */
    fun isDownloaded(context: Context, editionId: String): Boolean {
        val ed = TafsirEditions.byId(editionId) ?: return false
        if (ed.bundled) return true
        val f = dbFile(context, editionId)
        return f.exists() && f.length() > 1000L
    }

    /** حذف طبعةٍ منزَّلة — والمضمّنة تعود كأنها لم تُمسّ */
    fun remove(context: Context, editionId: String) {
        runCatching {
            dbFile(context, editionId).takeIf { it.exists() }?.delete()
            File(dir(context), "$editionId.zip.part").delete()
            _downloads.value = _downloads.value - editionId
        }
    }

    /**
     * نص تفسير آيةٍ واحدة — null يعني غير متاح (الآية بلا تعليقٍ في
     * هذه الطبعة أو الطبعة غير منزَّلة) فتُعرض ملاحظةٌ لطيفةٌ بدله.
     */
    suspend fun textFor(context: Context, editionId: String, sura: Int, ayah: Int): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (editionId == TafsirEditions.MUYASSAR_ID) {
                    MushafDb.tafsirRange(context, sura, ayah, ayah)
                        .firstOrNull()?.second?.takeIf { it.isNotBlank() }
                } else {
                    if (!isDownloaded(context, editionId)) return@runCatching null
                    openDb(context, editionId).use { db ->
                        db.rawQuery(
                            "SELECT text FROM tafsir WHERE sura=? AND ayah=?",
                            arrayOf(sura.toString(), ayah.toString()),
                        ).use { c ->
                            if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() } else null
                        }
                    }
                }
            }.getOrNull()
        }

    /** نصوص مُدى آيات — للوحة التفسير في المصحف */
    suspend fun rangeFor(
        context: Context,
        editionId: String,
        sura: Int,
        lo: Int,
        hi: Int,
    ): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (editionId == TafsirEditions.MUYASSAR_ID) {
                MushafDb.tafsirRange(context, sura, lo, hi)
            } else {
                if (!isDownloaded(context, editionId)) return@runCatching emptyList()
                openDb(context, editionId).use { db ->
                    db.rawQuery(
                        "SELECT ayah, text FROM tafsir WHERE sura=? AND ayah BETWEEN ? AND ? ORDER BY ayah",
                        arrayOf(sura.toString(), lo.toString(), hi.toString()),
                    ).use { c ->
                        val out = ArrayList<Pair<Int, String>>()
                        // الآيات بلا تعليقٍ في هذه الطبعة تعود نصاً فارغاً
                        // (كلاسيكياتٌ تدمج تعليق آياتٍ مجاورة) — واللوحة
                        // تعرض لها ملاحظةً بدل إخفاء الآية
                        while (c.moveToNext()) {
                            (c.getString(0) ?: "").toIntOrNull()?.let { a ->
                                out.add(a to (c.getString(1) ?: ""))
                            }
                        }
                        out
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun openDb(context: Context, editionId: String): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            dbFile(context, editionId).absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )

    /** بدء تنزيل طبعة — يضبط الحالة فورًا ثم يعمل على خيط الإدخال والإخراج */
    fun start(context: Context, editionId: String) {
        val ed = TafsirEditions.byId(editionId) ?: return
        if (ed.bundled || running.getAndSet(true)) return
        val app = context.applicationContext
        _downloads.value = _downloads.value + (editionId to DownloadState(editionId, true, 0f))
        scope.launch {
            try {
                downloadInternal(app, ed)
                _downloads.value = _downloads.value + (editionId to DownloadState(editionId, false, 1f))
            } catch (t: Throwable) {
                _downloads.value = _downloads.value + (
                    editionId to DownloadState(
                        editionId, false, 0f,
                        t.message ?: "تعذّر التنزيل — أعد المحاولة وسيكمل من حيث توقف",
                    )
                )
            } finally {
                running.set(false)
            }
        }
    }

    private fun downloadInternal(context: Context, ed: TafsirEditions.Edition) {
        val url = TafsirEditions.downloadUrl(ed.id) ?: throw IllegalStateException("طبعة بلا رابط")
        val part = File(dir(context), "${ed.id}.zip.part")
        var current = url
        // استئناف: الموجود من محاولةٍ سابقة يُبلَّغ عنه فورًا (درس الشريط الحي v1.80)
        var got = if (part.exists()) part.length() else 0L
        val total = ed.sizeBytes
        if (got > 0) report(ed, got, total)
        var redirects = 0
        while (true) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false // التحويل اليدوي كي لا يضيع رأس Range
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "NoorAlMuslim/1.81 (Android)")
                if (got > 0) setRequestProperty("Range", "bytes=$got-")
            }
            try {
                when (conn.responseCode) {
                    in 300..399 -> {
                        val loc = conn.getHeaderField("Location")
                            ?: throw IllegalStateException("تحويل بلا وجهة")
                        current = if (loc.startsWith("http")) loc
                        else URL(URL(current), loc).toString()
                        if (++redirects > 5) throw IllegalStateException("تحويلات كثيرة")
                        continue
                    }
                    HttpURLConnection.HTTP_PARTIAL, HttpURLConnection.HTTP_OK -> {
                        if (conn.responseCode == HttpURLConnection.HTTP_OK && got > 0L) {
                            got = 0L // المخدم تجاهل الاستئناف — من الصفر
                            part.delete()
                        }
                        FileOutputStream(part, got > 0L && part.exists()).use { out ->
                            conn.inputStream.use { input ->
                                val buffer = ByteArray(256 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    out.write(buffer, 0, read)
                                    got += read
                                    report(ed, got, total)
                                }
                            }
                        }
                        // التحقق: الحجم ثم البصمة ثم عدد الآيات
                        if (part.length() != total) {
                            throw IllegalStateException("الحزمة ناقصة — أعد المحاولة وسيكمل من حيث توقف")
                        }
                        val sha = MessageDigest.getInstance("SHA-256").apply {
                            part.inputStream().use { input ->
                                val b = ByteArray(1 shl 20)
                                while (true) {
                                    val r = input.read(b)
                                    if (r <= 0) break
                                    update(b, 0, r)
                                }
                            }
                        }.digest().joinToString("") { "%02x".format(it) }
                        if (sha != ed.sha256) throw IllegalStateException("بصمة الحزمة لا تطابق — أعد التنزيل")
                        extractAndValidate(context, ed, part)
                        part.delete()
                        return
                    }
                    else -> throw IllegalStateException("استجابة غير متوقعة (${conn.responseCode})")
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun report(ed: TafsirEditions.Edition, got: Long, total: Long) {
        if (total <= 0) return
        _downloads.value = _downloads.value + (
            ed.id to DownloadState(
                ed.id, true,
                (got.toFloat() / total).coerceIn(0f, 0.99f),
            )
        )
    }

    /** فك الحزمة وفحص القاعدة قبل اعتمادها — ٦٢٣٦ آية وجدولٌ سليم */
    private fun extractAndValidate(context: Context, ed: TafsirEditions.Edition, zip: File) {
        ZipFile(zip).use { zf ->
            val entry = zf.entries().asSequence().firstOrNull { it.name.endsWith(".db") }
                ?: throw IllegalStateException("حزمة بلا قاعدة")
            val out = dbFile(context, ed.id)
            zf.getInputStream(entry).use { input -> FileOutputStream(out).use { input.copyTo(it) } }
        }
        val db = openDb(context, ed.id)
        try {
            val n = db.rawQuery("SELECT COUNT(*) FROM tafsir WHERE 1", null).use { c ->
                c.moveToFirst(); c.getLong(0)
            }
            if (n != 6236L) throw IllegalStateException("قاعدة غير مكتملة ($n آية)")
        } finally {
            db.close()
        }
    }
}
