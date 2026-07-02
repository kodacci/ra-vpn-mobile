package pro.ra_tech.ra_vpn.mobile.client.common.tun;

import androidx.annotation.Nullable;

import io.netty.channel.Channel;
import pro.ra_tech.ra_vpn.mobile.client.common.ip.IpHeader;

public interface ChannelSupplier {
    @Nullable
    Channel get(IpHeader ipHeader);
}
