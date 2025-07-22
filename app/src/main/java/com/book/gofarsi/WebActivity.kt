package com.book.gofarsi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis
import java.util.concurrent.atomic.AtomicBoolean


//import org.jsoup.Jsoup

class WebActivity : AppCompatActivity() {
    var myWebView: WebView? = null
    private val list = arrayListOf(
        "https://book.gofarsi.ir/",
        "https://cloud-book.gofarsi.ir/",
        "https://ir1-book.gofarsi.ir/",
        "https://ipfs-book.gofarsi.ir/",
        "https://hku1-book.gofarsi.ir/",
        "https://aws1-book.gofarsi.ir/"
    )
    var urlIndex = 0
    var hasErrorInLoading = false
    var currentUrl = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Configure status bar and navigation bar
            configureStatusBar()

            setContentView(R.layout.activity_web)

            // Find the fastest URL before loading
            findViewById<TextView>(R.id.tvVersion).text = BuildConfig.VERSION_NAME
            myWebView = findViewById(R.id.webview)

            // --- WebView settings for PWA and offline caching ---
            val webSettings = myWebView!!.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.allowFileAccess = true
            webSettings.allowContentAccess = true
            // Note: allowFileAccessFromFileURLs and allowUniversalAccessFromFileURLs are deprecated
            // and removed for security reasons. Modern apps should use alternative approaches.
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Prevent WebView from going fullscreen
            webSettings.loadWithOverviewMode = true
            webSettings.useWideViewPort = true
            webSettings.setSupportZoom(true)  // ✅ ENABLE zoom functionality
            webSettings.builtInZoomControls = true  // ✅ ENABLE zoom controls
            webSettings.displayZoomControls = false // ❌ HIDE zoom buttons (users can still pinch-to-zoom)

