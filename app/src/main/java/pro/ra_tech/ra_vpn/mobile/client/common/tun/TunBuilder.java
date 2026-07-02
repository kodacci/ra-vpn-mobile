package pro.ra_tech.ra_vpn.mobile.client.common.tun;

import android.os.ParcelFileDescriptor;

public interface TunBuilder {
    ParcelFileDescriptor build(String virtualIp);
}
