package pro.ra_tech.ra_vpn.mobile.client.event;

import androidx.annotation.Nullable;

import java.net.InetAddress;
import java.time.Instant;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.ConnectPacket;

@Slf4j
@RequiredArgsConstructor
public class Connector implements Runnable {
    private final String clientId;
    private final InetAddress virtualIp;
    private final InetAddress serverHost;

    @Setter
    @Nullable
    private Channel channel;
    @Getter
    @Setter
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    @Getter
    private volatile Instant lastAlive = Instant.now();

    /**
     * Records that a keep-alive (or connect ack) was just received from the server,
     * resetting the disconnection-detection timer.
     */
    public void markAlive() {
        lastAlive = Instant.now();
    }

    @Override
    public void run() {
        if (state == ConnectionState.DISCONNECTED && channel != null) {
            channel.pipeline().fireUserEventTriggered(
                    new ConnectPacket(
                            clientId,
                            virtualIp,
                            serverHost
                    )
            );
        }
    }
}
