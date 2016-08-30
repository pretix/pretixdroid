package eu.pretix.pretixdroid.net.api;

import android.content.Context;

import eu.pretix.pretixdroid.check.TicketCheckProvider;

public class PretixApi {
    public static final int API_VERSION = 2;
    private String url;
    private String key;

    public PretixApi(String url, String key) {
        this.url = url;
        this.key = key;
    }
}
