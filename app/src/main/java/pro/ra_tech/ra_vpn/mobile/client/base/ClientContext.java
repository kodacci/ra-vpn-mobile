package pro.ra_tech.ra_vpn.mobile.client.base;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

import lombok.Value;
import lombok.experimental.Accessors;
import pro.ra_tech.ra_vpn.mobile.client.common.crypto.PacketEncryptor;
import pro.ra_tech.ra_vpn.mobile.client.common.tun.VpnTunDevice;
import pro.ra_tech.ra_vpn.mobile.client.event.Connector;

@Value
@Accessors(fluent = true)
public class ClientContext {
        InetSocketAddress server;
        VpnTunDevice tun;
        Supplier<PacketEncryptor> encryptorSupplier;
        Connector connector;
        String clientId;
}
