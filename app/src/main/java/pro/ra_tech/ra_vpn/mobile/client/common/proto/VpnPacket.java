package pro.ra_tech.ra_vpn.mobile.client.common.proto;

import io.netty.buffer.ByteBuf;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.PacketEncryptor;

public interface VpnPacket {
    VpnPacketType getType();
    VpnPacketPayload getPayload();
}
