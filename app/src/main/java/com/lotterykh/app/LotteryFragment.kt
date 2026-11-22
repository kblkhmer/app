package com.lotterykh.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.load
import coil.size.Dimension
import coil.size.Size
import com.lotterykh.app.data.AdsApi
import com.lotterykh.app.data.LotteryApi
import com.lotterykh.app.databinding.FragmentLotteryBinding
import com.lotterykh.app.model.Ad
import com.lotterykh.app.model.LotteryDraw
import com.lotterykh.app.model.LotteryResult
import kotlin.concurrent.thread

class LotteryFragment : Fragment() {

    private var _binding: FragmentLotteryBinding? = null
    private val binding get() = _binding!!

    private var lotteryCode: String = ""
    private var lotteryTitle: String = ""
    private var date: String? = null
    /** Draw to scroll to once results load (e.g. opened from a "new result" notification). */
    private var focusTimeSlot: String? = null
    /** "Live Slots" (banner2) ads, fetched once and reused on swipe-refresh. */
    @Volatile private var slotAds: List<Ad>? = null

    companion object {
        private const val ARG_CODE = "lottery_code"
        private const val ARG_TITLE = "lottery_title"
        private const val ARG_DATE = "lottery_date"
        private const val ARG_FOCUS_SLOT = "focus_time_slot"

        fun newInstance(code: String, title: String, date: String? = null, focusTimeSlot: String? = null): LotteryFragment {
            return LotteryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CODE, code)
                    putString(ARG_TITLE, title)
                    if (date != null) putString(ARG_DATE, date)
                    if (focusTimeSlot != null) putString(ARG_FOCUS_SLOT, focusTimeSlot)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lotteryCode = arguments?.getString(ARG_CODE, "") ?: ""
        lotteryTitle = arguments?.getString(ARG_TITLE, "") ?: ""
        date = arguments?.getString(ARG_DATE)
        focusTimeSlot = arguments?.getString(ARG_FOCUS_SLOT)
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

    /** Ads for the "Live Slots" position, in the server's display order. Never throws. */
    private fun fetchSlotAds(): List<Ad> {
        slotAds?.let { return it }
        return try {
            AdsApi.fetchAds().ads
                .filter { it.position == "banner2" && !isVideo(it.imageUrl) }
                .sortedWith(compareBy({ it.displayOrder }, { it.id }))
                .also { slotAds = it }
        } catch (e: Exception) {
            Log.w("LotteryFragment", "Failed to load Live Slots ads: ${e.message}")
            emptyList()
        }
    }

    // Slot ads are shown with Coil, which can't play video; the popup handles video ads.
    private fun isVideo(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".3gp")
    }

    private fun loadFromApi() {
        thread {
            try {
                val ads = fetchSlotAds()
                val apiResponse = LotteryApi.fetchResults(date)
                val results = LotteryApi.convertToResults(apiResponse)
                val result = results.find { it.lotteryCode == lotteryCode }

                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        binding.swipeRefresh.isRefreshing = false
                        if (result != null) {
                            displayResult(result, ads)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LotteryFragment", "API error", e)
                activity?.runOnUiThread {
                    if (isAdded && _binding != null) {
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Failed to refresh", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun displayResult(result: LotteryResult, ads: List<Ad>) {
        val container = binding.contentContainer
        container.removeAllViews()

        var focusCard: View? = null
        for ((index, draw) in result.draws.withIndex()) {
            val card = addDrawCard(container, result.title, draw)
            if (draw.timeSlot == focusTimeSlot) focusCard = card
            // Same as the website's live page: an ad after every draw card, cycling through the ads.
            if (ads.isNotEmpty()) addSlotAd(container, ads[index % ads.size])
        }

        // Only jump once; later swipe-refreshes keep the user's scroll position.
        focusCard?.let { card ->
            focusTimeSlot = null
            binding.scrollView.post {
                if (_binding != null) binding.scrollView.smoothScrollTo(0, container.top + card.top)
            }
        }
    }

    private fun addDrawCard(container: LinearLayout, title: String, draw: LotteryDraw): View {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_draw_card, container, false)

        val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = title

        val tvSession = cardView.findViewById<TextView>(R.id.tvSession)
        tvSession.text = draw.timeSlot
        tvSession.background = ContextCompat.getDrawable(requireContext(), R.drawable.session_badge_bg)

        val tvDate = cardView.findViewById<TextView>(R.id.tvDate)
        val parts = draw.date.split("-")
        val formattedDate = if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else draw.date
        tvDate.text = "Date:  $formattedDate"

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
        return cardView
    }

    private fun addSlotAd(container: LinearLayout, ad: Ad) {
        val adView = LayoutInflater.from(requireContext()).inflate(R.layout.item_slot_ad, container, false)
        val image = adView.findViewById<ImageView>(R.id.slotAdImage)
        // Coil downloads, downsamples and caches the image off the main thread for us.
        // The card stays hidden until the image is ready, so a failed ad leaves no gap.
        // A hidden view is never measured, so give Coil the target width instead of waiting for layout.
        image.load(ad.imageUrl, ImageLoaders.get(requireContext())) {
            size(Size(Dimension(resources.displayMetrics.widthPixels), Dimension.Undefined))
            listener(
                onSuccess = { _, result ->
                    (result.drawable as? Animatable)?.start()
                    adView.visibility = View.VISIBLE
                },
                onError = { _, result ->
                    Log.w("LotteryFragment", "Failed to load Live Slots ad ${ad.id}: ${result.throwable.message}")
                }
            )
        }
        image.setOnClickListener {
            if (ad.linkUrl.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ad.linkUrl)))
            }
        }
        adView.findViewById<View>(R.id.slotAdClose).setOnClickListener {
            container.removeView(adView)
        }
        container.addView(adView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
