package com.example.insulincalculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.insulincalculator.data.LibreLinkUpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val email: String = "",
    val password: String = "",
    val region: String = "EU",
    val isLinked: Boolean = false,
    val linkedEmail: String? = null,
    val expiredEmail: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel(
    private val libreRepository: LibreLinkUpRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        val isLinked = libreRepository.isLinked()
        val storedEmail = libreRepository.getStoredEmail()
        _state.value = _state.value.copy(
            isLinked = isLinked,
            linkedEmail = if (isLinked) storedEmail else null,
            expiredEmail = if (!isLinked && storedEmail != null) storedEmail else null,
            email = if (!isLinked) storedEmail ?: "" else "",
            region = libreRepository.getStoredRegion()
        )
    }

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun updateRegion(region: String) {
        _state.value = _state.value.copy(region = region, error = null)
    }

    fun linkAccount() {
        val state = _state.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter your email and password")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null, successMessage = null)
        viewModelScope.launch {
            libreRepository.login(state.email, state.password, state.region)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLinked = true,
                        linkedEmail = state.email,
                        expiredEmail = null,
                        password = "",
                        successMessage = "Account linked successfully"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to link account"
                    )
                }
        }
    }

    fun unlink() {
        libreRepository.unlink()
        _state.value = SettingsState(region = libreRepository.getStoredRegion())
    }
}
