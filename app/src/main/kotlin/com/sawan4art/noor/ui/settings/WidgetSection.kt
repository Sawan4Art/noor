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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
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
    // معاينة المُحرر بحجمٍ أكبر — حوارٌ يملأ الشاشة (alpha63 — بلاغ
    // المالك: «عايزك تكبر شاشه معاينه الويدجت»)
    var bigPreview by remember { mutableStateOf(false) }
    // زر الحفظ — بلاغ المالك alpha63: «عايز زر حفظ في اعادادات
    // الويدجت علشان احفظ التعديل في يظهر في نفس الوقت على الويدجت
    // الموجوده في الشاشه» — ولمسة حفظٍ حديثة تُطمئن صاحبها
    var savedFlash by remember { mutableStateOf(false) }

    fun apply(update: (WidgetCustomization) -> WidgetCustomization) {
        val next = update(customization)
        customization = next
        WidgetStore.save(context, next)
        WidgetUpdater.refreshAll(context)
        // شكل الساعة يدير منبه الدقيقة — التناظرية تُنَبَّه والرقمية تُهدأ
        WidgetUpdater.updateMinuteTick(context, next)
    }

    fun saveNow() {
        WidgetStore.save(context, customization)
        WidgetUpdater.refreshAll(context)
        WidgetUpdater.updateMinuteTick(context, customization)
        savedFlash = true
    }

    // بلا تمريرٍ خاص — صفحة القسم نفسها تتمرّر فلا تمريرٌ مزدوج
    Column {
        Text(
            text = "ويدجت واحدة تخصّصها كما تحب: تستدلي بها من قائمة " +
                "ويدجتات الشاشة الرئيسية (اضغط مطولاً على الخلفية ← ويدجتات ← " +
                "نور المسلم)، وضغطةٌ على الويدجت نفسها على شاشتك تفتح هذا " +
                "القسم داخل التطبيق، وما تخفيه من عناصر يضمّ الباقون مكانه " +
                "بأجمل توزيع.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        // ===== المحرر التفاعلي (alpha61) — المعاينة الحية والسحب والزوم
        // واللوحة المنبثقة في واحدٍ — توجيه المالك: «عايز شكل الويدجت
        // يظهر في اعدادت الويدجت داخل التطبيق و يكون قابل لتحريك كل
        // العناصر و تكبيرها من خلال الزوم باليد … و لوحه الالوان تكون منبثقه»
        WidgetInteractiveEditor(
            customization = customization,
            onUpdate = { next -> apply { next } },
        )

        // معاينة أكبر — بلاغ المالك alpha63: «عايزك تكبر شاشه معاينه الويدجت»
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "افتح المعاينة بحجم أكبر ←",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { bigPreview = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            // زر الحفظ — بلاغ المالك alpha63: «عايز زر حفظ في اعادادات
            // الويدجت علشان احفظ التعديل في يظهر في نفس الوقت على
            // الويدجت الموجوده في الشاشه»
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                onClick = { saveNow() },
            ) {
                Text(
                    text = if (savedFlash) "حُفظ ✓ وعلى الويدجت الآن" else "حفظ التعديلات",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
        if (savedFlash) {
            LaunchedEffect(savedFlash) {
                kotlinx.coroutines.delay(2_500L)
                savedFlash = false
            }
        }

        Spacer(Modifier.height(16.dp))
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

        // ===== لون الخط العام — لوحة منبثقة (alpha61) =====
        SettingLabel("لون الخط العام")
        ColorPanelChipInline(
            selected = customization.textColor,
            onSelect = { hex -> apply { it.copy(textColor = hex) } },
        )

        Spacer(Modifier.height(16.dp))

        // ===== عناصر العرض =====
        SettingLabel("ما يظهر داخل الويدجت — والمخفي يُضمُّ ما حوله")
        ToggleRow("الوقت الحالي", customization.showClock) { on ->
            apply { it.copy(showClock = on) }
        }
        ToggleRow("مواقيت الصلوات الخمس — بلا الشروق", customization.showPrayerTimes) { on ->
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

        // ضباط كل عنصرٍ (الحجم واللون والترتيب) صارت في المحرر التفاعلي أعلاه
        // ولوحاته المنبثقة — بلا صفوفٍ مكرّرة (alpha61)

        Spacer(Modifier.height(16.dp))

        // ===== الإطار — بألوانٍ مختارةٍ أو بلا (alpha63) =====
        SettingLabel("إطار الويدجت")
        Text(
            text = "حوافٌ حادةٌ وإطارٌ بلونٍ تختاره من اللوحة — أو بلا " +
                "إطارٍ خالصاً — وضغطةٌ على الويدجت نفسها على شاشتك تفتح " +
                "إعداداتها داخل التطبيق.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        ColorPanelChipInline(
            selected = customization.frameColor,
            allowNone = true,
            onSelect = { hex -> apply { it.copy(frameColor = hex) } },
        )
    }

    // ===== المعاينة بحجمٍ أكبر (alpha63) — المُحرر نفسه في حوارٍ يملأ
    // الشاشة فتُرى عناصر الويدجت أوضح وتُحرَّر بنفس السحب والزوم =====
    if (bigPreview) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { bigPreview = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "معاينة الويدجت بحجمٍ أكبر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "إغلاق",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { bigPreview = false }
                                .padding(8.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    WidgetInteractiveEditor(
                        customization = customization,
                        onUpdate = { next -> apply { next } },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
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
internal fun SettingLabel(text: String) {
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
