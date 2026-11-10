package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.widget.WidgetArt
import com.sawan4art.noor.widget.WidgetCustomization
import com.sawan4art.noor.widget.WidgetPalette
import com.sawan4art.noor.widget.WidgetSnapshot
import com.sawan4art.noor.widget.countdownLabel
import com.sawan4art.noor.widget.palette
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * المحرر التفاعلي للويدجت (alpha61) — بلاغ المالك: «انا عايز ظبط اماكن
 * عناصر الويدجت يكون تفاعلي بمعنى عايز شكل الويدجت يظهر في اعدادت
 * الويدجت داخل التطبيق و يكون قابل لتحريك كل العناصر و تكبيرها من
 * خلال الزوم باليد لتكبير بعض العناصر او تصغيرها بشكل احترافي و
 * عايز كمان لوحه الالوان الخاصه بالويدجت تكون منبثقه».
 *
 * فالمحرر هنا ثلاث عقد معاً على معاينةٍ حيّةٍ تطابق الويدجت المنصوبة:
 * 1. **التحريك بالسحب**: ضغطةٌ مطوّلةٌ على أي عنصرٍ ثم سحبٌ لأعلى
 *    أو لأسفل يعيد ترتيب العناصر لحظةً بلحظة (وترتيبُ الويدجت نفسها
 *    من [WidgetCustomization.elementOrder] فيُعاد رسمها فوراً).
 * 2. **الزوم باليد**: اضغط عنصراً لفتح لوحته المنبثقة ثم اجمع إصبعين
 *    على المعاينة — القرص يكبّر العنصر المفتوح لوحته أو يصغّره (٥٠..٢٥٠٪).
 * 3. **اللوحة المنبثقة**: ضغطة العنصر تفتح لوحته: منزلق الحجم ولوحة
 *    الألوان (التلقائي + ثمانية ألوان) ومفتاح الظهور وأزرار الترتيب
 *    والاسترجاع — كلها في حوارٍ منبثقٍ واحد.
 */
