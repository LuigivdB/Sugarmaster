package com.sugarmaster.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.sugarmaster.presentation.screen.GlucoseScreen
import com.sugarmaster.presentation.screen.LoginScreen
import com.sugarmaster.presentation.theme.SugarmasterTheme

class MainActivity : ComponentActivity() {

    private val isAmbient = mutableStateOf(false)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient.value = true
        }

        override fun onExitAmbient() {
            isAmbient.value = false
        }

        override fun onUpdateAmbient() {
            // System calls this ~once per minute in ambient mode
        }
    }

    private val ambientObserver = AmbientLifecycleObserver(this, ambientCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)

        setContent {
            SugarmasterApp(isAmbient = isAmbient.value)
        }
    }
}

@Composable
fun SugarmasterApp(isAmbient: Boolean = false) {
    SugarmasterTheme {
        val viewModel: GlucoseViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        viewModel.setAmbient(isAmbient)

        if (uiState.isLoggedIn) {
            GlucoseScreen(
                state = uiState,
                isAmbient = isAmbient,
                onRefresh = { viewModel.refreshNow() },
                onLogout = { viewModel.logout() },
                onToggleVibration = { viewModel.toggleVibrationAlerts() }
            )
        } else {
            LoginScreen(
                isLoading = uiState.isLoading,
                error = uiState.loginError,
                onLogin = { email, password, region ->
                    viewModel.login(email, password, region)
                }
            )
        }
    }
}
