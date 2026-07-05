package pro.ra_tech.ra_vpn.mobile.client.common.crypto;

import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_PACKET_SIZE;
import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.SIGNATURE;
import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.VERSION_LENGTH;

import io.netty.channel.socket.DatagramPacket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pro.ra_tech.ra_vpn.mobile.client.common.bin.BinHelper;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.ConnectAckPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.ConnectPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.DataTransferPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.DisconnectPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.KeepAliveAckPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.KeepAlivePacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacket;
import pro.ra_tech.ra_vpn.mobile.client.common.proto.VpnPacketType;

import java.util.Arrays;

@Slf4j
public abstract class BaseEncryptor implements PacketEncryptor {
    protected static final int VERSION_OFFSET = SIGNATURE.length;
    protected static final int TYPE_OFFSET = VERSION_OFFSET + VERSION_LENGTH;
    protected static final int PAYLOAD_OFFSET = TYPE_OFFSET + 1;

    @Getter(AccessLevel.PROTECTED)
    private final byte[] buffer = new byte[MAX_PACKET_SIZE];

    protected  VpnPacket parseRawPacket(byte[] raw) {
        return parseRawPacket(raw, raw.length);
    }

    private VpnPacket parsePayload(VpnPacketType type, byte[] raw, int payloadOffset, int length) {
        switch (type) {
            case CONNECT: return ConnectPacket.fromBytes(raw, payloadOffset);
            case CONNECT_ACK: return ConnectAckPacket.fromBytes(raw, payloadOffset);
            case KEEP_ALIVE: return KeepAlivePacket.fromBytes(raw, payloadOffset);
            case KEEP_ALIVE_ACK: return KeepAliveAckPacket.fromBytes(raw, payloadOffset);
            case DATA_TRANSFER: return DataTransferPacket.fromBytes(raw, payloadOffset, length);
            case DISCONNECT: return DisconnectPacket.fromBytes(raw, payloadOffset);
            case UNKNOWN:
            default: throw new IllegalArgumentException("Unsupported vpn packet type " + raw[TYPE_OFFSET]);
        }
    }

    protected VpnPacket parseRawPacket(byte[] raw, int size) {
        BinHelper.hexDump(log, raw, 0, size);

        if (!Arrays.equals(raw, 0, SIGNATURE.length, SIGNATURE, 0, SIGNATURE.length)) {
            log.warn("Packet without signature, skipping ...");

            throw new IllegalArgumentException("Bad vpn signature");
        }

        val version = ((raw[VERSION_OFFSET] << 8) & 0xFF) + (raw[VERSION_OFFSET + 1] & 0xFF);
        if (version != 1) {
            log.warn("Unexpected VPN protocol version {}", version);

            throw new IllegalArgumentException("Bad version number: " + version);
        }

        val type = VpnPacketType.of(raw[TYPE_OFFSET]);
        val length = size - PAYLOAD_OFFSET;

        return parsePayload(type, raw, PAYLOAD_OFFSET, length);
    }

    protected VpnPacket parseRawHeadless(byte[] raw, int size) {
        val type = VpnPacketType.of(raw[0]);

        return parsePayload(type, raw, 1, size);
    }

    @Override
    public VpnPacket decrypt(DatagramPacket packet) {
        val size = packet.content().readableBytes();

        packet.content().readBytes(getBuffer(), 0, size);
        return decrypt(getBuffer(), size);
    }
}
