package org.lightsys.crmapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by nathan on 3/9/16.
 */
public class CRMBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "crmBase.db";

    public CRMBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + CRMDbSchema.PartnerTable.NAME + "(" +
                        "_ID integer primary key autoincrement, " +
                        CRMDbSchema.PartnerTable.Cols.PARNTER_ID + ", " +
                        CRMDbSchema.PartnerTable.Cols.PARTNER_NAME +
                        ")"
        );

        db.execSQL("create table " + CRMDbSchema.CollaborateeTable.NAME + "(" +
                        "_ID integer primary key autoincrement, " +
                        CRMDbSchema.CollaborateeTable.Cols.COLLABORATEE_ID + ", " +
                        CRMDbSchema.CollaborateeTable.Cols.PARTNER_ID +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
