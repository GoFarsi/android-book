package com.book.gofarsi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis


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

    private fun findFastestUrlAndLoad() {
        CoroutineScope(Dispatchers.IO).launch {
            val fastestUrl = findFastestUrlOptimized()

            withContext(Dispatchers.Main) {
                if (fastestUrl != null) {
                    urlIndex = list.indexOf(fastestUrl)
                    myWebView?.loadUrl(fastestUrl)
                } else {
                    // Fallback to first URL if none are responsive
                    urlIndex = 0
                    myWebView?.loadUrl(list[0])
                    findViewById<RelativeLayout>(R.id.loading).visibility = View.VISIBLE
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

        // Otherwise, test URLs concurrently with race condition
        return withContext(Dispatchers.IO) {
            val jobs = list.map { url ->
                async {
                    val responseTime = measureUrlResponseTime(url)
                    if (responseTime != null) {
                        // Cache the result
                        responseTimeCache[url] = responseTime to currentTime
                        url to responseTime
                    } else {
                        null
                    }
                }
            }

            // Wait for first successful response (race condition)
            var fastestResult: Pair<String, Long>? = null

            try {
                // Use select to get the first successful result
                for (job in jobs) {
                    val result = job.await()
                    if (result != null) {
                        if (fastestResult == null || result.second < fastestResult.second) {
                            fastestResult = result
                        }
                        // If we find a very fast response (< 500ms), use it immediately
                        if (result.second < MIN_TIMEOUT_MS) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WebActivity", "Error in concurrent URL testing: ${e.message}")
            }

            fastestResult?.first
        }
    }

    private suspend fun measureUrlResponseTime(url: String): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val responseTime = measureTimeMillis {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.apply {
                        requestMethod = "HEAD"
                        connectTimeout = MAX_TIMEOUT_MS.toInt()
                        readTimeout = MAX_TIMEOUT_MS.toInt()
                        setRequestProperty("User-Agent", "Android-Book-App")
                        // Add cache control to get fresh response
                        setRequestProperty("Cache-Control", "no-cache")
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
        private const val MAX_TIMEOUT_MS = 2000L
        private const val MIN_TIMEOUT_MS = 500L

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