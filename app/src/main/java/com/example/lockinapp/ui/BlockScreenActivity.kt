package com.example.lockinapp.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lockinapp.data.Prefs
import com.example.lockinapp.security.PasswordManager

class BlockScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val blockedPkg = intent.getStringExtra("blocked_pkg") ?: ""

        setContent {
            BlockScreen(blockedPkg = blockedPkg, onUnlocked = {
                finish()
            })
        }
    }
}

@Composable
private fun BlockScreen(blockedPkg: String, onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("The App is blocked.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("BLOCKED", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(blockedPkg, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val hashB64 = Prefs.getPasswordHash(ctx)
                val saltB64 = Prefs.getPasswordSalt(ctx)

                if (hashB64 == null || saltB64 == null) {
                    msg = "No password set."
                    return@Button
                }

                val ok = PasswordManager.verify(
                    input.toCharArray(),
                    PasswordManager.fromB64(saltB64),
                    PasswordManager.fromB64(hashB64)
                )

                if (ok) {
                    // unlock 5 minute pentru pachetul respectiv
                    val unlockUntil = System.currentTimeMillis() + 5 * 60 * 1000
                    Prefs.setUnlockUntil(ctx, blockedPkg, unlockUntil)
                    input = ""
                    onUnlocked()
                } else {
                    msg = "Wrong password."
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unblock in 5 minutes")
        }

        Spacer(Modifier.height(12.dp))
        Text(msg)
    }
}
