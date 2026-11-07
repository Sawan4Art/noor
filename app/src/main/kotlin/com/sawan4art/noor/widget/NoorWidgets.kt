package com.sawan4art.noor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.sawan4art.noor.R
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.theme.AccentPalette

/**
 * ويدجتات «نور المسلم» الخمسة — توجيه المالك ٢٠٢٦-١١-٠٧:
 * «هنعمل خمس اشكال مختلفه بنفس تصميم التطبيق و هتكون الويدجت دي
 * قابله للتخصيص والتعديل الكامل من قبل المستخدم».
 *
 * الأشكال الخمسة (كلٌّ استدلاؤه من قائمة ويدجتات النظام باسمه):
 * 1) الصلاة القادمة 4×2 — الاسم والوقت والعدّاد الحي.
 * 2) مواقيت اليوم 4×3 — الصلوات الست صفوفاً والقادمة مميزة.
 * 3) التاريخان 4×1 — الهجري والميلادي والمدينة.
 * 4) ذكر اليوم 2×2 — ذكر يدور كل يوم.
 * 5) الشريط الحي 4×1 — الاسم والعدّاد والوقت في سطر واحد.
 *
 * والتخصيص المشترك ([WidgetCustomization]) يقرأ عند كل رسم: المظهر
 * والشفافية ولون التطبيق وعناصر العرض — درس v0.4: JSON في
 * SharedPreferences والخلفية ألفافٍ مرسومة بأطراف دائرية.
 */

/** الأشكال الخمسة — بهوياتها ومقاساتها وأسماؤها للعرض */
enum class WidgetDesign(val title: String) {
    NEXT_PRAYER("الصلاة القادمة"),
    DAY_GRID("مواقيت اليوم"),
    DATES("التاريخان"),
    ZIKR("ذكر اليوم"),
    LIVE_BAR("الشريط الحي"),
}

/** ألوان الرسم — من التخصيص: مظهرٌ ومَيُ shown ولون التطبيق */
private data class WidgetPalette(
    val bgTop: Int,
    val bgBottom: Int,
    val text: Int,
    val muted: Int,
    val accent: Int,
    val accentSoft: Int,
)

