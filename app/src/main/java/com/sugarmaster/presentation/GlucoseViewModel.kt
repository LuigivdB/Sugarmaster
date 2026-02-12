package com.sugarmaster.presentation

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    val glucoseUnit: String = "mg/dL",
    val vibrationAlerts: Boolean = false
)

private enum class GlucoseStatus { LOW, NORMAL, HIGH, UNKNOWN }

class GlucoseViewModel(application: Application) : AndroidViewModel(application) {

    private val credentialStore = CredentialStore(application)
    private val repository = GlucoseRepository(credentialStore)

    private val _uiState = MutableStateFlow(GlucoseUiState())
    val uiState: StateFlow<GlucoseUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var isAmbient: Boolean = false
    private var previousStatus: GlucoseStatus = GlucoseStatus.UNKNOWN

    companion object {
        private const val ACTIVE_POLL_INTERVAL_MS = 60_000L   // 1 minute when screen is on
        private const val AMBIENT_POLL_INTERVAL_MS = 300_000L // 5 minutes in ambient mode
    }

    init {
        viewModelScope.launch {
            // Load vibration preference
            val vibEnabled = credentialStore.vibrationAlerts.first()
            _uiState.value = _uiState.value.copy(vibrationAlerts = vibEnabled)

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
            if (_uiState.value.isLoggedIn) {
                startPolling()
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
            previousStatus = GlucoseStatus.UNKNOWN
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

    private fun getCurrentStatus(isHigh: Boolean, isLow: Boolean, mgDl: Double?): GlucoseStatus {
        return when {
            isLow -> GlucoseStatus.LOW
            isHigh -> GlucoseStatus.HIGH
            mgDl != null && mgDl < 70 -> GlucoseStatus.LOW
            mgDl != null && mgDl > 180 -> GlucoseStatus.HIGH
            mgDl != null -> GlucoseStatus.NORMAL
            else -> GlucoseStatus.UNKNOWN
        }
    }

    private fun vibrateOnce() {
        val app = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
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

                val newIsHigh = measurement?.isHigh == true
                val newIsLow = measurement?.isLow == true
                val newMgDl = measurement?.valueInMgPerDl

                // Check for threshold crossing
                val newStatus = getCurrentStatus(newIsHigh, newIsLow, newMgDl)
                if (_uiState.value.vibrationAlerts &&
                    previousStatus == GlucoseStatus.NORMAL &&
                    (newStatus == GlucoseStatus.HIGH || newStatus == GlucoseStatus.LOW)
                ) {
                    vibrateOnce()
                }
                previousStatus = newStatus

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentValue = measurement?.value,
                    currentValueMgDl = newMgDl,
                    trendArrow = TrendArrow.fromCode(measurement?.trendArrow),
                    isHigh = newIsHigh,
                    isLow = newIsLow,
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
