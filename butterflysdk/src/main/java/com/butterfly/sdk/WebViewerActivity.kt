package com.butterfly.sdk

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Button
import com.butterfly.sdk.utils.SdkLogger
import com.butterfly.sdk.utils.Utils
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class WebViewerActivity : Activity() {
    interface NavigationRequestsListener {
        fun onNavigationRequest(urlString: String)
    }

    companion object {
        fun open(activity: Activity, apiKey: String) {
            Utils.saveContext(activity)
            if (apiKey.isEmpty()) return

            var languageCode = Locale.getDefault().language

            languageCodeToOverride?.let {
                if (it.length == 2) {
                    languageCode = it
                }
            }

            val customColorHexa = customColorHexaString ?: "n"

            var countryCode = "n"
            countryCodeToOverride?.let {
                if (it.length == 2) {
                    countryCode = it.lowercase()
                }
            }

            val urlString =
                "https://butterfly-button.web.app/reporter/" +
                        "?language=$languageCode" +
                        "&api_key=$apiKey" +
                        "&sdk-version=${Utils.BUTTERFLY_SDK_VERSION}" +
                        "&override_country=${countryCode}" +
                        "&colorize=${customColorHexa}" +
                        "&is-embedded-via-mobile-sdk=1"

            activity.startActivity(
                Intent(activity, WebViewerActivity::class.java).putExtra(
                    "url",
                    urlString
                )
            )
        }

        val TAG: String get() {
            return WebViewerActivity::class.java.simpleName
        }
        var languageCodeToOverride: String? = null
        var countryCodeToOverride: String? = null
        var customColorHexaString: String? = null // May be: "0xFF91BA48", "FF91BA48", "91BA48"

        private val urlWhiteList: HashSet<String> = HashSet()
    }

    private var initialUrl: String? = null
    private val webView: WebView by lazy { WebView(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(webView)

        val butterflyWebViewClient = ButterflyWebViewClient(object : NavigationRequestsListener {
            override fun onNavigationRequest(urlString: String) {
                if (urlString.isEmpty()) return

                if (urlString.startsWith("https://the-butterfly.bridge/")) {
                    urlString.split("https://the-butterfly.bridge/").lastOrNull()
                        ?.let { messageFromWebPage ->
                            val components = messageFromWebPage.split("::")
                            val command = components.firstOrNull() ?: ""

                            when (command) {
                                "log" -> {
//                                    SdkLogger.log(TAG, messageFromWebPage)
                                }

                                "cancel" -> {
                                    finish()
                                }

                                "open" -> {
                                    val url = components.lastOrNull() ?: ""
                                    if (url.isNotEmpty()) {
                                        try {
                                            val i = Intent(Intent.ACTION_VIEW)
                                            i.data = Uri.parse(url)
                                            startActivity(i)
                                        } catch (e: Throwable) {
                                            SdkLogger.error(TAG, e)
                                        }
                                    }
                                }

                                "page error" -> {
                                    webView.removeSelf()
                                    val btnQuit = Button(applicationContext)
                                    btnQuit.text = "Communication error!"
                                    btnQuit.setOnClickListener {
                                        finish()
                                    }
                                    webView.addView(btnQuit)
                                }

                                else -> SdkLogger.error(
                                    TAG,
                                    "unhandled message: $messageFromWebPage"
                                )
                            }
                        }
                }
            }

        })
        webView.webViewClient = butterflyWebViewClient

        webView.settings.setSupportMultipleWindows(true)

        window.decorView.apply {
//            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        webView.settings.javaScriptEnabled = true
        val nativeCallbacksToJs: (resultString: String, commandId: String) -> Unit =
            { resultString, commandId ->
                runOnUiThread {
                    val jsCommand = "bfPureJs.commandResults['$commandId'] = '$resultString';"
                    webView.evaluateJavascript(jsCommand) { result ->
//                    Log.d(TAG, result)
                    }
                }
            }

        val androidJavascriptInterface = AndroidJavascriptInterface(nativeCallbacksToJs)
        webView.addJavascriptInterface(androidJavascriptInterface, "androidJavascriptInterface")
        androidJavascriptInterface.host = this
        intent?.getStringExtra("url")?.let { url ->
            initialUrl = url
            webView.loadUrl(url)
        }
    }

    override fun onPause() {
        // leave a blank screen because in any case it will exit by itself (so eventually it will hide the reporter from the "recent apps" view).
        webView.removeSelf()

        super.onPause()
    }

    override fun onStop() {
        if (!isFinishing) {
            // Going background => exit this screen.
            finish()
        }

        super.onStop()
    }

    private class ButterflyWebViewClient(val navigationRequestsListener: NavigationRequestsListener) : WebViewClient() {
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
            var ignoreThis = true
            val urlString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request?.url?.toString() ?: ""
            } else {
                request?.toString() ?: ""
            }

            if (urlString.isEmpty()) return ignoreThis

            navigationRequestsListener.onNavigationRequest(urlString)

            if (isWhiteListed(urlString)) {
                view?.loadUrl(urlString)
                return ignoreThis
            }

            return ignoreThis
        }

        private fun isWhiteListed(urlString: String): Boolean {
            return urlWhiteList.any { item -> urlString.startsWith(item) }
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
        lateinit var host: WebViewerActivity

        @JavascriptInterface
        fun postMessage(messageFromJs: String) {
            val messageJson = JSONObject(messageFromJs)
            val commandName = messageJson.remove("commandName")?.toString() ?: ""
            val commandId = messageJson.remove("commandId")?.toString() ?: ""

            when (commandName) {
                "sendRequest" -> {
                    messageJson.remove("urlString")?.toString()?.let { urlString ->
                        messageJson.remove("key")?.toString()?.let { apiKey ->
                            val butterflyApiKey: String = if (Utils.isDebuggable() && !apiKey.startsWith("debug-")) {
                                "debug-$apiKey"
                            } else {
                                apiKey
                            }

                            Communicator(urlString, messageJson, mapOf("butterfly_host_api_key" to butterflyApiKey)).call { netwrokResult ->
                                var resultString = "error"
                                if (netwrokResult == "OK") {
                                    resultString = netwrokResult
                                }
                                nativeCallbacksToJs.invoke(resultString, commandId)
                            }
                        }
                    }
                }

                "navigateTo" -> {
                    messageJson.remove("urlString")?.toString()?.let { urlString ->
                        host.startActivity(
                            Intent(host, WebViewerActivity::class.java).putExtra(
                                "url",
                                urlString
                            )
                        )
                    }
                }
                
                "allowNavigation" -> {
                    messageJson.remove("urlString")?.toString()?.let { urlString ->
                        urlWhiteList.add(urlString)
                        nativeCallbacksToJs.invoke("OK", commandId)
                    }
                }

                else -> {
                    // Unhandled command
                }
            }
        }
    }

    class Communicator(val urlString: String, private val requestBody: JSONObject? = null, private val headers: Map<String, String> = mapOf()) {
        private val threadHandler: Handler by lazy {
            val handlerThread = HandlerThread("BFCommunicator Thread")
            handlerThread.start()
            val handler = Handler(handlerThread.looper)

            handler
        }

        fun call(callback: (String) -> Unit) {
            if(urlString.isEmpty()) {
                callback.invoke("")
                return;
            }

            val url: URL = URL(urlString)
            val looper = Looper.myLooper() ?: return
            val callerHandler = Handler(looper)
            threadHandler.post {
                try {
                    // Thanks a lot to: https://johncodeos.com/post-get-put-delete-requests-with-httpurlconnection/
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json") // The format of the content we're sending to the server
                    headers.forEach { headerEntry ->
                        connection.setRequestProperty(headerEntry.key, headerEntry.value)
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
