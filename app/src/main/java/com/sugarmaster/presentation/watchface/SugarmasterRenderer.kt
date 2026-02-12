package com.sugarmaster.presentation.watchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.BatteryManager
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime

private const val GLUCOSE_COLOR_WHITE = 0xFFFFFFFF.toInt()
private const val GLUCOSE_COLOR_HIGH = 0xFFEA7B7A.toInt()
private const val GLUCOSE_COLOR_LOW = 0xFF92D4F0.toInt()
private const val COLOR_DIM = 0xFF444444.toInt()
private const val COLOR_DIM_AMBIENT = 0xFF333333.toInt()

class SugarmasterRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    private val dataProvider: () -> CachedGlucoseData
) : Renderer.CanvasRenderer2<SugarmasterRenderer.Assets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 60_000L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    class Assets : SharedAssets {
        override fun onDestroy() {}
    }

    private val is24Hour get() = DateFormat.is24HourFormat(context)
    private val sansSerif = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val sansSerifBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GLUCOSE_COLOR_WHITE
        textSize = spToPx(18f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(sansSerif, Typeface.NORMAL)
    }

    private val glucosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GLUCOSE_COLOR_WHITE
        textSize = spToPx(64f)
        textAlign = Paint.Align.CENTER
        typeface = sansSerifBold
    }

    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GLUCOSE_COLOR_WHITE
        textSize = spToPx(14f)
        textAlign = Paint.Align.CENTER
        typeface = sansSerif
    }

    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GLUCOSE_COLOR_WHITE
        textSize = spToPx(16f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(sansSerif, Typeface.NORMAL)
    }

    override suspend fun createSharedAssets(): Assets = Assets()

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Assets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val data = dataProvider()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()

        // Black background
        canvas.drawColor(android.graphics.Color.BLACK)

        // Anti burn-in offset in ambient mode
        val offset = if (isAmbient) {
            val minute = zonedDateTime.minute
            ((minute % 5) - 2).toFloat() * context.resources.displayMetrics.density
        } else 0f

        // --- Time at the top ---
        val timeText = formatTime(zonedDateTime)
        timePaint.textSize = spToPx(if (isAmbient) 16f else 18f)
        canvas.drawText(
            timeText,
            centerX + offset,
            bounds.top + spToPx(28f) + timePaint.textSize + offset,
            timePaint
        )

        // --- Glucose value in the center ---
        val glucoseColor = if (isAmbient) {
            GLUCOSE_COLOR_WHITE
        } else {
            when {
                data.isLow -> GLUCOSE_COLOR_LOW
                data.isHigh -> GLUCOSE_COLOR_HIGH
                data.valueMgDl != null && data.valueMgDl < 70 -> GLUCOSE_COLOR_LOW
                data.valueMgDl != null && data.valueMgDl > 180 -> GLUCOSE_COLOR_HIGH
                else -> GLUCOSE_COLOR_WHITE
            }
        }

        glucosePaint.textSize = spToPx(if (isAmbient) 56f else 64f)
        glucosePaint.color = glucoseColor

        val glucoseText = when {
            data.value == null -> "---"
            data.unit == "mmol/L" -> String.format("%.1f", data.value)
            else -> data.value.toInt().toString()
        }

        // Center the glucose text vertically
        val glucoseY = centerY + (glucosePaint.textSize / 3f)
        canvas.drawText(glucoseText, centerX + offset, glucoseY + offset, glucosePaint)

        // Dim color for "---"
        if (data.value == null) {
            glucosePaint.color = if (isAmbient) COLOR_DIM_AMBIENT else COLOR_DIM
            canvas.drawText(glucoseText, centerX + offset, glucoseY + offset, glucosePaint)
        }

        // --- Trend arrow + unit below glucose (interactive only) ---
        if (data.value != null && !isAmbient) {
            subPaint.color = (glucoseColor and 0x00FFFFFF) or 0xB3000000.toInt() // 70% alpha
            val subText = "${data.trendArrow.symbol}  ${data.unit}"
            canvas.drawText(subText, centerX, glucoseY + spToPx(20f), subPaint)
        }

        // --- Battery at the bottom ---
        val batteryLevel = getBatteryLevel()
        batteryPaint.textSize = spToPx(if (isAmbient) 14f else 16f)
        canvas.drawText(
            "$batteryLevel%",
            centerX + offset,
            bounds.bottom - spToPx(20f) + offset,
            batteryPaint
        )
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Assets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)
    }

    private fun formatTime(zonedDateTime: ZonedDateTime): String {
        val hour = if (is24Hour) {
            zonedDateTime.hour
        } else {
            val h = zonedDateTime.hour % 12
            if (h == 0) 12 else h
        }
        return String.format("%d:%02d", hour, zonedDateTime.minute)
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
    }
}
