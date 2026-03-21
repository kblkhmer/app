package com.lotterykh.app

import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.lotterykh.app.model.Ad
import java.net.URL
import kotlin.concurrent.thread

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
        val btnClose = view.findViewById<android.widget.ImageButton>(R.id.btnClose)

        // Load image in background
        thread {
            try {
                val url = URL(ad!!.imageUrl)
                val conn = url.openConnection()
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                adImage.post {
                    if (isAdded) {
                        adImage.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {
                adImage.post {
                    if (isAdded) dismissAllowingStateLoss()
                }
            }
        }

        // Click image to open link
        adImage.setOnClickListener {
            val linkUrl = ad?.linkUrl
            if (!linkUrl.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                startActivity(intent)
            }
            dismissAllowingStateLoss()
        }

        // Close button
        btnClose.setOnClickListener {
            dismissAllowingStateLoss()
        }

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
