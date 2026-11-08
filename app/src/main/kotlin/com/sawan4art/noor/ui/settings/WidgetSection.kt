package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.ui.theme.AccentPalette
import com.sawan4art.noor.util.Hijri
import com.sawan4art.noor.widget.WidgetCustomization
import com.sawan4art.noor.widget.WidgetStore
import com.sawan4art.noor.widget.WidgetUpdater

/**
 * قسم «الويدجت» في الإعدادات — alpha57 بلاغ المالك:
 * «خليها ويدجيت واحده و قابلة للتخصيص بالكامل من الاعدادات …»
 *
 * وأوسع في alpha59 بلاغ المالك:
 * - «عايز خانات لتحديد حجم الخط ولونه لكل عنصر» — فلكل عنصرٍ
 *   صفُّه: رقاقات الحجم (صغير/عادي/كبير) ودوائر اللون (تلقائي +
 *   ثمانية ألوان) خريطتَي [WidgetCustomization.elementSizes] و[elementColors].
 * - «عايز صيغ اكثر للتواريخ و عند اختيار صيغه للتاريخ تفتح له قائمه
 *   منبثقه» — فقائمتان منبثقتان بستٍّ صيغٍ لكل تقويمٍ بنموذجها الحي.
 * - «يتم ايضا اضافه ساعه تناظريه يمكن للمستخدم الاختيار بينها و بين
 *   الرقميه» — فرقاقتا شكل الساعة.
 * - «مواقيت الصلاه و اسماءها تظهر بشكل صغير في الويدجت» — فالخلايا
 *   الصغيرة عنصرٌ مستقلٌ بحجمه ولونه.
 * - وضمُّ العناصر الباقية عند إخفاء عنصرٍ — فلا فراغات مكانه.
 *
 * فالقسم كله ضباطٌ حيّة: كل ضغطة تُحفظ في [WidgetStore] وتعيد رسم
 * الويدجت المنصوبة فوراً بـ [WidgetUpdater].
 */
