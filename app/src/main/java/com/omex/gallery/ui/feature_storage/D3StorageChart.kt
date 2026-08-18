package com.omex.gallery.ui.feature_storage

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class StorageChartJsBridge(
    private val onSliceSelectedCallback: (key: String, label: String, sizeBytes: Long, count: Int) -> Unit,
    private val onReadyCallback: () -> Unit
) {
    @JavascriptInterface
    fun onSliceSelected(key: String, label: String, sizeBytes: Long, count: Int) {
        onSliceSelectedCallback(key, label, sizeBytes, count)
    }

    @JavascriptInterface
    fun onChartReady() {
        onReadyCallback()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3StorageChart(
    jsonData: String,
    groupingMode: StorageGroupingMode,
    metricMode: StorageMetricMode,
    selectedSliceKey: String?,
    onSliceSelected: (key: String, label: String, sizeBytes: Long, count: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isChartReady by remember { mutableStateOf(false) }

    val jsBridge = remember {
        StorageChartJsBridge(
            onSliceSelectedCallback = { key, label, sizeBytes, count ->
                onSliceSelected(key, label, sizeBytes, count)
            },
            onReadyCallback = {
                isChartReady = true
            }
        )
    }

    LaunchedEffect(jsonData, groupingMode, metricMode, selectedSliceKey, isChartReady, webViewInstance) {
        val webView = webViewInstance
        if (webView != null && isChartReady) {
            val safeJson = jsonData.replace("\\", "\\\\").replace("'", "\\'")
            val safeKey = selectedSliceKey ?: ""
            val script = "window.loadStorageData('$safeJson', '${groupingMode.name}', '${metricMode.name}', '$safeKey');"
            webView.evaluateJavascript(script, null)
        }
    }

    Box(modifier = modifier.height(300.dp).fillMaxWidth().testTag("d3_storage_chart_container")) {
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("d3_storage_webview"),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.allowFileAccess = true

                    addJavascriptInterface(jsBridge, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isChartReady = true
                            val safeJson = jsonData.replace("\\", "\\\\").replace("'", "\\'")
                            val safeKey = selectedSliceKey ?: ""
                            view?.evaluateJavascript("window.loadStorageData('$safeJson', '${groupingMode.name}', '${metricMode.name}', '$safeKey');", null)
                        }
                    }

                    webChromeClient = WebChromeClient()
                    loadUrl("file:///android_asset/d3_storage_chart.html")
                    webViewInstance = this
                }
            },
            update = { view ->
                if (isChartReady) {
                    val safeJson = jsonData.replace("\\", "\\\\").replace("'", "\\'")
                    val safeKey = selectedSliceKey ?: ""
                    view.evaluateJavascript("window.loadStorageData('$safeJson', '${groupingMode.name}', '${metricMode.name}', '$safeKey');", null)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}
