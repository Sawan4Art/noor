package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** مخزن مستقل للمسبحة — كثرة الكتابة لا تلمس مخزن الإعدادات العامة */
private val Context.misbahaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "misbaha_prefs",
)

/**
 * لقطة المسبحة المحفوظة — التسلسل الثابت لتسبيح ما بعد الصلاة (alpha26):
 * موضع القسم الحالي وعدّه وعدد المائات المكتملة. لا أذكار متعددة ولا
 * أهداف متغيرة ولا إجمالي تراكمي (توجيه المالك: «تشيل عداد الاجمالي
 * اللي فوق خالص» و«في المسبحه هنحط المهم فقط»).
 */
data class MisbahaSnapshot(
    /** موضع القسم الحالي في التسلسل ٠..٣ */
    val stepIndex: Int = 0,
    /** عدّ القسم الحالي داخل دوره */
    val count: Int = 0,
    /** عدد المائات المكتملة كاملة */
    val completedSequences: Int = 0,
)

class MisbahaRepository(private val context: Context) {

    private object Keys {
        const val LEGACY_SELECTED = "selected_dhikr"
        const val LEGACY_LIFETIME = "lifetime_total"
        val STEP = intPreferencesKey("sequence_step")
        val COUNT = intPreferencesKey("current_count")
        val SEQUENCES = intPreferencesKey("completed_sequences")

        const val LEGACY_COUNT_PREFIX = "count_"
        const val LEGACY_ROUNDS_PREFIX = "rounds_"
        const val LEGACY_TARGET_PREFIX = "target_"
    }

    val snapshot: Flow<MisbahaSnapshot> = context.misbahaDataStore.data.map { p ->
        MisbahaSnapshot(
            stepIndex = (p[Keys.STEP] ?: 0).coerceIn(0, MisbahaDhikrList.SEQUENCE.lastIndex),
            count = p[Keys.COUNT] ?: 0,
            completedSequences = p[Keys.SEQUENCES] ?: 0,
        )
    }

    /** كتابة اللقطة كاملة — يُستدعى مؤجَّلاً من المسؤول لا مع كل تسبيحة */
    suspend fun save(s: MisbahaSnapshot) {
        context.misbahaDataStore.edit { p ->
            p[Keys.STEP] = s.stepIndex
            p[Keys.COUNT] = s.count
            p[Keys.SEQUENCES] = s.completedSequences
            // محو مفاتيح التصميم القديم (أذكار متعددة وأهداف وإجمالي) —
            // ماتت مع ميزتها في alpha26 ولا تعود
            for (key in p.asMap().keys.toList()) {
                if (key.name == Keys.LEGACY_SELECTED ||
                    key.name == Keys.LEGACY_LIFETIME ||
                    key.name.startsWith(Keys.LEGACY_COUNT_PREFIX) ||
                    key.name.startsWith(Keys.LEGACY_ROUNDS_PREFIX) ||
                    key.name.startsWith(Keys.LEGACY_TARGET_PREFIX)
                ) {
                    p.remove(key)
                }
            }
        }
    }
}