            webSettings.cacheMode = if (isNetworkAvailable()) {
                WebSettings.LOAD_DEFAULT
            } else {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                ServiceWorkerController.getInstance().setServiceWorkerClient(object : ServiceWorkerClient() {
                    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                        return null
                    }
                })
            }
            // --- End WebView settings ---

            // Launch coroutine to find fastest URL
            findFastestUrlAndLoad()
        } catch (e: Exception) {
            Log.e("WebActivity", "Error in onCreate: ${e.message}", e)
            finish()
        }

        myWebView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d("bootlegger", url)
                if (!url.contains("gofarsi.ir")) {
                    loadBrowser(url)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("bootlegger", "onPageStarted: $url")
                hasErrorInLoading = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.d("bootlegger", "onReceivedError")
                super.onReceivedError(view, request, error)
                hasErrorInLoading = true
                if (urlIndex == list.size - 1) return
                myWebView?.loadUrl(list[++urlIndex])
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentUrl = url ?: ""
                if (!hasErrorInLoading) findViewById<RelativeLayout>(R.id.loading).visibility = View.INVISIBLE
                Log.d("bootlegger", "onPageFinished: $url")
            }

        }

        // Replace deprecated onBackPressed with OnBackPressedDispatcher
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (myWebView?.canGoBack() == true && currentUrl != list[urlIndex]) {
                    myWebView?.goBack()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun configureStatusBar() {
        // Make sure status bar is visible and properly configured
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set status bar color
            window.statusBarColor = Color.parseColor("#2196F3") // Blue color, change as needed

            // For Android 6.0+ (API 23), we can control light/dark status bar content
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use WindowInsetsController for better control (API 30+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(false)
                    val controller = window.insetsController
                    controller?.show(WindowInsetsCompat.Type.statusBars())
                    // Use light content (white icons) on dark status bar
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        controller?.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                    }
                } else {
                    // For older versions, use deprecated flags
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
            }
        }

        // Ensure the activity doesn't go full screen
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Show the status bar if it was hidden
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsetsCompat.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
        }
    }

    private fun findFastestUrlAndLoad() {
        // Start loading the first URL immediately while testing others in background
        CoroutineScope(Dispatchers.Main).launch {
            // Load first URL immediately to show something to user
            myWebView?.loadUrl(list[0])
            urlIndex = 0
        }

        // Test URLs in background and switch if we find a faster one
        CoroutineScope(Dispatchers.IO).launch {
            val fastestUrl = findFastestUrlOptimized()

            withContext(Dispatchers.Main) {
                if (fastestUrl != null && fastestUrl != list[0]) {
                    // Only switch if we found a different, faster URL
                    val newIndex = list.indexOf(fastestUrl)
                    if (newIndex != urlIndex) {
                        urlIndex = newIndex
                        myWebView?.loadUrl(fastestUrl)
                        Log.d("WebActivity", "Switched to faster URL: $fastestUrl")
                    }
                }
            }
        }
    }

    private suspend fun findFastestUrlOptimized(): String? {
        // First, check cache for recently tested URLs
        val currentTime = System.currentTimeMillis()
        val cachedResults = mutableListOf<Pair<String, Long>>()

        // Get cached results that are still valid
        for (url in list) {
            responseTimeCache[url]?.let { (responseTime, timestamp) ->
                if (currentTime - timestamp < CACHE_VALIDITY_MS) {
                    cachedResults.add(url to responseTime)
                }
            }
        }

        // If we have enough cached results, use the fastest one
        if (cachedResults.size >= list.size / 2) {
            return cachedResults.minByOrNull { it.second }?.first
        }

        // Use a race condition approach - first successful response wins
        return withContext(Dispatchers.IO) {
            val firstSuccessful = AtomicBoolean(false)
            var fastestUrl: String? = null
            var fastestTime = Long.MAX_VALUE

            // Launch all requests concurrently
            val jobs = list.mapIndexed { index, url ->
                async {
                    try {
                        // Use shorter timeout for first quick scan
                        val timeout = if (firstSuccessful.get()) MAX_TIMEOUT_MS else FAST_LOAD_TIMEOUT_MS
                        val responseTime = measureUrlResponseTime(url, timeout)

                        if (responseTime != null && responseTime < fastestTime) {
                            synchronized(this@WebActivity) {
                                if (responseTime < fastestTime) {
                                    fastestTime = responseTime
                                    fastestUrl = url

                                    // Cache the result
                                    responseTimeCache[url] = responseTime to currentTime

                                    // If we find a very fast response, we can use it immediately
                                    if (responseTime < MIN_TIMEOUT_MS) {
                                        firstSuccessful.set(true)
                                    }
                                }
                            }
                        }
                        responseTime
                    } catch (e: Exception) {
                        Log.d("WebActivity", "URL $url failed: ${e.message}")
                        null
                    }
                }
            }

            // Wait for either the first very fast response or all jobs to complete
            try {
                // Wait a short time for a quick response
                withTimeout(FAST_LOAD_TIMEOUT_MS + 200) {
                    for (job in jobs) {
                        if (firstSuccessful.get()) break
                        try {
                            job.await()
                        } catch (e: Exception) {
                            // Continue with other jobs
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                // If we timeout, use whatever we have so far
                Log.d("WebActivity", "Quick scan timeout, using best result so far")
            }

            fastestUrl
        }
    }

    private suspend fun measureUrlResponseTime(url: String, timeoutMs: Long = MAX_TIMEOUT_MS): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val responseTime = measureTimeMillis {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.apply {
                        requestMethod = "HEAD"
                        connectTimeout = timeoutMs.toInt()
                        readTimeout = timeoutMs.toInt()
                        setRequestProperty("User-Agent", "Android-Book-App")
                        // Add cache control to get fresh response
                        setRequestProperty("Cache-Control", "no-cache")
                        // Add connection keep-alive for faster subsequent requests
                        setRequestProperty("Connection", "keep-alive")
                    }

                    val responseCode = connection.responseCode
                    connection.disconnect()

                    if (responseCode !in 200..299) {
                        throw Exception("HTTP $responseCode")
                    }
                }
                responseTime
            } catch (e: Exception) {
                Log.d("WebActivity", "URL $url failed: ${e.message}")
                null
            }
        }
    }

    fun loadBrowser(url:String){
        val uri: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    // Method to clear response time cache (useful for testing or when network conditions change)
    private fun clearResponseTimeCache() {
        responseTimeCache.clear()
    }


    /* fun getNewContent(htmltext: String): String {

         val doc = Jsoup.parse(htmltext)
         val elements = doc.getElementsByTag("img")
         for (element in elements) {
             element.attr("width", "100%").attr("height", "auto")
         }

         return doc.toString()
     }*/


    companion object {
        val KEY_TITLE = "title"
        val KEY_LINK = "link"

        // Cache for storing response times and last check time
        private val responseTimeCache = ConcurrentHashMap<String, Pair<Long, Long>>() // URL -> (responseTime, timestamp)
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        private const val MAX_TIMEOUT_MS = 1500L // Reduced from 2000ms for faster first load
        private const val MIN_TIMEOUT_MS = 400L // Reduced for quicker selection
        private const val FAST_LOAD_TIMEOUT_MS = 800L // Very quick timeout for first attempt

        fun navigate(context: Context, title: String, link: String) {
            val intent = Intent(context, WebActivity::class.java).apply {
                putExtra(KEY_TITLE, title)
                putExtra(KEY_LINK, link)
            }
            context.startActivity(intent)
        }

        fun loadWebView(content: String, myWebView: WebView) {
            setSettingWebView(myWebView)
            myWebView.loadUrl(content)
        }

        private fun setSettingWebView(myWebView: WebView) {
            val settings = myWebView.settings
            settings.builtInZoomControls = false
            settings.domStorageEnabled = true
            settings.setGeolocationEnabled(true)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            myWebView.isScrollbarFadingEnabled = false
            myWebView.clearCache(false)
            settings.allowFileAccess = true
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.defaultTextEncodingName = "utf-8"
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(myWebView, true)
            }
        }
    }


    // Checks if network is available
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val network = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                connectivityManager.activeNetwork ?: return false
            } else {
                // For older devices, fallback to deprecated method
                @Suppress("DEPRECATION")
                return connectivityManager.activeNetworkInfo?.isConnected == true
            }
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }

    override fun onDestroy() {
        try {
            myWebView?.clearHistory()
            myWebView?.clearCache(true)
            myWebView?.loadUrl("about:blank")
            myWebView?.onPause()
            myWebView?.removeAllViews()
            // Note: destroyDrawingCache() is deprecated and no longer needed
            myWebView?.destroy()
            myWebView = null
        } catch (e: Exception) {
            Log.e("WebActivity", "Error in onDestroy: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        try {
            myWebView?.onPause()
        } catch (e: Exception) {
            Log.e("WebActivity", "Error in onPause: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            myWebView?.onResume()
        } catch (e: Exception) {
            Log.e("WebActivity", "Error in onResume: ${e.message}")
        }
    }
}