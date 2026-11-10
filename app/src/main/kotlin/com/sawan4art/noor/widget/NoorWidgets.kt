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

/** خلفية الويدجت — تدرّج بشفافية التخصيص وإطارٌ بلون التخصيص أو بلا
 *  إطارٍ خالصاً — **الإطار مربّعٌ بحوافٍ دائرية** (بلاغ المالك
 *  2026-11-11: «و يكون اطار مربع بحواف دائريه») فيُرسم حدّه بقوسٍ
 *  دائريٍ فوق التدرّج الحادّ (حواف الويدجت نفسها بقيت حادة — alpha63)
 *  والإطار مرسومٌ في الخلفية نفسها كي لا يزيد بايتان في باكِت
 *  البايندر (درس alpha58)، و[frameColor] صفرٌ يعني بلا إطار. */
internal fun widgetBackgroundBitmap(
    width: Int,
    height: Int,
    top: Int,
    bottom: Int,
    alpha: Float,
    frameColor: Int?,
): Bitmap {
    val w = width.coerceIn(1, 2048)
    val h = height.coerceIn(1, 2048)
    val drawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(top, bottom),
    ).apply {
        // الحواف حادة بلا دائرية — بلاغ المالك alpha63
        cornerRadius = 0f
    }
    drawable.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    if (frameColor != null) {
        // الإطار مربّع بحوافٍ دائرية — حدٌّ مستقلٌ فوق التدرّج لا يمسّه
        val stroke = (3f * w / 250f).toInt().coerceAtLeast(2)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = stroke.toFloat()
            color = withAlpha(frameColor, 0.85f)
        }
        val radius = w * 0.06f
        val inset = stroke / 2f
        canvas.drawRoundRect(
            inset, inset, w - inset, h - inset,
            radius, radius, paint,
        )
    }
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
 * **إطار كل عنصرٍ على حدة** (بلاغ المالك 2026-11-11: «عايز اطار
 * للويدجت ولكل عنصر من الويدجت يكون اختيار اما اضافه الاطار بلون
 * معين او ازاله الاطار … اطار لكل عنصر على حده و اطار للويدجت كله»):
 * كل عنصرٍ له خلفيةٌ جاهزة (el_frame_1..8) بحدٍّ مربّعٍ بحوافٍ دائريةٍ
 * بلونٍ من لوحة الويدجت الثمانية — تُضبط ب setBackgroundResource على
 * جذر العنصر، وغياب الإطار يعني بلا خلفيةٍ أصلاً.
 */
object WidgetRenderer {

    /** إطارات العناصر الثمانية — لون اللوحة إلى الرسم الجاهز بحوافٍ دائرية */
    internal val ELEMENT_FRAME_DRAWABLES: Map<String, Int> = mapOf(
        "#233029" to R.drawable.el_frame_1,
        "#F2EDE0" to R.drawable.el_frame_2,
        "#2FA48C" to R.drawable.el_frame_3,
        "#C98A3D" to R.drawable.el_frame_4,
        "#B04A3A" to R.drawable.el_frame_5,
        "#5B6E9E" to R.drawable.el_frame_6,
        "#7A5B9E" to R.drawable.el_frame_7,
        "#4E7A4E" to R.drawable.el_frame_8,
    )

    /** جذر العنصر الذي يُضبط عليه الإطار — بمفتاحه */
    internal fun elementRootId(key: String): Int = when (key) {
        WidgetCustomization.ELEMENT_HIJRI -> R.id.widget_date_hijri
        WidgetCustomization.ELEMENT_GREG -> R.id.widget_date_greg
        WidgetCustomization.ELEMENT_NEXT -> R.id.widget_next_row
        WidgetCustomization.ELEMENT_COUNTDOWN -> R.id.widget_countdown_frame
        WidgetCustomization.ELEMENT_GRID -> R.id.grid_row
        else -> R.id.widget_el_root
    }

