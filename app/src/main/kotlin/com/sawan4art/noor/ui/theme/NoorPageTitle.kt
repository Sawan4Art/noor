package com.sawan4art.noor.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * عنوان الصفحات داخل إطار (alpha39 — بلاغ المالك: «عايز كل عناوين الصفحات
 * تكون داخل اطار متماشي مع تصميم التطبيق»):
 *
 * إطار مستدير الزوايا بخلفية من غسلة تدرّج عائلة السوائل نفسها
 * (primaryContainer ← secondaryContainer بشفافية خفيفة) وحاشية رقيقة بلون
 * الحدود — لغة بطاقات الأقسام والصناديق المتدرّجة ذاتها، فينسجم العنوان مع
 * هوية التطبيق بدل نصٍ عارٍ فوق الصفحة. العرض يلتف على النص (يلتصق ببدايته
 * في RTL) ولا يمتد لعرض الشاشة.
 */
@Composable
fun NoorPageTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        scheme.primaryContainer.copy(alpha = 0.32f),
                        scheme.secondaryContainer.copy(alpha = 0.32f),
                    ),
                ),
                shape = TitleFrameShape,
            )
            .border(
                width = 1.dp,
                color = scheme.outline.copy(alpha = 0.30f),
                shape = TitleFrameShape,
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** زاوية الإطار واحدة في الخلفية والحدّ كي لا يختلف انحناؤهما بكسلاً */
private val TitleFrameShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
