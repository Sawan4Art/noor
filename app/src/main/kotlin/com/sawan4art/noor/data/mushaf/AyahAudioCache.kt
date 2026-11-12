package com.sawan4art.noor.data.mushaf

import android.content.Context
import com.sawan4art.noor.domain.mushaf.QuranReciters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * الذاكرة الذكية لأصوات الآيات — عقد المالك ٢٠٢٦-١١-١٢:
 * «فور ضغط المستخدم على أي آية للاستماع إليها يتم تحميل ملف الـMP3
 * الخاص بها وحفظه محلياً … وفي حال طلب نفس الآية مرة أخرى تُشغَّل
 * فوراً من ذاكرة الهاتف بسرعة صفر ثوانٍ ودون أي استهلاك للإنترنت».
 *
 * • **ضغطةٌ واحدةٌ لكل آية** — طلبُها هو تنزيلُها مرةً واحدةً حتى لو
 *   تزاحم عليها طلباتٌ متزامنة (single-flight): أول طلبٍ يفتح العملية
 *   والبقية تنتظر نافجها نفسه، والحاضرة على القرص تعود **لحظياً**
 *   بلا أي شبكة.
 * • **المخزن هو مكتبة التلاوة نفسها** — ملفات «٣ سورة + ٣ آية.mp3»
 *   في مجال التطبيق الخاص، فما نزّله المستخدم سورةً سورةً من «أصوات
 *   الشيخ» وما تُنزّله الذاكرة الذكية آيةً آيةً مخزنٌ واحدٌ بلا
 *   ازدواجٍ ولا تنزيلٍ ثانٍ لما حاضر.
 * • **سياسة الإخلاء** (Cache Eviction Policy) — سقفٌ صارم
 *   [MAX_BYTES] (٢٫٥ غيغابايت): بعد كل تنزيلٍ جديدٍ يُمسح **الأقدم
 *   استعمالاً** (LRU بلحظة آخر لمسةٍ للتشغيل أو التنزيل) حتى يرجع
 *   الحجم دون السقف — فمساحة الهاتف محميةٌ بلا سقفٍ زائف، والآيات
 *   التي يسمعها القارئ دائماً تبقى والأيتام التي هجرها تمضي أولاً.
 *   والملف اليتيم الذي لا يزال يُسمَع الآن **محميٌّ** بلا مسٍّ (تبلّغ
 *   المشغّل عن حاضريه عبر [protectedPaths]).
 * • **الإحصاء الحي** — [stats] حجمٌ وعدّادٌ حقيقيان من القرص تُعرض
 *   للأدوات، وكل العمل على Dispatchers.IO/Default فالواجهة لا تلمس
 *   قرصاً قط.
 */
object AyahAudioCache {

    /**
     * سقف الذاكرة الذكية بالبايت — ٢٫٥ غيغابايت (توجيه المالك:
     * سياسة إخلاءٍ تحمي مساحة الهاتف من الامتلاء).
     */
    const val MAX_BYTES: Long = 2_500_000_000L

    /**
     * نافذة التحميل المسبق أثناء التلاوة — الآيتان بعد الجارية تجلبان
     * للذاكرة في الخلفية، فانتقال «آية-آية» يصير بلا أي انتظارٍ ولا
     * صمتٍ بين الآيات (المشغّل يربط تاليتها المحضَّرة لحظتها).
     */
    const val PRELOAD_AHEAD = 2

    /** إحصاء المخزن من القرص — حجمه وعدد ملفاته الحقيقيان */
    data class Stats(
        val totalBytes: Long = 0L,
        val fileCount: Int = 0,
    ) {
        val maxBytes: Long get() = MAX_BYTES

        /** نسبة الامتلاء من السقف — ٠..١ للشرائط الدائرية */
        val fraction: Float
            get() = if (MAX_BYTES <= 0L) 0f else (totalBytes.toFloat() / MAX_BYTES).coerceIn(0f, 1f)
    }

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats

