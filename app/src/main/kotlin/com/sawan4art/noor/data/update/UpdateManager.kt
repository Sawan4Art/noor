package com.sawan4art.noor.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sawan4art.noor.BuildConfig
import com.sawan4art.noor.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * مدير التحديثات — منسق السلسلة: فحص عند **كل فتح للتطبيق** + فحص دوري
 * في الخلفية (12 ساعة) + **إشعار متكرر كل ١٢ ساعة ما دام التحديث لم يُثبَّت**
 * (alpha37) + ملخص «ما الجديد».
 *
 * **alpha46 — التسليم للمتصفح**: بلاغ المالك «فحص جوجل بيكتشف احتمال ان
 * يكون التطبيق مضر و ده بيخوف الناس» — التطبيق الذي ينزّل APK ويثبّته
 * بنفسه يُصنّفه فحص جوجل ضمن الأنماط الخطرة. صار زر التحديث يفتح ملف
 * الـ APK في المتصفح مباشرة، فيمشي التنزيل والتثبيت في الطريق الذي يعرفه
 * كل مستخدم، والتطبيق نفسه بلا أي صلاحية تثبيت — سلوكُ تطبيقٍ عادي مأمون.
 *
 * alpha35 إصلاح بلاغ المالك «لا مظهرش» — كان الفحص عند الفتح بسقفٍ
 * ٣ ساعات فكانت النسخة الجديدة تمرّ صامتاً ساعاتٍ إذا كان آخر فحص
 * حديثاً: كل فتحات المستخدم داخل السقف لا تفحص ولا تُشعر. الآن كل
 * فتحٍ بارد يفحص فعلاً (نشرُ الإشعار ثوانٍ من صدور النسخة)، وتكفي
 * أرضية دقيقةٍ داخل العملية لضبط كثرة الطلبات عند دوران الشاشة
 * وتغيير المظهر — والفحص اليدوي من الإعدادات بلا أرضية كما هو.
 */
object UpdateManager {

    /** Extra نية فتح التحديث — يأتي من ضغط إشعار التحديث */
    const val EXTRA_OPEN_UPDATE = "noor_extra_open_update"

    /** أرضية الفحص داخل العملية — طلب HTTP كل دقيقة على الأكثر في
     *  الجلسة الواحدة، ضد تكرار الدوران وتغيير المظهر لا ضد الفتح */
    private const val PROCESS_CHECK_FLOOR_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var appContext: Context
    private lateinit var repository: SettingsRepository

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state

    /** آخر نسخة استبعد المالك حوارها — لا يُعاد إزعاجه بها حتى تصدر بعدها نسخة */
    private val _dismissedVersion = MutableStateFlow<String?>(null)
    val dismissedVersion: StateFlow<String?> = _dismissedVersion

    val currentVersion: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    private var initialized = false
    private var checkRunning = false

    /** زمن انتهاء آخر فحص مكتمل (ساعة تشغيل العملية الرتيبة) */
    @Volatile
    private var lastFinishedCheckElapsed = Long.MIN_VALUE / 2

