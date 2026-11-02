package com.sawan4art.noor.ui.hadith

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
 * - **البحث**: هدوء ٣٠٠ مللي سكّة بعد آخر ضغطة ثم MATCH على فهرس FTS5
 *   مرتباً بالصلة، بسقف ٢٠٠ نتيجة كي لا يستنزف «الله» قائمة لا تنتهي.
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
        val loading: Boolean = true,
        val canLoadMore: Boolean = false,
        /** بلغ سقف نتائج البحث — تظهر رسالة «جرّب كلمات أدق» */
        val capped: Boolean = false,
        val failed: Boolean = false,
        /** تزيد مع كل تحميل جديد — الشاشة تمرر القائمة لأعلى حينها */
        val resetToken: Int = 0,
    )

    private data class BrowsePage(val items: List<HadithEntity>, val total: Int)

    private val repository = HadithRepository(application)
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

    /** تبديل كتاب الفلتر — تحميل فوري بلا انتظار هدوء البحث */
    fun onBookSelect(bookId: Long?) {
        if (_state.value.selectedBookId == bookId) return
        _state.update { it.copy(selectedBookId = bookId) }
        reload()
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
                if (current.isSearching) {
                    val expr = HadithSearch.matchExpression(current.query.trim())
                        ?: return@launch
                    val limit = minOf(PAGE_SIZE, SEARCH_CAP - offset)
                    val more = repository.search(expr, current.selectedBookId, limit, offset)
                    more to limit
                } else {
                    repository.browse(current.selectedBookId, PAGE_SIZE, offset) to PAGE_SIZE
                }
            }
            request.fold(
                onSuccess = { (more, requested) ->
                    _state.update { state ->
                        val items = state.items + more
                        state.copy(
                            items = items,
                            canLoadMore = more.size == requested &&
                                (!state.isSearching || items.size < SEARCH_CAP),
                            capped = state.isSearching && items.size >= SEARCH_CAP,
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
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    failed = false,
                    items = emptyList(),
                    totalCount = 0,
                    isSearching = query.isNotEmpty(),
                    canLoadMore = false,
                    capped = false,
                    resetToken = it.resetToken + 1,
                )
            }
            runCatching {
                if (query.isEmpty()) {
                    val items = repository.browse(bookId, PAGE_SIZE, 0)
                    val total = repository.browseCount(bookId)
                    BrowsePage(items, total)
                } else {
                    val expr = HadithSearch.matchExpression(query)
                    if (expr == null) {
                        // استعلام من علامات فقط — يتصرف كالتصفح
                        BrowsePage(
                            repository.browse(bookId, PAGE_SIZE, 0),
                            repository.browseCount(bookId),
                        )
                    } else {
                        val limit = minOf(PAGE_SIZE, SEARCH_CAP)
                        BrowsePage(repository.search(expr, bookId, limit, 0), -1)
                    }
                }
            }.fold(
                onSuccess = { page ->
                    _state.update { state ->
                        state.copy(
                            loading = false,
                            items = page.items,
                            totalCount = page.total,
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
