package com.dyh.LocationModify;

import android.location.Location;
import android.os.Parcel;

public class LocationStruct {
    private int id;
    private String name;
    private String alias;
    private byte[] data;

    public LocationStruct(int id, String name, String alias, byte[] data) {
        this.id = id;
        this.name = name;
        this.alias = alias;
        this.data = data;
    }

    public Location getLocation(){
        Parcel parcel = Parcel.obtain();
        //Log.e("load", Arrays.toString(data));
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0);
        Location location = Location.CREATOR.createFromParcel(parcel);
        //Log.e("load", location.toString());
        parcel.recycle();
        return location;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
