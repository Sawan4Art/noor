package com.sawan4art.noor.ui.hadith

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.data.hadith.BookEntity
import com.sawan4art.noor.data.hadith.HadithEntity
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.theme.NoorPageTitle

/** مدى تكبير خط بطاقات الأحاديث وصفحة القراءة — أرضية لضعاف البصر وسقفٌ معقول */
private const val FONT_SCALE_MIN = 0.85f
private const val FONT_SCALE_MAX = 2f
private const val FONT_SCALE_STEP = 0.15f

/** خط صفحة القراءة بملء الشاشة — أساسه أكبر من الكارت (بلاغ المالك alpha44) */
private const val READER_BASE_SP = 20f

/**
 * تبويب مكتبة الأحاديث — توجيه المالك («عايزين نضيف قسم جديد لـ مكتبة
 * الأحاديث والبحث الأوفلاين»):
 *
 * - **شريط بحث فوق**: بحث لحظي أوفلاين على فهرس FTS4 — برقم الحديث أو
 *   بكلمة أو جزء جملة من نصّه (alpha38)، ويفهم التشكيل والهمزات
 *   (HadithSearch يطبّع الاستعلام كما طُبّع الفهرس نفسه).
 * - **فلترة حسب الكتاب**: صف شرائح أفقية بعشرة كتب + شريحة «الكل»
 *   + شريحة «المفضلة» بنجمة (alpha38) تفتح قائمة المحفوظ.
 * - **قائمة الأحاديث** بتصميم كروت الأذكار نفسه: Surface بحاشية
 *   outline، شارة الكتاب والرقم على نمط عدّاد الأذكار، ونص الحديث
 *   bodyLarge — والحديث الطويل يُطوى ويُفرد بلمسة (مثل بطاقة الذكر)،
 *   وأسفل كل كارت نجمة الحفظ وزر المشاركة (alpha38).
 */
@Composable
fun HadithScreen(viewModel: HadithViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // القراءة بلغت آخر القائمة — عند الاقتراب يُحمَّل صف التالي
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.items.isNotEmpty() && last >= state.items.size - 6
        }
    }
    LaunchedEffect(nearEnd, state.items.size) {
        if (nearEnd) viewModel.loadMore()
    }
    // نتائج جديدة (بحث/فلتر) — القائمة تبدأ من أعلى
    LaunchedEffect(state.resetToken) {
        if (state.resetToken > 0) listState.scrollToItem(0)
    }

    val bookTitles = remember(state.books) {
        state.books.associate { it.id to it.title }
    }
    val context = LocalContext.current
    // صفحة القراءة بملء الشاشة (alpha44 — بلاغ المالك: «عايز زر اضافي في كل
    // كارت من الاحاديث يمكن المستخدم من فتح الحديث في صفحه كامل بخط اكبر
    // يناسب كامل الصفحه لقراءه افضل»)
    var readingHadith by remember { mutableStateOf<HadithEntity?>(null) }
    val shareHadith: (HadithEntity, String) -> Unit = { hadith, title ->
        // نص المشاركة: الحديث ثم مصدره ثم توقيع التطبيق — مقروء في أي وسيط مشاركة
        val text = buildString {
            append('«').append(hadith.text).append('»')
            append("\n\n")
            append(title)
            append(" · حديث ")
            append(MisbahaMath.arabicNumber(hadith.numberInBook.toLong()))
            append("\n\n")
            append("من تطبيق نور المسلم")
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    "مشاركة الحديث",
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // alpha39: العنوان داخل إطار NoorPageTitle (توجيه المالك:
        // «عايز كل عناوين الصفحات تكون داخل اطار متماشي مع تصميم التطبيق»)
        NoorPageTitle(text = "الأحاديث")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "عشرة كتب من السنة الشريفة أوفلاين — ابحث برقم الحديث أو بكلمة من نصّه",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        HadithSearchField(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
        )
        Spacer(Modifier.height(10.dp))

        BookFilterChips(
            books = state.books,
            selectedBookId = state.selectedBookId,
            favoritesMode = state.favoritesMode,
            onSelect = viewModel::onBookSelect,
            onFavorites = viewModel::onFavoritesToggle,
        )
        Spacer(Modifier.height(10.dp))

        // alpha40: تنبيه البحث بالرقم — الأرقام ترقيمٌ داخل كل كتاب، فعند
        // كتابة رقمٍ من غير اختيار كتابٍ يُنبَّه المستخدم أن يختار الكتاب أولًا
        if (state.isNumberSearch && state.selectedBookId == null && !state.favoritesMode) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = scheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "أرقام الأحاديث لكل كتابٍ على حِدة — اختر الكتاب من الشرائح ثم ابحث بالرقم",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(
            text = when {
                state.isSearching && state.capped ->
                    "أول " + MisbahaMath.arabicNumber(state.items.size.toLong()) +
                        " نتيجة — جرّب كلمات أدق"
                state.isSearching ->
                    "النتائج: " + MisbahaMath.arabicNumber(state.items.size.toLong())
                state.favoritesMode ->
                    "المحفوظة: " + MisbahaMath.arabicNumber(state.totalCount.toLong())
                else ->
                    MisbahaMath.arabicNumber(state.totalCount.toLong()) + " حديثاً"
            },
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = scheme.primary)
            }
            state.failed -> FailedState(onRetry = viewModel::retry, modifier = Modifier.weight(1f))
            state.items.isEmpty() && state.favoritesMode ->
                EmptyFavorites(modifier = Modifier.weight(1f))
            state.items.isEmpty() -> EmptyState(searching = state.isSearching, modifier = Modifier.weight(1f))
            else -> LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.items, key = { it.id }) { hadith ->
                    HadithCard(
                        hadith = hadith,
                        bookTitle = bookTitles[hadith.bookId].orEmpty(),
                        isFavorite = hadith.id in state.favoriteIds,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onShare = shareHadith,
                        onRead = { readingHadith = hadith },
                    )
                }
                if (state.canLoadMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = scheme.primary,
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(100.dp)) // مساحة شريط التنقل الزجاجي
                }
            }
        }
    }

    // صفحة القراءة بملء الشاشة — فوق كل شيء (alpha44)
    readingHadith?.let { hadith ->
        FullScreenHadithReader(
            hadith = hadith,
            bookTitle = bookTitles[hadith.bookId].orEmpty(),
            onDismiss = { readingHadith = null },
        )
    }
}

