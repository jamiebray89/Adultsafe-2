package uk.co.skills4all.adultsafe.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import uk.co.skills4all.adultsafe.api.AppSession
import uk.co.skills4all.adultsafe.api.LiveApi

/** Uses per-site role, person-type and section options, never hard-coded roles. */
@Composable
fun NewPersonDialog(session: AppSession, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var personType by remember { mutableStateOf("volunteer") }
    var childYear by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    val sections = remember { mutableStateListOf<String>() }
    var metadata by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session.site.url) {
        try { metadata = LiveApi.metadata(session.site.url, session.token) }
        catch (e: Exception) { error = e.message ?: "Unable to load site settings." }
        finally { loading = false }
    }

    @Composable
    fun Options(source: JSONObject?, selected: String, onSelect: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val keys = remember(source?.toString()) {
            source?.keys()?.asSequence()?.toList().orEmpty().filter { it.isNotEmpty() }
        }
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = keys.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(source?.optString(selected).orEmpty().ifBlank { "Select" })
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                keys.forEach { key ->
                    DropdownMenuItem(text = { Text(source?.optString(key).orEmpty()) }, onClick = {
                        onSelect(key)
                        expanded = false
                    })
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Add person") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Email (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Person type")
                Options(metadata?.optJSONObject("person_types"), personType, { personType = it })
                if (personType == "parent_carer") OutlinedTextField(childYear, { childYear = it },
                    label = { Text("Child's birth year only") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Role")
                Options(metadata?.optJSONObject("roles"), role, { role = it })
                Text("Sections")
                val options = metadata?.optJSONObject("sections")
                if (options != null) {
                    val keys = options.keys().asSequence().toList()
                    keys.forEach { key ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = sections.contains(key), onCheckedChange = { checked ->
                                if (checked) sections.add(key) else sections.remove(key)
                            })
                            Text(options.optString(key))
                        }
                    }
                }
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                saving = true
                error = null
                scope.launch {
                    try {
                        val request = JSONObject().put("name", name.trim()).put("email", email.trim())
                            .put("person_type", personType).put("role", role).put("child_year_of_birth", childYear.trim())
                            .put("sections", JSONArray(sections.toList()))
                        LiveApi.createPerson(session.site.url, session.token, request)
                        onCreated()
                    } catch (e: Exception) { error = e.message ?: "Could not create person." }
                    finally { saving = false }
                }
            }, enabled = !saving && !loading && metadata != null && name.trim().isNotEmpty() && role.isNotBlank() && (childYear.isBlank() || childYear.matches(Regex("(19|20)[0-9]{2}")))) {
                Text(if (saving) "Saving…" else "Save person")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}
