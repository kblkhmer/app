package com.lotterykh.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.databinding.ActivityLotteryBinding
import com.lotterykh.app.model.LotteryResult
import kotlin.concurrent.thread

class LotteryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLotteryBinding

    companion object {
        const val EXTRA_LOTTERY_CODE = "lottery_code"
        const val EXTRA_LOTTERY_TITLE = "lottery_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLotteryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val lotteryCode = intent.getStringExtra(EXTRA_LOTTERY_CODE)
        val lotteryTitle = intent.getStringExtra(EXTRA_LOTTERY_TITLE) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = lotteryTitle

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (lotteryCode != null) {
            loadSingleLottery(lotteryCode)
        } else {
            loadAllLotteries()
        }
    }

    private fun loadSingleLottery(code: String) {
        binding.tabLayout.visibility = android.view.View.GONE
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults()
                val results = LotteryApi.convertToResults(apiResponse)
                val filtered = results.filter { it.lotteryCode == code }

                runOnUiThread {
                    if (filtered.isNotEmpty()) {
                        setupTabs(filtered)
                    } else {
                        Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LotteryActivity", "Failed to fetch data", e)
                runOnUiThread {
                    Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadAllLotteries() {
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults()
                val results = LotteryApi.convertToResults(apiResponse)

                runOnUiThread {
                    setupTabs(results)
                }
            } catch (e: Exception) {
                Log.e("LotteryActivity", "Failed to fetch data", e)
                runOnUiThread {
                    Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTabs(results: List<LotteryResult>) {
        val adapter = LotteryPagerAdapter(this, results)
        binding.viewPager.adapter = adapter

        if (results.size > 1) {
            binding.tabLayout.visibility = android.view.View.VISIBLE
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = results[position].title
            }.attach()
        }
    }
}
