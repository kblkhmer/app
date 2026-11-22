package com.lotterykh.app.update

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.lotterykh.app.LocaleHelper
import java.util.concurrent.TimeUnit

/**
 * Periodically asks Google Play whether a newer production release exists and, if so,
 * posts a system notification once per new versionCode — even when the app isn't open.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val info = Tasks.await(
                AppUpdateManagerFactory.create(applicationContext).appUpdateInfo,
                30, TimeUnit.SECONDS
            )
            val prefs = UpdatePrefs(applicationContext)
            val available = info.availableVersionCode()
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                available > prefs.lastNotifiedVersion &&
                UpdateNotifier.showUpdateAvailable(LocaleHelper.applyLocale(applicationContext), available)
            ) {
                prefs.lastNotifiedVersion = available
            }
            Result.success()
        } catch (e: Exception) {
            // Not retried: Play errors (e.g. app not installed from Play) won't fix themselves,
            // and the next periodic run is only 3 hours away.
            Log.w(TAG, "Update check failed: ${e.message}")
            Result.success()
        }
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val WORK_NAME = "play_update_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(3, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            // UPDATE, not KEEP: KEEP would leave existing installs on their old check interval.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }
    }
}
