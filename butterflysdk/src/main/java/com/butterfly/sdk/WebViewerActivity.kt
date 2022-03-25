package com.butterfly.sdk

import android.app.Activity
import android.os.*
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class WebViewerActivity : Activity() {
    companion object {
        val TAG: String get() {
            return WebViewerActivity::class.java.simpleName
        }
    }
    private var initialUrl: String? = null
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_viewer)
        webView = findViewById(R.id.butterfly_web_view)
        webView.webViewClient = ButterflyWebViewClient { messageFromWebPage ->
            when (messageFromWebPage) {
                "log" -> {
                    Log.d(TAG, messageFromWebPage)
                }

                "cancel" -> {
                    finish()
                }

                "page error" -> {
                    webView.removeSelf()
                    val container: LinearLayout = findViewById(R.id.butterfly_web_view_main_view)
                    val txtView = TextView(applicationContext)
                    txtView.text = "Communication error!"
                    txtView.setOnClickListener {
                        finish()
                    }
                    container.addView(txtView)
                }

                else -> Log.e(
                    TAG,
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
        webView.addJavascriptInterface(AndroidJavascriptInterface { resultString, commandId ->
            runOnUiThread {
                val jsCommand = "bfPureJs.commandResults['$commandId'] = '$resultString';"
                webView.evaluateJavascript(jsCommand) { result ->
                    Log.d(TAG, result)
                }
            }
        }, "androidJavascriptInterface")
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
//            handler("page error")
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
//            handler("page error")
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

    class AndroidJavascriptInterface(private val nativeCallbacksToJs: (resultString: String, commandId: String) -> Unit) {
        @JavascriptInterface
        fun postMessage(messageFromJs: String) {
            val messageJson = JSONObject(messageFromJs)
            val commandName = messageJson.remove("commandName")?.toString() ?: ""
            when (commandName) {
                "sendRequest" -> {
                    messageJson.remove("urlString")?.toString()?.let { urlString ->
                        messageJson.remove("key")?.toString()?.let { apiKey ->
                            val commandId = messageJson.remove("commandId")?.toString() ?: ""
                            Communicator(urlString, messageJson, mapOf("butterfly_host_api_key" to apiKey)).call {
                                print(it)
                                var resultFromJs = "error"
                                if (it == "OK") {
                                    resultFromJs = it
                                }
                                nativeCallbacksToJs.invoke(resultFromJs, commandId)
                            }
                        }
                    }
                }
                else -> {
                    // Unhandled command
                }
            }
        }
    }

    class Communicator(urlString: String, private val requestBody: JSONObject? = null, private val headers: Map<String, String> = mapOf()) {
        private val url: URL = URL(urlString)
        private val threadHandler: Handler by lazy {
            val handlerThread = HandlerThread("BFCommunicator Thread")
            handlerThread.start()
            val handler = Handler(handlerThread.looper)

            handler
        }

        fun call(callback: (String) -> Unit) {
            val looper = Looper.myLooper() ?: return
            val callerHandler = Handler(looper)
            threadHandler.post {
                try {
                    // Thanks a lot to: https://johncodeos.com/post-get-put-delete-requests-with-httpurlconnection/
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json") // The format of the content we're sending to the server
                    headers.forEach { headerEntry ->
                        connection.setRequestProperty(headerEntry.key, headerEntry.value);
                    }
                    connection.setRequestProperty("Accept", "application/json") // The format of response we want to get from the server
                    connection.doInput = true
                    connection.doOutput = true

                    // Send the JSON we created
                    val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                    outputStreamWriter.write(requestBody.toString())
                    outputStreamWriter.flush()

                    var responseString = ""
                    // Check if the connection is successful
                    val responseCode = connection.responseCode
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        responseString = connection.inputStream.bufferedReader().use { it.readText() }  // defaults to UTF-8
//                        Log.d("Pretty Printed JSON :", responseString)
                    } else {
                        //Log.e("HTTPS URL CONNECTION ERROR", responseCode.toString())
                    }

                    callerHandler.post {
                        callback.invoke(responseString)
                    }
                } catch (e: Throwable) {
//                    e.printStackTrace()
                }
            }
        }
    }
}

private fun View.removeSelf() {
    (parent as? ViewGroup)?.removeView(this)
}
