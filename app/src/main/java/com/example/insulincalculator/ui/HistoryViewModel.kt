package com.example.insulincalculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insulincalculator.data.InsulinEntry
import com.example.insulincalculator.data.InsulinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class FilterState(
    val selectedDate: Date? = null,
    val bgMin: Double? = null,
    val bgMax: Double? = null,
    val carbsMin: Double? = null,
    val carbsMax: Double? = null,
    val correctionMin: Double? = null,
    val correctionMax: Double? = null,
    val targetMin: Double? = null,
    val targetMax: Double? = null,
    val icrMin: Int? = null,
    val icrMax: Int? = null,
    val doseMin: Double? = null,
    val doseMax: Double? = null
)

data class HistoryState(
    val entries: List<InsulinEntry> = emptyList(),
    val filterState: FilterState = FilterState(),
    val sortColumn: String = "timestamp",
    val ascending: Boolean = false,
    val currentPage: Int = 0,
    val pageSize: Int = 10
)

class HistoryViewModel(
    private val repository: InsulinRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllEntries()
                .map { entries ->
                    val filteredEntries = filterEntries(entries)
                    val sortedEntries = sortEntries(filteredEntries)
                    _state.value.copy(
                        entries = sortedEntries,
                        currentPage = 0
                    )
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    fun updateFilter(filterState: FilterState) {
        _state.value = _state.value.copy(
            filterState = filterState,
            currentPage = 0
        )
    }

    fun updateSort(column: String) {
        val currentSort = _state.value.sortColumn
        val currentAscending = _state.value.ascending
        _state.value = _state.value.copy(
            sortColumn = column,
            ascending = if (currentSort == column) !currentAscending else true
        )
    }

    fun updatePage(page: Int) {
        _state.value = _state.value.copy(currentPage = page)
    }

    private fun filterEntries(entries: List<InsulinEntry>): List<InsulinEntry> {
        val filter = _state.value.filterState
        return entries.filter { entry ->
            val dateMatch = filter.selectedDate == null || 
                entry.timestamp.toInstant().toEpochMilli() >= filter.selectedDate.toInstant().toEpochMilli() &&
                entry.timestamp.toInstant().toEpochMilli() < filter.selectedDate.toInstant().toEpochMilli() + 86400000

            val bgMatch = (filter.bgMin == null || entry.currentBG >= filter.bgMin) &&
                (filter.bgMax == null || entry.currentBG <= filter.bgMax)

            val carbsMatch = (filter.carbsMin == null || entry.carbs >= filter.carbsMin) &&
                (filter.carbsMax == null || entry.carbs <= filter.carbsMax)

            val correctionMatch = (filter.correctionMin == null || entry.correctionDose >= filter.correctionMin) &&
                (filter.correctionMax == null || entry.correctionDose <= filter.correctionMax)

            val targetMatch = (filter.targetMin == null || entry.targetBG >= filter.targetMin) &&
                (filter.targetMax == null || entry.targetBG <= filter.targetMax)

            val icrMatch = (filter.icrMin == null || entry.icr >= filter.icrMin) &&
                (filter.icrMax == null || entry.icr <= filter.icrMax)

            val doseMatch = (filter.doseMin == null || entry.finalInsulinDose >= filter.doseMin) &&
                (filter.doseMax == null || entry.finalInsulinDose <= filter.doseMax)

            dateMatch && bgMatch && carbsMatch && correctionMatch && targetMatch && icrMatch && doseMatch
        }
    }

    private fun sortEntries(entries: List<InsulinEntry>): List<InsulinEntry> {
        val sorted = when (_state.value.sortColumn) {
            "timestamp" -> entries.sortedBy { it.timestamp }
            "currentBG" -> entries.sortedBy { it.currentBG }
            "carbs" -> entries.sortedBy { it.carbs }
            "correctionDose" -> entries.sortedBy { it.correctionDose }
            "targetBG" -> entries.sortedBy { it.targetBG }
            "icr" -> entries.sortedBy { it.icr }
            "finalInsulinDose" -> entries.sortedBy { it.finalInsulinDose }
            else -> entries
        }
        return if (_state.value.ascending) sorted else sorted.reversed()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearEntries()
        }
    }

    fun deleteEntry(entry: InsulinEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }
} 