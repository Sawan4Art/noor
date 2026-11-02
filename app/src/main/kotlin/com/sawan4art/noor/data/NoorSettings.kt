package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sawan4art.noor.domain.prayer.CalculationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noor_settings",
)

/** وضع المظهر */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
}
