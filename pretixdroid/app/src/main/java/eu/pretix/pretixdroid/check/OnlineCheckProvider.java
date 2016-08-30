package eu.pretix.pretixdroid.check;

import android.content.Context;

public class OnlineCheckProvider implements TicketCheckProvider {
    private Context ctx;

    public OnlineCheckProvider(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public CheckResult check(String ticketid) {
        return new CheckResult(CheckResult.Type.ERROR);
    }
}
