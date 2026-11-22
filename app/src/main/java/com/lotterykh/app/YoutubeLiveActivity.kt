package com.lotterykh.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lotterykh.app.databinding.ActivityYoutubeLiveBinding

/**
 * Shows the channel's YouTube live streams inside the app, with the app's own header and
 * back button, so users can always get back to the results.
 */
class YoutubeLiveActivity : BaseActivity() {

    companion object {
        const val YOUTUBE_LIVE_URL =
            "https://www.youtube.com/@%E1%9E%95%E1%9F%92%E1%9E%91%E1%9F%87_%E1%9E%86%E1%9F%92%E1%9E%93%E1%9F%84%E1%9E%8F%E1%9E%99%E1%9E%BD%E1%9E%93/streams"
    }

    private lateinit var binding: ActivityYoutubeLiveBinding

    /** The full-screen video view YouTube hands us, while a video is in full screen. */
    private var fullScreenView: View? = null
    private var fullScreenCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled") // YouTube needs JavaScript to play video.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubeLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets(binding.root, binding.youtubeHeader, binding.webView)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.card_youtube_live)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme
                if (scheme == "http" || scheme == "https") return false
                // intent://, vnd.youtube: and similar links belong to other apps.
                openExternally(request.url)
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                binding.pageProgress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                binding.pageProgress.visibility = View.GONE
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.pageProgress.progress = newProgress
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                enterFullScreen(view, callback)
            }

            override fun onHideCustomView() {
                exitFullScreen()
            }
        }

        // Back: leave full screen, then go back inside YouTube, then return to the app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullScreenView != null -> exitFullScreen()
                    binding.webView.canGoBack() -> binding.webView.goBack()
                    else -> finish()
                }
            }
        })

        if (savedInstanceState == null) {
            binding.webView.loadUrl(YOUTUBE_LIVE_URL)
        } else {
            binding.webView.restoreState(savedInstanceState)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_youtube_live, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_open_youtube) {
            openExternally(Uri.parse(binding.webView.url ?: YOUTUBE_LIVE_URL))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openExternally(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Log.w("YoutubeLiveActivity", "No app can open $uri")
        }
    }

    private fun enterFullScreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullScreenView != null) {
            callback.onCustomViewHidden()
            return
        }
        fullScreenView = view
        fullScreenCallback = callback
        (window.decorView as ViewGroup).addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitFullScreen() {
        val view = fullScreenView ?: return
        (window.decorView as ViewGroup).removeView(view)
        fullScreenView = null
        fullScreenCallback?.onCustomViewHidden()
        fullScreenCallback = null
        WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onPause() {
        binding.webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
