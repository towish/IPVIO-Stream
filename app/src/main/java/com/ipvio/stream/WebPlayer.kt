package com.ipvio.stream

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AColor
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen web player for the web sources (see WebSources): the source's own embed page
 * plays the video, the same way the Streambert desktop app plays it in its Chromium window.
 * These hosters live on pop-ups and redirects, so the window is locked down: no multiple
 * windows, no window.open, and only http(s) navigations in the main frame — intent://,
 * market:// and ad redirects are dropped. Back leaves the player (dialog dismiss).
 */
@SuppressLint("SetJavaScriptEnabled")
private class WebPlayerView(ctx: Context) : FrameLayout(ctx) {
    private val web = WebView(ctx)
    private var customView: View? = null

    init {
        setBackgroundColor(AColor.BLACK)
        web.setBackgroundColor(AColor.BLACK)
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url?.toString().orEmpty()
                // Only web pages may load; app deep links and store redirects are dropped
                return !(u.startsWith("https://") || u.startsWith("http://"))
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            // The hosters' players go full screen through the HTML5 fullscreen API
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view
                web.visibility = View.GONE
                addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                view.requestFocus()
            }

            override fun onHideCustomView() {
                customView?.let { removeView(it) }
                customView = null
                web.visibility = View.VISIBLE
                web.requestFocus()
            }
        }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        web.isFocusable = true
        web.isFocusableInTouchMode = true
    }

    fun load(url: String) {
        web.loadUrl(url)
        web.post { web.requestFocus() }
    }

    fun destroy() {
        try {
            web.stopLoading()
            web.destroy()
        } catch (_: Exception) {
        }
    }
}

@Composable
fun WebPlayerScreen(url: String, title: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx -> WebPlayerView(ctx).also { it.load(url) } },
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                raw = true, text = title, color = Color(0x99FFFFFF), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 12.dp)
            )
        }
    }
}
