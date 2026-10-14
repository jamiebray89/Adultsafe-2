package uk.co.skills4all.adultsafe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.co.skills4all.adultsafe.api.AppSession
import uk.co.skills4all.adultsafe.api.LiveApi
import uk.co.skills4all.adultsafe.api.LiveDevice
import uk.co.skills4all.adultsafe.ui.components.AppCard
import uk.co.skills4all.adultsafe.ui.components.SectionTitle
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeMuted

/** Revocations are executed on WordPress, never simulated by changing UI state. */
@Composable
fun DeviceManager(session: AppSession, onSelfRevoked: () -> Unit) {
    val scope = rememberCoroutineScope()
    var devices by remember(session.site.url) { mutableStateOf<List<LiveDevice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<LiveDevice?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(session.site.url, session.token, refresh) {
        loading = true
        error = null
        try { devices = LiveApi.devices(session.site.url, session.token) }
        catch (e: Exception) { error = e.message ?: "Could not load connected devices." }
        finally { loading = false }
    }

    val chosen = target
    if (chosen != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) target = null },
            title = { Text("Revoke this device?") },
            text = { Text("This immediately invalidates its AdultSafe token. The device must sign in again.\n\n" + chosen.name) },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            LiveApi.revokeDevice(session.site.url, session.token, chosen.id)
                            target = null
                            if (chosen.id == session.deviceId) onSelfRevoked() else refresh++
                        } catch (e: Exception) {
                            error = e.message ?: "Unable to revoke device."
                            target = null
                        } finally { busy = false }
                    }
                }) { Text("Revoke") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { target = null }) { Text("Cancel") } }
        )
    }

    AppCard {
        SectionTitle("Connected devices", "Manage authorised AdultSafe app sessions.")
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        if (!loading && devices.isEmpty()) Text("No connected devices.", color = AdultSafeMuted)
        devices.forEach { d ->
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(d.name + if (d.id == session.deviceId) " · This phone" else "")
            Text(if (d.revoked) "Revoked" else "Last used: " + d.lastUsed, color = AdultSafeMuted)
            OutlinedButton(onClick = { target = d }, enabled = !d.revoked && !busy,
                modifier = Modifier.fillMaxWidth()) { Text("Revoke access") }
        }
        TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Refresh devices") }
    }
}
