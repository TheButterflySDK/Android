package com.butterfly.sdk.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.WebView
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import com.butterfly.sdk.R
import com.butterfly.sdk.utils.EventBus
import com.butterfly.sdk.utils.SdkLogger
import com.butterfly.sdk.utils.Utils
import org.json.JSONObject
import java.util.Locale

class WebViewerActivity: Activity(), EventBus.Listener {
    class AbortEvent(val caller: Activity) : EventBus.Event()
    internal object IntentExtraKeys {
        const val URL = "url"
        const val SHOULD_CLEAR_CACHE = "shouldClearCache"
    }

    interface NavigationRequestsListener {
        fun onNavigationRequest(urlString: String)
    }

    internal companion object {
        private val eventBus = EventBus()
        private const val SHOULD_DISAPPEAR_ON_BLUR: Boolean = false
        private var webViewsCount = 0

        val TAG: String get() {
            return WebViewerActivity::class.java.simpleName
        }
        var languageCodeToOverride: String? = null
        var countryCodeToOverride: String? = null
        var customColorHexaString: String? = null // May be: "0xFF91BA48", "FF91BA48", "91BA48"

        internal val urlWhiteList: HashSet<String> = HashSet()
        private val cachedButterflyParams: HashMap<String, Map<String, String>> = hashMapOf()
        // Reporter Handling
        fun open(activity: Activity, apiKey: String) {
            open(activity, apiKey, null)
        }

        // Reporter Handling via deep link
        fun handleIncomingURI(activity: Activity, uri: Uri, apiKey: String) {
            val urlParams: MutableMap<String, String> = extractParamsFromUri(uri)
            if (urlParams.isEmpty()) return

            val cachedKeys = urlParams.keys.sorted().joinToString(",")

            fun handleResponse(backendParams: Map<String, String>) {
                val extraParams = convertMapToQueryStringParams(backendParams)

                if (extraParams.isEmpty()) {
                    SdkLogger.log(TAG, "No need to handle deep link params. Aborting URL handling...")
                } else {
                    open(activity, apiKey, extraParams)
                }
            }

            cachedButterflyParams[cachedKeys]?.let { cachedBackendParams ->
                SdkLogger.log(TAG, "Using cached deep link params for keys: $cachedKeys")
                handleResponse(cachedBackendParams)
            } ?: run {
                Communicator.fetchButterflyParamsFromURL(
                    urlParams,
                    appKey = apiKey,
                    sdkVersion = Utils.BUTTERFLY_SDK_VERSION
                ) { butterflyParams ->
                    val backendParams = butterflyParams ?: return@fetchButterflyParamsFromURL
                    cachedButterflyParams[cachedKeys] = backendParams
                    handleResponse(backendParams)
                }
            }
        }

        private fun open(activity: Activity, apiKey: String, extraParams: String?) {
            Utils.saveContext(activity)
            if (apiKey.isEmpty()) return
            if (webViewsCount != 0) return

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
            var applicationId = ""
            try {
                val appInfo = activity.packageManager.getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
                applicationId = appInfo.packageName
                appInfo.metaData?.getString("com.butterfly.sdk.BASE_URL")?.let { customBaseUrl ->
                    if (baseUrl != customBaseUrl) {
                        baseUrl = customBaseUrl
                        shouldClearCache = true
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }

            var urlString =
                baseUrl +
                        "?language=$languageCode" +
                        "&api_key=$apiKey" +
                        "&sdk-version=${Utils.BUTTERFLY_SDK_VERSION}" +
                        "&override_country=${countryCode}" +
                        "&colorize=${customColorHexa}" +
                        "&is-embedded-via-mobile-sdk=1"

            extraParams?.takeIf { it.isNotEmpty() }?.let {
                urlString += "&$it"
            }

            applicationId.trim().takeIf { it.isNotEmpty() }?.let {
                urlString += "&mobile-app-id=$it"
            }

            activity.startActivity(
                Intent(activity, WebViewerActivity::class.java)
                    .putExtra(IntentExtraKeys.URL, urlString)
                    .putExtra(IntentExtraKeys.SHOULD_CLEAR_CACHE, shouldClearCache)
            )
        }

        private fun extractParamsFromUri(uri: Uri): MutableMap<String, String> {
            val params = mutableMapOf<String, String>()

            if (uri.toString().isEmpty()) {
                return params
            }

            // Works for both http(s):// and custom-scheme URIs like butterfly://
            for (name in uri.queryParameterNames) {
                val value = uri.getQueryParameter(name)
                if (!name.isNullOrEmpty() && value != null) {
                    params[name] = value
                }
            }

            return params
        }

        fun convertMapToQueryStringParams(resultParams: Map<String, String>?): String {
            val params: Map<String, String> = resultParams ?: return ""
            if (params.isEmpty()) return ""

            val queryItems = params.map { (key, value) ->
                val encodedKey = Uri.encode(key)
                val encodedValue = Uri.encode(value)
                "$encodedKey=$encodedValue"
            }

            if (queryItems.isEmpty()) return ""

            return queryItems.joinToString("&")
        }
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

        webViewsCount++
        token = eventBus.addListener(this, AbortEvent::class.java)
        layout = RelativeLayout(this)
        layout.addView(webView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        setContentView(layout)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            webView.setOnApplyWindowInsetsListener { v, insets ->
                val ime = insets.getInsets(WindowInsets.Type.ime())
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                val bottom = maxOf(ime.bottom, bars.bottom)
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom + 500)
                insets
            }
        }

        val abortButtonRelativeLayoutParams = RelativeLayout.LayoutParams(
                35.dpToPx(),
                35.dpToPx(),
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT) // Use ALIGN_PARENT_RIGHT for older versions
            marginEnd = 6 // margin right
            topMargin = 16 // margin top
        }

        val abortButton = TextView(this).apply {
            text = "ⓧ"
            textSize = 16f
            setTextColor(Color.BLACK)
//            setBackgroundResource(R.drawable.butterfly_semi_transparent_round_bg)
            gravity = Gravity.CENTER

            setOnClickListener {
                beGone()
            }
        }

        layout.addView(abortButton, abortButtonRelativeLayoutParams)

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

                                "close" -> {
                                    finish()
                                }

                                "cancel", "abort" -> {
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
        eventBus.notify(AbortEvent(this))

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
        webView.alpha = 0f

        super.onPause()
    }

    override fun onResume() {
        webView.alpha = 1f

        super.onResume()
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
        webViewsCount--

        super.onDestroy()
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

    override fun onEvent(event: EventBus.Event) {
        val abortEvent = (event as? AbortEvent) ?: return
        if (abortEvent.caller != this) {
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
