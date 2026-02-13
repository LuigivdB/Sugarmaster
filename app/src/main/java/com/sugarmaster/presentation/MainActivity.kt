package com.sugarmaster.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import com.sugarmaster.presentation.screen.LoginScreen
import com.sugarmaster.presentation.screen.SettingsScreen
import com.sugarmaster.presentation.theme.SugarmasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SugarmasterApp()
        }
    }
}

@Composable
fun SugarmasterApp() {
    SugarmasterTheme {
        val viewModel: GlucoseViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        when {
            uiState.isInitializing -> {
                // Show loading while checking login state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.isLoggedIn -> {
                SettingsScreen(
                    state = uiState,
                    onToggleVibration = { viewModel.toggleVibrationAlerts() },
                    onLogout = { viewModel.logout() }
                )
            }
            else -> {
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
}