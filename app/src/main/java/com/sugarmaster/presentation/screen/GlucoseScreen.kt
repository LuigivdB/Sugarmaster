package com.sugarmaster.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.sugarmaster.data.model.GlucoseItem
import com.sugarmaster.presentation.GlucoseUiState
import com.sugarmaster.presentation.theme.GlucoseBlue
import com.sugarmaster.presentation.theme.GlucoseRed
import com.sugarmaster.presentation.theme.GlucoseWhite
import com.sugarmaster.presentation.theme.GlucoseYellow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GlucoseScreen(
    state: GlucoseUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 8.dp)
        ) {
            // Current time at the top
            item {
                CurrentTimeDisplay()
            }

            // Main glucose value display
            item {
                GlucoseValueDisplay(state)
            }

            // Mini graph
            if (state.graphItems.isNotEmpty()) {
                item {
                    GlucoseMiniGraph(
                        items = state.graphItems,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            // Last reading timestamp
            if (state.timestamp != null) {
                item {
                    Text(
                        text = formatTimestamp(state.timestamp),
                        style = MaterialTheme.typography.caption3,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Error message
            if (state.error != null) {
                item {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.caption3,
                        color = GlucoseRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Action buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("↻", fontSize = 16.sp)
                        }
                    }

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("✕", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTimeDisplay() {
    var currentTime by remember { mutableStateOf(formatCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatCurrentTime()
            delay(1000L)
        }
    }

    Text(
        text = currentTime,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF888888),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

@Composable
private fun GlucoseValueDisplay(state: GlucoseUiState) {
    // Color based on status: red = too high, white = good, blue = too low
    val glucoseColor = when {
        state.isLow -> GlucoseBlue
        state.isHigh -> GlucoseRed
        state.currentValueMgDl != null && state.currentValueMgDl < 70 -> GlucoseBlue
        state.currentValueMgDl != null && state.currentValueMgDl > 180 -> GlucoseRed
        else -> GlucoseWhite
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Glucose value — large
            val displayValue = state.currentValue
            if (displayValue != null) {
                Text(
                    text = if (state.glucoseUnit == "mmol/L") {
                        String.format("%.1f", displayValue)
                    } else {
                        displayValue.toInt().toString()
                    },
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = glucoseColor
                )
            } else {
                Text(
                    text = "---",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Trend arrow + unit
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.trendArrow.symbol,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = glucoseColor
                )
                Text(
                    text = state.glucoseUnit,
                    fontSize = 9.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        // Status indicator dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(glucoseColor, CircleShape)
        )
    }
}

@Composable
fun GlucoseMiniGraph(
    items: List<GlucoseItem>,
    modifier: Modifier = Modifier
) {
    val lowThreshold = 70f
    val highThreshold = 180f

    Canvas(modifier = modifier) {
        if (items.isEmpty()) return@Canvas

        val values = items.mapNotNull { it.valueInMgPerDl?.toFloat() }
        if (values.isEmpty()) return@Canvas

        val minVal = (values.min() - 10f).coerceAtLeast(40f)
        val maxVal = (values.max() + 10f).coerceAtMost(400f)
        val range = maxVal - minVal

        val padding = 4.dp.toPx()
        val graphWidth = size.width - padding * 2
        val graphHeight = size.height - padding * 2

        // Draw range bands
        val lowY = padding + graphHeight * (1f - (lowThreshold - minVal) / range)
        val highY = padding + graphHeight * (1f - (highThreshold - minVal) / range)

        // Low range line (blue zone)
        if (lowThreshold > minVal) {
            drawLine(
                color = GlucoseBlue.copy(alpha = 0.25f),
                start = Offset(padding, lowY),
                end = Offset(size.width - padding, lowY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // High range line (red zone)
        if (highThreshold < maxVal) {
            drawLine(
                color = GlucoseRed.copy(alpha = 0.25f),
                start = Offset(padding, highY),
                end = Offset(size.width - padding, highY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw glucose line
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (values.size - 1).coerceAtLeast(1)) * graphWidth
            val y = padding + graphHeight * (1f - (value - minVal) / range)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.7f),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw current value dot
        if (values.isNotEmpty()) {
            val lastX = size.width - padding
            val lastY = padding + graphHeight * (1f - (values.last() - minVal) / range)
            val dotColor = when {
                values.last() < lowThreshold -> GlucoseBlue
                values.last() > highThreshold -> GlucoseRed
                else -> Color.White
            }
            drawCircle(
                color = dotColor,
                radius = 3.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val parts = timestamp.split(" ")
        if (parts.size >= 2) {
            parts.drop(1).joinToString(" ")
        } else {
            timestamp
        }
    } catch (_: Exception) {
        timestamp
    }
}
