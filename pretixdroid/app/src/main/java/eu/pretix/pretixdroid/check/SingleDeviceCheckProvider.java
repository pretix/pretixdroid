package eu.pretix.pretixdroid.check;

import android.content.Context;

public class SingleDeviceCheckProvider implements TicketCheckProvider {
    private Context ctx;

    public SingleDeviceCheckProvider(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public CheckResult check(String ticketid) {
        return new CheckResult(CheckResult.Type.ERROR, "Foobar");
    }
}
