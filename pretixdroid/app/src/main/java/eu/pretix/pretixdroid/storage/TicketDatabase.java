package eu.pretix.pretixdroid.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TicketDatabase extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "tickets";
    public static final String TICKETS_TABLE_NAME = "tickets";
    private static final String TICKETS_TABLE_CREATE =
            "CREATE TABLE " + TICKETS_TABLE_NAME + " (" +
                    "id TEXT primary key, " +
                    "item TEXT, " +
                    "variation TEXT, " +
                    "attendee_name TEXT" +
                    "redeemed INTEGER DEFAULT 0);" +
                    "CREATE UNIQUE INDEX ticket_id_index ON tickets (ticket_id);";


    public TicketDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TICKETS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
