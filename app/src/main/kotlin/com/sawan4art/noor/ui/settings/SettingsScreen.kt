package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.BuildConfig
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.ui.theme.AccentPalette
import java.util.Locale

/**
 * إعدادات المرحلة الأولى: الموقع + طريقة الحساب + مذهب العصر + ألوان التطبيق + التحديثات.
 * صارت تبويبًا في الشريط الزجاجي — زر الرجوع لم يعد له معنى.
 * alpha18: إعداد المظهر أُزيل (المزلاج في الرئيسية هو المتحكم بالوضعين
 * بطلب المالك) ومحله بطاقة ألوان التطبيق الجديدة بعشرة ألوان.
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
        Spacer(Modifier.height(16.dp))

        SettingsCard(title = "الموقع") {
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

        SettingsCard(title = "طريقة حساب المواقيت") {
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

        SettingsCard(title = "مذهب العصر") {
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

        SettingsCard(title = "ألوان التطبيق") {
            AccentGrid(selectedId = settings.accentId, onPick = onSetAccent)
        }

        UpdatesCard(
            updateState = updateState,
            currentVersion = BuildConfig.VERSION_NAME,
            onCheckUpdates = onCheckUpdates,
            onInstallUpdate = onInstallUpdate,
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

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * بطاقة التحديثات — النسخة الحالية + زر فحص يدوي بلا سقف زمني،
 * وحالة السلسلة الكاملة: فحص → تنزيل → جاهز للتثبيت داخلياً.
 */
@Composable
private fun UpdatesCard(
    updateState: UpdateUiState,
    currentVersion: String,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val busy = updateState is UpdateUiState.Checking || updateState is UpdateUiState.Downloading

    SettingsCard(title = "التحديثات") {
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
 * شبكة الألوان العشرة — صفّان بخمس دوائر في الصف، دائرة لكل لون
 * بلونه النهاري واسمه عربياً تحته، والمختار بإطار مميز (توجيه المالك alpha18).
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
