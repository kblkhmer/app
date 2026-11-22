package com.lotterykh.app.results

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.lotterykh.app.LotteryActivity
import com.lotterykh.app.MainActivity
import com.lotterykh.app.R
import com.lotterykh.app.model.Prize
import com.lotterykh.app.update.UpdateNotifier

/** A freshly completed draw, ready to be shown as a notification. */
data class NewDraw(
    val date: String,
    val lotteryCode: String,
    val lotteryName: String,
    val timeSlot: String,
    val prizes: List<Prize>
)

/** Heads-up notifications for new lottery results, one per lottery, bundled into a group. */
object ResultNotifier {

    private const val CHANNEL_ID = "lottery_results"
    private const val GROUP_KEY = "com.homelottery.app.RESULTS"
    private const val SUMMARY_ID = 2000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_results),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_results_desc)
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, draws: List<NewDraw>) {
        if (draws.isEmpty() || !UpdateNotifier.canNotify(context)) return
        createChannel(context)
        val manager = NotificationManagerCompat.from(context)

        try {
            for (draw in draws) {
                manager.notify(notificationId(draw.lotteryCode), buildDraw(context, draw))
            }
            if (draws.size > 1) {
                manager.notify(SUMMARY_ID, buildSummary(context, draws))
            }
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    private fun buildDraw(context: Context, draw: NewDraw) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_result)
            .setColor(colorFor(draw.lotteryCode))
            .setColorized(false)
            .setContentTitle(context.getString(R.string.notif_result_title, draw.lotteryName, draw.timeSlot))
            // Collapsed: compact "A 17 · B 59 · C 47 · D 32"
            .setContentText(draw.prizes.joinToString("  ·  ") { "${it.label} ${it.twoDigit}" })
            // Expanded: one line per prize with 2D and 3D numbers
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    draw.prizes.take(6).forEach { p ->
                        val three = if (p.threeDigit.isNotEmpty()) "   │   ${p.threeDigit}" else ""
                        style.addLine("${p.label}    ${p.twoDigit}$three")
                    }
                }
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(openLottery(context, draw))
            .addAction(
                R.drawable.ic_notification_result,
                context.getString(R.string.notif_result_view),
                openLottery(context, draw)
            )
            .build()

    private fun buildSummary(context: Context, draws: List<NewDraw>) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_result)
            .setColor(Color.parseColor("#D32F2F"))
            .setContentTitle(context.getString(R.string.notif_result_summary, draws.size))
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    draws.forEach { style.addLine("${it.lotteryName} · ${it.timeSlot}") }
                }
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

    /** Opens the lottery's result screen with the home screen underneath for Back. */
    private fun openLottery(context: Context, draw: NewDraw): PendingIntent {
        val home = Intent(context, MainActivity::class.java)
        val lottery = Intent(context, LotteryActivity::class.java).apply {
            putExtra(LotteryActivity.EXTRA_LOTTERY_CODE, draw.lotteryCode)
            putExtra(LotteryActivity.EXTRA_LOTTERY_TITLE, draw.lotteryName)
            putExtra(LotteryActivity.EXTRA_DATE, draw.date)
            putExtra(LotteryActivity.EXTRA_TIME_SLOT, draw.timeSlot)
        }
        return TaskStackBuilder.create(context)
            .addNextIntent(home)
            .addNextIntent(lottery)
            .getPendingIntent(
                notificationId(draw.lotteryCode),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
    }

    private fun notificationId(code: String) = SUMMARY_ID + 1 + (code.hashCode() and 0xFFFF)

    // Same colours as the home-screen cards.
    private fun colorFor(code: String) = Color.parseColor(
        when (code) {
            "kh" -> "#FF7301"
            "tc" -> "#E53935"
            "th" -> "#388E3C"
            "mn" -> "#00897B"
            "ts" -> "#3949AB"
            else -> "#D32F2F"
        }
    )
}