@Composable
fun WidgetSection() {
    val context = LocalContext.current
    var customization by remember {
        mutableStateOf(WidgetStore.customization(context))
    }

    fun apply(update: (WidgetCustomization) -> WidgetCustomization) {
        val next = update(customization)
        customization = next
        WidgetStore.save(context, next)
        WidgetUpdater.refreshAll(context)
        // شكل الساعة يدير منبه الدقيقة — التناظرية تُنَبَّه والرقمية تُهدأ
        WidgetUpdater.updateMinuteTick(context, next)
    }

    // بلا تمريرٍ خاص — صفحة القسم نفسها تتمرّر فلا تمريرٌ مزدوج
    Column {
        Text(
            text = "ويدجت واحدة تخصّصها كما تحب: تستدلي بها من قائمة " +
                "ويدجتات الشاشة الرئيسية (اضغط مطولاً على الخلفية ← ويدجتات ← " +
                "نور المسلم)، وكل ضغطةٍ هنا تعيد رسمها فوراً على شاشتك — " +
                "وما تخفيه من عناصر يضمّ الباقون مكانه بأجمل توزيع.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        // ===== المظهر =====
        SettingLabel("المظهر")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip("بالثيم", customization.theme == WidgetCustomization.Theme.APP) {
                apply { it.copy(theme = WidgetCustomization.Theme.APP) }
            }
            ThemeChip("فاتح", customization.theme == WidgetCustomization.Theme.LIGHT) {
                apply { it.copy(theme = WidgetCustomization.Theme.LIGHT) }
            }
            ThemeChip("داكن", customization.theme == WidgetCustomization.Theme.DARK) {
                apply { it.copy(theme = WidgetCustomization.Theme.DARK) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== الشفافية =====
        SettingLabel("الشفافية — ${customization.opacityPercent}٪")
        Slider(
            value = customization.opacityPercent.toFloat(),
            onValueChange = { value ->
                val next = customization.copy(opacityPercent = value.toInt())
                customization = next
                WidgetStore.save(context, next)
            },
            onValueChangeFinished = { WidgetUpdater.refreshAll(context) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Spacer(Modifier.height(10.dp))

        // ===== لون التطبيق =====
        SettingLabel("لون التطبيق")
        AccentGrid(
            selectedId = customization.accentId,
            onSelect = { id -> apply { it.copy(accentId = id) } },
        )

        Spacer(Modifier.height(16.dp))

        // ===== لون الخط العام =====
        SettingLabel("لون الخط العام")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ThemeChip(
                "تلقائي",
                customization.textColor == WidgetCustomization.COLOR_AUTO,
            ) {
                apply { it.copy(textColor = WidgetCustomization.COLOR_AUTO) }
            }
            TextColorSwatches(
                selected = customization.textColor,
                onSelect = { hex -> apply { it.copy(textColor = hex) } },
            )
        }

        Spacer(Modifier.height(16.dp))

        // ===== عناصر العرض =====
        SettingLabel("ما يظهر داخل الويدجت — والمخفي يُضمُّ ما حوله")
        ToggleRow("الوقت الحالي", customization.showClock) { on ->
            apply { it.copy(showClock = on) }
        }
        ToggleRow("مواقيت الصلوات الست", customization.showPrayerTimes) { on ->
            apply { it.copy(showPrayerTimes = on) }
        }
        ToggleRow("المتبقي على الصلاة التالية", customization.showCountdown) { on ->
            apply { it.copy(showCountdown = on) }
        }
        ToggleRow("التاريخ الهجري", customization.showHijri) { on ->
            apply { it.copy(showHijri = on) }
        }
        ToggleRow("التاريخ الميلادي", customization.showGregorian) { on ->
            apply { it.copy(showGregorian = on) }
        }
        ToggleRow("المسبحة للتسبيح من الويدجت", customization.showMisbaha) { on ->
            apply { it.copy(showMisbaha = on) }
        }
        ToggleRow("المدينة", customization.showCity) { on ->
            apply { it.copy(showCity = on) }
        }
        ToggleRow("اسم التطبيق", customization.showBrand) { on ->
            apply { it.copy(showBrand = on) }
        }

        // ===== شكل الساعة — رقمية أو تناظرية (بلاغ alpha59) =====
        if (customization.showClock) {
            Spacer(Modifier.height(14.dp))
            SettingLabel("شكل الساعة")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip(
                    "رقمية حيّة",
                    customization.clockStyle == WidgetCustomization.ClockStyle.DIGITAL,
                ) {
                    apply { it.copy(clockStyle = WidgetCustomization.ClockStyle.DIGITAL) }
                }
                ThemeChip(
                    "تناظرية",
                    customization.clockStyle == WidgetCustomization.ClockStyle.ANALOG,
                ) {
                    apply { it.copy(clockStyle = WidgetCustomization.ClockStyle.ANALOG) }
                }
            }
        }

        // ===== صيغ التاريخ — قائمتان منبثقتان بنماذج حية (بلاغ alpha59) =====
        if (customization.showHijri || customization.showGregorian) {
            Spacer(Modifier.height(14.dp))
            SettingLabel("صيغ التاريخ — تختار من القائمة المنبثقة")
            if (customization.showHijri) {
                val samples = remember { Hijri.hijriFormatSamples() }
                FormatDropdown(
                    label = "التاريخ الهجري",
                    selected = samples.getOrElse(customization.hijriFormat) { samples.first() },
                    samples = samples,
                    onSelect = { i -> apply { it.copy(hijriFormat = i) } },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (customization.showGregorian) {
                val samples = remember { Hijri.gregorianFormatSamples() }
                FormatDropdown(
                    label = "التاريخ الميلادي",
                    selected = samples.getOrElse(customization.gregFormat) { samples.first() },
                    samples = samples,
                    onSelect = { i -> apply { it.copy(gregFormat = i) } },
                )
            }
        }

        // ===== حجم ولون كل عنصر (بلاغ alpha59) =====
        Spacer(Modifier.height(16.dp))
        SettingLabel("حجم الخط ولونه لكل عنصر")
        ElementStyleRow(
            title = "الوقت",
            shown = customization.showClock,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_CLOCK],
            color = customization.elementColors[WidgetCustomization.ELEMENT_CLOCK],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_CLOCK to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_CLOCK to hex)) } },
        )
        ElementStyleRow(
            title = "التاريخ الهجري",
            shown = customization.showHijri,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_HIJRI],
            color = customization.elementColors[WidgetCustomization.ELEMENT_HIJRI],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_HIJRI to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_HIJRI to hex)) } },
        )
        ElementStyleRow(
            title = "التاريخ الميلادي",
            shown = customization.showGregorian,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_GREG],
            color = customization.elementColors[WidgetCustomization.ELEMENT_GREG],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_GREG to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_GREG to hex)) } },
        )
        ElementStyleRow(
            title = "الصلاة القادمة",
            shown = true,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_NEXT],
            color = customization.elementColors[WidgetCustomization.ELEMENT_NEXT],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_NEXT to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_NEXT to hex)) } },
        )
        ElementStyleRow(
            title = "العدّاد المتبقي",
            shown = customization.showCountdown,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_COUNTDOWN],
            color = customization.elementColors[WidgetCustomization.ELEMENT_COUNTDOWN],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_COUNTDOWN to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_COUNTDOWN to hex)) } },
        )
        ElementStyleRow(
            title = "مواقيت الصلوات الصغيرة",
            shown = customization.showPrayerTimes,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_GRID],
            color = customization.elementColors[WidgetCustomization.ELEMENT_GRID],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_GRID to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_GRID to hex)) } },
        )
        ElementStyleRow(
            title = "المسبحة",
            shown = customization.showMisbaha,
            size = customization.elementSizes[WidgetCustomization.ELEMENT_MISBAHA],
            color = customization.elementColors[WidgetCustomization.ELEMENT_MISBAHA],
            onSize = { v -> apply { it.copy(elementSizes = it.elementSizes + (WidgetCustomization.ELEMENT_MISBAHA to v)) } },
            onColor = { hex -> apply { it.copy(elementColors = it.elementColors + (WidgetCustomization.ELEMENT_MISBAHA to hex)) } },
        )

        Spacer(Modifier.height(16.dp))

        // ===== النقطة السائلة — كل مقامٍ بفقاعتِه ولونها =====
        SettingLabel("النقطة السائلة الحية")
        Text(
            text = "الفقاعة السائلة الآن برياضيات السائل الحي نفسه في " +
                "التطبيق: سطحٌ متموّج ولمعةٌ زجاجية وقطرةٌ تابعة — لا نقطةٍ جامدة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        ToggleRow("خلف الوقت المتبقي", customization.bubbleOnCountdown) { on ->
            apply { it.copy(bubbleOnCountdown = on) }
        }
        if (customization.bubbleOnCountdown) {
            ColorSwatchRow(
                selected = customization.bubbleColorCountdown,
                onSelect = { hex -> apply { it.copy(bubbleColorCountdown = hex) } },
            )
        }
        ToggleRow("تحديد الصلاة القادمة", customization.bubbleOnNext) { on ->
            apply { it.copy(bubbleOnNext = on) }
        }
        if (customization.bubbleOnNext) {
            ColorSwatchRow(
                selected = customization.bubbleColorNext,
                onSelect = { hex -> apply { it.copy(bubbleColorNext = hex) } },
            )
        }
        ToggleRow("تحديد الصلاة الحالية", customization.bubbleOnCurrent) { on ->
            apply { it.copy(bubbleOnCurrent = on) }
        }
        if (customization.bubbleOnCurrent) {
            ColorSwatchRow(
                selected = customization.bubbleColorCurrent,
                onSelect = { hex -> apply { it.copy(bubbleColorCurrent = hex) } },
            )
        }
    }
}