@Composable
internal fun WidgetInteractiveEditor(
    customization: WidgetCustomization,
    onUpdate: (WidgetCustomization) -> Unit,
    /** عرض بالحجم الأصلي — بلاغ المالك 2026-11-11: «شاشه المعاينه تكون
     *  اكبر لتعرض كامل الويدجت بالحجم الاصلي» — بلا تصغيرٍ ولا نسبة
     *  قياسٍ: عرضُ الويدجت الحقيقي وارتفاعُها بمحتواها، والتمرير
     *  الرأسي يكمل ما لا تتسع له الشاشة */
    realSize: Boolean = false,
) {
    val context = LocalContext.current
    val snapshot = remember { WidgetSnapshot.compute(context) }
    val pal = remember(customization.theme, customization.accentId, customization.textColor) {
        palette(customization)
    }

    var panelKey by remember { mutableStateOf<String?>(null) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    val dragOffsets = remember { mutableStateMapOf<String, Float>() }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }

    // شكل الويدجت كاملاً في المعاينة — بلاغ المالك alpha62: «شاشه
    // معاينه و تعديل الويدجت في التطبيق لا تعرض كامل شكل الويدجت»
    // فالمحتوى إذا زاد عن الإطار كان يُقص؛ والآن يُقاس ارتفاعُ المحتوى
    // الحقيقي ويُصغَّر كلُّه بنسبةٍ واحدةٍ (graphicsLayer) فيظهر شكل
    // الويدجت كاملاً دائماً مهما تعددت العناصر وكبُرت أحجامها —
    // **ووضع الحجم الأصلي** (بلاغ المالك 2026-11-11) بلا تصغيرٍ أبداً
    var containerH by remember { mutableStateOf(0) }
    var contentH by remember { mutableStateOf(0) }
    val fitScale = if (realSize) {
        1f
    } else if (containerH > 0 && contentH > containerH) {
        containerH.toFloat() / contentH
    } else {
        1f
    }

    // العناصر الظاهرة فقط — بترتيبها الحالي، وهي التي تُسحب وتُرتب
    val visibleKeys = customization.elementOrder.filter { isVisibleIn(customization, it) }

    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in visibleKeys.indices ||
            toIndex !in visibleKeys.indices
        ) {
            return
        }
        val display = visibleKeys.toMutableList()
        val item = display.removeAt(fromIndex)
        display.add(toIndex, item)
        // الترتيب الكامل = دورة على الترتيب القديم: الظاهر يأخذ ترتيبه
        // الجديد بالتسلسل، والمخفي يبقى في مكانه النسبي
        var d = 0
        val full = customization.elementOrder.map { key ->
            if (isVisibleIn(customization, key)) display.getOrNull(d++).orEmpty() else key
        }.filter { it.isNotBlank() }
        onUpdate(customization.copy(elementOrder = WidgetCustomization.normalizeOrder(full)))
    }

    Column {
        SettingLabel("شكل الويدجت الآن — محرر تفاعلي حيّ")
        Text(
            text = "معاينةٌ مطابقةٌ للويدجت على شاشتك وتُظهر شكلها كاملاً: " +
                "اسحب أي عنصرٍ بضغطةٍ مطوّلةٍ لتحريكه وترتيبه، واضغط عليه " +
                "تُفتح لوحته المنبثقة (الحجم واللون والظهور)، وافتح لوحة " +
                "العنصر ثم اجمع إصبعين على المعاينة للزوم تكبيراً وتصغيراً " +
                "— وضغطةٌ على الويدجت المنصوبة نفسها تُبدّل لون إطارها الدائري.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (realSize) {
                        // الحجم الأصلي: الارتفاع بمحتواها — وهي عرض الويدجت الحقيقي
                        Modifier
                    } else {
                        Modifier.aspectRatio(250f / 180f)
                    },
                )
                // الحواف حادة كالويدجت نفسها (alpha63) — والإطار بلونه
                // الحقيقي من التخصيص أو بلا إطارٍ خالصاً
                .clip(RectangleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(pal.bgTop).copy(alpha = customization.opacityFraction),
                            Color(pal.bgBottom).copy(alpha = customization.opacityFraction),
                        ),
                    ),
                )
                .then(
                    if (customization.frameColor == WidgetCustomization.FRAME_NONE) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 2.dp,
                            color = Color(
                                WidgetCustomization.parseColor(customization.frameColor)
                                    ?: pal.accent,
                            ).copy(alpha = 0.85f),
                            shape = RectangleShape,
                        )
                    },
                )
                // الزوم باليد — إصبعان على المعاينة يكبّران العنصر المفتوح
                // لوحته ويصغّرانه — **داخل حدود عنصره** (alpha63: «يعني
                // مثلا اوقات الصلاه اذا كبرت على اعلى مستوى تكبير فيستم
                // قص بعض الحروف» فسقف الخلايا الخمس ١٦٠٪)
                .pointerInput(panelKey, customization) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val key = panelKey ?: return@detectTransformGestures
                        if (zoom != 1f) {
                            val current = customization.elementSizes[key] ?: 100
                            val next = WidgetCustomization.clampSize(
                                key,
                                (current * zoom).roundToInt(),
                            )
                            if (next != current) {
                                onUpdate(
                                    customization.copy(
                                        elementSizes = customization.elementSizes + (key to next),
                                    ),
                                )
                            }
                        }
                    }
                },
        ) {
            // المستقبِل المتاح بعد الحواشي — هو سقفُ المحتوى
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (realSize) {
                            Modifier
                        } else {
                            Modifier.fillMaxSize()
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .onSizeChanged { containerH = it.height },
                verticalArrangement = Arrangement.Center,
            ) {
                // والمحتوى نفسه — يُقاس ارتفاعُه الحقيقي ويُصغَّر بنسبةٍ
                // واحدةٍ إن زاد عن السقف فيظهر الشكل كاملاً بلا قصٍّ
                Box(
                    modifier = Modifier
                        .onSizeChanged { contentH = it.height }
                        .graphicsLayer {
                            scaleX = fitScale
                            scaleY = fitScale
                        },
                ) {
                visibleKeys.forEachIndexed { index, key ->
                    val offset = dragOffsets[key] ?: 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offsetY(offset.roundToInt())
                            .onSizeChanged { itemHeights[key] = it.height }
                            .let { m ->
                                m.pointerInput(key, visibleKeys) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingKey = key },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            val current = dragOffsets[key] ?: 0f
                                            val next = current + amount.y
                                            val h = (itemHeights[key] ?: 40) * 0.55f
                                            val idx = visibleKeys.indexOf(key)
                                            when {
                                                next > h && idx < visibleKeys.size - 1 -> {
                                                    move(idx, idx + 1)
                                                    dragOffsets[key] = 0f
                                                }
                                                next < -h && idx > 0 -> {
                                                    move(idx, idx - 1)
                                                    dragOffsets[key] = 0f
                                                }
                                                else -> dragOffsets[key] = next
                                            }
                                        },
                                        onDragEnd = {
                                            draggingKey = null
                                            dragOffsets.clear()
                                        },
                                        onDragCancel = {
                                            draggingKey = null
                                            dragOffsets.clear()
                                        },
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                // إطار العنصر في المعاينة — بعين ما يُرسم في
                                // الويدجت (حدٌّ بحوافٍ دائريةٍ بلون التخصيص)
                                customization.elementFrames[key]
                                    ?.let { hex ->
                                        WidgetCustomization.parseColor(hex)?.let { argb ->
                                            Modifier.border(
                                                width = 2.dp,
                                                color = Color(argb),
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                        }
                                    }
                                    ?: Modifier,
                            )
                            .then(
                                if (panelKey == key || draggingKey == key) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = Color(pal.accent).copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { panelKey = key },
                    ) {
                        WidgetElementPreview(
                            key = key,
                            customization = customization,
                            snapshot = snapshot,
                            pal = pal,
                        )
                    }
                }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "عنصرٌ مفتوحٌ للوحة: ${elementTitle(panelKey)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButtonReset {
                onUpdate(
                    WidgetCustomization.DEFAULT.copy(
                        theme = customization.theme,
                        opacityPercent = customization.opacityPercent,
                        accentId = customization.accentId,
                        textColor = customization.textColor,
                    ),
                )
            }
        }
    }

    if (panelKey != null) {
        ElementPanelDialog(
            key = panelKey!!,
            customization = customization,
            onUpdate = onUpdate,
            onDismiss = { panelKey = null },
        )
    }
}

