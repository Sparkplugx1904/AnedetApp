package com.example.anemiadetector.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.anemiadetector.data.local.entity.ExaminationEntity
import com.example.anemiadetector.data.repository.ExaminationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for History Screen
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ExaminationRepository
) : ViewModel() {

    // Filter state
    private val _filterState = MutableStateFlow(FilterType.ALL)
    val filterState: StateFlow<FilterType> = _filterState.asStateFlow()

    // Sort state
    private val _sortState = MutableStateFlow(SortType.NEWEST)
    val sortState: StateFlow<SortType> = _sortState.asStateFlow()

    // Examinations list
    val examinations: StateFlow<List<ExaminationEntity>> = combine(
        repository.getAllExaminations(),
        _filterState,
        _sortState
    ) { exams: List<ExaminationEntity>, filter: FilterType, sort: SortType ->
        var filtered = when (filter) {
            FilterType.ALL -> exams
            FilterType.ANEMIA -> exams.filter { it.predictedLabel == "Anemia" }
            FilterType.NON_ANEMIA -> exams.filter { it.predictedLabel == "Non-Anemia" }
        }

        filtered = when (sort) {
            SortType.NEWEST -> filtered.sortedByDescending { it.timestamp }
            SortType.OLDEST -> filtered.sortedBy { it.timestamp }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Set filter type
     */
    fun setFilter(filter: FilterType) {
        _filterState.value = filter
    }

    /**
     * Set sort type
     */
    fun setSort(sort: SortType) {
        _sortState.value = sort
    }

    /**
     * Delete examination
     */
    fun deleteExamination(examination: ExaminationEntity) {
        viewModelScope.launch {
            repository.delete(examination)
        }
    }

    /**
     * Delete all examinations
     */
    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}

enum class FilterType {
    ALL, ANEMIA, NON_ANEMIA
}

enum class SortType {
    NEWEST, OLDEST
}
