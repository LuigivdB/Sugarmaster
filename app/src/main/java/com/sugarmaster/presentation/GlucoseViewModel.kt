package com.sugarmaster.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sugarmaster.data.api.LibreLinkUpClient
import com.sugarmaster.data.model.GlucoseItem
import com.sugarmaster.data.model.TrendArrow
import com.sugarmaster.data.repository.GlucoseRepository
import com.sugarmaster.data.repository.GlucoseResult
import com.sugarmaster.data.store.CredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GlucoseUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginError: String? = null,
    val vibrationAlerts: Boolean = false
)

class GlucoseViewModel(application: Application) : AndroidViewModel(application) {

    private val credentialStore = CredentialStore(application)
    private val repository = GlucoseRepository(credentialStore)

    private val _uiState = MutableStateFlow(GlucoseUiState())
    val uiState: StateFlow<GlucoseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val vibEnabled = credentialStore.vibrationAlerts.first()
            val isLoggedIn = credentialStore.isLoggedIn.first()
            _uiState.value = _uiState.value.copy(
                isLoggedIn = isLoggedIn,
                vibrationAlerts = vibEnabled
            )
        }
    }

    fun login(email: String, password: String, region: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loginError = null)

            val result = repository.login(email, password, region)

            when (result) {
                is GlucoseResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoading = false,
                        loginError = null
                    )
                }

                is GlucoseResult.RegionRedirect -> {
                    val retryResult = repository.login(email, password, result.region)
                    when (retryResult) {
                        is GlucoseResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                isLoading = false,
                                loginError = null
                            )
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                loginError = "Login failed after region redirect"
                            )
                        }
                    }
                }

                is GlucoseResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = result.message
                    )
                }
            }
        }
    }

    fun toggleVibrationAlerts() {
        viewModelScope.launch {
            val newValue = !_uiState.value.vibrationAlerts
            credentialStore.setVibrationAlerts(newValue)
            _uiState.value = _uiState.value.copy(vibrationAlerts = newValue)
        }
    }

    fun logout() {
        viewModelScope.launch {
            LibreLinkUpClient.clearAuth()
            credentialStore.clear()
            _uiState.value = GlucoseUiState()
        }
    }
}