/** زر استرجاع الترتيب الافتراضي كله */
@Composable
private fun TextButtonReset(onClick: () -> Unit) {
    Text(
        text = "استرجاع الترتيب الافتراضي",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    )
}

private fun Modifier.offsetY(y: Int): Modifier = this.offset { IntOffset(0, y) }

/** عنوان العنصر — للعرض في سطر الحالة */
internal fun elementTitle(key: String?): String = when (key) {
    WidgetCustomization.ELEMENT_HEADER -> "الترويسة (الاسم والمدينة)"
    WidgetCustomization.ELEMENT_CLOCK -> "الوقت"
    WidgetCustomization.ELEMENT_HIJRI -> "التاريخ الهجري"
    WidgetCustomization.ELEMENT_GREG -> "التاريخ الميلادي"
    WidgetCustomization.ELEMENT_NEXT -> "الصلاة القادمة"
    WidgetCustomization.ELEMENT_COUNTDOWN -> "العدّاد المتبقي"
    WidgetCustomization.ELEMENT_GRID -> "مواقيت الصلوات"
    null -> "لا شيء — اضغط عنصراً في المعاينة"
    else -> key
}

/** هل العنصر ظاهرٌ بالتخصيص الحالي؟ — نفس عقد الرسّام */
internal fun isVisibleIn(customization: WidgetCustomization, key: String): Boolean = when (key) {
    WidgetCustomization.ELEMENT_HEADER -> customization.showBrand || customization.showCity
    WidgetCustomization.ELEMENT_CLOCK -> customization.showClock
    WidgetCustomization.ELEMENT_HIJRI -> customization.showHijri
    WidgetCustomization.ELEMENT_GREG -> customization.showGregorian
    WidgetCustomization.ELEMENT_NEXT -> true
    WidgetCustomization.ELEMENT_COUNTDOWN -> customization.showCountdown
    WidgetCustomization.ELEMENT_GRID -> customization.showPrayerTimes
    else -> false
}