private fun palette(c: WidgetCustomization): WidgetPalette {
    val accent = AccentPalette.byId(c.accentId)
    return when (c.theme) {
        WidgetCustomization.Theme.LIGHT -> WidgetPalette(
            bgTop = Color.parseColor("#F5F1E4"),
            bgBottom = Color.parseColor("#E8E2CE"),
            text = Color.parseColor("#233029"),
            muted = Color.parseColor("#5D6B62"),
            accent = accent.day.primary.toArgb(),
            accentSoft = accent.day.primaryContainer.toArgb(),
        )
        WidgetCustomization.Theme.DARK -> WidgetPalette(
            bgTop = Color.parseColor("#16110D"),
            bgBottom = Color.parseColor("#221B14"),
            text = Color.parseColor("#F2EDE0"),
            muted = Color.parseColor("#A99F8D"),
            accent = accent.night.primary.toArgb(),
            accentSoft = accent.night.primaryContainer.toArgb(),
        )
        WidgetCustomization.Theme.APP -> {
            // يتبع لون التطبيق نفسه — تدرّج بطاقة الهيرو وخفاها
            WidgetPalette(
                bgTop = accent.night.heroTop.toArgb(),
                bgBottom = accent.night.heroBottom.toArgb(),
                text = accent.night.heroContent.toArgb(),
                muted = accent.night.heroMuted.toArgb(),
                accent = accent.night.primary.toArgb(),
                accentSoft = accent.night.primaryContainer.toArgb(),
            )
        }
    }
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
 * الرسّام الموحد — يبني RemoteViews لأي شكلٍ بأي تخصيص.
 * العدّاد الحي: Chronometer تنازلي يعدّ ثانيةً بثانية بلا تحديثٍ جديد
 * (درس v0.4: عدّاد الويدجت الحي) وقاعدته «زمن الإقلاع + المتبقي».
 */
object WidgetRenderer {

    internal fun render(
        context: Context,
        design: WidgetDesign,
        snapshot: WidgetSnapshot,
        customization: WidgetCustomization,
        widthDp: Int = 250,
        heightDp: Int = 110,
    ): RemoteViews {
        val pal = palette(customization)
        val layout = when (design) {
            WidgetDesign.NEXT_PRAYER -> R.layout.widget_next_prayer
            WidgetDesign.DAY_GRID -> R.layout.widget_day_grid
            WidgetDesign.DATES -> R.layout.widget_dates
            WidgetDesign.ZIKR -> R.layout.widget_zikr
            WidgetDesign.LIVE_BAR -> R.layout.widget_live_bar
        }
        val views = RemoteViews(context.packageName, layout)

        // الخلفية — نرسمها بدقة معقولة ثابتة (الويدجت يمدّدها)
        val bg = widgetBackgroundBitmap(
            widthDp * 4, heightDp * 4,
            pal.bgTop, pal.bgBottom, customization.opacityFraction, pal.accent,
        )
        views.setImageViewBitmap(R.id.widget_bg, bg)

        // ألوان النصوص المشتركة
        val brandVisible = if (customization.showBrand) android.view.View.VISIBLE else android.view.View.GONE
        views.setViewVisibility(R.id.widget_brand, brandVisible)
        views.setTextColor(R.id.widget_brand, pal.text)
        views.setTextColor(R.id.widget_city, pal.muted)

        when (design) {
            WidgetDesign.NEXT_PRAYER -> {
                views.setTextViewText(R.id.widget_next_name, snapshot.nextName)
                views.setTextColor(R.id.widget_next_name, pal.text)
                views.setTextViewText(R.id.widget_next_time, snapshot.nextTimeText)
                views.setTextColor(R.id.widget_next_time, pal.accent)
                bindCountdown(views, snapshot, customization, pal, R.id.widget_countdown)
                views.setViewVisibility(
                    R.id.widget_city,
                    if (customization.showCity) android.view.View.VISIBLE else android.view.View.GONE,
                )
                views.setTextViewText(R.id.widget_city, snapshot.city)
            }
            WidgetDesign.DAY_GRID -> {
                bindCountdown(views, snapshot, customization, pal, R.id.widget_grid_countdown)
                snapshot.rows.forEachIndexed { i, row ->
                    val nameId = GRID_NAME_IDS[i]
                    val timeId = GRID_TIME_IDS[i]
                    views.setTextViewText(nameId, row.name)
                    views.setTextColor(nameId, if (row.isPast) pal.muted else pal.text)
                    views.setTextViewText(timeId, row.time)
                    views.setTextColor(
                        timeId,
                        if (row.isNext) pal.accent else if (row.isPast) pal.muted else pal.text,
                    )
                }
                views.setViewVisibility(
                    R.id.widget_city,
                    if (customization.showCity) android.view.View.VISIBLE else android.view.View.GONE,
                )
                views.setTextViewText(R.id.widget_city, snapshot.city)
            }
            WidgetDesign.DATES -> {
                val datesVisible =
                    if (customization.showDates) android.view.View.VISIBLE else android.view.View.GONE
                views.setViewVisibility(R.id.widget_date_hijri, datesVisible)
                views.setViewVisibility(R.id.widget_date_greg, datesVisible)
                views.setTextViewText(R.id.widget_date_hijri, snapshot.hijri)
                views.setTextColor(R.id.widget_date_hijri, pal.text)
                views.setTextViewText(R.id.widget_date_greg, snapshot.gregorian)
                views.setTextColor(R.id.widget_date_greg, pal.accent)
                views.setViewVisibility(
                    R.id.widget_city,
                    if (customization.showCity) android.view.View.VISIBLE else android.view.View.GONE,
                )
                views.setTextViewText(R.id.widget_city, snapshot.city)
            }
            WidgetDesign.ZIKR -> {
                views.setTextViewText(R.id.widget_zikr_text, snapshot.zikr)
                views.setTextColor(R.id.widget_zikr_text, pal.text)
            }
            WidgetDesign.LIVE_BAR -> {
                views.setTextViewText(R.id.widget_bar_name, snapshot.nextName)
                views.setTextColor(R.id.widget_bar_name, pal.text)
                views.setTextViewText(R.id.widget_bar_time, snapshot.nextTimeText)
                views.setTextColor(R.id.widget_bar_time, pal.accent)
                bindCountdown(views, snapshot, customization, pal, R.id.widget_bar_countdown)
            }
        }
        return views
    }

    /** العدّاد الحي — Chronometer تنازلي، وإن عُطّل من التخصيص اختفى */
    private fun bindCountdown(
        views: RemoteViews,
        snapshot: WidgetSnapshot,
        customization: WidgetCustomization,
        pal: WidgetPalette,
        id: Int,
    ) {
        if (!customization.showCountdown) {
            views.setViewVisibility(id, android.view.View.GONE)
            return
        }
        views.setViewVisibility(id, android.view.View.VISIBLE)
        views.setTextColor(id, pal.muted)
        views.setCharSequence(id, "setFormat", "متبقي %s حتى الأذان")
        views.setBoolean(id, "setCountDown", true)
        views.setLong(id, "setBase", chronometerBase(snapshot.remainingSeconds, SystemClock.elapsedRealtime()))
    }

    /** معرفات صفوف الشبكة — أسماء وأوقات الصلوات الست بالترتيب */
    internal val GRID_NAME_IDS = intArrayOf(
        R.id.widget_pname_0, R.id.widget_pname_1, R.id.widget_pname_2,
        R.id.widget_pname_3, R.id.widget_pname_4, R.id.widget_pname_5,
    )
    internal val GRID_TIME_IDS = intArrayOf(
        R.id.widget_ptime_0, R.id.widget_ptime_1, R.id.widget_ptime_2,
        R.id.widget_ptime_3, R.id.widget_ptime_4, R.id.widget_ptime_5,
    )

    /** أرقام هندية للعرض داخل الويدجت */
    internal fun arabic(n: Int): String = MisbahaMath.arabicNumber(n.toLong())
}

/**
 * الأساس المشترك للويدجتات — كل تحديث يعيد الحساب والرسم من الإعدادات
 * الحية، فأي تخصيصٍ أو تغيير موقَعٍ ينعكس عند أقرب نبضة.
 */
abstract class NoorWidgetBase : AppWidgetProvider() {

    /** شكل هذا الويدجت */
    abstract val design: WidgetDesign

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = WidgetSnapshot.compute(context)
        val customization = WidgetStore.customization(context)
        for (id in appWidgetIds) {
            manager.updateAppWidget(id, WidgetRenderer.render(context, design, snapshot, customization))
        }
    }
}

