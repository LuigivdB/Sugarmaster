package com.sugarmaster.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sugarmaster.data.api.LibreLinkUpClient
import com.sugarmaster.data.model.GlucoseMeasurement
import com.sugarmaster.data.model.GlucoseItem
import com.sugarmaster.data.model.TrendArrow
import com.sugarmaster.data.repository.GlucoseRepository
import com.sugarmaster.data.repository.GlucoseResult
import com.sugarmaster.data.store.CredentialStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GlucoseUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val currentValue: Double? = null,
    val currentValueMgDl: Double? = null,
    val trendArrow: TrendArrow = TrendArrow.NOT_DETERMINED,
    val isHigh: Boolean = false,
    val isLow: Boolean = false,
    val timestamp: String? = null,
    val graphItems: List<GlucoseItem> = emptyList(),
    val patientName: String = "",
    val error: String? = null,
    val loginError: String? = null,
    val glucoseUnit: String = "mg/dL"
)

class GlucoseViewModel(application: Application) : AndroidViewModel(application) {

    private val credentialStore = CredentialStore(application)
    private val repository = GlucoseRepository(credentialStore)

    private val _uiState = MutableStateFlow(GlucoseUiState())
    val uiState: StateFlow<GlucoseUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var isAmbient: Boolean = false

    companion object {
        private const val ACTIVE_POLL_INTERVAL_MS = 60_000L   // 1 minute when screen is on
        private const val AMBIENT_POLL_INTERVAL_MS = 300_000L // 5 minutes in ambient mode
    }

    init {
        viewModelScope.launch {
            val isLoggedIn = credentialStore.isLoggedIn.first()
            if (isLoggedIn) {
                val token = credentialStore.token.first()
                val accountId = credentialStore.accountId.first()
                val region = credentialStore.region.first()

                LibreLinkUpClient.setRegion(region)
                LibreLinkUpClient.setAuth(token, accountId)

                _uiState.value = _uiState.value.copy(isLoggedIn = true)
                startPolling()
            }
        }
    }

    fun setAmbient(ambient: Boolean) {
        if (isAmbient != ambient) {
            isAmbient = ambient
            // Restart polling with the appropriate interval
            if (_uiState.value.isLoggedIn) {
                startPolling()
            }
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
                    startPolling()
                }

                is GlucoseResult.RegionRedirect -> {
                    // Retry login with the correct region
                    val retryResult = repository.login(email, password, result.region)
                    when (retryResult) {
                        is GlucoseResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                isLoading = false,
                                loginError = null
                            )
                            startPolling()
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

    fun logout() {
        viewModelScope.launch {
            pollingJob?.cancel()
            LibreLinkUpClient.clearAuth()
            credentialStore.clear()
            _uiState.value = GlucoseUiState()
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            fetchGlucoseData()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchGlucoseData()
                val interval = if (isAmbient) AMBIENT_POLL_INTERVAL_MS else ACTIVE_POLL_INTERVAL_MS
                delay(interval)
            }
        }
    }

    private suspend fun fetchGlucoseData() {
        val patientId = credentialStore.patientId.first()
        if (patientId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "No patient configured")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        var result = repository.getCurrentGlucose(patientId)

        // If we get an auth error, try to re-authenticate
        if (result is GlucoseResult.Error && result.message.contains("401")) {
            val reAuth = repository.reAuthenticate()
            if (reAuth is GlucoseResult.Success) {
                result = repository.getCurrentGlucose(patientId)
            }
        }

        when (result) {
            is GlucoseResult.Success -> {
                val data = result.data
                val measurement = data.currentMeasurement
                val connection = data.connection

                val patientName = listOfNotNull(
                    connection?.firstName,
                    connection?.lastName
                ).joinToString(" ")

                val glucoseUnit = when (measurement?.glucoseUnits) {
                    1 -> "mg/dL"
                    2 -> "mmol/L"
                    else -> "mg/dL"
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentValue = measurement?.value,
                    currentValueMgDl = measurement?.valueInMgPerDl,
                    trendArrow = TrendArrow.fromCode(measurement?.trendArrow),
                    isHigh = measurement?.isHigh == true,
                    isLow = measurement?.isLow == true,
                    timestamp = measurement?.timestamp,
                    graphItems = data.graphItems,
                    patientName = patientName,
                    glucoseUnit = glucoseUnit,
                    error = null
                )
            }

            is GlucoseResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }

            is GlucoseResult.RegionRedirect -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Region redirect needed"
                )
            }
        }
    }
}
