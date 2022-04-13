package com.butterfly.sdk.utils

import android.util.Log

class SdkLogger {
    companion object {
        fun log(tag: String, logMessage: String) {
            if (Utils.isDebuggable()) {
                Log.d(tag, logMessage)
            }
        }

        fun error(tag: String, errorMessage: String) {
            if (Utils.isDebuggable()) {
                Log.e(tag, errorMessage)
            }
        }

        fun error(tag: String, throwable: Throwable) {
            if (Utils.isDebuggable()) {
                error(tag, throwable.message.toString())
                throwable.printStackTrace()
            }
        }
    }
}
