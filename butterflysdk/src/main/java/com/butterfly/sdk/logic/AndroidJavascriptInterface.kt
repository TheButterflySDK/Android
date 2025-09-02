package com.butterfly.sdk.logic

import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import com.butterfly.sdk.logic.WebViewerActivity.Companion.TAG
import com.butterfly.sdk.logic.WebViewerActivity.Companion.urlWhiteList
import com.butterfly.sdk.logic.WebViewerActivity.IntentExtraKeys
import com.butterfly.sdk.utils.SdkLogger
import com.butterfly.sdk.utils.Utils
import org.json.JSONObject

internal class AndroidJavascriptInterface(private val markAsHandled: (resultString: String, commandId: String) -> Unit) {
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

