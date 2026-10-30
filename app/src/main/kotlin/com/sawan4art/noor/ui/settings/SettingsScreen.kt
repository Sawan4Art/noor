package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.BuildConfig
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.domain.prayer.CalculationMethod
import java.util.Locale

/**
 * إعدادات المرحلة الأولى: الموقع + طريقة الحساب + مذهب العصر + المظهر.
 * إعدادات التنبيهات تصل مع مرحلة الأذان (المرحلة الرابعة).
 */
@Composable
fun SettingsScreen(
    settings: NoorSettingsState,
    locating: Boolean,
    onBack: () -> Unit,
    onDetectLocation: () -> Unit,
    onSetMethod: (CalculationMethod) -> Unit,
    onSetHanafi: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "الإعدادات",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
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

        SettingsCard(title = "المظهر") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeButton(
                    label = "النظام",
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    modifier = Modifier.weight(1f),
                ) { onSetThemeMode(ThemeMode.SYSTEM) }
                ThemeButton(
                    label = "نهاري",
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                ) { onSetThemeMode(ThemeMode.LIGHT) }
                ThemeButton(
                    label = "ليلي",
                    selected = settings.themeMode == ThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                ) { onSetThemeMode(ThemeMode.DARK) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "نور المسلم ${BuildConfig.VERSION_NAME} — إعادة البناء الأصلية بلغة كوتلن",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
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

@Composable
private fun ThemeButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scheme.secondaryContainer else scheme.surfaceVariant,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
    }
}
