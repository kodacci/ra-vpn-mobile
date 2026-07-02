package pro.ra_tech.ra_vpn.mobile.client.common.ip;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class IpHeader {
        Protocol proto;
        Ip4Address srcAddress;
        Ip4Address dstAddress;
}
