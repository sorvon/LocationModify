package com.dyh.LocationModify;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.provider.DocumentsContract;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Location currentLocation = null;
    List<LocationStruct> locationList = null;
    LocationAdapter locationAdapter = null;
    LocationDatabase locationDatabase = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File path = new File(documentsDir, "LocationModify");
        File file = new File(path, "tmpLocation.json");
        path.mkdirs();
        try {
            InputStream inputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            inputStream.read(data);
            inputStream.close();
            Log.e("json", new String(data));
            JSONObject jsonObject = new JSONObject(new String(data));
            jsonObject.put("active", 0);
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(jsonObject.toString().getBytes());
            outputStream.close();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        //database init
        locationDatabase = new LocationDatabase(this);

        //recycleView init
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        locationAdapter = new LocationAdapter(new ArrayList<>(), this);

        recyclerView.setAdapter(locationAdapter);
        try {
            reloadLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 202);

//        try {
//            Runtime.getRuntime().exec("su");
//        } catch (IOException e) {
//            Toast.makeText(this, "没root就算了吧", Toast.LENGTH_SHORT).show();
//            e.printStackTrace();
//        }

        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 201);

        LocationManager locationManager = null;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "没有定位权限，可去设置中手动开启", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 201);
            return;
        }
        GpsLocationListener gpsLocationListener = new GpsLocationListener();
        NetLocationListener netLocationListener = new NetLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, gpsLocationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, netLocationListener);


    }

    public void saveButtonClicked(View view) throws FileNotFoundException {
        TextView locationView = findViewById(R.id.textView);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "没有定位权限，可去设置中手动开启", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 201);
            return;
        }
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        Log.e("cellInfoList", String.valueOf(cellInfoList.size()));
        for (CellInfo cellInfo:cellInfoList){
            Log.e("cellInfoList", cellInfo.toString());

        }

        JSONObject jsonCellPos = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        byte[]data = new byte[0];
        try {
            jsonObject.put("11", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }



        if (currentLocation == null) {
            Toast.makeText(this, "正在定位中...", Toast.LENGTH_SHORT).show();
        } else {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = new ArrayList<>();
            try {
                addressList = geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 10);
                Log.e("save", String.valueOf(addressList.size()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            StringBuilder addressStrBuilder = new StringBuilder("");
            for (Address address:addressList){

                for (int i=0; i < address.getMaxAddressLineIndex(); i++){
                    addressStrBuilder.append(address.getAddressLine(i)).append("\n");
                }
                Log.e("save", addressStrBuilder.toString());
                String s = address.getThoroughfare();
                if (s != null){
                    Log.e("save", s);
                }
            }
            Parcel parcel = Parcel.obtain();
            currentLocation.writeToParcel(parcel, 0);
            Log.e("save", currentLocation.toString());

            locationDatabase.insertLocation(addressStrBuilder.toString(), "", parcel.marshall());
            try {
                reloadLocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
            parcel.recycle();

            currentLocation = null;
            Toast.makeText(this, "保存位置", Toast.LENGTH_SHORT).show();
        }
    }

    public void aliasButtonClicked(View view){
        locationAdapter.changeAlias(locationDatabase);
    }
    public void deleteButtonClicked(View view){
        int reId = locationAdapter.removeItem();
        locationDatabase.delById(reId);
        Toast.makeText(this, "删除位置", Toast.LENGTH_SHORT).show();
    }

    public void reloadLocation() throws IOException {
        locationAdapter.reloadItem(locationDatabase.getAll());
    }

    public void beginButtonClicked(View view) {
        //File externalCacheFile = new File(context.getExternalCacheDir(), filename);
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File path = new File(documentsDir, "LocationModify");
        File file = new File(path, "tmpLocation.json");
        path.mkdirs();



        int locationId = locationAdapter.getCurrentLocationId();
        if (locationId == -1){
            Toast.makeText(this, "请先选择一个位置", Toast.LENGTH_SHORT).show();
            return;
        }
        Button beginButton = findViewById(R.id.beginButton);
        if (beginButton.getText().toString().equals("开始定位")){
            Location loc = locationDatabase.getLocById(locationId);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("active", 1);
                jsonObject.put("latitude", loc.getLatitude());
                jsonObject.put("longitude", loc.getLongitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                OutputStream outputStream = new FileOutputStream(file);
                outputStream.write(jsonObject.toString().getBytes());
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            beginButton.setText("停止定位");
            beginButton.setTextColor(0xffff0000);
            setViewAndChildrenEnabled(findViewById(R.id.recyclerView), false);
            setViewAndChildrenEnabled(findViewById(R.id.briefSwitch), false);
            setViewAndChildrenEnabled(findViewById(R.id.saveButton), false);
            setViewAndChildrenEnabled(findViewById(R.id.deleteButton), false);
            setViewAndChildrenEnabled(findViewById(R.id.aliasButton), false);
            Toast.makeText(this, "开始定位", Toast.LENGTH_SHORT).show();
        } else {
            try {
                InputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);
                inputStream.close();
                Log.e("json", new String(data));
                JSONObject jsonObject = new JSONObject(new String(data));
                jsonObject.put("active", 0);
                OutputStream outputStream = new FileOutputStream(file);
                outputStream.write(jsonObject.toString().getBytes());
                outputStream.close();

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            beginButton.setText("开始定位");
            beginButton.setTextColor(0xffffffff);
            setViewAndChildrenEnabled(findViewById(R.id.recyclerView), true);
            setViewAndChildrenEnabled(findViewById(R.id.briefSwitch), true);
            setViewAndChildrenEnabled(findViewById(R.id.saveButton), true);
            setViewAndChildrenEnabled(findViewById(R.id.deleteButton), true);
            setViewAndChildrenEnabled(findViewById(R.id.aliasButton), true);
            Toast.makeText(this, "停止定位", Toast.LENGTH_SHORT).show();
        }
    }

    private void setViewAndChildrenEnabled(View view, boolean enabled){
        view.setEnabled(enabled);
        if (view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++){
                View childView = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(childView, enabled);
            }
        }
    }


    private class NetLocationListener implements LocationListener{

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (currentLocation == null){
                currentLocation = location;
            } else if (currentLocation.getTime()-location.getTime()>9000){
                currentLocation = location;
                Toast.makeText(getApplicationContext(), "切换为network定位"+location.getLatitude(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {

        }
    }

    private class GpsLocationListener implements LocationListener{

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (currentLocation != null){
                if (!currentLocation.getProvider().equals("gps")){
                    Toast.makeText(getApplicationContext(), "切换为GPS定位", Toast.LENGTH_SHORT).show();
                }
            }
            currentLocation = location;
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {

        }
    }
}