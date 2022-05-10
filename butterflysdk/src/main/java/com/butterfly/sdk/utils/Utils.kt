package com.butterfly.sdk.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import java.lang.ref.WeakReference

class Utils {
    companion object {
        const val BUTTERFLY_SDK_VERSION: String = "1.1.5"

        var isDebuggable: Boolean? = null
        var applicationContextWeakReference: WeakReference<Context>? = null

        fun isDebuggable(): Boolean {
            isDebuggable?.let {
                return it
            } ?: run {
                val applicationContext = applicationContextWeakReference?.get() ?: return false

                isDebuggable = 0 != applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

                return isDebuggable ?: false
            }
        }

        fun saveContext(context: Context) {
            applicationContextWeakReference = WeakReference(context.applicationContext)
        }
    }
}
