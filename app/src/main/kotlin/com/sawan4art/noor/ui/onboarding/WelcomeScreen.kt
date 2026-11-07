package com.sawan4art.noor.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.theme.LiquidDeveloperBadge
import kotlinx.coroutines.launch

/**
 * شاشة الترحيب — توجيه المالك ٢٠٢٦-١١-٠٧:
 * «عايز شاشه ترحيب في اول مره يتم فتح فيها التطبيق تعرف المستخدم
 * بطريقه استخدام التطبيق و بمميزاته بكل بساطه و طبعا بدون عرض اي شئ
 * يخص الكود… بشكل احترافي متناسق مع تصميم التطبيق و يكون فيها زر
 * تخطي و طبعا الشاشه دي هيكون فيها اسم المطور في اخر الصفحه بنفس
 * الشكل الموجود حاليا في الاعدادات».
 *
 * خمس صفحات تنقَّل بالسحب أو بالزر، يألف فيها المستخدم نفسه بخصائص
 * التطبيق كلها بلغة الناس لا لغة التقنية — وآخر صفحة تحمل توقيع
 * المطور بإطاره السائل نفسه من الإعدادات وزرَّ الشكاوى والمقترحات
 * وزرَّ البدء. «تخطي» يعلو الصفحات من أولها، وما اكتملت بالبدء أو
 * التخطي خُزّن الأمر فلا تعود للظهور أبداً.
 */

/** صفحة واحدة من الترحيب — أيقونة فئةٍ وعنوان وثلاث فوائد */
private data class WelcomePage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val points: List<String>,
)

private fun pages(): List<WelcomePage> = listOf(
    WelcomePage(
        icon = NoorIcons.Mosque,
        title = "أهلاً بك في نور المسلم",
        subtitle = "رفيقك اليومي في الصلاة والقرآن والذكر — كل شيء في تطبيق واحد وبدون إنترنت في أكثره.",
        points = listOf(
            "مواقيت صحيحة بمدينتك بدقة الثانية",
            "أذان وإقامة بصوت مختار وتنبيه فوق القفل",
            "مظهر بلونك المفضل من عشرين لوناً",
        ),
    ),
    WelcomePage(
        icon = NoorIcons.PrayerTimes,
        title = "مواقيت وأذان على مسؤولية",
        subtitle = "العدّاد الحي يريك الوقت المتبقي حتى الأذان، والفواصل بين الأذان والإقامة بيدك لكل صلاة.",
        points = listOf(
            "أذان بصوت الشيخ الذي تختاره أو صوت من هاتفك",
            "كتم أصوات الهاتف بعد الإقامة والعودة تلقائياً",
            "زر خطبة الجمعة يوم الجمعة بخطب جاهزة وكتابة",
        ),
    ),
    WelcomePage(
        icon = NoorIcons.Mushaf,
        title = "مصحف كامل في جيبك",
        subtitle = "٦٠٤ صفحات بترقيم المدينة وتلوين التجويد، بخطٍ واضح وزومٍ بإصبعيك — والصفحة تُذكرك مكمنك.",
        points = listOf(
            "حدد آيات مع بعضها وشاركها وانسخها",
            "تفسير ميسر وعلامة مرجعية وحفظ ميسر للآيات",
            "عشرون شيخاً — نزّل صوت من تحب واسمع بلا إنترنت",
        ),
    ),
    WelcomePage(
        icon = NoorIcons.Adhkar,
        title = "ذكرٌ لا ينقطع",
        subtitle = "أذكار الصباح والمساء والصلاة، ومسبحة سائلة كبيرة، ومكتبة أحاديث بعشرة كتب تبحث فيها برقم الحديث أو كلمة.",
        points = listOf(
            "أذكار موقّتة تذكّرك في وقتها",
            "مسبحة بعدّاد كبير واهتزاز لطيف",
            "قبلة بدقة من موقعك ولو كنت في سفر",
        ),
    ),
    WelcomePage(
        icon = NoorIcons.Palette,
        title = "كما تحب أنت",
        subtitle = "لون التطبيق واللون الأساسي من عشرين لوناً، ومظهر ليلي بنقلة شمسٍ وقمر، وويدجتات خمسة للشاشة الرئيسية بهويّتك.",
        points = listOf(
            "ويدجت مواقيت وويدجت تاريخ وويدجت ذكر — كلها قابلة للتخصيص",
            "تحديثات التطبيق تصلك من داخله مباشرة",
            "كل شيء بلا حساب ولا تسجيل — فوراً وبساطة",
        ),
    ),
)

@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = remember { pages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer.copy(alpha = 0.55f), scheme.surface),
                ),
            )
            .statusBarsPadding(),
    ) {
        // تخطي — يعلو من أول صفحة
        Text(
            text = "تخطي",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onFinish() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { index ->
                WelcomePageContent(pages[index])
            }

            // النقاط
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 14.dp),
            ) {
                repeat(pages.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) scheme.primary
                                else scheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )
                }
            }

            Button(
                onClick = {
                    if (isLast) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (isLast) "ابدأ الآن" else "التالي",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // الصفحة الأخيرة تحمل توقيع المطور وزر الشكاوى —
            // بنفس شكليهما في الإعدادات (توجيه المالك الحرفي)
            if (isLast) {
                Spacer(Modifier.height(18.dp))
                LiquidDeveloperBadge(
                    name = "Yousef Sawan",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.35f)),
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:yousefsawan940@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "شكاوى ومقترحات — نور المسلم")
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "شكاوى ومقترحات")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 18.dp),
                ) {
                    Text(
                        text = "شكاوى ومقترحات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
            } else {
                Spacer(Modifier.height(56.dp))
            }
        }
    }
}

/** محتوى الصفحة — أيقونة كبيرة في إطار وعنوان وفوائد */
@Composable
private fun WelcomePageContent(page: WelcomePage) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        page.points.forEach { point ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }
        }
    }
}
