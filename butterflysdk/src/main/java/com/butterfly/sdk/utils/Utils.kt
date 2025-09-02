package com.butterfly.sdk.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import java.lang.ref.WeakReference

class Utils {
    companion object {
        const val BUTTERFLY_SDK_VERSION: String = "2.2.0"

        private var isDebuggable: Boolean? = null
        var applicationContextWeakReference: WeakReference<Context>? = null

        /**
         * Lazy initialization of the underlying variable `isDebuggable`.
         *
         * @return `true` if the application is debuggable and if there's an application context available, otherwise `false`.
         */
        fun isDebuggable(): Boolean {
            if (isDebuggable != null) return isDebuggable ?: false

            val applicationContext = applicationContextWeakReference?.get() ?: return false
            isDebuggable = 0 != applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

            return isDebuggable ?: false
        }

        fun saveContext(context: Context) {
            if (applicationContextWeakReference != null) return

            applicationContextWeakReference = WeakReference(context.applicationContext)
        }
    }
}
