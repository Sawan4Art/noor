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
import kotlinx.coroutines.withContext
import java.io.File

/**
 * مدير التحديثات — منسق السلسلة الكاملة داخل التطبيق (طلب المالك):
 * فحص تلقائي عند الفتح (بسقف 3 ساعات) + فحص دوري في الخلفية (12 ساعة)
 * + إشعار واحد لكل نسخة + ملخص «ما الجديد» + تنزيل بشريط تقدم
 * + تثبيت بحوار النظام فوق التطبيق بلا متصفح.
 */
object UpdateManager {

    /** Extra نية فتح التحديث — يأتي من ضغط إشعار التحديث */
    const val EXTRA_OPEN_UPDATE = "noor_extra_open_update"

    /** سقف الفحص التلقائي — يحمي حد الـ API العامة (60 طلب/ساعة لكل IP) */
    private const val AUTO_CHECK_INTERVAL_MS = 3L * 60 * 60 * 1000

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
    private var downloadRunning = false

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

    /** فحص عند فتح التطبيق — مقيّد بالسقف الزمني كي لا نسأل عند كل فتح */
    fun onAppStart() {
        if (!initialized) return
        scope.launch {
            val lastCheck = repository.updatePrefs.first().lastCheckAt
            val elapsed = System.currentTimeMillis() - lastCheck
            val busy = _state.value is UpdateUiState.Checking ||
                _state.value is UpdateUiState.Downloading
            if (elapsed >= AUTO_CHECK_INTERVAL_MS && !busy) {
                check(auto = true)
            }
        }
    }

    /** فحص يدوي فوري (زر الإعدادات أو ضغط الإشعار) — بلا سقف زمني */
    fun checkNow() {
        if (!initialized || checkRunning || downloadRunning) return
        scope.launch { check(auto = false) }
    }

    /** فحص الخلفية الدوري — نفس مسار الفحص التلقائي مع الإشعار */
    suspend fun backgroundCheck() = check(auto = true)

    private suspend fun check(auto: Boolean) {
        if (checkRunning || downloadRunning) return
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
        }
    }

    /** الإشعار مرة واحدة لكل نسخة — حتى لو فُحص مرات عديدة */
    private suspend fun maybeNotify(info: UpdateInfo) {
        val notified = repository.updatePrefs.first().notifiedVersion
        if (!NoorVersion.isNewer(info.tag, notified)) return
        UpdateNotifier.notifyNewVersion(appContext, info)
        repository.setNotifiedVersion(info.tag)
    }

    /** بدء/استئناف تنزيل الـ APK — من حالة متاح أو فشل (إعادة محاولة) */
    fun startDownload() {
        val info = when (val current = _state.value) {
            is UpdateUiState.Available -> current.info
            is UpdateUiState.Failed -> current.info ?: return
            else -> return
        }
        if (downloadRunning) return
        downloadRunning = true
        _state.value = UpdateUiState.Downloading(info, 0, info.apkSizeBytes)
        scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) {
                    UpdateDownloader.download(appContext, info) { received, total ->
                        _state.value = UpdateUiState.Downloading(info, received, total)
                    }
                }
                _state.value = UpdateUiState.ReadyToInstall(info, apk.absolutePath)
            } catch (t: Throwable) {
                _state.value = UpdateUiState.Failed(failureMessage(t), info)
            } finally {
                downloadRunning = false
            }
        }
    }

    /**
     * التثبيت — يُفتح حوار نظام التثبيت فوق التطبيق مباشرة.
     * أول مرة بلا إذن المصدر: نفتح صفحة الإعداد لتطبيقنا ثم يعود المستخدم ويضغط من جديد.
     */
    fun installApk(apkPath: String): Boolean {
        if (!initialized) return false
        val file = File(apkPath)
        return if (UpdateInstaller.canRequestInstall(appContext)) {
            UpdateInstaller.installApk(appContext, file)
        } else {
            UpdateInstaller.openInstallPermissionSettings(appContext)
            false
        }
    }

    val canRequestInstall: Boolean
        get() = initialized && UpdateInstaller.canRequestInstall(appContext)

    /**
     * استبعاد حوار نسخة حتى تصدر بعدها نسخة — الحالة نفسها تبقى فبطاقة
     * الإعدادات تعرض التحديث وزر التثبيت لمن استبعد الحوار وتراجع.
     */
    fun dismiss(versionName: String) {
        _dismissedVersion.value = versionName
        scope.launch { repository.setDismissedVersion(versionName) }
    }

    /** رسالة فشل عربية مفهومة بدل تتبع الاستثناءات الخام */
    private fun failureMessage(t: Throwable): String = when {
        t is UpdateException -> t.message ?: "حدث خطأ غير متوقع"
        t.message?.contains("timeout", ignoreCase = true) == true ||
            t.message?.contains("connect", ignoreCase = true) == true ->
            "تعذر الوصول للشبكة — تأكد من اتصالك وحاول من جديد"
        else -> "حدث خطأ غير متوقع — حاول من جديد"
    }
}
