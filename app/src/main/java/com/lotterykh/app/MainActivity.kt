package com.lotterykh.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.databinding.ActivityMainBinding
import com.lotterykh.app.model.LotteryResult
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadData()

        PlayIntegrityHelper(this).requestIntegrityToken { token, error ->
            if (token != null) {
                Log.d("MainActivity", "Play Integrity token received")
            } else {
                Log.w("MainActivity", "Play Integrity failed: $error")
            }
        }
    }

    private fun loadData() {
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults()
                val results = LotteryApi.convertToResults(apiResponse)

                runOnUiThread {
                    setupTabs(results)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch data", e)
                runOnUiThread {
                    Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTabs(results: List<LotteryResult>) {
        val adapter = LotteryPagerAdapter(this, results)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = results[position].title
        }.attach()
    }
}
