package pro.ra_tech.ra_vpn.mobile.client.common.proto;

import java.net.InetAddress;

public interface VpnPacketPayload {
    byte[] toBytes();
    InetAddress srcAddress();
    InetAddress dstAddress();
}
