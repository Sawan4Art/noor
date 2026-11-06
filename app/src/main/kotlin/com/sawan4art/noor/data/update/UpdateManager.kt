package com.sawan4art.noor.data.update

import android.content.Context
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
 * **alpha48 — عودة التحديث من جوّه التطبيق بلا متصفح**: بلاغ المالك
 * «التحديث بقا يفتح المتصفح انا مش عايز كده» — بعد أن كانت alpha46
 * قد سلّمت التنزيل للمتصفح خوفاً من فحص جوجل. الطريق الجديد أهدأ
 * ما يمكن: التنزيل بمدير تنزيل النظام نفسه (التطبيق لا يلمس بايتات
 * الملف) ثم فتح مثبّت أندرويد الرسمي عبر موفّر الملفات — سلوكٌ قياسي
 * لا كود تثبيت مصنوعاً داخلياً، ومع خطوة إذن «التثبيت من هذا المصدر»
 * مرة واحدة قبل أول تنزيل.
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

    /** حالة تنزيل النسخة الجديدة داخل التطبيق (alpha48) */
    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download: StateFlow<DownloadState> = _download

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
     * تنزيل النسخة من داخل التطبيق ثم فتح مثبّت النظام — بلا متصفح
     * وبلا مدير تنزيل النظام (alpha53 بتوجيه المالك: «عالج مشكله فحص
     * جوجل بان التطبيق قد يكون ضار بدون ما نعمل حركه تحميل التحديث
     * من المتصفح»): اتصال HTTPS مباشر داخل مسار التطبيق الخاص فلا
     * يظهر ملف النسخة في قوائم عامة يجتاحها فاحص جوجل، وعند الاكتمال
     * يُفتح حوار التثبيت المألوف فوراً. خطوة إذن «التثبيت من هذا
     * المصدر» تُطلب مرة واحدة قبل أول تنزيل.
     */
    fun downloadAndInstall(info: UpdateInfo) {
        if (!initialized) return
        when (val current = _download.value) {
            is DownloadState.Running -> return
            is DownloadState.Done -> {
                // الملف جاهز — ضغطة أخرى تعيد فتح مثبّت النظام بلا تنزيل ثاني
                UpdateInstaller.openApk(appContext)
                return
            }
            else -> Unit
        }
        if (!UpdateInstaller.canInstall(appContext)) {
            _download.value = DownloadState.NeedsPermission
            UpdateInstaller.openInstallPermissionSettings(appContext)
            return
        }
        _download.value = DownloadState.Running(0)
        scope.launch(Dispatchers.IO) {
            val result = UpdateInstaller.download(appContext, info) { percent ->
                // التقدم يصل من خيط التنزيل — نحدّث الحالة الآمنة قيمةً قيمة
                val current = _download.value
                if (current is DownloadState.Running && current.percent != percent) {
                    _download.value = DownloadState.Running(percent)
                }
            }
            // فتح المثبّت وتحديث الحالة النهائية على الخيط الرئيسي
            scope.launch {
                when (result) {
                    is DownloadState.Done -> {
                        _download.value = DownloadState.Done
                        UpdateInstaller.openApk(appContext)
                    }
                    is DownloadState.Failed -> _download.value = result
                    else -> Unit
                }
            }
        }
    }

    /** إعادة الزر للوضع العادي بعد حالة إذن أو فشل — لا يُبقى على الإنذار */
    fun resetDownloadState() {
        if (_download.value !is DownloadState.Running) {
            _download.value = DownloadState.Idle
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
