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
 * - النصف الفاتح يسار الحدّ والمظلم يمينه دائماً — المعاينة **قصّ صلب**
 *   (clipRect بلا أي DstIn/Offscreen — آلية القناع القديمة كانت تفشل بصمت
 *   على جهاز المالك فلم يرَ الانقسام قط).
 * - الرفع يحسم بالضفة التي عبرت المنتصف، والنقرة تبدّل فوراً برحلة كاملة.
 * - الفصل **حاد وصريح** وعلى حدّ الانقسام **خط ضوء رفيع ساطع** بهالة ضيقة
 *   مركّزة عليه حصرياً (توجيه المالك alpha15: «خط فاصل رفيع ومشرق... المنطقة
 *   الانتقالية لا تتعدى بضع بكسلات... النصف الأيمن داكن بالكامل والأيسر فاتح
 *   بالكامل») — لا حزام عريض ولا بؤرة.
 * - الإيماءة من الأيقونة وحدها (صندوق لمس يركب الأيقونة) — الفراغ والمسار
 *   يمرّان للطبقات تحتيهما فلا لاج ولا ابتلاع لمسات (درس alpha7).
 * - المزلاج كلّه في الرئيسية فقط — مغادرتها **تُطبّع الحالة بلا شرط**
 *   (الإحباط المشروط كان فخ alpha13: بلاغ المالك «الأيقونة معلقة في النص»
 *   عند العودة من صفحة أخرى — الصفحة المغادرة أثناء انزلاق التبويب كانت
 *   تُبلّغ بإحداثيات منزلقة تسمّم المرسى، ورحلة التصحيح آنذاك كانت تهبط
 *   على المرسى المسموم وتبقى فيه).
 * - **قاتل الومضة** (بلاغ المالك alpha12: «ومضة بعد ما الوضع يتفعل سواء
 *   المظلم أو الفاتح»): نهاية الرحلة **لا تُسقط** غطاء المعاينة فوراً —
 *   الغطاء صورة كاملة للثيم الجديد ويبقى معلقاً حتى يلتقط الثيم المثبت
 *   هدفه فعلاً (وصوله متأخر عبر DataStore إطاراتٍ قليلة) ثم يُسقط من NoorApp
 *   في لحظة الوصول نفسها. الإسقاط الفوري كان يكشف الثيم القديم لحظةً =
 *   الومضة. ومهلة أمان قصيرة تمنع تجمّد المزلاج لو تأخر الوصول عنها.
 */
class ThemeDialState {

    // ===== هندسة المسار بإحداثيات الجذر (بكسل) =====
    var trackLeft by mutableFloatStateOf(0f); private set
    var trackTop by mutableFloatStateOf(0f); private set
    var trackWidth by mutableFloatStateOf(0f); private set
    var trackHeight by mutableFloatStateOf(0f); private set
    var screenWidth by mutableFloatStateOf(0f); private set
    var screenHeight by mutableFloatStateOf(0f); private set

    // مقاييس مرسلة من الواجهة بالبكسل
    var blobRadiusPx by mutableFloatStateOf(0f); private set
    var anchorPadPx by mutableFloatStateOf(0f); private set
    /** نصف هالة خط الضوء بالبكسل — أوسع ما يُرسم حول حدّ الانقسام (سُمت المسح) */
    var edgeGlowPx by mutableFloatStateOf(0f); private set

    // ===== السائل الحي — نفس نمط الشريط السفلي =====
    val headX = Animatable(0f)
    val headR = Animatable(0f)
    val tailX = mutableFloatStateOf(0f)
    val tailR = Animatable(0f)
    val pulse = Animatable(1f)
    val jiggle = Animatable(0f)
    val maskX = Animatable(0f)
    /** قوة التوهج الخافت 0..1 — يخفت مع الرجوع لمرسى البداية كي لا يُطفأ فجأة */
    val beam = Animatable(0f)

    // ===== أطوار المزلاج =====
    var dragging by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var splitActive by mutableStateOf(false); private set
    var overlayIsLight by mutableStateOf(false); private set
    var settled by mutableStateOf(false); private set

    private var dragStartTouch = 0f
    private var dragStartHead = 0f