/** 1) الصلاة القادمة 4×2 */
class NextPrayerWidget : NoorWidgetBase() {
    override val design = WidgetDesign.NEXT_PRAYER
}

/** 2) مواقيت اليوم 4×3 */
class DayGridWidget : NoorWidgetBase() {
    override val design = WidgetDesign.DAY_GRID
}

/** 3) التاريخان 4×1 */
class DatesWidget : NoorWidgetBase() {
    override val design = WidgetDesign.DATES
}

/** 4) ذكر اليوم 2×2 */
class ZikrWidget : NoorWidgetBase() {
    override val design = WidgetDesign.ZIKR
}

/** 5) الشريط الحي 4×1 */
class LiveBarWidget : NoorWidgetBase() {
    override val design = WidgetDesign.LIVE_BAR
}

/** أدوات الدفع الجماعي — تحديث الويدجتات الخمسة كلها من التطبيق */
object WidgetUpdater {

    private val PROVIDERS = listOf(
        NextPrayerWidget::class.java,
        DayGridWidget::class.java,
        DatesWidget::class.java,
        ZikrWidget::class.java,
        LiveBarWidget::class.java,
    )

    /** تحديث كل ويدجتات التطبيق المنصوبة — يُستدعى من التطبيق والمنبهات */
    fun refreshAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val snapshot = WidgetSnapshot.compute(context)
            val customization = WidgetStore.customization(context)
            for (cls in PROVIDERS) {
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isEmpty()) continue
                val design = when (cls) {
                    NextPrayerWidget::class.java -> WidgetDesign.NEXT_PRAYER
                    DayGridWidget::class.java -> WidgetDesign.DAY_GRID
                    DatesWidget::class.java -> WidgetDesign.DATES
                    ZikrWidget::class.java -> WidgetDesign.ZIKR
                    else -> WidgetDesign.LIVE_BAR
                }
                val views = WidgetRenderer.render(context, design, snapshot, customization)
                manager.updateAppWidget(ids, views)
            }
        }
    }
}
