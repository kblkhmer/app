package com.lotterykh.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.databinding.FragmentLotteryBinding
import com.lotterykh.app.model.LotteryDraw
import com.lotterykh.app.model.LotteryResult
import kotlin.concurrent.thread

class LotteryFragment : Fragment() {

    private var _binding: FragmentLotteryBinding? = null
    private val binding get() = _binding!!

    private var lotteryCode: String = ""
    private var lotteryTitle: String = ""

    companion object {
        private const val ARG_CODE = "lottery_code"
        private const val ARG_TITLE = "lottery_title"

        fun newInstance(code: String, title: String): LotteryFragment {
            return LotteryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CODE, code)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lotteryCode = arguments?.getString(ARG_CODE, "") ?: ""
        lotteryTitle = arguments?.getString(ARG_TITLE, "") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLotteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadFromApi()

        binding.swipeRefresh.setOnRefreshListener {
            loadFromApi()
        }
    }

    private fun loadFromApi() {
        thread {
            try {
                val apiResponse = LotteryApi.fetchResults()
                val results = LotteryApi.convertToResults(apiResponse)
                val result = results.find { it.lotteryCode == lotteryCode }

                activity?.runOnUiThread {
                    binding.swipeRefresh.isRefreshing = false
                    if (result != null) {
                        displayResult(result)
                    }
                }
            } catch (e: Exception) {
                Log.e("LotteryFragment", "API error", e)
                activity?.runOnUiThread {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(context, "Failed to refresh", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayResult(result: LotteryResult) {
        val container = binding.contentContainer
        container.removeAllViews()

        for (draw in result.draws) {
            addDrawCard(container, result.title, draw)
        }
    }

    private fun addDrawCard(container: LinearLayout, title: String, draw: LotteryDraw) {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_draw_card, container, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = title

        val tvSession = cardView.findViewById<TextView>(R.id.tvSession)
        tvSession.text = draw.timeSlot
        tvSession.background = ContextCompat.getDrawable(requireContext(), R.drawable.session_badge_bg)

        val tvDate = cardView.findViewById<TextView>(R.id.tvDate)
        tvDate.text = "Date:  ${draw.date}"

        val prizeContainer = cardView.findViewById<LinearLayout>(R.id.prizeContainer)

        for ((index, prize) in draw.prizes.withIndex()) {
            if (prize.twoDigit.isEmpty() && prize.threeDigit.isEmpty()) continue

            val rowView = inflater.inflate(R.layout.item_prize_row, prizeContainer, false)

            rowView.findViewById<TextView>(R.id.tvPrizeLabel).text = prize.label
            rowView.findViewById<TextView>(R.id.tvTwoDigit).text = prize.twoDigit
            rowView.findViewById<TextView>(R.id.tvThreeDigit).text = prize.threeDigit

            if (index % 2 == 1) {
                rowView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.row_alt_bg))
            }

            prizeContainer.addView(rowView)
        }

        container.addView(cardView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
