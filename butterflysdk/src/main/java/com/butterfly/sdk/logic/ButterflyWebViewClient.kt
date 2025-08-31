package com.butterfly.sdk.logic

import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.butterfly.sdk.logic.WebViewerActivity.Companion.urlWhiteList
import com.butterfly.sdk.logic.WebViewerActivity.NavigationRequestsListener

internal class ButterflyWebViewClient(val navigationRequestsListener: NavigationRequestsListener) : WebViewClient() {
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

