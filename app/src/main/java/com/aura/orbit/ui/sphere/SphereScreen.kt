package com.aura.orbit.ui.sphere

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SphereScreen() {

    var bridgeRef: WebAppBridge? = null

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val bridge = WebAppBridge(context)
            bridgeRef = bridge

            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                bridge.attachWebView(this)
                addJavascriptInterface(bridge, "AndroidBridge")

                loadUrl("file:///android_asset/web/index.html")
            }
        }
    )

    // Detectar cuando el usuario vuelve a la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bridgeRef?.reloadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

class WebAppBridge(private val context: Context) {

    private var webViewRef: WeakReference<WebView>? = null

    fun attachWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    fun reloadApps() {
        getInstalledApps()
    }

    @JavascriptInterface
    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    @JavascriptInterface
    fun getInstalledApps() {
        Thread {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val appsArray = JSONArray()

            resolveInfos.take(24).forEach { resolveInfo ->
                val appJson = JSONObject().apply {
                    put("packageName", resolveInfo.activityInfo.packageName)
                    put("name", resolveInfo.loadLabel(pm).toString())
                    put("iconBase64", getAppIconBase64(resolveInfo.activityInfo.packageName))
                }
                appsArray.put(appJson)
            }

            val jsonString = appsArray.toString()
            val base64Json = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val webView = webViewRef?.get()
            webView?.post {
                webView.evaluateJavascript("receiveApps('$base64Json')", null)
            }
        }.start()
    }

    @JavascriptInterface
    fun onAppLaunched(packageName: String) {}

    @JavascriptInterface
    fun onSphereTouched(angle: Float) {}

    private fun getAppIconBase64(packageName: String): String? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
