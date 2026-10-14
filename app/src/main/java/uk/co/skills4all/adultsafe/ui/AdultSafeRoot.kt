package uk.co.skills4all.adultsafe.ui

import android.app.KeyguardManager
import android.content.Context
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uk.co.skills4all.adultsafe.api.*
import uk.co.skills4all.adultsafe.data.DemoRepository
import uk.co.skills4all.adultsafe.data.SessionStore
import uk.co.skills4all.adultsafe.model.AppMode
import uk.co.skills4all.adultsafe.ui.components.*
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeBackground
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeMuted
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeNavy

private enum class Tab(val label: String) { Home("Home"), People("People"), Alerts("Alerts"), Docs("Docs"), Settings("Settings") }

@Composable
fun AdultSafeRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val restored = remember(context) { SessionStore.restore(context) }
    var session by remember { mutableStateOf<AppSession?>(restored) }
    var savedSites by remember { mutableStateOf(SessionStore.saved(context)) }
    var showCreate by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(if (restored == null) AppMode.Disconnected else AppMode.Locked) }
    var tab by remember { mutableStateOf(Tab.Home) }
    var selected by remember { mutableStateOf<LivePerson?>(null) }
    var filter by remember { mutableStateOf("") }
    var data by remember { mutableStateOf<LiveHome?>(null) }
    var filtered by remember { mutableStateOf<List<LivePerson>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var unlocking by remember { mutableStateOf(false) }

    fun validateUnlockedSession() {
        val active = session ?: return
        unlocking = true
        error = null
        scope.launch {
            try {
                val me = LiveApi.validate(active.site.url, active.token)
                val scopes = me.optJSONArray("permissions")?.let { array ->
                    (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                } ?: active.permissions
                val updated = active.copy(permissions = scopes)
                SessionStore.save(context, updated)
                session = updated
                savedSites = SessionStore.saved(context)
                mode = AppMode.ConnectedLive
            } catch (e: Exception) {
                error = e.message ?: "Unable to verify your saved sign-in."
            } finally { unlocking = false }
        }
    }
    val lockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) validateUnlockedSession()
    }
    val activity = context as? FragmentActivity
    val biometric = remember(activity) {
        activity?.let { host ->
            BiometricPrompt(host, ContextCompat.getMainExecutor(host), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    validateUnlockedSession()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    error = errString.toString()
                }
            })
        }
    }
    // Re-lock confidential screens after returning from background inactivity.
    // Brief camera and document picker trips do not invalidate in-progress work.
    var stoppedAt by remember { mutableLongStateOf(0L) }
    DisposableEffect(activity) {
        if (activity == null) onDispose { }
        else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && mode == AppMode.ConnectedLive) {
                    stoppedAt = SystemClock.elapsedRealtime()
                } else if (event == Lifecycle.Event.ON_START && stoppedAt > 0L) {
                    if (mode == AppMode.ConnectedLive &&
                        SystemClock.elapsedRealtime() - stoppedAt >= 30000L) {
                        mode = AppMode.Locked
                    }
                    stoppedAt = 0L
                }
            }
            activity.lifecycle.addObserver(observer)
            onDispose { activity.lifecycle.removeObserver(observer) }
        }
    }

    fun unlock() {
        val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!manager.isDeviceSecure) {
            error = "Set a device PIN, password or pattern in Android settings before opening confidential records."
            return
        }
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (Build.VERSION.SDK_INT >= 30 && activity != null && biometric != null &&
            BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometric.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock AdultSafe")
                    .setSubtitle("Use your fingerprint, face or device PIN")
                    .setAllowedAuthenticators(authenticators)
                    .build()
            )
        } else {
            val intent = manager.createConfirmDeviceCredentialIntent("Unlock AdultSafe", "Confirm your device PIN, pattern or password")
            if (intent == null) error = "Secure device authentication is unavailable. Your records remain locked."
            else lockLauncher.launch(intent)
        }
    }

    LaunchedEffect(mode, session?.site?.url, session?.token, refresh) {
        if (mode == AppMode.ConnectedLive) {
            val s = session ?: return@LaunchedEffect
            loading = true
            error = null
            try {
                data = LiveApi.load(s.site.url, s.token)
            } catch (e: Exception) {
                data = null // Never show demonstration records when a live request fails.
                error = e.message ?: "Unable to load records."
            } finally { loading = false }
        }
    }
    LaunchedEffect(mode, filter) {
        filtered = null
        if (mode == AppMode.ConnectedLive && filter.isNotBlank()) {
            val s = session ?: return@LaunchedEffect
            try { filtered = LiveApi.filteredPeople(s.site.url, s.token, filter) }
            catch (e: Exception) { error = e.message ?: "Unable to load filtered people." }
        }
    }

    fun leave() {
        session = null
        selected = null
        data = null
        filtered = null
        error = null
        mode = AppMode.Disconnected
        tab = Tab.Home
        savedSites = SessionStore.saved(context)
    }

    if (showCreate && mode == AppMode.ConnectedLive && session != null) {
        NewPersonDialog(
            session = session!!,
            onDismiss = { showCreate = false },
            onCreated = {
                showCreate = false
                tab = Tab.People
                filter = ""
                refresh++
            }
        )
    }

    when (mode) {
        AppMode.Disconnected, AppMode.Connecting, AppMode.Error -> SignInScreen(
            onUseDemo = {
                session = null
                data = null
                selected = null
                filter = ""
                mode = AppMode.ConnectedDemo
            },
            onAuthenticated = { s ->
                session = s
                savedSites = SessionStore.saved(context)
                data = null
                selected = null
                tab = Tab.Home
                filter = ""
                mode = AppMode.ConnectedLive
            }
        )
        AppMode.Locked -> Surface(color = AdultSafeBackground, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Logo(size = 84)
                Spacer(Modifier.height(18.dp))
                Text("AdultSafe is locked", fontSize = 25.sp, color = AdultSafeNavy, fontWeight = FontWeight.Bold)
                Text(session?.site?.name.orEmpty(), maxLines = 2, color = AdultSafeMuted)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { unlock() }, enabled = !unlocking) { Text(if (unlocking) "Checking session…" else "Secure unlock") }
                if (error != null) { Spacer(Modifier.height(12.dp)); Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = {
                    val s = session
                    if (s != null) scope.launch { runCatching { LiveApi.logout(s.site.url, s.token) } }
                    runCatching { SessionStore.clear(context) }
                    leave()
                }) { Text("Sign out") }
            }
        }
        AppMode.ConnectedDemo, AppMode.ConnectedLive -> {
            val isDemo = mode == AppMode.ConnectedDemo
            val visible = if (isDemo) {
                DemoRepository.people().map {
                    LivePerson(it.id, it.name, it.initials, "volunteer", it.role, emptyList(),
                        it.dbsStatus.name, it.dbsExpiry.orEmpty(), listOfNotNull(it.warning.takeIf(String::isNotBlank)), "", true)
                }
            } else data?.people.orEmpty()
            val shown = if (filter.isBlank() || isDemo) visible else filtered.orEmpty()
            val displayTitle = if (isDemo) "AdultSafe Demo" else session?.site?.name.orEmpty()
            Scaffold(
                containerColor = AdultSafeBackground,
                topBar = {
                    Surface(color = Color.White, shadowElevation = 1.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Logo(size = 42)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AdultSafe", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AdultSafeNavy)
                                Text(displayTitle + if (isDemo) " · Demo" else "", maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, color = AdultSafeMuted, fontSize = 12.sp)
                            }
                            IconButton(onClick = { refresh++ }, enabled = !isDemo && !loading) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh live records", tint = AdultSafeNavy)
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Color.White, tonalElevation = 2.dp) {
                        Tab.entries.forEach { item ->
                            val icon = when (item) {
                                Tab.Home -> Icons.Outlined.Dashboard
                                Tab.People -> Icons.Outlined.People
                                Tab.Alerts -> Icons.Outlined.Notifications
                                Tab.Docs -> Icons.Outlined.Description
                                Tab.Settings -> Icons.Outlined.Settings
                            }
                            NavigationBarItem(
                                selected = tab == item, onClick = { selected = null; tab = item; if (item != Tab.People) filter = "" },
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(item.label, maxLines = 1, fontSize = 11.sp) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    if (selected != null && session != null && !isDemo) {
                        LivePersonScreen(person = selected!!, session = session!!, onBack = { selected = null },
                            onChanged = { refresh++ })
                    } else if (selected != null && isDemo) {
                        Column(Modifier.padding(20.dp)) {
                            TextButton(onClick = { selected = null }) { Text("← Back") }
                            Text(selected!!.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Sample record. Actions are unavailable in demo mode.")
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (error != null) Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE6E6))) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(error.orEmpty(), color = Color(0xFF842525))
                                    TextButton(onClick = { refresh++ }) { Text("Retry") }
                                }
                            }
                            when (tab) {
                                Tab.Home -> {
                                    val tiles = if (isDemo) DemoRepository.dashboardTiles().map { LiveTile(it.slug, it.title, it.count, it.slug) }
                                        else data?.tiles.orEmpty()
                                    LiveDashboard(tiles, isDemo || data != null, onTile = { t ->
                                        filter = t.filter
                                        selected = null
                                        tab = Tab.People
                                    })
                                }
                                Tab.People -> LivePeopleList(shown, loading, filter, filter.isNotBlank(),
                                    onClearFilter = { filter = "" }, onSelect = { selected = it },
                                    ready = isDemo || data != null,
                                    onCreate = if (!isDemo && (session?.permissions?.contains("people.write") == true || session?.roles?.contains("manager") == true)) { { showCreate = true } } else null)
                                Tab.Alerts -> {
                                    val alerts = if (isDemo) DemoRepository.alerts().map {
                                        LiveAlert(it.title, it.body, it.tone.name, it.personId.orEmpty())
                                    } else data?.alerts.orEmpty()
                                    LiveAlerts(alerts, visible, ready = isDemo || data != null, onPerson = { selected = it })
                                }
                                Tab.Docs -> {
                                    val ids = if (isDemo) emptySet() else data?.documentPersonIds.orEmpty()
                                    LivePeopleList(if (isDemo) visible else visible.filter { it.id in ids }, loading, "", false,
                                        onClearFilter = {}, onSelect = { selected = it }, title = "Documents",
                                        subtitle = "People with outstanding documents or signing actions",
                                        ready = isDemo || data != null)
                                }
                                Tab.Settings -> LiveSettings(
                                    title = displayTitle, url = if (isDemo) "" else session?.site?.url.orEmpty(),
                                    onLock = { if (!isDemo) { error = null; mode = AppMode.Locked } },
                                    onSignOut = {
                                        val s = session
                                        if (s != null) scope.launch { runCatching { LiveApi.logout(s.site.url, s.token) } }
                                        runCatching { SessionStore.clear(context) }
                                        leave()
                                    },
                                    onAddSite = { leave() },
                                    sites = savedSites,
                                    onSwitch = { site ->
                                        try {
                                            SessionStore.activate(context, site)
                                            session = site
                                            selected = null
                                            data = null
                                            filter = ""
                                            tab = Tab.Home
                                            mode = AppMode.Locked
                                        } catch (e: Exception) {
                                            error = e.message ?: "Unable to switch site."
                                        }
                                    },
                                    isDemo = isDemo,
                                    session = session
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveDashboard(tiles: List<LiveTile>, ready: Boolean, onTile: (LiveTile) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        if (ready && tiles.isEmpty()) item { MessageCard("All clear", "There are no outstanding dashboard actions.") }
        items(tiles) { t ->
            Card(onClick = { onTile(t) }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E2)),
                shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = AdultSafeNavy)
                    Text(t.count.toString(), fontSize = 29.sp, color = AdultSafeNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LivePeopleList(
    people: List<LivePerson>, loading: Boolean, filter: String, filtered: Boolean,
    onClearFilter: () -> Unit, onSelect: (LivePerson) -> Unit,
    title: String = "People", subtitle: String = "Volunteers and parents from your AdultSafe site",
    onCreate: (() -> Unit)? = null,
    ready: Boolean = true
) {
    var search by remember { mutableStateOf("") }
    val matches = people.filter {
        it.name.contains(search, true) || it.role.contains(search, true) ||
            it.personType.contains(search, true) || it.sections.any { s -> s.contains(search, true) }
    }.sortedBy { it.name.substringAfterLast(" ").lowercase() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onCreate != null) TextButton(onClick = onCreate) { Text("+ Add person") }
        } }
        item { Text(subtitle, color = AdultSafeMuted) }
        if (filtered) item { TextButton(onClick = onClearFilter) { Text("Clear filter: " + filter) } }
        item { OutlinedTextField(search, { search = it }, label = { Text("Search people") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (matches.isEmpty() && !loading && ready) item {
            MessageCard("No matching records", "Try another search or clear the filter. The WordPress site returned no matching people.")
        }
        items(matches, key = { it.id }) { p ->
            Card(onClick = { onSelect(p) }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(AdultSafeNavy, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) {
                        Text(p.initials, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(p.personType.replaceFirstChar { it.uppercase() } + " · " + p.role,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, color = AdultSafeMuted, fontSize = 13.sp)
                        if (p.warnings.isNotEmpty()) Text(p.warnings.joinToString(", "), color = Color(0xFF984719), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAlerts(alerts: List<LiveAlert>, people: List<LivePerson>, ready: Boolean, onPerson: (LivePerson) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Alerts", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        if (alerts.isEmpty() && ready) item { MessageCard("No alerts", "There are no current alerts for this site.") }
        items(alerts) { a ->
            Card(onClick = { people.firstOrNull { it.id == a.personId }?.let(onPerson) },
                colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text(a.title, fontWeight = FontWeight.Bold)
                    Text(a.message, color = AdultSafeMuted)
                }
            }
        }
    }
}

@Composable
private fun LiveSettings(title: String, url: String, onLock: () -> Unit, onSignOut: () -> Unit,
    onAddSite: () -> Unit, sites: List<AppSession>, onSwitch: (AppSession) -> Unit, isDemo: Boolean,
    session: AppSession?) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        item { AppCard {
            SectionTitle("Connected site", title)
            if (url.isNotBlank()) Text(url, color = AdultSafeMuted)
        } }
        item { AppCard {
            SectionTitle("Security", "Your session is stored encrypted on this device.")
            if (!isDemo) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onLock, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Lock, null); Text(" Lock app") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        } }
        if (!isDemo && session != null) item { DeviceManager(session, onSignOut) }
        item { AppCard {
            SectionTitle("Sites")
            Text("Connect another AdultSafe WordPress installation.", color = AdultSafeMuted)
            sites.filter { it.site.url != url }.forEach { s ->
                OutlinedButton(onClick = { onSwitch(s) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Switch to " + s.site.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAddSite, modifier = Modifier.fillMaxWidth()) { Text("Connect another site") }
        } }
    }
}

@Composable
private fun MessageCard(title: String, message: String) {
    AppCard { SectionTitle(title); Text(message, color = AdultSafeMuted) }
}
