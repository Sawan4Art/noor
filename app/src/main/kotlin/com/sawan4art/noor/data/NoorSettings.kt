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

/** ذاكرة نظام التحديثات — متى فُحص آخر مرة وماذا أُشعر عنه واستُبعد */
data class UpdatePrefs(
    val lastCheckAt: Long = 0L,
    val notifiedVersion: String? = null,
    val dismissedVersion: String? = null,
)

/**
 * إعدادات التطبيق — DataStore (الخليفة الحديث لـ SharedPreferences).
 * القيم الافتراضية: القاهرة (نفس المرجع) — أول تحديد موقع يستبدلها.
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
        val UPDATE_LAST_CHECK = longPreferencesKey("update_last_check_at")
        val UPDATE_NOTIFIED = stringPreferencesKey("update_notified_tag")
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
        )
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
            dismissedVersion = p[Keys.UPDATE_DISMISSED],
        )
    }

    suspend fun setUpdateChecked(atMillis: Long) {
        context.noorDataStore.edit { it[Keys.UPDATE_LAST_CHECK] = atMillis }
    }

    suspend fun setNotifiedVersion(tag: String) {
        context.noorDataStore.edit { it[Keys.UPDATE_NOTIFIED] = tag }
    }

    suspend fun setDismissedVersion(versionName: String) {
        context.noorDataStore.edit { it[Keys.UPDATE_DISMISSED] = versionName }
    }
}
