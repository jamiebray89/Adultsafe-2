package uk.co.skills4all.adultsafe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uk.co.skills4all.adultsafe.api.AdultSafeApi
import uk.co.skills4all.adultsafe.data.DemoRepository
import uk.co.skills4all.adultsafe.model.AdultSafeAlert
import uk.co.skills4all.adultsafe.model.AppMode
import uk.co.skills4all.adultsafe.model.AppScreen
import uk.co.skills4all.adultsafe.model.ConnectedSite
import uk.co.skills4all.adultsafe.model.DashboardTile
import uk.co.skills4all.adultsafe.model.DbsStatus
import uk.co.skills4all.adultsafe.model.DocumentRequirement
import uk.co.skills4all.adultsafe.model.DocumentState
import uk.co.skills4all.adultsafe.model.PersonFilter
import uk.co.skills4all.adultsafe.model.PersonSummary
import uk.co.skills4all.adultsafe.model.TileTone
import uk.co.skills4all.adultsafe.ui.components.AppCard
import uk.co.skills4all.adultsafe.ui.components.BrandHero
import uk.co.skills4all.adultsafe.ui.components.Logo
import uk.co.skills4all.adultsafe.ui.components.PrimaryAction
import uk.co.skills4all.adultsafe.ui.components.SecondaryAction
import uk.co.skills4all.adultsafe.ui.components.SectionTitle
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeBackground
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeMuted
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeNavy
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeText

@Composable
fun AdultSafeRoot() {
    var mode by remember { mutableStateOf(AppMode.Disconnected) }
    var activeSite by remember { mutableStateOf<ConnectedSite?>(null) }
    var connectedSites by remember { mutableStateOf<List<ConnectedSite>>(emptyList()) }
    var selectedScreen by remember { mutableStateOf(AppScreen.Dashboard) }
    var selectedFilter by remember { mutableStateOf(PersonFilter.All) }
    var selectedPerson by remember { mutableStateOf<PersonSummary?>(null) }

    when (mode) {
        AppMode.Disconnected, AppMode.Connecting, AppMode.Error -> ConnectScreen(
            mode = mode,
            onUseDemo = {
                activeSite = DemoRepository.demoSite
                connectedSites = listOf(DemoRepository.demoSite)
                mode = AppMode.ConnectedDemo
            },
            onConnected = { site ->
                activeSite = site
                connectedSites = connectedSites + site
                mode = if (site.demoMode) AppMode.ConnectedDemo else AppMode.ConnectedLive
            },
            onMode = { mode = it }
        )
        AppMode.Locked -> LockScreen(activeSite, onUnlock = { mode = if (activeSite?.demoMode == true) AppMode.ConnectedDemo else AppMode.ConnectedLive })
        AppMode.ConnectedDemo, AppMode.ConnectedLive -> AdultSafeHome(
            activeSite = activeSite ?: DemoRepository.demoSite,
            connectedSites = connectedSites,
            selectedScreen = selectedScreen,
            selectedFilter = selectedFilter,
            selectedPerson = selectedPerson,
            onScreen = {
                selectedPerson = null
                selectedScreen = it
            },
            onFilter = {
                selectedFilter = it
                selectedPerson = null
                selectedScreen = AppScreen.People
            },
            onPerson = { selectedPerson = it },
            onBack = { selectedPerson = null },
            onLock = { mode = AppMode.Locked },
            onLogout = {
                activeSite = null
                connectedSites = emptyList()
                selectedPerson = null
                selectedScreen = AppScreen.Dashboard
                mode = AppMode.Disconnected
            },
            onAddSite = { mode = AppMode.Disconnected },
            onSwitchSite = { activeSite = it }
        )
    }
}