/** شريط البحث — نفس هوية الكروت: Surface بحاشية outline وأيقونة هادئة */
@Composable
private fun HadithSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "ابحث في الأحاديث…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "مسح البحث",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** صف فلترة الكتب — شرائح أفقية، المختار بلون الحاوية كعائلة الثيم؛
 *  وشريحة «المفضلة» بنجمة في أول الصف (alpha38) تفتح قائمة المحفوظ */
@Composable
private fun BookFilterChips(
    books: List<BookEntity>,
    selectedBookId: Long?,
    favoritesMode: Boolean,
    onSelect: (Long?) -> Unit,
    onFavorites: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = "المفضلة",
            selected = favoritesMode,
            onClick = onFavorites,
            star = true,
        )
        FilterChip(
            label = "الكل",
            selected = !favoritesMode && selectedBookId == null,
            onClick = { onSelect(null) },
        )
        books.forEach { book ->
            FilterChip(
                label = book.title,
                selected = !favoritesMode && selectedBookId == book.id,
                onClick = { onSelect(book.id) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    star: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) scheme.primary else scheme.outline.copy(alpha = 0.28f),
        ),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
            if (star) {
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = if (selected) NoorIcons.StarFilled else NoorIcons.StarOutline,
                    contentDescription = null,
                    tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * بطاقة حديث — على هيئة بطاقة الأذكار تماماً: Surface بحاشية، شارة
 * الكتاب والرقم على نمط عدّاد الأذكار، واسم الباب في الطرف المقابل،
 * ونص الحديث الذي يُطوى بلمسة إن طال (هيئة الأطوال متفاوتة في البخاري)،
 * وأسفلها صف الحفظ والمشاركة (alpha38 توجيه المالك: «اضف زر مشاركه لكل
 * كارت من الاحاديث و زر نجمه للحفظ في قائمه المفضله») — النجمة تمتلئ
 * بلون التطبيق حين يُحفَظ الحديث، والمشاركة تبعث نصّه بمصدره.
 */
@Composable
private fun HadithCard(
    hadith: HadithEntity,
    bookTitle: String,
    isFavorite: Boolean,
    onToggleFavorite: (Long) -> Unit,
    onShare: (HadithEntity, String) -> Unit,
    onRead: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }
    // حجم خط هذه البطاقة — يتذكره التطبيق أثناء التنقل (alpha44)
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.3f)),
        onClick = { if (truncated) expanded = !expanded },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.surfaceVariant,
                    contentColor = scheme.onSurfaceVariant,
                ) {
                    Text(
                        text = bookTitle + " · " +
                            MisbahaMath.arabicNumber(hadith.numberInBook.toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                if (hadith.chapter.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = hadith.chapter,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = hadith.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontScale).sp,
                ),
                color = scheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 7,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!expanded) truncated = result.hasVisualOverflow
                },
            )
            if (truncated && !expanded) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "اضغط لقراءة الحديث كاملاً",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onToggleFavorite(hadith.id) },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) NoorIcons.StarFilled else NoorIcons.StarOutline,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "حفظ في المفضلة",
                        tint = if (isFavorite) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(23.dp),
                    )
                }
                IconButton(
                    onClick = { onShare(hadith, bookTitle) },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "مشاركة الحديث",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                // أزرار تكبير وتصغير الخط لهذه البطاقة (alpha44) —
                // alpha45: أيقونات «+» و«−» صافية بلا حرف (بلاغ المالك:
                // «ازل حرف أ من علامات تكبير الخط من الكروت»)
                IconButton(
                    onClick = {
                        fontScale = (fontScale - FONT_SCALE_STEP).coerceAtLeast(FONT_SCALE_MIN)
                    },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        NoorIcons.Minus,
                        contentDescription = "تصغير خط الحديث",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = {
                        fontScale = (fontScale + FONT_SCALE_STEP).coerceAtMost(FONT_SCALE_MAX)
                    },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "تكبير خط الحديث",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                // فتح الحديث بصفحة كاملة بخطٍ أكبر (alpha44)
                IconButton(
                    onClick = onRead,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        NoorIcons.Expand,
                        contentDescription = "قراءة بملء الشاشة",
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                NoorIcons.Hadith,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (searching) "لا توجد نتائج مطابقة" else "لا توجد أحاديث للعرض",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (searching) "جرّب كلمات أخرى أو رقم الحديث — البحث يفهم التشكيل والهمزات" else "حدّث التطبيق أو أعد فتح المكتبة",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** المفضلة الفاضية — دعوة لأول حفظ بالنجمة (alpha38) */
@Composable
private fun EmptyFavorites(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                NoorIcons.StarOutline,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "لا توجد أحاديث محفوظة بعد",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "اضغط النجمة على أي حديث ليُحفظ هنا",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FailedState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                NoorIcons.Hadith,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "تعذر فتح المكتبة",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                onClick = onRetry,
            ) {
                Text(
                    text = "إعادة المحاولة",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * صفحة قراءة الحديث بملء الشاشة (alpha44 — بلاغ المالك: «عايز زر اضافي
 * في كل كارت من الاحاديث يمكن المستخدم من فتح الحديث في صفحه كامل بخط
 * اكبر يناسب كامل الصفحه لقراءه افضل»): نافذة تمتد على الشاشة كلها،
 * الحديث بخطٍ أساسه أكبر من خط الكارت (٢٠sp) وقابل للتكبير والتصغير،
 * ويتوسط الصفحة قراءةً مريحة، وزر إغلاقٍ وزر مشاركة في متناول الإبهام.
 */
@Composable
private fun FullScreenHadithReader(
    hadith: HadithEntity,
    bookTitle: String,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }

    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = scheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
            ) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "إغلاق القراءة",
                            tint = scheme.primary,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scheme.surfaceVariant,
                        contentColor = scheme.onSurfaceVariant,
                    ) {
                        Text(
                            text = bookTitle + " · " +
                                MisbahaMath.arabicNumber(hadith.numberInBook.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = hadith.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (READER_BASE_SP * fontScale).sp,
                            lineHeight = ((READER_BASE_SP * fontScale) * 1.8f).sp,
                        ),
                        color = scheme.onSurface,
                        textAlign = TextAlign.Start,
                    )
                    if (hadith.chapter.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = hadith.chapter,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                }
                // أدوات الصفحة — تكبير وتصغير ومشاركة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(
                        onClick = {
                            fontScale = (fontScale - FONT_SCALE_STEP).coerceAtLeast(FONT_SCALE_MIN)
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            NoorIcons.Minus,
                            contentDescription = "تصغير خط القراءة",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            fontScale = (fontScale + FONT_SCALE_STEP).coerceAtMost(FONT_SCALE_MAX)
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "تكبير خط القراءة",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val text = buildString {
                                append('«').append(hadith.text).append('»')
                                append("\n\n")
                                append(bookTitle)
                                append(" · حديث ")
                                append(MisbahaMath.arabicNumber(hadith.numberInBook.toLong()))
                                append("\n\n")
                                append("من تطبيق نور المسلم")
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        },
                                        "مشاركة الحديث",
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "مشاركة الحديث",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
