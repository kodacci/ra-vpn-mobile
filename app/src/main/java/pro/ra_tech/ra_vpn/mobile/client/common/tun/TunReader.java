package pro.ra_tech.ra_vpn.mobile.client.common.tun;

public interface TunReader {
    void read() throws InterruptedException;
    String getTunName();
}