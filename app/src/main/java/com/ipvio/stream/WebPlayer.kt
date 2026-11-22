package com.ipvio.stream

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AColor
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    /** Called (on a WebView thread) when a better stream candidate has been sniffed. */
    var onStream: ((WebExtract.Found) -> Unit)? = null

    @Volatile
    private var bestUrl: String? = null

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

            // Extraction: watch the page's own requests for the stream it mints
            // (.m3u8 / .mpd) and keep the best candidate with the headers it was
            // requested with, so the title can switch to the native player.
            // The request itself is never intercepted — the web player keeps playing.
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val u = request.url?.toString().orEmpty()
                if (WebExtract.isStream(u)) {
                    val next = WebExtract.better(bestUrl, u)
                    if (next != bestUrl) {
                        bestUrl = next
                        val h = LinkedHashMap<String, String>()
                        request.requestHeaders?.let { h.putAll(it) }
                        h["User-Agent"] = view.settings.userAgentString
                        if (h["Referer"].isNullOrBlank()) view.url?.let { h["Referer"] = it }
                        runCatching { CookieManager.getInstance().getCookie(u) }
                            .getOrNull()?.takeIf { it.isNotBlank() }?.let { h["Cookie"] = it }
                        onStream?.invoke(WebExtract.Found(next, h))
                    }
                }
                return null
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
fun WebPlayerScreen(url: String, title: String, onNative: (WebExtract.Found) -> Unit, onClose: () -> Unit) {
    var found by remember { mutableStateOf<WebExtract.Found?>(null) }
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
                factory = { ctx ->
                    val main = Handler(Looper.getMainLooper())
                    WebPlayerView(ctx).also { v ->
                        v.onStream = { f -> main.post { found = f } }
                        v.load(url)
                    }
                },
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                raw = true, text = title, color = Color(0x99FFFFFF), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 12.dp)
            )
            // Extraction found a stream: offer the switch to the native player.
            // The pill takes the focus when it appears; OK plays natively, Back closes.
            found?.let { f ->
                val fr = remember { FocusRequester() }
                var pillFocus by remember { mutableStateOf(false) }
                LaunchedEffect(f.url) { runCatching { fr.requestFocus() } }
                val pill = RoundedCornerShape(999.dp)
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
                        .focusRequester(fr)
                        .background(Tok.GlassStrong, pill)
                        .border(if (pillFocus) 2.dp else 1.dp, if (pillFocus) Accent2 else Color(0x55FFFFFF), pill)
                        .onFocusChanged { pillFocus = it.hasFocus }
                        .clickable { WebExtract.put(f); onNative(f) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nativ abspielen", color = Accent2, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    val host = runCatching { java.net.URI(f.url).host } .getOrNull()
                    if (!host.isNullOrBlank()) {
                        Text(raw = true, text = " · $host", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
