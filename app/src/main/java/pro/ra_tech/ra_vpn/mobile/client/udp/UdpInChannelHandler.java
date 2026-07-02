package pro.ra_tech.ra_vpn.mobile.client.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.base.ClientContext;
import pro.ra_tech.ra_vpn.mobile.client.base.VpnPacketHandler;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.PacketEncryptor;

@Slf4j
public class UdpInChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private final PacketEncryptor encryptor;
    private final VpnPacketHandler handler;

    public UdpInChannelHandler(ClientContext ctx) {
        encryptor = ctx.encryptorSupplier().get();
        handler = new VpnPacketHandler(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            val packet = encryptor.decrypt(msg);
            handler.handle(
                    packet,
                    msg.sender(),
                    res -> ctx.writeAndFlush(new DatagramPacket(encryptor.encrypt(res), msg.sender()))
            );
        } catch (Exception ex) {
            log.error("Error handling packet from {}:", msg.sender(), ex);
        }
    }
}
