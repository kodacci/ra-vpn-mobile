package pro.ra_tech.ra_vpn.mobile.client.common.proto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VpnPacketType {
    CONNECT((byte) 0),
    CONNECT_ACK((byte) 1),
    DISCONNECT((byte) 2),
    KEEP_ALIVE((byte) 3),
    KEEP_ALIVE_ACK((byte) 4),
    DATA_TRANSFER((byte) 5),
    UNKNOWN((byte) 255);

    @Getter
    private final byte code;

    public static VpnPacketType of(byte code) {
        switch (code) {
            case 0: return CONNECT;
            case 1: return CONNECT_ACK;
            case 2: return DISCONNECT;
            case 3: return KEEP_ALIVE;
            case 4: return KEEP_ALIVE_ACK;
            case 5: return DATA_TRANSFER;
            default: return UNKNOWN;
        }
    }
}
