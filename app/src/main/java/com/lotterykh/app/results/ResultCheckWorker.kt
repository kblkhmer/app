package com.lotterykh.app.results

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lotterykh.app.LocaleHelper
import com.lotterykh.app.R
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.model.ApiResponse
import com.lotterykh.app.model.Prize
import java.util.concurrent.TimeUnit

/**
 * Polls today's results and posts a notification for every draw that has just been
 * completed since the last check. Runs every 15 minutes (Android's minimum for periodic work).
 */
class ResultCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = ResultPrefs(applicationContext)
        if (!prefs.enabled) return Result.success()

        return try {
            val response = LotteryApi.fetchResults()
            val completed = completedDraws(response)
            val keys = completed.keys

            val firstRun = prefs.seenDate == null
            val seen = if (prefs.seenDate == response.date) prefs.seenDraws else emptySet()
            prefs.saveSeen(response.date, keys)

            // On the very first run just record the baseline so the user isn't flooded.
            if (!firstRun) {
                val context = LocaleHelper.applyLocale(applicationContext)
                val newDraws = completed.filterKeys { it !in seen }.values
                    // If several draws of the same lottery arrived at once, only alert the latest.
                    .groupBy { it.lotteryCode }
                    .map { (_, draws) -> draws.last() }
                    .map { it.copy(lotteryName = localizedName(context, it.lotteryCode, it.lotteryName)) }
                ResultNotifier.show(context, newDraws)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Result check failed: ${e.message}")
            Result.retry()
        }
    }

    /** Draws whose main prizes are all published, keyed by "date|lottery|timeSlot", in API order. */
    private fun completedDraws(response: ApiResponse): LinkedHashMap<String, NewDraw> {
        val out = LinkedHashMap<String, NewDraw>()
        for (lottery in response.lotteries) {
            for (slot in lottery.results) {
                // Same "main prize" rule as LotteryApi.convertToResults.
                val main = slot.groups.filter { it.group.length <= 2 && !it.group.startsWith("LO") }
                if (main.isEmpty()) continue
                val done = main.all { it.status.equals("completed", ignoreCase = true) && it.result2d.isNotBlank() }
                if (!done) continue
                out["${response.date}|${lottery.lottery}|${slot.timeSlot}"] = NewDraw(
                    date = response.date,
                    lotteryCode = lottery.lottery,
                    lotteryName = lottery.name,
                    timeSlot = slot.timeSlot,
                    prizes = main.map { Prize(it.group, it.result2d, it.result3d ?: "") }
                )
            }
        }
        return out
    }

    private fun localizedName(context: Context, code: String, fallback: String) = when (code) {
        "kh" -> context.getString(R.string.card_khmer)
        "tc" -> context.getString(R.string.card_vietnam)
        "th" -> context.getString(R.string.card_thai)
        "mn" -> context.getString(R.string.card_minhnam)
        "ts" -> context.getString(R.string.card_thaisou)
        else -> fallback
    }

    companion object {
        private const val TAG = "ResultCheckWorker"
        private const val WORK_NAME = "lottery_result_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResultCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
