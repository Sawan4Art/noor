package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** مخزن مستقل للمسبحة — كثرة الكتابة لا تلمس مخزن الإعدادات العامة */
private val Context.misbahaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "misbaha_prefs",
)

/**
 * لقطة المسبحة المحفوظة — عدّادٌ وذهابٌ لكل ذكر، وهدفٌ اختياري مُعاد
 * تعريفه، وإجمالي تسبيح العمر لا يُمسح بإعادة التعيين.
 */
data class MisbahaSnapshot(
    val selectedId: String = MisbahaDhikrList.ALL.first().id,
    val counts: Map<String, Int> = emptyMap(),
    val rounds: Map<String, Int> = emptyMap(),
    /** تجاوز الهدف الافتراضي — الغائب يعود لافتراضي الذكر */
    val targets: Map<String, Int> = emptyMap(),
    val lifetimeTotal: Long = 0L,
)

class MisbahaRepository(private val context: Context) {

    private object Keys {
        const val SELECTED = "selected_dhikr"
        const val LIFETIME = "lifetime_total"
        const val P_COUNT = "count_"
        const val P_ROUNDS = "rounds_"
        const val P_TARGET = "target_"

        fun count(id: String) = intPreferencesKey(P_COUNT + id)
        fun rounds(id: String) = intPreferencesKey(P_ROUNDS + id)
        fun target(id: String) = intPreferencesKey(P_TARGET + id)
    }

    private val selectedKey = stringPreferencesKey(Keys.SELECTED)
    private val lifetimeKey = longPreferencesKey(Keys.LIFETIME)

    val snapshot: Flow<MisbahaSnapshot> = context.misbahaDataStore.data.map { p ->
        val ids = MisbahaDhikrList.ALL.map { it.id }
        MisbahaSnapshot(
            selectedId = p[selectedKey]?.takeIf { ids.contains(it) }
                ?: MisbahaDhikrList.ALL.first().id,
            counts = ids.associateWith { p[Keys.count(it)] ?: 0 }.filterValues { it != 0 },
            rounds = ids.associateWith { p[Keys.rounds(it)] ?: 0 }.filterValues { it != 0 },
            targets = ids.mapNotNull { id ->
                p[Keys.target(id)]?.takeIf { it != MisbahaDhikrList.byId(id).defaultTarget }
                    ?.let { id to it }
            }.toMap(),
            lifetimeTotal = p[lifetimeKey] ?: 0L,
        )
    }

    /** كتابة اللقطة كاملة — يُستدعى مؤجَّلاً من المسؤول لا مع كل تسبيحة */
    suspend fun save(s: MisbahaSnapshot) {
        context.misbahaDataStore.edit { p ->
            p[selectedKey] = s.selectedId
            p[lifetimeKey] = s.lifetimeTotal
            for (dhikr in MisbahaDhikrList.ALL) {
                val id = dhikr.id
                val count = s.counts[id] ?: 0
                val rounds = s.rounds[id] ?: 0
                val target = s.targets[id]
                if (count != 0) p[Keys.count(id)] = count else p.remove(Keys.count(id))
                if (rounds != 0) p[Keys.rounds(id)] = rounds else p.remove(Keys.rounds(id))
                if (target != null && target != dhikr.defaultTarget) {
                    p[Keys.target(id)] = target
                } else {
                    p.remove(Keys.target(id))
                }
            }
        }
    }
}
