package com.sawan4art.noor.ui.notice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * حوار الإشعار الموحد — **إشعارات التطبيق الداخلية بلغة واحدة ولون الثيم**
 * (بلاغ المالك alpha19: «الاشعارات التفاعلية والغير التفاعلية ليست بنفس لون
 * الثيم ولا بنفس التصميم»).
 *
 * الجذر: حوارات بوابات الموقع كانت AlertDialog افتراضياً — شكل نظام مختلف
 * عن حوار التحديث المصمم يدوياً، وبألوان ليست من هوية اللون المختار.
 * العلاج: **مصمم واحد** لكل إشعارات التطبيق — نفس لغة UpdateDialog حرفياً:
 * سطح بزوايا 26dp بلون الثيم، شريحة أيقونة بلون الحاوية الثانوية، عنوان
 * عريض، وزر إجراء أساسي ممتلئ بلون الثيم (الإشعار التفاعلي) وزر «حسناً»
 * (الإشعار غير التفاعلي) — فالإشعاران يصيران أخوين من نفس القالب.
 */
@Composable
fun NoorNoticeDialog(
    title: String,
    message: String,
    icon: ImageVector,
    iconDescription: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    dismissLabel: String = "حسناً",
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(22.dp)) {
                // الترويسة: شريحة الأيقونة + العنوان — نفس لغة حوار التحديث
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = iconDescription,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.padding(top = 14.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                )

                actionLabel?.takeIf { onAction != null }?.let { label ->
                    Spacer(Modifier.padding(top = 16.dp))
                    Button(
                        onClick = onAction!!,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(label)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = dismissLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
