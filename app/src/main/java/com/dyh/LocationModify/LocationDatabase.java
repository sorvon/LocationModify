package com.dyh.LocationModify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Parcel;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocationDatabase {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "LocationDatabase";
    private static final String TABLE_NAME = "Location";
    SQLiteDatabase database = null;

    public LocationDatabase(Context context) {
        DBHelper dbHelper = new DBHelper(context, DB_NAME, null, DB_VERSION);
        database = dbHelper.getWritableDatabase();
    }

    public static String getDbName() {
        return DB_NAME;
    }

    public void insertLocation(String name, String alias, byte[] data){
        database.beginTransaction();

        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("alias", alias);
        contentValues.put("data", data);
        database.insert(TABLE_NAME, null, contentValues);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public List<LocationStruct> getAll(){
        Cursor cursor = database.rawQuery("select * from "+TABLE_NAME, null);
        List<LocationStruct> locationStructList = null;
        if (cursor.getCount()>0) {
            locationStructList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()){
                LocationStruct locationStruct = new LocationStruct(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getBlob(3));
                locationStructList.add(locationStruct);
            }
        }
        cursor.close();
        return locationStructList;
    }
    public int getCount(){
        Cursor cursor = database.rawQuery("select * from "+TABLE_NAME, null);
        int tmp = cursor.getCount();
        cursor.close();
        return cursor.getCount();
    }

    public Location getLocById(int id){
        Cursor cursor = database.rawQuery("select data from "+TABLE_NAME+" where id = "+id, null);
        Pair<Double, Double> loc = null;
        Location location = null;
        if (cursor.getCount()>0){
            cursor.moveToNext();
            byte[] data = cursor.getBlob(0);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            location = Location.CREATOR.createFromParcel(parcel);
        }
        cursor.close();
        return location;
    }

    public void delById(int id){
        database.beginTransaction();

        String delSQL = "delete from "+TABLE_NAME+" where id = "+id;
        database.execSQL(delSQL);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void changeAliasById(int id, String alias){
        database.beginTransaction();

        String changeSQL = "update "+TABLE_NAME+" set alias = \""+alias+"\" where id = "+id;
        database.execSQL(changeSQL);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            Log.e("db","create");
            String createTableSQL =
                    "create table Location" +
                            "(id integer primary key autoincrement, name text, alias text, data blob)";
            sqLiteDatabase.execSQL(createTableSQL);

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }
}
