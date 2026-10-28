package uk.co.skills4all.adultsafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class LivePerson(
    val id: String,
    val name: String,
    val initials: String,
    val personType: String,
    val role: String,
    val sections: List<String>,
    val status: String,
    val expiry: String,
    val warnings: List<String>,
    val description: String,
    val demo: Boolean
)

data class LiveTile(val key: String, val title: String, val count: Int, val filter: String)
data class LiveAlert(val title: String, val message: String, val severity: String, val personId: String)
data class LiveHome(val people: List<LivePerson>, val tiles: List<LiveTile>, val alerts: List<LiveAlert>,
    val documentPersonIds: Set<String>)
data class LiveEvidence(val id: String, val name: String, val documentKey: String)
data class LiveDevice(val id: String, val name: String, val lastUsed: String, val revoked: Boolean)
data class LiveDocument(val key: String, val label: String, val state: String)

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
private fun JSONArray?.stringsList(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }

object LiveApi {
    private const val ROOT = "/wp-json/adultsafe/v1"
    private const val TIMEOUT = 15000

    private fun open(url: String, token: String, method: String): HttpURLConnection {
        require(token.isNotBlank()) { "You are not signed in." }
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = TIMEOUT
        con.readTimeout = TIMEOUT
        con.instanceFollowRedirects = false
        con.requestMethod = method
        con.setRequestProperty("X-AdultSafe-App-Token", token)
        con.setRequestProperty("Accept", "application/json")
        con.setRequestProperty("Cache-Control", "no-store")
        return con
    }

    private fun response(con: HttpURLConnection): JSONObject {
        val status = con.responseCode
        val reader = if (status in 200..299) con.inputStream else con.errorStream
        val body = reader?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val parsed = runCatching { JSONObject(body) }.getOrNull()
        if (status !in 200..299) {
            val message = parsed?.optString("message").orEmpty()
            throw AdultSafeConnectionException(when (status) {
                401, 403 -> "Your session has expired or your AdultSafe permissions have changed. Please sign in again."
                404 -> "This record or API endpoint was not found."
                501 -> "This action is not yet supported by the installed WordPress plugin."
                in 500..599 -> "The AdultSafe site encountered a server error (HTTP " + status + ")."
                else -> message.ifBlank { "Request failed (HTTP " + status + ")." }
            })
        }
        return parsed ?: throw AdultSafeConnectionException("The server returned an invalid JSON response.")
    }

    private fun request(base: String, token: String, path: String, method: String = "GET", data: JSONObject? = null): JSONObject {
        val con = open(base + ROOT + path, token, method)
        try {
            if (data != null) {
                con.doOutput = true
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                con.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
            }
            try { return response(con) } catch (e: AdultSafeConnectionException) {
                throw AdultSafeConnectionException("API " + path.replace(Regex("/people/[^/]+"), "/people/{id}") + ": " + (e.message ?: "Request failed"))
            }
        } finally { con.disconnect() }
    }

    private fun person(j: JSONObject) = LivePerson(
        id = j.optString("id"),
        name = j.optString("name", "Unknown"),
        initials = j.optString("initials").ifBlank { j.optString("name").take(2).uppercase() },
        personType = j.optString("person_type", "volunteer"),
        role = j.optString("role"),
        sections = j.optJSONArray("sections").stringsList(),
        status = j.optString("status"),
        expiry = j.optString("expiry_date"),
        warnings = j.optJSONArray("warnings").stringsList(),
        description = listOf(j.optString("line_1"), j.optString("line_2")).filter { it.isNotBlank() }.joinToString(" · "),
        demo = j.optBoolean("demo", false)
    )

    suspend fun load(base: String, token: String, allowDemoRecords: Boolean = false): LiveHome = withContext(Dispatchers.IO) {
        val peopleResponse = request(base, token, "/people")
        val peopleArray = peopleResponse.optJSONArray("people")
            ?: throw AdultSafeConnectionException("The WordPress people response is missing its records list.")
        val p = peopleArray.objects().map(::person).filter { allowDemoRecords || !it.demo }
        val d = request(base, token, "/dashboard")
        val tileArray = d.optJSONArray("tiles")
            ?: throw AdultSafeConnectionException("The WordPress dashboard response is missing its tiles.")
        val tiles = tileArray.objects().map {
            LiveTile(it.optString("key"), it.optString("label"), it.optInt("count"), it.optString("filter"))
        }.filter { it.count > 0 }
        val alertsJson = request(base, token, "/alerts")
        val alertsData = alertsJson.optJSONArray("items")
            ?: throw AdultSafeConnectionException("The WordPress alerts response is missing its items list.")
        val alerts = alertsData.objects().map {
            LiveAlert(it.optString("title"), it.optString("message"), it.optString("severity"), it.optString("person_id"))
        }
        val docJson = request(base, token, "/workspaces/default/document-acknowledgements")
        val docRows = docJson.optJSONArray("rows")
            ?: throw AdultSafeConnectionException("The document response is missing its records list.")
        val awaitingDocuments = docRows.objects().filter { row ->
            val states = row.optJSONObject("documents") ?: JSONObject()
            val keys = states.keys()
            var pending = false
            while (keys.hasNext()) {
                val state = states.optJSONObject(keys.next())?.optString("state").orEmpty()
                if (state == "required" || state == "followup" || state == "signing_pending") pending = true
            }
            pending
        }.map { it.optString("person_id") }.toSet()
        LiveHome(p, tiles, alerts, awaitingDocuments)
    }

