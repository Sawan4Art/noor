package com.sawan4art.noor.ui.hadith

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.data.hadith.BookEntity
import com.sawan4art.noor.data.hadith.HadithEntity
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.theme.NoorPageTitle

/**
 * تبويب مكتبة الأحاديث — توجيه المالك («عايزين نضيف قسم جديد لـ مكتبة
 * الأحاديث والبحث الأوفلاين»):
 *
 * - **شريط بحث فوق**: بحث لحظي أوفلاين على فهرس FTS5 — برقم الحديث أو
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
                    modifier = Modifier.size(15.dp),
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
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }

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
                style = MaterialTheme.typography.bodyLarge,
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
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = if (isFavorite) NoorIcons.StarFilled else NoorIcons.StarOutline,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "حفظ في المفضلة",
                        tint = if (isFavorite) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp),
                    )
                }
                IconButton(
                    onClick = { onShare(hadith, bookTitle) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "مشاركة الحديث",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
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
