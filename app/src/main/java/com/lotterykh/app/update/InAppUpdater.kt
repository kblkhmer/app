package com.lotterykh.app.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.lotterykh.app.R

/**
 * Checks Google Play for a newer production release and shows a modern bottom sheet.
 * Uses a flexible (background download) update by default, and an immediate (full screen)
 * update when the release is high priority (set via the Play Developer API) or the
 * installed version is very old.
 *
 * Must be constructed before the activity is STARTED (e.g. as a property or in onCreate).
 */
class InAppUpdater(
    private val activity: AppCompatActivity,
    private val snackbarAnchor: () -> View
) : DefaultLifecycleObserver {

    private val manager = AppUpdateManagerFactory.create(activity)
    private val prefs = UpdatePrefs(activity)
    private var sheet: BottomSheetDialog? = null
    private var progressSnackbar: Snackbar? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.i(TAG, "Update flow not completed: resultCode=${result.resultCode}")
            prefs.immediateUpdateInProgress = false
        }
    }

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val percent = if (total > 0) (state.bytesDownloaded() * 100 / total).toInt() else 0
                showDownloadProgress(percent)
            }
            InstallStatus.DOWNLOADED -> showRestartPrompt()
            InstallStatus.FAILED -> {
                progressSnackbar?.dismiss()
                Snackbar.make(snackbarAnchor(), R.string.update_failed, Snackbar.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    init {
        activity.lifecycle.addObserver(this)
        manager.registerListener(installListener)
    }

    /** @param force ignore the "Later" snooze, e.g. when opened from the update notification. */
    fun checkForUpdate(force: Boolean = false) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (activity.isFinishing || activity.isDestroyed) return@addOnSuccessListener
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    showRestartPrompt()
                    return@addOnSuccessListener
                }
                if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
                val version = info.availableVersionCode()
                if (!force && prefs.isSnoozed(version)) return@addOnSuccessListener

                // User has now seen this release in-app; the background worker needn't notify too.
                if (version > prefs.lastNotifiedVersion) prefs.lastNotifiedVersion = version
                UpdateNotifier.cancel(activity)
                showUpdateSheet(info)
            }
            .addOnFailureListener { Log.w(TAG, "Update check failed: ${it.message}") }
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (activity.isFinishing || activity.isDestroyed) return@addOnSuccessListener
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> showRestartPrompt()
                // An immediate update was interrupted (app backgrounded) — resume it.
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    prefs.immediateUpdateInProgress -> startUpdate(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        manager.unregisterListener(installListener)
        sheet?.dismiss()
        sheet = null
    }

    private fun showUpdateSheet(info: AppUpdateInfo) {
        if (sheet?.isShowing == true) return

        val important = isHighPriority(info)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_update, null)
        val dialog = BottomSheetDialog(activity, R.style.ThemeOverlay_LotteryKH_BottomSheetDialog)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        view.findViewById<TextView>(R.id.tvUpdateVersion).text =
            activity.getString(R.string.update_version, info.availableVersionCode())

        val badge = view.findViewById<TextView>(R.id.tvUpdateBadge)
        val later = view.findViewById<MaterialButton>(R.id.btnUpdateLater)
        if (important) {
            badge.setText(R.string.update_badge_important)
            badge.setTextColor(ContextCompat.getColor(activity, R.color.white))
            badge.background.mutate().setTint(ContextCompat.getColor(activity, R.color.red_dark))
            later.visibility = View.GONE
            dialog.setCancelable(false)
        }

        view.findViewById<MaterialButton>(R.id.btnUpdateNow).setOnClickListener {
            dialog.dismiss()
            val type = when {
                important && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                else -> null
            }
            if (type != null) startUpdate(info, type) else openPlayStore()
        }
        later.setOnClickListener {
            prefs.snooze(info.availableVersionCode())
            dialog.dismiss()
        }

        dialog.setOnDismissListener { sheet = null }
        sheet = dialog
        dialog.show()
    }

    private fun isHighPriority(info: AppUpdateInfo): Boolean =
        info.updatePriority() >= 4 || (info.clientVersionStalenessDays() ?: 0) >= STALE_DAYS

    private fun startUpdate(info: AppUpdateInfo, type: Int) {
        try {
            prefs.immediateUpdateInProgress = type == AppUpdateType.IMMEDIATE
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        } catch (e: Exception) {
            Log.w(TAG, "Could not start update flow: ${e.message}")
            prefs.immediateUpdateInProgress = false
            openPlayStore()
        }
    }

    private fun showDownloadProgress(percent: Int) {
        val text = activity.getString(R.string.update_downloading, percent)
        val bar = progressSnackbar
        if (bar != null && bar.isShownOrQueued) {
            bar.setText(text)
        } else {
            progressSnackbar = Snackbar.make(snackbarAnchor(), text, Snackbar.LENGTH_INDEFINITE)
                .also { it.show() }
        }
    }

    private fun showRestartPrompt() {
        progressSnackbar?.dismiss()
        progressSnackbar = Snackbar.make(
            snackbarAnchor(), R.string.update_downloaded, Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.update_restart) {
            manager.completeUpdate()
        }.setActionTextColor(ContextCompat.getColor(activity, R.color.yellow_badge))
            .also { it.show() }
    }

    private fun openPlayStore() {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(UpdateNotifier.playStoreUri(activity)))
                    .setPackage("com.android.vending")
            )
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(UpdateNotifier.playStoreWebUri(activity)))
            )
        }
    }

    companion object {
        private const val TAG = "InAppUpdater"
        private const val STALE_DAYS = 30
    }
}
