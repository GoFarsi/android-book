package com.book.gofarsi

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


//import org.jsoup.Jsoup

class WebActivity : AppCompatActivity() {
    var myWebView :WebView? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)

//        webSettings()

        var title = intent.extras?.getString(KEY_TITLE) ?: ""
        var link = intent.extras?.getString(KEY_LINK) ?: ""

        link = "https://book.gofarsi.ir/"

        findViewById<TextView>(R.id.tvVersion).text = BuildConfig.VERSION_NAME
        myWebView = findViewById(R.id.webview);
        loadWebView(this, link, myWebView!!)
        var view = findViewById<RelativeLayout>(R.id.loading)
        Handler(Looper.getMainLooper()).postDelayed({
            view.visibility = View.INVISIBLE
        }, 5000)

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
            myWebView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return false
                }
            }
        }
    }

    override fun onBackPressed() {
        if (myWebView?.canGoBack() == true) {
            myWebView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}