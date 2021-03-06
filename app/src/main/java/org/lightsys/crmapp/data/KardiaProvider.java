package org.lightsys.crmapp.data;

import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Objects;

/**
 * Created by nathan on 3/10/16.
 *
 * Commented by Judah on 7/26/16.
 * This is the class that is supposed to talk to the database
 * but as far as we can tell it is not working
 * we are not sure if the problem is with this class or with something else
 * all we know is that when we call a query, it doesn't return anything
 *
 *
 * this class holds methods for query, insert, delete, and update
 */
public class KardiaProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private CRMOpenHelper mOpenHelper;

    private SQLiteDatabase mDatabase;

    private Context mContext;
    private AccountManager mAccountManager;

    static {
        //things used to identify what tables we are dealing with
        sUriMatcher.addURI(CRMContract.providerAuthority, "staff", 1);
        sUriMatcher.addURI(CRMContract.providerAuthority, "collaboratees", 2);
        sUriMatcher.addURI(CRMContract.providerAuthority, "timeline", 3);
    }


    @Override
    public boolean onCreate() {
        mOpenHelper = new CRMOpenHelper(getContext());
        mDatabase = mOpenHelper.getWritableDatabase();

        return true;
    }

    //function that queries the database
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        //determine what table we are dealing with
        switch (sUriMatcher.match(uri)) {
            case 1:
                builder.setTables(CRMContract.StaffTable.TABLE_NAME);
                break;
            case 2:
                builder.setTables(CRMContract.CollaborateeTable.TABLE_NAME);
                break;
            case 3:
                builder.setTables(CRMContract.TimelineTable.TABLE_NAME);
                break;
            default:
                break;
        }

        return builder.query(mDatabase, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    //function that inserts stuffs into the database
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table;
        int id;

        //determine what table we are dealing with
        switch (sUriMatcher.match(uri)) {
            case 1:
                table = CRMContract.StaffTable.TABLE_NAME;
                id = values.getAsInteger(CRMContract.StaffTable.PARTNER_ID);
                break;
            case 2:
                table = CRMContract.CollaborateeTable.TABLE_NAME;
                id = values.getAsInteger(CRMContract.CollaborateeTable.COLLABORATER_ID);
                break;
            case 3:
                table = CRMContract.TimelineTable.TABLE_NAME;
                id = values.getAsInteger(CRMContract.TimelineTable.CONTACT_HISTORY_ID);
                break;
            default:
                table = "";
                id = 0;
                break;
        }

        mDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        return ContentUris.withAppendedId(uri, id);
    }

    //function that deletes stuffs from the database
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    //function that updates stuffs in the database
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String table;

        //determine what table we are dealing with
        switch (sUriMatcher.match(uri)) {
            case 1:
                table = CRMContract.StaffTable.TABLE_NAME;
                break;
            case 2:
                table = CRMContract.CollaborateeTable.TABLE_NAME;
                break;
            case 3:
                table = CRMContract.TimelineTable.TABLE_NAME;
                break;
            default:
                table = "";
                break;
        }

        return mDatabase.update(table, values, selection, selectionArgs);
    }
}