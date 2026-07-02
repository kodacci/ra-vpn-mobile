package pro.ra_tech.ra_vpn.mobile.client.common.proto.payload;

import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_CLIENT_ID_LENGTH;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.common.bin.BinHelper;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacketPayload;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

@Value
@Accessors(fluent = true)
public class ConnectAckPayload implements VpnPacketPayload {
    String clientId;
    InetAddress srcAddress;
    InetAddress dstAddress;

    private static final int PAYLOAD_LENGTH = MAX_CLIENT_ID_LENGTH + 8;

    public static ConnectAckPayload fromBytes(byte[] bytes, int offset) {
        if (bytes.length < PAYLOAD_LENGTH + offset) {
            throw new IllegalArgumentException("CONNECT_ACK: Insufficient packet length " + bytes.length);
        }

        try {
            val id = BinHelper.toClientId(bytes, offset);
            val src = BinHelper.toInetAddress(bytes, MAX_CLIENT_ID_LENGTH + offset);
            val dst = BinHelper.toInetAddress(bytes, MAX_CLIENT_ID_LENGTH + offset + 4);

            return new ConnectAckPayload(id, src, dst);
        } catch (Exception ex) {
            throw new IllegalArgumentException("CONNECT_ACK: Invalid payload", ex);
        }
    }

    @Override
    public byte[] toBytes() {
        val source = clientId.getBytes(StandardCharsets.UTF_8);
        val bytes = new byte[PAYLOAD_LENGTH];
        System.arraycopy(source, 0, bytes, 0, Math.min(source.length, MAX_CLIENT_ID_LENGTH));
        System.arraycopy(srcAddress.getAddress(), 0, bytes, MAX_CLIENT_ID_LENGTH, 4);
        System.arraycopy(dstAddress.getAddress(), 0, bytes, MAX_CLIENT_ID_LENGTH + 4, 4);

        return bytes;
    }
}
