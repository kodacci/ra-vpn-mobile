package pro.ra_tech.ra_vpn.mobile.client.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.event.ConnectionState;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.PacketEncryptor;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacket;
import pro.ra_tech.ra_vpn.mobile.client.event.Connector;

import java.net.InetSocketAddress;

import static pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacketType.CONNECT;

@Slf4j
@RequiredArgsConstructor
public class UdpOutChannelHandler extends ChannelInboundHandlerAdapter {
    private final InetSocketAddress server;
    private final PacketEncryptor encryptor;
    private final Connector connector;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        val packet = (VpnPacket) event;

        if (connector.getState() != ConnectionState.CONNECTED && packet.getType() != CONNECT) {
            log.info("Not connected, skipping packet");

            return;
        }

        ctx.writeAndFlush(new DatagramPacket(
                encryptor.encrypt(packet),
                server
        ));
    }
}
