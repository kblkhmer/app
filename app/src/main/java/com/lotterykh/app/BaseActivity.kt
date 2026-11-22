package com.lotterykh.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

open class BaseActivity : AppCompatActivity() {

    /** True when the header behind the status bar is dark enough to need white icons. */
    protected open val darkSystemBarBackground = true

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val statusStyle = if (darkSystemBarBackground) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, LEGACY_BAR_SCRIM)
        }
        // Every screen has a light content area behind the navigation bar.
        val navStyle = SystemBarStyle.light(Color.TRANSPARENT, LEGACY_BAR_SCRIM)
        enableEdgeToEdge(statusBarStyle = statusStyle, navigationBarStyle = navStyle)
        super.onCreate(savedInstanceState)
    }

    /**
     * Lets [top] (the header) run under the status bar and keeps [bottom] (the scrolling content)
     * clear of the navigation bar, by adding the system bar sizes to their padding.
     */
    protected fun applySystemBarInsets(root: View, top: View, bottom: View) {
        val topStart = top.paddingTop
        val bottomStart = bottom.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            top.updatePadding(top = topStart + bars.top)
            bottom.updatePadding(bottom = bottomStart + bars.bottom)
            root.updatePadding(left = bars.left, right = bars.right)
            insets
        }
    }

    private companion object {
        // Keeps white icons readable on old Android versions that can't draw dark bar icons.
        val LEGACY_BAR_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
