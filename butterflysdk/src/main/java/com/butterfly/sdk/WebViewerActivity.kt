package com.butterfly.sdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewerActivity : AppCompatActivity() {
    private var initialUrl: String? = null
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_viewer)
        webView = findViewById(R.id.butterfly_web_view)
        webView.webViewClient = ButterflyWebViewClient { messageFromWebPage ->
            when (messageFromWebPage) {
                "cancel" -> {
                    finish()
                }

                else -> Log.e(
                    WebViewerActivity::class.java.simpleName,
                    "unhandled message: $messageFromWebPage"
                )
            }
        }

        webView.settings.setSupportMultipleWindows(true)

        window.decorView.apply {
//            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

//        this.flutterWebView = FlutterWebView(this, 1, mapOf(), view)

        webView.settings.javaScriptEnabled = true
        intent?.getStringExtra("url")?.let { url ->
            initialUrl = url
            webView.loadUrl(url)
        }
    }

    private class ButterflyWebViewClient(val handler: (String) -> (Unit)) : WebViewClient() {
        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }

//        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//            url?.let {
//                view?.loadUrl(it)
//            }
//            return true //super.shouldOverrideUrlLoading(view, url)
//        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val urlString = request?.url?.toString() ?: ""
            if (urlString.isEmpty()) return false

            if (urlString.startsWith("https://the-butterfly.bridge/")) {
                urlString.split("https://the-butterfly.bridge/").lastOrNull()?.let { messageFromWebPage ->
                    handler(messageFromWebPage)
                }
            }

            if (urlString.startsWith("https://butterfly-host.web.app/")) {
                view?.loadUrl(urlString)
                return true
            }

            return false
        }
    }

    override fun onBackPressed() {
        val initialUrl = this.initialUrl ?: ""
        if (webView.canGoBack() && webView.url != initialUrl) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}