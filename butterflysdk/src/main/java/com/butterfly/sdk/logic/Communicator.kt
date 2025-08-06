package com.butterfly.sdk.Logic

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.butterfly.sdk.Logic.WebViewerActivity.Companion.TAG
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
