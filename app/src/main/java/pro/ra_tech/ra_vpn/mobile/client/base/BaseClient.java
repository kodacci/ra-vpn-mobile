package pro.ra_tech.ra_vpn.mobile.client.base;

import static lombok.AccessLevel.PROTECTED;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.concurrent.AutoScalingEventExecutorChooserFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.Client;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.EncryptorFactory;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.EncryptorType;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.PacketEncryptor;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.TunReaderHandler;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.VpnTunDevice;
import pro.ra_tech.ra_vpn.mobile.client.event.ConnectionWatchdog;
import pro.ra_tech.ra_vpn.mobile.client.event.Connector;

@Slf4j
@Getter
@RequiredArgsConstructor(access = PROTECTED)
public abstract class BaseClient implements Client {
    protected static final int CONNECTION_INTERVAL_SEC = 3;

    /**
     * If no keep-alive arrives from the server within this window while connected, the
     * connection is treated as lost and a reconnect is triggered. The server sends keep-alives
     * every 5s, so this allows a few missed ones before reacting.
     */
    protected static final Duration KEEP_ALIVE_TIMEOUT = Duration.ofSeconds(15);

    private final InetSocketAddress server;
    private final EncryptorType encryptorType;
    private final VpnTunDevice tun;
    private final String clientId;
    private final String virtualIp;
    @Nullable
    private final String base64Key;

    private Channel channel;
    private Thread tunReaderThread;
    private MultiThreadIoEventLoopGroup masterGroup;

    @RequiredArgsConstructor
    protected enum ClientType {
        TCP("TCP"),
        UDP("UDP");

        private final String type;

        @Override
        public String toString() {
            return type;
        }
    }

    private PacketEncryptor buildEncryptor() {
        return EncryptorFactory.ofType(encryptorType, base64Key);
    }

    protected abstract Channel bootstrap(
            IoEventLoopGroup masterGroup,
            ClientContext ctx
    ) throws InterruptedException;
    protected abstract ClientType getType();

    protected abstract Closeable openChannel() throws IOException;

    /**
     * Hook for scheduling transport-specific connection-loss detection. No-op by default; only
     * the connectionless UDP transport schedules a {@link ConnectionWatchdog} here, since TCP
     * detects a dropped connection through the socket itself.
     */
    protected void scheduleWatchdog(ScheduledExecutorService executor, Connector connector) {
    }

    @Override
    public void start() {
        val factory = new AutoScalingEventExecutorChooserFactory(
                1,
                4,
                500,
                TimeUnit.MILLISECONDS,
                0.25,
                0.8,
                1,
                1,
                2
        );

        masterGroup = new MultiThreadIoEventLoopGroup(4, null, factory, NioIoHandler.newFactory());

        try (
                val source = openChannel();
                val connExecutor = Executors.newSingleThreadScheduledExecutor()
        ) {
            val virtualAddr = InetAddress.getByName(virtualIp);
            val connector = new Connector(clientId, virtualAddr, server.getAddress());
            val ctx = new ClientContext(
                    getServer(), tun, this::buildEncryptor, connector, clientId
            );

            channel = bootstrap(masterGroup, ctx);
            connector.setChannel(channel);
            connExecutor.scheduleWithFixedDelay(connector, 0, CONNECTION_INTERVAL_SEC, TimeUnit.SECONDS);
            scheduleWatchdog(connExecutor, connector);

            log.info("{} client started on {}", getType(), channel.localAddress());

            tunReaderThread = TunReaderHandler.start(new ClientTunReader(tun, header -> channel));
            tunReaderThread.join();
        } catch (InterruptedException ex) {
            log.warn("UDP client interrupted");
            Thread.currentThread().interrupt();
            tunReaderThread.interrupt();
        } catch (Exception ex) {
            log.error("Error opening tun device: ", ex);
        } finally {
            masterGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() {
        try {
            if (tunReaderThread != null && !tunReaderThread.isInterrupted()) {
                tunReaderThread.interrupt();
            }

            if (channel != null) {
                channel.close();
                channel.closeFuture().sync();
            }
            tun.close();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("Tun close error", ex);
        } finally {
            masterGroup.shutdownGracefully();
        }
    }
}