    /** رسم إطار عنصرٍ بلونه — «auto»/«none»/الغريب = بلا إطار */
    internal fun elementFrameRes(frameValue: String?): Int? {
        if (frameValue == null) return null
        if (frameValue == WidgetCustomization.COLOR_AUTO) return null
        if (frameValue == WidgetCustomization.FRAME_NONE) return null
        return ELEMENT_FRAME_DRAWABLES[frameValue.uppercase()]
            ?: ELEMENT_FRAME_DRAWABLES[frameValue]
    }

    internal fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        customization: WidgetCustomization,
        widthDp: Int = 250,
        heightDp: Int = 180,
        appWidgetId: Int = -1,
    ): RemoteViews {
        val pal = palette(customization)
        // **شجرة جديدة كل تحديث** (alpha64): التبادل بين التوأمين
        // widget_main/widget_main_alt يقلّب معرف التخطيط فيقلّب قرار
        // الهوست بين النفخ الكامل وإعادة التطبيق — والنفخ الكامل هو
        // المطلوب: كل تحديث شجرةٌ من أوله لا شجرةٌ قديمة تتراكم عليها
        // أفعالٌ جديدة (جذر بلاغ التكرار alpha63 وخلط الشكل بعده).
        val views = RemoteViews(context.packageName, mainLayoutForUpdate(System.currentTimeMillis()))

        // **جذر تكرار العناصر** (alpha63 — بلاغ المالك: «بعد ما بحط الويدجت
        // في الشاشه لو ضغط عليه الويدجت بيكرر نفسه بشكل غريب» — والصورة
        // تُري المحتوى أربع مراتٍ فوق بعض): التخطيط نفسه يُعاد تطبيقه على
        // العرض الحي في كل تحديثٍ فتنعقد أفعال addView فوق أبناءٍ موجودين
        // فيتضاعف العمود. تفريغ العُلبة قبل البناء يُنهي التكرار من جذره
        // — كل تحديثٍ عمودٌ نظيفٌ من أوله لا فوق قديمٍ له.
        views.removeAllViews(R.id.widget_stack)

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
        // التحديث وبدت الويدجت «تظهر لكن لا تعمل». والإطار الدائري
        // (alpha62) مرسومٌ في هذه الخلفية نفسها بلون التخصيص.
        // الإطار بلونٍ مختارٍ أو بلا إطارٍ خالصاً (alpha63) — والتلقائي
        // يتبع لون التطبيق كما كان
        val frameResolved = if (customization.frameColor == WidgetCustomization.FRAME_NONE) {
            null
        } else {
            WidgetCustomization.parseColor(customization.frameColor) ?: pal.accent
        }
        val bg = widgetBackgroundBitmap(
            (widthDp * 3 / 2).coerceAtMost(360),
            (heightDp * 3 / 2).coerceAtMost(260),
            pal.bgTop, pal.bgBottom, customization.opacityFraction, frameResolved,
        )
        views.setImageViewBitmap(R.id.widget_bg, bg)
        // ضغطة الويدجت تفتح إعداداتها داخل التطبيق (alpha63) — بلاغ
        // المالك: «عايز لما اضغط عليه يفتح اعادات الويدجت داخل التطبيق»
        views.setOnClickPendingIntent(
            R.id.widget_root,
            settingsPendingIntent(context, appWidgetId),
        )

        val analog = customization.clockStyle == WidgetCustomization.ClockStyle.ANALOG
        var slot = 0

        /** إضافة عنصرٍ إلى العمود بترتيب المستخدم — المخفي لا يُضاف أصلاً */
        fun addElement(key: String, visible: Boolean, build: (RemoteViews) -> Unit) {
            if (!visible) return
            val child = runCatching {
                RemoteViews(context.packageName, elementLayout(key)).also(build)
            }.getOrNull() ?: return
            // إطار العنصر — حدٌّ مربّعٌ بحوافٍ دائريةٍ بلونٍ مختارٍ أو بلا
            runCatching {
                elementFrameRes(customization.elementFrames[key])?.let { res ->
                    child.setInt(elementRootId(key), "setBackgroundResource", res)
                }
            }
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
                        // (alpha63: الساعة التناظرية أكبر — ١٥٠ أساساً)
                        val scale = (customization.elementSizes[key] ?: 100) / 100f
                        child.setViewVisibility(R.id.widget_clock_analog, View.VISIBLE)
                        child.setImageViewBitmap(
                            R.id.widget_clock_analog,
                            WidgetArt.analogClockBitmap(
                                (WidgetArt.ANALOG_SIZE * scale).toInt().coerceIn(64, 260),
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
                    // الجملة كاملةً TextView عاديّاً (alpha64) — نفس منادى
                    // setText الذي يظهر عند المالك في التواريخ والمدينة
                    // والمواقيت، فلا يعتمد المعنى على صيغة Chronometer
                    // التي سقطت ميدانياً فظهرت أرقامٌ عارية بلا كلمات
                    child.setTextViewText(
                        R.id.widget_countdown_label,
                        countdownLabel(snapshot.nextName),
                    )
                    child.setFloat(R.id.widget_countdown_label, "setTextSize", sizeOf(key, 12f))
                    child.setTextColor(R.id.widget_countdown_label, colorOf(key, pal.muted))
                    // الجذر المُصلَح (alpha58): setChronometer(…, started=true)
                    // هي التي تُشغّل العدّاد — والصيغة أرقامٌ خالصة (alpha64)
                    child.setChronometer(
                        R.id.widget_countdown,
                        chronometerBase(snapshot.remainingSeconds, SystemClock.elapsedRealtime()),
                        COUNTDOWN_FORMAT,
                        true,
                    )
                    child.setFloat(R.id.widget_countdown, "setTextSize", sizeOf(key, 12f))
                    child.setTextColor(R.id.widget_countdown, colorOf(key, pal.muted))
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
                    }
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
        else -> R.layout.widget_el_header
    }

    /**
     * تخطيط الحاضنة لهذا التحديث — التبادل بين التوأمين كل دقيقة:
     * تحديثان متتاليان في دقيقتين مختلفتين يأخذان معرفين مختلفين فيُنفخ
     * الهوست الشجرة من أوله (alpha64). القرار نقي على الزمن ([useAltMainLayout])
     * كي يُختبر بلا R، والخريطة هنا وحدةُ الربط بالموارد.
     */
    internal fun mainLayoutForUpdate(nowMillis: Long): Int =
        if (useAltMainLayout(nowMillis)) R.layout.widget_main_alt else R.layout.widget_main

    /** هل يأخذ هذا التحديث التوأم البديل؟ — دقيقة زوجية أصليّة وفرديّة بديلة */
    internal fun useAltMainLayout(nowMillis: Long): Boolean =
        nowMillis / 60_000L % 2L != 0L

    /**
     * نية ضغطة الويدجت — فتح قسم «الويدجت» في إعدادات التطبيق
     * (alpha63 — بلاغ المالك: «عايز لما اضغط عليه يفتح اعادات
     * الويدجت داخل التطبيق») ومعرّف الويدجت نفسها كي يحرّر المحرر
     * تخصيصَها هو لا العام (الأشكال الستة: بلاغ المالك 2026-11-11).
     */
    private fun settingsPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, com.sawan4art.noor.MainActivity::class.java).apply {
            action = NoorWidget.ACTION_OPEN_WIDGET_SETTINGS
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (appWidgetId > 0) {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        }
        return PendingIntent.getActivity(
            context,
            3 + (if (appWidgetId > 0) appWidgetId else 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** معرفات صفوف المواقيت — أسماء وأوقات الصلوات الخمس بالترتيب */
    internal val GRID_NAME_IDS = intArrayOf(
        R.id.widget_pname_0, R.id.widget_pname_1, R.id.widget_pname_2,
        R.id.widget_pname_3, R.id.widget_pname_4,
    )
    internal val GRID_TIME_IDS = intArrayOf(
        R.id.widget_ptime_0, R.id.widget_ptime_1, R.id.widget_ptime_2,
        R.id.widget_ptime_3, R.id.widget_ptime_4,
    )
}

/**
 * ويدجت «نور المسلم» — مستقبِل الشكل الافتراضي وجذرُ الأشكال الستة
 * (بلاغ المالك 2026-11-11: «٦ اشكال للويدجت تكون جاهزة… و اشكالهم
 * ظاهره في قائمه التطبيقات المصغره في الهاتف علشان المستخدم يقدر
 * يفرق بينهم»). تحديثها يعيد الحساب والرسم من تخصيص **هذه الويدجت**
 * (لكل ويدجتٍ مخزنٌ خاص بمعرّفها — درس الأشكال الستة) وضغطةٌ عليها
 * تفتح قسم إعداداتها داخل التطبيق (alpha63). ومع الساعة التناظرية
 * بثّ نبضةٍ كل دقيقةٍ يعيد رسم العقارب ما دامت ظاهرةً — ونبضةٌ يوم
 * منتصف الليل تعيد الأسماء والتواريخ.
 */
open class NoorWidget : AppWidgetProvider() {

    /** رقم الشكل الجاهز لهذا المستقبِل — null للشكل الافتراضي */
    protected open val presetIndex: Int? = null

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // زرع الشكل الجاهز في أول رسمٍ للويدجت — مرةً واحدةً فلا يمسّ
        // تخصيص المستخدم اليدوي بعدها
        presetIndex?.let { idx ->
            val preset = WidgetPresets.byIndex(idx)
            for (id in appWidgetIds) {
                WidgetStore.seedPresetIfAbsent(context, id, preset.customization)
            }
        }
        // التحصين (alpha58): أي غلطةٍ في الحساب أو الرسم لا تقتل التحديث
        // كله — كل ويدجت تُرسم بأمانها: تخصيصها هو فإن انفجر الافتراضي
        val snapshot = WidgetSnapshot.compute(context)
        for (id in appWidgetIds) {
            val customization = WidgetStore.customizationFor(context, id)
            val views = runCatching {
                WidgetRenderer.render(context, snapshot, customization, appWidgetId = id)
            }.getOrElse {
                runCatching {
                    WidgetRenderer.render(
                        context, WidgetSnapshot.compute(context), WidgetCustomization.DEFAULT,
                        appWidgetId = id,
                    )
                }.getOrNull()
            }
            if (views != null) manager.updateAppWidget(id, views)
        }
        // نبضتا الحياة: دقيقة العقارب ومنتصف الليل (الأسماء والتواريخ)
        WidgetUpdater.updateMinuteTick(context, WidgetStore.customization(context))
        WidgetUpdater.updateDayTick(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // مخزن الويدجت المحذوفة يُمسح كي لا يتراكم الموتى
        for (id in appWidgetIds) WidgetStore.clearFor(context, id)
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TICK -> {
                // عقارب الساعة التناظرية — إعادة رسمٍ ثم دقّة الدقيقة التالية
                WidgetUpdater.refreshAll(context)
                WidgetUpdater.updateMinuteTick(context, WidgetStore.customization(context))
                return
            }
            ACTION_DAY_TICK -> {
                // انتقل اليوم — الأسماء (الظهر يوم الجمعة) والتاريخان يُعاد حسابهم
                WidgetUpdater.refreshAll(context)
                WidgetUpdater.updateMinuteTick(context, WidgetStore.customization(context))
                WidgetUpdater.updateDayTick(context)
                return
            }
            else -> return
        }
        // نبضة الدقيقة أعادت الرسم — لا شيء بعدها
    }

    companion object {
        /** نية فتح قسم إعدادات الويدجت من ضغطة الويدجت على الشاشة (alpha63) */
        const val ACTION_OPEN_WIDGET_SETTINGS = "com.sawan4art.noor.widget.OPEN_SETTINGS"

        /** بثّ داخلي: نبضة الدقيقة للساعة التناظرية (alpha59) */
        const val ACTION_TICK = "com.sawan4art.noor.widget.CLOCK_TICK"

        /** بثّ داخلي: نبضة منتصف الليل — تاريخٌ جديد وأسماء يومٍ جديد (alpha62) */
        const val ACTION_DAY_TICK = "com.sawan4art.noor.widget.DAY_TICK"
    }
}

// ===== الأشكال الستة في قائمة ويدجتات النظام =====
// كلٌّ مستقبِلٌ مستقلٌ بمعاينته واسمه — أول رسمٍ يزرع تخصيص شكله
// (بلاغ المالك 2026-11-11: «عايز ٦ اشكال للويدجت تكون جاهزة…»)

class NoorWidgetPreset1 : NoorWidget() { override val presetIndex = 1 }
class NoorWidgetPreset2 : NoorWidget() { override val presetIndex = 2 }
class NoorWidgetPreset3 : NoorWidget() { override val presetIndex = 3 }
class NoorWidgetPreset4 : NoorWidget() { override val presetIndex = 4 }
class NoorWidgetPreset5 : NoorWidget() { override val presetIndex = 5 }
class NoorWidgetPreset6 : NoorWidget() { override val presetIndex = 6 }

/** أدوات الدفع الجماعي — تحديث الويدجت من التطبيق والمنبهات */
object WidgetUpdater {

    /** كل مستقبِلات الويدجت — الافتراضي والأشكال الستة */
    val PROVIDERS: List<Class<out AppWidgetProvider>> = listOf(
        NoorWidget::class.java,
        NoorWidgetPreset1::class.java, NoorWidgetPreset2::class.java,
        NoorWidgetPreset3::class.java, NoorWidgetPreset4::class.java,
        NoorWidgetPreset5::class.java, NoorWidgetPreset6::class.java,
    )
    private const val TICK_REQUEST_CODE = 2001
    private const val DAY_REQUEST_CODE = 2002

    /** هل هناك أي ويدجتٍ منصوبةٍ من أي شكل؟ */
    fun hasAnyWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return PROVIDERS.any { provider ->
            manager.getAppWidgetIds(ComponentName(context, provider)).isNotEmpty()
        }
    }

    /**
     * تحديث كل ويدجتات التطبيق المنصوبة — كلٌّ بتخصيصها الخاص بمعرّفها
     * (الأشكال الستة: بلاغ المالك 2026-11-11) — يُستدعى من التطبيق والمنبهات
     */
    fun refreshAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val snapshot = WidgetSnapshot.compute(context)
            for (provider in PROVIDERS) {
                val ids = manager.getAppWidgetIds(ComponentName(context, provider))
                for (id in ids) {
                    val customization = WidgetStore.customizationFor(context, id)
                    runCatching {
                        manager.updateAppWidget(
                            id,
                            WidgetRenderer.render(context, snapshot, customization, appWidgetId = id),
                        )
                    }
                }
            }
        }
    }

    /**
     * نبضة الدقيقة — عقارب الساعة التناظرية لا تتحرك وحدها في RemoteViews
     * (بلاغ alpha59: «ساعه تناظريه») فمنبهٌ رقيقٌ كل دقيقةٍ يعيد الرسم
     * ما دامت الساعة التناظرية مختارةً في أي ويدجتٍ منصوبة، ويُلغى بعودة
     * الرقمية — فلا بطارية تُهدر إلا لعقاربٍ تُرى.
     */
    fun updateMinuteTick(context: Context, customization: WidgetCustomization) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val hasWidget = manager != null && hasAnyWidget(context)
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
            Intent(context, NoorWidget::class.java).setAction(NoorWidget.ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * نبضة منتصف الليل (alpha62) — بلاغ المالك: «تأكد ان يوم الجمعه
     * يتغير اسم صلاه الظهر الى الجمعه في الويدجت» — أسمُّ الصلوات
     * والتاريخان يُحسبان لحظة الرسم، لكن الويدجت لا تُرسم وحدها
     * عند انتقال اليوم؛ فمنبهٌ رقيقٌ عند أول الدقيقة من اليوم الجديد
     * يعيد الرسم فيتبدل «الظهر» ليلة الجمعة «الجمعة» والهجري
     * والميلادي يومُهم الجديد — ويُعاد جدولُه كل ليلة.
     */
    fun updateDayTick(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val hasWidget = manager != null && hasAnyWidget(context)
            if (!hasWidget) return
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return
            val pi = dayPendingIntent(context)
            am.cancel(pi)
            val nextMidnight = java.time.LocalDate.now()
                .plusDays(1L)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight, pi)
        }
    }

    private fun dayPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            DAY_REQUEST_CODE,
            Intent(context, NoorWidget::class.java).setAction(NoorWidget.ACTION_DAY_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
