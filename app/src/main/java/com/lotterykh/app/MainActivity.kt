package com.lotterykh.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lotterykh.app.data.AdsApi
import com.lotterykh.app.databinding.ActivityMainBinding
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    shareApp()
                    true
                }
                else -> false
            }
        }

        binding.cardKhmer.setOnClickListener {
            openLottery("kh", getString(R.string.card_khmer))
        }

        binding.cardVietnam.setOnClickListener {
            openLottery("tc", getString(R.string.card_vietnam))
        }

        binding.cardThai.setOnClickListener {
            openLottery("th", getString(R.string.card_thai))
        }

        binding.cardPrediction.setOnClickListener {
            Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
        }

        PlayIntegrityHelper(this).requestIntegrityToken { token, error ->
            if (token != null) {
                Log.d("MainActivity", "Play Integrity token received")
            } else {
                Log.w("MainActivity", "Play Integrity failed: $error")
            }
        }

        loadAds()
    }

    private fun openLottery(code: String, title: String) {
        val intent = Intent(this, LotteryActivity::class.java).apply {
            putExtra(LotteryActivity.EXTRA_LOTTERY_CODE, code)
            putExtra(LotteryActivity.EXTRA_LOTTERY_TITLE, title)
        }
        startActivity(intent)
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    private fun loadAds() {
        thread {
            try {
                val response = AdsApi.fetchAds()

                // Show popup ad
                val popupAd = response.ads
                    .filter { it.position == "popup" }
                    .minByOrNull { it.displayOrder }
                if (popupAd != null) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            AdPopupDialog.newInstance(popupAd)
                                .show(supportFragmentManager, "ad_popup")
                        }
                    }
                }

                // Show banner ad
                val bannerAd = response.ads
                    .filter { it.position == "banner" }
                    .minByOrNull { it.displayOrder }
                if (bannerAd != null) {
                    try {
                        val url = URL(bannerAd.imageUrl)
                        val conn = url.openConnection()
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                binding.bannerAdImage.setImageBitmap(bitmap)
                                binding.bannerAdCard.visibility = View.VISIBLE
                                binding.bannerAdCard.setOnClickListener {
                                    if (bannerAd.linkUrl.isNotEmpty()) {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bannerAd.linkUrl)))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("MainActivity", "Failed to load banner image: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to load ads: ${e.message}")
            }
        }
    }
}
