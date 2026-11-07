package com.sawan4art.noor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.sawan4art.noor.R
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.ui.theme.AccentPalette
import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.util.TimeFormat

/**
 * ويدجت «نور المسلم» الواحدة — alpha57 بلاغ المالك:
 * «الويدجت مش كلها شغاله اقولك افضل خليها ويدجيت واحده و قابلة
 * للتخصيص بالكامل من الاعدادات».
 *
 * ويدجتٌ واحدة 4×2 قابلة للتمديد، كل عناصرها بيتٌ مستقل يُظهر ويخفيه
 * [WidgetCustomization] من قسم «الويدجت» في الإعدادات: الوقت الحالي،
 * التاريخان بصيغهما، الصلاة القادمة والعدّاد الحيّ، مواقيت الصلوات
 * الستّ، والمسبحة (زرّا تسبيحٍ وتصفيرٍ يعملان من الويدجت ذاتها)،
 * والنقطةُ السائلة تقف خلف الوقت المتبقي أو تُحدّد الصلاة القادمة
 * والحالية كلٌّ بلونها.
 *
 * درس v0.4 المعتمد: JSON في SharedPreferences والخلفية ألفافٍ مرسومة
 * بأطراف دائرية.
 */

/** ألوان الرسم — من التخصيص: مظهرٌ ولونُ خطٍّ ومَيُ أُبرز */
private data class WidgetPalette(
    val bgTop: Int,
    val bgBottom: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
)

private fun palette(c: WidgetCustomization): WidgetPalette {
    val accent = AccentPalette.byId(c.accentId)
    val explicitText = WidgetCustomization.parseColor(c.textColor)
    val base = when (c.theme) {
        WidgetCustomization.Theme.LIGHT -> WidgetPalette(
            bgTop = Color.parseColor("#F5F1E4"),
            bgBottom = Color.parseColor("#E8E2CE"),
            text = explicitText ?: Color.parseColor("#233029"),
            muted = Color.parseColor("#5D6B62"),
            accent = accent.day.primary.toArgb(),
        )
        WidgetCustomization.Theme.DARK -> WidgetPalette(
            bgTop = Color.parseColor("#16110D"),
            bgBottom = Color.parseColor("#221B14"),
            text = explicitText ?: Color.parseColor("#F2EDE0"),
            muted = Color.parseColor("#A99F8D"),
            accent = accent.night.primary.toArgb(),
        )
        WidgetCustomization.Theme.APP -> {
            // يتبع لون التطبيق نفسه — تدرّج بطاقة الهيرو وخفاها
            WidgetPalette(
                bgTop = accent.night.heroTop.toArgb(),
                bgBottom = accent.night.heroBottom.toArgb(),
                text = explicitText ?: accent.night.heroContent.toArgb(),
                muted = explicitText ?: accent.night.heroMuted.toArgb(),
                accent = accent.night.primary.toArgb(),
            )
        }
    }
    return base
}

/** خلفية الويدجت — تدرّج دائري الأطراف بشفافية التخصيص وحدّ لون التطبيق */
internal fun widgetBackgroundBitmap(
    width: Int,
    height: Int,
    top: Int,
    bottom: Int,
    alpha: Float,
    accent: Int,
): Bitmap {
    val w = width.coerceIn(1, 2048)
    val h = height.coerceIn(1, 2048)
    val drawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(top, bottom),
    ).apply {
        cornerRadius = 28f * w / 250f
        setStroke((2f * w / 250f).toInt().coerceAtLeast(1), withAlpha(accent, 0.35f))
    }
    drawable.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bmp
}

/** int بلونٍ وألفا جديدة */
internal fun withAlpha(color: Int, alpha: Float): Int =
    (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24)

/**
 * الرسّام الواحد — يبني RemoteViews للويدجت بأي تخصيص.
 * العدّاد الحي: Chronometer تنازلي يعدّ ثانيةً بثانية بلا تحديثٍ جديد
 * (درس v0.4: عدّاد الويدجت الحي) وقاعدته «زمن الإقلاع + المتبقي».
 */
object WidgetRenderer {

