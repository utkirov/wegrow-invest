package uz.wegrow.invest.data

import android.content.Context
import android.content.SharedPreferences
import uz.wegrow.invest.BuildConfig

class AuthSessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("wegrow_prefs", Context.MODE_PRIVATE)

    fun loadSession(): AuthSession {
        return AuthSession(
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, "") ?: "",
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: "",
            identifier = prefs.getString(KEY_IDENTIFIER, "") ?: "",
            isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
        )
    }

    fun saveSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_IDENTIFIER, session.identifier)
            .putBoolean(KEY_IS_GUEST, session.isGuest)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_IDENTIFIER)
            .remove(KEY_IS_GUEST)
            .apply()
    }

    fun getServerUrl(): String =
        prefs.getString(KEY_TUNNEL_URL, BuildConfig.DEFAULT_TUNNEL_URL) ?: BuildConfig.DEFAULT_TUNNEL_URL

    fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_TUNNEL_URL, url.trim()).apply()
    }

    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, true)

    fun saveDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply()
    }

    fun getLangCode(): String = prefs.getString(KEY_LANG_CODE, "ru") ?: "ru"

    fun saveLangCode(lang: String) {
        prefs.edit().putString(KEY_LANG_CODE, lang).apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_IDENTIFIER = "auth_identifier"
        private const val KEY_IS_GUEST = "auth_is_guest"
        private const val KEY_TUNNEL_URL = "tunnel_url"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_LANG_CODE = "lang_code"
    }
}
