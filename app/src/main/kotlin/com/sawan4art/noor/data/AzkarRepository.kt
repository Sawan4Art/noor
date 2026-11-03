package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sawan4art.noor.domain.azkar.AzkarLibrary
import com.sawan4art.noor.domain.azkar.CustomDhikr
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
 * - [rounds] العدّاد الرقمي الجاري لكل بطاقة (alpha28): «معرّف الذكر» إلى
 *   ما عدّاه في جولته الحالية — تُصفَّر عند الإتمام وتتجدد كل يوم معها.
 */
data class AzkarSnapshot(
    val praiseCount: Int = 0,
    val day: Long = 0L,
    val done: Set<String> = emptySet(),
    val rounds: Map<String, Int> = emptyMap(),
    val customs: List<CustomDhikr> = emptyList(),
    val deleted: Set<String> = emptySet(),
)

class AzkarRepository(private val context: Context) {

    private object Keys {
        val PRAISE_COUNT = intPreferencesKey("praise_count")
        val DAY = longPreferencesKey("done_day")
        val DONE = stringSetPreferencesKey("done_ids")
        // العدّاد الرقمي — مدخلات «المعرّف:العدّ» في مجموعة نصوص (DataStore لا يحفظ خرائط)
        val ROUNDS = stringSetPreferencesKey("round_counts")
        // أذكار المستخدم المضافة ومعرفات الافتراضي المحذوف (alpha44)
        val CUSTOMS = stringSetPreferencesKey("custom_dhikr")
        val DELETED = stringSetPreferencesKey("deleted_ids")
    }

    val snapshot: Flow<AzkarSnapshot> = context.azkarDataStore.data.map { p ->
        AzkarSnapshot(
            praiseCount = (p[Keys.PRAISE_COUNT] ?: 0).coerceAtLeast(0),
            day = p[Keys.DAY] ?: 0L,
            done = p[Keys.DONE] ?: emptySet(),
            rounds = parseRounds(p[Keys.ROUNDS] ?: emptySet()),
            customs = parseCustoms(p[Keys.CUSTOMS] ?: emptySet()),
            deleted = p[Keys.DELETED] ?: emptySet(),
        )
    }

    /** كتابة اللقطة كاملة — يُستدعى مؤجَّلاً من المسؤول لا مع كل ضغطة */
    suspend fun save(s: AzkarSnapshot) {
        context.azkarDataStore.edit { p ->
            p[Keys.PRAISE_COUNT] = s.praiseCount.coerceAtLeast(0)
            p[Keys.DAY] = s.day
            p[Keys.DONE] = s.done
            p[Keys.ROUNDS] = encodeRounds(s.rounds)
            p[Keys.CUSTOMS] = s.customs.mapNotNull(AzkarLibrary::encodeCustom).toSet()
            p[Keys.DELETED] = s.deleted
        }
    }

    private fun encodeRounds(rounds: Map<String, Int>): Set<String> =
        rounds.filterValues { it > 0 }.entries.mapTo(HashSet()) { "${it.key}:${it.value}" }

    /** تحليل متسامح — المدخل التالف يُهمل ولا يكسر القراءة أبداً */
    private fun parseRounds(stored: Set<String>): Map<String, Int> =
        stored.mapNotNull { entry ->
            val sep = entry.lastIndexOf(':')
            if (sep <= 0) return@mapNotNull null
            val count = entry.substring(sep + 1).toIntOrNull() ?: return@mapNotNull null
            if (count <= 0) null else entry.substring(0, sep) to count
        }.toMap()

    /** تحليل متسامح — مدخل مضافٍ تالف يُهمل ولا يكسر القراءة أبداً */
    private fun parseCustoms(stored: Set<String>): List<CustomDhikr> =
        stored.mapNotNull(AzkarLibrary::decodeCustom).sortedBy { it.item.id }
}
