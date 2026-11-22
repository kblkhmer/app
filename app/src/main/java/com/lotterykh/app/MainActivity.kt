package com.lotterykh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import coil.load
import com.lotterykh.app.data.AdsApi
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.databinding.ActivityMainBinding
import com.lotterykh.app.model.Ad
import com.lotterykh.app.model.LotteryDraw
import com.lotterykh.app.results.ResultCheckWorker
import com.lotterykh.app.results.ResultNotifier
import com.lotterykh.app.results.ResultPrefs
import com.lotterykh.app.update.InAppUpdater
import com.lotterykh.app.update.UpdateCheckWorker
import com.lotterykh.app.update.UpdateNotifier
import com.lotterykh.app.update.UpdatePrefs
import kotlin.concurrent.thread

class MainActivity : BaseActivity() {

    companion object {
        const val EXTRA_SHOW_UPDATE = "show_update"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var updater: InAppUpdater

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("MainActivity", "Notification permission granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updater = InAppUpdater(this) { binding.root }
        UpdateNotifier.createChannel(this)
        UpdateCheckWorker.schedule(this)
        ResultNotifier.createChannel(this)
        if (ResultPrefs(this).enabled) ResultCheckWorker.schedule(this)
        requestNotificationPermission()
        if (savedInstanceState == null) {
            updater.checkForUpdate(force = intent.getBooleanExtra(EXTRA_SHOW_UPDATE, false))
        }

        applySystemBarInsets(binding.root, binding.homeHeader, binding.homeScroll)
        setSupportActionBar(binding.toolbar)
        // The header shows the logo and app name, so the toolbar only carries the menu icons.
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.cardKhmer.setOnClickListener {
            openLottery("kh", getString(R.string.card_khmer))
        }

        binding.cardVietnam.setOnClickListener {
            openLottery("tc", getString(R.string.card_vietnam))
        }

        binding.cardMinhNam.setOnClickListener {
            openLottery("mn", getString(R.string.card_minhnam))
        }

        binding.cardThai.setOnClickListener {
            openLottery("th", getString(R.string.card_thai))
        }

        binding.cardThaiSou.setOnClickListener {
            openLottery("ts", getString(R.string.card_thaisou))
        }

        binding.cardPrediction.setOnClickListener {
            startActivity(Intent(this, PredictionsActivity::class.java))
        }

        binding.cardYoutubeLive.setOnClickListener {
            startActivity(Intent(this, YoutubeLiveActivity::class.java))
        }

        PlayIntegrityHelper(this).requestIntegrityToken { token, error ->
            if (token != null) {
                Log.d("MainActivity", "Play Integrity token received")
            } else {
                Log.w("MainActivity", "Play Integrity failed: $error")
            }
        }

        loadAds()
        loadLastResult()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_UPDATE, false)) {
            updater.checkForUpdate(force = true)
        }
    }

    /** Android 13+ needs runtime permission to show the "new version" notification. Ask once. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = UpdatePrefs(this)
        if (prefs.notificationPermissionAsked) return
        prefs.notificationPermissionAsked = true
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_result_alerts).isChecked = ResultPrefs(this).enabled
        return true
    }

    private fun toggleResultAlerts(item: MenuItem) {
        val enabled = !item.isChecked
        item.isChecked = enabled
        ResultPrefs(this).enabled = enabled
        if (enabled) {
            ResultCheckWorker.schedule(this)
            if (!UpdateNotifier.canNotify(this)) openNotificationSettings()
        } else {
            ResultCheckWorker.cancel(this)
        }
        Toast.makeText(
            this,
            if (enabled) R.string.result_alerts_on else R.string.result_alerts_off,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Log.w("MainActivity", "Could not open notification settings")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language -> { showLanguageDialog(); true }
            R.id.action_share -> { shareApp(); true }
            R.id.action_result_alerts -> { toggleResultAlerts(item); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openLottery(code: String, title: String) {
        val intent = Intent(this, LotteryActivity::class.java).apply {
            putExtra(LotteryActivity.EXTRA_LOTTERY_CODE, code)
            putExtra(LotteryActivity.EXTRA_LOTTERY_TITLE, title)
        }
        startActivity(intent)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_khmer), getString(R.string.lang_english))
        val codes = arrayOf(LocaleHelper.LANG_KH, LocaleHelper.LANG_EN)
        val currentLang = LocaleHelper.getLanguage(this)
        val checkedItem = codes.indexOf(currentLang).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_dialog_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                dialog.dismiss()
                if (codes[which] != currentLang) {
                    LocaleHelper.setLanguage(this, codes[which])
                    recreate()
                }
            }
            .show()
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    private fun loadLastResult() {
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults()
                val results = LotteryApi.convertToResults(apiResponse)
                val parts = apiResponse.date.split("-")
                val dateDisplay = if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else apiResponse.date

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    binding.tvLastResultDate.text = dateDisplay
                    val container = binding.lastResultContainer
                    container.removeAllViews()
                    // Only each lottery's latest draw; the API lists draws in time order.
                    for (result in results) {
                        val latest = result.draws.lastOrNull() ?: continue
                        addLastResultDrawCard(container, result.title, latest)
                    }
                    binding.lastResultCard.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to load last result: ${e.message}")
            }
        }
    }

    private fun addLastResultDrawCard(container: LinearLayout, title: String, draw: LotteryDraw) {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.item_draw_card, container, false)

        cardView.findViewById<TextView>(R.id.tvTitle).text = title
        cardView.findViewById<TextView>(R.id.tvSession).text = draw.timeSlot
        cardView.findViewById<TextView>(R.id.tvSession).background =
            ContextCompat.getDrawable(this, R.drawable.session_badge_bg)

        val parts = draw.date.split("-")
        val formattedDate = if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else draw.date
        cardView.findViewById<TextView>(R.id.tvDate).text = "Date:  $formattedDate"

        val prizeContainer = cardView.findViewById<LinearLayout>(R.id.prizeContainer)
        for ((index, prize) in draw.prizes.withIndex()) {
            if (prize.twoDigit.isEmpty() && prize.threeDigit.isEmpty()) continue
            val rowView = inflater.inflate(R.layout.item_prize_row, prizeContainer, false)
            rowView.findViewById<TextView>(R.id.tvPrizeLabel).text = prize.label
            rowView.findViewById<TextView>(R.id.tvTwoDigit).text = prize.twoDigit
            rowView.findViewById<TextView>(R.id.tvThreeDigit).text = prize.threeDigit
            if (index % 2 == 1) {
                rowView.setBackgroundColor(ContextCompat.getColor(this, R.color.row_alt_bg))
            }
            prizeContainer.addView(rowView)
        }

        container.addView(cardView)
    }

    private fun loadAds() {
        thread {
            try {
                val response = AdsApi.fetchAds()

                // Show banner ad
                val bannerAd = response.ads
                    .filter { it.position == "banner" }
                    .minByOrNull { it.displayOrder }
                if (bannerAd != null) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) showBannerAd(bannerAd)
                    }
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to load ads: ${e.message}")
            }
        }
    }

    private fun showBannerAd(bannerAd: Ad) {
        // Coil downloads, downsamples and caches the image off the main thread for us.
        binding.bannerAdImage.load(bannerAd.imageUrl, ImageLoaders.get(this)) {
            listener(
                onSuccess = { _, result ->
                    (result.drawable as? Animatable)?.start()
                    binding.bannerAdCard.visibility = View.VISIBLE
                },
                onError = { _, result ->
                    Log.w("MainActivity", "Failed to load banner image: ${result.throwable.message}")
                }
            )
        }
        binding.bannerAdCard.setOnClickListener {
            if (bannerAd.linkUrl.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bannerAd.linkUrl)))
            }
        }
    }
}
