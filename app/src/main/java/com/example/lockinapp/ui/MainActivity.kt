package com.example.lockinapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lockinapp.data.Prefs
import com.example.lockinapp.data.RuleEngine
import com.example.lockinapp.security.PasswordManager
import com.example.lockinapp.vpn.DnsBlockVpnService
import com.example.lockinapp.vpn.VpnStatus

data class AppInfo(val label: String, val packageName: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val ctx = LocalContext.current
    val pm = ctx.packageManager

    val priority = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.facebook.katana"
    )

    val allApps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        pm.queryIntentActivities(intent, 0)
            .map { ri ->
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareByDescending<AppInfo> { priority.contains(it.packageName) }
                    .thenBy { it.label.lowercase() }
            )
    }

    var search by remember { mutableStateOf("") }

    val apps = remember(allApps, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) allApps
        else allApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    var blocked by remember { mutableStateOf(Prefs.getBlockedPackages(ctx).toSet()) }
    var newPassword by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    var vpnActive by remember { mutableStateOf(VpnStatus.isVpnActive(ctx)) }

    LaunchedEffect(status) {
        vpnActive = VpnStatus.isVpnActive(ctx)
    }

    val startVpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            DnsBlockVpnService.start(ctx)
            status = "VPN started."
        } else {
            status = "VPN not authorized."
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("LockInApp") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Password", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Set / change password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (newPassword.length < 4) {
                        status = "Password must be at least 4 characters."
                        return@Button
                    }
                    val salt = PasswordManager.generateSalt()
                    val hash = PasswordManager.hashPassword(newPassword.toCharArray(), salt)
                    Prefs.setPasswordHashSalt(ctx, PasswordManager.b64(hash), PasswordManager.b64(salt))
                    newPassword = ""
                    status = "Password saved."
                }
            ) { Text("Save password") }

            Spacer(Modifier.height(16.dp))
            Text("Apps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search (name or package)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(apps) { app ->
                    val isBlocked = blocked.contains(app.packageName)
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(app.label)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = isBlocked,
                                    onCheckedChange = { checked ->
                                        val nextBlocked = blocked.toMutableSet()
                                        if (checked) {
                                            nextBlocked.add(app.packageName)
                                            if (Prefs.getMode(ctx, app.packageName).isBlank()) {
                                                Prefs.setMode(ctx, app.packageName, "INDEFINITE")
                                            }
                                        } else {
                                            nextBlocked.remove(app.packageName)
                                        }
                                        blocked = nextBlocked
                                        Prefs.setBlockedPackages(ctx, blocked)
                                    }
                                )
                            }

                            if (isBlocked) {
                                Spacer(Modifier.height(8.dp))
                                AppRuleEditor(packageName = app.packageName)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            Text(
                text = if (vpnActive) "VPN Status: Connected" else "VPN Status: Not connected",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val intent = VpnService.prepare(ctx)
                    if (intent != null) {
                        startVpnLauncher.launch(intent)
                    } else {
                        DnsBlockVpnService.start(ctx)
                        status = "VPN started."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start Site Blocking (VPN)") }

            OutlinedButton(
                onClick = {
                    DnsBlockVpnService.stop(ctx)
                    status = "VPN stopped."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Stop VPN") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open VPN Settings") }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open Accessibility settings") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ctx.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Overlay permission (optional)") }

            if (status.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(status)
            }
        }
    }
}

@Composable
private fun AppRuleEditor(packageName: String) {
    val ctx = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(Prefs.getMode(ctx, packageName)) }

    var startText by remember { mutableStateOf(minToText(Prefs.getStartMinute(ctx, packageName))) }
    var endText by remember { mutableStateOf(minToText(Prefs.getEndMinute(ctx, packageName))) }
    var msg by remember { mutableStateOf("") }

    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Mode: $mode")
            Box {
                TextButton(onClick = { expanded = true }) { Text("Change") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("INDEFINITE") }, onClick = {
                        mode = "INDEFINITE"
                        Prefs.setMode(ctx, packageName, mode)
                        expanded = false
                        msg = ""
                    })
                    DropdownMenuItem(text = { Text("DURATION") }, onClick = {
                        mode = "DURATION"
                        Prefs.setMode(ctx, packageName, mode)
                        expanded = false
                        msg = ""
                    })
                    DropdownMenuItem(text = { Text("INTERVAL") }, onClick = {
                        mode = "INTERVAL"
                        Prefs.setMode(ctx, packageName, mode)
                        expanded = false
                        msg = ""
                    })
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        when (mode) {
            "INDEFINITE" -> {
                Text("Blocked until you enter the password.", style = MaterialTheme.typography.bodySmall)
            }

            "DURATION" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            RuleEngine.applyDurationMinutes(ctx, packageName, 30)
                            msg = "Blocked for 30 minutes."
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("30 min") }

                    Button(
                        onClick = {
                            RuleEngine.applyDurationMinutes(ctx, packageName, 60)
                            msg = "Blocked for 60 minutes."
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("60 min") }
                }
                val until = Prefs.getLockedUntil(ctx, packageName)
                if (until > System.currentTimeMillis()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Active now.", style = MaterialTheme.typography.bodySmall)
                }
            }

            "INTERVAL" -> {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Start (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("End (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val s = textToMin(startText)
                        val e = textToMin(endText)
                        if (s == null || e == null) {
                            msg = "Invalid format. Example: 22:00"
                            return@Button
                        }
                        Prefs.setInterval(ctx, packageName, s, e)
                        msg = "Interval saved."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save interval") }
                Text("The interval can cross midnight (e.g., 22:00–07:00).", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (msg.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun minToText(min: Int): String {
    val h = (min / 60).coerceIn(0, 23)
    val m = (min % 60).coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}

private fun textToMin(s: String): Int? {
    val parts = s.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23) return null
    if (m !in 0..59) return null
    return h * 60 + m
}
