package com.sawan4art.noor.ui.mushaf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sawan4art.noor.domain.mushaf.MushafPages
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * المصحف — رواية حفص مجوّد بالتشكيل الملون للتجويد (alpha49).
 *
 * توجيه المالك ٢٠٢٦-١١-٠٥: «غير المصحف تماما انا عايز راويه حفص مجود
 * زي الصوره كده و يكون عرض صفحات المصحف بكامل الشاشه مع زر فوق على
 * اليمين للرجوع و يكون زر صغير»:
 *
 * • صفحات المصحف الملون بالتجويد نفسه (٦٠٤ صفحة بترقيم المدنة القياسي)
 *   تُعرض كاملة على خلفية داكنة بلا أي إطار أو هامش — قلب ورق حقيقي:
 *   السحب يجلب الصفحة التالية من اليسار كما في قلب ورق المصحف العربي
 *   (اتجاه RTL المثبت للشاشة يقود هذا السلوك تلقائياً).
 * • زر رجوع صغير (٣٤dp) أعلى اليمين فوق الصفحة — مع مساحة لمس أكبر
 *   من شكله حتى لا يخطئ الإصبع.
 * • الصفحة تُحمَّل من الشبكة عند أول مشاهدة وتُحفظ في ذاكرة القرص
 *   فتعمل بعد ذلك بلا إنترنت، والصفحة المجاورة تُحمَّل مسبقاً كي
 *   يكون القلب سلساً بلا انتظار.
 * • فشل الشبكة الأساسي يجرب المصدر الاحتياطي تلقائياً، وما زال
 *   الفشل يعرض رسالة خفيفة بضغطة إعادة محاولة.
 * • آخر صفحة تُحفظ في البيانات فيفتح المصحف مكمنه الذي تركه القارئ.
 */
@Composable
fun MushafScreen(
    /** آخر صفحة محفوظة (١..٦٠٤) — نقطة بداية المصحف */
    initialPage: Int,
    /** حفظ آخر صفحة وصل إليها القارئ */
    onSavePage: (Int) -> Unit,
    /** زر الرجوع الصغير — يعود إلى الرئيسية */
    onBack: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = MushafPages.clampPage(initialPage) - 1,
        pageCount = { MushafPages.PAGE_COUNT },
    )

    // حفظ آخر صفحة عند وصول القارئ إليها — المصحف يظل مكمنه الذي تركه
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onSavePage(page + 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            // الصفحة المجاورة تُحمَّل مسبقاً كي يقلب القارئ بلا انتظار
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            MushafPage(page = index + 1)
        }

        // زر الرجوع الصغير — أعلى اليمين فيزيائياً: TopStart في الاتجاه
        // RTL المثبت للتطبيق هو الزاوية اليمنى العليا كما طلب المالك،
        // وشكله ٣٤dp صغير فوق الصفحة ومساحة لمسه ٤٨dp مريحة للإصبع
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 4.dp)
                .size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.38f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع إلى الرئيسية",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * صفحة واحدة من المصحف — صورة الصفحة الملونة بكامل الشاشة مع
 * تسلسل الشبكة: الأساسي ← الاحتياطي بصمت ← رسالة إعادة المحاولة.
 */
@Composable
private fun MushafPage(page: Int) {
    // ٠ = الرابط الأساسي، ١ = الاحتياطي، ٢+ = إعادة محاولة بالتناوب
    var attempt by remember(page) { mutableIntStateOf(0) }
    var failed by remember(page) { mutableStateOf(false) }
    var loading by remember(page) { mutableStateOf(true) }

    val context = LocalContext.current
    val model = remember(page, attempt) {
        val url = if (attempt % 2 == 0) {
            MushafPages.primaryUrl(page)
        } else {
            MushafPages.fallbackUrl(page)
        }
        ImageRequest.Builder(context)
            .data(url)
            // معامل المحاولة يفصل مفتاح الطلب فتعيد الضغطة الشبكة فعلاً
            .setParameter("attempt", attempt)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = "صفحة $page من المصحف بالتجويد الملون",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onLoading = {
                loading = true
                failed = false
            },
            onSuccess = {
                loading = false
                failed = false
            },
            onError = {
                // فشل الرابط الأساسي → المصدر الاحتياطي فوراً بصمت،
                // وفشل هو الآخر → رسالة الإعادة بضغطة تُبدّل المحاولة
                if (attempt == 0) {
                    attempt = 1
                } else {
                    loading = false
                    failed = true
                }
            },
        )

        if (loading && !failed) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Color.White.copy(alpha = 0.75f),
                strokeWidth = 2.dp,
            )
        }

        if (failed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { attempt += 1 },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "تعذّر تحميل الصفحة $page",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "تأكد من الاتصال ثم اضغط للمحاولة من جديد",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
