package uk.co.skills4all.adultsafe.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONArray
import org.json.JSONObject
import uk.co.skills4all.adultsafe.api.AppSession
import uk.co.skills4all.adultsafe.model.ConnectedSite

/** Credentials remain in Android Keystore-backed encrypted local preferences; backup is disabled. */
object SessionStore {
    private const val FILE = "adultsafe_secure_session"
    private const val KEY = "session"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(context: Context, session: AppSession) {
        val value = JSONObject()
            .put("site_url", session.site.url)
            .put("site_name", session.site.name)
            .put("api_version", session.site.apiVersion)
            .put("token", session.token)
            .put("user_id", session.userId)
            .put("user_name", session.userName)
            .put("roles", JSONArray(session.roles))
            .put("permissions", JSONArray(session.permissions))
        check(prefs(context).edit().putString(KEY, value.toString()).commit()) {
            "Secure sign-in could not be saved."
        }
    }

    fun restore(context: Context): AppSession? = runCatching {
        val saved = prefs(context).getString(KEY, null) ?: return@runCatching null
        val json = JSONObject(saved)
        val token = json.optString("token").takeIf { it.isNotBlank() } ?: return@runCatching null
        val site = ConnectedSite(
            name = json.getString("site_name"),
            url = json.getString("site_url"),
            apiVersion = json.optString("api_version")
        )
        fun values(key: String): List<String> = json.optJSONArray(key)?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } ?: emptyList()
        AppSession(site, token, json.optString("user_id"), json.optString("user_name"), values("roles"), values("permissions"))
    }.getOrNull()

    fun clear(context: Context) {
        check(prefs(context).edit().remove(KEY).commit()) { "Could not clear the local session." }
    }
}
