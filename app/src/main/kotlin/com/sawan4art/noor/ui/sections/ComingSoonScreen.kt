package com.sawan4art.noor.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.ui.nav.NoorTab

/**
 * شاشة «قريبًا» — أقسام تُستعاد من النسخة القديمة في مراحل إعادة البناء
 * القادمة. مصممة بهوية «نور» نفسها كي لا يشعر المالك بكسر في الإيقاع البصري.
 */
@Composable
fun ComingSoonScreen(tab: NoorTab) {
    val info = sectionInfo(tab)
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.secondaryContainer),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = info.title,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = info.description,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer,
        ) {
            Text(
                text = "قريبًا بإذن الله",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(120.dp)) // مساحة شريط التنقل الزجاجي
    }
}

private data class SectionInfo(val title: String, val description: String)

private fun sectionInfo(tab: NoorTab): SectionInfo = when (tab) {
    NoorTab.QURAN -> SectionInfo(
        "المصحف الكريم",
        "٦٠٤ صفحات بالتشكيل الملون للتجويد، تلاوة صوتية للقرّاء، وختمات متابعة — يعود في مرحلة المصحف من إعادة البناء.",
    )
    NoorTab.QIBLA -> SectionInfo(
        "بوصلة القبلة",
        "تحديد اتجاه القبلة بدقة حسب موقعك الحالي مع بوصلة حية سلسة.",
    )
    NoorTab.ADHKAR -> SectionInfo(
        "الأذكار",
        "أذكار الصباح والمساء، وأذكار بعد الصلاة، مع عدّاد متابعة يومية.",
    )
    NoorTab.TASBIH -> SectionInfo(
        "المسبحة",
        "مسبحة إلكترونية بعدّاد واهتزاز خفيف وأهداف يومية.",
    )
    else -> SectionInfo("قريبًا", "هذا القسم في طريقه.")
}
