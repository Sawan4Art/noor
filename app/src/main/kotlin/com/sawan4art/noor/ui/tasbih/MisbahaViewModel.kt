package com.sawan4art.noor.ui.tasbih

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sawan4art.noor.data.MisbahaRepository
import com.sawan4art.noor.data.MisbahaSnapshot
import com.sawan4art.noor.domain.tasbih.MisbahaDhikr
import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** أحداث المسبحة — الاهتزاز ونبضة الحلقة تنصت لها الشاشة */
enum class MisbahaEvent { TAP, ROUND_COMPLETED }

/**
 * حالة المسبحة — كل ذكر يحفظ عدّه وذُهابه وهدفه الخاص، والإجمالي
 * التراكمي للأعمار محفوظ ومحميّ من مسح إعادة التعيين.
 */
data class MisbahaState(
    val selectedId: String = MisbahaDhikrList.ALL.first().id,
    val counts: Map<String, Int> = emptyMap(),
    val rounds: Map<String, Int> = emptyMap(),
    val targets: Map<String, Int> = emptyMap(),
    val lifetimeTotal: Long = 0L,
) {
    val dhikr: MisbahaDhikr
        get() = MisbahaDhikrList.byId(selectedId)
    val count: Int get() = counts[selectedId] ?: 0
    val roundsNow: Int get() = rounds[selectedId] ?: 0
    val target: Int get() = targets[selectedId] ?: dhikr.defaultTarget
}

/**
 * مسؤول المسبحة — العدّ يعيش في الذاكرة سرعةً لتتابع النقر السريع،
 * والحفظ إلى DataStore **مؤجَّل** (٧٠٠ مللي سكّة هدوء) فلا قرصٌ صلب
 * مع كل تسبيحة، وإتمامُ دورةٍ يحفظ فوراً كي لا يضيع حصاد الدورة.
 */
class MisbahaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MisbahaRepository(application)

    private val _state = MutableStateFlow(MisbahaState())
    val state: StateFlow<MisbahaState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MisbahaEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<MisbahaEvent> = _events.asSharedFlow()

    /** التحميل الأول من المخزن — بعده يصبح العدّ في الذاكرة هو المرجع */
    private var loaded = false
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val snap = repository.snapshot.first()
            _state.value = MisbahaState(
                selectedId = snap.selectedId,
                counts = snap.counts,
                rounds = snap.rounds,
                targets = snap.targets,
                lifetimeTotal = snap.lifetimeTotal,
            )
            loaded = true
        }
    }

    /** تسبيحة واحدة — العدّ ثم نبضة الحدث، وإتمام الدورة يحفظ فوراً */
    fun tap() {
        if (!loaded) return
        val s = _state.value
        val step = MisbahaMath.step(s.count, s.target)
        _state.update { cur ->
            cur.copy(
                counts = cur.counts + (cur.selectedId to step.count),
                rounds = if (step.roundCompleted) {
                    cur.rounds + (cur.selectedId to (cur.rounds[cur.selectedId] ?: 0) + 1)
                } else {
                    cur.rounds
                },
                lifetimeTotal = cur.lifetimeTotal + 1,
            )
        }
        _events.tryEmit(if (step.roundCompleted) MisbahaEvent.ROUND_COMPLETED else MisbahaEvent.TAP)
        if (step.roundCompleted) {
            persistNow()
        } else {
            scheduleSave()
        }
    }

    /** اختيار ذكر — يُحمّل عدّه وذهابه وهدفه المحفوظين */
    fun selectDhikr(id: String) {
        if (!loaded || id == _state.value.selectedId) return
        _state.update { it.copy(selectedId = id) }
        scheduleSave()
    }

    /** هدف جديد للذكر الحالي — ٠ = بلا حدّ */
    fun setTarget(target: Int) {
        if (!loaded) return
        _state.update { it.copy(targets = it.targets + (it.selectedId to target)) }
        scheduleSave()
    }

    /** إعادة العدّ الحالي فقط — بلا ذُهاب وبلا إجمالي */
    fun resetCurrent() {
        if (!loaded) return
        _state.update { it.copy(counts = it.counts + (it.selectedId to 0)) }
        persistNow()
    }

    /** إعادة الكل — العدادات والذُهاب تصفر والإجمالي التراكمي يبقى محفوظاً */
    fun resetAll() {
        if (!loaded) return
        _state.update { it.copy(counts = emptyMap(), rounds = emptyMap()) }
        persistNow()
    }

    /** حفظ مؤجَّل — ٧٠٠ مللي سكّة هدوء تتجمع معها نقرات السرعة في قرصٍ واحد */
    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(700)
            persist()
        }
    }

    private fun persistNow() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch { persist() }
    }

    private suspend fun persist() {
        val s = _state.value
        repository.save(
            MisbahaSnapshot(
                selectedId = s.selectedId,
                counts = s.counts,
                rounds = s.rounds,
                targets = s.targets,
                lifetimeTotal = s.lifetimeTotal,
            ),
        )
    }
}
