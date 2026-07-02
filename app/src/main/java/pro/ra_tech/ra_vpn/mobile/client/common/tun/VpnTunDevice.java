package pro.ra_tech.ra_vpn.mobile.client.common.tun;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetAddress;

public interface VpnTunDevice extends AutoCloseable {
    boolean isInitialized();
    void setVirtualIp(InetAddress virtualIp, InetAddress gatewayIp) throws Exception;
    @Nullable InetAddress getVirtualIp();
    String getName();
    int read(byte[] buffer) throws IOException;
    void write(byte[] data, int size) throws IOException;
    InetAddress getGatewayIp();
}
