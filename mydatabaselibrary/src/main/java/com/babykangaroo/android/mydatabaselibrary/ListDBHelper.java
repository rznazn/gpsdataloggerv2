package com.babykangaroo.android.mydatabaselibrary;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sport on 5/31/2017.
 */

public class ListDBHelper extends SQLiteOpenHelper {


    public static final String LOG_TAG = ListDBHelper.class.getName();
    private static final String DATABASE_NAME =  "directory.db";
    private static final int DATABASE_VERSION = 1;

    /** public constructor for new instance of the PetDbHelper*/
    public ListDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /** create two tables, one for the list titles and one for list items*/
    @Override
    public void onCreate(SQLiteDatabase db) {

        String SQL_CREATE_NEW_DIRECTORY_TABLE = "CREATE TABLE " + ListContract.ListContractEntry.DIRECTORY_TABLE_NAME +" ("
                + ListContract.ListContractEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ListContract.ListContractEntry.COLUMN_LOG_NAME + " TEXT UNIQUE NOT NULL);";

        String SQL_CREATE_NEW_ITEMS_TABLE = "CREATE TABLE " + ListContract.ListContractEntry.ITEMS_TABLE_NAME +" ("
                + ListContract.ListContractEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ListContract.ListContractEntry.COLUMN_ITEM_PARENT_LIST + " TEXT NOT NULL, "
                + ListContract.ListContractEntry.COLUMN_EVENT_KEYWORD + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_TIME + " TEXT, "
                + ListContract.ListContractEntry.COlUMN_TRACK_NUMBER + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_DIRECTIVE + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_LATITUDE + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_LONGITUDE + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_ALTITUDE + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_FIGURE_COLOR + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_BEARING_MAG + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_BEARING_FROM_LAST + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_RANGE + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_EVENT_END_TIME + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_GPS_ACCURACY + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_SPEED_FROM_LAST + " TEXT, "
                + ListContract.ListContractEntry.COLUMN_ITEM_NOTE + " TEXT);";

        db.execSQL(SQL_CREATE_NEW_DIRECTORY_TABLE);
        db.execSQL(SQL_CREATE_NEW_ITEMS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /** todo implement this method later if needed*/
    }
}
