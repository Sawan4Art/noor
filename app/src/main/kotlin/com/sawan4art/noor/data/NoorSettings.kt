package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sawan4art.noor.domain.prayer.CalculationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noor_settings",
)

/** وضع المظهر */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * تفضيلات الأذان والإقامة — القسم الجديد في الإعدادات:
 * - الأذان مفعّل افتراضياً للصلوات الخمس (الشروق بلا أذان)
 * - الإقامة مطفأة افتراضياً والفاصل بينها وبين الأذان ١٠ دقائق
 * - الجمعة: أذان مرتين — فاصل ١٠ دقائق بين الأذانين ثم الإقامة بعد ٣٠ دقيقة
 * - كتم أصوات الهاتف بعد الأذان اختياري (مطفأ افتراضياً، ١٠ دقائق)
 * - صوت مخصص من الهاتف للأذان وللإقامة (مسار واسم الملف)
 * - أو نغمة إشعارات الهاتف بدلاً من الأذان/الإقامة
 */
data class AdhanPrefs(
    val enabled: Boolean = true,
    /** صوت الأذان: معرّف مدمج | custom (من الهاتف) | notification (نغمة الإشعارات) */
    val adhanVoice: String = "main",
    val iqamahEnabled: Boolean = false,
    val iqamahOffsetMinutes: Int = 10,
    val iqamahVoice: String = "makkah",
    val fridayDouble: Boolean = true,
    val fridayAdhanGapMinutes: Int = 10,
    val fridayIqamahGapMinutes: Int = 30,
    val muteEnabled: Boolean = false,
    val muteMinutes: Int = 10,
    val customAdhanPath: String = "",
    val customAdhanName: String = "",
    val customIqamahPath: String = "",
    val customIqamahName: String = "",
)

/** ذاكرة نظام التحديثات — متى فُحص آخر مرة وماذا أُشعر عنه ومتى واستُبعد */
data class UpdatePrefs(
    val lastCheckAt: Long = 0L,
    val notifiedVersion: String? = null,
    /** آخر وقت نُشر فيه إشعار فعلي — عصا سياسة التكرار (alpha37) */
    val lastNotifiedAt: Long = 0L,
    val dismissedVersion: String? = null,
)

/**
 * إعدادات التطبيق — DataStore (الخليفة الحديث لـ SharedPreferences).
 * القيم الافتراضية: القاهرة (نفس المرجع) — أول تحديد موقع يستبدلها.
 * accentId معرّف لون التطبيق (ui.theme.AccentPalette) — زمردي افتراضياً.
 * primaryId معرّف **اللون الأساسي** (alpha32) — فارغ = يتبع لون التطبيق
 * الحالي (وهذا هو الافتراضي بأمر المالك: «طبعا الافتراضي هو الحالي»)،
 * وقيمته من AccentPalette نفسها فالقائمتان نفسا بقائمة العشرين.
 */
