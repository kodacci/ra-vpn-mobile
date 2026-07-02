package pro.ra_tech.ra_vpn.mobile.client.common.proto;

import java.net.InetAddress;

import lombok.RequiredArgsConstructor;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.payload.ConnectPayload;

@RequiredArgsConstructor
public class ConnectPacket implements VpnPacket {
    private final ConnectPayload payload;

    public static ConnectPacket fromBytes(byte[] bytes, int offset) {
        return new ConnectPacket(ConnectPayload.fromBytes(bytes, offset));
    }

    public ConnectPacket(String clientId, InetAddress src, InetAddress dst) {
        payload = new ConnectPayload(clientId, src, dst);
    }

    @Override
    public VpnPacketType getType() {
        return VpnPacketType.CONNECT;
    }

    @Override
    public ConnectPayload getPayload() {
        return payload;
    }
}
