package com.example.insulincalculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insulincalculator.data.InsulinEntry
import com.example.insulincalculator.data.InsulinRepository
import com.example.insulincalculator.data.LibreLinkUpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class CalculatorState(
    val currentBG: Double = 6.0,
    val carbs: Double = 0.0,
    val correctionDose: Double = 0.5,
    val targetBG: Double = 5.5,
    val finalInsulinDose: Double = 0.0,
    val isLoadingGlucose: Boolean = false,
    val glucoseError: String? = null
)

class InsulinCalculatorViewModel(
    private val repository: InsulinRepository,
    private val libreRepository: LibreLinkUpRepository
) : ViewModel() {

    companion object {
        const val ICR = 10
    }

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    fun updateCurrentBG(value: Double) {
        _state.value = _state.value.copy(currentBG = value, glucoseError = null)
        calculateInsulinDose()
    }

    fun updateCarbs(value: Double) {
        _state.value = _state.value.copy(carbs = value)
        calculateInsulinDose()
    }

    fun updateCorrectionDose(value: Double) {
        _state.value = _state.value.copy(correctionDose = value)
        calculateInsulinDose()
    }

    fun updateTargetBG(value: Double) {
        _state.value = _state.value.copy(targetBG = value)
        calculateInsulinDose()
    }

    fun autoFetchGlucoseIfLinked() {
        if (!libreRepository.isLinked()) return
        fetchGlucoseFromLibre()
    }

    fun fetchGlucoseFromLibre() {
        _state.value = _state.value.copy(isLoadingGlucose = true, glucoseError = null)
        viewModelScope.launch {
            libreRepository.fetchCurrentGlucose()
                .onSuccess { glucose ->
                    val rounded = kotlin.math.round(glucose * 10) / 10
                    updateCurrentBG(rounded.coerceIn(2.0, 25.0))
                    _state.value = _state.value.copy(isLoadingGlucose = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoadingGlucose = false,
                        glucoseError = e.message ?: "Failed to fetch glucose"
                    )
                }
        }
    }

    fun clearGlucoseError() {
        _state.value = _state.value.copy(glucoseError = null)
    }

    private fun calculateInsulinDose() {
        val state = _state.value
        val insulinDoseRaw = state.correctionDose * (state.carbs / ICR)

        var rangeCorrection = (state.currentBG - state.targetBG) / 2
        if (rangeCorrection < 0) rangeCorrection = 0.0

        _state.value = state.copy(finalInsulinDose = insulinDoseRaw + rangeCorrection)
    }

    fun saveEntry() {
        viewModelScope.launch {
            val state = _state.value
            val entry = InsulinEntry(
                timestamp = Date(),
                currentBG = state.currentBG,
                carbs = state.carbs,
                correctionDose = state.correctionDose,
                targetBG = state.targetBG,
                icr = ICR,
                finalInsulinDose = state.finalInsulinDose
            )
            repository.saveEntry(entry)
        }
    }
}
