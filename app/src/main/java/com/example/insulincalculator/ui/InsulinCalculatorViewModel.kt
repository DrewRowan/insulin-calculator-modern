package com.example.insulincalculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insulincalculator.data.InsulinEntry
import com.example.insulincalculator.data.InsulinRepository
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
    val icr: Int = 10,
    val finalInsulinDose: Double = 0.0
)

class InsulinCalculatorViewModel(
    private val repository: InsulinRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    fun updateCurrentBG(value: Double) {
        _state.value = _state.value.copy(currentBG = value)
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

    fun updateICR(value: Int) {
        _state.value = _state.value.copy(icr = value)
        calculateInsulinDose()
    }

    private fun calculateInsulinDose() {
        val state = _state.value
        val insulinDoseRaw = if (state.icr > 0) {
            state.correctionDose * (state.carbs / state.icr)
        } else 0.0

        var rangeCorrection = (state.currentBG - state.targetBG) / 2
        if (rangeCorrection < 0) {
            rangeCorrection = 0.0
        }

        val finalInsulinDose = insulinDoseRaw + rangeCorrection
        _state.value = state.copy(finalInsulinDose = finalInsulinDose)
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
                icr = state.icr,
                finalInsulinDose = state.finalInsulinDose
            )
            repository.saveEntry(entry)
        }
    }
} 