package com.moblin.android.platform.webkit

import android.content.Context
import android.webkit.WebView

class InterimBrowserWebView(context: Context) : WebView(context) {
    fun borrowView(): WebView {
        return this
    }

    fun returnView() = Unit
}
