package com.sawan4art.noor.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * حالة «مزلاج الشمس والقمر» — منسّق السحب السائل وطبقة الانقسام.
 *
 * نفس إيقاع الشريط السفلي حرفياً (نقل ألزق بتمهّل — دفعة alpha4):
 * المصدر يُنقص، والرأس يهبط بـ EaseOutBack، ثم رجفة حية تذبل بعد الهبوط.
 *
 * قواعد المالك (2026-10-31):
 * - الأيقونة مع اليد حرفياً أثناء السحب (ما لم تُرفع اليد تبقى حيث هي).
 * - الفاتح خلف الأيقونة (جهة المغادرة) والمظلم أمامها — حدّ الانقسام عند الأيقونة.
 * - الرفع يحسم بالضفة التي عبرت المنتصف، والنقرة تبدّل فوراً برحلة كاملة.
 */
class ThemeDialState {

    // ===== هندسة المسار بإحداثيات الجذر (بكسل) =====
    var trackLeft by mutableFloatStateOf(0f); private set
    var trackTop by mutableFloatStateOf(0f); private set
    var trackWidth by mutableFloatStateOf(0f); private set
    var trackHeight by mutableFloatStateOf(0f); private set
    var screenWidth by mutableFloatStateOf(0f); private set

    // مقاييس مرسلة من الواجهة بالبكسل
    var blobRadiusPx by mutableFloatStateOf(0f); private set
    var fadePx by mutableFloatStateOf(0f); private set
    var bandPx by mutableFloatStateOf(0f); private set
    var radialPx by mutableFloatStateOf(0f); private set
    var anchorPadPx by mutableFloatStateOf(0f); private set

    // ===== السائل الحي — نفس نمط الشريط السفلي =====
    val headX = Animatable(0f)
    val headR = Animatable(0f)
    val tailX = mutableFloatStateOf(0f)
    val tailR = Animatable(0f)
    val pulse = Animatable(1f)
    val jiggle = Animatable(0f)
    val glow = Animatable(0f)
    val maskX = Animatable(0f)

    // ===== أطوار المزلاج =====
    var dragging by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var splitActive by mutableStateOf(false); private set
    var overlayIsLight by mutableStateOf(false); private set
    var settled by mutableStateOf(false); private set

    // نطاق المسح الحالي — كسر التقدم يذوّب التوهج أثناء الرسم
    var sweepFrom by mutableFloatStateOf(0f); private set
    var sweepTo by mutableFloatStateOf(1f); private set

    private var dragStartTouch = 0f
    private var dragStartHead = 0f

    val anchorLeft: Float get() = trackLeft + anchorPadPx
    val anchorRight: Float get() = trackLeft + trackWidth - anchorPadPx
    val trackCenterY: Float get() = trackTop + trackHeight / 2f

    /** تحديد موضع المسار من العنصر الفاعل (تُستدعى من النسختين — القيم متطابقة) */
    fun placeTrack(left: Float, top: Float, width: Float, height: Float) {
        if (abs(trackLeft - left) < 0.5f && abs(trackTop - top) < 0.5f &&
            abs(trackWidth - width) < 0.5f && abs(trackHeight - height) < 0.5f
        ) {
            return
        }
        trackLeft = left
        trackTop = top
        trackWidth = width
        trackHeight = height
    }

    fun configure(
        screenW: Float,
        blobRadius: Float,
        fade: Float,
        band: Float,
        radial: Float,
        anchorPad: Float,
    ) {
        screenWidth = screenW
        blobRadiusPx = blobRadius
        fadePx = fade
        bandPx = band
        radialPx = radial
        anchorPadPx = anchorPad
    }

    /** قبضة كريمية حول المسار كله — الأيقونة سائلة تستحق هامش لمس سخي */
    fun isInsideTrack(x: Float, y: Float): Boolean {
        val slop = blobRadiusPx
        return x >= trackLeft - slop && x <= trackLeft + trackWidth + slop &&
            y >= trackTop - slop && y <= trackTop + trackHeight + slop
    }

    fun progressOfHead(): Float = ThemeDialMath.progress(headX.value, anchorLeft, anchorRight)

    /** كسر المسح الحالي 0..1 — التوهج يذبل به في الرحلة النهائية */
    fun sweepFraction(): Float {
        val span = sweepTo - sweepFrom
        if (abs(span) < 1e-4f) return 0f
        return ((maskX.value - sweepFrom) / span).coerceIn(0f, 1f)
    }

    /** طاقة حياة السائل: اشتعال أثناء السحب، رجفة بعد الهبوط، سكون هادئ في الثبات */
    fun energy(): Float = if (dragging) 1f else jiggle.value

    // ===== بداية السحب: وُلد المصدر عند مرسى الرأس وامتلأ =====
    suspend fun beginDrag(touchX: Float, committedDark: Boolean) {
        if (busy || dragging || trackWidth <= 0f) return
        dragging = true
        overlayIsLight = committedDark // الهدف دائماً عكس المثبت
        splitActive = true
        dragStartTouch = touchX
        dragStartHead = headX.value
        tailX.floatValue = headX.value
        tailR.snapTo(blobRadiusPx)
        headR.snapTo(blobRadiusPx)
        maskX.snapTo(headX.value)
        glow.snapTo(1f)
        sweepFrom = headX.value
        sweepTo = headX.value
    }

    /** الأيقونة مع اليد حرفياً — إزاحة عن نقطة القبض محصورة بين المرسى */
    suspend fun dragTo(touchX: Float) {
        if (!dragging) return
        val target = dragStartHead + (touchX - dragStartTouch)
        headX.snapTo(ThemeDialMath.clampX(target, anchorLeft, anchorRight))
        maskX.snapTo(headX.value) // حدّ الانقسام يركب الأيقونة
    }

