package com.sawan4art.noor.data.mushaf

import android.content.Context
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.QuranAudioMath
import com.sawan4art.noor.domain.mushaf.QuranReciters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * مكتبة أصوات الشيوخ — توجيه المالك ٢٠٢٦-١١-٠٦:
 * «يتم تحميل اصواتهم من قبل المستخدم نفسه … مع امكانيه تحميل كل سوره
 * على حده او تحميل المصحف كامل لصوت الشيخ المحدد».
 *
 * • **التنزيل في مجال التطبيق نفسه** (إصلاح alpha56 — بلاغ المالك:
 *   «تحميل اصوات الشيوخ لا يعمل»): كان التنزيل مرتبطاً بمجال الواجهة
 *   فمات مع إغلاق اللوحة أو مغادرة الإعدادات؛ الآن يعيش في مجالٍ مستقل
 *   يكمل ما بدأه حتى لو أُغلقت كل الشاشات، وبمحاولتين: everyayah أولاً
 *   فإن فشل الآية جُرّبت نظيرتها في مكتبة islamic.network للشيوخ
 *   الذين لهم إصدارٌ هناك.
 * • **ملفات مؤقتة تُسمّى نهائياً بعد اكتمالها فقط** — فالملف الحاضر
 *   مضمونٌ كاملٌ لا ناقص، والتنزيل المقطوع يستأنف من حيث توقف لأن
 *   الموجود يُتخطى أولاً. مجلدات التطبيق الخارجية الخاصة
 *   (getExternalFilesDir) فلا إذن تخزين ولا يلمسها أحد غير التطبيق.
 * • **التشغيل محلي فقط** (توجيه المالك alpha56: «انا عايز كل الاصوات
 *   يجب تحميلها من قبل المستخدم»): لا بثٌّ من الشبكة إطلاقاً —
 *   [localAyahPath] يرجع ملف الشيخ إن نزل وإلا null فيطلب المشغّل
 *   من واجهة المصحف تأكيدَ التنزيل أولاً.
 * • **الإلغاء والمسح**: ضغطة توقف التنزيل، وضغطة تمسح السورة أو الشيخ
 *   كله، وعدّاد الملفات الحقيقي من القرص هو مصدر العرض الوحيد.
 */
object QuranAudioLibrary {

