package com.lotterykh.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.lotterykh.app.data.AdsApi
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.databinding.ActivityLotteryBinding
import com.lotterykh.app.model.LotteryResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class LotteryActivity : BaseActivity() {

    private lateinit var binding: ActivityLotteryBinding
    private var lotteryCode: String? = null
    private var requestToken = 0
    private var tabLayoutMediator: TabLayoutMediator? = null
    private val calendar = Calendar.getInstance()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    companion object {
        const val EXTRA_LOTTERY_CODE = "lottery_code"
        const val EXTRA_LOTTERY_TITLE = "lottery_title"
        /** Optional "yyyy-MM-dd" to open on instead of today. */
        const val EXTRA_DATE = "lottery_date"
        /** Optional draw time slot (e.g. "08:35 PM") to scroll to. */
        const val EXTRA_TIME_SLOT = "lottery_time_slot"
    }

    private var focusTimeSlot: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLotteryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lotteryCode = intent.getStringExtra(EXTRA_LOTTERY_CODE)
        val lotteryTitle = intent.getStringExtra(EXTRA_LOTTERY_TITLE) ?: ""
        focusTimeSlot = intent.getStringExtra(EXTRA_TIME_SLOT)
        intent.getStringExtra(EXTRA_DATE)?.let { date ->
            runCatching { apiDateFormat.parse(date) }.getOrNull()?.let { calendar.time = it }
        }

        applySystemBarInsets(binding.root, binding.appBar, binding.viewPager)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = lotteryTitle

        binding.toolbar.setNavigationOnClickListener { finish() }

        updateDateDisplay()

        binding.btnPrevDate.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDateDisplay()
            loadData()
        }

        binding.btnNextDate.setOnClickListener {
            val today = Calendar.getInstance()
            if (calendar.before(today)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                updateDateDisplay()
                loadData()
            }
        }

        loadData()
        loadPopupAd()
    }

    private fun loadPopupAd() {
        Log.d("LotteryActivity", "loadPopupAd called")
        thread {
            try {
                Log.d("LotteryActivity", "Fetching ads...")
                val response = AdsApi.fetchAds()
                Log.d("LotteryActivity", "Got ${response.ads.size} ads, total=${response.total}")
                val popupAd = response.ads
                    .filter { it.position == "popup" }
                    .minByOrNull { it.displayOrder }
                Log.d("LotteryActivity", "Popup ad: ${popupAd?.title}, url=${popupAd?.imageUrl}")
                if (popupAd != null) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved) {
                            Log.d("LotteryActivity", "Showing popup dialog")
                            AdPopupDialog.newInstance(popupAd)
                                .show(supportFragmentManager, "ad_popup")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LotteryActivity", "Failed to load popup ad: ${e.message}", e)
            }
        }
    }

    private fun updateDateDisplay() {
        binding.tvCurrentDate.text = displayDateFormat.format(calendar.time)
        val today = Calendar.getInstance()
        val isToday = apiDateFormat.format(calendar.time) == apiDateFormat.format(today.time)
        binding.btnNextDate.alpha = if (isToday) 0.3f else 1.0f
    }

    private fun getSelectedDate(): String = apiDateFormat.format(calendar.time)

    private fun loadData() {
        val token = ++requestToken
        val code = lotteryCode
        if (code != null) {
            loadSingleLottery(code, token)
        } else {
            loadAllLotteries(token)
        }
    }

    private fun loadSingleLottery(code: String, token: Int) {
        Log.d("LotteryActivity", "loadSingleLottery: code=$code")
        binding.tabLayout.visibility = View.GONE
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults(getSelectedDate())
                val results = LotteryApi.convertToResults(apiResponse)
                val filtered = results.filter { it.lotteryCode == code }
                Log.d("LotteryActivity", "loadSingleLottery: results=${results.size}, filtered=${filtered.size}")

                runOnUiThread {
                    if (!isStale(token)) {
                        if (filtered.isNotEmpty()) {
                            binding.viewPager.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE
                            setupTabs(filtered)
                        } else {
                            binding.viewPager.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LotteryActivity", "Failed to fetch data", e)
                runOnUiThread {
                    if (!isStale(token)) {
                        binding.viewPager.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun loadAllLotteries(token: Int) {
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults(getSelectedDate())
                val results = LotteryApi.convertToResults(apiResponse)

                runOnUiThread {
                    if (!isStale(token)) {
                        if (results.isNotEmpty()) {
                            binding.viewPager.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE
                            setupTabs(results)
                        } else {
                            binding.viewPager.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LotteryActivity", "Failed to fetch data", e)
                runOnUiThread {
                    if (!isStale(token)) {
                        binding.viewPager.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    /** True if the activity is gone or a newer request has superseded [token]. */
    private fun isStale(token: Int): Boolean =
        isFinishing || isDestroyed || token != requestToken

    private fun setupTabs(results: List<LotteryResult>) {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        val adapter = LotteryPagerAdapter(this, results, getSelectedDate(), focusTimeSlot)
        focusTimeSlot = null // only on first load, not after changing date
        binding.viewPager.adapter = adapter

        if (results.size > 1) {
            binding.tabLayout.visibility = View.VISIBLE
            tabLayoutMediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = results[position].title
            }.also { it.attach() }
        } else {
            binding.tabLayout.visibility = View.GONE
        }
    }
}
