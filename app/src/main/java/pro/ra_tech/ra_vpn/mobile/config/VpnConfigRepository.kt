package pro.ra_tech.ra_vpn.mobile.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_config")

/**
 * Loads and stores the [VpnConfig] in a DataStore preferences file. Falls back to [DEFAULT]
 * for any parameter that has never been set.
 */
class VpnConfigRepository(private val context: Context) {

    suspend fun load(): VpnConfig =
        context.dataStore.data.map { prefs ->
            VpnConfig(
                serverHost = prefs[SERVER_HOST] ?: DEFAULT.serverHost,
                serverPort = prefs[SERVER_PORT] ?: DEFAULT.serverPort,
                clientId = prefs[CLIENT_ID] ?: DEFAULT.clientId,
                cipherKey = prefs[CIPHER_KEY] ?: DEFAULT.cipherKey,
                proxyEnabled = prefs[PROXY_ENABLED] ?: DEFAULT.proxyEnabled,
                proxyHost = prefs[PROXY_HOST] ?: DEFAULT.proxyHost,
                proxyPort = prefs[PROXY_PORT] ?: DEFAULT.proxyPort,
            )
        }.first()

    suspend fun save(config: VpnConfig) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_HOST] = config.serverHost
            prefs[SERVER_PORT] = config.serverPort
            prefs[CLIENT_ID] = config.clientId
            prefs[CIPHER_KEY] = config.cipherKey
            prefs[PROXY_ENABLED] = config.proxyEnabled
            prefs[PROXY_HOST] = config.proxyHost
            prefs[PROXY_PORT] = config.proxyPort
        }
    }

    companion object {
        val DEFAULT = VpnConfig(
            serverHost = "",
            serverPort = 9867,
            clientId = "",
            cipherKey = "",
            proxyEnabled = false,
            proxyHost = "",
            proxyPort = 9867,
        )

        private val SERVER_HOST = stringPreferencesKey("server_host")
        private val SERVER_PORT = intPreferencesKey("server_port")
        private val CLIENT_ID = stringPreferencesKey("client_id")
        private val CIPHER_KEY = stringPreferencesKey("cipher_key")
        private val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        private val PROXY_HOST = stringPreferencesKey("proxy_host")
        private val PROXY_PORT = intPreferencesKey("proxy_port")
    }
}
