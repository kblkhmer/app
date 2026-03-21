package com.lotterykh.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lotterykh.app.data.SampleData
import com.lotterykh.app.databinding.FragmentLotteryBinding
import com.lotterykh.app.model.LotteryDraw
import com.lotterykh.app.model.LotteryResult
import com.lotterykh.app.model.Session

class LotteryFragment : Fragment() {

    private var _binding: FragmentLotteryBinding? = null
    private val binding get() = _binding!!

    private var resultIndex: Int = 0

    companion object {
        private const val ARG_INDEX = "result_index"

        fun newInstance(index: Int): LotteryFragment {
            return LotteryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INDEX, index)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultIndex = arguments?.getInt(ARG_INDEX, 0) ?: 0
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

        val result = SampleData.getAllResults().getOrNull(resultIndex) ?: return
        displayResult(result)

        binding.swipeRefresh.setOnRefreshListener {
            displayResult(result)
            binding.swipeRefresh.isRefreshing = false
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

        // Title
        val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = title

        // Session badge
        val tvSession = cardView.findViewById<TextView>(R.id.tvSession)
        tvSession.text = draw.session.khmer

        val sessionBg = if (draw.session == Session.MORNING) {
            ContextCompat.getDrawable(requireContext(), R.drawable.session_morning_bg)
        } else {
            ContextCompat.getDrawable(requireContext(), R.drawable.session_evening_bg)
        }
        tvSession.background = sessionBg

        // Date
        val tvDate = cardView.findViewById<TextView>(R.id.tvDate)
        tvDate.text = "Date:  ${draw.date}"

        // Prize rows
        val prizeContainer = cardView.findViewById<LinearLayout>(R.id.prizeContainer)

        // Add special numbers first for evening (if present: ??? If A + ??? If A)
        if (draw.specialNumbers != null) {
            val specialView = inflater.inflate(R.layout.item_special_numbers, prizeContainer, false)

            val prizeAValues = draw.specialNumbers.prizeAValues
            if (prizeAValues.size >= 3) {
                specialView.findViewById<TextView>(R.id.tvSpecial1).text = prizeAValues[0]
                specialView.findViewById<TextView>(R.id.tvSpecial2).text = prizeAValues[1]
                specialView.findViewById<TextView>(R.id.tvSpecial3).text = prizeAValues[2]
            }

            val twoDigits = draw.specialNumbers.twoDigitValues
            if (twoDigits.size >= 4) {
                specialView.findViewById<TextView>(R.id.tvDigit1).text = twoDigits[0]
                specialView.findViewById<TextView>(R.id.tvDigit2).text = twoDigits[1]
                specialView.findViewById<TextView>(R.id.tvDigit3).text = twoDigits[2]
                specialView.findViewById<TextView>(R.id.tvDigit4).text = twoDigits[3]
            }

            prizeContainer.addView(specialView)
        }

        // Regular prize rows
        for ((index, prize) in draw.prizes.withIndex()) {
            if (prize.twoDigit.isEmpty() && prize.threeDigit.isEmpty()) continue

            val rowView = inflater.inflate(R.layout.item_prize_row, prizeContainer, false)

            rowView.findViewById<TextView>(R.id.tvPrizeLabel).text = prize.label
            rowView.findViewById<TextView>(R.id.tvTwoDigit).text = prize.twoDigit
            rowView.findViewById<TextView>(R.id.tvThreeDigit).text = prize.threeDigit

            // Alternate row background
            if (index % 2 == 1) {
                rowView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.row_alt_bg))
            }

            prizeContainer.addView(rowView)
        }

        // Message banners
        if (draw.session == Session.MORNING) {
            addMessageBanner(prizeContainer, "??????????? ???????????????!!!", R.color.green_accent, Color.WHITE)
            addMessageBanner(prizeContainer, "??????????????????????????????????!", R.color.red_primary, Color.WHITE)
        } else {
            addMessageBanner(prizeContainer, "????????! ?????????????????", R.color.blue_banner, Color.WHITE)
            addMessageBanner(prizeContainer, "???! ????????????????!", R.color.red_primary, Color.WHITE)
        }

        container.addView(cardView)
    }

    private fun addMessageBanner(container: LinearLayout, text: String, bgColorRes: Int, textColor: Int) {
        val inflater = LayoutInflater.from(requireContext())
        val bannerView = inflater.inflate(R.layout.item_message_banner, container, false)
        val tvMessage = bannerView.findViewById<TextView>(R.id.tvMessage)
        tvMessage.text = text
        tvMessage.setTextColor(textColor)
        tvMessage.setBackgroundColor(ContextCompat.getColor(requireContext(), bgColorRes))
        container.addView(bannerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
