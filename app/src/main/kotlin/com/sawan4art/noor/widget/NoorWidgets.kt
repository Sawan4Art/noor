package com.sawan4art.noor.widget

import android.app.AlarmManager
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
import java.time.LocalTime

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
internal data class WidgetPalette(
    val bgTop: Int,
    val bgBottom: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
)

internal fun palette(c: WidgetCustomization): WidgetPalette {
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
 *
 * alpha59 — بلاغ المالك: «عايز خانات لتحديد حجم الخط ولونه لكل عنصر»
 * — فكل عنصرٍ يمرّ على [sizeOf]/[colorOf]: الحجم نسبةٌ مئويةٍ من
 * قاعدته (الغياب = ١٠٠) واللون إما تلقائي من المظهر أو صريح.
 *
 * **alpha61 — المحرر التفاعلي** (بلاغ المالك: «عايز شكل الويدجت يظهر
 * في اعدادت الويدجت داخل التطبيق و يكون قابل لتحريك كل العناصر و
 * تكبيرها من خلال الزوم باليد… و لوحه الالوان تكون منبثقه»): كل عنصرٍ
 * صار RemoteViews مستقلاً (widget_el_*) يُضاف إلى عُلبة widget_stack
 * **بترتيب المستخدم** [WidgetCustomization.elementOrder] — فالتحريك
 * ترتيبٌ حيّ في المحرر، والحجم زومٌ باليد، واللون لوحةٌ منبثقة —
 * والمخفي لا يُضاف أصلاً فلا فراغ مكانه. وأي غلطةٍ في عنصرٍ واحدٍ
 * لا تقتل البقية: كل عنصرٍ يُبنى بأمانه (درس alpha58).
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

        // نسبة الحجم واللون لكل عنصر — «خانات لتحديد حجم الخط ولونه لكل عنصر»
        fun sizeOf(key: String, base: Float): Float =
            base * (customization.elementSizes[key] ?: 100) / 100f

        fun colorOf(key: String, fallback: Int): Int =
            customization.elementColors[key]
                ?.let { WidgetCustomization.parseColor(it) } ?: fallback

        fun show(child: RemoteViews, id: Int, visible: Boolean) =
            child.setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)

        // الخلفية — دقةٌ معتدلة — درس alpha58: مجموع بايتات الصور داخل
        // باكِت RemoteViews لا يجاوز مخزن البايندر (~١MB) وإلا فشل
        // التحديث وبدت الويدجت «تظهر لكن لا تعمل».
        val bg = widgetBackgroundBitmap(
            (widthDp * 3 / 2).coerceAtMost(360),
            (heightDp * 3 / 2).coerceAtMost(260),
            pal.bgTop, pal.bgBottom, customization.opacityFraction, pal.accent,
        )
        views.setImageViewBitmap(R.id.widget_bg, bg)

        val analog = customization.clockStyle == WidgetCustomization.ClockStyle.ANALOG
        var slot = 0

        /** إضافة عنصرٍ إلى العمود بترتيب المستخدم — المخفي لا يُضاف أصلاً */
        fun addElement(key: String, visible: Boolean, build: (RemoteViews) -> Unit) {
            if (!visible) return
            val child = runCatching {
                RemoteViews(context.packageName, elementLayout(key)).also(build)
            }.getOrNull() ?: return
            runCatching {
                // الإضافة بالتتابع تكفي: addView يلحق بآخر العمود
                // فترتيب الاستدعاء هو الترتيب المختار
                views.addView(R.id.widget_stack, child)
                slot++
            }
        }

        for (key in WidgetCustomization.normalizeOrder(customization.elementOrder)) {
            when (key) {
                WidgetCustomization.ELEMENT_HEADER -> addElement(
                    key,
                    visible = customization.showBrand || (customization.showCity && snapshot.city.isNotBlank()),
                ) { child ->
                    show(child, R.id.widget_brand, customization.showBrand)
                    child.setTextColor(R.id.widget_brand, colorOf(key, pal.text))
                    child.setFloat(R.id.widget_brand, "setTextSize", sizeOf(key, 11f))
                    val cityVisible = customization.showCity && snapshot.city.isNotBlank()
                    show(child, R.id.widget_city, cityVisible)
                    if (cityVisible) {
                        child.setTextViewText(R.id.widget_city, snapshot.city)
                        child.setTextColor(R.id.widget_city, colorOf(key, pal.muted))
                        child.setFloat(R.id.widget_city, "setTextSize", sizeOf(key, 10f))
                    }
                }

                WidgetCustomization.ELEMENT_CLOCK -> addElement(
                    key,
                    visible = customization.showClock,
                ) { child ->
                    show(child, R.id.widget_clock, !analog)
                    child.setCharSequence(R.id.widget_clock, "setFormat12Hour", "h:mm a")
                    child.setCharSequence(R.id.widget_clock, "setFormat24Hour", "HH:mm")
                    child.setFloat(R.id.widget_clock, "setTextSize", sizeOf(key, 15f))
                    child.setTextColor(R.id.widget_clock, colorOf(key, pal.text))
                    if (analog) {
                        // الميناء بحجم الزوم نفسه — wrap_content يقيسه لصورته
                        val scale = (customization.elementSizes[key] ?: 100) / 100f
                        child.setViewVisibility(R.id.widget_clock_analog, View.VISIBLE)
                        child.setImageViewBitmap(
                            R.id.widget_clock_analog,
                            WidgetArt.analogClockBitmap(
                                (WidgetArt.ANALOG_SIZE * scale).toInt().coerceIn(48, 240),
                                LocalTime.now().hour, LocalTime.now().minute,
                                dialColor = pal.text,
                                handColor = colorOf(key, pal.text),
                                accentColor = pal.accent,
                                onDialColor = pal.text,
                            ),
                        )
                    } else {
                        child.setViewVisibility(R.id.widget_clock_analog, View.GONE)
                    }
                }

                WidgetCustomization.ELEMENT_HIJRI -> addElement(
                    key,
                    visible = customization.showHijri,
                ) { child ->
                    child.setTextViewText(
                        R.id.widget_date_hijri,
                        Hijri.formatHijri(snapshot.hijriDate, customization.hijriFormat, snapshot.gregorianDate),
                    )
                    child.setFloat(R.id.widget_date_hijri, "setTextSize", sizeOf(key, 11f))
                    child.setTextColor(R.id.widget_date_hijri, colorOf(key, pal.text))
                }

                WidgetCustomization.ELEMENT_GREG -> addElement(
                    key,
                    visible = customization.showGregorian,
                ) { child ->
                    child.setTextViewText(
                        R.id.widget_date_greg,
                        Hijri.formatGregorian(snapshot.gregorianDate, customization.gregFormat),
                    )
                    child.setFloat(R.id.widget_date_greg, "setTextSize", sizeOf(key, 11f))
                    child.setTextColor(R.id.widget_date_greg, colorOf(key, pal.accent))
                }

                WidgetCustomization.ELEMENT_NEXT -> addElement(
                    key,
                    visible = true,
                ) { child ->
                    child.setTextViewText(R.id.widget_next_name, snapshot.nextName)
                    child.setFloat(R.id.widget_next_name, "setTextSize", sizeOf(key, 20f))
                    child.setTextColor(R.id.widget_next_name, colorOf(key, pal.text))
                    child.setTextViewText(R.id.widget_next_time, snapshot.nextTimeText)
                    child.setFloat(R.id.widget_next_time, "setTextSize", sizeOf(key, 14f))
                    child.setTextColor(R.id.widget_next_time, colorOf(key, pal.accent))
                }

                WidgetCustomization.ELEMENT_COUNTDOWN -> addElement(
                    key,
                    visible = customization.showCountdown,
                ) { child ->
                    // الجذر المُصلَح (alpha58): setChronometer(…, started=true)
                    // هي التي تُشغّل العدّاد — وصيغته التنازلية من التخطيط
                    child.setChronometer(
                        R.id.widget_countdown,
                        chronometerBase(snapshot.remainingSeconds, SystemClock.elapsedRealtime()),
                        COUNTDOWN_FORMAT,
                        true,
                    )
                    child.setFloat(R.id.widget_countdown, "setTextSize", sizeOf(key, 12f))
                    child.setTextColor(R.id.widget_countdown, colorOf(key, pal.muted))
                    bindBubble(
                        child, R.id.widget_bubble_countdown,
                        customization.bubbleOnCountdown,
                        customization.bubbleColorCountdown, pal,
                        WidgetArt.COUNTDOWN_BUBBLE_W, WidgetArt.COUNTDOWN_BUBBLE_H, 0f,
                    )
                }

                WidgetCustomization.ELEMENT_GRID -> addElement(
                    key,
                    visible = customization.showPrayerTimes,
                ) { child ->
                    val currentIndex = WidgetSnapshot.currentRowIndex(snapshot.rows)
                    snapshot.rows.forEachIndexed { i, row ->
                        val nameId = GRID_NAME_IDS[i]
                        val timeId = GRID_TIME_IDS[i]
                        // الاسم الصغير فوق الوقت — بلاغ المالك alpha59
                        child.setTextViewText(nameId, row.name)
                        child.setFloat(nameId, "setTextSize", sizeOf(key, 10f))
                        child.setTextColor(
                            nameId,
                            if (row.isPast && i != currentIndex) pal.muted else colorOf(key, pal.text),
                        )
                        child.setTextViewText(timeId, row.time)
                        child.setFloat(timeId, "setTextSize", sizeOf(key, 10f))
                        child.setTextColor(
                            timeId,
                            when {
                                row.isNext -> colorOf(key, pal.accent)
                                row.isPast && i != currentIndex -> pal.muted
                                else -> colorOf(key, pal.text)
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
                        bindBubble(
                            child, bubbleId, lit, bubbleColor, pal,
                            WidgetArt.GRID_BUBBLE_W, WidgetArt.GRID_BUBBLE_H, i * 0.9f,
                        )
                    }
                }

                WidgetCustomization.ELEMENT_MISBAHA -> addElement(
                    key,
                    visible = customization.showMisbaha,
                ) { child ->
                    val count = WidgetStore.misbahaCount(context)
                    child.setTextViewText(
                        R.id.widget_misbaha_count,
                        "المسبحة: ${MushafSurahs.arabicDigits(count)}",
                    )
                    child.setFloat(R.id.widget_misbaha_count, "setTextSize", sizeOf(key, 14f))
                    child.setTextColor(R.id.widget_misbaha_count, colorOf(key, pal.text))
                    child.setFloat(R.id.widget_misbaha_tap, "setTextSize", sizeOf(key, 12f))
                    child.setTextColor(R.id.widget_misbaha_tap, colorOf(key, pal.accent))
                    child.setFloat(R.id.widget_misbaha_reset, "setTextSize", sizeOf(key, 11f))
                    child.setTextColor(R.id.widget_misbaha_reset, colorOf(key, pal.muted))
                    child.setOnClickPendingIntent(
                        R.id.widget_misbaha_tap,
                        misbahaPendingIntent(context, NoorWidget.ACTION_MISBAHA_TAP),
                    )
                    child.setOnClickPendingIntent(
                        R.id.widget_misbaha_reset,
                        misbahaPendingIntent(context, NoorWidget.ACTION_MISBAHA_RESET),
                    )
                }
            }
        }
        return views
    }

    /** تخطيط عنصر الويدجت بمفتاحه — والغريب يعود للترويسة بأمان */
    internal fun elementLayout(key: String): Int = when (key) {
        WidgetCustomization.ELEMENT_HEADER -> R.layout.widget_el_header
        WidgetCustomization.ELEMENT_CLOCK -> R.layout.widget_el_clock
        WidgetCustomization.ELEMENT_HIJRI -> R.layout.widget_el_hijri
        WidgetCustomization.ELEMENT_GREG -> R.layout.widget_el_greg
        WidgetCustomization.ELEMENT_NEXT -> R.layout.widget_el_next
        WidgetCustomization.ELEMENT_COUNTDOWN -> R.layout.widget_el_countdown
        WidgetCustomization.ELEMENT_GRID -> R.layout.widget_el_grid
        WidgetCustomization.ELEMENT_MISBAHA -> R.layout.widget_el_misbaha
        else -> R.layout.widget_el_header
    }

    /**
     * فقاعةٌ سائلةٌ حيّةٌ خلف مقامٍ — بلاغ المالك alpha59: «يظهر كنقطه
     * ماء مخالفه تماماً للسائل الحي الموجود في التطبيق فهذا ما اريده و
     * ليس مجرد نقطه» — فبدل المتّجه النقطيّ القديم تُرسم لكل مقامٍ
     * صورةُ قطرةٍ متموجةٍ بلونها وطورٍ مختلفٍ كأنها سائلٌ التطبيق نفسه
     * وقد وقف في مقامه.
     */
    private fun bindBubble(
        views: RemoteViews,
        id: Int,
        visible: Boolean,
        colorRaw: String,
        pal: WidgetPalette,
        widthPx: Int,
        heightPx: Int,
        phase: Float,
    ) {
        if (visible) {
            val color = WidgetCustomization.parseColor(colorRaw) ?: pal.accent
            views.setViewVisibility(id, View.VISIBLE)
            views.setImageViewBitmap(id, WidgetArt.liquidBubbleBitmap(widthPx, heightPx, color, phase))
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
 * الرسم فوراً. ومع الساعة التناظرية (alpha59) بثّ نبضةٍ كل دقيقةٍ
 * يعيد رسم العقارب ما دامت ظاهرة — ويُلغى ما إن تعود رقمية.
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
        // نبضة الدقيقة للساعة التناظرية فقط — الرقمية حيّةٌ وحدها
        WidgetUpdater.updateMinuteTick(context, customization)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MISBAHA_TAP -> WidgetStore.misbahaTap(context)
            ACTION_MISBAHA_RESET -> WidgetStore.misbahaReset(context)
            ACTION_TICK -> {
                // عقارب الساعة التناظرية — إعادة رسمٍ ثم دقّة الدقيقة التالية
                WidgetUpdater.refreshAll(context)
                WidgetUpdater.updateMinuteTick(context, WidgetStore.customization(context))
                return
            }
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

        /** بثّ داخلي: نبضة الدقيقة للساعة التناظرية (alpha59) */
        const val ACTION_TICK = "com.sawan4art.noor.widget.CLOCK_TICK"
    }
}

/** أدوات الدفع الجماعي — تحديث الويدجت من التطبيق والمنبهات */
object WidgetUpdater {

    private val PROVIDER = NoorWidget::class.java
    private const val TICK_REQUEST_CODE = 2001

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

    /**
     * نبضة الدقيقة — عقارب الساعة التناظرية لا تتحرك وحدها في RemoteViews
     * (بلاغ alpha59: «ساعه تناظريه») فمنبهٌ رقيقٌ كل دقيقةٍ يعيد الرسم
     * ما دامت الساعة التناظرية مختارةً والويدجت منصوبة، ويُلغى بعودة
     * الرقمية — فلا بطارية تُهدر إلا لعقاربٍ تُرى.
     */
    fun updateMinuteTick(context: Context, customization: WidgetCustomization) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val hasWidget = manager != null &&
                manager.getAppWidgetIds(ComponentName(context, PROVIDER)).isNotEmpty()
            val wanted = hasWidget && customization.showClock &&
                customization.clockStyle == WidgetCustomization.ClockStyle.ANALOG
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return
            val pi = tickPendingIntent(context)
            am.cancel(pi)
            if (wanted) {
                // بداية الدقيقة التالية — والعقرب يقفز في موعدها
                val nextMinute = (System.currentTimeMillis() / 60_000L + 1L) * 60_000L
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMinute, pi)
            }
        }
    }

    private fun tickPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            TICK_REQUEST_CODE,
            Intent(context, PROVIDER).setAction(NoorWidget.ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
