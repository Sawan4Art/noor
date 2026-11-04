package com.sawan4art.noor.ui.home

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sawan4art.noor.alarm.AlarmScheduler
import com.sawan4art.noor.data.AdhanPrefs
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.domain.adhan.AdhanSoundKind
import com.sawan4art.noor.domain.location.CityGeocoder
import com.sawan4art.noor.domain.location.LocationGate
import com.sawan4art.noor.domain.location.LocationGateChecker
import com.sawan4art.noor.domain.location.LocationSource
import com.sawan4art.noor.domain.prayer.CalculationMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * مسؤول الشاشة الرئيسية — لقط كل ثانية والشاشة ظاهرة فقط (توفير بطارية)،
 * والإعدادات تُقرأ رياضياً من DataStore فأي تغيير ينعكس فوراً.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application), com.sawan4art.noor.ui.settings.adhan.AdhanActions {

    private val repository = SettingsRepository(application)
    private val locationSource = LocationSource(application)
    private val geocoder = CityGeocoder()

    val settings: StateFlow<NoorSettingsState> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, NoorSettingsState())

    /** تفضيلات الأذان والإقامة — مصدر الإعدادات الجديدة */
    val adhanPrefs: StateFlow<AdhanPrefs> = repository.adhanPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, AdhanPrefs())

    /** نبضة الثانية — تعمل والشاشة مشتركة فقط وتتوقف عند الخروج */
    private val ticker: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    data class HomeUiState(
        val settings: NoorSettingsState = NoorSettingsState(),
        val nowMillis: Long = System.currentTimeMillis(),
    )

    val uiState: StateFlow<HomeUiState> = combine(settings, ticker) { s, t ->
        HomeUiState(s, t)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(NoorSettingsState(), System.currentTimeMillis()),
    )

    /** حالة تحديد الموقع الجاري (لعرض دوّار على الزر) */
    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating

    /**
     * رسالة بوابات الموقع (توجيه المالك alpha18) — كل بوابة مقفولة تُنهي
     * الطلب فوراً برسالة عربية وإجراء فتح الإعدادات المناسب، بلا انتظار
     * عقد المهلة القديم الذي كان يُقرأ لاجاً
     */
    private val _locationGate = MutableStateFlow<LocationGate?>(null)
    val locationGate: StateFlow<LocationGate?> = _locationGate

    fun dismissLocationGate() {
        _locationGate.value = null
    }

    fun detectLocation() {
        if (_locating.value) return
        // البوابات أولاً — فحص رخيص فوري يمنع اللاج القديم (انتظار مهلة
        // كاملة ثم محاولتا شبكة ثم سكوت بلا رسالة)
        val gate = LocationGateChecker.check(getApplication())
        if (gate.blocked) {
            _locationGate.value = gate
            return
        }
        _locating.value = true
        viewModelScope.launch {
            try {
                val coords = locationSource.currentLocation()
                if (coords != null) {
                    val geo = geocoder.reverseGeocode(coords.first, coords.second)
                    // توجيه المالك alpha20: «اسم القرية أو العزبة أو الكفر أولاً
                    // ثم اسم المدينة التابعة لها» — السطر = قرية، مدينة، محافظة
                    val city = CityGeocoder.buildLocationLabel(
                        geo.village, geo.city, geo.region, geo.country,
                    )
                    repository.setLocation(city, coords.first, coords.second)
                } else {
                    // البوابات مفتوحة والقراءة فشلت (مهلة الموقع انقضت) — رسالة عامة
                    _locationGate.value = LocationGate.READING_FAILED
                }
            } finally {
                _locating.value = false
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    /** لون التطبيق الجديد من إعدادات الألوان (alpha18) */
    fun setAccent(id: String) = viewModelScope.launch { repository.setAccentId(id) }

    /**
     * **اللون الأساسي المستقل** (alpha32) — «اعمل نفس الالوان في قائمه
     * جديده لتغيير اللون الاساسي في التطبيق و طبعا الافتراضي هو الحالي» —
     * نص فارغ يعني «الحالي»: يتبع لون التطبيق ولا يجبر على قيمة.
     */
    fun setPrimary(id: String) = viewModelScope.launch { repository.setPrimaryId(id) }

    fun setMethod(method: CalculationMethod) =
        viewModelScope.launch { repository.setMethod(method) }

    fun setHanafi(hanafi: Boolean) = viewModelScope.launch { repository.setHanafi(hanafi) }

    /* ==================== الأذان والإقامة ==================== */

    override fun setAdhanEnabled(on: Boolean) {
        viewModelScope.launch { repository.setAdhanEnabled(on) }
    }

    override fun setAdhanVoice(id: String) {
        viewModelScope.launch { repository.setAdhanVoice(id) }
    }

    override fun setIqamahEnabled(on: Boolean) {
        viewModelScope.launch { repository.setIqamahEnabled(on) }
    }

    override fun setIqamahOffsetMinutes(minutes: Int) {
        viewModelScope.launch { repository.setIqamahOffsetMinutes(minutes) }
    }

    override fun setIqamahVoice(id: String) {
        viewModelScope.launch { repository.setIqamahVoice(id) }
    }

    override fun setFridayDouble(on: Boolean) {
        viewModelScope.launch { repository.setFridayDouble(on) }
    }

    override fun setFridayAdhanGapMinutes(minutes: Int) {
        viewModelScope.launch { repository.setFridayAdhanGapMinutes(minutes) }
    }

    override fun setFridayIqamahGapMinutes(minutes: Int) {
        viewModelScope.launch { repository.setFridayIqamahGapMinutes(minutes) }
    }

    override fun setMuteEnabled(on: Boolean) {
        viewModelScope.launch { repository.setMuteEnabled(on) }
    }

    override fun setMuteMinutes(minutes: Int) {
        viewModelScope.launch { repository.setMuteMinutes(minutes) }
    }

    /** استيراد صوت مخصص من الهاتف — يُنسخ داخل مسار التطبيق ليبقى دائماً */
    override fun importCustomSound(kind: AdhanSoundKind, uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            runCatching {
                val resolver = app.contentResolver
                val name = queryDisplayName(resolver, uri) ?: "صوت مخصص"
                val ext = guessExtension(resolver, uri, name)
                val dir = File(app.filesDir, "sounds").apply { if (!exists()) mkdirs() }
                val outFile = File(
                    dir,
                    if (kind == AdhanSoundKind.ADHAN) "adhan_custom$ext" else "iqamah_custom$ext",
                )
                resolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                repository.setCustomSound(kind, outFile.absolutePath, name)
                // الاختيار ينتقل تلقائياً للصوت المستورد
                if (kind == AdhanSoundKind.ADHAN) repository.setAdhanVoice("custom")
                else repository.setIqamahVoice("custom")
            }
        }
    }

    /** حذف الصوت المخصص والعودة للمدمج */
    override fun clearCustomSound(kind: AdhanSoundKind) {
        viewModelScope.launch {
        val app = getApplication<Application>()
        runCatching {
            val path = when (kind) {
                AdhanSoundKind.ADHAN -> adhanPrefs.value.customAdhanPath
                AdhanSoundKind.IQAMAH -> adhanPrefs.value.customIqamahPath
            }
            if (path.isNotEmpty()) File(path).delete()
            repository.clearCustomSound(kind)
            // لو كان المخصص مختاراً — نرجع للافتراضي
            if (kind == AdhanSoundKind.ADHAN && adhanPrefs.value.adhanVoice == "custom") {
                repository.setAdhanVoice("main")
            }
            if (kind == AdhanSoundKind.IQAMAH && adhanPrefs.value.iqamahVoice == "custom") {
                repository.setIqamahVoice("makkah")
            }
        }
        }
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: Uri): String? {
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    c.getString(idx)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun guessExtension(resolver: android.content.ContentResolver, uri: Uri, name: String): String {
        val fromName = name.substringAfterLast('.', "").lowercase()
        if (fromName.isNotEmpty() && fromName.length <= 4 && fromName.all { ch -> ch.isLetterOrDigit() }) {
            return "." + fromName
        }
        val mime = runCatching { resolver.getType(uri) }.getOrNull() ?: return ".audio"
        return when {
            mime.contains("mpeg") || mime.contains("mp3") -> ".mp3"
            mime.contains("ogg") || mime.contains("opus") -> ".ogg"
            mime.contains("wav") -> ".wav"
            mime.contains("flac") -> ".flac"
            mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> ".m4a"
            else -> ".audio"
        }
    }

    init {
        // أول تشغيل: بناء منبهات الأذان للأيام القادمة فوراً
        viewModelScope.launch {
            runCatching { AlarmScheduler.reschedule(getApplication()) }
        }
        // أي تغيير في الإعدادات المؤثرة (الموقع/الطريقة/تفضيلات الأذان)
        // يعيد الجدولة تلقائياً — الموقع من الكشف اليدوي يمر من هنا أيضاً
        viewModelScope.launch {
            combine(settings, adhanPrefs) { s, a -> s to a }
                .drop(1)
                .distinctUntilChanged()
                .collect {
                    runCatching { AlarmScheduler.reschedule(getApplication()) }
                }
        }
    }
}
