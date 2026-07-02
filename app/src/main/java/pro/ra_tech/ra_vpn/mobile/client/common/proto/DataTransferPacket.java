package pro.ra_tech.ra_vpn.mobile.client.common.proto;

import java.net.InetAddress;

import lombok.RequiredArgsConstructor;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.payload.DataTransferPayload;

@RequiredArgsConstructor
public class DataTransferPacket implements VpnPacket {
    private final DataTransferPayload payload;

    public static DataTransferPacket fromBytes(byte[] bytes, int offset, int length) {
        return new DataTransferPacket(DataTransferPayload.fromBytes(bytes, offset, length));
    }

    public DataTransferPacket(byte[] data, InetAddress src, InetAddress dst) {
        payload = new DataTransferPayload(data, src, dst);
    }

    @Override
    public VpnPacketType getType() {
        return VpnPacketType.DATA_TRANSFER;
    }

    @Override
    public DataTransferPayload getPayload() {
        return payload;
    }
}
