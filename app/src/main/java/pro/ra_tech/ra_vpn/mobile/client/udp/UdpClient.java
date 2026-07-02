package pro.ra_tech.ra_vpn.mobile.client.udp;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.base.BaseClient;
import pro.ra_tech.ra_vpn.mobile.client.base.ClientContext;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.EncryptorType;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.VpnTunDevice;
import pro.ra_tech.ra_vpn.mobile.client.event.ConnectionWatchdog;
import pro.ra_tech.ra_vpn.mobile.client.event.Connector;

@Slf4j
public class UdpClient extends BaseClient {
    private DatagramChannel source;
    private final UdpSocketProtector protector;

    public UdpClient(
            InetSocketAddress server,
            EncryptorType encryptorType,
            VpnTunDevice tun,
            String clientId,
            UdpSocketProtector protector,
            String virtualIp,
            @Nullable String base64Key
    ) {
        super(server, encryptorType, tun, clientId, virtualIp, base64Key);
        this.protector = protector;
    }

    protected Channel bootstrap(
            IoEventLoopGroup masterGroup,
            ClientContext ctx
    ) throws InterruptedException {
        val boot = new Bootstrap();

        boot.group(masterGroup)
                .channelFactory(() -> new NioDatagramChannel(source))
                .handler(new UdpClientInitializer(ctx));

        return boot.bind(0).sync().channel();
    }

    @Override
    protected ClientType getType() {
        return ClientType.UDP;
    }

    @Override
    protected Closeable openChannel() throws IOException {
        source = DatagramChannel.open();
        protector.protect(source.socket());

        return source;
    }

    @Override
    protected void scheduleWatchdog(ScheduledExecutorService executor, Connector connector) {
        executor.scheduleWithFixedDelay(
                new ConnectionWatchdog(connector, KEEP_ALIVE_TIMEOUT),
                CONNECTION_INTERVAL_SEC, CONNECTION_INTERVAL_SEC, TimeUnit.SECONDS
        );
    }
}
