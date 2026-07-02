package pro.ra_tech.ra_vpn.mobile.client.tcp;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import pro.ra_tech.ra_vpn.mobile.client.base.ClientContext;
import pro.ra_tech.ra_vpn.mobile.client.base.VpnPacketHandler;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacket;

@Slf4j
public class TcpVpnPacketHandler extends ChannelInboundHandlerAdapter {
    private final VpnPacketHandler handler;

    public TcpVpnPacketHandler(ClientContext ctx) {
        handler = new VpnPacketHandler(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Connected to server");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        handler.handle(
                (VpnPacket) msg,
                (InetSocketAddress) ctx.channel().remoteAddress(),
                ctx::writeAndFlush
        );
    }
}
