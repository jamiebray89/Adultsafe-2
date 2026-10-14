package uk.co.skills4all.adultsafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.skills4all.adultsafe.model.ConnectedSite
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AdultSafeApi {
    suspend fun discoverSite(input: String): ConnectedSite? = withContext(Dispatchers.IO) {
        val base = input.trim().removeSuffix("/")
        runCatching {
            val siteJson = getText("$base/wp-json/adultsafe/v1/site")
            val healthJson = getText("$base/wp-json/adultsafe/v1/app/health")
            val name = extractString(siteJson, "site_name")
                ?: extractString(siteJson, "name")
                ?: URL(base).host
            val apiVersion = extractString(healthJson, "api_version")
                ?: extractString(siteJson, "api_version")
                ?: "unknown"
            val demoMode = extractBoolean(healthJson, "demo_mode") ?: false
            ConnectedSite(name = name, url = base, apiVersion = apiVersion, demoMode = demoMode)
        }.getOrNull()
    }

    suspend fun login(site: ConnectedSite, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val json = "{\"username\":\"${escape(username)}\",\"password\":\"${escape(password)}\"}"
            postJson("${site.url}/wp-json/adultsafe/v1/app/login", json)
            "token-placeholder-until-plugin-auth-is-live"
        }
    }

    suspend fun uploadIdEvidence(
        site: ConnectedSite,
        personId: String,
        token: String,
        filename: String,
        bytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Multipart upload wiring point. Kept central so camera/gallery/file sources can feed the same route.
            // Target endpoint: POST /people/{id}/id-evidence. No permanent evidence cache should remain on-device.
            require(bytes.isNotEmpty()) { "Evidence file is empty" }
            require(token.isNotBlank()) { "Missing app token" }
            "${site.url}/wp-json/adultsafe/v1/people/$personId/id-evidence:$filename"
        }
    }

    private fun getText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun postJson(url: String, json: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        OutputStreamWriter(connection.outputStream).use { it.write(json) }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun extractString(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val regex = Regex("\"$key\"\\s*:\\s*(true|false)")
        return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
