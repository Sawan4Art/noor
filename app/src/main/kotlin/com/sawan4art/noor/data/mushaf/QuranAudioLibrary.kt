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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

/**
 * مكتبة أصوات الشيوخ — توجيه المالك ٢٠٢٦-١١-٠٦:
 * «يتم تحميل اصواتهم من قبل المستخدم نفسه … مع امكانيه تحميل كل سوره
 * على حده او تحميل المصحف كامل لصوت الشيخ المحدد».
 *
 * • **التنزيل في مجال التطبيق نفسه** (إصلاح alpha56 — بلاغ المالك:
 *   «تحميل اصوات الشيوخ لا يعمل»): كان التنزيل مرتبطاً بمجال الواجهة
 *   فمات مع إغلاق اللوحة أو مغادرة الإعدادات؛ الآن يعيش في مجالٍ مستقل
 *   يكمل ما بدأه حتى لو أُغلقت كل الشاشات.
 * • **مرآة جت هب أولًا** (alpha59 — بلاغ المالك: «عايز اخلي الاصوات دي
 *   تكون موجوده على سيرفر على جت هب علشان نضمن ان التحميل يتم صح»):
 *   السورة كلها ضغطةٌ واحدة من مستودع noor-audio (إصدار audio-v1)
 *   وتُفكّ إلى آياتها، فإن غاب الأصل أو تعذر جاء دور everyayah آيةً
 *   آيةً ثم نظيرتها في مكتبة islamic.network لمن له إصدارٌ هناك.
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

    /** عمال تنزيل الآيات المتوازي — أربعة تعمل معاً (سرعة alpha61) */
    internal const val PARALLEL_AYAH_WORKERS = 4

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
        /**
         * طورٌ جارٍ يُعرض بجوار العدّاد (alpha62) — مثل «جارٍ تنزيل ضغط
         * السورة — ٤٥٪» كي لا يبدو الشريط ساكناً أثناء نزول الأصل الكبير.
         */
        val phase: String = "",
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

    // ===== طابور التنزيلات الدائم (alpha63) =====
    // **جذر «بعض الأصوات لا تُنزَّل»** (بلاغ المالك: «مازالت بعض الاصوات
    // لا يتم تنزيلها و تفشل حل المشكله من جذورها»): التنزيل يعيش في مجال
    // التطبيق لكن **العملية نفسها** لو قُتلت (الذاكرة أو المستخدم من
    // الريسنتس أو إعادة تشغيل الجهاز) مات التنزيل صامتاً بلا رسالةٍ ولا
    // استكمال — فيبدو «يفشل» مع أن المرآة سليمة (الفحص الآلي ٢١٦٦/٢١٦٦
    // أصلاً). الطابور الدائم في SharedPreferences يقرأه إقلاع التطبيق
    // فيستأنف كل تنزيلٍ متوقفٍ وحده، والحفظ قبل البدء والمسح بعد النهاية.
    private const val QUEUE_PREFS = "audio_downloads_queue"
    private const val QUEUE_KEY = "pending_json"

    /** إدخال الطابور — «شيخ:سورة» وسورة صفر للمصحف كله */
    private data class QueueEntry(val reciterId: String, val sura: Int)

    private fun queuePrefs(context: Context) =
        context.getSharedPreferences(QUEUE_PREFS, Context.MODE_PRIVATE)

    /** قراءة الطابور الحالي — الفاسد يسير بأمان إلى لا شيء */
    private fun queueLoad(context: Context): List<QueueEntry> =
        runCatching {
            org.json.JSONArray(queuePrefs(context).getString(QUEUE_KEY, "[]")).let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val r = o.optString("r")
                    val s = o.optInt("s", -1)
                    if (r.isNotBlank() && s in 0..114) QueueEntry(r, s) else null
                }
            }
        }.getOrDefault(emptyList())

    private fun queueSave(context: Context, entries: List<QueueEntry>) {
        val arr = org.json.JSONArray()
        entries.forEach { arr.put(org.json.JSONObject().put("r", it.reciterId).put("s", it.sura)) }
        queuePrefs(context).edit().putString(QUEUE_KEY, arr.toString()).apply()
    }

    private fun queueAdd(context: Context, entry: QueueEntry) {
        queueSave(context, queueLoad(context).filter { it != entry } + entry)
    }

    private fun queueRemove(context: Context, entry: QueueEntry) {
        queueSave(context, queueLoad(context).filter { it != entry })
    }

    /**
     * استكمال ما توقف — يُنادى من إقلاع التطبيق: كل إدخالٍ في الطابور
     * يُعاد إطلاقه في مجال التطبيق نفسه (والسور المتاحة تُتخطى كعادتها
     * فالاستكمال من حيث توقف). خطأٌ في إدخالٍ لا يمنع بقية الطابور.
     */
    fun resumePending(context: Context) {
        val pending = queueLoad(context)
        if (pending.isEmpty()) return
        val appContext = context.applicationContext
        scope.launch {
            val counts = runCatching {
                (1..114).associateWith { MushafDb.surahAyahCount(appContext, it) }
            }.getOrDefault(emptyMap())
            for (entry in pending) {
                // لا تُعاد إطلاقَ تنزيلٍ يعمل أصلاً (start يعيد نافجه نفسه)
                runCatching {
                    start(appContext, entry.reciterId, entry.sura, counts)
                }
            }
        }
    }

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
        // الطابور الدائم — لو ماتت العملية يُستأنف من إقلاع التطبيق (alpha63)
        queueAdd(appContext, QueueEntry(reciterId, sura))
        return synchronized(running) {
            running[stateKey] ?: scope.async {
                try {
                    downloadInternal(appContext, reciterId, sura, surahCounts, stateKey)
                } finally {
                    running.remove(stateKey)
                    // اكتمل أو أوقفه المستخدم — خرج من الطابور؛ الموت
                    // المفاجئ للعملية وحده هو الذي يُبقيه للاستكمال
                    runCatching { queueRemove(appContext, QueueEntry(reciterId, sura)) }
                }
            }.also { running[stateKey] = it }
        }
    }

    /**
     * تنزيل فعلي — كل آية موجودة تُتخطى فالاستئناف مجاني، والملف يُكتب
     * بمؤقتٍ ثم يُسمّى نهائياً بعد اكتماله فلا يظل ناقصاً واقفاً باسمه
     * الصحيح.
     *
     * **مرونة alpha57**: فشل الآية الواحدة قاطعاً لا يُميت التنزيل كله
     * — تُحصى الفاشلة ويُكمل الطريق إلى آخر الآيات ثم يُبلغ «اكتمل مع
     * تعذّر N» — إعادة الضغطة تستأنف من الموجود فتحاول الفاشل وحده.
     *
     * **سرعة alpha61** (بلاغ المالك: «بعضها يعمل و البعض الاخر لا يعمل
     * و الذي يعمل بطيء في التحميل ايضا»): الطريق الآية-آية كان متتابعاً
     * آيةً تنتظر أختها — فصار أربع آياتٍ تعمل في وقتٍ واحدٍ فيكمل
     * السور الكبيرة في رُبع الزمن، وضغطُ المرآة الذي يفشل يُعاد محاولةٌ
     * ثانية قبل الهبوط إلى الطريق القديم.
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
        val done = java.util.concurrent.atomic.AtomicInteger(0)
        var failed = 0
        var cancelled = false

        fun publish(active: Boolean, error: String = "", phase: String = "") {
            _downloads.value = _downloads.value + (
                stateKey to DownloadState(
                    reciterId = reciterId, sura = sura,
                    done = done.get(), total = total, active = active, error = error,
                    phase = phase,
                )
                )
        }
        publish(active = true)

        // فحص المساحة قبل أي بايت (alpha63): جهازٌ ممتلئٌ هو أصلٌ شائع
        // لبلاغ «يفشل ولا يكمل» — الرسالة الآن صريحة بدل تعذّرٍ غامض
        val neededBytes = if (sura == 0) 150_000_000L else 15_000_000L
        val usable = runCatching { dir.usableSpace }.getOrDefault(Long.MAX_VALUE)
        if (usable in 0 until neededBytes) {
            publish(
                active = false,
                error = "مساحة التخزين شارفت على الامتلاء — امسح تنزيلاً " +
                    "لا تستعمله أو ملفات أخرى ثم أعد المحاولة",
            )
            return false
        }

        outer@ for (s in surahs) {
            val count = QuranAudioMath.surahFileCount(s, surahCounts[s] ?: 0)
            // (١) مرآة جت هب — السورة كلها ضغطةً واحدة من noor-audio ثم
            // فكٌّ إلى آياتها بأسمائها المعتمدة. محاولتان (سرعة alpha61):
            // انقطاعُ الشبكة في المحاولة الأولى لا يهبط بالطريق البطيء
            // فوراً. وإن رجعت السورة ناقصةً أو غاب الأصل فالمخطّ القديم
            // يكمل بعده آيةً آيةً.
            var viaMirror = false
            for (mirrorAttempt in 1..2) {
                val base = done.get()
                val ok = runCatching {
                    withContext(Dispatchers.IO) {
                        downloadSurahZip(
                            context, reciter, s,
                            // سرعة alpha62: نسبة نزول الضغطة نفسها تُرى حيّة
                            onZipPct = { pct ->
                                publish(
                                    active = true,
                                    phase = "جارٍ تنزيل ضغط السورة — " +
                                        MushafSurahs.arabicDigits(pct) + "٪",
                                )
                            },
                        ) {
                            done.set(base + surahDownloadedCount(context, reciterId, s, count))
                            publish(active = true)
                        }
                    }
                }.getOrDefault(false)
                if (ok) {
                    done.set(base + surahDownloadedCount(context, reciterId, s, count))
                    publish(active = true)
                    if (done.get() - base >= count) {
                        viaMirror = true
                        break
                    }
                }
                if (mirrorAttempt == 1) delay(1_200L)
            }
            if (viaMirror) {
                publish(active = true)
                continue
            }

            // (٢) الطريق القديم — آية آية من everyayah ثم islamic.network
            // **بتوازٍ أربعة** (سرعة alpha61): كل آيةٍ عاملةٌ مستقلة،
            // والموجودة على القرص تُتخطى، والفاشلة تُعاد حتى ثلاثٍ ثم
            // تُحصى ولا تُميت البقية. إلغاء النافج يوقف كل العمال.
            val missing = (1..count).filter { ayah ->
                val f = ayahFile(context, reciterId, s, ayah)
                !(f.isFile && f.length() > 0L)
            }
            val failedHere = java.util.concurrent.atomic.AtomicInteger(0)
            kotlinx.coroutines.coroutineScope {
                val semaphore = kotlinx.coroutines.sync.Semaphore(PARALLEL_AYAH_WORKERS)
                for (ayah in missing) {
                    launch {
                        semaphore.withPermit {
                            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return@withPermit
                            val target = ayahFile(context, reciterId, s, ayah)
                            var ok = withContext(Dispatchers.IO) {
                                fetchAyahFile(context, reciter, s, ayah, target)
                            }
                            for (attempt in 1..2) {
                                if (ok) break
                                if (!kotlinx.coroutines.currentCoroutineContext().isActive) return@withPermit
                                delay(1_500L * attempt)
                                ok = withContext(Dispatchers.IO) {
                                    fetchAyahFile(context, reciter, s, ayah, target)
                                }
                            }
                            if (ok) {
                                done.incrementAndGet()
                                publish(active = true)
                            } else {
                                failedHere.incrementAndGet()
                                // سبب الفشل يُرى — والمساحة الممتلئة أول المشتبهات
                                val spaceTight = runCatching { dir.usableSpace }
                                    .getOrDefault(Long.MAX_VALUE) < 50_000_000L
                                publish(
                                    active = true,
                                    error = when {
                                        spaceTight -> "مساحة التخزين شارفت على الامتلاء — " +
                                            "امسح ملفات ثم أعد المحاولة لتكملة المتبقي"
                                        else -> "تعذّر " + MushafSurahs.arabicDigits(failedHere.get()) +
                                            " آية حتى الآن — التنزيل يُكمل وإعادة الضغطة تكملتها"
                                    },
                                )
                            }
                        }
                    }
                }
            }
            cancelled = cancelled || !kotlinx.coroutines.currentCoroutineContext().isActive
            if (cancelled) break@outer
            failed += failedHere.get()
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
     * تنزيل ضغط السورة من مرآة جت هب وفكّه إلى ملفات الآيات — true إن
     * استُخرج من الأصل آيةٌ واحدة على الأقل. المدخلات الغريبة (أسماء
     * غير معتمدة أو سورةٌ غير المطلوبة) تُرمى ولا تُكتب أبدًا، والآيات
     * الحاضرة على القرص تُحسب ولا تُعاد.
     */
    private fun downloadSurahZip(
        context: Context,
        reciter: QuranReciters.Reciter,
        sura: Int,
        onZipPct: (Int) -> Unit = {},
        onExtracted: () -> Unit = {},
    ): Boolean {
        if (sura !in 1..114) return false
        val zip = File.createTempFile("noor_surah", ".zip", context.cacheDir)
        try {
            // سرعة alpha62 — ناقل الضغعات: تنزيلٌ متوازٍ بالتقطيع
            // (أربعة اتصالات) باستئنافٍ حقيقي لكل قطعةٍ ومتابعة التحويلات
            // يدوياً — بدل اتصالٍ واحدٍ يموت في مهلته ويبدأ من الصفر.
            if (!SurahZipFetch.fetch(QuranReciters.mirrorSurahUrl(reciter, sura), zip, onZipPct)) {
                return false
            }
            var extracted = 0
            ZipInputStream(FileInputStream(zip).buffered()).use { zin ->
                while (true) {
                    val entry = zin.nextEntry ?: break
                    val name = entry.name
                    if (!entry.isDirectory && QuranAudioMath.validZipEntry(name, sura)) {
                        val ayah = QuranAudioMath.zipEntryAyah(name)
                        val target = ayahFile(context, reciter.id, sura, ayah)
                        if (target.isFile && target.length() > 0L) {
                            extracted++
                        } else {
                            target.parentFile?.mkdirs()
                            val tmp = File(target.parentFile, "${target.name}.part")
                            FileOutputStream(tmp).use { zin.copyTo(it) }
                            if (finalizeDownloadFile(tmp, target)) {
                                extracted++
                                onExtracted()
                            }
                        }
                    }
                    zin.closeEntry()
                }
            }
            return extracted > 0
        } finally {
            zip.delete()
        }
    }

    /**
     * جلب آية واحدة — محاولاتُ بترتيب «سيرفرنا أولاً ثم المفتوح ثم النظير»
     * (بلاغ المالك 2026-11-11: «تأكد من جميع الاصوات انها قبله للتنزيل
     * و خلي كل الاصوات في السيرفر الخاص بينا افضل»):
     * ١) everyayah — آيةٌ واحدةٌ خفيفةٌ (الطريق الأسرع للبثّ الحي).
     * ٢) **المرآة الخاصة** (noor-audio) — ضغطُ السورة كله بضغطةٍ واحدةٍ
     *    وفكُّ الآيات الغائبة منه فأكثرُ من آيةٍ تُصلَح بطلبٍ واحدٍ من
     *    سيرفرنا — وهي السقوطُ الذي كان يفشل معه القطامي ومن لا نظير
     *    له في islamic.network فلا شيء بعده.
     * ٣) islamic.network للشيخ صاحب إصدارٍ نظير.
     * والتنزيل إلى ملف مؤقت ثم تسميته نهائياً بعد اكتماله.
     */
    private fun fetchAyahFile(
        context: Context,
        reciter: QuranReciters.Reciter,
        sura: Int,
        ayah: Int,
        target: File,
    ): Boolean {
        // مكتملة على القرص؟ لا تعيد تنزيلها — درس alpha58: المحاولات
        // الأربع كانت تعيد تنزيل الآية المكتملة لأن النجاح كان يُحسب فشلاً
        if (target.isFile && target.length() > 0L) return true
        if (downloadTo(QuranReciters.ayahUrl(reciter, sura, ayah), target)) return true
        // المرآة الخاصة — ضغطُ السورة كله وفكّ ما غاب منه (بلاغ 2026-11-11)
        if (downloadSurahZip(context, reciter, sura)) return target.isFile && target.length() > 0L
        // الاحتياط الأخير — بترقيم الآية الكلي من قاعدة المصحف نفسها
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
        // الجذر المُصلَح (alpha58 — بلاغ المالك: «يقول جاري التحميل و
        // الشريط لا يتقدم حتى خطوة»): الحكم على الاكتمال كان بعد rename
        // بحساب length() للملف المنقول نفسه فطوله صفرٌ دائماً — كل آيةٍ
        // نزلت سليمةً حُسبت فاشلةً وأُعيد تنزيلها خمس مرات وظلّ العدّاد
        // والشريط على صفر. الآن الحكم من حجم المؤقت قبل النقل:
        finalizeDownloadFile(tmp, target)
    }.getOrDefault(false)

    /**
     * إنهاء تنزيلٍ ناجح: المؤقت المكتمل يُسمّى نهائياً والحكم من حجمة
     * **قبل** النقل — دالة نقية لاختبار الوحدة (عقد alpha58: التنزيل
     * الناجح يرجع true بلا استثناء، وقرار النجاح قبل النقل لا بعده).
     */
    internal fun finalizeDownloadFile(tmp: File, target: File): Boolean {
        if (tmp.length() <= 0L) {
            tmp.delete()
            return false
        }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.delete()
            return false
        }
        return target.isFile && target.length() > 0L
    }

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
