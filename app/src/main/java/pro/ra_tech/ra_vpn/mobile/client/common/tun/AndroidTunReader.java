package pro.ra_tech.ra_vpn.mobile.client.common.tun;

import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_PACKET_SIZE;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.common.bin.BinHelper;
import pro.ra_tech.ra_vpn.mobile.client.common.ip.IpHeaderParser;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.DataTransferPacket;

import java.io.IOException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AndroidTunReader {
    private static void read(VpnTunDevice tun, byte[] buffer, IpHeaderParser parser, Channel channel) throws InterruptedException {
        try {
            if (!tun.isInitialized()) {
                Thread.sleep(1000);

                return;
            }

            val length = tun.read(buffer);
            if (length == 0) {
                return;
            }

            val header = parser.parse(buffer);
            val packet = new byte[length];
            System.arraycopy(buffer, 0, packet, 0, length);
            channel.pipeline().fireChannelRead(new DataTransferPacket(
                    packet,
                    header.srcAddress().toInetAddress(),
                    header.dstAddress().toInetAddress())
            );
        } catch (IOException ex) {
            log.error("Error reading from tun device", ex);
            Thread.sleep(1000);
        }
    }

    public static Thread start(VpnTunDevice tun, Channel channel) {
        val thread = new Thread(() -> {
            try {
                val buffer = new byte[MAX_PACKET_SIZE];
                val parser = new IpHeaderParser();

                log.info("Starting reading from tun device {}", tun.getName());
                for (;;) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("Tun reader interrupted");
                        return;
                    }
                    read(tun, buffer, parser, channel);
                }
            } catch (InterruptedException ex) {
                log.warn("Tun reader interrupted");
                Thread.currentThread().interrupt();
            }
        }, "TunReader-" + tun.getName());

        thread.start();

        return thread;
    }
}