    /** مجال الذاكرة الذكية — حياة التطبيق لا حياة الشاشة */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** عمليات الجلب الجارية — مفتاحها «شيخ:سورة:آية» والواحدة فقط لكل مفتاح */
    private val inflight = ConcurrentHashMap<String, Deferred<String?>>()
    private val inflightLock = Any()

    /**
     * مساراتٌ محميةٌ من الإخلاء — المشغّل يبلّغ عن آياته السائرة
     * (الجارية والمحضَّرة التالية) فلا يمسها الإخلاءُ ولو نضب السقف.
     */
    @Volatile
    var protectedPaths: Set<String> = emptySet()

    /**
     * استدعاء آيةٍ للتشغيل — **قلب النظام الذكي**:
     * • الحاضرة على القرص تُلمَس (تجديد رصيدها LRU) ويعود مسارها
     *   **فوراً بلا إنترنت** — سرعة الصفر للطلب المكرر.
     * • الغائبة تُنزَّل مرةً واحدةً بثلاثة طرقها (everyayah ← مرآتنا
     *   ← islamic.network) وتُحفظ بالاسم المعتمد ثم يُجدَّد الإخلاء.
     * • الزحامُ آمن: كل المتسابقين على الآية نفسها ينتظرن العملية
     *   الأولى نفسها فلا تنزيلٍ مزدوجاً قط.
     * يعود مسار الملف الجاهز للتشغيل، أو لا شيء إن تعذّر التنزيل.
     */
    suspend fun ensureAyah(
        context: Context,
        reciterId: String,
        sura: Int,
        ayah: Int,
    ): String? {
        val appContext = context.applicationContext
        val key = "$reciterId:$sura:$ayah"
        synchronized(inflightLock) { inflight[key] }?.let { return it.await() }

        // الحاضرة تعود لحظياً — سرعة الصفر بلا أي شبكة (عقد المالك)
        val local = QuranAudioLibrary.ayahFile(appContext, reciterId, sura, ayah)
        if (QuranAudioLibrary.hasAyah(appContext, reciterId, sura, ayah)) {
            touch(local)
            return local.absolutePath
        }

        val deferred: Deferred<String?> = synchronized(inflightLock) {
            // سباقٌ آخر ربما فتح العملية بين القراءتين — لا ينزل مرتين
            inflight[key]?.let { return@synchronized it }
            scope.async {
                try {
                    // إعادة فحصٍ داخل العملية — العائد الأول قد أكملها
                    if (!QuranAudioLibrary.hasAyah(appContext, reciterId, sura, ayah)) {
                        val ok = QuranAudioLibrary.fetchSingleAyah(appContext, reciterId, sura, ayah)
                        if (!ok) return@async null
                        scheduleMaintenance(appContext)
                    }
                    val file = QuranAudioLibrary.ayahFile(appContext, reciterId, sura, ayah)
                    if (!QuranAudioLibrary.hasAyah(appContext, reciterId, sura, ayah)) {
                        return@async null
                    }
                    touch(file)
                    file.absolutePath
                } finally {
                    synchronized(inflightLock) { inflight.remove(key) }
                }
            }.also { inflight[key] = it }
        }
        return deferred.await()
    }