data class NoorSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val method: CalculationMethod = CalculationMethod.EGYPT,
    val isHanafi: Boolean = false,
    val city: String = "القاهرة، مصر",
    val latitude: Double = 30.0444,
    val longitude: Double = 31.2357,
    /** هل حدد المستخدم موقعه بنفسه ولو مرة (لعرض زر التحديد كإجراء أول) */
    val locationResolved: Boolean = false,
    /** لون التطبيق (مُعرّف من AccentPalette) — تغييره من الإعدادات (alpha18) */
    val accentId: String = "emerald",
    /** اللون الأساسي (alpha32) — فارغ = يتبع لون التطبيق الحالي */
    val primaryId: String = "",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val METHOD = stringPreferencesKey("calc_method")
        val HANAFI = booleanPreferencesKey("asr_hanafi")
        val CITY = stringPreferencesKey("city")
        val LAT = doublePreferencesKey("latitude")
        val LNG = doublePreferencesKey("longitude")
        val LOCATION_RESOLVED = booleanPreferencesKey("location_resolved")
        val ACCENT = stringPreferencesKey("accent_color")
        val PRIMARY = stringPreferencesKey("primary_color")
        val UPDATE_LAST_CHECK = longPreferencesKey("update_last_check_at")
        val UPDATE_NOTIFIED = stringPreferencesKey("update_notified_tag")
        val UPDATE_NOTIFIED_AT = longPreferencesKey("update_notified_at")
        val UPDATE_DISMISSED = stringPreferencesKey("update_dismissed_version")
        val HADITH_FAVORITES = stringPreferencesKey("hadith_favorites")
        // الأذان والإقامة
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val ADHAN_VOICE = stringPreferencesKey("adhan_voice")
        val IQAMAH_ENABLED = booleanPreferencesKey("iqamah_enabled")
        val IQAMAH_OFFSET = intPreferencesKey("iqamah_offset_minutes")
        val IQAMAH_VOICE = stringPreferencesKey("iqamah_voice")
        val FRIDAY_DOUBLE = booleanPreferencesKey("friday_double_adhan")
        val FRIDAY_ADHAN_GAP = intPreferencesKey("friday_adhan_gap_minutes")
        val FRIDAY_IQAMAH_GAP = intPreferencesKey("friday_iqamah_gap_minutes")
        val MUTE_ENABLED = booleanPreferencesKey("adhan_mute_enabled")
        val MUTE_MINUTES = intPreferencesKey("adhan_mute_minutes")
        val CUSTOM_ADHAN_PATH = stringPreferencesKey("custom_adhan_path")
        val CUSTOM_ADHAN_NAME = stringPreferencesKey("custom_adhan_name")
        val CUSTOM_IQAMAH_PATH = stringPreferencesKey("custom_iqamah_path")
        val CUSTOM_IQAMAH_NAME = stringPreferencesKey("custom_iqamah_name")
    }

    val settings: Flow<NoorSettingsState> = context.noorDataStore.data.map { p ->
        NoorSettingsState(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            method = CalculationMethod.fromId(p[Keys.METHOD]),
            isHanafi = p[Keys.HANAFI] ?: false,
            city = p[Keys.CITY] ?: "القاهرة، مصر",
            latitude = p[Keys.LAT] ?: 30.0444,
            longitude = p[Keys.LNG] ?: 31.2357,
            locationResolved = p[Keys.LOCATION_RESOLVED] ?: false,
            accentId = p[Keys.ACCENT] ?: "emerald",
            primaryId = p[Keys.PRIMARY] ?: "",
        )
    }

    /** تفضيلات الأذان والإقامة — أي تغيير يعيد جدولة منبهات الأذان */
    val adhanPrefs: Flow<AdhanPrefs> = context.noorDataStore.data.map { p ->
        AdhanPrefs(
            enabled = p[Keys.ADHAN_ENABLED] ?: true,
            adhanVoice = p[Keys.ADHAN_VOICE] ?: "main",
            iqamahEnabled = p[Keys.IQAMAH_ENABLED] ?: false,
            iqamahOffsetMinutes = p[Keys.IQAMAH_OFFSET] ?: 10,
            iqamahVoice = p[Keys.IQAMAH_VOICE] ?: "makkah",
            fridayDouble = p[Keys.FRIDAY_DOUBLE] ?: true,
            fridayAdhanGapMinutes = p[Keys.FRIDAY_ADHAN_GAP] ?: 10,
            fridayIqamahGapMinutes = p[Keys.FRIDAY_IQAMAH_GAP] ?: 30,
            muteEnabled = p[Keys.MUTE_ENABLED] ?: false,
            muteMinutes = p[Keys.MUTE_MINUTES] ?: 10,
            customAdhanPath = p[Keys.CUSTOM_ADHAN_PATH] ?: "",
            customAdhanName = p[Keys.CUSTOM_ADHAN_NAME] ?: "",
            customIqamahPath = p[Keys.CUSTOM_IQAMAH_PATH] ?: "",
            customIqamahName = p[Keys.CUSTOM_IQAMAH_NAME] ?: "",
        )
    }

    /** قراءة فورية واحدة للتفضيلات — للمجدول والمستقبلات خارج دورة الواجهة */
    suspend fun adhanPrefsOnce(): AdhanPrefs = adhanPrefs.first()

    /** قراءة فورية واحدة للإعدادات العامة — للمجدول */
    suspend fun settingsOnce(): NoorSettingsState = settings.first()

    suspend fun setAdhanEnabled(on: Boolean) {
        context.noorDataStore.edit { it[Keys.ADHAN_ENABLED] = on }
    }

    suspend fun setAdhanVoice(id: String) {
        context.noorDataStore.edit { it[Keys.ADHAN_VOICE] = id }
    }

    suspend fun setIqamahEnabled(on: Boolean) {
        context.noorDataStore.edit { it[Keys.IQAMAH_ENABLED] = on }
    }

    suspend fun setIqamahOffsetMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.IQAMAH_OFFSET] = minutes }
    }

    suspend fun setIqamahVoice(id: String) {
        context.noorDataStore.edit { it[Keys.IQAMAH_VOICE] = id }
    }

    suspend fun setFridayDouble(on: Boolean) {
        context.noorDataStore.edit { it[Keys.FRIDAY_DOUBLE] = on }
    }

    suspend fun setFridayAdhanGapMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.FRIDAY_ADHAN_GAP] = minutes }
    }

    suspend fun setFridayIqamahGapMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.FRIDAY_IQAMAH_GAP] = minutes }
    }

    suspend fun setMuteEnabled(on: Boolean) {
        context.noorDataStore.edit { it[Keys.MUTE_ENABLED] = on }
    }

    suspend fun setMuteMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.MUTE_MINUTES] = minutes }
    }

    /** حفظ صوت مخصص من الهاتف (الأذان أو الإقامة) — يُنسخ داخل مسار التطبيق */
    suspend fun setCustomSound(
        kind: com.sawan4art.noor.domain.adhan.AdhanSoundKind,
        path: String,
        name: String,
    ) {
        context.noorDataStore.edit {
            if (kind == com.sawan4art.noor.domain.adhan.AdhanSoundKind.ADHAN) {
                it[Keys.CUSTOM_ADHAN_PATH] = path
                it[Keys.CUSTOM_ADHAN_NAME] = name
            } else {
                it[Keys.CUSTOM_IQAMAH_PATH] = path
                it[Keys.CUSTOM_IQAMAH_NAME] = name
            }
        }
    }

    /** حذف الصوت المخصص — يعود الاختيار للأصوات المدمجة */
    suspend fun clearCustomSound(kind: com.sawan4art.noor.domain.adhan.AdhanSoundKind) {
        context.noorDataStore.edit {
            if (kind == com.sawan4art.noor.domain.adhan.AdhanSoundKind.ADHAN) {
                it.remove(Keys.CUSTOM_ADHAN_PATH)
                it.remove(Keys.CUSTOM_ADHAN_NAME)
            } else {
                it.remove(Keys.CUSTOM_IQAMAH_PATH)
                it.remove(Keys.CUSTOM_IQAMAH_NAME)
            }
        }
    }

    suspend fun setAccentId(id: String) {
        context.noorDataStore.edit { it[Keys.ACCENT] = id }
    }

    /** اللون الأساسي — نص فارغ يعني «الحالي» (يتبع لون التطبيق) */
    suspend fun setPrimaryId(id: String) {
        context.noorDataStore.edit { it[Keys.PRIMARY] = id }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.noorDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setMethod(method: CalculationMethod) {
        context.noorDataStore.edit { it[Keys.METHOD] = method.id }
    }

    suspend fun setHanafi(hanafi: Boolean) {
        context.noorDataStore.edit { it[Keys.HANAFI] = hanafi }
    }

    suspend fun setLocation(city: String, latitude: Double, longitude: Double) {
        context.noorDataStore.edit {
            it[Keys.CITY] = city
            it[Keys.LAT] = latitude
            it[Keys.LNG] = longitude
            it[Keys.LOCATION_RESOLVED] = true
        }
    }

    /** ذاكرة التحديثات — نفس مخزن الإعدادات لأنها كلها تفضيلات المستخدم */
    val updatePrefs: Flow<UpdatePrefs> = context.noorDataStore.data.map { p ->
        UpdatePrefs(
            lastCheckAt = p[Keys.UPDATE_LAST_CHECK] ?: 0L,
            notifiedVersion = p[Keys.UPDATE_NOTIFIED],
            lastNotifiedAt = p[Keys.UPDATE_NOTIFIED_AT] ?: 0L,
            dismissedVersion = p[Keys.UPDATE_DISMISSED],
        )
    }

    suspend fun setUpdateChecked(atMillis: Long) {
        context.noorDataStore.edit { it[Keys.UPDATE_LAST_CHECK] = atMillis }
    }

    /**
     * تسجيل النسخة التي نُشر عنها إشعار فعلي + وقت النشر — الوقت هو
     * ما تمشي عليه سياسة التكرار (alpha37 بلاغ المالك: «عايز الاشعار
     * ده يوصل لو كتير لو المستخدم محدثش التطبيق»).
     */
    suspend fun setNotifiedVersion(tag: String) {
        context.noorDataStore.edit {
            it[Keys.UPDATE_NOTIFIED] = tag
            it[Keys.UPDATE_NOTIFIED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDismissedVersion(versionName: String) {
        context.noorDataStore.edit { it[Keys.UPDATE_DISMISSED] = versionName }
    }

    /** إلغاء الاستبعاد — ضغط إشعار التحديث يعيد فتح «ما الجديد» دائماً (alpha37) */
    suspend fun clearDismissedVersion() {
        context.noorDataStore.edit { it.remove(Keys.UPDATE_DISMISSED) }
    }

    /**
     * معرفات الأحاديث المحفوظة (alpha38 توجيه المالك: «زر نجمه للحفظ
     * في قائمه المفضله») — سلسلة مفصولة بفواصل بترتيب الحفظ الأحدث
     * أولاً فتظهر قائمة المفضلة بأحدث ما حفظه المستخدم أعلاها.
     */
    val favoriteHadithIds: Flow<List<Long>> = context.noorDataStore.data.map { p ->
        p[Keys.HADITH_FAVORITES]?.split(',')?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    /** حفظ حديث في المفضلة — يُقدَّم إلى رأس القائمة وإن سبق حفظه */
    suspend fun addHadithFavorite(id: Long) {
        context.noorDataStore.edit { p ->
            val current = p[Keys.HADITH_FAVORITES]
                ?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
            p[Keys.HADITH_FAVORITES] = (listOf(id) + (current - id)).joinToString(",")
        }
    }

    /** إخراج حديث من المفضلة — وتُحذف المفتاح كلياً عند آخر حديث */
    suspend fun removeHadithFavorite(id: Long) {
        context.noorDataStore.edit { p ->
            val current = p[Keys.HADITH_FAVORITES]
                ?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
            val rest = current - id
            if (rest.isEmpty()) p.remove(Keys.HADITH_FAVORITES)
            else p[Keys.HADITH_FAVORITES] = rest.joinToString(",")
        }
    }
}
