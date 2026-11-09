package com.sawan4art.noor.ui.azkar

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.data.mushaf.MushafDb
import com.sawan4art.noor.domain.azkar.Ruqyah
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.ui.theme.NoorPageTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * شاشة الرقية الشرعية (alpha63) — بلاغ المالك: «عايز زر اضافي شبيه بزري
 * الصلاة على النبي و اسماء الله الحسنى تحت زر الصلاه علي النبي يكون خاص
 * بالرقيه الشرعيه المعتمده و الصحيحه».
 *
 * بلغة [AllahNamesScreen] نفسها: عنوانٌ داخل إطار NoorPageTitle ثم
 * بطاقاتٌ بألوان السطح — والآيات نصوصُها من قاعدة المصحف نفسها
 * (لا نصاً مكتوباً يدوياً قد يخطئ) والأدعية الثابتة بمصادرها.
 */
@Composable
fun RuqyahScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    // نصوص الآيات من قاعدة المصحف — قراءةٌ واحدة خلفيةً ثم ثابتة
    var verseTexts by remember { mutableStateOf<Map<Ruqyah.VerseRef, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        verseTexts = withContext(Dispatchers.IO) {
            Ruqyah.VERSES.associateWith { ref ->
                (ref.from..ref.to).joinToString("\n") { ayah ->
                    val text = MushafDb.ayahText(context, ref.sura, ayah).orEmpty()
                    "$text ﴿${MushafSurahs.arabicDigits(ayah)}﴾"
                }
            }
        }
    }

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
            NoorPageTitle(text = "الرقية الشرعية")
        }
        Text(
            text = "الرقية بالكتاب والسنة — بشروطها الثلاثة، وبقدر الله تعالى",
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Ruqyah.BLOCKS, key = { it.id }) { block ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = scheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = scheme.outline.copy(alpha = 0.25f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = block.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = block.subtitle,
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant,
                            lineHeight = 17.sp,
                        )
                        Spacer(Modifier.height(8.dp))

                        // الآيات من المصحف
                        block.verses.forEach { ref ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(scheme.primary.copy(alpha = 0.07f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Column {
                                    Text(
                                        text = ref.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.primary,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = verseTexts[ref] ?: "…",
                                        fontSize = 17.sp,
                                        lineHeight = 30.sp,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // الأدعية الثابتة بمصادرها
                        block.duas.forEach { dua ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(scheme.surfaceVariant.copy(alpha = 0.22f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Column {
                                    Text(
                                        text = dua.text,
                                        fontSize = 15.sp,
                                        lineHeight = 25.sp,
                                        color = scheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = dua.source,
                                        fontSize = 11.sp,
                                        color = scheme.secondary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