    /** رفع اليد: الضفة التي عبرها الإصبع تحسم، وإلا رجوع سائل لمرسى البداية */
    fun endDrag(onCommitLight: (Boolean) -> Unit, scope: CoroutineScope) {
        if (!dragging) return
        dragging = false
        val releaseX = headX.value
        val releaseLight = ThemeDialMath.commitLight(releaseX, anchorLeft, anchorRight)
        val commit = releaseLight == overlayIsLight
        if (commit) {
            sweepToTarget(overlayIsLight, onCommitLight, scope)
        } else {
            revertTo(if (overlayIsLight) anchorLeft else anchorRight, scope)
        }
    }

    /** نقرة: تبديل فوري برحلة كاملة من المرسى الحاضر — شروق من جهته */
    suspend fun toggle(committedDark: Boolean, onCommitLight: (Boolean) -> Unit, scope: CoroutineScope) {
        if (busy || dragging || trackWidth <= 0f) return
        overlayIsLight = committedDark
        splitActive = true
        tailX.floatValue = headX.value
        tailR.snapTo(blobRadiusPx)
        headR.snapTo(blobRadiusPx)
        maskX.snapTo(headX.value)
        glow.snapTo(1f)
        sweepToTarget(committedDark, onCommitLight, scope)
    }

    /**
     * مزامنة المرسى مع الثيم المثبت: أول قياس تُثبَّت فوراً، وتغيّر الإعدادات
     * يولّد رحلة سائلة صغيرة — بنفس حيوية الشريط السفلي.
     */
    suspend fun syncTo(committedDark: Boolean, scope: CoroutineScope) {
        if (busy || dragging || trackWidth <= 0f) return
        val target = if (committedDark) anchorLeft else anchorRight
        if (!settled) {
            headX.snapTo(target)
            headR.snapTo(blobRadiusPx)
            tailR.snapTo(0f)
            maskX.snapTo(target)
            settled = true
            return
        }
        if (abs(headX.value - target) > 1f) {
            busy = true
            tailX.floatValue = headX.value
            tailR.snapTo(blobRadiusPx)
            scope.launch { headX.animateTo(target, tween(HEAD_MS, easing = EaseOutBack)) }
            scope.launch { tailR.animateTo(0f, tween(HEAD_MS, easing = FastOutSlowInEasing)) }
            scope.launch {
                delay(HEAD_MS - 40L)
                jiggle.snapTo(1f)
                jiggle.animateTo(0f, tween(JIGGLE_MS, easing = LinearEasing))
            }
            scope.launch {
                delay(HEAD_MS + 40L)
                busy = false
            }
        }
    }

    /** الرحلة النهائية: المصدر يُنقص، الرأس يهبط بارتداد، والقناع يُساق خارج الشاشة */
    private fun sweepToTarget(commitLight: Boolean, onCommitLight: (Boolean) -> Unit, scope: CoroutineScope) {
        busy = true
        val endB = ThemeDialMath.sweepTargetX(commitLight, screenWidth, fadePx)
        sweepFrom = maskX.value
        sweepTo = endB
        val anchor = if (commitLight) anchorRight else anchorLeft
        scope.launch { tailR.animateTo(0f, tween(DRAIN_MS, easing = FastOutSlowInEasing)) }
        scope.launch { headX.animateTo(anchor, tween(HEAD_MS, easing = EaseOutBack)) }
        scope.launch { maskX.animateTo(endB, tween(SWEEP_MS, easing = FastOutSlowInEasing)) }
        scope.launch {
            delay(HEAD_MS - 40L)
            jiggle.snapTo(1f)
            jiggle.animateTo(0f, tween(JIGGLE_MS, easing = LinearEasing))
            pulse.snapTo(1.16f)
            pulse.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 280f))
        }
        scope.launch {
            delay(SWEEP_MS + 40L)
            onCommitLight(commitLight)
            splitActive = false
            busy = false
            tailR.snapTo(0f)
            maskX.snapTo(endB)
        }
    }

    /** الرجوع: الرأس يعود لمرساه والمصدر يُسحب إليه حتى آخر قطرة */
    private fun revertTo(startAnchor: Float, scope: CoroutineScope) {
        busy = true
        sweepFrom = maskX.value
        sweepTo = startAnchor
        scope.launch { headX.animateTo(startAnchor, tween(HEAD_MS, easing = EaseOutBack)) }
        scope.launch { maskX.animateTo(startAnchor, tween(HEAD_MS, easing = FastOutSlowInEasing)) }
        scope.launch { tailR.animateTo(0f, tween(HEAD_MS, easing = FastOutSlowInEasing)) }
        scope.launch {
            delay(HEAD_MS - 40L)
            jiggle.snapTo(1f)
            jiggle.animateTo(0f, tween(JIGGLE_MS, easing = LinearEasing))
        }
        scope.launch {
            delay(HEAD_MS + 40L)
            splitActive = false
            busy = false
            headR.snapTo(blobRadiusPx)
            maskX.snapTo(startAnchor)
        }
    }

    companion object {
        // نفس مدد الشريط السفلي حرفياً (alpha4: نقل ألزق بتمهّل)
        const val DRAIN_MS = 1040
        const val HEAD_MS = 800
        const val JIGGLE_MS = 1250
        /** المسح النهائي أطول قليلاً من رحلة الرأس ليغمر الثيم كاملاً قبل القلب */
        const val SWEEP_MS = HEAD_MS + 160
    }
}
