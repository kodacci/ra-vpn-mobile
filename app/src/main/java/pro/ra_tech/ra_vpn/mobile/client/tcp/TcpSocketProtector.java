package pro.ra_tech.ra_vpn.mobile.client.tcp;

import java.net.Socket;

public interface TcpSocketProtector {
    void protect(Socket socket);
}
