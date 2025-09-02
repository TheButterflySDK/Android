package com.butterfly.sdk.logic

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.butterfly.sdk.logic.WebViewerActivity.Companion.TAG
import com.butterfly.sdk.utils.SdkLogger
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal class Communicator(private val urlString: String, private val requestBody: JSONObject? = null, private val headers: Map<String, String> = mapOf()) {
    companion object {
        private val bgThreadHandler: Handler by lazy {
            val handlerThread = HandlerThread("BFCommunicator Thread")
            handlerThread.start()
            val handler = Handler(handlerThread.looper)

            handler
        }

        // Ref: https://stackoverflow.com/a/3584332/2735029
        fun pingUrl(url: String, timeout: Int, callback: (Boolean) -> Unit) {
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

        fun fetchButterflyParamsFromURL(
            urlParams: MutableMap<String, String>?,
            appKey: String,
            sdkVersion: String,
            callback: (Map<String, String>?) -> Unit
        ) {
            val jsonBody = mapOf(
                "apiKey" to appKey,
                "sdkVersion" to sdkVersion,
                "platform" to "android",
                "urlParams" to urlParams
            )

            val baseURL = "https://us-central1-butterfly-button.cloudfunctions.net/convertToUrlParams"
            val url = URL(baseURL)
            val looper = Looper.myLooper() ?: return
            val callerHandler = Handler(looper)

            bgThreadHandler.post {
                var resultParams: Map<String, String>? = null
                try {

                    val connection = url.openConnection() as HttpsURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doInput = true
                    connection.doOutput = true

                    // Send JSON body
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        val jsonString = JSONObject(jsonBody).toString()
                        writer.write(jsonString)
                        writer.flush()
                    }

                    if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                        val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonResponse = JSONObject(responseString)

                        if (jsonResponse.has("result") && jsonResponse.get("result") is JSONObject) {
                            val resultJson = jsonResponse.getJSONObject("result")
                            resultParams = mutableMapOf<String, String>().apply {
                                for (key in resultJson.keys()) {
                                    put(key, resultJson.getString(key))
                                }
                            }
                        } else {
                            SdkLogger.error(TAG, "Invalid or missing 'result' key in JSON")
                        }
                    } else {
                        SdkLogger.error(TAG, "HTTP error code: ${connection.responseCode}")
                    }
                } catch (e: Throwable) {
                    SdkLogger.error(TAG, "Exception: ${e.message}")
                }

                callerHandler.post {
                    callback(resultParams)
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
                    // SdkLogger.log(TAG, "Pretty Printed JSON: $responseString")
                } else {
                    // SdkLogger.error(TAG, "HTTP error code: $responseCode HTTPS URL CONNECTION ERROR")
                }

                callerHandler.post {
                    callback.invoke(responseString)
                }
            } catch (e: Throwable) {
                //SdkLogger.error(TAG, e)
            }
        }
    }
}
