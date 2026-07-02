package pro.ra_tech.ra_vpn.mobile.client;

import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_VPN_PACKET_SIZE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.net.InetSocketAddress;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.EncryptorType;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.AndroidVpnTunDevice;
import pro.ra_tech.ra_vpn.mobile.client.tcp.TcpClient;
import pro.ra_tech.ra_vpn.mobile.client.udp.UdpClient;

@Slf4j
@SuppressLint("VpnServicePolicy")
public class VpnMobileClientService extends VpnService implements Runnable {
    private static final String TAG = "VPN-Client";

    public static final String EXTRA_USE_TCP = "USE_TCP";
    public static final String EXTRA_SERVER_HOST = "SERVER_HOST";
    public static final String EXTRA_SERVER_PORT = "SERVER_PORT";
    public static final String EXTRA_CLIENT_ID = "CLIENT_ID";
    public static final String EXTRA_CIPHER_KEY = "CIPHER_KEY";
    public static final String EXTRA_PROXY_ENABLED = "PROXY_ENABLED";
    public static final String EXTRA_PROXY_HOST = "PROXY_HOST";
    public static final String EXTRA_PROXY_PORT = "PROXY_PORT";

    private static final String DEFAULT_SERVER_HOST = "example.com";
    private static final int DEFAULT_SERVER_PORT = 9867;
    private static final String DEFAULT_CLIENT_ID = "";
    private static final String DEFAULT_CIPHER_KEY = "";
    private static final int DEFAULT_PROXY_PORT = 9867;

    private static final String UDP_VIRTUAL_IP = "10.10.0.20";
    private static final String TCP_VIRTUAL_IP = "10.11.0.20";
    private static final String SESSION_NAME = "RA-VPN";
    private static final int SUBNET_LENGTH = 24;

    private Thread thread;
    private Client client;
    private boolean useTcp = false;
    private String serverHost = DEFAULT_SERVER_HOST;
    private int serverPort = DEFAULT_SERVER_PORT;
    private String clientId = DEFAULT_CLIENT_ID;
    private String cipherKey = DEFAULT_CIPHER_KEY;
    private boolean proxyEnabled = false;
    private String proxyHost = "";
    private int proxyPort = DEFAULT_PROXY_PORT;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (thread != null) {
            stop();
            return START_STICKY;
        }

        val extras = intent.getExtras();
        if (extras != null) {
            useTcp = extras.getBoolean(EXTRA_USE_TCP, false);
            serverHost = extras.getString(EXTRA_SERVER_HOST, DEFAULT_SERVER_HOST);
            serverPort = extras.getInt(EXTRA_SERVER_PORT, DEFAULT_SERVER_PORT);
            clientId = extras.getString(EXTRA_CLIENT_ID, DEFAULT_CLIENT_ID);
            cipherKey = extras.getString(EXTRA_CIPHER_KEY, DEFAULT_CIPHER_KEY);
            proxyEnabled = extras.getBoolean(EXTRA_PROXY_ENABLED, false);
            proxyHost = extras.getString(EXTRA_PROXY_HOST, "");
            proxyPort = extras.getInt(EXTRA_PROXY_PORT, DEFAULT_PROXY_PORT);
        }
        thread = new Thread(this, "vpn-client");
        thread.start();

        return START_STICKY;
    }

    private void stop() {
        try {
            if (client != null && thread != null) {
                client.stop();
                thread.join();
                thread = null;
            }
        } catch (InterruptedException ex) {
            Log.w(TAG, "Destroy method interrupted");
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping VPN service");
        super.onDestroy();
    }

    private ParcelFileDescriptor buildTun(String virtualIp) {
        Builder builder = new Builder();

        builder.addAddress(virtualIp, SUBNET_LENGTH);
        builder.addRoute("0.0.0.0", 0);
        builder.setMtu(MAX_VPN_PACKET_SIZE);
        builder.setSession(SESSION_NAME);

        return builder.establish();
    }

    @Override
    public void run() {
        val type = useTcp ? "TCP" : "UDP";
        Log.i(TAG, "Configuring " + type + " VPN client");

        try {
            val useProxy = proxyEnabled && !proxyHost.isEmpty();
            val targetHost = useProxy ? proxyHost : serverHost;
            val targetPort = useProxy ? proxyPort : serverPort;
            if (useProxy) {
                Log.i(TAG, "Routing connection through proxy " + targetHost + ":" + targetPort);
            }

            val server = new InetSocketAddress(targetHost, targetPort);
            val wrapper = new AndroidVpnTunDevice(this::buildTun, SESSION_NAME);
            client = useTcp
                    ? new TcpClient(
                            server,
                            EncryptorType.AES,
                            wrapper,
                            clientId,
                            this::protect,
                            TCP_VIRTUAL_IP,
                            cipherKey
                    )
                    : new UdpClient(
                            server,
                            EncryptorType.AES,
                            wrapper,
                            clientId,
                            this::protect,
                            UDP_VIRTUAL_IP,
                            cipherKey
                    );

            Log.i(TAG, "Starting " + type + " VPN client");
            client.start();
        } catch (Exception ex) {
            Log.e(TAG, "Error starting UDP VPN client:", ex);
        }
    }
}
