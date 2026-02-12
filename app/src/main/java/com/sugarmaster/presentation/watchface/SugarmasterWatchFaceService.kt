package com.sugarmaster.presentation.watchface

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.sugarmaster.data.api.LibreLinkUpClient
import com.sugarmaster.data.model.TrendArrow
import com.sugarmaster.data.repository.GlucoseRepository
import com.sugarmaster.data.repository.GlucoseResult
import com.sugarmaster.data.store.CredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CachedGlucoseData(
    val value: Double? = null,
    val valueMgDl: Double? = null,
    val unit: String = "mg/dL",
    val isHigh: Boolean = false,
    val isLow: Boolean = false,
    val trendArrow: TrendArrow = TrendArrow.NOT_DETERMINED
)

private enum class GlucoseStatus { LOW, NORMAL, HIGH, UNKNOWN }

class SugarmasterWatchFaceService : WatchFaceService() {

    private lateinit var credentialStore: CredentialStore
    private lateinit var repository: GlucoseRepository
    private var fetchScope: CoroutineScope? = null

    @Volatile
    var cachedGlucose: CachedGlucoseData = CachedGlucoseData()
        private set

    @Volatile
    private var vibrationAlerts: Boolean = false
    private var previousStatus: GlucoseStatus = GlucoseStatus.UNKNOWN

    companion object {
        private const val ACTIVE_POLL_MS = 60_000L
        private const val AMBIENT_POLL_MS = 300_000L
    }

    override fun onCreate() {
        super.onCreate()
        credentialStore = CredentialStore(applicationContext)
        repository = GlucoseRepository(credentialStore)
    }

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = SugarmasterRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            dataProvider = { cachedGlucose }
        )

        startDataFetching(watchState)

        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }

    private fun startDataFetching(watchState: WatchState) {
        fetchScope?.cancel()
        fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fetchScope?.launch {
            // Load settings
            vibrationAlerts = credentialStore.vibrationAlerts.first()

            // Initialize auth
            val token = credentialStore.token.first()
            val accountId = credentialStore.accountId.first()
            val region = credentialStore.region.first()

            if (token.isBlank()) return@launch

            LibreLinkUpClient.setRegion(region)
            LibreLinkUpClient.setAuth(token, accountId)

            val patientId = credentialStore.patientId.first()
            if (patientId.isBlank()) return@launch

            while (isActive) {
                fetchGlucose(patientId)
                // Reload vibration setting periodically
                vibrationAlerts = credentialStore.vibrationAlerts.first()
                val interval = if (watchState.isAmbient.value == true) AMBIENT_POLL_MS else ACTIVE_POLL_MS
                delay(interval)
            }
        }
    }

    private suspend fun fetchGlucose(patientId: String) {
        var result = repository.getCurrentGlucose(patientId)

        if (result is GlucoseResult.Error && result.message.contains("401")) {
            val reAuth = repository.reAuthenticate()
            if (reAuth is GlucoseResult.Success) {
                result = repository.getCurrentGlucose(patientId)
            }
        }

        when (result) {
            is GlucoseResult.Success -> {
                val measurement = result.data.currentMeasurement

                val glucoseUnit = when (measurement?.glucoseUnits) {
                    1 -> "mg/dL"
                    2 -> "mmol/L"
                    else -> "mg/dL"
                }

                val newIsHigh = measurement?.isHigh == true
                val newIsLow = measurement?.isLow == true
                val newMgDl = measurement?.valueInMgPerDl

                // Check threshold crossing for vibration
                val newStatus = getStatus(newIsHigh, newIsLow, newMgDl)
                if (vibrationAlerts &&
                    previousStatus == GlucoseStatus.NORMAL &&
                    (newStatus == GlucoseStatus.HIGH || newStatus == GlucoseStatus.LOW)
                ) {
                    vibrateOnce()
                }
                previousStatus = newStatus

                cachedGlucose = CachedGlucoseData(
                    value = measurement?.value,
                    valueMgDl = newMgDl,
                    unit = glucoseUnit,
                    isHigh = newIsHigh,
                    isLow = newIsLow,
                    trendArrow = TrendArrow.fromCode(measurement?.trendArrow)
                )
            }
            else -> { /* keep cached data */ }
        }
    }

    private fun getStatus(isHigh: Boolean, isLow: Boolean, mgDl: Double?): GlucoseStatus {
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
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        fetchScope?.cancel()
        super.onDestroy()
    }
}
