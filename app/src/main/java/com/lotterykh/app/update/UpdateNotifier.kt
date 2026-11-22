package com.lotterykh.app.update

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.lotterykh.app.MainActivity
import com.lotterykh.app.R

/** Posts a heads-up system notification when a new production release is live on Google Play. */
object UpdateNotifier {

    // Android locks a channel's sound and vibration once created, so changing them needs a new id.
    private const val CHANNEL_ID = "app_updates_v2"
    private const val OLD_CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 1001
    private val VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)
    private const val LARGE_ICON_PX = 192

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val oldChannel = manager.getNotificationChannel(OLD_CHANNEL_ID)
        // Keep the alert off for anyone who had turned off the old channel.
        val importance = if (oldChannel?.importance == NotificationManager.IMPORTANCE_NONE) {
            NotificationManager.IMPORTANCE_NONE
        } else {
            NotificationManager.IMPORTANCE_HIGH
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_updates),
            importance
        ).apply {
            description = context.getString(R.string.notif_channel_updates_desc)
            enableLights(true)
            lightColor = ContextCompat.getColor(context, R.color.red_primary)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        if (oldChannel != null) manager.deleteNotificationChannel(OLD_CHANNEL_ID)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** @return false if the notification couldn't be posted, so the caller can try again later. */
    fun showUpdateAvailable(context: Context, versionCode: Int): Boolean {
        if (!canNotify(context)) return false
        createChannel(context)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Tapping the notification opens the app, which shows the in-app update sheet.
        val openApp = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_SHOW_UPDATE, true)
        }
        val contentIntent = PendingIntent.getActivity(context, 0, openApp, flags)

        // "Update now" action jumps straight to the Play Store listing.
        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri(context))).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val updateAction = PendingIntent.getActivity(context, 1, playIntent, flags)

        val text = context.getString(R.string.notif_update_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_update)
            .setLargeIcon(ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap(LARGE_ICON_PX, LARGE_ICON_PX))
            .setColor(ContextCompat.getColor(context, R.color.red_primary))
            .setContentTitle(context.getString(R.string.notif_update_title))
            .setContentText(text)
            .setSubText(context.getString(R.string.update_version, versionCode))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .setVibrate(VIBRATION_PATTERN)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_system_update, context.getString(R.string.update_now), updateAction)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
            false
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun playStoreUri(context: Context) = "market://details?id=${context.packageName}"

    fun playStoreWebUri(context: Context) =
        "https://play.google.com/store/apps/details?id=${context.packageName}"
}
