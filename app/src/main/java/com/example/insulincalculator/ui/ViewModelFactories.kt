package com.example.insulincalculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.insulincalculator.data.InsulinRepository
import com.example.insulincalculator.data.LibreLinkUpRepository

class InsulinCalculatorViewModelFactory(
    private val repository: InsulinRepository,
    private val libreRepository: LibreLinkUpRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsulinCalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsulinCalculatorViewModel(repository, libreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HistoryViewModelFactory(
    private val repository: InsulinRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettingsViewModelFactory(
    private val libreRepository: LibreLinkUpRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(libreRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
