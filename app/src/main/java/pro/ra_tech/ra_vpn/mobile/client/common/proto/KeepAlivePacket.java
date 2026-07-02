package pro.ra_tech.ra_vpn.mobile.client.common.proto;

import lombok.RequiredArgsConstructor;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.payload.ConnectPayload;

@RequiredArgsConstructor
public class KeepAlivePacket implements VpnPacket {
    private final ConnectPayload payload;

    public static KeepAlivePacket fromBytes(byte[] bytes, int offset) {
        return new KeepAlivePacket(ConnectPayload.fromBytes(bytes, offset));
    }

    @Override
    public VpnPacketType getType() {
        return VpnPacketType.KEEP_ALIVE;
    }

    @Override
    public VpnPacketPayload getPayload() {
        return payload;
    }
}

