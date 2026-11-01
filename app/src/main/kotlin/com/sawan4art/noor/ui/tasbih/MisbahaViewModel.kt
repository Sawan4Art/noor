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

    /** اكتمال قسم (٣٣/٣٣/٣٣) — اهتزاز وانتقال تلقائي للقسم الذي يليه بالترتيب */
    data object DhikrCompleted : MisbahaEvent

    /** اكتمال المائة كلها (٩٩ وتكميلتها) — احتفال أكبر وعودة لأول الترتيب */
    data object SequenceCompleted : MisbahaEvent

    /** انكشاف التسرع — تنبيه وبدء إيقاف العدّ */
    data object RushWarned : MisbahaEvent
}

/**
 * حالة المسبحة — التسلسل الثابت لتسبيح ما بعد الصلاة (alpha26):
 * القسم الحالي في الترتيب وعدّه الداخلي وعدد المائات المكتملة.
 */
data class MisbahaState(
    /** موضع القسم الحالي في التسلسل ٠..٣ */
    val stepIndex: Int = 0,
    /** عدّ القسم الحالي داخل دوره */
    val count: Int = 0,
    /** عدد المائات المكتملة كاملة (٩٩ وتكميلتها) */
    val completedSequences: Int = 0,
) {
    val step: MisbahaDhikr
        get() = MisbahaDhikrList.SEQUENCE[stepIndex.coerceIn(0, MisbahaDhikrList.SEQUENCE.lastIndex)]

    /** العدّاد التنازلي — يبدأ من العدد المسنون ويهبط حتى الصفر */
    val remaining: Int get() = MisbahaMath.remaining(count, step.target)
}

/**
 * مسؤول المسبحة — العدّ يعيش في الذاكرة سرعةً لتتابع النقر السريع،
 * والحفظ إلى DataStore **مؤجَّل** (٧٠٠ مللي سكّة هدوء) فلا قرصٌ صلب
 * مع كل تسبيحة، واكتمالُ قسمٍ أو مائةٍ يحفظ فوراً كي لا يضيع الحصاد.
 *
 * **الانتقال التلقائي بالترتيب (alpha26 — توجيه المالك)**: «بعد اتمام
 * تسبيح معين اوتماتك ينتقل للي بعده بس بترتيب» — إتمام القسم يهتز
 * وينتقل وحده، ولا خلط بين الأقسام: المسبحة تسبيح ما بعد الصلاة فقط.
 *
 * **حارس التسرع (alpha25/26)**: النقر بآلة أسرع من النطق يُنبه ويوقف
 * العدّ ٣ ثوانٍ — الضغطات أثناء الإيقاف مُهمَلة بلا عدّ ولا تناثر،
 * ويُعرض وقت الاستئناف للشاشة عبر pausedUntil.
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
            val target = MisbahaDhikrList.SEQUENCE[
                snap.stepIndex.coerceIn(0, MisbahaDhikrList.SEQUENCE.lastIndex),
            ].target
            _state.value = MisbahaState(
                stepIndex = snap.stepIndex,
                // عدّ قديم خارج حدود الهدف يبدأ نظيفاً لا مكسوراً
                count = snap.count.coerceIn(0, target - 1),
                completedSequences = snap.completedSequences,
            )
            loaded = true
        }
    }

    /**
     * تسبيحة واحدة — حارس التسرع أولاً: مُهمَلة أثناء الإيقاف، وانكشاف
     * التسرع ينبّه ويوقف. المعدودة تتقدم بالتسلسل الثابت: عدّ القسم
     * يكبر حتى يكتمل فينتقل تلقائياً للقسم التالي، وآخر الأقسام تُكمل
     * المائة وترجع لأول الترتيب.
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
        val adv = MisbahaMath.advanceSequence(s.stepIndex, s.count, s.completedSequences)
        _state.update { cur ->
            cur.copy(
                stepIndex = adv.stepIndex,
                count = adv.count,
                completedSequences = adv.completedSequences,
            )
        }
        when {
            adv.sequenceCompleted -> {
                _events.tryEmit(MisbahaEvent.SequenceCompleted)
                persistNow()
            }
            adv.dhikrCompleted -> {
                _events.tryEmit(MisbahaEvent.DhikrCompleted)
                persistNow()
            }
            else -> {
                _events.tryEmit(
                    MisbahaEvent.Counted(
                        particles = MisbahaSplashMath.tapParticles(rate),
                    ),
                )
                scheduleSave()
            }
        }
    }

    /** إعادة عدّ القسم الحالي فقط — يعيد العدّاد التنازلي لعدّه المسنون */
    fun resetCurrent() {
        if (!loaded) return
        _state.update { it.copy(count = 0) }
        persistNow()
    }

    /** إعادة الكل — العودة لأول الترتيب وتصفير المائات المكتملة */
    fun resetAll() {
        if (!loaded) return
        _state.update { MisbahaState() }
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

    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            repository.save(
                MisbahaSnapshot(
                    stepIndex = s.stepIndex,
                    count = s.count,
                    completedSequences = s.completedSequences,
                ),
            )
        }
    }
}
