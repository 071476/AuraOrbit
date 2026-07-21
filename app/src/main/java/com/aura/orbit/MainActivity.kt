package com.aura.orbit

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aura.orbit.data.BillingManager
import com.aura.orbit.ui.sphere.SphereScreen
import com.aura.orbit.ui.theme.AuraOrbitTheme

class MainActivity : ComponentActivity() {
    private var billingManager: BillingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuraOrbitTheme {
                AuraOrbitApp(billingManagerRef = { billingManager = it })
            }
        }
    }

    override fun onDestroy() {
        billingManager?.destroy()
        super.onDestroy()
    }
}

@Composable
fun AuraOrbitApp(billingManagerRef: (BillingManager) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val prefs = remember { context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE) }

    var showSetup by remember { mutableStateOf(!prefs.getBoolean("setup_done", false)) }
    var isPro by remember { mutableStateOf(false) }
    var billingReady by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }

    val billingManager = remember {
        BillingManager(context) { success ->
            if (success) {
                isPro = true
                showPaywall = false
            }
        }
    }

    DisposableEffect(Unit) {
        billingManagerRef(billingManager)
        billingManager.startConnection()

        val lifecycleOwner = context as androidx.lifecycle.LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPro = billingManager.isPro()
                billingReady = true
                if (!showSetup && isPro) {
                    showPaywall = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050510))) {

        // Esfera (siempre presente debajo)
        if (!showSetup && (isPro || showPaywall)) {
            SphereScreen()
        }

        // Pantalla de setup (primera vez)
        AnimatedVisibility(
            visible = showSetup,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SetupScreen(
                onReady = {
                    prefs.edit().putBoolean("setup_done", true).apply()
                    showSetup = false
                    isPro = billingManager.isPro()
                    if (!isPro) showPaywall = true
                }
            )
        }

        // Paywall (si no es pro)
        AnimatedVisibility(
            visible = showPaywall && !showSetup,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PaywallScreen(
                onPurchase = {
                    billingManager.launchPurchase(activity)
                },
                onContinue = {
                    showPaywall = false
                }
            )
        }
    }
}

@Composable
fun PaywallScreen(onPurchase: () -> Unit, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050510)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "AURA ORBIT PRO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF22D3EE),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Desbloquea tu universo de apps",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "✦ Esfera 3D con tus apps reales\n✦ Hasta 48 apps orbitando\n✦ Actualización automática\n✦ Diseño único de inicio",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onPurchase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B21A8)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Comprar por \$2.50 USD",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Probar gratis",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
