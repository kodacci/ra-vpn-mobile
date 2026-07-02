package pro.ra_tech.ra_vpn.mobile.client.udp;

import java.net.DatagramSocket;

public interface UdpSocketProtector {
    void protect(DatagramSocket socket);
}