    internal fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        customization: WidgetCustomization,
        widthDp: Int = 250,
        heightDp: Int = 180,
    ): RemoteViews {
        val pal = palette(customization)
        val views = RemoteViews(context.packageName, R.layout.widget_main)

        // الخلفية — دقةٌ معتدلة (١٫٥×) — درس alpha58: كانت ١٠٠٠×٧٢٠
        // (~٢٫٧٥MB داخل باكِت RemoteViews) فوق مخزن البايندر (~١MB) ففشل
        // التحديث وبدت الويدجت «تظهر لكن لا تعمل» (بلاغ المالك)
        val bg = widgetBackgroundBitmap(
            (widthDp * 3 / 2).coerceAtMost(500),
            (heightDp * 3 / 2).coerceAtMost(360),
            pal.bgTop, pal.bgBottom, customization.opacityFraction, pal.accent,
        )
        views.setImageViewBitmap(R.id.widget_bg, bg)

        fun show(id: Int, visible: Boolean) =
            views.setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)

        // ===== الصف ١: العلامة والمدينة والساعة =====
        show(R.id.widget_brand, customization.showBrand)
        views.setTextColor(R.id.widget_brand, pal.text)
        show(R.id.widget_city, customization.showCity && snapshot.city.isNotBlank())
        views.setTextViewText(R.id.widget_city, snapshot.city)
        views.setTextColor(R.id.widget_city, pal.muted)
        show(R.id.widget_clock, customization.showClock)
        views.setTextColor(R.id.widget_clock, pal.text)
        views.setCharSequence(R.id.widget_clock, "setFormat12Hour", "h:mm a")
        views.setCharSequence(R.id.widget_clock, "setFormat24Hour", "HH:mm")

        // ===== الصف ٢: التاريخان بصيغتيهما =====
        show(R.id.widget_date_hijri, customization.showHijri)
        views.setTextViewText(
            R.id.widget_date_hijri,
            Hijri.formatHijri(snapshot.hijriDate, customization.hijriFormat),
        )
        views.setTextColor(R.id.widget_date_hijri, pal.text)
        show(R.id.widget_date_greg, customization.showGregorian)
        views.setTextViewText(
            R.id.widget_date_greg,
            Hijri.formatGregorian(snapshot.gregorianDate, customization.gregFormat),
        )
        views.setTextColor(R.id.widget_date_greg, pal.accent)

        // ===== الصف ٣: القادمة ووقتها والعدّاد =====
        views.setTextViewText(R.id.widget_next_name, snapshot.nextName)
        views.setTextColor(R.id.widget_next_name, pal.text)
        views.setTextViewText(R.id.widget_next_time, snapshot.nextTimeText)
        views.setTextColor(R.id.widget_next_time, pal.accent)
        if (customization.showCountdown) {
            views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
            views.setTextColor(R.id.widget_countdown, pal.muted)
            // الجذر المُصلَح (alpha58 — بلاغ المالك: «الويدجت تظهر في
            // التطبيقات المصغرة لكن لا تعمل»): ضبط القاعدة وحدها لا يُحرك
            // العدّاد — RemoteViews لا يستدعي start() أبداً فبقي العدّاد
            // واقفاً على ٠٠:٠٠. setChronometer(…, started=true) هي التي
            // تُشغّله، وصيغتها التنازلية من `android:countDown` في التخطيط:
            views.setChronometer(
                R.id.widget_countdown,
                chronometerBase(snapshot.remainingSeconds, SystemClock.elapsedRealtime()),
                COUNTDOWN_FORMAT,
                true,
            )
        } else {
            views.setViewVisibility(R.id.widget_countdown, View.GONE)
        }
        bindBubble(
            views, R.id.widget_bubble_countdown,
            customization.bubbleOnCountdown && customization.showCountdown,
            customization.bubbleColorCountdown, pal,
        )

        // ===== الصف ٤: مواقيت الصلوات الست + فقاعتا القادمة والحالية =====
        val currentIndex = WidgetSnapshot.currentRowIndex(snapshot.rows)
        snapshot.rows.forEachIndexed { i, row ->
            val nameId = GRID_NAME_IDS[i]
            val timeId = GRID_TIME_IDS[i]
            views.setTextViewText(nameId, row.name)
            views.setTextColor(nameId, if (row.isPast && i != currentIndex) pal.muted else pal.text)
            views.setTextViewText(timeId, row.time)
            views.setTextColor(
                timeId,
                when {
                    row.isNext -> pal.accent
                    row.isPast && i != currentIndex -> pal.muted
                    else -> pal.text
                },
            )
            val bubbleId = GRID_BUBBLE_IDS[i]
            val lit = when {
                customization.bubbleOnNext && row.isNext -> true
                customization.bubbleOnCurrent && i == currentIndex -> true
                else -> false
            }
            val bubbleColor = when {
                customization.bubbleOnNext && row.isNext -> customization.bubbleColorNext
                customization.bubbleOnCurrent && i == currentIndex -> customization.bubbleColorCurrent
                else -> WidgetCustomization.DEFAULT_BUBBLE
            }
            bindBubble(views, bubbleId, lit && customization.showPrayerTimes, bubbleColor, pal)
        }
        show(R.id.grid_row, customization.showPrayerTimes)

        // ===== الصف ٥: المسبحة =====
        show(R.id.widget_misbaha_row, customization.showMisbaha)
        if (customization.showMisbaha) {
            val count = WidgetStore.misbahaCount(context)
            views.setTextViewText(
                R.id.widget_misbaha_count,
                "المسبحة: ${MushafSurahs.arabicDigits(count)}",
            )
            views.setTextColor(R.id.widget_misbaha_count, pal.text)
            views.setTextColor(R.id.widget_misbaha_tap, pal.accent)
            views.setTextColor(R.id.widget_misbaha_reset, pal.muted)
            views.setOnClickPendingIntent(
                R.id.widget_misbaha_tap,
                misbahaPendingIntent(context, NoorWidget.ACTION_MISBAHA_TAP),
            )
            views.setOnClickPendingIntent(
                R.id.widget_misbaha_reset,
                misbahaPendingIntent(context, NoorWidget.ACTION_MISBAHA_RESET),
            )
        }
        return views
    }

    /** فقاعةٌ سائلة خلف مقامٍ — تظهر وتُلوَّن بلونها أو تختفي */
    private fun bindBubble(views: RemoteViews, id: Int, visible: Boolean, colorRaw: String, pal: WidgetPalette) {
        if (visible) {
            val color = WidgetCustomization.parseColor(colorRaw) ?: pal.accent
            views.setViewVisibility(id, View.VISIBLE)
            views.setInt(id, "setColorFilter", color)
        } else {
            views.setViewVisibility(id, View.GONE)
        }
    }

    private fun misbahaPendingIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            if (action == NoorWidget.ACTION_MISBAHA_TAP) 1 else 2,
            Intent(context, NoorWidget::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** معرفات صفوف المواقيت — أسماء وأوقات الصلوات الست بالترتيب */
    internal val GRID_NAME_IDS = intArrayOf(
        R.id.widget_pname_0, R.id.widget_pname_1, R.id.widget_pname_2,
        R.id.widget_pname_3, R.id.widget_pname_4, R.id.widget_pname_5,
    )
    internal val GRID_TIME_IDS = intArrayOf(
        R.id.widget_ptime_0, R.id.widget_ptime_1, R.id.widget_ptime_2,
        R.id.widget_ptime_3, R.id.widget_ptime_4, R.id.widget_ptime_5,
    )
    internal val GRID_BUBBLE_IDS = intArrayOf(
        R.id.widget_bubble_p0, R.id.widget_bubble_p1, R.id.widget_bubble_p2,
        R.id.widget_bubble_p3, R.id.widget_bubble_p4, R.id.widget_bubble_p5,
    )
}

