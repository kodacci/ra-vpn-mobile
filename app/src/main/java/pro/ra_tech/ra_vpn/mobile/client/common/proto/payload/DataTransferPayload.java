package pro.ra_tech.ra_vpn.mobile.client.common.proto.payload;

import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.Value;
import lombok.experimental.Accessors;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.common.ip.IpHeaderParser;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacketPayload;

@Value
@Accessors(fluent = true)
public class DataTransferPayload implements VpnPacketPayload {
    byte[] data;
    InetAddress srcAddress;
    InetAddress dstAddress;

    private static final IpHeaderParser parser = new IpHeaderParser();

    public static DataTransferPayload fromBytes(byte[] bytes, int offset, int length) {
        val payload = new byte[length];
        System.arraycopy(bytes, offset, payload, 0, length);
        val header = parser.parse(payload);
        try {
            return new DataTransferPayload(
                    payload,
                    header.srcAddress().toInetAddress(),
                    header.dstAddress().toInetAddress()
            );
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Bad DATA_TRANSFER payload", ex);
        }
    }

    @Override
    public byte[] toBytes() {
        return data;
    }
}
