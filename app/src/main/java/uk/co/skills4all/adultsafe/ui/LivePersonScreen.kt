package uk.co.skills4all.adultsafe.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import uk.co.skills4all.adultsafe.api.*
import uk.co.skills4all.adultsafe.ui.components.AppCard
import uk.co.skills4all.adultsafe.ui.components.SectionTitle
import uk.co.skills4all.adultsafe.ui.theme.AdultSafeMuted
import java.io.ByteArrayOutputStream

@Composable
fun LivePersonScreen(person: LivePerson, session: AppSession, onBack: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val url = session.site.url
    val token = session.token
    var page by remember(person.id) { mutableStateOf("Details") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var details by remember(person.id) { mutableStateOf<JSONObject?>(null) }
    var dbs by remember(person.id) { mutableStateOf<JSONObject?>(null) }
    var idCheck by remember(person.id) { mutableStateOf<JSONObject?>(null) }
    var documents by remember(person.id) { mutableStateOf<List<LiveDocument>>(emptyList()) }
    var evidence by remember(person.id) { mutableStateOf<List<LiveEvidence>>(emptyList()) }
    var history by remember(person.id) { mutableStateOf<JSONArray?>(null) }

    val canId = session.permissions.contains("id.upload")
    val canSign = session.permissions.contains("documents.manage")
    val canRequest = session.permissions.contains("documents.confirmation.request")
    val canViewHistory = session.permissions.contains("history.read")

    LaunchedEffect(person.id, reload) {
        busy = true
        error = null
        try {
            details = LiveApi.personDetail(url, token, person.id)
            dbs = LiveApi.dbs(url, token, person.id)
            idCheck = LiveApi.idCheck(url, token, person.id)
            documents = LiveApi.documents(url, token, person.id)
            if (canId) evidence = LiveApi.evidence(url, token, person.id)
            if (canViewHistory) history = LiveApi.history(url, token, person.id)
        } catch (e: Exception) { error = e.message ?: "Unable to load this person's records." }
        finally { busy = false }
    }

    fun update(task: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        message = null
        scope.launch {
            try { task(); message = "Changes saved"; reload++; onChanged() }
            catch (e: Exception) { error = e.message ?: "Unable to save. No changes were confirmed." }
            finally { busy = false }
        }
    }

    var showSignature by remember { mutableStateOf(false) }
    var signingKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var sharedLink by remember { mutableStateOf("") }

    if (showSignature) SignatureDialog(
        onDismiss = { showSignature = false },
        onCaptured = { png ->
            showSignature = false
            update { LiveApi.recordGiven(url, token, person.id, signingKeys, png) }
        }
    )
    if (sharedLink.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { sharedLink = "" },
            title = { Text("Confirmation link created") },
            text = { Text("This link is ready to send using your normal communication channel.\n\n" + sharedLink) },
            confirmButton = {
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("AdultSafe signing link", sharedLink))
                    sharedLink = ""
                }) { Text("Copy link") }
            },
            dismissButton = { TextButton(onClick = { sharedLink = "" }) { Text("Close") } }
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("← Back to people") } }
        item { Text(person.name, fontSize = 24.sp, color = uk.co.skills4all.adultsafe.ui.theme.AdultSafeNavy) }
        item { Text(person.personType.replaceFirstChar { it.uppercase() } + " · " + person.role, color = AdultSafeMuted) }
        item {
            androidx.compose.foundation.horizontalScroll(rememberScrollState()).let { scrollModifier ->
                Row(scrollModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Details", "DBS / ID", "Documents", "History").forEach { tab ->
                        FilterChip(selected = page == tab, onClick = { page = tab }, label = { Text(tab) })
                    }
                }
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error != null) item { AppCard {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { reload++ }) { Text("Retry") }
        } }
        if (message != null) item { Text(message.orEmpty(), color = Color(0xFF15634E)) }
        when(page) {
            "Details" -> item {
                val personDetail = details
                if (personDetail != null) {
                    PersonEditCard(person, personDetail, busy, onSave = { values ->
                        update { LiveApi.updatePerson(url, token, person.id, values) }
                    })
                }
            }
            "DBS / ID" -> {
                item {
                    val record = dbs
                    if (record != null) DbsEditCard(record, busy, onSave = { changes ->
                        update { LiveApi.updateDbs(url, token, person.id, changes) }
                    })
                }
                item {
                    val record = idCheck
                    if (record != null) IdEvidenceCard(
                        record, evidence, busy, canId, context,
                        onUpload = { name, mime, bytes, key ->
                            update { LiveApi.upload(url, token, person.id, name, mime, bytes, key) }
                        },
                        onCheck = { values -> update { LiveApi.updateIdCheck(url, token, person.id, values) } }
                    )
                }
            }
            "Documents" -> item {
                DocumentsEditCard(
                    documents = documents, busy = busy, canSign = canSign, canRequest = canRequest,
                    onRequirements = { required, notRequired ->
                        update { LiveApi.updateRequirements(url, token, person.id, required, notRequired) }
                    },
                    onSign = { keys -> signingKeys = keys; showSignature = true },
                    onRequest = { keys ->
                        update {
                            sharedLink = LiveApi.requestConfirmation(url, token, person.id, keys)
                        }
                    }
                )
            }
            "History" -> item { AppCard {
                SectionTitle("Document history")
                val rows = history
                if (!canViewHistory) Text("Your account cannot view document history.")
                else if (rows == null || rows.length() == 0) Text("No recorded document handovers yet.")
                else (0 until rows.length()).forEach { idx ->
                    val row = rows.optJSONObject(idx) ?: JSONObject()
                    Text(row.optString("event", "Document event").replace("_", " "), color = Color(0xFF123D71))
                    Text(row.optString("signed_at"), color = AdultSafeMuted)
                    // Never print signature data or untrusted HTML into the UI.
                    HorizontalDivider()
                }
            } }
        }
    }
}

