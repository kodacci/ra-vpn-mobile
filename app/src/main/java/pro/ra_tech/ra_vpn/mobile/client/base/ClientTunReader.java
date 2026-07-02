package pro.ra_tech.ra_vpn.mobile.client.base;

import io.netty.channel.Channel;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.DataTransferPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.BaseTunReader;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.ChannelSupplier;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.VpnTunDevice;

public class ClientTunReader extends BaseTunReader {
    public ClientTunReader(
            VpnTunDevice tunDevice,
            ChannelSupplier channelSupplier
    ) {
        super(tunDevice, channelSupplier);
    }

    @Override
    protected void sendPacket(Channel channel, DataTransferPacket packet) {
        channel.pipeline().fireUserEventTriggered(packet);
    }
}
