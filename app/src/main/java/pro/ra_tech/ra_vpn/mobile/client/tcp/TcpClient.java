package pro.ra_tech.ra_vpn.mobile.client.tcp;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.base.BaseClient;
import pro.ra_tech.ra_vpn.mobile.client.base.ClientContext;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.EncryptorType;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.VpnTunDevice;

public class TcpClient extends BaseClient {
    private SocketChannel source;
    private final TcpSocketProtector protector;

    public TcpClient(
            InetSocketAddress server,
            EncryptorType encryptorType,
            VpnTunDevice tun,
            String clientId,
            TcpSocketProtector protector,
            String virtualIp,
            @Nullable String base64Key
    ) {
        super(server, encryptorType, tun, clientId, virtualIp, base64Key);
        this.protector = protector;
    }

    @Override
    protected Channel bootstrap(IoEventLoopGroup masterGroup, ClientContext ctx) throws InterruptedException {
        val boot = new Bootstrap();
        boot.group(masterGroup);
        boot.channelFactory(() -> new NioSocketChannel(source));
        boot.option(ChannelOption.SO_KEEPALIVE, true);
        boot.option(ChannelOption.TCP_NODELAY, true);
        boot.handler(new TcpClientInitializer(ctx));

        return boot.connect(getServer()).sync().channel();
    }

    @Override
    protected ClientType getType() {
        return ClientType.TCP;
    }

    @Override
    protected Closeable openChannel() throws IOException {
        source = SocketChannel.open();
        protector.protect(source.socket());

        return source;
    }
}

