package com.sugarmaster.presentation.screen

import android.content.Context
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.sugarmaster.presentation.GlucoseUiState
import com.sugarmaster.presentation.theme.GlucoseBlue
import com.sugarmaster.presentation.theme.GlucoseRed
import com.sugarmaster.presentation.theme.GlucoseWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlucoseScreen(
    state: GlucoseUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // Time — respects system 12h/24h setting
    var currentTime by remember { mutableStateOf(formatTime(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatTime(context)
            delay(1000L)
        }
    }

    // Battery level
    var batteryLevel by remember { mutableIntStateOf(getBatteryLevel(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            batteryLevel = getBatteryLevel(context)
            delay(30_000L)
        }
    }

    // Glucose color
    val glucoseColor = when {
        state.isLow -> GlucoseBlue
        state.isHigh -> GlucoseRed
        state.currentValueMgDl != null && state.currentValueMgDl < 70 -> GlucoseBlue
        state.currentValueMgDl != null && state.currentValueMgDl > 180 -> GlucoseRed
        else -> GlucoseWhite
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                onClick = onRefresh,
                onLongClick = onLogout
            )
    ) {
        // Time at the top
        Text(
            text = currentTime,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
                .fillMaxWidth()
        )

        // Glucose value — large and centered
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            val displayValue = state.currentValue
            if (displayValue != null) {
                Text(
                    text = if (state.glucoseUnit == "mmol/L") {
                        String.format("%.1f", displayValue)
                    } else {
                        displayValue.toInt().toString()
                    },
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = glucoseColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "---",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444),
                    textAlign = TextAlign.Center
                )
            }

            // Trend arrow + unit below the value
            if (displayValue != null) {
                Text(
                    text = "${state.trendArrow.symbol}  ${state.glucoseUnit}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = glucoseColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Battery percentage at the bottom
        Text(
            text = "${batteryLevel}%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .fillMaxWidth()
        )
    }
}

private fun formatTime(context: Context): String {
    val is24Hour = DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "h:mm"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date())
}

private fun getBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}
