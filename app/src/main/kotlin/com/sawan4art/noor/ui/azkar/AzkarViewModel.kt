package com.sawan4art.noor.ui.azkar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sawan4art.noor.data.AzkarRepository
import com.sawan4art.noor.data.AzkarSnapshot
import com.sawan4art.noor.domain.azkar.AzkarItem
import com.sawan4art.noor.domain.azkar.AzkarLibrary
import com.sawan4art.noor.domain.azkar.AzkarMath
import com.sawan4art.noor.domain.azkar.CustomDhikr
import com.sawan4art.noor.domain.tasbih.MisbahaRushGuard
import com.sawan4art.noor.domain.tasbih.MisbahaSplashMath
import java.time.ZoneId
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
 * أحداث الأذكار — الشاشة (النقطة السائلة لعدّاد الصلاة على النبي)
 * تنصت لها للتناثر والاهتزاز.
 */
sealed interface AzkarEvent {

    /** صلاة معدودة — [particles] قطرات التناثر المقترحة (دالة سرعة النقر) */
    data class PraiseCounted(val particles: Int) : AzkarEvent

    /** بلوغ مائةٍ كاملة (وكل مائة بعدها) — رشّة أقصى ونبضة واهتزاز أوضح */
    data object PraiseHundredth : AzkarEvent

    /** انكشاف التسرع — تنبيه وإيقاف العدّ */
    data object RushWarned : AzkarEvent
}

/**
 * حالة الأذكار:
 * - [praiseCount] عدّ الصلاة على النبي الحرّ.
 * - [today] مفتاح اليوم الحالي و[doneToday] علامات إتمام الأذكار
 *   الصالحة لهذا اليوم فقط (تتجدد كل يوم).
 * - [roundsToday] العدّاد الرقمي الجاري لكل بطاقة (alpha28) —
 *   يُصفَّر عند إتمام الذكر ويتجدد كل يوم مع العلامات.
 */
data class AzkarState(
    val praiseCount: Int = 0,
    val today: Long = 0L,
    val doneToday: Set<String> = emptySet(),
    val roundsToday: Map<String, Int> = emptyMap(),
    val customs: List<CustomDhikr> = emptyList(),
    val deleted: Set<String> = emptySet(),
)

/**
 * مسؤول الأذكار — على نمط MisbahaViewModel نفسه: العدّ يعيش في
 * الذاكرة سرعةً لتتابع النقر، والحفظ مؤجَّل (٧٠٠ مللي سكّة هدوء)
 * لا مع كل ضغطة، وحارس التسرع هو MisbahaRushGuard نفسه المُختبَر
 * (الصلاة الإبراهيمية أطول نطقاً من التسبيحة فيلمس الحارس فتيرة
 * العجلة فقط لا القراءة العادية).
 */
class AzkarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AzkarRepository(application)

    private val _state = MutableStateFlow(AzkarState())
    val state: StateFlow<AzkarState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AzkarEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<AzkarEvent> = _events.asSharedFlow()

    /** حارس التسرع — إيقاف العدّ عند النقر بآلة أسرع من النطق */
    private val rushGuard = MisbahaRushGuard()

    /** نهاية فترة الإيقاف الحالية (0 = لا إيقاف) — للتنبيه والعدّ التنازلي */
    private val _pausedUntil = MutableStateFlow(0L)
    val pausedUntil: StateFlow<Long> = _pausedUntil.asStateFlow()

    /** أوقات النقر الأخيرة لقياس سرعة النقر → كثافة التناثر */
    private val tapTimes = ArrayDeque<Long>()

    private var loaded = false
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val snap = repository.snapshot.first()
            val today = AzkarMath.epochDayOf(System.currentTimeMillis(), ZoneId.systemDefault())
            _state.value = AzkarState(
                praiseCount = snap.praiseCount,
                today = today,
                doneToday = AzkarMath.effectiveDone(today, snap.day, snap.done),
                roundsToday = if (today == snap.day) snap.rounds else emptyMap(),
                customs = snap.customs,
                deleted = snap.deleted,
            )
            loaded = true
        }
    }

    /**
     * صلاةٌ واحدة على النبي ﷺ — حارس التسرع أولاً كالمسبحة: مُهمَلة
     * أثناء الإيقاف، وانكشاف التسرع ينبّه ويوقف. العدّ حرّ يتصاعد،
     * وكل مائةٍ احتفالٌ وحفظٌ فوري كي لا يضيع الحصاد.
     */
    fun tapPraise() {
        if (!loaded) return
        val now = System.currentTimeMillis()
        when (rushGuard.onTap(now)) {
            MisbahaRushGuard.Verdict.IGNORED_PAUSED -> return
            MisbahaRushGuard.Verdict.RUSH_DETECTED -> {
                _pausedUntil.value = rushGuard.pausedUntil
                _events.tryEmit(AzkarEvent.RushWarned)
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

        val next = AzkarMath.nextPraiseCount(_state.value.praiseCount)
        _state.update { it.copy(praiseCount = next) }
        if (AzkarMath.isHundredth(next)) {
            _events.tryEmit(AzkarEvent.PraiseHundredth)
            persistNow()
        } else {
            _events.tryEmit(
                AzkarEvent.PraiseCounted(
                    particles = MisbahaSplashMath.tapParticles(rate),
                ),
            )
            scheduleSave()
        }
    }

    /** تصفير عدّ الصلاة على النبي — من الصفر من جديد */
    fun resetPraise() {
        if (!loaded) return
        _state.update { it.copy(praiseCount = 0) }
        persistNow()
    }

    /**
     * نقرة عدّ على بطاقة ذكرٍ بعدده المسنون [target] — العدّاد الرقمي
     * (alpha28): كل نقرة تُصاعد عدّ الجولة، وبلوغ العدد يعلّم البطاقة
     * مُتمّة (فيتغير لونها كما في التعليم) ويرجع العدّ لجولةٍ جديدة،
     * ونقرة البطاقة المكتملة تبدأ دورةً من الصفر. يُحفظ فوراً —
     * نقرات عدّ البطاقات بطيئة بمقياس محرك المسبحة.
     */
    fun tapCount(itemId: String, target: Int) {
        if (!loaded) return
        _state.update { cur ->
            val today = AzkarMath.epochDayOf(System.currentTimeMillis(), ZoneId.systemDefault())
            val base = if (today != cur.today) {
                // يوم جديد والتطبيق مفتوح — علامات وعدّادات أمس تُحصَّن ولا تُمَس
                cur.copy(today = today, doneToday = emptySet(), roundsToday = emptyMap())
            } else {
                cur
            }
            if (itemId in base.doneToday) {
                // البطاقة مكتملة — النقرة تفتح دورةً جديدة (إلغاء التعليم + عدّ من الصفر)
                base.copy(
                    doneToday = base.doneToday - itemId,
                    roundsToday = base.roundsToday - itemId,
                )
            } else {
                val tap = AzkarMath.tapRound(base.roundsToday[itemId] ?: 0, target)
                if (tap.completed) {
                    base.copy(
                        doneToday = base.doneToday + itemId,
                        roundsToday = base.roundsToday - itemId,
                    )
                } else {
                    base.copy(roundsToday = base.roundsToday + (itemId to tap.round))
                }
            }
        }
        persistNow()
    }

    /**
     * تحديث مفتاح اليوم عند فتح الشاشات — لو مرّت ليلةٌ والتطبيق مفتوح
     * تعود العلامات صفراً للجميع (وملخصات التبويب تتبعها).
     */
    fun refreshDay() {
        if (!loaded) return
        val today = AzkarMath.epochDayOf(System.currentTimeMillis(), ZoneId.systemDefault())
        _state.update { cur ->
            if (today == cur.today) cur
            else cur.copy(today = today, doneToday = emptySet(), roundsToday = emptyMap())
        }
    }

    // ===== أذكار المستخدم — إضافة وحذف واسترجاع (alpha44) =====

    /**
     * إنشاء ذكرٍ إضافي في قسمٍ — معرّفه فريد يبدأ بـ «custom_»،
     * وعدده مسنون من العادات (١، ٣، ٧، ١٠، ٣٣، ١٠٠) والمصدر اختياري.
     * يعود true إن قُبل الذكر (نصٌ غير خالٍ وعددٌ صحيح).
     */
    fun addCustom(groupId: String, text: String, count: Int, source: String): Boolean {
        if (!loaded) return false
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return false
        val safeCount = AzkarMath.sanitizedCount(count)
        val cleanSource = source.trim()
        val id = AzkarLibrary.CUSTOM_PREFIX + System.currentTimeMillis().toString(36) +
            "_" + (0..999).random()
        _state.update { cur ->
            cur.copy(
                customs = cur.customs + CustomDhikr(
                    groupId,
                    AzkarItem(id, cleanText, safeCount, cleanSource.ifEmpty { "ذكر مضاف" }),
                ),
            )
        }
        persistNow()
        return true
    }

    /**
     * حذف ذكرٍ: المضاف من المستخدم يُمحى نهائياً، والافتراضي يُسجَّل
     * في «المحذوف» فقط — فزر الاسترجاع يعيده متى شاء المالك.
     */
    fun deleteItem(itemId: String) {
        if (!loaded) return
        _state.update { cur ->
            if (AzkarLibrary.isCustom(itemId)) {
                cur.copy(customs = cur.customs.filter { it.item.id != itemId })
            } else {
                cur.copy(
                    deleted = cur.deleted + itemId,
                    // علامة إتمامه وعدّاده يُحصَّنان معه
                    doneToday = cur.doneToday - itemId,
                    roundsToday = cur.roundsToday - itemId,
                )
            }
        }
        persistNow()
    }

    /** استرجاع كل الأذكار الافتراضية المحذوفة — المضاف لا يُمَس */
    fun restoreDefaults() {
        if (!loaded) return
        _state.update { cur -> cur.copy(deleted = emptySet()) }
        persistNow()
    }

    /** حفظ مؤجَّل — ٧٠٠ مللي سكّة هدوء تتجمع نقرات السرعة في قرصٍ واحد */
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
                AzkarSnapshot(
                    praiseCount = s.praiseCount,
                    day = s.today,
                    done = s.doneToday,
                    rounds = s.roundsToday,
                ),
            )
        }
    }
}
