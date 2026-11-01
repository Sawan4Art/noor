package com.sawan4art.noor.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.BuildConfig
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.theme.AccentPalette
import java.util.Locale

/**
 * الإعدادات — كل قسمٍ في صفحته الخاصة (alpha28 توجيه المالك:
 * «عايز كل قسم في الاعدادت يكون له صفحه لوحده»):
 *
 * الصفحة الرئيسية قائمة أقسام بملخصاتها الحية (المدينة، الطريقة
 * المختارة، المذهب، اسم اللون، حالة التحديث) وكل قسم يفتح صفحته
 * الخاصة بزر رجوع يصعد خطوة واحدة — التنقل الداخلي على نمط تبويب
 * الأذكار نفسه (الرجوع يعود للقائمة ولا يسرقه معالج NoorApp لأن
 * آخر معالج مُركَّب هو الأسبق).
 */
@Composable
fun SettingsScreen(
    settings: NoorSettingsState,
    locating: Boolean,
    onDetectLocation: () -> Unit,
    onSetMethod: (CalculationMethod) -> Unit,
    onSetHanafi: (Boolean) -> Unit,
    onSetAccent: (String) -> Unit,
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    // الروابط الداخلية: home ← location / method / asr / colors / updates
    var section by rememberSaveable { mutableStateOf("home") }
    BackHandler(enabled = section != "home") { section = "home" }

    when (section) {
        "location" -> SectionPage(title = "الموقع", onBack = { section = "home" }) {
            SectionCard {
                LocationSection(settings = settings, locating = locating, onDetectLocation = onDetectLocation)
            }
        }
        "method" -> SectionPage(title = "طريقة حساب المواقيت", onBack = { section = "home" }) {
            SectionCard {
                MethodSection(settings = settings, onSetMethod = onSetMethod)
            }
        }
        "asr" -> SectionPage(title = "مذهب العصر", onBack = { section = "home" }) {
            SectionCard {
                AsrSection(settings = settings, onSetHanafi = onSetHanafi)
            }
        }
        "colors" -> SectionPage(title = "ألوان التطبيق", onBack = { section = "home" }) {
            SectionCard {
                ColorsSection(settings = settings, onSetAccent = onSetAccent)
            }
        }
        "updates" -> SectionPage(title = "التحديثات", onBack = { section = "home" }) {
            SectionCard {
                UpdatesSection(
                    updateState = updateState,
                    currentVersion = BuildConfig.VERSION_NAME,
                    onCheckUpdates = onCheckUpdates,
                    onInstallUpdate = onInstallUpdate,
                )
            }
        }
        else -> SettingsHome(
            settings = settings,
            updateState = updateState,
            onOpen = { section = it },
        )
    }
}

/** قائمة الأقسام — صفحة الإعدادات الرئيسية: كل قسم صفٌ بملخصه الحي */
@Composable
private fun SettingsHome(
    settings: NoorSettingsState,
    updateState: UpdateUiState,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "الإعدادات",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "اضغط أي قسمٍ ليفتح صفحته الخاصة",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        SectionRow(
            icon = Icons.Filled.LocationOn,
            title = "الموقع",
            summary = settings.city,
            onOpen = { onOpen("location") },
        )
        SectionRow(
            icon = NoorIcons.PrayerTimes,
            title = "طريقة حساب المواقيت",
            summary = settings.method.arabicName,
            onOpen = { onOpen("method") },
        )
        SectionRow(
            icon = NoorIcons.AsrShadow,
            title = "مذهب العصر",
            summary = if (settings.isHanafi) "الحنفي — ظل مثلهين" else "الجمهور — ظل مثله",
            onOpen = { onOpen("asr") },
        )
        SectionRow(
            icon = NoorIcons.Palette,
            title = "ألوان التطبيق",
            summary = AccentPalette.byId(settings.accentId).arabicName +
                " — من بين " + MisbahaMath.arabicNumber(AccentPalette.All.size.toLong()) + " ألوان",
            accentDot = AccentPalette.byId(settings.accentId).day.primary,
            onOpen = { onOpen("colors") },
        )
        SectionRow(
            icon = NoorIcons.Update,
            title = "التحديثات",
            summary = updatesSummary(updateState),
            onOpen = { onOpen("updates") },
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "نور المسلم ${BuildConfig.VERSION_NAME} — إعادة البناء الأصلية بلغة كوتلن",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(116.dp)) // مساحة شريط التنقل الزجاجي العائم
    }
}

/** ملخص التحديث في القائمة — لغة مفهومة من غير فتح الصفحة */
private fun updatesSummary(state: UpdateUiState): String = when (state) {
    is UpdateUiState.Available -> "تحديث جديد متاح: ${state.info.versionName}"
    is UpdateUiState.Downloading -> "جارٍ تنزيل التحديث… ${(state.fraction * 100).toInt()}%"
    is UpdateUiState.ReadyToInstall -> "التحديث جاهز — افتح الصفحة للتثبيت"
    is UpdateUiState.Failed -> "تعذّر الفحص — افتح الصفحة لإعادة المحاولة"
    else -> "أنت على أحدث نسخة — والفحص تلقائي عند فتح التطبيق"
}

/**
 * صف قسمٍ في القائمة — بنية بطاقات مجموعات الأذكار نفسها: أيقونة في
 * مربع متدرّج، عنوان وملخص حي، وسهم الدخول في اتجاه القراءة.
 */
