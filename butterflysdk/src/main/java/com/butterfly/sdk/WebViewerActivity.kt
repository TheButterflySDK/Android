package com.butterfly.sdk

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat

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

                "page error" -> {
                    webView.removeSelf()
                    val container: LinearLayoutCompat = findViewById(R.id.butterfly_web_view_main_view)
                    val txtView = TextView(applicationContext)
                    txtView.text = "Communication error!"
                    txtView.setOnClickListener {
                        finish()
                    }
                    container.addView(txtView)
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

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            handler("page error")
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            handler("page error")
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val urlString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request?.url?.toString() ?: ""
            } else {
                request?.toString() ?: ""
            }
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

private fun View.removeSelf() {
    (parent as? ViewGroup)?.removeView(this)
}
