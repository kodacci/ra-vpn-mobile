package pro.ra_tech.ra_vpn.mobile.config

/**
 * User-editable VPN connection parameters persisted via [VpnConfigRepository].
 */
data class VpnConfig(
    val serverHost: String,
    val serverPort: Int,
    val clientId: String,
    val cipherKey: String,
    val proxyEnabled: Boolean,
    val proxyHost: String,
    val proxyPort: Int,
)
