package com.aura.orbit.ui.sphere

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

@Composable
fun SphereScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE) }
    var showSetup by remember { mutableStateOf(!prefs.getBoolean("setup_done", false)) }
    var showContent by remember { mutableStateOf(prefs.getBoolean("setup_done", false)) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showContent) {
            OrbitWebView(showSetup = false, onSetupHidden = {})
        }

        AnimatedVisibility(
            visible = showSetup,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SetupScreen(
                onReady = {
                    prefs.edit().putBoolean("setup_done", true).apply()
                    showSetup = false
                    showContent = true
                }
            )
        }
    }
}

@Composable
fun SetupScreen(onReady: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "AURA ORBIT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22D3EE),
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tu universo de apps",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Para activar AuraOrbit como tu pantalla de inicio:",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "1. Toca el botón de abajo\n2. Selecciona AuraOrbit\n3. Elige \"Siempre\"",
                fontSize = 14.sp,
                color = Color(0xFF22D3EE),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B21A8)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Configurar como inicio",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continuar sin configurar",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OrbitWebView(showSetup: Boolean, onSetupHidden: () -> Unit) {

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

            resolveInfos.take(48).forEach { resolveInfo ->
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