/** لوحة ألوان الخط — التلقائي ثم ثمانية ألوانٍ صريحة */
private val TEXT_COLORS = listOf(
    "#233029", "#F2EDE0", "#2FA48C", "#C98A3D",
    "#B04A3A", "#5B6E9E", "#7A5B9E", "#4E7A4E",
)

/** اللوحات نفسها للفقاعات — بلوني الافتراضيهما في مقدمتها */
private val BUBBLE_COLORS = listOf(
    "#2FA48C", "#C98A3D", "#B04A3A", "#5B6E9E",
    "#7A5B9E", "#4E7A4E", "#233029", "#F2EDE0",
)

/** مقامات الحجم الثلاثة — صغير/عادي/كبير بنسبها المئوية */
private val SIZE_STEPS = listOf(
    "صغير" to 70,
    "عادي" to 100,
    "كبير" to 135,
)

@Composable
private fun TextColorSwatches(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TEXT_COLORS.forEach { hex ->
            ColorDot(hex, selected == hex, onClick = { onSelect(hex) })
        }
    }
}

@Composable
private fun ColorSwatchRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    ) {
        BUBBLE_COLORS.forEach { hex ->
            ColorDot(hex, selected == hex, onClick = { onSelect(hex) })
        }
    }
}

/**
 * صف ضباط العنصر الواحد — اسمه ثم رقاقات الحجم الثلاث ودوائر اللون
 * (التلقائي أولها) — وإن كان العنصر مخفياً من العرض يُرسم باهتاً
 * كي يعرف المالك أن ضباطه محفوظة لظهره.
 */
