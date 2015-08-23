package eu.pretix.pretixdroid.net.discovery;

import android.net.nsd.NsdServiceInfo;

public class DiscoveredDevice {
    private NsdServiceInfo serviceInfo;

    public DiscoveredDevice(NsdServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public NsdServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(NsdServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiscoveredDevice that = (DiscoveredDevice) o;

        return !(serviceInfo != null
                ? !serviceInfo.getServiceName().equals(that.serviceInfo.getServiceName())
                : that.serviceInfo != null);

    }
}
