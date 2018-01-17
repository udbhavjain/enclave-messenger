package com.jain.udbhav.enclave;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by udbhav on 4/7/17.
 */

public class MsgStoreHelper extends SQLiteOpenHelper {


    private static final int VERSION = 1;
    private static final String DBNAME = "MsgStore.db";
    private static final String MSGID = "msgID";
    private static final String TIME = "time";
    private static final String DATE = "date";
    private static final String SENDER = "sender";
    private static final String HASH = "hash";
    private static final String MSG = "msg";
    private static final String TABLE = "messages";


    public MsgStoreHelper(Context context)
    {
        super(context,DBNAME,null,VERSION);
    }

    public MsgStoreHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table " + TABLE +"("
        + MSGID + ","
        + DATE + ","
        + TIME + ","
        + SENDER + ","
        + HASH + ","
        + MSG + ")");

    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
