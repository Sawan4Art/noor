package com.sawan4art.noor.ui.hadith

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.data.hadith.BookEntity
import com.sawan4art.noor.data.hadith.HadithEntity
import com.sawan4art.noor.data.hadith.HadithRepository
import com.sawan4art.noor.domain.hadith.HadithSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * مسؤول مكتبة الأحداث — على نمط AzkarViewModel (نفس دورة حياة النشاط
 * فالحالة تعيش عبر تبديل التبويبات):
 *
 * - **التصفح**: استعلام فارغ — صفحات بترتيب الكتاب ثم رقم الحديث.
 * - **البحث النصي**: هدوء ٣٠٠ مللي سكّة بعد آخر ضغطة ثم MATCH على فهرس
 *   FTS4 — العبارة الحرفية أولاً ثم الكلمات المتفرقة — بسقف ٢٠٠ نتيجة
 *   كي لا يستنزف «الله» قائمة لا تنتهي.
 * - **البحث برقم الحديث** (alpha38): استعلام رقماً خالصاً أو «حديث ٥»
 *   يُطابق numberInBook تاماً ويرتب بترتيب الكتب.
 * - **المفضلة** (alpha38): شريحة المستندة إلى DataStore — بلا بحث تُقرأ
 *   بترتيب الحفظ الأحدث أولاً، ومع بحث تُصفّى نتائج البحث على المحفوظ.
 * - التصفح يعيد العدّ الفعلي عبر COUNT، والبحث يكتفي بمعروضه لأن عدّ
 *   كل المطابقات في كل ضغطة يعيد تشغيل الفهرس مرتين بلا فائدة.
 */
class HadithViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PAGE_SIZE = 30
        private const val SEARCH_CAP = 200
        private const val DEBOUNCE_MS = 300L
    }

    data class HadithUiState(
        val books: List<BookEntity> = emptyList(),
        val query: String = "",
        val selectedBookId: Long? = null,
        val items: List<HadithEntity> = emptyList(),
        /** عدد أحاديث التصفح — بلا معنى في وضع البحث (-1) */
        val totalCount: Int = 0,
        val isSearching: Boolean = false,
        /** الاستعلام رقماً خالصاً — التحميل من numberInBook لا من الفهرس */
        val isNumberSearch: Boolean = false,
        /** وضع قائمة المفضلة — شريحة النجمة في أول صف الفلترة */
        val favoritesMode: Boolean = false,
        /** معرفات المحفوظ بترتيب الحفظ الأحدث أولاً */
        val favoriteIds: List<Long> = emptyList(),
        val loading: Boolean = true,
        val canLoadMore: Boolean = false,
        /** بلغ سقف نتائج البحث — تظهر رسالة «جرّب كلمات أدق» */
        val capped: Boolean = false,
        val failed: Boolean = false,
        /** تزيد مع كل تحميل جديد — الشاشة تمرر القائمة لأعلى حينها */
        val resetToken: Int = 0,
    )

    private data class BrowsePage(
        val items: List<HadithEntity>,
        val total: Int,
        val numberSearch: Boolean = false,
    )

    private val repository = HadithRepository(application)
    private val settings = SettingsRepository(application)
    private val _state = MutableStateFlow(HadithUiState())
    val state: StateFlow<HadithUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var loadJob: Job? = null
    private var loadMoreRunning = false

    init {
        viewModelScope.launch {
            val books = runCatching { repository.books() }.getOrDefault(emptyList())
            _state.update { it.copy(books = books) }
        }
        // قائمة المفضلة تعيش في DataStore — أي حفظ أو حذف يجددها هنا
        viewModelScope.launch {
            settings.favoriteHadithIds.collect { ids ->
                val previous = _state.value.favoriteIds
                _state.update { it.copy(favoriteIds = ids) }
                // حذف من قائمة المفضلة المفتوحة يُسقط الكارت فوراً
                if (previous != ids && _state.value.favoritesMode &&
                    _state.value.query.isBlank()
                ) {
                    reload()
                }
            }
        }
        // انطلاق أول تحميل + البحث المؤجَّل: آخر قيمة بعد هدوئها هي التي تُحمَّل
        viewModelScope.launch {
            queryFlow.debounce(DEBOUNCE_MS).collectLatest { reload() }
        }
    }

    /** كل ضغطة تُحدّث الحقل فوراً وتعيد توقيت التحميل المؤجَّل */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    /**
     * تبديل كتاب الفلتر — تحميل فوري بلا انتظار هدوء البحث. واختيار
     * كتابٍ وهو في المفضلة يخرجه من المفضلة (الشريحتا حصرٌ لا تجمع بينهما
     * فالمفضلة قائمة مستقلة لا رفّ كتاب).
     */
    fun onBookSelect(bookId: Long?) {
        val current = _state.value
        if (current.selectedBookId == bookId && !current.favoritesMode) return
        _state.update { it.copy(selectedBookId = bookId, favoritesMode = false) }
        reload()
    }

    /** شريحة المفضلة — تبديل وضع القائمة المحفوظة وإعادة التحميل فوراً */
    fun onFavoritesToggle() {
        val entering = !_state.value.favoritesMode
        _state.update {
            it.copy(
                favoritesMode = entering,
                // المفضلة قائمة مستقلة — لا رفّ كتاب داخلها
                selectedBookId = if (entering) null else it.selectedBookId,
            )
        }
        reload()
    }

    /** نجمة كارت — حفظ أو إخراج، والحالة تعود من تدفّق DataStore نفسها */
    fun toggleFavorite(id: Long) {
        val saved = id in _state.value.favoriteIds
        viewModelScope.launch {
            if (saved) settings.removeHadithFavorite(id) else settings.addHadithFavorite(id)
        }
    }

    fun retry() = reload()

    /** بلغ المستخدم آخر القائمة — صفحة تالية من النتائج نفسها */
    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.canLoadMore || loadMoreRunning) return
        loadMoreRunning = true
        viewModelScope.launch {
            val offset = current.items.size
            val request = runCatching {
                when {
                    current.favoritesMode && current.query.trim().isEmpty() -> {
                        val pageIds = current.favoriteIds.drop(offset).take(PAGE_SIZE)
                        repository.byIds(pageIds) to pageIds.size
                    }
                    current.isNumberSearch -> {
                        val number = HadithSearch.pureNumber(current.query.trim())
                            ?: return@launch
                        repository.byNumber(
                            number,
                            current.selectedBookId,
                            PAGE_SIZE,
                            offset,
                        ) to PAGE_SIZE
                    }
                    current.isSearching -> {
                        val expr = HadithSearch.matchExpression(current.query.trim())
                            ?: return@launch
                        val limit = minOf(PAGE_SIZE, SEARCH_CAP - offset)
                        repository.search(expr, current.selectedBookId, limit, offset) to limit
                    }
                    else ->
                        repository.browse(current.selectedBookId, PAGE_SIZE, offset) to PAGE_SIZE
                }
            }
            request.fold(
                onSuccess = { (more, requested) ->
                    _state.update { state ->
                        val items = state.items + more
                        val exhausted = state.favoritesMode && state.query.trim().isEmpty() &&
                            items.size >= state.favoriteIds.size
                        state.copy(
                            items = items,
                            canLoadMore = !exhausted &&
                                requested > 0 &&
                                more.size == requested &&
                                (!state.isSearching || state.isNumberSearch ||
                                    items.size < SEARCH_CAP) &&
                                (!state.isNumberSearch || items.size < state.totalCount),
                            capped = state.isSearching && !state.isNumberSearch &&
                                items.size >= SEARCH_CAP,
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(failed = true, canLoadMore = false) }
                },
            )
            loadMoreRunning = false
        }
    }

    /** إلغاء أي تحميل جارٍ وبدء صفحة أولى جديدة من الحالة الحالية */
    private fun reload() {
        loadJob?.cancel()
        val snapshot = _state.value
        val query = snapshot.query.trim()
        val bookId = snapshot.selectedBookId
        val favoritesMode = snapshot.favoritesMode
        val favoriteIds = snapshot.favoriteIds
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    failed = false,
                    items = emptyList(),
                    totalCount = 0,
                    isSearching = query.isNotEmpty(),
                    isNumberSearch = false,
                    canLoadMore = false,
                    capped = false,
                    resetToken = it.resetToken + 1,
                )
            }
            runCatching {
                when {
                    // المفضلة بلا بحث — بترتيب الحفظ الأحدث أولاً
                    favoritesMode && query.isEmpty() ->
                        BrowsePage(
                            repository.byIds(favoriteIds.take(PAGE_SIZE)),
                            favoriteIds.size,
                        )
                    query.isEmpty() ->
                        BrowsePage(
                            repository.browse(bookId, PAGE_SIZE, 0),
                            repository.browseCount(bookId),
                        )
                    else -> {
                        val favFilter: (HadithEntity) -> Boolean =
                            if (favoritesMode) ({ it.id in favoriteIds }) else ({ true })
                        val number = HadithSearch.pureNumber(query)
                        when {
                            // «حديث ٥» أو رقماً خالصاً — مطابقة تامة بالرقم؛ وفي
                            // المفضلة تُجمع كل المطابقات في صفحة واحدة فيكون
                            // العدد المعروض هو المحفوظ منها فحسب
                            number != null -> {
                                val raw = repository.byNumber(
                                    number,
                                    bookId,
                                    if (favoritesMode) SEARCH_CAP else PAGE_SIZE,
                                    0,
                                )
                                val filtered = raw.filter(favFilter)
                                BrowsePage(
                                    filtered,
                                    if (favoritesMode) {
                                        filtered.size
                                    } else {
                                        repository.countByNumber(number, bookId)
                                    },
                                    numberSearch = true,
                                )
                            }
                            else -> {
                                val expr = HadithSearch.matchExpression(query)
                                if (expr == null) {
                                    // استعلام من علامات فقط — يتصرف كالتصفح
                                    BrowsePage(
                                        repository.browse(bookId, PAGE_SIZE, 0),
                                        repository.browseCount(bookId),
                                    )
                                } else {
                                    // في المفضلة تُجمع النتائج دفعة واحدة وتُصفّى
                                    // هنا — إزاحة الترقيم على النتائج المصفّاة
                                    // تخسر كروتاً، فالصفحة الواحدة أكفأ هنا
                                    val limit =
                                        if (favoritesMode) SEARCH_CAP else minOf(PAGE_SIZE, SEARCH_CAP)
                                    val raw = repository.search(expr, bookId, limit, 0)
                                    val filtered = raw.filter(favFilter)
                                    BrowsePage(
                                        filtered,
                                        if (favoritesMode) filtered.size else -1,
                                    )
                                }
                            }
                        }
                    }
                }
            }.fold(
                onSuccess = { page ->
                    _state.update { state ->
                        state.copy(
                            loading = false,
                            items = page.items,
                            totalCount = page.total,
                            isNumberSearch = page.numberSearch,
                            canLoadMore = if (page.total < 0) {
                                page.items.size >= minOf(PAGE_SIZE, SEARCH_CAP)
                            } else {
                                page.items.size < page.total
                            },
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(loading = false, failed = true) }
                },
            )
        }
    }
}
