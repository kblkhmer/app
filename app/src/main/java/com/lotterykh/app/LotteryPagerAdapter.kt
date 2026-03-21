package com.lotterykh.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lotterykh.app.model.LotteryResult

class LotteryPagerAdapter(
    activity: FragmentActivity,
    private val results: List<LotteryResult>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = results.size

    override fun createFragment(position: Int): Fragment {
        val result = results[position]
        return LotteryFragment.newInstance(result.lotteryCode, result.title)
    }
}
