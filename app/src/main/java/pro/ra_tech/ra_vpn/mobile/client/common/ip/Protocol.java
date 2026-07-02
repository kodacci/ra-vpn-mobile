package pro.ra_tech.ra_vpn.mobile.client.common.ip;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Protocol {
    ICMP(1),
    IGMP(2),
    GGP(3),
    TCP(6),
    UDP(17),
    NARP(54),
    OTHER(255);

    private final int value;

    public static Protocol of(int id) {
        switch (id) {
            case 1: return ICMP;
            case 2: return IGMP;
            case 3: return GGP;
            case 6: return TCP;
            case 17: return UDP;
            case 54: return NARP;
            default: return OTHER;
        }
    }
}
