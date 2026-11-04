package com.sawan4art.noor.ui.settings.adhan

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.alarm.AdhanSoundPlayer
import com.sawan4art.noor.data.AdhanPrefs
import com.sawan4art.noor.domain.adhan.AdhanGaps
import com.sawan4art.noor.domain.adhan.AdhanSoundKind
import com.sawan4art.noor.domain.adhan.AdhanVoices

/**
 * إعدادات الأذان والإقامة — القسم الجديد كاملاً:
 * تشغيل الأذان • صوت الأذان (خمسة مدمجة + من الهاتف + نغمة الإشعارات)
 * • الإقامة وصوتها والفاصل بينها وبين الأذان — ثلاثة اختيارات فقط
 * ١٠/١٥/٢٠ دقيقة (توجيه المالك alpha48) • أذان الجمعة مرتين بفاصلين:
 * بين الأذانين اختيارات، وبين الأذان الثاني والإقامة خانة كتابة حرة
 * من دقيقة إلى ١٢٠ (ساعتان) • كتم أصوات الهاتف بعد الأذان (اختياري)
 */

/** أزرار صفحة الأذان — ينفّذها مسؤول الواجهة الرئيسي */
interface AdhanActions {
    fun setAdhanEnabled(on: Boolean)

    fun setAdhanVoice(id: String)

    fun setIqamahEnabled(on: Boolean)

    fun setIqamahOffsetMinutes(minutes: Int)

    fun setIqamahVoice(id: String)

    fun setFridayDouble(on: Boolean)

    fun setFridayAdhanGapMinutes(minutes: Int)

    fun setFridayIqamahGapMinutes(minutes: Int)

    fun setMuteEnabled(on: Boolean)

    fun setMuteMinutes(minutes: Int)

    fun importCustomSound(kind: AdhanSoundKind, uri: Uri)

    fun clearCustomSound(kind: AdhanSoundKind)
}

/** خيارات الفواصل الزمنية — ثلاثة فقط بتوجيه المالك (alpha48) */
private val IQAMAH_OFFSETS = AdhanGaps.IQAMAH_OFFSET_CHOICES
private val FRIDAY_ADHAN_GAPS = listOf(5, 10, 15, 20, 30)
private val MUTE_MINUTES = listOf(5, 10, 15, 20, 30, 45, 60)

