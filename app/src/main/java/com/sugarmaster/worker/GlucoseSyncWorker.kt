package com.sugarmaster.worker

import android.content.Context
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sugarmaster.data.api.LibreLinkUpClient
import com.sugarmaster.data.repository.GlucoseRepository
import com.sugarmaster.data.repository.GlucoseResult
import com.sugarmaster.data.store.CredentialStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class GlucoseSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = CredentialStore(applicationContext)
        val repository = GlucoseRepository(store)

        val isLoggedIn = store.isLoggedIn.first()
        if (!isLoggedIn) return Result.success()

        // Ensure API client is configured
        val token = store.token.first()
        val accountId = store.accountId.first()
        val region = store.region.first()
        LibreLinkUpClient.setRegion(region)
        LibreLinkUpClient.setAuth(token, accountId)

        val patientId = store.patientId.first()
        if (patientId.isBlank()) return Result.failure()

        var result = repository.getCurrentGlucose(patientId)

        if (result is GlucoseResult.Error && result.message.contains("401")) {
            val reAuth = repository.reAuthenticate()
            if (reAuth is GlucoseResult.Success) {
                result = repository.getCurrentGlucose(patientId)
            }
        }

        return when (result) {
            is GlucoseResult.Success -> {
                // Request tile update
                TileService.getUpdater(applicationContext)
                    .requestUpdate(GlucoseTileService::class.java)
                Result.success()
            }
            else -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "glucose_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GlucoseSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    /**
     * Tile service that displays the current glucose value on the watch face.
     */
    class GlucoseTileService : TileService() {

        override fun onTileRequest(request: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
            val tile = TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder().build()
                )
                .build()
            return Futures.immediateFuture(tile)
        }

        override fun onTileResourcesRequest(request: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
            val resources = ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
            return Futures.immediateFuture(resources)
        }
    }
}
