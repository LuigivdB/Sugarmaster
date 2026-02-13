package com.sugarmaster.presentation.screen

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.input.RemoteInputIntentHelper

private const val KEY_EMAIL = "email"
private const val KEY_PASSWORD = "password"
private const val KEY_REGION = "region"

@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    onLogin: (email: String, password: String, region: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("eu") }

    val emailLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            results?.getCharSequence(KEY_EMAIL)?.toString()?.let { email = it }
        }
    }

    val passwordLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            results?.getCharSequence(KEY_PASSWORD)?.toString()?.let { password = it }
        }
    }

    val regionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            results?.getCharSequence(KEY_REGION)?.toString()?.let { region = it }
        }
    }

    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Sugarmaster",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }

            item {
                InputChip(
                    label = "Email",
                    value = email,
                    onClick = {
                        val remoteInputs = listOf(
                            RemoteInput.Builder(KEY_EMAIL)
                                .setLabel("Email")
                                .build()
                        )
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        emailLauncher.launch(intent)
                    }
                )
            }

            item {
                InputChip(
                    label = "Password",
                    value = if (password.isEmpty()) "" else "\u2022".repeat(password.length),
                    onClick = {
                        val remoteInputs = listOf(
                            RemoteInput.Builder(KEY_PASSWORD)
                                .setLabel("Password")
                                .build()
                        )
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        passwordLauncher.launch(intent)
                    }
                )
            }

            item {
                InputChip(
                    label = "Region",
                    value = region,
                    onClick = {
                        val remoteInputs = listOf(
                            RemoteInput.Builder(KEY_REGION)
                                .setLabel("Region (eu, us, ap...)")
                                .build()
                        )
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        regionLauncher.launch(intent)
                    }
                )
            }

            if (error != null) {
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = { onLogin(email, password, region) },
                        enabled = email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.primaryButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign In", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InputChip(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        label = {
            Text(
                text = if (value.isEmpty()) label else value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = if (value.isNotEmpty()) {
            { Text(text = label, style = MaterialTheme.typography.caption3) }
        } else null,
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
