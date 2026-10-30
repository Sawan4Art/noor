package com.sawan4art.noor.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.domain.location.CityGeocoder
import com.sawan4art.noor.domain.location.LocationSource
import com.sawan4art.noor.domain.prayer.CalculationMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * مسؤول الشاشة الرئيسية — لقط كل ثانية والشاشة ظاهرة فقط (توفير بطارية)،
 * والإعدادات تُقرأ رياضياً من DataStore فأي تغيير ينعكس فوراً.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val locationSource = LocationSource(application)
    private val geocoder = CityGeocoder()

    val settings: StateFlow<NoorSettingsState> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, NoorSettingsState())

    /** نبضة الثانية — تعمل والشاشة مشتركة فقط وتتوقف عند الخروج */
    private val ticker: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    data class HomeUiState(
        val settings: NoorSettingsState = NoorSettingsState(),
        val nowMillis: Long = System.currentTimeMillis(),
    )

    val uiState: StateFlow<HomeUiState> = combine(settings, ticker) { s, t ->
        HomeUiState(s, t)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(NoorSettingsState(), System.currentTimeMillis()),
    )

    /** حالة تحديد الموقع الجاري (لعرض دوّار على الزر) */
    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating

    fun detectLocation() {
        if (_locating.value) return
        _locating.value = true
        viewModelScope.launch {
            try {
                val coords = locationSource.currentLocation()
                if (coords != null) {
                    val geo = geocoder.reverseGeocode(coords.first, coords.second)
                    val city = if (geo.country != null && !geo.city.contains(geo.country)) {
                        "${geo.city}، ${geo.country}"
                    } else {
                        geo.city
                    }
                    repository.setLocation(city, coords.first, coords.second)
                }
            } finally {
                _locating.value = false
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setMethod(method: CalculationMethod) =
        viewModelScope.launch { repository.setMethod(method) }

    fun setHanafi(hanafi: Boolean) = viewModelScope.launch { repository.setHanafi(hanafi) }
}
