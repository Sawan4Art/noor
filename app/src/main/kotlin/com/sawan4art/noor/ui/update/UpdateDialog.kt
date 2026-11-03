package com.sawan4art.noor.ui.update

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.data.update.ChangelogFormatter
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.ui.icons.NoorIcons

/**
 * حوار التحديث — ملخص «ما الجديد» وزر «تحديث الآن».
 *
 * **alpha46**: زر التحديث يفتح ملف الـ APK في المتصفح مباشرة — التنزيل
 * والتثبيت في الطريق الذي يعرفه كل مستخدم، والتطبيق بلا أي صلاحية تثبيت
 * فلا يصنّفه فحص جوجل ضمن الأنماط الخطرة (بلاغ المالك: «فحص جوجل بيكتشف
 * احتمال ان يكون التطبيق مضر و ده بيخوف الناس»). وتحت الزر سطر تطمينٍ
 * هادئ لمن يواجه رسالة فحص جوجل عند تثبيت الملف لأول مرة.
 *
 * يظهر تلقائياً مع كل نسخة جديدة مرة واحدة، والاستبعاد «لاحقاً» يبقى
 * حتى تصدر بعدها نسخة.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: (versionName: String) -> Unit,
    onUpdate: (apkUrl: String) -> Unit,
    onRetry: () -> Unit,
) {
    val info = when (state) {
        is UpdateUiState.Available -> state.info
        is UpdateUiState.Failed -> state.info
        else -> null
    } ?: return

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
                        PrimaryButton(
                            text = "تحديث الآن",
                            onClick = { onUpdate(info.apkUrl) },
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Text(
                            text = "يفتح التحديث في المتصفح — وإن سألك جوجل عند " +
                                "التثبيت فالملف من موقع التطبيق الرسمي، اختر «التثبيت على أي حال»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is UpdateUiState.Failed -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.padding(top = 10.dp))
                        PrimaryButton(text = "إعادة المحاولة", onClick = onRetry)
                    }
                    else -> Unit
                }

                TextButton(
                    onClick = { onDismiss(info.versionName) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "لاحقاً",
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