@Composable
private fun ElementStyleRow(
    title: String,
    shown: Boolean,
    size: Int?,
    color: String?,
    onSize: (Int) -> Unit,
    onColor: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primary.copy(alpha = if (shown) 0.06f else 0.03f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (shown) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SIZE_STEPS.forEach { (label, value) ->
                SizeChip(
                    label = label,
                    selected = (size ?: 100) == value,
                    dimmed = !shown,
                    onClick = { onSize(value) },
                )
            }
            Spacer(Modifier.width(6.dp))
            ColorDot(
                hex = "",
                selected = color == null || color == "auto",
                dimmed = !shown,
                onClick = { onColor("auto") },
                autoContent = true,
            )
            TEXT_COLORS.take(4).forEach { hex ->
                ColorDot(hex, color == hex, dimmed = !shown, onClick = { onColor(hex) })
            }
        }
        if (color != null && color != "auto" && !TEXT_COLORS.take(4).contains(color)) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "لون مخصص محفوظ — اختر من الألوان لتغييره",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** رقاقة حجم — صغير/عادي/كبير */
@Composable
private fun SizeChip(label: String, selected: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) scheme.primary else scheme.surface,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.3f)),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                scheme.onPrimary
            } else {
                scheme.onSurface.copy(alpha = if (dimmed) 0.4f else 0.8f)
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/**
 * القائمة المنبثقة لصيغ التاريخ — بلاغ المالك alpha59: «عند اختيار
 * صيغه للتاريخ تفتح له قائمه منبثقه» — القيمة الظاهرة نموذج اليوم الحي،
 * وخيارات القائمة الستة كلٌّ بنموذجه فالاختيار بعينٍ لا بالأرقام.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatDropdown(
    label: String,
    selected: String,
    samples: List<String>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        // عضو نطاق ExposedDropdownMenuBoxScope — لا يُنادي إلا من داخلها
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            samples.forEachIndexed { index, sample ->
                DropdownMenuItem(
                    text = { Text(sample) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorDot(
    hex: String,
    selected: Boolean,
    dimmed: Boolean = false,
    onClick: () -> Unit,
    autoContent: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val color = if (autoContent) {
        Color.Transparent
    } else {
        runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .getOrDefault(scheme.primary)
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) scheme.onSurface else scheme.outline.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (autoContent) {
            Text(
                text = "أ",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurface.copy(alpha = if (dimmed) 0.4f else 0.75f),
            )
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** رقاقة مظهر — مختارة تُملأ بلون التطبيق */
@Composable
private fun ThemeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scheme.primary else scheme.surface,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.3f)),
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

/** شبكة ألوان التطبيق العشرون — دوائر الألوان نفسها بلغة قسم الألوان */
@Composable
private fun AccentGrid(selectedId: String, onSelect: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AccentPalette.All.chunked(10).forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowColors.forEach { accent ->
                    val color = accent.day.primary
                    val selected = accent.id == selectedId
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) scheme.onSurface
                                else scheme.outline.copy(alpha = 0.35f),
                                shape = CircleShape,
                            )
                            .clickable { onSelect(accent.id) },
                    )
                }
            }
        }
    }
}

/** صف مفتاح جانبي */
@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
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
