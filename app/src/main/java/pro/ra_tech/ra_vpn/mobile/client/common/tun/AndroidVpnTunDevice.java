package pro.ra_tech.ra_vpn.mobile.client.common.tun;

import android.os.ParcelFileDescriptor;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AndroidVpnTunDevice implements VpnTunDevice {
    private final TunBuilder tunBuilder;
    @Getter
    private final String name;

    private ParcelFileDescriptor tun;
    private FileInputStream in;
    private FileOutputStream out;

    @Getter
    private InetAddress virtualIp;
    private InetAddress gatewayIp;

    @Override
    public int read(byte[] buffer) throws IOException {
        return in.read(buffer);
    }

    @Override
    public void write(byte[] data, int size) throws IOException {
        out.write(data, 0, size);
    }

    @Override
    public boolean isInitialized() {
        return tun != null;
    }

    @Override
    public void setVirtualIp(InetAddress virtualIp, InetAddress gatewayIp) throws Exception {
        if (virtualIp.equals(this.virtualIp) && tun != null) {
            return;
        }

        close();

        tun = tunBuilder.build(virtualIp.getHostAddress());
        in = new FileInputStream(tun.getFileDescriptor());
        out = new FileOutputStream(tun.getFileDescriptor());
        this.virtualIp = virtualIp;
        this.gatewayIp = gatewayIp;
    }

    @Override
    @Nullable
    public InetAddress getGatewayIp() {
        return gatewayIp;
    }

    @Override
    public void close() throws Exception {
        if (tun != null) {
            in.close();
            out.close();
            tun.close();
            tun = null;
            virtualIp = null;
        }
    }
}
