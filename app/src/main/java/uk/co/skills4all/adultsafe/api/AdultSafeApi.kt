package uk.co.skills4all.adultsafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import uk.co.skills4all.adultsafe.BuildConfig
import uk.co.skills4all.adultsafe.model.ConnectedSite
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class AppSession(
    val site: ConnectedSite,
    val token: String,
    val userId: String,
    val userName: String,
    val roles: List<String>,
    val permissions: List<String>
)

class AdultSafeConnectionException(message: String) : Exception(message)

object AdultSafeApi {
    private const val API = "/wp-json/adultsafe/v1"
    private const val TIMEOUT_MS = 10000

    fun normaliseSiteUrl(input: String): String {
        val text = input.trim().trimEnd('/')
        val uri = try { URI(text) } catch (_: Exception) { null }
        if (uri?.scheme?.lowercase() != "https" || uri.host.isNullOrBlank()
            || uri.userInfo != null || uri.query != null || uri.fragment != null
            || uri.port !in -1..65535
        ) throw AdultSafeConnectionException("Enter a valid HTTPS site URL.")
        return uri.toASCIIString().trimEnd('/')
    }

    suspend fun discoverSite(input: String): ConnectedSite = withContext(Dispatchers.IO) {
        val base = normaliseSiteUrl(input)
        val site = getObject("$base$API/site")
        val health = getObject("$base$API/app/health")
        val info = site.optJSONObject("data") ?: site
        val status = health.optJSONObject("data") ?: health
        ConnectedSite(
            name = info.optString("site_name").ifBlank {
                info.optString("name").ifBlank { URI(base).host }
            },
            url = base,
            apiVersion = status.optString("api_version").ifBlank {
                info.optString("api_version").ifBlank { "unknown" }
            },
            demoMode = status.optBoolean("demo_mode", false)
        )
    }

    suspend fun login(input: String, username: String, password: String): AppSession =
        withContext(Dispatchers.IO) {
            if (username.isBlank() || password.isBlank())
                throw AdultSafeConnectionException("Enter your email or username and password.")
            val site = discoverSite(input)
            val body = JSONObject()
                .put("username", username.trim())
                .put("password", password)
                .put("device_name", android.os.Build.MODEL.ifBlank { "Android device" })
                .put("app_version", BuildConfig.VERSION_NAME)
            parseSession(site, postObject("${site.url}$API/app/login", body))
        }

    suspend fun acceptInvite(qr: String): AppSession = withContext(Dispatchers.IO) {
        // Only structured AdultSafe QR payloads; never follow arbitrary scanned links.
        val payload = try { JSONObject(qr) } catch (_: Exception) {
            throw AdultSafeConnectionException("This is not an AdultSafe invite QR code.")
        }
        val url = payload.optString("site_url").ifBlank { payload.optString("site") }
        val token = payload.optString("invite_token").ifBlank { payload.optString("token") }
        if (token.isBlank() || token == "null")
            throw AdultSafeConnectionException("This invite QR code is missing its invite token.")
        val site = discoverSite(url)
        val body = JSONObject()
            .put("invite_token", token)
            .put("device_name", android.os.Build.MODEL.ifBlank { "Android device" })
            .put("app_version", BuildConfig.VERSION_NAME)
        parseSession(site, postObject("${site.url}$API/app/invites/accept", body))
    }

    private fun parseSession(site: ConnectedSite, raw: JSONObject): AppSession {
        val json = raw.optJSONObject("data") ?: raw
        val token = json.optString("token").trim()
        if (token.isBlank() || token == "null")
            throw AdultSafeConnectionException("The site did not return a valid app session. Contact your site administrator.")
        val user = json.optJSONObject("user")
            ?: throw AdultSafeConnectionException("The site returned an incomplete sign-in response.")
        val siteInfo = json.optJSONObject("site")
        val verifiedSite = site.copy(
            name = siteInfo?.optString("name")?.takeIf { it.isNotBlank() } ?: site.name,
            apiVersion = siteInfo?.optString("api_version")?.takeIf { it.isNotBlank() } ?: site.apiVersion,
            demoMode = false
        )
        return AppSession(
            site = verifiedSite,
            token = token,
            userId = user.optString("id"),
            userName = user.optString("name"),
            roles = user.optJSONArray("roles").strings(),
            permissions = json.optJSONArray("permissions").strings()
        )
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList()
        else (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() && s != "null" } }

    private fun getObject(endpoint: String): JSONObject = request(endpoint, "GET", null)
    private fun postObject(endpoint: String, body: JSONObject): JSONObject = request(endpoint, "POST", body)

    private fun request(endpoint: String, method: String, body: JSONObject?): JSONObject {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.requestMethod = method
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw AdultSafeConnectionException(when (status) {
                    401, 403 -> "Sign-in was rejected. Check your details or ask your site administrator."
                    404 -> "This site does not have the AdultSafe app connection enabled."
                    408, 429, 500, 502, 503, 504 -> "The site is not responding right now. Please try again."
                    in 300..399 -> "The site redirected the app connection. Check the site URL."
                    else -> "The site could not complete this request (error $status)."
                })
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return try { JSONObject(response) } catch (_: Exception) {
                throw AdultSafeConnectionException("The site returned a response the app could not read.")
            }
        } catch (e: AdultSafeConnectionException) {
            throw e
        } catch (_: java.net.SocketTimeoutException) {
            throw AdultSafeConnectionException("The site took too long to respond. Check your connection.")
        } catch (_: Exception) {
            throw AdultSafeConnectionException("Could not reach your AdultSafe site. Check the address and internet connection.")
        } finally {
            connection.disconnect()
        }
    }

    // Existing evidence-upload foundation retained. This is not yet a completed upload.
    suspend fun uploadIdEvidence(
        site: ConnectedSite, personId: String, token: String, filename: String, bytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(bytes.isNotEmpty()) { "Evidence file is empty" }
            require(token.isNotBlank()) { "Missing app token" }
            "${site.url}$API/people/$personId/id-evidence:$filename"
        }
    }
}
