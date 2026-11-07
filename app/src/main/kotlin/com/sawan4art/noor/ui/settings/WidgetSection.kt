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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.ui.theme.AccentPalette
import com.sawan4art.noor.widget.WidgetCustomization
import com.sawan4art.noor.widget.WidgetStore
import com.sawan4art.noor.widget.WidgetUpdater

/**
 * قسم «الويدجت» في الإعدادات — alpha57 بلاغ المالك:
 * «خليها ويدجيت واحده و قابلة للتخصيص بالكامل من الاعدادات التخصيص
 * المتاح هو عرض او ايقاف الوقت و عرض او ايقاف مواقيت الصلاه و عرض او
 * ايقاف المتبقي على الصلاه التاليه و عرض او ايقاف التاريخ الهجري و
 * الميلادي بكذا صيغه و عرض او ايقاف المسبحه للتسبيح من خلال الويدجت
 * و امكانيه جعلها شفافه و اختيار الوان الخط او الوانها و امكانيه
 * اضافه شكل النقطه السائله في اي مكان … مع تغيير لون كل فقاعه».
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
    }

    Column {
        Text(
            text = "ويدجت واحدة تخصّصها كما تحب: تستدلي بها من قائمة " +
                "ويدجتات الشاشة الرئيسية (اضغط مطولاً على الخلفية ← ويدجتات ← " +
                "نور المسلم)، وكل ضغطةٍ هنا تعيد رسمها فوراً على شاشتك.",
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

        // ===== لون الخط =====
        SettingLabel("لون الخط")
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

        // ===== عناصر العرض (بلاغ المالك الخمسة) =====
        SettingLabel("ما يظهر داخل الويدجت")
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

        // ===== صيغ التاريخ =====
        if (customization.showHijri || customization.showGregorian) {
            Spacer(Modifier.height(14.dp))
            SettingLabel("صيغ التاريخ")
            if (customization.showHijri) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("١٤ رمضان ١٤٤٧ هـ", customization.hijriFormat == 0) {
                        apply { it.copy(hijriFormat = 0) }
                    }
                    ThemeChip("١٤ رمضان", customization.hijriFormat == 1) {
                        apply { it.copy(hijriFormat = 1) }
                    }
                    ThemeChip("١٤/٩/١٤٤٧", customization.hijriFormat == 2) {
                        apply { it.copy(hijriFormat = 2) }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (customization.showGregorian) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("الجمعة ٧ نوفمبر", customization.gregFormat == 0) {
                        apply { it.copy(gregFormat = 0) }
                    }
                    ThemeChip("٧ نوفمبر", customization.gregFormat == 1) {
                        apply { it.copy(gregFormat = 1) }
                    }
                    ThemeChip("٠٧/١١/٢٠٢٦", customization.gregFormat == 2) {
                        apply { it.copy(gregFormat = 2) }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ===== النقطة السائلة — كل مقامٍ بفقاعتِه ولونها =====
        SettingLabel("النقطة السائلة")
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

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(scheme.primary)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) scheme.onSurface else scheme.outline.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
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