    /** حالة تنزيل واحد (شيخ × سورة) أو المصحف كاملاً */
    data class DownloadState(
        /** معرّف الشيخ */
        val reciterId: String,
        /** السورة — صفر يعني المصحف كاملاً */
        val sura: Int,
        /** عدد الملفات المكتملة حتى الآن */
        val done: Int,
        /** العدد الكلي المتوقع */
        val total: Int,
        /** هل التنزيل يعمل الآن */
        val active: Boolean = true,
        /** رسالة خطأ أخيرة — فارغة تعني سلامة */
        val error: String = "",
    ) {
        val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    }

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())

    /** التنزيلات الجارية/المنتهية — المفتاح «شيخ:سورة» وسورة ٠ للمصحف كله */
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads

    /**
     * مجال التنزيل المستقل — حياة التطبيق لا حياة الشاشة؛ إغلاق اللوحة
     * أو صفحة الإعدادات أو المصحف لا يقتل تنزيلاً جارياً.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** تنزيلات الجري — مفتاحها «شيخ:سورة» وواحد فقط لكل مفتاح */
    private val running = ConcurrentHashMap<String, Deferred<Boolean>>()

    /** مجلد صوت القرآن للشيخ — خارجي خاص التطبيق، والقياس بالبايت دائم */
    fun reciterDir(context: Context, reciterId: String): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "quran_audio/$reciterId")
    }

    /** ملف آية الشيخ المحلي — موجودٌ أم لا يعرف المُنادي بنفسه */
    fun ayahFile(context: Context, reciterId: String, sura: Int, ayah: Int): File =
        File(reciterDir(context, reciterId), QuranAudioMath.ayahFileName(sura, ayah))

    /** هل آية الشيخ موجودة بالجهاز؟ */
    fun hasAyah(context: Context, reciterId: String, sura: Int, ayah: Int): Boolean {
        val f = ayahFile(context, reciterId, sura, ayah)
        return f.isFile && f.length() > 0L
    }

    /**
     * مسار آية الشيخ **المحمَّل على الجهاز فقط** — null إن غاب الملف
     * (توجيه المالك alpha56: لا بثٌّ من الشبكة، كل الأصوات تحميلٌ
     * من المستخدم) — والمشغّل يطلب تنزيلَ الغائب قبل تشغيله.
     */
    fun localAyahPath(context: Context, reciterId: String, sura: Int, ayah: Int): String? {
        val local = ayahFile(context, reciterId, sura, ayah)
        return if (local.isFile && local.length() > 0L) local.absolutePath else null
    }

    /** عدد ملفات السورة الموجودة فعلاً على القرص — مصدر العرض الوحيد */
    fun surahDownloadedCount(context: Context, reciterId: String, sura: Int, surahAyahCount: Int): Int {
        var n = 0
        for (ayah in 1..surahAyahCount.coerceAtLeast(0)) {
            if (hasAyah(context, reciterId, sura, ayah)) n++
        }
        return n
    }

    /** عدد ملفات الشيخ كلها على القرص — لصف القائمة وعرض الحجم */
    fun totalDownloadedCount(context: Context, reciterId: String): Int {
        val dir = reciterDir(context, reciterId)
        if (!dir.isDirectory) return 0
        return dir.listFiles()?.count { it.isFile && it.extension == "mp3" } ?: 0
    }

    /** مجموع بايتات صوت الشيخ على القرص */
    fun totalBytes(context: Context, reciterId: String): Long {
        val dir = reciterDir(context, reciterId)
        if (!dir.isDirectory) return 0L
        return dir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    /** مفتاح حالة التنزيل في الخريطة */
    private fun key(reciterId: String, sura: Int) = "$reciterId:$sura"

    /** هل هذا التنزيل يعمل الآن؟ */
    fun isActive(reciterId: String, sura: Int): Boolean =
        _downloads.value[key(reciterId, sura)]?.active == true

    /**
     * بدء تنزيل سورة — أو المصحف كاملاً بسورة صفر — **في مجال التطبيق**:
     * يكمل لو أُغلقت الشاشة كلها، وإن كان هو أصلاً جارياً أُعيد نافجه
     * نفسه فلا يتضاعف. المُنادي إن أراد انتظار الاكتمال awaits النافج.
     */
    fun start(context: Context, reciterId: String, sura: Int, surahCounts: Map<Int, Int>): Deferred<Boolean> {
        val stateKey = key(reciterId, sura)
        val appContext = context.applicationContext
        return synchronized(running) {
            running[stateKey] ?: scope.async {
                try {
                    downloadInternal(appContext, reciterId, sura, surahCounts, stateKey)
                } finally {
                    running.remove(stateKey)
                }
            }.also { running[stateKey] = it }
        }
    }

    /**
     * تنزيل فعلي متتابع — كل آية موجودة تُتخطى فالاستئناف مجاني،
     * والملف يُكتب بمؤقتٍ ثم يُسمّى نهائياً بعد اكتماله فلا يظل ناقصاً
     * واقفاً باسمه الصحيح.
     *
     * **مرونة alpha57**: فشل الآية الواحدة قاطعاً (أربع محاولات) لا يُميت
     * التنزيل كله — تُحصى الفاشلة ويُكمل الطريق إلى آخر الآيات ثم يُبلغ
     * «اكتمل مع تعذّر N» — إعادة الضغطة تستأنف من الموجود فتحاول الفاشل
     * وحده (بلاغ المالك: «تحميل اصوات الشيوخ مش شغال» — كان التنزيل
     * كله يسقط على أول آيةٍ عالقة في الشبكة).
     */
    private suspend fun downloadInternal(
        context: Context,
        reciterId: String,
        sura: Int,
        surahCounts: Map<Int, Int>,
        stateKey: String,
    ): Boolean {
        val reciter = QuranReciters.byId(reciterId)
        val dir = reciterDir(context, reciterId)
        dir.mkdirs()

        // قائمة العمل: سورة واحدة أو ١١٤ للمصحف كامل
        val surahs = if (sura in 1..114) listOf(sura) else (1..114)
        val total = surahs.sumOf { QuranAudioMath.surahFileCount(it, surahCounts[it] ?: 0) }
        var done = 0
        var failed = 0
        var cancelled = false

        fun publish(active: Boolean, error: String = "") {
            _downloads.value = _downloads.value + (
                stateKey to DownloadState(
                    reciterId = reciterId, sura = sura,
                    done = done, total = total, active = active, error = error,
                )
                )
        }
        publish(active = true)

        outer@ for (s in surahs) {
            val count = QuranAudioMath.surahFileCount(s, surahCounts[s] ?: 0)
            for (ayah in 1..count) {
                // العقد مع نافج البدء — إلغاؤه يقيم `isActive` هنا إلى false
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                    cancelled = true
                    break@outer
                }
                val target = ayahFile(context, reciterId, s, ayah)
                if (target.isFile && target.length() > 0L) {
                    done++
                    publish(active = true)
                    continue
                }
                var ok = withContext(Dispatchers.IO) {
                    fetchAyahFile(context, reciter, s, ayah, target)
                }
                if (!ok) {
                    // فشل شبكة — مهلة قصيرة ثم محاولة الآية نفسها مجدداً
                    // حتى أربع مرات، فإن استعصت سُجّلت فاشلةً وكُمل الطريق
                    for (attempt in 1..4) {
                        delay(1_500L * attempt)
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            cancelled = true
                            break@outer
                        }
                        if (withContext(Dispatchers.IO) { fetchAyahFile(context, reciter, s, ayah, target) }) {
                            ok = true
                            break
                        }
                    }
                }
                if (ok) {
                    done++
                    publish(active = true)
                } else {
                    failed++
                    publish(active = true, error = "")
                }
            }
        }
        val message = when {
            cancelled -> ""
            failed > 0 -> "اكتمل مع تعذّر ${MushafSurahs.arabicDigits(failed)} آية — أعد المحاولة لتكملتها، والمحمّل باقٍ بلا مس"
            else -> ""
        }
        publish(active = false, error = message)
        return !cancelled
    }

    /**
     * جلب آية واحدة — محاولتان: everyayah برواية الشيخ الأصلية، فإن
     * خذلت جُرّبت نظيرتها في islamic.network (لمن له إصدار هناك) —
     * والتنزيل إلى ملف مؤقت ثم تسميته نهائياً بعد اكتماله.
     */
    private fun fetchAyahFile(
        context: Context,
        reciter: QuranReciters.Reciter,
        sura: Int,
        ayah: Int,
        target: File,
    ): Boolean {
        if (downloadTo(QuranReciters.ayahUrl(reciter, sura, ayah), target)) return true
        // الاحتياط — بترقيم الآية الكلي من قاعدة المصحف نفسها
        val global = MushafDb.globalAyah(context, sura, ayah)
        val fallback = QuranReciters.fallbackUrl(reciter, global) ?: return false
        return downloadTo(fallback, target)
    }

    /** تنزيل رابطٍ إلى الملف الهدف — false لأي عطلٍ أو ردٍّ غير سليم */
    private fun downloadTo(url: String, target: File): Boolean = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            // مصدر التلاوات يقوده ويطوله — صبرٌ أطول ينجّي الآيات العالقة
            readTimeout = 30_000
            instanceFollowRedirects = true
            // بعض المستضيفين يردّون 403 على الطلب العريّ بلا هوية
            setRequestProperty("User-Agent", "NoorAlMuslim/0.5 (Android)")
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            return@runCatching false
        }
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.part")
        conn.inputStream.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        conn.disconnect()
        if (tmp.length() <= 0L) {
            tmp.delete()
            return@runCatching false
        }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
        tmp.length() > 0L
    }.getOrDefault(false)

    /** إيقاف تنزيل جارٍ — بلا مسحٍ لما اكتمل منه */
    fun cancel(reciterId: String, sura: Int) {
        running[key(reciterId, sura)]?.cancel()
        val current = _downloads.value[key(reciterId, sura)]
        if (current != null) {
            _downloads.value = _downloads.value + (
                key(reciterId, sura) to current.copy(active = false)
                )
        }
    }

    /** مسح سورة الشيخ من الجهاز — ملفات آياتها المسطّحة في مجلد الشيخ */
    fun deleteSurah(context: Context, reciterId: String, sura: Int, surahAyahCount: Int) {
        for (ayah in 1..surahAyahCount.coerceAtLeast(0)) {
            ayahFile(context, reciterId, sura, ayah).delete()
        }
    }

    /** مسح صوت الشيخ كله — مجلده من الجذور */
    fun deleteReciter(context: Context, reciterId: String) {
        cancel(reciterId, 0)
        for (s in 1..114) cancel(reciterId, s)
        reciterDir(context, reciterId).deleteRecursively()
    }

    /**
     * مسح كل أصوات القرآن من الجهاز — تنهار كل الحالات النشطة معه.
     * نادر الاستعمال من واجهة المكتبة نفسها (زر مسح الكل).
     */
    fun deleteAll(context: Context) {
        for (reciter in QuranReciters.ALL) deleteReciter(context, reciter.id)
        _downloads.value = emptyMap()
    }
}
