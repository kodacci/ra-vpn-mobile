package pro.ra_tech.ra_vpn.mobile.client.common.tun;

import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_PACKET_SIZE;
import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_VPN_PACKET_SIZE;

import java.io.IOException;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.common.ip.IpHeaderParser;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.DataTransferPacket;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseTunReader implements TunReader {
    private final VpnTunDevice tun;
    private final ChannelSupplier channelSupplier;

    private final IpHeaderParser parser = new IpHeaderParser();
    private final byte[] buffer = new byte[MAX_PACKET_SIZE];

    @Override
    public void read() throws InterruptedException {
        try {
            if (!tun.isInitialized()) {
                Thread.sleep(1000);
                return;
            }

            val read = tun.read(buffer);
            if (read <= 0) {
                return;
            }

            if (read > MAX_VPN_PACKET_SIZE) {
                log.warn("Too big packet size from internet: {}, dropping", read);
                return;
            }

            val header = parser.parse(buffer);
            val channel = channelSupplier.get(header);
            if (channel == null) {
                return;
            }

            val data = new byte[read];
            System.arraycopy(buffer, 0, data, 0, read);
            sendPacket(channel, new DataTransferPacket(
                    data,
                    header.srcAddress().toInetAddress(),
                    header.dstAddress().toInetAddress()
            ));
        } catch (IOException ex) {
            log.error("Error reading from tun device", ex);
            Thread.sleep(1000);
        }
    }

    @Override
    public String getTunName() {
        return tun.getName();
    }

    protected abstract void sendPacket(Channel channel, DataTransferPacket packet);
}