@Composable
private fun ConnectScreen(mode: AppMode, onUseDemo: () -> Unit, onConnected: (ConnectedSite) -> Unit, onMode: (AppMode) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    var message by remember { mutableStateOf("Enter your AdultSafe site address or use demo mode.") }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = AdultSafeBackground) {
        LazyColumn(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            item {
                BrandHero("AdultSafe", "Connect your AdultSafe Records site.")
                Spacer(Modifier.height(18.dp))
                AppCard {
                    SectionTitle("Connect site", "URL, QR invite and app-token login are V1-critical.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Site address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    PrimaryAction(
                        label = if (mode == AppMode.Connecting) "Checking…" else "Connect",
                        onClick = {
                            onMode(AppMode.Connecting)
                            message = "Checking AdultSafe API…"
                            scope.launch {
                                val site = AdultSafeApi.discoverSite(url)
                                if (site == null) {
                                    message = "Could not confirm AdultSafe on that site."
                                    onMode(AppMode.Error)
                                } else {
                                    onConnected(site)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = mode != AppMode.Connecting && url.startsWith("https://") && url.length > 12
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction("Use demo app data", onUseDemo, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    SecondaryAction("Scan QR invite", { message = "QR scanner track is reserved for V1 before field testing." }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = AdultSafeMuted)
                }
            }
        }
    }
}

@Composable
private fun AdultSafeHome(activeSite: ConnectedSite, connectedSites: List<ConnectedSite>, selectedScreen: AppScreen, selectedFilter: PersonFilter, selectedPerson: PersonSummary?, onScreen: (AppScreen) -> Unit, onFilter: (PersonFilter) -> Unit, onPerson: (PersonSummary) -> Unit, onBack: () -> Unit, onLock: () -> Unit, onLogout: () -> Unit, onAddSite: () -> Unit, onSwitchSite: (ConnectedSite) -> Unit) {
    val people = DemoRepository.people()
    val siteMode = if (activeSite.demoMode) "Demo mode" else "Live site"
    Column(Modifier.fillMaxSize().background(AdultSafeBackground)) {
        AppCard(Modifier.padding(16.dp), contentPadding = 14) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(size = 54)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("AdultSafe", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AdultSafeNavy)
                    Text("${activeSite.name} · $siteMode", color = AdultSafeMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryAction("Lock", onLock, Modifier.weight(1f))
                SecondaryAction("Add site", onAddSite, Modifier.weight(1f))
                SecondaryAction("Logout", onLogout, Modifier.weight(1f))
            }
        }
        NavRow(selectedScreen, onScreen)
        Box(Modifier.weight(1f)) {
            if (selectedPerson != null) {
                PersonProfile(selectedPerson, onBack)
            } else {
                when (selectedScreen) {
                    AppScreen.Dashboard -> DashboardScreen(onFilter)
                    AppScreen.People -> PeopleScreen(selectedFilter, people, onFilter, onPerson)
                    AppScreen.Alerts -> AlertsScreen(people, onPerson)
                    AppScreen.Documents -> DocumentsOverview(people, onPerson)
                    AppScreen.Settings -> SettingsScreen(activeSite, connectedSites, onSwitchSite, onAddSite, onLock, onLogout)
                }
            }
        }
    }
}

@Composable
private fun NavRow(selectedScreen: AppScreen, onScreen: (AppScreen) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AppScreen.values().forEach { screen ->
            Button(onClick = { onScreen(screen) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (screen == selectedScreen) AdultSafeNavy else Color.White, contentColor = if (screen == selectedScreen) Color.White else AdultSafeNavy)) { Text(screen.name, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun DashboardScreen(onFilter: (PersonFilter) -> Unit) {
    val tiles = DemoRepository.dashboardTiles()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Dashboard", "Action tiles hide when the real API returns zero counts.") }
        tiles.groupBy { it.group }.forEach { (group, groupTiles) ->
            item { Text(group, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AdultSafeText) }
            items(groupTiles) { tile -> DashboardTileCard(tile) { tile.filter?.let(onFilter) } }
        }
    }
}

@Composable
private fun DashboardTileCard(tile: DashboardTile, onClick: () -> Unit) {
    val bg = when (tile.tone) {
        TileTone.Good -> Color(0xFFE8F8F2)
        TileTone.Attention -> Color(0xFFFFF7ED)
        TileTone.Warning -> Color(0xFFFFEDD5)
        TileTone.Danger -> Color(0xFFFFE4E6)
    }
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(tile.title, fontWeight = FontWeight.Bold, color = AdultSafeText)
            Text(tile.count.toString(), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = AdultSafeText)
        }
    }
}

@Composable
private fun PeopleScreen(filter: PersonFilter, people: List<PersonSummary>, onFilter: (PersonFilter) -> Unit, onPerson: (PersonSummary) -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = DemoRepository.filterPeople(filter, people).filter { it.name.contains(search, ignoreCase = true) || it.role.contains(search, ignoreCase = true) }.sortedBy { it.name.substringAfterLast(" ") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("People", "Active filter: ${filter.name}") }
        item { OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search people") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(PersonFilter.All, PersonFilter.NeedsAttention, PersonFilter.Documents).forEach { f -> SecondaryAction(f.name, { onFilter(f) }, Modifier.weight(1f)) }
            }
        }
        if (filtered.isEmpty()) item { EmptyState("No matching people", "Clear the filter or search term.") }
        items(filtered) { person -> PersonCard(person, onClick = { onPerson(person) }) }
    }
}

@Composable
private fun PersonCard(person: PersonSummary, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(AdultSafeNavy), contentAlignment = Alignment.Center) { Text(person.initials, color = Color.White, fontWeight = FontWeight.Bold) }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(person.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AdultSafeText)
                Text(person.role, color = AdultSafeMuted)
                Text(person.warning.ifBlank { dbsLabel(person.dbsStatus) }, color = AdultSafeNavy, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PersonProfile(person: PersonSummary, onBack: () -> Unit) {
    var tab by remember { mutableStateOf("Details") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SecondaryAction("Back", onBack) }
        item { PersonCard(person, onClick = {}) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Details", "DBS / ID", "Documents", "History").forEach { name -> Button(onClick = { tab = name }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tab == name) AdultSafeNavy else Color.White, contentColor = if (tab == name) Color.White else AdultSafeNavy)) { Text(name, fontSize = 11.sp) } }
            }
        }
        item {
            when (tab) {
                "DBS / ID" -> DbsIdPanel(person)
                "Documents" -> DocumentsPanel(person)
                "History" -> HistoryPanel(person)
                else -> DetailsPanel(person)
            }
        }
    }
}

@Composable
private fun DetailsPanel(person: PersonSummary) = AppCard {
    SectionTitle("Details", "Role, section, person type and email.")
    Text("Email: ${person.email ?: "Missing"}", color = AdultSafeMuted)
    Text("Status: ${dbsLabel(person.dbsStatus)}", color = AdultSafeMuted)
}

@Composable
private fun DbsIdPanel(person: PersonSummary) = AppCard {
    SectionTitle("DBS / ID", "Three evidence items, checker details and result dates.")
    Text("DBS status: ${dbsLabel(person.dbsStatus)}", color = AdultSafeText, fontWeight = FontWeight.SemiBold)
    Text("DBS expiry: ${person.dbsExpiry ?: "Not set"}", color = AdultSafeMuted)
    Spacer(Modifier.height(12.dp))
    person.evidence.forEach { item -> EvidenceRow(item.label, item.present, item.source) }
    if (person.evidenceCount < 3) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Override reason if fewer than 3 IDs") }, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SecondaryAction("Camera", {}, Modifier.weight(1f))
        SecondaryAction("Gallery", {}, Modifier.weight(1f))
        SecondaryAction("File", {}, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    PrimaryAction("Save ID check", {}, Modifier.fillMaxWidth(), enabled = person.evidenceCount >= 3)
}

@Composable
private fun EvidenceRow(label: String, present: Boolean, source: String) {
    Text("${if (present) "✓" else "○"} $label — $source", color = if (present) AdultSafeText else AdultSafeMuted)
}

@Composable
private fun DocumentsPanel(person: PersonSummary) = AppCard {
    SectionTitle("Documents", "Requirements first, then documents given or confirmation link.")
    person.documents.forEach { DocumentRow(it) }
    Spacer(Modifier.height(10.dp))
    PrimaryAction("Record documents given", {}, Modifier.fillMaxWidth(), enabled = person.documents.any { it.state == DocumentState.Outstanding })
    Spacer(Modifier.height(8.dp))
    SecondaryAction("Request confirmation", {}, Modifier.fillMaxWidth(), enabled = person.documents.any { it.state == DocumentState.Outstanding || it.state == DocumentState.SigningLinkPending })
}

@Composable
private fun DocumentRow(document: DocumentRequirement) {
    Text("${document.name}: ${document.state.name.replace('_', ' ')}", color = AdultSafeText)
    if (!document.signingLink.isNullOrBlank()) Text("Link pending", color = AdultSafeMuted)
}

@Composable
private fun HistoryPanel(person: PersonSummary) = AppCard {
    SectionTitle("History", "Audit events from the plugin will appear here.")
    person.history.forEach { Text("• $it", color = AdultSafeMuted) }
}

@Composable
private fun AlertsScreen(people: List<PersonSummary>, onPerson: (PersonSummary) -> Unit) {
    val alerts = DemoRepository.alerts()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Alerts", "Day-to-day actions needing attention.") }
        items(alerts) { alert -> AlertCard(alert) { alert.personId?.let { id -> people.firstOrNull { it.id == id }?.let(onPerson) } } }
    }
}

@Composable
private fun AlertCard(alert: AdultSafeAlert, onClick: () -> Unit) = DashboardTileCard(
    DashboardTile(slug = "alert-${alert.title.lowercase().replace(" ", "-")}", title = alert.title, count = 1, group = "Alerts", tone = alert.tone),
    onClick
)

@Composable
private fun DocumentsOverview(people: List<PersonSummary>, onPerson: (PersonSummary) -> Unit) {
    val documentPeople = people.filter { person -> person.documents.any { it.state == DocumentState.Outstanding || it.state == DocumentState.FollowUpNeeded || it.state == DocumentState.SigningLinkPending } }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Documents", "Outstanding, follow-up and signing-link records.") }
        items(documentPeople) { PersonCard(it, onClick = { onPerson(it) }) }
    }
}

