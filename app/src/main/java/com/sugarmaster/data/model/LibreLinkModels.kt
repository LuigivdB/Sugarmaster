package com.sugarmaster.data.model

import com.google.gson.annotations.SerializedName

// --- Auth ---

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val status: Int,
    val data: AuthData?
)

data class AuthData(
    val user: User?,
    val authTicket: AuthTicket?,
    val redirect: Boolean?,
    val region: String?
)

data class User(
    val id: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?
)

data class AuthTicket(
    val token: String?,
    val expires: Long?,
    val duration: Long?
)

// --- Connections ---

data class ConnectionsResponse(
    val status: Int,
    val data: List<Connection>?
)

data class Connection(
    val id: String?,
    val patientId: String?,
    val firstName: String?,
    val lastName: String?,
    val glucoseMeasurement: GlucoseMeasurement?,
    val glucoseItem: GlucoseItem?,
    val sensor: Sensor?
)

data class Sensor(
    val deviceId: String?,
    val sn: String?,
    @SerializedName("a")
    val activated: Long?,
    val w: Int?,
    val pt: Int?
)

// --- Glucose Data ---

data class GraphResponse(
    val status: Int,
    val data: GraphData?
)

data class GraphData(
    @SerializedName("connection")
    val connection: Connection?,
    @SerializedName("activeSensors")
    val activeSensors: List<ActiveSensor>?,
    @SerializedName("graphData")
    val graphData: List<GlucoseItem>?
)

data class ActiveSensor(
    val sensor: Sensor?,
    val device: Device?
)

data class Device(
    val did: String?,
    val dtid: Int?,
    val v: String?,
    val ll: Int?,
    val hl: Int?,
    val fixedLowAlarmValues: AlarmValues?,
    val fixedHighAlarmValues: AlarmValues?
)

data class AlarmValues(
    val mgDl: Double?,
    val mmolL: Double?
)

data class GlucoseMeasurement(
    @SerializedName("FactoryTimestamp")
    val factoryTimestamp: String?,
    @SerializedName("Timestamp")
    val timestamp: String?,
    val type: Int?,
    @SerializedName("ValueInMgPerDl")
    val valueInMgPerDl: Double?,
    @SerializedName("TrendArrow")
    val trendArrow: Int?,
    @SerializedName("TrendMessage")
    val trendMessage: String?,
    @SerializedName("MeasurementColor")
    val measurementColor: Int?,
    @SerializedName("GlucoseUnits")
    val glucoseUnits: Int?,
    @SerializedName("Value")
    val value: Double?,
    val isHigh: Boolean?,
    val isLow: Boolean?
)

data class GlucoseItem(
    @SerializedName("FactoryTimestamp")
    val factoryTimestamp: String?,
    @SerializedName("Timestamp")
    val timestamp: String?,
    val type: Int?,
    @SerializedName("ValueInMgPerDl")
    val valueInMgPerDl: Double?,
    @SerializedName("TrendArrow")
    val trendArrow: Int?,
    @SerializedName("MeasurementColor")
    val measurementColor: Int?,
    @SerializedName("GlucoseUnits")
    val glucoseUnits: Int?,
    @SerializedName("Value")
    val value: Double?,
    val isHigh: Boolean?,
    val isLow: Boolean?
)

// --- Trend Arrow Mapping ---

enum class TrendArrow(val code: Int, val symbol: String, val description: String) {
    NOT_DETERMINED(0, "?", "Not determined"),
    FALLING_FAST(1, "↓↓", "Falling fast"),
    FALLING(2, "↓", "Falling"),
    STABLE(3, "→", "Stable"),
    RISING(4, "↑", "Rising"),
    RISING_FAST(5, "↑↑", "Rising fast");

    companion object {
        fun fromCode(code: Int?): TrendArrow =
            entries.firstOrNull { it.code == code } ?: NOT_DETERMINED
    }
}

// --- Measurement Color Mapping ---

enum class GlucoseRangeColor(val code: Int) {
    GREEN(1),   // In range
    YELLOW(2),  // Low
    ORANGE(3),  // High
    RED(4);     // Very high/low

    companion object {
        fun fromCode(code: Int?): GlucoseRangeColor =
            entries.firstOrNull { it.code == code } ?: GREEN
    }
}
