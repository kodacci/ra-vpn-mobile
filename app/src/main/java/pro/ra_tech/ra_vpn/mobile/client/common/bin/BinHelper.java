package pro.ra_tech.ra_vpn.mobile.client.common.bin;

import static pro.ra_tech.ra_vpn.mobile.client.common.Constants.MAX_CLIENT_ID_LENGTH;

import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.val;

public interface BinHelper {
    static void hexDump(Logger log, byte[] buffer, int offset, int length) {}

    static InetAddress toInetAddress(byte[] bytes, int offset) throws UnknownHostException {
        val addr = new byte[4];
        System.arraycopy(bytes, offset, addr, 0, 4);

        return InetAddress.getByAddress(addr);
    }

    static String toClientId(byte[] bytes, int offset) {
        int idLength = MAX_CLIENT_ID_LENGTH;
        for (int i = offset; i < MAX_CLIENT_ID_LENGTH; ++i) {
            if (bytes[i] == 0) {
                idLength = i - offset;
                break;
            }
        }

        if (idLength == 0) {
            throw new IllegalArgumentException("CONNECT: Invalid client id");
        }

        return new String(bytes, offset, idLength);
    }
}
