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
import androidx.compose.foundation.layout.offset
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
import androidx.wear.compose.material.Text
import com.sugarmaster.presentation.GlucoseUiState
import com.sugarmaster.presentation.theme.GlucoseBlue
import com.sugarmaster.presentation.theme.GlucoseRed
import com.sugarmaster.presentation.theme.GlucoseWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlucoseScreen(
    state: GlucoseUiState,
    isAmbient: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // Time — updates once per minute, synced to the minute boundary
    var currentTime by remember { mutableStateOf(formatTime(context)) }
    LaunchedEffect(isAmbient) {
        while (true) {
            currentTime = formatTime(context)
            // Sleep until the next minute starts
            val now = Calendar.getInstance()
            val msUntilNextMinute = (60 - now.get(Calendar.SECOND)) * 1000L -
                    now.get(Calendar.MILLISECOND)
            delay(msUntilNextMinute.coerceAtLeast(1000L))
        }
    }

    // Battery level — updates every 5 minutes
    var batteryLevel by remember { mutableIntStateOf(getBatteryLevel(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            batteryLevel = getBatteryLevel(context)
            delay(300_000L) // 5 minutes
        }
    }

    // Glucose color — white only in ambient mode (OLED burn-in protection)
    val glucoseColor = if (isAmbient) {
        Color.White
    } else {
        when {
            state.isLow -> GlucoseBlue
            state.isHigh -> GlucoseRed
            state.currentValueMgDl != null && state.currentValueMgDl < 70 -> GlucoseBlue
            state.currentValueMgDl != null && state.currentValueMgDl > 180 -> GlucoseRed
            else -> GlucoseWhite
        }
    }

    // Anti burn-in: shift content slightly each minute in ambient mode
    val burnInOffset = if (isAmbient) {
        val minute = Calendar.getInstance().get(Calendar.MINUTE)
        ((minute % 5) - 2).dp
    } else {
        0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .offset(x = burnInOffset, y = burnInOffset)
            .then(
                if (!isAmbient) {
                    Modifier.combinedClickable(
                        onClick = onRefresh,
                        onLongClick = onLogout
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Time at the top
        Text(
            text = currentTime,
            fontSize = if (isAmbient) 16.sp else 18.sp,
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
                    fontSize = if (isAmbient) 56.sp else 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = glucoseColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "---",
                    fontSize = if (isAmbient) 56.sp else 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAmbient) Color(0xFF333333) else Color(0xFF444444),
                    textAlign = TextAlign.Center
                )
            }

            // Trend arrow + unit below the value
            if (displayValue != null && !isAmbient) {
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
            fontSize = if (isAmbient) 14.sp else 16.sp,
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
