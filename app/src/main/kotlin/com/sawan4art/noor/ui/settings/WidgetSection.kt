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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
 * قسم «الويدجت» في الإعدادات — توجيه المالك ٢٠٢٦-١١-٠٧:
 * «قسم جديد في الاعدادات خاص بالويدجت… خمس اشكال مختلفه بنفس تصميم
 * التطبيق و هتكون الويدجت دي قابله للتخصيص والتعديل الكامل من قبل
 * المستخدم».
 *
 * الأشكال الخمسة تُستدلى من قائمة ويدجتات النظام نفسها (المهم من هنا
 * هو التخصيص)، ومن هنا يضبط المالك: المظهر (بالثيم/فاتح/داكن)،
 * الشفافية، لون التطبيق، وعناصر العرض — وكل ضغطة تعيد رسم الويدجتات
 * المنصوبة فوراً لتعايش المعاينات الصغيرة ما تغيّر.
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
            text = "خمسة ويدجتات بلون التطبيق: تستدلي بها من قائمة ويدجتات " +
                "الشاشة الرئيسية (اضغط مطولاً على الخلفية ← ويدجتات ← نور المسلم)، " +
                "ومن هنا تخصّصها كما تحب وتراها تتغير فوراً.",
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
            colors = androidx.compose.material3.SliderDefaults.colors(
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

        // ===== عناصر العرض =====
        SettingLabel("ما يظهر داخل الويدجت")
        ToggleRow("المدينة", customization.showCity) { on ->
            apply { it.copy(showCity = on) }
        }
        ToggleRow("العدّاد التنازلي حتى الأذان", customization.showCountdown) { on ->
            apply { it.copy(showCountdown = on) }
        }
        ToggleRow("التاريخ الهجري والميلادي", customization.showDates) { on ->
            apply { it.copy(showDates = on) }
        }
        ToggleRow("اسم التطبيق", customization.showBrand) { on ->
            apply { it.copy(showBrand = on) }
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