@Composable
private fun SectionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    accentDot: Color? = null,
    onOpen: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = scheme.outline.copy(alpha = 0.28f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                scheme.primaryContainer.copy(alpha = 0.75f),
                                scheme.secondaryContainer.copy(alpha = 0.75f),
                            ),
                        ),
                        shape = RoundedCornerShape(15.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    if (accentDot != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = accentDot, modifier = Modifier.size(14.dp)) {}
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "فتح",
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** صفحة قسمٍ — ترويسة برجوع يصعد خطوةً واحدة ومحتوى قابل للسحب */
@Composable
private fun SectionPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = scheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
            Spacer(Modifier.height(116.dp)) // مساحة شريط التنقل الزجاجي العائم
        }
    }
}

/** بطاقة القسم الواحد داخل صفحته — بلا عنوان مكرر فوقه */
@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

// ===== محتوى الأقسام الخمسة — كامل إعدادات المرحلة الأولى =====

/** قسم الموقع — مدينتك الحالية وتحديدها تلقائياً */
@Composable
private fun LocationSection(
    settings: NoorSettingsState,
    locating: Boolean,
    onDetectLocation: () -> Unit,
) {
    Text(
        text = settings.city,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = String.format(Locale.US, "%.4f° ، %.4f°", settings.latitude, settings.longitude),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = onDetectLocation,
        enabled = !locating,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (locating) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
        }
        Text("تحديد موقعي تلقائياً")
    }
}

/** قسم طريقة الحساب — الطرق الأربع المعتمدة */
@Composable
private fun MethodSection(
    settings: NoorSettingsState,
    onSetMethod: (CalculationMethod) -> Unit,
) {
    CalculationMethod.entries.forEach { method ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            RadioButton(
                selected = settings.method == method,
                onClick = { onSetMethod(method) },
            )
            Column {
                Text(
                    text = method.arabicName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = method.englishName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** قسم مذهب العصر — الحنفي أو الجمهور */
@Composable
private fun AsrSection(
    settings: NoorSettingsState,
    onSetHanafi: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "المذهب الحنفي",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "ظل مثلهين — العصر متأخراً. الإيقاف = ظل مثله (الجمهور)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = settings.isHanafi, onCheckedChange = onSetHanafi)
    }
}

/** قسم الألوان — شبكة الألوان العشرين: الهادئة ثم الجريئة */
@Composable
private fun ColorsSection(
    settings: NoorSettingsState,
    onSetAccent: (String) -> Unit,
) {
    Text(
        text = "عشرة ألوان هادئة وعشرة جريئة — والتطبيق كله يتبع اختيارك في الوضعين",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(14.dp))
    AccentGrid(selectedId = settings.accentId, onPick = onSetAccent)
}

/** قسم التحديثات — النسخة الحالية والفحص اليدوي والتثبيت الداخلي */
@Composable
private fun UpdatesSection(
    updateState: UpdateUiState,
    currentVersion: String,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val busy = updateState is UpdateUiState.Checking || updateState is UpdateUiState.Downloading

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "النسخة الحالية",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = currentVersion,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.secondary,
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = updateStatusText(updateState),
        style = MaterialTheme.typography.bodySmall,
        color = when {
            updateState is UpdateUiState.Failed -> scheme.error
            updateState is UpdateUiState.Available || updateState is UpdateUiState.ReadyToInstall -> scheme.secondary
            else -> scheme.onSurfaceVariant
        },
    )
    if (updateState is UpdateUiState.Downloading) {
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { updateState.fraction },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onCheckUpdates,
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
        ),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = scheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
        }
        Text("تحقق من التحديثات الآن")
    }
    if (updateState is UpdateUiState.ReadyToInstall) {
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onInstallUpdate,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.secondary,
                contentColor = scheme.onSecondary,
            ),
        ) {
            Text("تثبيت الآن")
        }
    }
}

/** نص حالة التحديث بلغة مفهومة لكل حالة */
private fun updateStatusText(state: UpdateUiState): String = when (state) {
    is UpdateUiState.Idle -> "يبحث تلقائياً عند فتح التطبيق ومع كل نسخة جديدة يصلك إشعار"
    is UpdateUiState.Checking -> "جارٍ البحث عن تحديثات…"
    is UpdateUiState.Available -> "تحديث جديد متاح: ${state.info.versionName} — ${state.info.sizeText}"
    is UpdateUiState.Downloading -> "جارٍ تنزيل التحديث… ${(state.fraction * 100).toInt()}%"
    is UpdateUiState.ReadyToInstall -> "التحديث جاهز — اضغط تثبيت الآن"
    is UpdateUiState.Failed -> state.message
    is UpdateUiState.UpToDate -> "أنت على أحدث نسخة — لا يوجد تحديث جديد"
}

/**
 * شبكة الألوان العشرين — صفوف بخمس دوائر في الصف، دائرة لكل لون
 * بلونه النهاري واسمه عربياً تحته، والمختار بإطار مميز (توجيه المالك
 * alpha18 للعشرة الأولى وalpha28 للعشرة الجريئة المضافة).
 */
@Composable
private fun AccentGrid(selectedId: String, onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentPalette.All.chunked(5).forEach { rowAccents ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowAccents.forEach { accent ->
                    val selected = accent.id == selectedId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            onClick = { onPick(accent.id) },
                            shape = CircleShape,
                            color = accent.day.primary,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) scheme.secondary else scheme.outline.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = accent.day.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = accent.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) scheme.secondary else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
