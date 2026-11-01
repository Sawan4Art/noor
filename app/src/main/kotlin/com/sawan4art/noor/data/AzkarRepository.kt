package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** مخزن مستقل للأذكار — كثرة الكتابة لا تلمس مخزن الإعدادات ولا مخزن المسبحة */
private val Context.azkarDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "azkar_prefs",
)

/**
 * لقطة الأذكار المحفوظة:
 * - [praiseCount] عدّ الصلاة على النبي الحرّ — لا يومَ له ولا هدف (ذكر لوحده).
 * - [day] مفتاح يوم علامات الإتمام + [done] معرّفات الأذكار المُتمَّة —
 *   تجدد كل يوم (AzkarMath.effectiveDone) فلا إجماليات تراكمية.
 */
data class AzkarSnapshot(
    val praiseCount: Int = 0,
    val day: Long = 0L,
    val done: Set<String> = emptySet(),
)

class AzkarRepository(private val context: Context) {

    private object Keys {
        val PRAISE_COUNT = intPreferencesKey("praise_count")
        val DAY = longPreferencesKey("done_day")
        val DONE = stringSetPreferencesKey("done_ids")
    }

    val snapshot: Flow<AzkarSnapshot> = context.azkarDataStore.data.map { p ->
        AzkarSnapshot(
            praiseCount = (p[Keys.PRAISE_COUNT] ?: 0).coerceAtLeast(0),
            day = p[Keys.DAY] ?: 0L,
            done = p[Keys.DONE] ?: emptySet(),
        )
    }

    /** كتابة اللقطة كاملة — يُستدعى مؤجَّلاً من المسؤول لا مع كل ضغطة */
    suspend fun save(s: AzkarSnapshot) {
        context.azkarDataStore.edit { p ->
            p[Keys.PRAISE_COUNT] = s.praiseCount.coerceAtLeast(0)
            p[Keys.DAY] = s.day
            p[Keys.DONE] = s.done
        }
    }
}
