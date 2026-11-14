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
 * • **السرعة أولاً — المصادر مُعاد ترتيبها بالدراسة الحية** (بلاغ
 *   المالك 2026-11-13: «ادرس كويس جدا طريق تنزيل و اصوات الشيوخ
 *   الموجوده داخل تطبيقات المصحف علشان التنزيل للاصوات عندنا مش شغال
 *   و مش سريع»): قياسٌ حيٌّ بروابط حقيقية لأربعة مصادر أثبت أن CDN
 *   مكتبة القرآن المفتوحة (islamic.network) أسرعُ أربع مراتٍ من
 *   everyayah وأن ضغط مرآة جت هب أبطأها كلها — فالآيات تبدأ موازيةً
 *   من CDN ثم everyayah بستة عمّالٍ، وضغط المرآة صار **حشواً
 *   للفجوات لا المصدرَ الأول**، واستسلامٌ مبكرٌ يهبط للضغط حين
 *   تثبت الشبكة عدمَ جدواها بلا هدرِ مهلات.
 * • **مرآة جت هب باقيةً ضغطةً واحدة للسورة** (alpha59 — بلاغ المالك:
 *   «عايز اخلي الاصوات دي تكون موجوده على سيرفر على جت هب علشان نضمن
 *   ان التحميل يتم صح»): السورة كلها ضغطةٌ واحدةٌ من مستودع noor-audio
 *   (إصدار audio-v1) وتُفكّ إلى آياتها — صار موضعها بعد الطريق السريع
 *   فتُكمِل ما فاته، وهي خطُّ الدفاع لمن فشلت شبكاتُ التوزيع كلها.
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

    /**
     * عمال تنزيل الآيات المتوازي — ستة تعمل معاً: الدراسة الحية
     * 2026-11-13 قاست المصادر الأولى كلها شبكات توزيعٍ (Bunny لـ
     * everyayah وCloudflare للـ CDN) فتحمّل ستة اتصالاتٍ صغيرةٍ
     * بلا إثقال — والارتفاع عن أربعة (سرعة alpha61) زائدُ سرعة
     * المصادر الجديدة يُقصّر زمن السورة الكبيرة إلى الثلث.
     */
    internal const val PARALLEL_AYAH_WORKERS = 6

    /**
     * حد الاستسلام المبكر للطريق الآية-آية — ثمانية فشلٍ **بلا نجاحٍ
     * واحد** تعني أن المصادر غير مجدية فالهبوط إلى ضغط المرآة أسرع
     * من هدر مهلات الاتصال على كل آيةٍ من السورة.
     */
    internal const val EARLY_ABORT_FAILURES = 8

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
         * طورٌ جارٍ يُعرض بجوار العدّاد (alpha62) — مثل «جارٍ تكملة
         * السورة من مرآة التطبيق — ٤٥٪» كي لا يبدو الشريط ساكناً
         * أثناء نزول الأصل الكبير.
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
     * **مسار الدراسة الحية 2026-11-13** (بلاغ المالك: «ادرس كويس جدا
     * طريق تنزيل و اصوات الشيوخ … التنزيل للاصوات عندنا مش شغال و مش
     * سريع») — أربعة أطوار لكل سورة:
     * (٠) **المرآة أولاً لمن لا طريقَ CDN له** (بلاغ المالك: «البنا و
     *     الطبلاوي و القطامي و الدوسري كل منهم اصواتهم لا تقبل التحميل
     *     و تفشل و باقي الاصوات تعمل بشكل جيد») — هؤلاء طريقُهم
     *     الآيّة-آية الوحيد everyayah فتنزيل سورتهم يبدأ من أصول
     *     المرآة ضغطةً واحدةً ثم تكملةُ النواقص من everyayah بعدها.
     * (١) **الطريق السريع**: الآيات الغائبة موازيةً بستة عمّالٍ من
     *     شبكات التوزيع بترتيبها (CDN ١٢٨ لمن له → everyayah → CDN ٦٤)
     *     **بلا أي ضغطٍ هنا** — واستسلامٌ مبكرٌ حين ثمانية فشلٍ بلا
     *     نجاحٍ واحدٍ فالمصادر غير مجدية.
     * (٢) **حشو المرآة**: ما بقي من الطريق السريع يُكمَل ضغطةً واحدةً
     *     للسورة كلها من مرآة جت هب (محاولتان) — صارت حشواً لا أول.
     * (٣) **آخر مرورٍ رخيص**: ما بقي بعد الضغط آيةً آيةً بلا ضغطٍ
     *     ولا استسلامٍ مبكر.
     * والفشل قاطعاً لا يميت التنزيل: يُحصى المتبقي ويُبلغ «اكتمل مع
     * تعذّر N» وإعادة الضغطة تستأنف من الموجود فتحاول الفاشل وحده.
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
            val base = done.get()
            fun recount() {
                done.set(base + surahDownloadedCount(context, reciterId, s, count))
            }

            /**
             * حشو المرآة — ضغطةٌ واحدةٌ للسورة كلها من مرآة جت هب وفكُّ
             * ما غاب منها بمحاولاتٍ [attempts] وبنسبة نزولٍ حيّةٍ كما في
             * alpha62 — تُستدعى مرتين: **أولاً لمن لا طريقَ CDN له**
             * (بلاغ المالك 2026-11-13) ثم حشواً لما بقي عند الجميع.
             */
            suspend fun mirrorFill(attempts: Int) {
                for (mirrorAttempt in 1..attempts) {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            downloadSurahZip(
                                context, reciter, s,
                                // نسبة نزول الضغطة نفسها تُرى حيّة
                                onZipPct = { pct ->
                                    publish(
                                        active = true,
                                        phase = "جارٍ تكملة السورة من مرآة التطبيق — " +
                                            MushafSurahs.arabicDigits(pct) + "٪",
                                    )
                                },
                            ) {
                                recount()
                                publish(active = true)
                            }
                        }
                    }
                    recount()
                    publish(active = true)
                    if (surahDownloadedCount(context, reciterId, s, count) >= count) break
                    if (mirrorAttempt < attempts) delay(1_200L)
                }
            }

            // (٠) **المرآة أولاً لمن لا طريقَ CDN له** (بلاغ المالك
            // 2026-11-13: «البنا و الطبلاوي و القطامي و الدوسري كل منهم
            // اصواتهم لا تقبل التحميل و تفشل و باقي الاصوات تعمل بشكل
            // جيد») — الجذر قيساً حياً: هؤلاء الأربعة وحدهم طريقُهم
            // الآيّة-آية الوحيد everyayah (بلا CDN ١٢٨ ولا ٦٤)، فإذا
            // تعطل everyayah على شبكة المالك مات تنزيلهم كله وآخرون
            // نجاوا بالـ CDN — فأصول المرآة (١١٤ أصلاً لكل واحدٍ منهم
            // تحقّقت آلياً) صارت **أول طريقٍ** لتنزيل سورتهم ضغطةً
            // واحدةً، وتكملةُ النواقص آيةً آيةً من everyayah تأتي
            // بعدها لا قبلها
            if (QuranReciters.hasNoCdnPath(reciter)) {
                mirrorFill(attempts = 2)
            }

            // (١) الطريق السريع — الآيات موازيةً من شبكات التوزيع بلا ضغط
            val missing1 = missingAyahs(context, reciterId, s, count)
            if (missing1.isNotEmpty()) {
                runParallelAyahs(
                    context, reciter, dir, reciterId, s, missing1,
                    abortLimit = EARLY_ABORT_FAILURES,
                    onProgress = { publish(active = true) },
                    onError = { failedCount ->
                        // سبب الفشل يُرى — والمساحة الممتلئة أول المشتبهات
                        val spaceTight = runCatching { dir.usableSpace }
                            .getOrDefault(Long.MAX_VALUE) < 50_000_000L
                        publish(
                            active = true,
                            error = when {
                                spaceTight -> "مساحة التخزين شارفت على الامتلاء — " +
                                    "امسح ملفات ثم أعد المحاولة لتكملة المتبقي"
                                else -> "تعذّر " + MushafSurahs.arabicDigits(failedCount) +
                                    " آية حتى الآن — التنزيل يُكمل وإعادة الضغطة تكملتها"
                            },
                        )
                    },
                )
                recount()
                publish(active = true)
            }
            cancelled = cancelled || !kotlinx.coroutines.currentCoroutineContext().isActive
            if (cancelled) break@outer

            // (٢) حشو المرآة — لما بقي بعد الطريق السريع (محاولتان
            // بنسبةٍ حيّة كما في alpha62)
            if (surahDownloadedCount(context, reciterId, s, count) < count) {
                mirrorFill(attempts = 2)
            }

            // (٣) آخر مرورٍ رخيص — ما بقي بعد الضغط آيةً آيةً بلا ضغطٍ
            // ولا استسلامٍ مبكرٍ فالمتبقي قليلٌ غالباً
            val missing3 = missingAyahs(context, reciterId, s, count)
            if (missing3.isNotEmpty()) {
                runParallelAyahs(
                    context, reciter, dir, reciterId, s, missing3,
                    abortLimit = 0,
                    onProgress = { publish(active = true) },
                    onError = { /* الحساب النهائي يغطي البقية */ },
                )
                recount()
                publish(active = true)
            }
            cancelled = cancelled || !kotlinx.coroutines.currentCoroutineContext().isActive
            if (cancelled) break@outer
            failed += count - surahDownloadedCount(context, reciterId, s, count)
        }
        val message = when {
            cancelled -> ""
            failed > 0 -> "اكتمل مع تعذّر ${MushafSurahs.arabicDigits(failed)} آية — أعد المحاولة لتكملتها، والمحمّل باقٍ بلا مس"
            else -> ""
        }
        publish(active = false, error = message)
        return !cancelled
    }

    /** قرار الاستسلام المبكر — دالةٌ نقية تُختبر: نجاحٌ صفرٌ وفشلٌ بلغ الحد
     *  يعني شبكةً غير مجدية، وأي نجاحٍ واحدٍ يلغي الاستسلام فالمصدر حيّ */
    internal fun shouldAbortPerAyah(successes: Int, failures: Int, limit: Int): Boolean =
        limit > 0 && successes == 0 && failures >= limit

    /** الآيات الغائبة من القرص لهذه السورة — الموجودة كاملةً لا تُمسّ */
    private fun missingAyahs(context: Context, reciterId: String, sura: Int, count: Int): List<Int> =
        (1..count).filter { ayah ->
            val f = ayahFile(context, reciterId, sura, ayah)
            !(f.isFile && f.length() > 0L)
        }

    /**
     * مرورٌ موازٍ على آياتٍ معطاة — كل آيةٍ عاملةٌ مستقلة بعمّال
     * [PARALLEL_AYAH_WORKERS]، والموجودة على القرص تُتخطى (المرور
     * يُستدعى بالغائبة فقط)، والفاشلة تُعاد حتى ثلاثٍ ثم تُحصى ولا
     * تُميت البقية. إلغاء النافج يوقف كل العمال، و[abortLimit] > 0
     * يوقف المرور حين نجاحٌ صفرٌ وفشلٌ بلغ الحد (استسلامٌ مبكرٌ
     * يهبط بالمُنادي إلى ضغط المرآة بدل هدر المهلات).
     * يرجع عدد الناجح في هذا المرور.
     */
    private suspend fun runParallelAyahs(
        context: Context,
        reciter: QuranReciters.Reciter,
        dir: File,
        reciterId: String,
        sura: Int,
        ayahs: List<Int>,
        abortLimit: Int,
        onProgress: () -> Unit,
        onError: (Int) -> Unit,
    ): Int {
        if (ayahs.isEmpty()) return 0
        val successes = java.util.concurrent.atomic.AtomicInteger(0)
        val failures = java.util.concurrent.atomic.AtomicInteger(0)
        kotlinx.coroutines.coroutineScope {
            val semaphore = kotlinx.coroutines.sync.Semaphore(PARALLEL_AYAH_WORKERS)
            for (ayah in ayahs) {
                launch {
                    semaphore.withPermit {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) return@withPermit
                        if (shouldAbortPerAyah(successes.get(), failures.get(), abortLimit)) {
                            return@withPermit
                        }
                        val target = ayahFile(context, reciterId, sura, ayah)
                        var ok = withContext(Dispatchers.IO) {
                            fetchAyahFile(context, reciter, sura, ayah, target, allowZip = false)
                        }
                        for (attempt in 1..2) {
                            if (ok) break
                            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return@withPermit
                            delay(1_500L * attempt)
                            ok = withContext(Dispatchers.IO) {
                                fetchAyahFile(context, reciter, sura, ayah, target, allowZip = false)
                            }
                        }
                        if (ok) {
                            successes.incrementAndGet()
                            onProgress()
                        } else {
                            failures.incrementAndGet()
                            onError(failures.get())
                        }
                    }
                }
            }
        }
        return successes.get()
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
     * جلب آية واحدة — بترتيب المصادر بعد الدراسة الحية 2026-11-13
     * (بلاغ المالك: «ادرس كويس جدا طريق تنزيل و اصوات الشيوخ … الاصوات
     * اللي بتنزل بتبقى بطيئه»):
     * ١) **CDN المكتبة المفتوحة ١٢٨kbps** — أسرع مصدرٍ في القياس الحي
     *    وبنفس جودة مجلد الشيخ، لمن ثبت أن إصداره يخدم الجودة العالية
     *    (المنشاوي والحصري وجبريل والعفاسي والمعيقلي).
     * ٢) **everyayah** — الشامل لكل الشيوخ على شبكة Bunny للتوزيع.
     * ٣) **CDN المكتبة ٦٤kbps** — أخف الطرق للشيخ صاحب الإصدار النظير.
     * ٤) **ضغط المرآة** — أثقل الطرق: السورة كلها بضغطةٍ واحدة،
     *    وممنوع في المرور الموازي ([allowZip] = false) كي لا يُنزّل
     *    الضغط لكل آيةٍ غائبةٍ منفردةً — فالضغط طورٌ مستقلٌ في المسار.
     * والتنزيل إلى ملف مؤقت ثم تسميته نهائياً بعد اكتماله.
     */
    private fun fetchAyahFile(
        context: Context,
        reciter: QuranReciters.Reciter,
        sura: Int,
        ayah: Int,
        target: File,
        allowZip: Boolean = true,
    ): Boolean {
        // مكتملة على القرص؟ لا تعيد تنزيلها — درس alpha58: المحاولات
        // الأربع كانت تعيد تنزيل الآية المكتملة لأن النجاح كان يُحسب فشلاً
        if (target.isFile && target.length() > 0L) return true
        // الترقيم الكلي من ذاكرة MushafDb الدائمة — حسابٌ نقيٌّ رخيص
        val global = MushafDb.globalAyah(context, sura, ayah)
        // (١) CDN ١٢٨ — أول طريقٍ لمن إصداره يخدم الجودة العالية
        QuranReciters.cdnAyahUrl(reciter, global)?.let { url ->
            if (downloadTo(url, target)) return true
        }
        // (٢) everyayah — الشامل لكل الشيوخ
        if (downloadTo(QuranReciters.ayahUrl(reciter, sura, ayah), target)) return true
        // (٣) CDN ٦٤ — للشيخ صاحب الإصدار النظير
        QuranReciters.fallbackUrl(reciter, global)?.let { url ->
            if (downloadTo(url, target)) return true
        }
        // (٤) ضغط المرآة — الطريق الأخير الثقيل
        if (allowZip && downloadSurahZip(context, reciter, sura)) {
            return target.isFile && target.length() > 0L
        }
        return false
    }

    /**
     * جلب آيةٍ واحدةٍ عند الطلب — للذاكرة الذكية [AyahAudioCache]:
     * الحاضرة على القرص تعود ناجحةً بلا أي شبكة، والغائبة تمشي طرقها
     * الأربعة (CDN ١٢٨ ← everyayah ← CDN ٦٤ ← ضغط المرآة). تعليقةٌ
     * تعمل على Dispatchers.IO كاملةً فالواجهة لا تلتقط أنفاساً.
     */
    suspend fun fetchSingleAyah(context: Context, reciterId: String, sura: Int, ayah: Int): Boolean =
        withContext(Dispatchers.IO) {
            val target = ayahFile(context, reciterId, sura, ayah)
            if (target.isFile && target.length() > 0L) return@withContext true
            fetchAyahFile(context, QuranReciters.byId(reciterId), sura, ayah, target)
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