@Composable
private fun PersonEditCard(person: LivePerson, details: JSONObject, busy: Boolean, onSave: (JSONObject) -> Unit) {
    var name by remember(person.id) { mutableStateOf(person.name) }
    var role by remember(person.id) { mutableStateOf(person.role) }
    var membership by remember(person.id) { mutableStateOf(details.optString("membership_number")) }
    var personType by remember(person.id) { mutableStateOf(details.optString("person_type", person.personType)) }
    AppCard {
        SectionTitle("Person details", "Changes are saved to this AdultSafe site.")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(role, { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(membership, { membership = it }, label = { Text("Membership number") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(personType == "volunteer", onClick = { personType = "volunteer" }, label = { Text("Volunteer") })
            FilterChip(personType != "volunteer", onClick = { personType = "parent_carer" }, label = { Text("Parent/carer") })
        }
        Text("Sections: " + person.sections.joinToString(", ").ifBlank { "None" }, color = AdultSafeMuted)
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            onSave(JSONObject().put("name", name.trim()).put("role", role.trim())
                .put("membership_number", membership.trim()).put("person_type", personType))
        }, enabled = !busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save details") }
    }
}

@Composable
private fun DbsEditCard(record: JSONObject, busy: Boolean, onSave: (JSONObject) -> Unit) {
    var requested by remember(record.toString()) { mutableStateOf(record.optString("requested_date")) }
    var submitted by remember(record.toString()) { mutableStateOf(record.optString("submitted_date")) }
    var result by remember(record.toString()) { mutableStateOf(record.optString("result_received_date")) }
    var expiry by remember(record.toString()) { mutableStateOf(record.optString("expiry_date")) }
    AppCard {
        SectionTitle("DBS record", "Use YYYY-MM-DD dates; leave expiry blank to use site defaults after a result.")
        Text("Current status: " + record.optString("status").replace("_", " "), color = AdultSafeMuted)
        OutlinedTextField(requested, { requested = it }, label = { Text("DBS requested") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(submitted, { submitted = it }, label = { Text("Submitted date") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(result, { result = it }, label = { Text("Result received") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(expiry, { expiry = it }, label = { Text("Expiry date") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            onSave(JSONObject().put("requested_date", requested).put("submitted_date", submitted)
                .put("result_received_date", result).put("expiry_date", expiry))
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save DBS dates") }
    }
}

@Composable
private fun IdEvidenceCard(
    record: JSONObject, evidence: List<LiveEvidence>, busy: Boolean, canId: Boolean, context: Context,
    onUpload: (String, String, ByteArray, String) -> Unit, onCheck: (JSONObject) -> Unit
) {
    var checker by remember(record.toString()) { mutableStateOf(record.optString("checker_name")) }
    var membership by remember(record.toString()) { mutableStateOf(record.optString("checker_membership_number")) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    fun handleBytes(mime: String, bytes: ByteArray) {
        if (bytes.isEmpty() || bytes.size > 15*1024*1024) { uploadError = "Please select a file up to 15 MB."; return }
        val next = (evidence.size + 1).coerceAtMost(10)
        onUpload("ID evidence " + next, mime, bytes, "id_document_" + next)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                require(type.startsWith("image/") || type == "application/pdf") { "Select an image or PDF." }
                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    while (true) {
                        val read = stream.read(buf)
                        if (read < 0) break
                        if (out.size() + read > 15*1024*1024) throw IllegalArgumentException("File exceeds 15 MB.")
                        out.write(buf, 0, read)
                    }
                    out.toByteArray()
                } ?: throw IllegalArgumentException("Could not open file.")
                handleBytes(type, bytes)
            } catch (e: Exception) { uploadError = e.message ?: "Could not open the selected file." }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            handleBytes("image/jpeg", out.toByteArray())
            bitmap.recycle()
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null) else uploadError = "Camera permission is needed to take an ID photo."
    }

    AppCard {
        SectionTitle("ID evidence", "ID photos upload directly to the site's private evidence store.")
        Text(evidence.size.toString() + " uploaded · minimum " + record.optInt("minimum_evidence", 3), color = AdultSafeMuted)
        evidence.forEach { e -> Text("✓ " + e.name.ifBlank { e.documentKey }) }
        if (uploadError != null) Text(uploadError.orEmpty(), color = MaterialTheme.colorScheme.error)
        if (canId) {
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    camera.launch(null)
                else cameraPermission.launch(Manifest.permission.CAMERA)
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Take ID photo") }
            OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*", "application/pdf")) },
                enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose photo or file") }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("ID checker")
        OutlinedTextField(checker, { checker = it }, label = { Text("Checker's name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(membership, { membership = it }, label = { Text("Checker membership number") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Button(onClick = { onCheck(JSONObject().put("checker_name", checker)
                .put("checker_membership_number", membership).put("confirm_checked_now", true)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canId && !busy && evidence.size >= record.optInt("minimum_evidence", 3)
                && checker.isNotBlank() && membership.isNotBlank()) {
            Text("Complete ID check")
        }
        if (!canId) Text("Your account does not have ID upload permission.", color = AdultSafeMuted)
    }
}

@Composable
private fun DocumentsEditCard(
    documents: List<LiveDocument>, busy: Boolean, canSign: Boolean, canRequest: Boolean,
    onRequirements: (List<String>, List<String>) -> Unit, onSign: (List<String>) -> Unit,
    onRequest: (List<String>) -> Unit
) {
    val selected = remember(documents) { mutableStateListOf<String>() }
    val notRequired = remember(documents) {
        mutableStateListOf<String>().also { it.addAll(documents.filter { d -> d.state == "not_required" }.map { d -> d.key }) }
    }
    val outstanding = documents.filter { it.state == "required" || it.state == "followup" }
    AppCard {
        SectionTitle("Documents", "Select outstanding documents to record a signed handover or request confirmation.")
        if (documents.isEmpty()) Text("No document requirements are configured.", color = AdultSafeMuted)
        documents.forEach { doc ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (doc.state == "required" || doc.state == "followup") {
                    Checkbox(checked = doc.key in selected, onCheckedChange = { yes ->
                        if (yes) selected.add(doc.key) else selected.remove(doc.key)
                    })
                }
                Column(Modifier.weight(1f)) {
                    Text(doc.label)
                    Text(doc.state.replace("_", " "), color = AdultSafeMuted, fontSize = 12.sp)
                }
                if (doc.state != "signed") {
                    Checkbox(checked = doc.key in notRequired, onCheckedChange = { yes ->
                        if (yes) notRequired.add(doc.key) else notRequired.remove(doc.key)
                    })
                    Text("N/R", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            onRequirements(documents.filter { it.key !in notRequired }.map { it.key }, notRequired.toList())
        }, enabled = !busy && canSign, modifier = Modifier.fillMaxWidth()) { Text("Save requirements") }
        Button(onClick = { onSign(selected.toList()) }, enabled = !busy && canSign && selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()) { Text("Record documents given & sign") }
        OutlinedButton(onClick = { onRequest(selected.toList()) }, enabled = !busy && canRequest && selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()) { Text("Create confirmation link") }
        if (!canSign) Text("Document recording is not allowed for your account.", color = AdultSafeMuted)
        if (outstanding.isEmpty()) Text("No documents awaiting handover.", color = AdultSafeMuted)
    }
}

@Composable
private fun SignatureDialog(onDismiss: () -> Unit, onCaptured: (String) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var size by remember { mutableStateOf(IntSize(0, 0)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Volunteer / parent acknowledgement") },
        text = {
            Column {
                Text("The person receiving the documents must sign below. Confirm they have received the selected items.")
                Spacer(Modifier.height(12.dp))
                Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                    Canvas(
                        Modifier.fillMaxWidth().height(180.dp)
                            .onSizeChanged { size = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset -> current = listOf(offset) },
                                    onDragEnd = {
                                        if (current.isNotEmpty()) strokes.add(current)
                                        current = emptyList()
                                    },
                                    onDragCancel = { current = emptyList() },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        current = current + change.position
                                    }
                                )
                            }
                    ) {
                        (strokes + listOf(current)).filter { it.isNotEmpty() }.forEach { line ->
                            val path = Path()
                            path.moveTo(line.first().x, line.first().y)
                            line.drop(1).forEach { p -> path.lineTo(p.x, p.y) }
                            drawPath(path, color = Color(0xFF123D71), style = Stroke(width = 4f))
                        }
                    }
                }
                TextButton(onClick = { strokes.clear(); current = emptyList() }) { Text("Clear signature") }
            }
        },
        confirmButton = {
            Button(enabled = strokes.isNotEmpty() && size.width > 0 && size.height > 0, onClick = {
                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(18, 61, 113)
                    strokeWidth = 4f
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                }
                strokes.forEach { line ->
                    if (line.isNotEmpty()) {
                        val path = android.graphics.Path()
                        path.moveTo(line.first().x, line.first().y)
                        line.drop(1).forEach { path.lineTo(it.x, it.y) }
                        canvas.drawPath(path, paint)
                    }
                }
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                bitmap.recycle()
                onCaptured("data:image/png;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP))
            }) { Text("Save signed handover") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
