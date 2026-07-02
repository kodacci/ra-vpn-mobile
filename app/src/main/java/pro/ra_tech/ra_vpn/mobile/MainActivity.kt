package pro.ra_tech.ra_vpn.mobile

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pro.ra_tech.ra_vpn.mobile.client.VpnMobileClientService
import pro.ra_tech.ra_vpn.mobile.config.VpnConfig
import pro.ra_tech.ra_vpn.mobile.config.VpnConfigRepository

class MainActivity : ComponentActivity() {
    private var connected: Boolean = false
    private var useTcp: Boolean = false
    private var cipherKeyVisible: Boolean = false

    private lateinit var configRepo: VpnConfigRepository
    private lateinit var transportSwitch: Switch
    private lateinit var serverHostInput: EditText
    private lateinit var serverPortInput: EditText
    private lateinit var clientIdInput: EditText
    private lateinit var cipherKeyInput: EditText
    private lateinit var proxySwitch: Switch
    private lateinit var proxyHostInput: EditText
    private lateinit var proxyPortInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        configRepo = VpnConfigRepository(applicationContext)

        serverHostInput = findViewById(R.id.serverHostInput)
        serverPortInput = findViewById(R.id.serverPortInput)
        clientIdInput = findViewById(R.id.clientIdInput)
        cipherKeyInput = findViewById(R.id.cipherKeyInput)
        setupCipherKeyToggle()
        proxyHostInput = findViewById(R.id.proxyHostInput)
        proxyPortInput = findViewById(R.id.proxyPortInput)

        val connectBtn: Button = findViewById(R.id.connectButton)
        connectBtn.setText(R.string.connect_button_connect_title)
        connectBtn.setOnClickListener { onConnBtnClick(connectBtn) }

        transportSwitch = findViewById(R.id.transportSwitch)
        transportSwitch.setOnCheckedChangeListener(this::onTcpSwitchChanged)

        proxySwitch = findViewById(R.id.proxySwitch)
        proxySwitch.setOnCheckedChangeListener { _, checked -> onProxySwitchChanged(checked) }

        setEditingEnabled(true)

        // Load persisted configuration on startup and populate the fields.
        lifecycleScope.launch {
            applyConfig(configRepo.load())
        }
    }

    private fun applyConfig(config: VpnConfig) {
        serverHostInput.setText(config.serverHost)
        serverPortInput.setText(config.serverPort.toString())
        clientIdInput.setText(config.clientId)
        cipherKeyInput.setText(config.cipherKey)
        proxyHostInput.setText(config.proxyHost)
        proxyPortInput.setText(config.proxyPort.toString())
        proxySwitch.isChecked = config.proxyEnabled
        setProxyInputsEnabled(config.proxyEnabled)
    }

    private fun readConfig(): VpnConfig {
        val default = VpnConfigRepository.DEFAULT
        return VpnConfig(
            serverHost = serverHostInput.text.toString().trim().ifEmpty { default.serverHost },
            serverPort = serverPortInput.text.toString().trim().toIntOrNull() ?: default.serverPort,
            clientId = clientIdInput.text.toString().trim().ifEmpty { default.clientId },
            cipherKey = cipherKeyInput.text.toString().trim().ifEmpty { default.cipherKey },
            proxyEnabled = proxySwitch.isChecked,
            proxyHost = proxyHostInput.text.toString().trim().ifEmpty { default.proxyHost },
            proxyPort = proxyPortInput.text.toString().trim().toIntOrNull() ?: default.proxyPort,
        )
    }

    private fun setEditingEnabled(enabled: Boolean) {
        serverHostInput.isEnabled = enabled
        serverPortInput.isEnabled = enabled
        clientIdInput.isEnabled = enabled
        cipherKeyInput.isEnabled = enabled
        transportSwitch.isEnabled = enabled
        proxySwitch.isEnabled = enabled
        setProxyInputsEnabled(enabled && proxySwitch.isChecked)
    }

    private fun setProxyInputsEnabled(enabled: Boolean) {
        proxyHostInput.isEnabled = enabled
        proxyPortInput.isEnabled = enabled
    }

    /** Tapping the trailing eye icon of the cipher key field toggles masking. */
    @Suppress("ClickableViewAccessibility")
    private fun setupCipherKeyToggle() {
        cipherKeyInput.setOnTouchListener { _, event ->
            val icon = cipherKeyInput.compoundDrawablesRelative[2]
            if (event.action == MotionEvent.ACTION_UP && icon != null) {
                val hitStart = cipherKeyInput.width - cipherKeyInput.paddingEnd - icon.bounds.width()
                if (event.x >= hitStart) {
                    toggleCipherKeyVisibility()
                    cipherKeyInput.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun toggleCipherKeyVisibility() {
        cipherKeyVisible = !cipherKeyVisible
        cipherKeyInput.transformationMethod = if (cipherKeyVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        val icon = if (cipherKeyVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        cipherKeyInput.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, icon, 0)
        cipherKeyInput.setSelection(cipherKeyInput.text.length)
    }

    private fun buildStartIntent(config: VpnConfig): Intent =
        Intent(this, VpnMobileClientService::class.java).apply {
            putExtra(VpnMobileClientService.EXTRA_USE_TCP, useTcp)
            putExtra(VpnMobileClientService.EXTRA_SERVER_HOST, config.serverHost)
            putExtra(VpnMobileClientService.EXTRA_SERVER_PORT, config.serverPort)
            putExtra(VpnMobileClientService.EXTRA_CLIENT_ID, config.clientId)
            putExtra(VpnMobileClientService.EXTRA_CIPHER_KEY, config.cipherKey)
            putExtra(VpnMobileClientService.EXTRA_PROXY_ENABLED, config.proxyEnabled)
            putExtra(VpnMobileClientService.EXTRA_PROXY_HOST, config.proxyHost)
            putExtra(VpnMobileClientService.EXTRA_PROXY_PORT, config.proxyPort)
        }

    @Deprecated("TODO: Move to registerForActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            startService(buildStartIntent(readConfig()))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onConnBtnClick(btn: Button) {
        Log.i("MAIN", "Connect button clicked")

        if (!connected) {
            val config = readConfig()
            lifecycleScope.launch { configRepo.save(config) }

            val intent = VpnMobileClientService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, 1)
            } else {
                startService(buildStartIntent(config))
            }
            connected = true
            setEditingEnabled(false)
            btn.setText(R.string.connect_button_disconnect_title)

            return
        }

        startService(Intent(this, VpnMobileClientService::class.java))
        stopService(Intent(this, VpnMobileClientService::class.java))
        connected = false
        setEditingEnabled(true)
        btn.setText(R.string.connect_button_connect_title)
    }

    private fun onTcpSwitchChanged(btn: CompoundButton, checked: Boolean) {
        useTcp = checked
    }

    private fun onProxySwitchChanged(checked: Boolean) {
        setProxyInputsEnabled(checked)
    }
}