    /**
     * تحميلٌ مسبقٌ نافع — تجلب الآيات المعطاة للذاكرة بالترتيب وبلا
     * مشغّلات: تُستدعى بالمشغّل لجلب نافذةٍ بعد الجارية فيصير انتقال
     * الآيات متتابعاً بلا صمتٍ ولا انتظارٍ ظاهر. كلها عملياتٌ خلفيةٌ
     * أهدأ — إلغاءُ المجال أو التطبيق لا يكسر شيئاً.
     */
    fun preload(context: Context, reciterId: String, sura: Int, ayahs: List<Int>) {
        if (ayahs.isEmpty()) return
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            // عمليتان متوازيتان كحدٍّ أقصى — التحميل المسبق أداةٌ خلفيةٌ
            // مهذبة لا منافسٌ للتنزيل الذي يستعده المستخدم بنفسه
            val gate = Semaphore(2)
            for (ayah in ayahs) {
                gate.withPermit {
                    runCatching { ensureAyah(appContext, reciterId, sura, ayah) }
                }
            }
        }
    }

    /** لمسة LRU — آخر استعمالٍ يُبطئ عمر الملف فالأكثر سماعاً يبقى */
    fun touch(file: File) {
        runCatching { file.setLastModified(System.currentTimeMillis()) }
    }

    /**
     * **سياسة الإخلاء النقية** — دالةٌ صرفةٌ تُختبر آلياً: من إدخالات
     * المخزن وسقفٍ ومحميّين تعود المسارات الممسوحة — الأقدم لمسةً
     * أولاً حتى ينزل الحجم دون السقف، والمحميّون لا يُمسّون مهما بلغ
     * العجز، ومسحُ ما تحت السقف لا يحدث أبداً.
     */
    internal data class Entry(val path: String, val size: Long, val modified: Long)

    internal fun evictionPlan(
        entries: List<Entry>,
        capBytes: Long,
        keep: Set<String>,
    ): List<String> {
        var total = entries.sumOf { it.size }
        if (total <= capBytes) return emptyList()
        val deletable = entries
            .filter { it.path !in keep && it.size > 0L }
            .sortedBy { it.modified }
        val plan = mutableListOf<String>()
        for (entry in deletable) {
            if (total <= capBytes) break
            plan += entry.path
            total -= entry.size
        }
        return plan
    }

    /**
     * جردُ المخزن وتطبيقُ السياسة — يمشي مجلدات الشيخ كلها ويُحصي
     * الحجم الحقيقي ثم ينفّذ خطةَ الإخلاء ويحدّث الإحصاء بما مُسح
     * فعلاً. يعود الإحصاء بعد المسح.
     */
    suspend fun trimAndReport(context: Context): Stats = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val entries = mutableListOf<Entry>()
        for (reciter in QuranReciters.ALL) {
            val dir = QuranAudioLibrary.reciterDir(appContext, reciter.id)
            dir.listFiles()?.forEach { f ->
                if (f.isFile && f.extension == "mp3" && f.length() > 0L) {
                    entries += Entry(f.absolutePath, f.length(), runCatching { f.lastModified() }.getOrDefault(0L))
                }
            }
        }
        val keep = protectedPaths
        val planned = evictionPlan(entries, MAX_BYTES, keep).toSet()
        var deletedCount = 0
        var deletedBytes = 0L
        for (entry in entries) {
            if (entry.path in planned && File(entry.path).delete()) {
                deletedCount++
                deletedBytes += entry.size
            }
        }
        Stats(
            totalBytes = (entries.sumOf { it.size } - deletedBytes).coerceAtLeast(0L),
            fileCount = (entries.size - deletedCount).coerceAtLeast(0),
        ).also { _stats.value = it }
    }

    /**
     * جدولة الصيانة (جردٌ وإخلاءٌ) بعد كل تنزيلٍ جديد — مرةً واحدةً
     * في آنٍ فلا تتزاحم جولاتُ الإخلاء مع التنزيلات المتوازية.
     */
    private val maintenanceBusy = AtomicBoolean(false)

    private fun scheduleMaintenance(context: Context) {
        if (!maintenanceBusy.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                kotlinx.coroutines.delay(500L) // دفعةٌ للتنزيلات المتلاحقة
                runCatching { trimAndReport(appContext) }
            } finally {
                maintenanceBusy.set(false)
            }
        }
    }

    /**
     * إخلاءٌ يدويٌّ فوري — من أدوات المالك (زر «تحرير الذاكرة») وفيه
     * يُطبَّق السقف مهما كان سبب تجاوزه.
     */
    fun evictNow(context: Context) {
        scope.launch { runCatching { trimAndReport(context) } }
    }

    /**
     * إقلاع الذاكرة الذكية — من [com.sawan4art.noor.NoorApplication]:
     * جردٌ أوليٌّ للإحصاء وتطبيقُ السياسة إن كانت المخزنة تجاوزت
     * السقف في جلسةٍ سابقة.
     */
    fun init(context: Context) {
        scope.launch(Dispatchers.IO) {
            runCatching { trimAndReport(context.applicationContext) }
        }
    }
}
