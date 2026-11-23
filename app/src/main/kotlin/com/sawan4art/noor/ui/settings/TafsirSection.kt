package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.domain.tafsir.TafsirEditions

/**
 * قسم «التفاسير» — v1.82 (بلاغ المالك: «انا عايز التفاسير تكون مدمجه
 * داخل التطبيق نفسه مش قابله للتنزيل لا تكون موجوده بشكل افتراضي في
 * التطبيق و المستخدم يختار بينهم»):
 *
 * الطبعات الست كلها **مدمجةٌ في حزمة التطبيق** فلا تنزيلً ولا
 * تقدّماً ولا حذفاً — قائمةُ اختيارٍ نقيةٌ: ضغطةُ الصف تعتمد الطبعة
 * افتراضيةً للوحة التفسير في المصحف، والميسر الافتراضيُّ الأول،
 * والتبديل الفوري موجودٌ أيضاً داخل لوحة التفسير نفسها.
 */
@Composable
fun TafsirSection(
    currentEdition: String,
    onSetTafsirEdition: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "الطبعات الست كلها مدمجةٌ داخل التطبيق فتعمل فوراً بلا إنترنت ولا تنزيل — اختر الطبعة الافتراضية للوحة التفسير في المصحف، أو بدّل بينها أثناء القراءة من لوحة التفسير نفسها.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        TafsirEditions.ALL.forEach { ed ->
            val selected = currentEdition == ed.id
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) scheme.primary.copy(alpha = 0.08f)
                        else scheme.surface,
                    )
                    .border(
                        width = 1.dp,
                        color = scheme.outline.copy(alpha = if (selected) 0.45f else 0.28f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSetTafsirEdition(ed.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                // مؤشر الاختيار
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) scheme.primary
                            else scheme.outline.copy(alpha = 0.25f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(scheme.onPrimary),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ed.arabicName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                        )
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "الافتراضية",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(scheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "مدمجةٌ داخل التطبيق — جاهزةٌ فوراً بلا إنترنت",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
