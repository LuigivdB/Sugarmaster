package com.sugarmaster.data.api

import com.sugarmaster.data.model.ConnectionsResponse
import com.sugarmaster.data.model.GraphResponse
import com.sugarmaster.data.model.LoginRequest
import com.sugarmaster.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LibreLinkUpApi {

    @POST("llu/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("llu/connections")
    suspend fun getConnections(): Response<ConnectionsResponse>

    @GET("llu/connections/{patientId}/graph")
    suspend fun getGraph(@Path("patientId") patientId: String): Response<GraphResponse>
}
