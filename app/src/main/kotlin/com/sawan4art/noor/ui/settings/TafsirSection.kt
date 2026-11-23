package com.sawan4art.noor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.data.tafsir.TafsirLibrary
import com.sawan4art.noor.domain.tafsir.TafsirEditions

/**
 * قسم «التفاسير» — v1.81 (بلاغ المالك: «اضف التفاسير التاليه الجلالين
 * و القرطبي و ابن كثير الطبري السعدي و طبعا مع ابقاء التفسير الميسر
 * مع امكانيه تغيير طبعه التفسير من الاعدادات و حطلها قسم جديد باسم
 * التفاسير»):
 *
 * • **الميسر باقٍ** — مضمّنٌ في التطبيق جاهزٌ فوراً وبلا إنترنت وهو
 *   الافتراضي، ولا تُحذف قاعدته أبداً (تظهر «مضمّنة» بلا أزرار).
 * • **الخمس الأخرى** — لكل طبعة صفٌّ بحجمها وشعارها: زر تنزيلٍ
 *   (بشريط تقدمٍ حيٍّ من [TafsirLibrary.downloads] واستئنافٍ من
 *   موجده) وزر حذفٍ بعد الاكتمال.
 * • **اختيار الطبعة** — ضغطة الصف المنزَّل (أو المضمّن) تختاره
 *   طبعةً افتراضيةً للوحة التفسير في المصحف، والضغطة على غير
 *   المنزَّل تبدأ تنزيله وبعد اكتماله ضغطةٌ أخرى تعتمده.
 */
@Composable
fun TafsirSection(
    currentEdition: String,
    onSetTafsirEdition: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val downloads by TafsirLibrary.downloads.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column {
        Text(
            text = "اختر الطبعة الافتراضية للوحة التفسير في المصحف — والميسر مضمّنٌ جاهزٌ بلا إنترنت، والبقية تُنزَّل مرةً واحدةً فتعمل بعدها دائماً دون اتصال.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        TafsirEditions.ALL.forEach { ed ->
            val dl = downloads[ed.id]
            val downloaded = TafsirLibrary.isDownloaded(context, ed.id)
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
                    .clickable {
                        when {
                            // المضمّنة تُختار فوراً
                            ed.bundled -> onSetTafsirEdition(ed.id)
                            // المنزَّلة تُختار فوراً
                            downloaded && dl?.running != true -> onSetTafsirEdition(ed.id)
                            // الجارية تُتجاهل الضغطة حتى تكتمل
                            dl?.running == true -> Unit
                            // غير المنزَّلة تبدأ التنزيل وتُعتمد عند اكتمالها
                            else -> TafsirLibrary.start(context, ed.id)
                        }
                    }
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
                        Spacer(Modifier.width(8.dp))
                        if (ed.bundled) {
                            Text(
                                text = "مضمّنة",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(scheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        } else if (selected) {
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
                    val status = when {
                        dl?.running == true -> "جارٍ التنزيل… ${(dl.fraction * 100).toInt()}٪"
                        dl?.failedMessage != null -> dl.failedMessage
                        ed.bundled -> "جاهزة فوراً بلا إنترنت — افتراضية التطبيق"
                        downloaded -> "منزَّلة — تعمل بلا إنترنت • ${TafsirEditions.sizeLabel(ed.sizeBytes)}"
                        else -> "غير منزَّلة • ${TafsirEditions.sizeLabel(ed.sizeBytes)} — الضغطة تبدأ التنزيل"
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    if (dl?.running == true) {
                        LinearProgressIndicator(
                            progress = { dl.fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = scheme.primary,
                            trackColor = scheme.outline.copy(alpha = 0.2f),
                        )
                    }
                }
                if (!ed.bundled && downloaded && dl?.running != true) {
                    Text(
                        text = "حذف",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { TafsirLibrary.remove(context, ed.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                if (dl?.running == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = scheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = "مصادر النصوص: التفسير الميسر والجلالين والقرطبي وابن كثير والطبري والسعدي — نسخٌ متحققةٌ بتغطيةٍ كاملةٍ للقرآن (٦٢٣٦ آية) وتُفحص بصمةُ كل حزمةٍ بعد التنزيل قبل اعتمادها.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
