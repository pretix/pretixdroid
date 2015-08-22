package eu.pretix.pretixdroid.storage;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;

public class TicketProvider extends ContentProvider {
    private TicketDatabase database;

    private static final int TICKETS = 10;
    private static final int TICKET_ID = 20;

    private static final String AUTHORITY = "eu.pretix.pretixdroid.ticketprovider";

    private static final String BASE_PATH = "tickets";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/tickets";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/ticket";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, TICKETS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/*", TICKET_ID);
    }

    @Override
    public boolean onCreate() {
        database = new TicketDatabase(getContext());
        return false;
    }

    private void checkColumns(String[] projection) {
        /**
         * Checks if the caller has requested a column which does not exists
         */
        String[] available = {"id", "item", "variation", "attendee_name", "redeemed"};
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(TicketDatabase.TICKETS_TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case TICKETS:
                break;
            case TICKET_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere("id = ");
                queryBuilder.appendWhereEscapeString(uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        switch (uriType) {
            case TICKETS:
                try {
                    sqlDB.insertOrThrow(TicketDatabase.TICKETS_TABLE_NAME, null, values);
                } catch (SQLException e) {
                    sqlDB.update(TicketDatabase.TICKETS_TABLE_NAME, values,
                            "id = ?", new String[]{values.getAsString("id")});
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + values.get("id"));
    }

    public static <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case TICKETS:
                rowsDeleted = sqlDB.delete(TicketDatabase.TICKETS_TABLE_NAME, selection,
                        selectionArgs);
                break;
            case TICKET_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(TicketDatabase.TICKETS_TABLE_NAME,
                            "id = ?", new String[]{id});
                } else {
                    if (selectionArgs == null) {
                        selectionArgs = new String[]{id};
                    } else {
                        selectionArgs = concatenate(new String[]{id}, selectionArgs);
                    }
                    rowsDeleted = sqlDB.delete(TicketDatabase.TICKETS_TABLE_NAME,
                            "id = ? and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case TICKETS:
                rowsUpdated = sqlDB.update(TicketDatabase.TICKETS_TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case TICKET_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(TicketDatabase.TICKETS_TABLE_NAME,
                            values, "id = ?", new String[]{id});
                } else {
                    if (selectionArgs == null) {
                        selectionArgs = new String[]{id};
                    } else {
                        selectionArgs = concatenate(new String[]{id}, selectionArgs);
                    }
                    rowsUpdated = sqlDB.update(TicketDatabase.TICKETS_TABLE_NAME,
                            values, "id = ? and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
