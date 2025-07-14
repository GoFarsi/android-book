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
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


//import org.jsoup.Jsoup

class WebActivity : AppCompatActivity() {
    var myWebView: WebView? = null
    private val list = arrayListOf("https://book.gofarsi.ir/", "https://ir1-book.gofarsi.ir/", "https://ipfs-book.gofarsi.ir/", "https://hku1-book.gofarsi.ir/", "https://aws1-book.gofarsi.ir/")
    var urlIndex = 0
    var hasErrorInLoading = false
    var currentUrl = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

        var title = intent.extras?.getString(KEY_TITLE) ?: ""
        var link = intent.extras?.getString(KEY_LINK) ?: ""

        // Find the fastest URL before loading
        findViewById<TextView>(R.id.tvVersion).text = BuildConfig.VERSION_NAME
        myWebView = findViewById(R.id.webview)

        // --- WebView settings for PWA and offline caching ---
        val webSettings = myWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setAppCacheEnabled(true)
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

        myWebView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d("bootiyar", url)
                if (!url.contains("gofarsi.ir")) {
                    loadBrowser(url)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("bootiyar", "onPageStarted: $url")
                hasErrorInLoading = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.d("bootiyar", "onReceivedError")
                super.onReceivedError(view, request, error)
                hasErrorInLoading = true
                if (urlIndex == list.size - 1) return
                myWebView?.loadUrl(list[++urlIndex])
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentUrl = url ?: ""
                if (!hasErrorInLoading) findViewById<RelativeLayout>(R.id.loading).visibility = View.INVISIBLE
                Log.d("bootiyar", "onPageFinished: $url")
            }

        }

    }

    private fun findFastestUrlAndLoad() {
        CoroutineScope(Dispatchers.IO).launch {
            var isUrlLoaded = false
            for (url in list) {
                if (isUrlResponsive(url)) {
                    withContext(Dispatchers.Main) {
                        myWebView?.loadUrl(url)
                    }
                    isUrlLoaded = true
                    break
                }
            }
            if (!isUrlLoaded) {
                withContext(Dispatchers.Main) {
                    // Handle case where no URL is responsive
                    findViewById<RelativeLayout>(R.id.loading).visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun isUrlResponsive(url: String): Boolean {
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.apply {
                requestMethod = "HEAD"
                connectTimeout = 3000
                readTimeout = 3000
            }
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun loadBrowser(url:String){
        val uri: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
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
        fun navigate(context: Context, title: String, link: String) {
            val intent = Intent(context, WebActivity::class.java).apply {
                putExtra(KEY_TITLE, title)
                putExtra(KEY_LINK, link)
            }
            context.startActivity(intent)
        }

        fun loadWebView(context: Context, content: String, myWebView: WebView) {

            setSettingWebView(context, myWebView)
            myWebView.loadUrl(content)

        }

        private fun setSettingWebView(context: Context, myWebView: WebView) {
            myWebView?.let {
                it.settings?.builtInZoomControls = false
                it.settings?.databaseEnabled = true
                it.settings?.domStorageEnabled = true
                it.settings?.setGeolocationEnabled(true)
                it.settings?.loadWithOverviewMode = true
                it.settings?.useWideViewPort = true
                it.isScrollbarFadingEnabled = false

                it.clearCache(false)
//                it.settings?.setAppCachePath(context.cacheDir?.absolutePath)
//                it.settings?.setAppCacheEnabled(true)
                it.settings?.allowFileAccess = true
                it.settings?.javaScriptEnabled = true
                it.settings?.cacheMode = WebSettings.LOAD_DEFAULT
                it.settings?.defaultTextEncodingName = "utf-8"
                it.settings?.javaScriptCanOpenWindowsAutomatically = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(it, true)
                }
                it.settings?.pluginState = WebSettings.PluginState.ON
            }
        }
    }


    override fun onBackPressed() {
        if (myWebView?.canGoBack() == true && currentUrl != list[urlIndex]) {
            myWebView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}