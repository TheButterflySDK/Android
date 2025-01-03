package com.butterfly.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import com.butterfly.sdk.utils.EventBus
import com.butterfly.sdk.utils.SdkLogger
import com.butterfly.sdk.utils.Utils
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class WebViewerActivity: Activity(), EventBus.Listener {
    class CloseAllEvent(val data: Activity) : EventBus.Event()
    private object IntentExtraKeys {
        const val URL = "url"
        const val SHOULD_CLEAR_CACHE = "shouldClearCache"
    }

    interface NavigationRequestsListener {
        fun onNavigationRequest(urlString: String)
    }

    companion object {
        private val eventBus = EventBus()
        private const val SHOULD_DISAPPEAR_ON_BLUR: Boolean = false

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

            var baseUrl = "https://butterfly-button.web.app/reporter/"
            var shouldClearCache = false
            try {
                val appInfo = activity.packageManager.getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
                appInfo.metaData?.getString("com.butterfly.sdk.BASE_URL")?.let { customBaseUrl ->
                    if (baseUrl != customBaseUrl) {
                        baseUrl = customBaseUrl
                        shouldClearCache = true
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }

            val urlString =
                baseUrl +
                        "?language=$languageCode" +
                        "&api_key=$apiKey" +
                        "&sdk-version=${Utils.BUTTERFLY_SDK_VERSION}" +
                        "&override_country=${countryCode}" +
                        "&colorize=${customColorHexa}" +
                        "&is-embedded-via-mobile-sdk=1"

            activity.startActivity(
                Intent(activity, WebViewerActivity::class.java)
                        .putExtra(IntentExtraKeys.URL, urlString)
                        .putExtra(IntentExtraKeys.SHOULD_CLEAR_CACHE, shouldClearCache)
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

    private var token: EventBus.ListenerToken? = null
    private lateinit var layout: RelativeLayout
    private var initialUrl: String? = null
    private var deviceRequestedTextZoom = 100
    private val webView: WebView by lazy {
        val w = WebView(this)
        // Keep for later use
        deviceRequestedTextZoom = w.settings.textZoom
        // Reset
        w.settings.textZoom = 100

        return@lazy w
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        token = eventBus.addListener(this, CloseAllEvent::class.java)
        layout = RelativeLayout(this)
        layout.addView(webView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        setContentView(layout)

        val closeButtonRelativeLayoutParams = RelativeLayout.LayoutParams(
                35.dpToPx(),
                35.dpToPx(),
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.ALIGN_PARENT_END) // Use ALIGN_PARENT_RIGHT for older versions
            marginEnd = 6 // margin right
            topMargin = 16 // margin top
        }

        val closeButton = TextView(this).apply {
            text = "ⓧ"
            textSize = 16f
            setTextColor(Color.BLACK)
//            setBackgroundResource(R.drawable.butterfly_semi_transparent_round_bg)
            gravity = Gravity.CENTER

            setOnClickListener {
                beGone()
            }
        }

        layout.addView(closeButton, closeButtonRelativeLayoutParams)

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
                                    beGone()
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
                                    markAsHandled("OK", command)
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

                                else -> {
                                    SdkLogger.error(
                                        TAG,
                                        "unhandled message: $messageFromWebPage"
                                    )
                                    markAsHandled("", command)
                                }
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
                    markAsHandled(commandId, resultString)
                }
            }

        val androidJavascriptInterface = AndroidJavascriptInterface(nativeCallbacksToJs)
        webView.addJavascriptInterface(androidJavascriptInterface, "androidJavascriptInterface")
        androidJavascriptInterface.host = this

        val shouldClearCache = intent?.getBooleanExtra(IntentExtraKeys.SHOULD_CLEAR_CACHE, false) ?: false
        if (shouldClearCache) {
            // from: https://stackoverflow.com/questions/34572748/how-to-make-android-webview-clear-cache
            webView.clearCache(true)
            webView.loadUrl("about:blank")
            webView.reload()
        }

        intent?.getStringExtra(IntentExtraKeys.URL)?.let { url ->
            initialUrl = url
            if (url.isEmpty()) return@let
            // Request headers only before loading URL
            Communicator.pingUrl(url, 5000) { isReachable ->
                if (isReachable) {
                    webView.loadUrl(url)
                } else {
                    webView.removeSelf()
                    val errorTextView = TextView(this)
                    errorTextView.text = "Disconnected ⛔️\n🔌"
                    errorTextView.textSize = 30f
                    errorTextView.setTextColor(Color.BLACK)
                    errorTextView.gravity = Gravity.CENTER
                    errorTextView.setOnClickListener {
                        finish()
                    }
                    setContentView(errorTextView)
                }
            }
        }
    }

    private fun beGone() {
        // Close all open activities if they're open
        eventBus.notify(CloseAllEvent(this))

        finish()
    }

    private fun markAsHandled(commandId: String, result: String) {
        val jsCommand = "bfPureJs.commandResults['$commandId'] = '$result';"
        webView.evaluateJavascript(jsCommand) { evaluationResult ->
            SdkLogger.log(TAG, evaluationResult)
        }
    }

    override fun onPause() {
        if (SHOULD_DISAPPEAR_ON_BLUR) {
            // leave a blank screen because in any case it will exit by itself (so eventually it will hide the reporter from the "recent apps" view).
            webView.removeSelf()
        }

        super.onPause()
    }

    override fun onStop() {
        if (SHOULD_DISAPPEAR_ON_BLUR) {
            if (!isFinishing) {
                // Going background => exit this screen.
                finish()
            }
        }

        super.onStop()
    }

    override fun onDestroy() {
        token?.remove()
        super.onDestroy()
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
            val ignoreUrl = true
            val urlString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request?.url?.toString() ?: ""
            } else {
                request?.toString() ?: ""
            }

            if (urlString.isEmpty()) return ignoreUrl

            navigationRequestsListener.onNavigationRequest(urlString)

            if (isWhiteListed(urlString)) {
                view?.loadUrl(urlString)
                return ignoreUrl
            }

            return ignoreUrl
        }

        private fun isWhiteListed(urlString: String): Boolean {
            return urlWhiteList.any { item -> urlString.startsWith(item) }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val initialUrl = this.initialUrl ?: ""
        if (webView.canGoBack() && webView.url != initialUrl) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private class AndroidJavascriptInterface(private val markAsHandled: (resultString: String, commandId: String) -> Unit) {
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

                            Communicator(urlString, messageJson, mapOf("butterfly_host_api_key" to butterflyApiKey)).call { networkResult ->
                                var resultString = "error"
                                if (networkResult == "OK") {
                                    resultString = networkResult
                                }
                                markAsHandled.invoke(resultString, commandId)
                            }
                        }
                    }
                }

                "navigateTo" -> {
                    messageJson.remove("urlString")?.toString()?.let { urlString ->
                        host.startActivity(
                            Intent(host, WebViewerActivity::class.java).putExtra(
                                IntentExtraKeys.URL,
                                urlString
                            )
                        )
                        markAsHandled("OK", commandId)
                    } ?: run {
                        markAsHandled("", commandId)
                    }
                }

                "allowNavigation" -> {
                    messageJson.remove("urlString")?.toString()?.let { urlString ->
                        urlWhiteList.add(urlString)
                        markAsHandled.invoke("OK", commandId)
                    } ?: run {
                        markAsHandled("", commandId)
                    }
                }

                "open" -> {
                    try {
                        val url = messageJson.getJSONArray("components").get(0).toString()
                        if (url.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            host.startActivity(intent)
                            markAsHandled.invoke("OK", commandId)
                        } else {
                            markAsHandled("", commandId)
                        }
                    } catch (e: Throwable) {
                        SdkLogger.error(TAG, e)
                        markAsHandled("", commandId)
                    }
                }

                else -> {
                    markAsHandled("", commandId)
                }
            }
        }
    }

    private class Communicator(private val urlString: String, private val requestBody: JSONObject? = null, private val headers: Map<String, String> = mapOf()) {
        companion object {

            private val bgThreadHandler: Handler by lazy {
                val handlerThread = HandlerThread("BFCommunicator Thread")
                handlerThread.start()
                val handler = Handler(handlerThread.looper)

                handler
            }

            // Ref: https://stackoverflow.com/a/3584332/2735029
            fun pingUrl(url: String, timeout: Int, callback: (Boolean) -> Unit) {
//                var url = url
//                url = url.replaceFirst(
//                    "^https".toRegex(),
//                    "http"
//                ) // (???) Otherwise an exception may be thrown on invalid SSL certificates.
                val looper = Looper.myLooper() ?: return
                val callerHandler = Handler(looper)
                bgThreadHandler.post {
                    val isReachable = try {
                        val connection: HttpURLConnection =
                            URL(url).openConnection() as HttpURLConnection
                        connection.connectTimeout = timeout
                        connection.readTimeout = timeout
                        connection.requestMethod = "HEAD"
                        val responseCode: Int = connection.responseCode
                        responseCode in 200..399
                    } catch (exception: IOException) {
                        SdkLogger.error(TAG, exception)
                        false
                    }

                    callerHandler.post {
                        callback.invoke(isReachable)
                    }
                }
            }
        }

        fun call(callback: (String) -> Unit) {
            if(urlString.isEmpty()) {
                callback.invoke("")
                return
            }

            val url = URL(urlString)
            val looper = Looper.myLooper() ?: return
            val callerHandler = Handler(looper)
            bgThreadHandler.post {
                try {
                    var responseString = ""
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

                    // Check if the connection is successful
                    val responseCode = connection.responseCode
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        responseString = connection.inputStream.bufferedReader()
                            .use { it.readText() }  // defaults to UTF-8
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

    override fun onEvent(event: EventBus.Event) {
        val closeAllEvent = (event as? CloseAllEvent) ?: return
        if (closeAllEvent.data != this) {
            finish()
        }
    }

    private fun <K, V> Map<K, V>.toJsonString(): String {
        return JSONObject(this).toString()
    }

    private fun View.removeSelf() {
        (parent as? ViewGroup)?.removeView(this)
    }
}