    /** يُنادى من NoorApplication — قناة الإشعار وجدولة الفاحص الدوري عند من يجدها */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        repository = SettingsRepository(appContext)
        scope.launch {
            _dismissedVersion.value = repository.updatePrefs.first().dismissedVersion
        }
    }

    /** فحص عند فتح التطبيق — كل فتحٍ بارد يفحص فعلاً كي يصل إشعار
     *  النسخة الجديدة فوراً، وأرضية الدقيقة تحمي من تكرار الطلبات
     *  عند دوران الشاشة وتغيير المظهر داخل الجلسة نفسها */
    fun onAppStart() {
        if (!initialized) return
        val sinceLast = android.os.SystemClock.elapsedRealtime() - lastFinishedCheckElapsed
        if (sinceLast < PROCESS_CHECK_FLOOR_MS) return
        if (_state.value is UpdateUiState.Checking) return
        scope.launch { check(auto = true) }
    }

    /** فحص يدوي فوري (زر الإعدادات أو ضغط الإشعار) — بلا سقف زمني */
    fun checkNow() {
        if (!initialized || checkRunning) return
        scope.launch { check(auto = false) }
    }

    /** فحص الخلفية الدوري — نفس مسار الفحص التلقائي مع الإشعار */
    suspend fun backgroundCheck() = check(auto = true)

    private suspend fun check(auto: Boolean) {
        if (checkRunning) return
        checkRunning = true
        _state.value = UpdateUiState.Checking
        try {
            val info = UpdateChecker.check(currentVersion)
            repository.setUpdateChecked(System.currentTimeMillis())
            _state.value = if (info != null) {
                maybeNotify(info)
                UpdateUiState.Available(info)
            } else {
                UpdateUiState.UpToDate(currentVersion)
            }
        } catch (t: Throwable) {
            // الفحص التلقائي الصامت لا يزعج بحالة فشل — يبقى الفشل للفحص اليدوي
            _state.value = if (auto) UpdateUiState.Idle
            else UpdateUiState.Failed(failureMessage(t), null)
        } finally {
            checkRunning = false
            lastFinishedCheckElapsed = android.os.SystemClock.elapsedRealtime()
        }
    }

    /**
     * الإشعار يتكرر ما دام التحديث لم يُثبَّت — تذكير كل ١٢ ساعة للنسخة
     * نفسها (alpha37 بلاغ المالك: «عايز الاشعار ده يوصل لو كتير لو
     * المستخدم محدثش») وبمجرد نسخة أحدث من آخر إشعار يُشعَر فوراً —
     * بلون الثيم المختار.
     */
    private suspend fun maybeNotify(info: UpdateInfo) {
        val prefs = repository.updatePrefs.first()
        val shouldNotify = UpdateNotifyPolicy.shouldNotify(
            notifiedVersion = prefs.notifiedVersion,
            lastNotifiedAt = prefs.lastNotifiedAt,
            candidateTag = info.tag,
            now = System.currentTimeMillis(),
        )
        if (!shouldNotify) return
        // لون الثيم المختار (alpha19) — فشل قراءته لا يمنع الإشعار
        val accentArgb = runCatching {
            UpdateNotifier.themedAccentArgb(
                appContext,
                repository.settings.first().accentId,
            )
        }.getOrNull()
        // **النشر الفعلي فقط يُحفظ كنسخة أُشعر عنها** — تعطيل إذن
        // الإشعارات (٣٣+) أو فشل النشر لا يضيّع إشعار النسخة: يُترك
        // الوسم غير محفوظ فيعيد الفحص القادم (عند الفتح أو الدوري
        // كل ١٢ ساعة) محاولة الإشعار حتى يصل للمستخدم فعلاً
        if (UpdateNotifier.notifyNewVersion(appContext, info, accentArgb)) {
            repository.setNotifiedVersion(info.tag)
        }
    }

    /**
     * فتح ملف الـ APK في المتصفح — التنزيل والتثبيت في طريق المتصفح
     * المعتاد (alpha46 بلاغ المالك: التحديث من داخل التطبيق كان يجعل فحص
     * جوجل يصنّف التطبيق ضمن الأنماط الخطرة). يعود برابطٍ إن فتح، وnull
     * إن لم يتوفر رابط أصلاً كي تعرض الواجهة رسالةً بدل الزر الصامت.
     */
    fun openDownloadPage(info: UpdateInfo): String? {
        if (!initialized) return null
        val url = info.apkUrl.ifBlank { info.releaseUrl }
        if (url.isBlank()) return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            appContext.startActivity(intent)
            url
        } catch (_: ActivityNotFoundException) {
            url
        }
    }

    /**
     * استبعاد حوار نسخة حتى تصدر بعدها نسخة — الحالة نفسها تبقى فبطاقة
     * الإعدادات تعرض التحديث وزر التثبيت لمن استبعد الحوار وتراجع.
     */
    fun dismiss(versionName: String) {
        _dismissedVersion.value = versionName
        scope.launch { repository.setDismissedVersion(versionName) }
    }

    /**
     * فتح مسار التحديث من ضغط إشعار النسخة — يلغي أي استبعاد سابق كي
     * يظهر «ما الجديد» دائماً بضغطة الإشعار (alpha37: كان ضغط الإشعار
     * بعد استبعاد الحوار يفحص ولا يظهر شيء فيبدو الزر ميتاً).
     */
    fun openFromNotification() {
        _dismissedVersion.value = null
        scope.launch { repository.clearDismissedVersion() }
        checkNow()
    }

    /** رسالة فشل عربية مفهومة بدل تتبع الاستثناءات الخام — الفحص وحده يفشل الآن */
    private fun failureMessage(t: Throwable): String = when {
        t is UpdateException -> t.message ?: "حدث خطأ غير متوقع"
        t.message?.contains("timeout", ignoreCase = true) == true ||
            t.message?.contains("connect", ignoreCase = true) == true ->
            "تعذر الوصول للشبكة — تأكد من اتصالك وحاول من جديد"
        else -> "حدث خطأ غير متوقع — حاول من جديد"
    }
}
