package uk.co.skills4all.adultsafe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import uk.co.skills4all.adultsafe.api.AdultSafeApi
import uk.co.skills4all.adultsafe.api.AppSession
import uk.co.skills4all.adultsafe.data.SessionStore
import uk.co.skills4all.adultsafe.ui.components.BrandHero
import uk.co.skills4all.adultsafe.ui.components.AppCard
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeBackground
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeMuted
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeNavy
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeTeal
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeText

@Composable
fun SignInScreen(
    onUseDemo: () -> Unit,
    onAuthenticated: (AppSession) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember(context) {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    var page by remember { mutableStateOf("welcome") }
    var siteUrl by remember { mutableStateOf("https://") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finish(session: AppSession) {
        try {
            SessionStore.save(context, session)
            password = ""
            onAuthenticated(session)
        } catch (_: Exception) {
            error = "Could not securely save your sign-in. Check your device storage and try again."
        }
    }

    fun scan() {
        if (busy) return
        error = null
        // Opens Google's native camera scanner UI directly, with no intermediate URL form.
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue
                if (value.isNullOrBlank()) {
                    error = "No AdultSafe invite was found in that QR code."
                } else {
                    busy = true
                    scope.launch {
                        try {
                            finish(AdultSafeApi.acceptInvite(value))
                        } catch (e: Exception) {
                            error = e.message ?: "This invite could not be accepted."
                        } finally { busy = false }
                    }
                }
            }
            .addOnCanceledListener { busy = false }
            .addOnFailureListener {
                busy = false
                error = "The camera scanner could not open. Check Google Play services and try again."
            }
    }

    Surface(Modifier.fillMaxSize(), color = AdultSafeBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(30.dp))
                BrandHero("Welcome to AdultSafe", "Sign in to your AdultSafe Records site.")
                Spacer(Modifier.height(24.dp))
                if (page == "welcome") {
                    EntryCard(
                        title = "Sign in with email",
                        subtitle = "Use your site address, email or username and password.",
                        isQr = false,
                        onClick = { error = null; page = "email" }
                    )
                    Spacer(Modifier.height(14.dp))
                    EntryCard(
                        title = "Scan QR code",
                        subtitle = "Scan an AdultSafe invite QR from another screen.",
                        isQr = true,
                        onClick = { scan() }
                    )
                } else {
                    TextButton(onClick = { if (!busy) { error = null; page = "welcome" } }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                        Text(" Back")
                    }
                    AppCard {
                        Text("Sign in with email", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AdultSafeNavy)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = siteUrl, onValueChange = { siteUrl = it; error = null },
                            label = { Text("Site URL") }, singleLine = true, enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = user, onValueChange = { user = it; error = null },
                            label = { Text("Email or username") }, singleLine = true, enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it; error = null },
                            label = { Text("Password") }, singleLine = true, enabled = !busy,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    try {
                                        finish(AdultSafeApi.login(siteUrl, user, password))
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not sign in. Please try again."
                                    } finally { busy = false }
                                }
                            },
                            enabled = !busy && siteUrl.trim().length > 8 && user.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdultSafeNavy)
                        ) {
                            if (busy) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("  Signing in…")
                            } else Text("Sign in", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (busy && page == "welcome") {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("  Checking your invite…", color = AdultSafeMuted)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEC)),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    ) { Text(error ?: "", Modifier.padding(16.dp), color = Color(0xFF9F2222)) }
                }
                Spacer(Modifier.height(28.dp))
                TextButton(onClick = onUseDemo, enabled = !busy) {
                    Text("Explore demo mode", color = AdultSafeMuted)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EntryCard(title: String, subtitle: String, isQr: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(60.dp)
                    .background(if (isQr) Color(0xFFE2F7F1) else Color(0xFFE7EEFA), RoundedCornerShape(19.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isQr) Icons.Outlined.QrCodeScanner else Icons.Outlined.AlternateEmail,
                    contentDescription = null,
                    tint = if (isQr) Color(0xFF007F70) else AdultSafeNavy,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AdultSafeText)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = AdultSafeMuted)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AdultSafeNavy)
        }
    }
}