@Composable
private fun SettingsScreen(activeSite: ConnectedSite, connectedSites: List<ConnectedSite>, onSwitchSite: (ConnectedSite) -> Unit, onAddSite: () -> Unit, onLock: () -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Settings", "Site, security and V1-critical capability tracks.") }
        item { AppCard { SectionTitle("Connected site", activeSite.url); Text("API: ${activeSite.apiVersion}", color = AdultSafeMuted) } }
        item { AppCard { SectionTitle("Security", "Persistent login, visible logout and local unlock."); PrimaryAction("Lock app now", onLock, Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); SecondaryAction("Logout", onLogout, Modifier.fillMaxWidth()) } }
        item { AppCard { SectionTitle("Connected sites", "Switching stays per self-hosted site."); connectedSites.forEach { site -> SecondaryAction(site.name, { onSwitchSite(site) }, Modifier.fillMaxWidth()) }; Spacer(Modifier.height(8.dp)); SecondaryAction("Add another site", onAddSite, Modifier.fillMaxWidth()) } }
        item { AppCard { SectionTitle("V1 critical tracks", "These are required before the first serious field APK."); DemoRepository.capabilityTracks().forEach { Text("• ${it.name}: ${it.status}", color = AdultSafeText); Text(it.note, color = AdultSafeMuted); Spacer(Modifier.height(6.dp)) } } }
    }
}

@Composable
private fun LockScreen(site: ConnectedSite?, onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = AdultSafeBackground) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(size = 96)
            Spacer(Modifier.height(16.dp))
            Text("AdultSafe locked", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AdultSafeNavy)
            Text(site?.name ?: "No active site", color = AdultSafeMuted)
            Spacer(Modifier.height(20.dp))
            PrimaryAction("Unlock for testing", onUnlock)
            Spacer(Modifier.height(8.dp))
            Text("Biometric prompt and PIN fallback are V1-critical before field testing.", color = AdultSafeMuted)
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) = AppCard { SectionTitle(title, body) }

private fun dbsLabel(status: DbsStatus): String = when (status) {
    DbsStatus.AwaitingProfileCompletion -> "Awaiting profile completion"
    DbsStatus.DbsRequested -> "DBS requested"
    DbsStatus.AwaitingIdCheck -> "Awaiting ID check"
    DbsStatus.ResultReturned -> "Result returned"
}