    suspend fun filteredPeople(base: String, token: String, filter: String): List<LivePerson> = withContext(Dispatchers.IO) {
        require(filter.matches(Regex("[a-z0-9_-]+"))) { "Invalid filter" }
        val json = request(base, token, "/dashboard/tile/" + filter)
        val rows = json.optJSONArray("people")
            ?: throw AdultSafeConnectionException("The WordPress filter returned an invalid records list.")
        rows.objects().map(::person).filter { !it.demo }
    }

    suspend fun personDetail(base: String, token: String, id: String): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id)
    }
    suspend fun dbs(base: String, token: String, id: String): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/dbs")
    }
    suspend fun idCheck(base: String, token: String, id: String): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/id-check")
    }
    suspend fun evidence(base: String, token: String, id: String): List<LiveEvidence> = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/id-evidence").optJSONArray("evidence").objects().map {
            LiveEvidence(it.optString("id"), it.optString("original_name"), it.optString("document_key"))
        }
    }
    suspend fun documents(base: String, token: String, id: String): List<LiveDocument> = withContext(Dispatchers.IO) {
        val obj = request(base, token, "/people/" + id + "/documents")
        val states = obj.optJSONObject("states") ?: JSONObject()
        val keys = states.keys()
        val result = mutableListOf<LiveDocument>()
        while (keys.hasNext()) {
            val key = keys.next()
            val row = states.optJSONObject(key)
            result.add(LiveDocument(key, row?.optString("label").orEmpty().ifBlank { key }, row?.optString("state").orEmpty()))
        }
        result
    }
    suspend fun history(base: String, token: String, id: String): JSONArray = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/documents/history").optJSONArray("history") ?: JSONArray()
    }
    suspend fun metadata(base: String, token: String): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/settings/meta")
    }
    suspend fun updatePerson(base: String, token: String, id: String, values: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id, "PUT", values)
    }
    suspend fun createPerson(base: String, token: String, values: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people", "POST", values)
    }
    suspend fun updateDbs(base: String, token: String, id: String, values: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/dbs", "PUT", values)
    }
    suspend fun updateIdCheck(base: String, token: String, id: String, values: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/id-check", "PUT", values)
    }
    suspend fun updateRequirements(base: String, token: String, id: String, required: List<String>, notRequired: List<String>): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/documents/requirements", "PUT", JSONObject().put("required", JSONArray(required)).put("not_required", JSONArray(notRequired)))
    }
    suspend fun requestConfirmation(base: String, token: String, id: String, keys: List<String>): String = withContext(Dispatchers.IO) {
        val json = request(base, token, "/people/" + id + "/documents/request-confirmation", "POST", JSONObject().put("documents", JSONArray(keys)))
        json.optString("url")
    }
    suspend fun recordGiven(base: String, token: String, id: String, keys: List<String>, signature: String): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/people/" + id + "/documents/given", "POST", JSONObject().put("documents", JSONArray(keys)).put("signature", signature))
    }
    suspend fun logout(base: String, token: String) = withContext(Dispatchers.IO) {
        request(base, token, "/app/logout", "POST", JSONObject())
        Unit
    }
    suspend fun devices(base: String, token: String): List<LiveDevice> = withContext(Dispatchers.IO) {
        request(base, token, "/app/devices").optJSONArray("devices").objects().map {
            LiveDevice(it.optString("id"), it.optString("device_name"),
                it.optString("last_used"), it.optBoolean("revoked", false))
        }
    }
    suspend fun revokeDevice(base: String, token: String, deviceId: String): Unit = withContext(Dispatchers.IO) {
        require(deviceId.startsWith("dev_")) { "Invalid device identifier." }
        request(base, token, "/app/devices/revoke", "POST", JSONObject().put("device_id", deviceId))
        Unit
    }

    suspend fun validate(base: String, token: String): JSONObject = withContext(Dispatchers.IO) {
        request(base, token, "/app/me")
    }

    suspend fun upload(base: String, token: String, id: String, name: String, mime: String, bytes: ByteArray, documentKey: String): List<LiveEvidence> = withContext(Dispatchers.IO) {
        require(bytes.isNotEmpty() && bytes.size <= 15 * 1024 * 1024) { "Select a file no larger than 15 MB." }
        val extension = when (mime.lowercase()) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "application/pdf" -> ".pdf"
            else -> throw IllegalArgumentException("Please select a JPEG, PNG, WebP or PDF file.")
        }
        // WordPress validates both actual file data and the extension.
        val safeName = "adultsafe_evidence" + extension
        val boundary = "AdultSafe-" + UUID.randomUUID().toString()
        val con = open(base + ROOT + "/people/" + id + "/id-evidence", token, "POST")
        try {
            con.doOutput = true
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
            val newline = "\r\n"
            val output = ByteArrayOutputStream()
            fun append(s: String) { output.write(s.toByteArray(Charsets.UTF_8)) }
            append("--" + boundary + newline + "Content-Disposition: form-data; name=\"document_key\"" + newline + newline + documentKey + newline)
            append("--" + boundary + newline + "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"" + newline)
            append("Content-Type: " + mime + newline + newline)
            output.write(bytes)
            append(newline + "--" + boundary + "--" + newline)
            con.outputStream.use { it.write(output.toByteArray()) }
            val received = response(con).optJSONArray("evidence")
                ?: throw AdultSafeConnectionException("The server did not confirm the evidence upload.")
            val evidence = received.objects().map {
                LiveEvidence(it.optString("id"), it.optString("original_name"), it.optString("document_key"))
            }
            if (evidence.none { it.documentKey == documentKey && it.id.isNotBlank() }) {
                throw AdultSafeConnectionException("The server did not confirm that the new evidence was stored.")
            }
            evidence
        } finally { con.disconnect() }
    }
}