/**
 * معاينة العنصر الواحد — نفس محتوى الويدجت الفعلي بنفس أحجامه وألوانه
 * (sizeOf/colorOf مثل الرسّام) كي تكون المعاينة مطابقةً لا شبيهة.
 */
@Composable
private fun WidgetElementPreview(
    key: String,
    customization: WidgetCustomization,
    snapshot: WidgetSnapshot,
    pal: WidgetPalette,
) {
    fun sizeOf(key: String, base: Float): Float =
        base * (customization.elementSizes[key] ?: 100) / 100f

    fun colorOf(key: String, fallback: Int): Color =
        customization.elementColors[key]
            ?.let { WidgetCustomization.parseColor(it) }
            ?.let { Color(it) } ?: Color(fallback)

    // الساعة الحيّة في المعاينة — تنبض كل عشر ثوانٍ كي تُرى حيّة
    val now: LocalTime by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            kotlinx.coroutines.delay(10_000L)
        }
    }

    when (key) {
        WidgetCustomization.ELEMENT_HEADER -> Row(verticalAlignment = Alignment.CenterVertically) {
            if (customization.showBrand) {
                Text(
                    text = "نور المسلم",
                    fontSize = sizeOf(key, 11f).sp,
                    fontWeight = FontWeight.Bold,
                    color = colorOf(key, pal.text),
                )
            }
            if (customization.showBrand && customization.showCity && snapshot.city.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
            }
            if (customization.showCity && snapshot.city.isNotBlank()) {
                Text(
                    text = snapshot.city,
                    fontSize = sizeOf(key, 10f).sp,
                    color = colorOf(key, pal.muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        WidgetCustomization.ELEMENT_CLOCK -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (customization.clockStyle == WidgetCustomization.ClockStyle.DIGITAL) {
                Text(
                    text = now.format(DateTimeFormatter.ofPattern("h:mm a")),
                    fontSize = sizeOf(key, 15f).sp,
                    fontWeight = FontWeight.Bold,
                    color = colorOf(key, pal.text),
                )
            } else {
                val scale = (customization.elementSizes[key] ?: 100) / 100f
                val bmp = remember(customization.elementSizes[key], pal) {
                    WidgetArt.analogClockBitmap(
                        (WidgetArt.ANALOG_SIZE * scale).toInt().coerceIn(64, 260),
                        now.hour, now.minute,
                        dialColor = pal.text,
                        handColor = colorOf(key, pal.text).toArgb(),
                        accentColor = pal.accent,
                        onDialColor = pal.text,
                    )
                }
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(((WidgetArt.ANALOG_SIZE * scale)).dp.coerceAtMost(170.dp)),
                )
            }
        }

        WidgetCustomization.ELEMENT_HIJRI -> Text(
            text = com.sawan4art.noor.util.Hijri.formatHijri(
                snapshot.hijriDate, customization.hijriFormat, snapshot.gregorianDate,
            ),
            fontSize = sizeOf(key, 11f).sp,
            color = colorOf(key, pal.text),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        WidgetCustomization.ELEMENT_GREG -> Text(
            text = com.sawan4art.noor.util.Hijri.formatGregorian(
                snapshot.gregorianDate, customization.gregFormat,
            ),
            fontSize = sizeOf(key, 11f).sp,
            color = colorOf(key, pal.accent),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        WidgetCustomization.ELEMENT_NEXT -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = snapshot.nextName,
                fontSize = sizeOf(key, 20f).sp,
                fontWeight = FontWeight.Bold,
                color = colorOf(key, pal.text),
                maxLines = 1,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = snapshot.nextTimeText,
                fontSize = sizeOf(key, 14f).sp,
                fontWeight = FontWeight.Bold,
                color = colorOf(key, pal.accent),
                maxLines = 1,
            )
        }

        WidgetCustomization.ELEMENT_COUNTDOWN -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // المعاينة تطابق الويدجت (alpha64): الجملة TextView والأرقام
            // للعدّاد الحي — بصيغة العدّاد نفسها
            Text(
                text = countdownLabel(snapshot.nextName),
                fontSize = sizeOf(key, 12f).sp,
                color = colorOf(key, pal.muted),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                maxLines = 1,
            )
            Text(
                text = remainingPreview(snapshot.remainingSeconds),
                fontSize = sizeOf(key, 12f).sp,
                color = colorOf(key, pal.muted),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                maxLines = 1,
            )
        }

        WidgetCustomization.ELEMENT_GRID -> Row(modifier = Modifier.fillMaxWidth()) {
            val currentIndex = WidgetSnapshot.currentRowIndex(snapshot.rows)
            snapshot.rows.forEachIndexed { i, row ->
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = row.name,
                            fontSize = sizeOf(key, 10f).sp,
                            color = if (row.isPast && i != currentIndex) {
                                Color(pal.muted)
                            } else {
                                colorOf(key, pal.text)
                            },
                            maxLines = 1,
                        )
                        Text(
                            text = row.time,
                            fontSize = sizeOf(key, 10f).sp,
                            color = when {
                                row.isNext -> colorOf(key, pal.accent)
                                row.isPast && i != currentIndex -> Color(pal.muted)
                                else -> colorOf(key, pal.text)
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** نص المتبقي في المعاينة — نفس شكل العدّاد الحي (س:د:ث / د:ث) */
private fun remainingPreview(seconds: Long): String {
    val total = seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/**
 * لوحة العنصر المنبثقة — منزلق الحجم (زومٌ يدويٌّ أيضاً من هنا) ولوحة
 * الألوان ومفتاح الظهور وأزرار الترتيب والاسترجاع — كلها حوارٌ واحد
 * بنفس لغة التطبيق (بلاغ المالك: «لوحه الالوان الخاصه بالويدجت تكون
 * منبثقه»).
 */
@Composable
private fun ElementPanelDialog(
    key: String,
    customization: WidgetCustomization,
    onUpdate: (WidgetCustomization) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val size = customization.elementSizes[key] ?: 100
    val color = customization.elementColors[key] ?: WidgetCustomization.COLOR_AUTO

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        title = {
            Text(
                text = elementTitle(key),
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
        },
        text = {
            Column {
                // ===== الحجم (الزوم) — داخل حدود العنصر نفسه (alpha63):
                // «يعني مثلا اوقات الصلاه اذا كبرت على اعلى مستوى تكبير
                // فيستم قص بعض الحروف» فسقف مواقيت الصلوات ١٦٠٪
                val bounds = remember(key) { WidgetCustomization.boundsOf(key) }
                Text(
                    text = "حجم العنصر — ${MisbahaArabic(size)}٪ " +
                        "(من ${MisbahaArabic(bounds.first)} إلى ${MisbahaArabic(bounds.last)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Slider(
                    value = size.toFloat(),
                    onValueChange = { value ->
                        val next = WidgetCustomization.clampSize(key, value.roundToInt())
                        onUpdate(
                            customization.copy(
                                elementSizes = customization.elementSizes + (key to next),
                            ),
                        )
                    },
                    valueRange = bounds.first.toFloat()..bounds.last.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.primary,
                        activeTrackColor = scheme.primary,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // ===== اللوحة المنبثقة للألوان =====
                Text(
                    text = "لون العنصر",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                ColorPanelChipInline(
                    selected = color,
                    onSelect = { hex ->
                        onUpdate(
                            customization.copy(
                                elementColors = customization.elementColors + (key to hex),
                            ),
                        )
                    },
                )

                Spacer(Modifier.height(12.dp))

                // ===== إطار العنصر — حدٌّ مربّعٌ بحوافٍ دائريةٍ بلونٍ مختارٍ
                // أو بلا إطارٍ (بلاغ المالك 2026-11-11: «عايز اطار للويدجت
                // ولكل عنصر … اطار مربع بحواف دائريه … اطار لكل عنصر على
                // حده و اطار للويدجت كله المستخدم يقرر يستخدمهم ام لا»)
                Text(
                    text = "إطار العنصر — حدٌّ بحوافٍ دائرية",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                ColorPanelChipInline(
                    selected = customization.elementFrames[key]
                        ?: WidgetCustomization.FRAME_NONE,
                    allowNone = true,
                    frameOnly = true,
                    onSelect = { hex ->
                        onUpdate(
                            customization.copy(
                                elementFrames = if (hex == WidgetCustomization.FRAME_NONE) {
                                    customization.elementFrames - key
                                } else {
                                    customization.elementFrames + (key to hex)
                                },
                            ),
                        )
                    },
                )

                // ===== الظهور =====
                if (key != WidgetCustomization.ELEMENT_NEXT) {
                    Spacer(Modifier.height(12.dp))
                    if (key == WidgetCustomization.ELEMENT_HEADER) {
                        PanelToggle(
                            title = "اسم التطبيق",
                            checked = customization.showBrand,
                        ) { on ->
                            onUpdate(customization.copy(showBrand = on))
                        }
                        PanelToggle(
                            title = "المدينة",
                            checked = customization.showCity,
                        ) { on ->
                            onUpdate(customization.copy(showCity = on))
                        }
                    } else {
                        val visible = isVisibleIn(customization, key)
                        PanelToggle(
                            title = "ظاهر في الويدجت",
                            checked = visible,
                        ) { on ->
                            when (key) {
                                WidgetCustomization.ELEMENT_CLOCK ->
                                    onUpdate(customization.copy(showClock = on))
                                WidgetCustomization.ELEMENT_HIJRI ->
                                    onUpdate(customization.copy(showHijri = on))
                                WidgetCustomization.ELEMENT_GREG ->
                                    onUpdate(customization.copy(showGregorian = on))
                                WidgetCustomization.ELEMENT_COUNTDOWN ->
                                    onUpdate(customization.copy(showCountdown = on))
                                WidgetCustomization.ELEMENT_GRID ->
                                    onUpdate(customization.copy(showPrayerTimes = on))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "الترتيب والاسترجاع",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PanelChip("أعلى") {
                        val visible = customization.elementOrder.filter {
                            isVisibleIn(customization, it)
                        }
                        val idx = visible.indexOf(key)
                        if (idx > 0) {
                            val display = visible.toMutableList()
                            val item = display.removeAt(idx)
                            display.add(0, item)
                            var d = 0
                            val full = customization.elementOrder.map { k ->
                                if (isVisibleIn(customization, k)) {
                                    display.getOrNull(d++).orEmpty()
                                } else {
                                    k
                                }
                            }.filter { it.isNotBlank() }
                            onUpdate(
                                customization.copy(
                                    elementOrder = WidgetCustomization.normalizeOrder(full),
                                ),
                            )
                        }
                    }
                    PanelChip("أسفل") {
                        val visible = customization.elementOrder.filter {
                            isVisibleIn(customization, it)
                        }
                        val idx = visible.indexOf(key)
                        if (idx in visible.indices && idx < visible.size - 1) {
                            val display = visible.toMutableList()
                            val item = display.removeAt(idx)
                            display.add(idx + 1, item)
                            var d = 0
                            val full = customization.elementOrder.map { k ->
                                if (isVisibleIn(customization, k)) {
                                    display.getOrNull(d++).orEmpty()
                                } else {
                                    k
                                }
                            }.filter { it.isNotBlank() }
                            onUpdate(
                                customization.copy(
                                    elementOrder = WidgetCustomization.normalizeOrder(full),
                                ),
                            )
                        }
                    }
                    PanelChip("استرجاع ضباطه") {
                        onUpdate(
                            customization.copy(
                                elementSizes = customization.elementSizes - key,
                                elementColors = customization.elementColors - key,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم", color = scheme.primary, fontWeight = FontWeight.Bold)
            }
        },
    )
}

/** رقم عربي صغير لعرض النسبة */
private fun MisbahaArabic(n: Int): String =
    com.sawan4art.noor.domain.mushaf.MushafSurahs.arabicDigits(n)

@Composable
private fun PanelToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun PanelChip(text: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.primary.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = scheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.primary,
        )
    }
}

/**
 * لوحة الألوان المنبثقة — رقاقةٌ تفتح حواراً بألوان التطبيق الثمانية
 * والتلقائي — بلاغ المالك: «عايز كمان لوحه الالوان الخاصه بالويدجت
 * تكون منبثقه». تُستعمل للون الخط العام ولون كل عنصرٍ ولون الإطار —
 * و[allowNone] يضيف خيار «بلا إطار» للإطار وحده (alpha63: «او بدون»).
 */
@Composable
internal fun ColorPanelChipInline(
    selected: String,
    allowNone: Boolean = false,
    /** للأطراف فقط: بلا خيار «تلقائي» — الإطار لونٌ صريحٌ أو بلا */
    frameOnly: Boolean = false,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.25f))
            .border(
                width = 1.dp,
                color = scheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { open = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        // عيّنة اللون الحالي
        val current = if (selected == WidgetCustomization.COLOR_AUTO) {
            null
        } else {
            runCatching { Color(android.graphics.Color.parseColor(selected)) }.getOrNull()
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(current ?: scheme.surface)
                .border(
                    width = 1.dp,
                    color = scheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = when {
                selected == WidgetCustomization.COLOR_AUTO -> "تلقائي — يتبع المظهر"
                selected == WidgetCustomization.FRAME_NONE -> if (frameOnly) "بلا إطار" else "بلا إطار"
                else -> "لون صريح محفوظ"
            },
            fontSize = 13.sp,
            color = scheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "اختيار…",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.primary,
        )
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = scheme.surface,
            title = {
                Text("اختر اللون", fontWeight = FontWeight.Bold, color = scheme.onSurface)
            },
            text = {
                Column {
                    // خيار التلقائي يُخفى في لوحات الأطراف — الإطار لونٌ صريحٌ أو بلا
                    if (!frameOnly) Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(scheme.surface)
                                .border(
                                    width = if (selected == WidgetCustomization.COLOR_AUTO) {
                                        2.5.dp
                                    } else {
                                        1.dp
                                    },
                                    color = if (selected == WidgetCustomization.COLOR_AUTO) {
                                        scheme.primary
                                    } else {
                                        scheme.outline.copy(alpha = 0.4f)
                                    },
                                    shape = CircleShape,
                                )
                                .clickable {
                                    onSelect(WidgetCustomization.COLOR_AUTO)
                                    open = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "أ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface.copy(alpha = 0.75f),
                            )
                        }
                        Text(
                            text = "تلقائي — يتبع المظهر",
                            fontSize = 13.sp,
                            color = scheme.onSurface,
                        )
                    }
                    // بلا إطار — للإطار وحده (alpha63: «بالوان مختاره او بدون»)
                    if (allowNone) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelect(WidgetCustomization.FRAME_NONE)
                                    open = false
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(scheme.surface)
                                    .border(
                                        width = if (selected == WidgetCustomization.FRAME_NONE) {
                                            2.5.dp
                                        } else {
                                            1.dp
                                        },
                                        color = if (selected == WidgetCustomization.FRAME_NONE) {
                                            scheme.primary
                                        } else {
                                            scheme.outline.copy(alpha = 0.4f)
                                        },
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            Text(
                                text = "بلا إطار — حوافٌ حادةٌ نظيفة",
                                fontSize = 13.sp,
                                color = scheme.onSurface,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    WIDGET_COLOR_PANEL.forEach { hex ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelect(hex)
                                    open = false
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        runCatching {
                                            Color(android.graphics.Color.parseColor(hex))
                                        }.getOrDefault(scheme.primary),
                                    )
                                    .border(
                                        width = if (selected == hex) 2.5.dp else 1.dp,
                                        color = if (selected == hex) {
                                            scheme.primary
                                        } else {
                                            scheme.outline.copy(alpha = 0.35f)
                                        },
                                        shape = CircleShape,
                                    ),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = hex.uppercase(),
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text("إلغاء", color = scheme.onSurfaceVariant)
                }
            },
        )
    }
}

/** ألوان لوحة الويدجت المنبثقة — نفس ألوان الخط الثمانية المعتمدة */
private val WIDGET_COLOR_PANEL = listOf(
    "#233029", "#F2EDE0", "#2FA48C", "#C98A3D",
    "#B04A3A", "#5B6E9E", "#7A5B9E", "#4E7A4E",
)
