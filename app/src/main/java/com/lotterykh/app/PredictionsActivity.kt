package com.lotterykh.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.lotterykh.app.data.AdsApi
import com.lotterykh.app.data.PredictionsApi
import com.lotterykh.app.databinding.ActivityPredictionsBinding
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class PredictionsActivity : BaseActivity() {

    private lateinit var binding: ActivityPredictionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPredictionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets(binding.root, binding.predictionsHeader, binding.predictionsScroll)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.card_prediction)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rv2d.layoutManager = GridLayoutManager(this, 2)
        binding.rv3d.layoutManager = GridLayoutManager(this, 2)

        loadPredictions()
        loadPopupAd()
    }

    private fun loadPopupAd() {
        thread {
            try {
                val response = AdsApi.fetchAds()
                val popupAd = response.ads.filter { it.position == "popup" }.minByOrNull { it.displayOrder }
                if (popupAd != null) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved) {
                            AdPopupDialog.newInstance(popupAd).show(supportFragmentManager, "ad_popup")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PredictionsActivity", "Failed to load popup ad: ${e.message}")
            }
        }
    }

    private fun loadPredictions() {
        Log.d("PredictionsActivity", "loadPredictions started")
        binding.progressBar.visibility = View.VISIBLE

        thread {
            try {
                Log.d("PredictionsActivity", "Fetching predictions...")
                val response = PredictionsApi.fetchPredictions()
                Log.d("PredictionsActivity", "Got response: ${response.numbers2d.size} 2D, ${response.numbers3d.size} 3D")
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE

                    if (response.numbers2d.isEmpty() && response.numbers3d.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        return@runOnUiThread
                    }

                    // Format date
                    try {
                        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        val date = apiFormat.parse(response.date)
                        if (date != null) {
                            binding.tvDate.text = displayFormat.format(date)
                        }
                    } catch (_: Exception) {
                        binding.tvDate.text = response.date
                    }

                    binding.rv2d.adapter = PredictionNumberAdapter(response.numbers2d)
                    binding.rv3d.adapter = PredictionNumberAdapter(response.numbers3d)
                }
            } catch (e: Exception) {
                Log.e("PredictionsActivity", "Failed to load predictions", e)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }
}