    /**
     * جيل الرحلة الحالية — كل تفاعل جديد يُبطل مهلة الأمان المعلّقة لرحلة
     * أقدم كي لا تُسقط غطاءً هي لعبة جديدة تماماً (انظر sweepToTarget).
     */
    private var sweepGeneration = 0

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
        screenH: Float,
        blobRadius: Float,
        anchorPad: Float,
        glowExtent: Float,
    ) {
        screenWidth = screenW
        screenHeight = screenH
        blobRadiusPx = blobRadius
        anchorPadPx = anchorPad
        edgeGlowPx = glowExtent
    }

    fun progressOfHead(): Float = ThemeDialMath.progress(headX.value, anchorLeft, anchorRight)

    /** قبضة الأيقونة وحدها — مربع لمس حول رأس السائل (الإيماءة لا تعمل من فراغ المسار) */
    fun isInsideIcon(x: Float, y: Float, halfTouch: Float): Boolean =
        abs(x - headX.value) <= halfTouch && abs(y - trackCenterY) <= halfTouch

    /**
     * لصق الرأس بمرسى الثيم المثبت عندما تنزلق إحداثيات الجذر (عودة الرئيسية
     * منزاحةً أثناء انتقال التبويب) — بلا أي أثر أثناء السحب أو الرحلة أو أول قياس.
     */
    suspend fun followAnchors(committedDark: Boolean) {
        if (dragging || busy || !settled || trackWidth <= 0f) return
        val target = if (committedDark) anchorLeft else anchorRight
        if (abs(headX.value - target) > 0.5f) {
            headX.snapTo(target)
            maskX.snapTo(target)
        }
    }

    /**
     * تطبيع كامل للحالة — يُستدعى عند مغادرة الرئيسية وتغيّر الثيم بعيداً عنها.
     *
     * درس alpha13 (بلاغ المالك: «الأيقونة معلقة في النص» بعد العودة من صفحة
     * أخرى): الإحباط المشروط بـ `if (!dragging && !busy)` كان يترك أي عالق
     * خارج شرطه بلا تنظيف — التطبيع **بلا شرط** أرخص من تشخيص كل سبب عالق:
     * الطبقة أُزيلت فنطاقها مات وأنيميشناتها أُلغيت، وهنا تُصفّر كل القيم
     * وتُلصق الأيقونة بمرسى الثيم المثبت الحاضر، فتعود الرئيسية نظيفة دائماً.
     */
    suspend fun abort(committedDark: Boolean) {
        dragging = false
        busy = false
        splitActive = false
        headX.snapTo(if (committedDark) anchorLeft else anchorRight)
        headR.snapTo(blobRadiusPx)
        tailR.snapTo(0f)
        maskX.snapTo(headX.value)
        beam.snapTo(0f)
        jiggle.snapTo(0f)
        pulse.snapTo(1f)
    }

    /** طاقة حياة السائل: اشتعال أثناء السحب، رجفة بعد الهبوط، سكون هادئ في الثبات */
    fun energy(): Float = if (dragging) 1f else jiggle.value

    /**
     * إسقاط غطاء المعاينة بعد وصول الثيم المثبت إلى هدفه — **لحظة القلب الآمنة**.
     * يُستدعى من NoorApp لحظة تغيّر الثيم المثبت (الوصول عبر DataStore متأخر
     * عن طلب التثبيت) — الغطاء والأساس يعرضان الثيم الجديد معاً فلا ومضة.
     */
    suspend fun dropPreview() {
        if (!splitActive || dragging) return
        splitActive = false
        busy = false
        maskX.snapTo(headX.value)
    }

    // ===== بداية السحب: وُلد المصدر عند مرسى الرأس وامتلأ =====
    suspend fun beginDrag(touchX: Float, committedDark: Boolean) {
        if (busy || dragging || trackWidth <= 0f) return
        sweepGeneration++ // أي تفاعل جديد يُبطل مهلة أمان رحلة أقدم
        dragging = true
        overlayIsLight = committedDark // الهدف دائماً عكس المثبت
        splitActive = true
        dragStartTouch = touchX
        dragStartHead = headX.value
        tailX.floatValue = headX.value
        tailR.snapTo(blobRadiusPx)
        headR.snapTo(blobRadiusPx)
        maskX.snapTo(headX.value)
        beam.snapTo(1f)
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
        beam.snapTo(1f)
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
                // لصق الهبوط (بلاغ المالك alpha13: «الأيقونة معلقة في النص»):
                // لو انزاحت مراسي المسار أثناء الرحلة (انتقال تبويب مثلاً)
                // يُلصق الرأس بالمرسى الحاضر لحظة الهبوط — لا يبقى عالقاً
                // بمرسى قديم بلا شيء يعيد تفعيله بعد زوال الانشغال
                val fresh = if (committedDark) anchorLeft else anchorRight
                headX.snapTo(fresh)
                maskX.snapTo(fresh)
            }
        }
    }

    /** الرحلة النهائية: المصدر يُنقص، الرأس يهبط بارتداد، والحدّ وخط ضوئه يُساقان خارج الشاشة */
    private fun sweepToTarget(commitLight: Boolean, onCommitLight: (Boolean) -> Unit, scope: CoroutineScope) {
        val gen = ++sweepGeneration
        busy = true
        val endB = ThemeDialMath.sweepTargetX(commitLight, screenWidth, edgeGlowPx)
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
            tailR.snapTo(0f)
            maskX.snapTo(endB)
            // قاتل الومضة (بلاغ المالك alpha12): الإسقاط الفوري كان يكشف
            // الثيم القديم إطاراتٍ قليلة قبل وصول تثبيت DataStore — الغطاء
            // (صورة كاملة للثيم الجديد) يبقى معلقاً حتى يُسقطه NoorApp لحظة
            // وصول الثيم المثبت. ومهلة الأمان تحمي من تعطّل الكتابة كي لا
            // يتجمّد المزلاج أبداً — وجيل الرحلة يُبطلها لو بدأ تفاعل جديد.
            delay(COMMIT_LANDING_GRACE_MS)
            if (splitActive && sweepGeneration == gen) {
                splitActive = false
                busy = false
                maskX.snapTo(headX.value)
            }
        }
    }

    /** الرجوع: الرأس يعود لمرساه والمصدر يُسحب إليه حتى آخر قطرة، والتوهج يخفت معه */
    private fun revertTo(startAnchor: Float, scope: CoroutineScope) {
        sweepGeneration++ // يُبطل أي مهلة أمان معلّقة كي لا تُسقط هذا الرجوع
        busy = true
        scope.launch { headX.animateTo(startAnchor, tween(HEAD_MS, easing = EaseOutBack)) }
        scope.launch { maskX.animateTo(startAnchor, tween(HEAD_MS, easing = FastOutSlowInEasing)) }
        scope.launch { tailR.animateTo(0f, tween(HEAD_MS, easing = FastOutSlowInEasing)) }
        scope.launch { beam.animateTo(0f, tween(HEAD_MS, easing = FastOutSlowInEasing)) }
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
            // لصق الهبوط نفسه: المرسى الحاضر يفوز على المرسي المُمرّر لو انزاح
            val fresh = if (overlayIsLight) anchorLeft else anchorRight
            headX.snapTo(fresh)
            maskX.snapTo(fresh)
        }
    }

    companion object {
        // نفس مدد الشريط السفلي حرفياً (alpha4: نقل ألزق بتمهّل)
        const val DRAIN_MS = 1040
        const val HEAD_MS = 800
        const val JIGGLE_MS = 1250
        /** المسح النهائي أطول قليلاً من رحلة الرأس ليغمر الثيم كاملاً قبل القلب */
        const val SWEEP_MS = HEAD_MS + 160
        /**
         * مهلة أمان انتظار وصول الثيم المثبت بعد طلب التثبيت — وصوله العادي
         * خلال إطار أو إطارين، وهذه المهلة شبكة أمان فقط (كتابة معطوبة)
         * كي لا يبقى الغطاء معلقاً والمزلاج مشغولاً إلى الأبد.
         */
        const val COMMIT_LANDING_GRACE_MS = 700L
    }
}
