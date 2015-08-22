package eu.pretix.pretixdroid.check;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import eu.pretix.pretixdroid.storage.TicketDatabase;
import eu.pretix.pretixdroid.storage.TicketProvider;

public class SingleDeviceCheckProvider implements TicketCheckProvider {
    private Context ctx;

    public SingleDeviceCheckProvider(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public CheckResult check(String ticketid) {
        Cursor cursor = ctx.getContentResolver().query(
                TicketProvider.CONTENT_URI.buildUpon().appendEncodedPath(ticketid).build(),
                TicketDatabase.TICKETS_TABLE_COLUMNS, null, null, null);

        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                CheckResult.Type type = (cursor.getLong(4) == 0 ? CheckResult.Type.VALID : CheckResult.Type.USED);
                CheckResult res = new CheckResult(type);
                res.setTicket(cursor.getString(1));
                res.setVariation(cursor.getString(2));
                res.setAttendee_name(cursor.getString(3));

                ContentValues values = new ContentValues();
                values.put("redeemed", 1);
                if(ctx.getContentResolver().update(
                        TicketProvider.CONTENT_URI.buildUpon().appendEncodedPath(ticketid).build(),
                        values, null, null) != 1) {
                    res.setType(CheckResult.Type.ERROR);
                    res.setMessage("Could not mark ticket as redeemed.");
                }
                return res;
            } else {
                return new CheckResult(CheckResult.Type.INVALID);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new CheckResult(CheckResult.Type.ERROR, e.getMessage());
        } finally {
            cursor.close();
        }
    }
}
