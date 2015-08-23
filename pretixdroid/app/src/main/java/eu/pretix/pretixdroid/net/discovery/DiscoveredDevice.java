package eu.pretix.pretixdroid.net.discovery;

import android.net.nsd.NsdServiceInfo;

public class DiscoveredDevice {
    public enum State {
        FOUND, VERIFIED, KEYMISMATCH, ERROR
    }

    private NsdServiceInfo serviceInfo;
    private State state = State.FOUND;
    private String fingerprint;

    public DiscoveredDevice(NsdServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public NsdServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(NsdServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
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
