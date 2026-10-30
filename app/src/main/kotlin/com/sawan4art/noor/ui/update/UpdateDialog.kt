package com.sawan4art.noor.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sawan4art.noor.data.update.ChangelogFormatter
import com.sawan4art.noor.data.update.UpdateInstaller
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.ui.icons.NoorIcons
import java.util.Locale

/**
 * حوار التحديث — ملخص «ما الجديد» مع سلسلة التنزيل والتثبيت كاملة داخله
 * (طلب المالك: التثبيت داخل التطبيق بلا متصفح). يظهر تلقائياً مع كل
 * نسخة جديدة مرة واحدة، والاستبعاد «لاحقاً» يبقى حتى تصدر بعدها نسخة.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: (versionName: String) -> Unit,
    onDownload: () -> Unit,
    onInstall: (apkPath: String) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val info = when (state) {
        is UpdateUiState.Available -> state.info
        is UpdateUiState.Downloading -> state.info
        is UpdateUiState.ReadyToInstall -> state.info
        is UpdateUiState.Failed -> state.info
        else -> null
    } ?: return

    // إذن التثبيت يُعاد فحصه عند العودة من صفحة الإعداد — بضغطة واحدة يكمل
    var canInstall by remember { mutableStateOf(UpdateManager.canRequestInstall) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canInstall = UpdateManager.canRequestInstall
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = { onDismiss(info.versionName) }) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(22.dp)) {
                // الترويسة: أيقونة + العنوان + رقم النسخة
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = NoorIcons.Update,
                            contentDescription = "تحديث",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "تحديث جديد متاح",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "النسخة ${info.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                Spacer(Modifier.padding(top = 14.dp))
                Text(
                    text = "ما الجديد",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.padding(top = 6.dp))

                // ملخص ما الجديد — قابل للتمرير داخل الحوار إن طال
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    val lines = ChangelogFormatter.bodyToLines(info.body)
                    if (lines.isEmpty()) {
                        Text(
                            text = "إصلاحات وتحسينات عامة",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        lines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    text = "الحجم: ${info.sizeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.padding(top = 16.dp))
                when (state) {
                    is UpdateUiState.Available -> {
                        PrimaryButton(text = "تنزيل التحديث", onClick = onDownload)
                    }
                    is UpdateUiState.Downloading -> {
                        val fraction = state.fraction
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            text = progressText(state.receivedBytes, state.totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        PrimaryButton(
                            text = if (canInstall) "تثبيت الآن" else "تفعيل التثبيت ثم التثبيت",
                            onClick = {
                                if (canInstall) onInstall(state.apk)
                                else UpdateInstaller.openInstallPermissionSettings(context)
                            },
                        )
                    }
                    is UpdateUiState.Failed -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.padding(top = 10.dp))
                        PrimaryButton(
                            text = if (state.info != null) "إعادة المحاولة" else "حاول من جديد",
                            onClick = onRetry,
                        )
                    }
                    else -> Unit
                }

                TextButton(
                    onClick = { onDismiss(info.versionName) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (state is UpdateUiState.Downloading) "المتابعة في الخلفية" else "لاحقاً",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text)
    }
}

/** نص التقدم: «3.2 من 7.1 م.ب» */
private fun progressText(received: Long, total: Long): String {
    val format = { bytes: Long ->
        val mb = bytes / (1024.0 * 1024.0)
        if (mb >= 1.0) String.format(Locale.US, "%.1f م.ب", mb)
        else String.format(Locale.US, "%.0f ك.ب", bytes / 1024.0)
    }
    return if (total > 0) "${format(received)} من ${format(total)}" else format(received)
}
