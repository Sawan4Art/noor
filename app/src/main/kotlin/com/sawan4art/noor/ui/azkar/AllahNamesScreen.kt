package com.sawan4art.noor.ui.azkar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.domain.azkar.AllahNames
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.theme.NoorPageTitle

/**
 * شاشة أسماء الله الحسنى (alpha61) — بلاغ المالك: «عايز زر فوق زر صلي
 * على النبي خاص باسماء الله الحسنى و بنفس الشكل». القائمة كاملة
 * (٩٩) بمعنىً موجزٍ لكل اسم، بمستند ثيم التطبيق نفسه: عنوانٌ داخل
 * إطار NoorPageTitle وبطاقاتٌ بألوان السطح وحدوده الهادئة.
 */
@Composable
fun AllahNamesScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = scheme.primary,
                )
            }
            NoorPageTitle(text = "أسماء الله الحسنى")
        }
        Text(
            text = AllahNames.HADITH,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 116.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AllahNames.ALL, key = { it.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = scheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = scheme.outline.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        // مقام الرقم — دائرة بلون الثيم كخلايا المواقيت
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.primary.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = MisbahaMath.arabicNumber(entry.id.toLong()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = entry.meaning,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
