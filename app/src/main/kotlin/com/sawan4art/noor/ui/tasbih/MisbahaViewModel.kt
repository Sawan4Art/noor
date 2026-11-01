package com.sawan4art.noor.ui.tasbih

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sawan4art.noor.data.MisbahaRepository
import com.sawan4art.noor.data.MisbahaSnapshot
import com.sawan4art.noor.domain.tasbih.MisbahaDhikr
import com.sawan4art.noor.domain.tasbih.MisbahaDhikrList
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.domain.tasbih.MisbahaRushGuard
import com.sawan4art.noor.domain.tasbih.MisbahaSplashMath
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

/**
 * أحداث المسبحة — الشاشة (النقطة السائلة) تنصت لها للتناثر والاهتزاز.
 */
sealed interface MisbahaEvent {
    /** تسبيحة معدودة — [particles] قطرات التناثر المقترحة (دالة سرعة النقر) */
    data class Counted(val particles: Int) : MisbahaEvent

    /** إتمام دورة — تناثر أقصى + نبضة الحلقة */
    data object RoundCompleted : MisbahaEvent

    /** انكشاف التسرع — تنبيه وبدء إيقاف العدّ */
    data object RushWarned : MisbahaEvent
}

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
 *
 * **حارس التسرع (alpha25 — توجيه المالك)**: النقر بسرعة تفوق نطق الجمل
 * يُنبه ويوقف العدّ ٣ ثوانٍ — الضغطات أثناء الإيقاف مُهمَلة بلا عدّ ولا
 * تناثر، ويُعرض وقت الاستئناف للشاشة عبر pausedUntil.
 */
class MisbahaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MisbahaRepository(application)

    private val _state = MutableStateFlow(MisbahaState())
    val state: StateFlow<MisbahaState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MisbahaEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<MisbahaEvent> = _events.asSharedFlow()

    /** حارس التسرع — إيقاف العدّ عند النقر بآلة أسرع من النطق */
    private val rushGuard = MisbahaRushGuard()

    /** نهاية فترة الإيقاف الحالية (0 = لا إيقاف) — للتنبيه والعدّ التنازلي */
    private val _pausedUntil = MutableStateFlow(0L)
    val pausedUntil: StateFlow<Long> = _pausedUntil.asStateFlow()

    /** أوقات النقر الأخيرة لقياس سرعة النقر → كثافة التناثر */
    private val tapTimes = ArrayDeque<Long>()

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

    /**
     * تسبيحة واحدة — حارس التسرع أولاً: مُهمَلة أثناء الإيقاف، وانكشاف
     * التسرع ينبّه ويوقف، والمعدودة تُبثّ بكمية تناثرٍ من سرعة النقر.
     */
    fun tap() {
        if (!loaded) return
        val now = System.currentTimeMillis()
        when (rushGuard.onTap(now)) {
            MisbahaRushGuard.Verdict.IGNORED_PAUSED -> return
            MisbahaRushGuard.Verdict.RUSH_DETECTED -> {
                _pausedUntil.value = rushGuard.pausedUntil
                _events.tryEmit(MisbahaEvent.RushWarned)
                return
            }
            MisbahaRushGuard.Verdict.COUNTED -> Unit
        }
        // قياس سرعة النقر — بعد تسجيل الضغطة الحالية
        tapTimes.addLast(now)
        while (tapTimes.isNotEmpty() && now - tapTimes.first() > MisbahaSplashMath.RATE_WINDOW_MS) {
            tapTimes.removeFirst()
        }
        val rate = MisbahaSplashMath.tapsPerSecond(tapTimes, now)

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
        if (step.roundCompleted) {
            _events.tryEmit(MisbahaEvent.RoundCompleted)
            persistNow()
        } else {
            _events.tryEmit(
                MisbahaEvent.Counted(
                    particles = MisbahaSplashMath.tapParticles(rate),
                ),
            )
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

    /** طبيعي حالة التسرع مع إعادة التعيين — عدّ نظيف من جديد */
    fun resetRush() {
        rushGuard.reset()
        _pausedUntil.value = 0L
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
