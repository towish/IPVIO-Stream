package com.ipvio.stream

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
/** The remote-controlled pointer drawn over the web player. */
private class CursorView(ctx: Context) : View(ctx) {
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 7f; color = AColor.argb(150, 0, 0, 0)
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = AColor.WHITE
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = AColor.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(cx, cy) - 5f
        canvas.drawCircle(cx, cy, r, shade)
        canvas.drawCircle(cx, cy, r, ring)
        canvas.drawCircle(cx, cy, 3.5f, dot)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private class WebPlayerView(ctx: Context) : FrameLayout(ctx) {
    private val web = WebView(ctx)
    private var customView: View? = null

    /** Called (on a WebView thread) when a better stream candidate has been sniffed. */
    var onStream: ((WebExtract.Found) -> Unit)? = null

    /** Called (main thread) when the user taps the "play natively" pill with the cursor. */
    var onNative: ((WebExtract.Found) -> Unit)? = null

    @Volatile
    private var bestUrl: String? = null

    // Remote cursor: the source pages' players are built for a mouse, so the
    // D-pad drives a pointer and OK taps where it points (language, quality,
    // fullscreen buttons included). The cursor fades out after a few seconds.
    private val density = ctx.resources.displayMetrics.density
    private val cursorSize = (46 * density).toInt()
    private val cursor = CursorView(ctx)
    private var curX = -1f
    private var curY = -1f
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideCursor = Runnable { cursor.visibility = View.GONE }

    // "Play in the IPVIO player" offer, shown once extraction found a stream.
    // It lives in this view (not in Compose) so the cursor can tap it like any
    // button of the page itself.
    private val pill = TextView(ctx)
    private var pendingNative: WebExtract.Found? = null

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
                if (pill.visibility == View.VISIBLE) pill.bringToFront()
                cursor.bringToFront()
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
        // Native-play pill (hidden until extraction finds a stream)
        val pillBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(AColor.argb(235, 16, 20, 30))
            setStroke((1.5f * density).toInt(), AColor.rgb(0x25, 0xE6, 0x7A))
        }
        pill.background = pillBg
        pill.setTextColor(AColor.WHITE)
        pill.textSize = 15f
        pill.gravity = Gravity.CENTER
        pill.setPadding((20 * density).toInt(), (10 * density).toInt(), (20 * density).toInt(), (10 * density).toInt())
        pill.visibility = View.GONE
        pill.setOnClickListener {
            pendingNative?.let { f -> WebExtract.put(f); onNative?.invoke(f) }
        }
        val pillLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (40 * density).toInt()
        }
        addView(pill, pillLp)
        // Cursor on top of everything, but it never takes a touch itself
        cursor.isClickable = false
        cursor.isFocusable = false
        cursor.visibility = View.GONE
        addView(cursor, LayoutParams(cursorSize, cursorSize))
    }

    /** Extraction found a stream: show the pill that switches to the native player. */
    fun offerNative(f: WebExtract.Found) {
        pendingNative = f
        val host = runCatching { java.net.URI(f.url).host }.getOrNull()
        pill.text = Lang.tr("Nativ abspielen") + if (!host.isNullOrBlank()) " · $host" else ""
        pill.visibility = View.VISIBLE
        pill.bringToFront()
        cursor.bringToFront()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { moveCursor(-1f, 0f, event.repeatCount); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { moveCursor(1f, 0f, event.repeatCount); return true }
                KeyEvent.KEYCODE_DPAD_UP -> { moveCursor(0f, -1f, event.repeatCount); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { moveCursor(0f, 1f, event.repeatCount); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    if (curX < 0f && width > 0 && height > 0) { curX = width / 2f; curY = height / 2f; placeCursor() }
                    if (curX >= 0f) tapAt(curX, curY)
                    pokeCursor()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun moveCursor(dx: Float, dy: Float, repeat: Int) {
        if (width == 0 || height == 0) return
        if (curX < 0f) { curX = width / 2f; curY = height / 2f }
        val step = (15f + repeat * 3f).coerceAtMost(60f) * density
        curX = (curX + dx * step).coerceIn(0f, width.toFloat())
        curY = (curY + dy * step).coerceIn(0f, height.toFloat())
        placeCursor()
        pokeCursor()
    }

    private fun placeCursor() {
        cursor.x = curX - cursorSize / 2f
        cursor.y = curY - cursorSize / 2f
    }

    private fun pokeCursor() {
        cursor.visibility = View.VISIBLE
        cursor.bringToFront()
        mainHandler.removeCallbacks(hideCursor)
        mainHandler.postDelayed(hideCursor, 3500)
    }

    private fun tapAt(x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(t, t + 70, MotionEvent.ACTION_UP, x, y, 0)
        dispatchTouchEvent(down)
        dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
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
    var hint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(9000); hint = false }
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
                        v.onStream = { f -> main.post { v.offerNative(f) } }
                        v.onNative = { f -> onNative(f) }
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
            if (hint) {
                Text(
                    "Pfeile: Cursor · OK: Klick", color = Color(0x99FFFFFF), fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp)
                )
            }
        }
    }
}