@Composable
fun AdhanSection(
    adhan: AdhanPrefs,
    actions: AdhanActions,
) {
    val context = LocalContext.current
    var previewing by remember { mutableStateOf<AdhanSoundKind?>(null) }

    // منتقي الصوت من الهاتف — أي ملف صوتي على الجهاز
    var pickingKind by remember { mutableStateOf<AdhanSoundKind?>(null) }
    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val kind = pickingKind
        pickingKind = null
        if (uri != null && kind != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            actions.importCustomSound(kind, uri)
        }
    }

    fun stopPreview() {
        AdhanSoundPlayer.stop()
        previewing = null
    }

    fun togglePreview(kind: AdhanSoundKind, soundId: String, customPath: String) {
        if (previewing == kind) {
            stopPreview()
            return
        }
        AdhanSoundPlayer.stop()
        AdhanSoundPlayer.start(context, kind, soundId, customPath)
        previewing = if (AdhanSoundPlayer.isPlaying) kind else null
    }

    SectionBlock(title = "تشغيل الأذان") {
        ToggleRow(
            title = "الأذان في وقت الصلوات",
            subtitle = if (adhan.enabled) {
                "الأذان يعمل في وقت الفجر والظهر والعصر والمغرب والعشاء — ولو الشاشة مطفأة"
            } else {
                "متوقف — لن يصدر أي أذان"
            },
            checked = adhan.enabled,
            onToggle = { on ->
                actions.setAdhanEnabled(on)
                if (on) requestBatteryExemption(context)
                stopPreview()
            },
        )
    }

    if (adhan.enabled) {
        SectionBlock(title = "صوت الأذان") {
            VoiceList(
                kind = AdhanSoundKind.ADHAN,
                selectedId = adhan.adhanVoice,
                customName = adhan.customAdhanName,
                onPickBuiltin = { id ->
                    stopPreview()
                    actions.setAdhanVoice(id)
                },
                onPickCustom = {
                    pickingKind = AdhanSoundKind.ADHAN
                    audioPicker.launch(arrayOf("audio/*"))
                },
                onClearCustom = {
                    stopPreview()
                    actions.clearCustomSound(AdhanSoundKind.ADHAN)
                },
            )
            Spacer(Modifier.height(10.dp))
            PreviewButton(
                label = if (previewing == AdhanSoundKind.ADHAN) "إيقاف المعاينة" else "معاينة صوت الأذان",
                playing = previewing == AdhanSoundKind.ADHAN,
                onClick = {
                    val customPath =
                        if (adhan.adhanVoice == "custom") adhan.customAdhanPath else ""
                    togglePreview(AdhanSoundKind.ADHAN, adhan.adhanVoice, customPath)
                },
            )
        }

        SectionBlock(title = "الإقامة") {
            ToggleRow(
                title = "أذان الإقامة",
                subtitle = "«قد قامت الصلاة» — يشتغل بعد الأذان بالمدة المحددة",
                checked = adhan.iqamahEnabled,
                onToggle = { actions.setIqamahEnabled(it) },
            )
            if (adhan.iqamahEnabled) {
                Spacer(Modifier.height(10.dp))
                ChipsRow(
                    label = "الفاصل بين الأذان والإقامة (افتراضي ١٠ دقائق)",
                    options = IQAMAH_OFFSETS,
                    selected = adhan.iqamahOffsetMinutes,
                    onSelect = { actions.setIqamahOffsetMinutes(it) },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "صوت الإقامة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                VoiceList(
                    kind = AdhanSoundKind.IQAMAH,
                    selectedId = adhan.iqamahVoice,
                    customName = adhan.customIqamahName,
                    onPickBuiltin = { id ->
                        stopPreview()
                        actions.setIqamahVoice(id)
                    },
                    onPickCustom = {
                        pickingKind = AdhanSoundKind.IQAMAH
                        audioPicker.launch(arrayOf("audio/*"))
                    },
                    onClearCustom = {
                        stopPreview()
                        actions.clearCustomSound(AdhanSoundKind.IQAMAH)
                    },
                )
                Spacer(Modifier.height(10.dp))
                PreviewButton(
                    label = if (previewing == AdhanSoundKind.IQAMAH) "إيقاف المعاينة" else "معاينة صوت الإقامة",
                    playing = previewing == AdhanSoundKind.IQAMAH,
                    onClick = {
                        val customPath =
                            if (adhan.iqamahVoice == "custom") adhan.customIqamahPath else ""
                        togglePreview(AdhanSoundKind.IQAMAH, adhan.iqamahVoice, customPath)
                    },
                )
            }
        }

        SectionBlock(title = "أذان يوم الجمعة") {
            ToggleRow(
                title = "الأذان مرتين ثم الإقامة",
                subtitle = if (adhan.fridayDouble) {
                    "يوم الجمعة فقط: أذان أول ثم أذان ثانٍ ثم الإقامة — والباقي بأذان واحد"
                } else {
                    "مطفأ — يوم الجمعة بأذان واحد كباقي الأيام"
                },
                checked = adhan.fridayDouble,
                onToggle = { actions.setFridayDouble(it) },
            )
            if (adhan.fridayDouble) {
                Spacer(Modifier.height(10.dp))
                ChipsRow(
                    label = "الفاصل بين الأذان الأول والثاني (افتراضي ١٠ دقائق)",
                    options = FRIDAY_ADHAN_GAPS,
                    selected = adhan.fridayAdhanGapMinutes,
                    onSelect = { actions.setFridayAdhanGapMinutes(it) },
                )
                Spacer(Modifier.height(12.dp))
                // alpha48 بتوجيه المالك: الفاصل بين الأذان الثاني والإقامة
                // في الجمعة ممكن يصل ساعتين — خانة كتابة حرة لا اختيارات
                GapInputRow(
                    label = "الفاصل بين الأذان الثاني والإقامة (افتراضي ٣٠ دقيقة)",
                    minutes = adhan.fridayIqamahGapMinutes,
                    onCommit = { actions.setFridayIqamahGapMinutes(it) },
                )
            }
        }

        SectionBlock(title = "كتم أصوات الهاتف") {
            ToggleRow(
                title = "كتم الهاتف مع الأذان",
                subtitle = if (adhan.muteEnabled) {
                    "يكتم الهاتف كله مع بدء الأذان ويرجع الصوت بعد الإقامة تلقائياً"
                } else {
                    "مطفأ — اختياري لمن يريد سكوناً تاماً وقت الصلاة"
                },
                checked = adhan.muteEnabled,
                onToggle = { actions.setMuteEnabled(it) },
            )
            if (adhan.muteEnabled) {
                Spacer(Modifier.height(10.dp))
                ChipsRow(
                    label = "بعد الإقامة بكم دقيقة يرجع الصوت؟ (افتراضي ١٠ دقائق)",
                    options = MUTE_MINUTES,
                    selected = adhan.muteMinutes,
                    onSelect = { actions.setMuteMinutes(it) },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "لكتم كامل تحتاج منح إذن «عدم الإزعاج» للتطبيق — إن لم يُمنح يُكتم الهاتف بالوضع الصامت على أي حال.",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** طلب استثناء توفير البطارية — بعض الهواتف تقتل المنبهات في التوفير الشديد */
private fun requestBatteryExemption(context: android.content.Context) {
    runCatching {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        val pkg = context.packageName
        if (pm != null && !pm.isIgnoringBatteryOptimizations(pkg)) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/* ==================== عناصر مساعدة بنمط التطبيق ==================== */

@Composable
internal fun SectionBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/** صف الأصوات: خمسة مدمجة + صوت من الهاتف + نغمة الإشعارات */
@Composable
internal fun VoiceList(
    kind: AdhanSoundKind,
    selectedId: String,
    customName: String,
    onPickBuiltin: (String) -> Unit,
    onPickCustom: () -> Unit,
    onClearCustom: () -> Unit,
) {
    androidx.compose.foundation.layout.Column {
        val builtin = if (kind == AdhanSoundKind.ADHAN) AdhanVoices.ADHAN else AdhanVoices.IQAMAH
        for (voice in builtin) {
            VoiceRow(
                title = voice.name,
                subtitle = voice.subtitle,
                selected = selectedId == voice.id,
                onClick = { onPickBuiltin(voice.id) },
            )
        }
        VoiceRow(
            title = "صوت من هاتفي",
            subtitle = if (selectedId == "custom" && customName.isNotEmpty()) {
                customName
            } else {
                "اختر أي ملف صوتي من ذاكرة هاتفك"
            },
            selected = selectedId == "custom",
            onClick = onPickCustom,
            trailingRemovable = selectedId == "custom",
            onRemove = onClearCustom,
        )
        VoiceRow(
            title = "نغمة إشعارات الهاتف",
            subtitle = "صوت الإشعارات الافتراضي لهاتفك بدلاً من الأذان",
            selected = selectedId == "notification",
            onClick = { onPickBuiltin("notification") },
            icon = Icons.Filled.Notifications,
        )
    }
}

@Composable
internal fun VoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    trailingRemovable: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                if (selected) scheme.primary.copy(alpha = 0.10f) else scheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp),
            )
            .border(
                BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) scheme.primary else scheme.outline.copy(alpha = 0.35f),
                ),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (selected && trailingRemovable) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingRemovable && onRemove != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "حذف",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "مختار",
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun ChipsRow(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = label,
        fontSize = 12.sp,
        color = scheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (option in options) {
            val isSelected = option == selected
            Text(
                text = option.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * خانة كتابة حرة لفاصل الجمعة بين الأذان الثاني والإقامة — بتوجيه
 * المالك (alpha48): «ممكن يصل لغايه ٢ ساعه ف عايز خانه كتابه و ليس
 * اختيارات». تقبل الأرقام العربية والغربية، وتحفظ فوراً كل قيمة
 * صالحة (١..١٢٠)، وما تقبلش قيمة خارج الحدود وتوضّح السبب.
 */
@Composable
internal fun GapInputRow(
    label: String,
    minutes: Int,
    onCommit: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(AdhanGaps.toArabicDigits(minutes)) }
    var invalid by remember { mutableStateOf(false) }

    fun commit(raw: String) {
        val parsed = AdhanGaps.parseMinutesInput(raw)
        when {
            parsed == null -> invalid = raw.isNotEmpty()
            AdhanGaps.isFridayGapInputValid(parsed) -> {
                invalid = false
                if (parsed != minutes) onCommit(parsed)
            }
            else -> invalid = true
        }
    }

    Text(
        text = label,
        fontSize = 12.sp,
        color = scheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = raw.filter { it in '٠'..'٩' || it in '0'..'9' }
            text = filtered
            commit(filtered)
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (!state.isFocused && (invalid || text.isBlank())) {
                    text = AdhanGaps.toArabicDigits(minutes)
                    invalid = false
                }
            },
        singleLine = true,
        isError = invalid,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = {
            Text(
                text = if (invalid) {
                    "اكتب عدد دقائق من ١ إلى ١٢٠ (ساعتان)"
                } else {
                    "اكتب المدة بالدقايق — من دقيقة إلى ١٢٠ دقيقة (ساعتان)"
                },
                fontSize = 11.sp,
                color = if (invalid) scheme.error else scheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
internal fun PreviewButton(
    label: String,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                scheme.primary.copy(alpha = if (playing) 0.18f else 0.08f),
                RoundedCornerShape(16.dp),
            )
            .border(
                BorderStroke(1.dp, scheme.primary.copy(alpha = 0.45f)),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Close else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.primary,
        )
    }
}
