package com.nuvio.tv.fork.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val days: List<CalendarDay> = emptyList(),
    val filter: CalendarFilter = CalendarFilter.ALL,
)

/** G9e: Cxsmo `CalendarViewModel` @ 3e0d0fa. */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(CalendarFilter.ALL)

    val uiState: StateFlow<CalendarUiState> = combine(repository.days, repository.isRefreshing, filter) { days, refreshing, selected ->
        CalendarUiState(
            isLoading = days.isEmpty() && refreshing,
            isRefreshing = refreshing,
            days = CalendarRules.filter(days, selected, LocalDate.now()),
            filter = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        repository.ensureStarted()
    }

    fun setFilter(value: CalendarFilter) {
        filter.value = value
    }

    fun refresh() = repository.refresh()
}