/**
 * الويدجت الواحدة — تحديثها يعيد الحساب والرسم من الإعدادات الحية،
 * وأزرار المسبحة فيها بثّانٍ داخليّان يزيّدان أو يصفّرون ثم يعيدان
 * الرسم فوراً.
 */
class NoorWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // التحصين (alpha58): أي غلطةٍ في الحساب أو الرسم لا تقتل التحديث
        // كله — كل ويدجت تُرسم بأمانها: التخصيص الحي فإن انفجر الافتراضي
        val snapshot = WidgetSnapshot.compute(context)
        val customization = WidgetStore.customization(context)
        for (id in appWidgetIds) {
            val views = runCatching {
                WidgetRenderer.render(context, snapshot, customization)
            }.getOrElse {
                runCatching {
                    WidgetRenderer.render(
                        context, WidgetSnapshot.compute(context), WidgetCustomization.DEFAULT,
                    )
                }.getOrNull()
            }
            if (views != null) manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MISBAHA_TAP -> WidgetStore.misbahaTap(context)
            ACTION_MISBAHA_RESET -> WidgetStore.misbahaReset(context)
            else -> return
        }
        // ضغطة المسبحة تعيد رسم الويدجت فوراً فيعدّ العدّ أمام عين المستخدم
        WidgetUpdater.refreshAll(context)
    }

    companion object {
        /** بثّ داخلي: ضغطة تسبيح من الويدجت */
        const val ACTION_MISBAHA_TAP = "com.sawan4art.noor.widget.MISBAHA_TAP"

        /** بثّ داخلي: تصفير المسبحة من الويدجت */
        const val ACTION_MISBAHA_RESET = "com.sawan4art.noor.widget.MISBAHA_RESET"
    }
}

/** أدوات الدفع الجماعي — تحديث الويدجت من التطبيق والمنبهات */
object WidgetUpdater {

    private val PROVIDER = NoorWidget::class.java

    /** تحديث ويدجت التطبيق المنصوبة — يُستدعى من التطبيق والمنبهات */
    fun refreshAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, PROVIDER))
            if (ids.isEmpty()) return
            val snapshot = WidgetSnapshot.compute(context)
            val customization = WidgetStore.customization(context)
            manager.updateAppWidget(ids, WidgetRenderer.render(context, snapshot, customization))
        }
    }
}
