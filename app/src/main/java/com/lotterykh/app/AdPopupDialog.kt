package com.lotterykh.app

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.VideoView
import androidx.fragment.app.DialogFragment
import coil.load
import com.lotterykh.app.model.Ad

class AdPopupDialog : DialogFragment() {

    private var ad: Ad? = null
    private var timer: CountDownTimer? = null

    companion object {
        fun newInstance(ad: Ad): AdPopupDialog {
            val dialog = AdPopupDialog()
            val args = Bundle().apply {
                putInt("id", ad.id)
                putString("title", ad.title)
                putString("imageUrl", ad.imageUrl)
                putString("linkUrl", ad.linkUrl)
                putInt("duration", ad.duration)
            }
            dialog.arguments = args
            return dialog
        }

        private fun getMediaType(url: String): String {
            val lower = url.lowercase()
            return when {
                lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".3gp") -> "video"
                lower.endsWith(".gif") -> "gif"
                else -> "image"
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        ad = Ad(
            id = args.getInt("id"),
            title = args.getString("title", ""),
            imageUrl = args.getString("imageUrl", ""),
            linkUrl = args.getString("linkUrl", ""),
            position = "popup",
            duration = args.getInt("duration", 10),
            displayOrder = 0
        )

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_ad_popup, null)
        dialog.setContentView(view)

        val adImage = view.findViewById<android.widget.ImageView>(R.id.adImage)
        val adVideo = view.findViewById<VideoView>(R.id.adVideo)
        val btnClose = view.findViewById<android.widget.ImageButton>(R.id.btnClose)

        val mediaType = getMediaType(ad!!.imageUrl)

        when (mediaType) {
            "video" -> loadVideo(adVideo, adImage)
            else -> loadImage(adImage) // Coil decodes both static images and GIFs
        }

        // Click to open link
        val clickListener = View.OnClickListener {
            val linkUrl = ad?.linkUrl
            if (!linkUrl.isNullOrEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)))
            }
            dismissAllowingStateLoss()
        }
        adImage.setOnClickListener(clickListener)
        adVideo.setOnClickListener(clickListener)

        btnClose.setOnClickListener { dismissAllowingStateLoss() }

        // Auto-dismiss after duration
        val duration = (ad?.duration ?: 10) * 1000L
        timer = object : CountDownTimer(duration, duration) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (isAdded) dismissAllowingStateLoss()
            }
        }.start()

        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    private fun loadImage(adImage: android.widget.ImageView) {
        // Coil downloads, downsamples, decodes (including GIFs) and caches off the main thread.
        adImage.load(ad!!.imageUrl, ImageLoaders.get(requireContext())) {
            listener(
                onSuccess = { _, result -> (result.drawable as? Animatable)?.start() },
                onError = { _, result ->
                    Log.w("AdPopupDialog", "Failed to load ad image: ${result.throwable.message}")
                    if (isAdded) dismissAllowingStateLoss()
                }
            )
        }
    }

    private fun loadVideo(adVideo: VideoView, adImage: android.widget.ImageView) {
        adImage.visibility = View.GONE
        adVideo.visibility = View.VISIBLE
        adVideo.setVideoURI(Uri.parse(ad!!.imageUrl))
        adVideo.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.start()
        }
        adVideo.setOnErrorListener { _, _, _ ->
            if (isAdded) dismissAllowingStateLoss()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        timer?.cancel()
        super.onDestroyView()
    }
}
