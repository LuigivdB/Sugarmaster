package com.sugarmaster.data.repository

import com.sugarmaster.data.api.LibreLinkUpClient
import com.sugarmaster.data.model.Connection
import com.sugarmaster.data.model.GlucoseItem
import com.sugarmaster.data.model.GlucoseMeasurement
import com.sugarmaster.data.model.LoginRequest
import com.sugarmaster.data.store.CredentialStore

sealed class GlucoseResult<out T> {
    data class Success<T>(val data: T) : GlucoseResult<T>()
    data class Error(val message: String) : GlucoseResult<Nothing>()
    data class RegionRedirect(val region: String) : GlucoseResult<Nothing>()
}

class GlucoseRepository(private val credentialStore: CredentialStore) {

    suspend fun login(email: String, password: String, region: String): GlucoseResult<Unit> {
        return try {
            LibreLinkUpClient.setRegion(region)
            LibreLinkUpClient.clearAuth()

            val api = LibreLinkUpClient.getApi()
            val response = api.login(LoginRequest(email, password))

            if (!response.isSuccessful) {
                return GlucoseResult.Error("Login failed: ${response.code()}")
            }

            val body = response.body() ?: return GlucoseResult.Error("Empty response")

            // Handle region redirect
            if (body.data?.redirect == true && !body.data.region.isNullOrBlank()) {
                val newRegion = body.data.region
                credentialStore.saveCredentials(email, password, newRegion)
                return GlucoseResult.RegionRedirect(newRegion)
            }

            val token = body.data?.authTicket?.token
                ?: return GlucoseResult.Error("No auth token received")
            val accountId = body.data.user?.id ?: ""

            LibreLinkUpClient.setAuth(token, accountId)
            credentialStore.saveCredentials(email, password, region)
            credentialStore.saveAuth(token, accountId)

            // Fetch patient ID
            val connectionsResult = fetchConnections()
            if (connectionsResult is GlucoseResult.Error) {
                return connectionsResult
            }

            GlucoseResult.Success(Unit)
        } catch (e: Exception) {
            GlucoseResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun fetchConnections(): GlucoseResult<Unit> {
        return try {
            val api = LibreLinkUpClient.getApi()
            val response = api.getConnections()

            if (!response.isSuccessful) {
                return GlucoseResult.Error("Failed to get connections: ${response.code()}")
            }

            val connections = response.body()?.data
            if (connections.isNullOrEmpty()) {
                return GlucoseResult.Error("No connections found. Set up sharing in LibreLinkUp first.")
            }

            val patientId = connections.first().patientId
                ?: return GlucoseResult.Error("No patient ID found")

            credentialStore.savePatientId(patientId)
            GlucoseResult.Success(Unit)
        } catch (e: Exception) {
            GlucoseResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun getCurrentGlucose(patientId: String): GlucoseResult<GlucoseData> {
        return try {
            val api = LibreLinkUpClient.getApi()
            val response = api.getGraph(patientId)

            if (!response.isSuccessful) {
                return GlucoseResult.Error("Failed to get glucose data: ${response.code()}")
            }

            val graphData = response.body()?.data
                ?: return GlucoseResult.Error("No glucose data")

            val currentMeasurement = graphData.connection?.glucoseMeasurement
            val graphItems = graphData.graphData ?: emptyList()

            if (currentMeasurement == null && graphItems.isEmpty()) {
                return GlucoseResult.Error("No glucose readings available")
            }

            GlucoseResult.Success(
                GlucoseData(
                    currentMeasurement = currentMeasurement,
                    graphItems = graphItems,
                    connection = graphData.connection
                )
            )
        } catch (e: Exception) {
            GlucoseResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun reAuthenticate(): GlucoseResult<Unit> {
        val creds = credentialStore.getStoredCredentials()
            ?: return GlucoseResult.Error("No stored credentials")
        return login(creds.first, creds.second, creds.third)
    }
}

data class GlucoseData(
    val currentMeasurement: GlucoseMeasurement?,
    val graphItems: List<GlucoseItem>,
    val connection: Connection?
)
