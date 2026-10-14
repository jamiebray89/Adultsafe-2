package uk.co.skills4all.adultsafe.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONArray
import org.json.JSONObject
import uk.co.skills4all.adultsafe.api.AppSession
import uk.co.skills4all.adultsafe.model.ConnectedSite

/** Keystore-backed tokens per AdultSafe site; never stores user passwords or evidence. */
object SessionStore {
    private const val FILE = "adultsafe_secure_session"
    private const val KEY = "session"
    private const val SITES = "site_sessions"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun encode(session: AppSession) = JSONObject()
        .put("site_url", session.site.url)
        .put("site_name", session.site.name)
        .put("api_version", session.site.apiVersion)
        .put("token", session.token)
        .put("user_id", session.userId)
        .put("user_name", session.userName)
        .put("roles", JSONArray(session.roles))
        .put("permissions", JSONArray(session.permissions))

    private fun decode(json: JSONObject): AppSession? {
        val token = json.optString("token").takeIf { it.isNotBlank() } ?: return null
        val url = json.optString("site_url").takeIf { it.startsWith("https://") } ?: return null
        fun values(key: String): List<String> = json.optJSONArray(key)?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } ?: emptyList()
        return AppSession(
            ConnectedSite(json.optString("site_name", url), url, json.optString("api_version", "unknown")),
            token, json.optString("user_id"), json.optString("user_name"), values("roles"), values("permissions")
        )
    }

    fun restore(context: Context): AppSession? = runCatching {
        prefs(context).getString(KEY, null)?.let { decode(JSONObject(it)) }
    }.getOrNull()

    fun saved(context: Context): List<AppSession> = runCatching {
        val p = prefs(context)
        val arr = JSONArray(p.getString(SITES, "[]"))
        val result = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::decode) }
        val current = p.getString(KEY, null)?.let { decode(JSONObject(it)) }
        (result + listOfNotNull(current)).distinctBy { it.site.url }
    }.getOrDefault(emptyList())

    fun save(context: Context, session: AppSession) {
        val all = saved(context).filter { it.site.url != session.site.url } + session
        val arr = JSONArray()
        all.forEach { arr.put(encode(it)) }
        check(prefs(context).edit()
            .putString(SITES, arr.toString())
            .putString(KEY, encode(session).toString())
            .commit()) { "Secure sign-in could not be saved." }
    }

    fun activate(context: Context, session: AppSession) {
        require(saved(context).any { it.site.url == session.site.url && it.token == session.token }) {
            "That site's saved session is no longer available."
        }
        check(prefs(context).edit().putString(KEY, encode(session).toString()).commit()) {
            "Unable to switch AdultSafe sites."
        }
    }

    fun clear(context: Context) {
        val current = restore(context)
        val remaining = saved(context).filter { it.site.url != current?.site?.url }
        val arr = JSONArray()
        remaining.forEach { arr.put(encode(it)) }
        val edit = prefs(context).edit().putString(SITES, arr.toString())
        if (remaining.isEmpty()) edit.remove(KEY)
        else edit.putString(KEY, encode(remaining.last()).toString())
        check(edit.commit()) { "Could not clear the local session." }
    }
}
