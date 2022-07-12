package com.dyh.LocationModify;


import android.os.Bundle;
import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class LocationHook implements IXposedHookLoadPackage {
//    double latitude = 30;
//    double longitude = 114;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("LocationModify Hook: "+lpparam.packageName);
        // 保证不hook自己
        if (lpparam.packageName.equals("com.dyh.LocationModify")){
            return;
        }
        // 小米hook这两个包会间接hook自己
        if (lpparam.packageName.equals("com.miui.catcherpatch")){
            return;
        }
        if (lpparam.packageName.equals("com.miui.contentcatcher")){
            return;
        }
        HookUtils.StackTraceHook(lpparam);
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
            if (jsonObject.getInt("active") == 1){
                double latitude = jsonObject.getDouble("latitude") + 0.00005 * (1 - (Math.random() * 2));
                double longitude = jsonObject.getDouble("longitude") + 0.00005 * (1 - (Math.random() * 2));
                if (lpparam.packageName.equals("com.tencent.wework")) {
                    latitude -= 0.002082;
                    longitude +=0.005203;
                }
                HookUtils.GPSHook(lpparam, latitude, longitude);
            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}

class HookUtils{
    static void StackTraceHook(XC_LoadPackage.LoadPackageParam lpparam){
        XposedHelpers.findAndHookMethod(StackTraceElement.class, "getClassName", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String className = (String) param.getResult();
                XposedBridge.log("getClassName: "+className);
                if (className != null && (className.contains("xposed") || className.contains("EdHooker"))) {
                    param.setResult("android.os.Handler");
                }
                super.afterHookedMethod(param);
            }
        });
    }
    static void GPSHook(XC_LoadPackage.LoadPackageParam lpparam, double latitude, double longitude){
        XposedHelpers.findAndHookMethod("android.location.Location", lpparam.classLoader,
                "getLatitude", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (!lpparam.packageName.equals("com.dyh.LocationModify")){
                            param.setResult(latitude);
                        }
                    }
                });
        XposedHelpers.findAndHookMethod("android.location.Location", lpparam.classLoader,
                "getLongitude", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (!lpparam.packageName.equals("com.dyh.LocationModify")){
                            param.setResult(longitude);
                            XposedBridge.log(lpparam.packageName);
                        }
                    }
                });
        XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader,
                "getAllCellInfo", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
        XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader,
                "getCellLocation", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                });
        XposedHelpers.findAndHookMethod(LocationManager.class, "getLastLocation", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Location l = new Location(LocationManager.GPS_PROVIDER);
                l.setLatitude(latitude);
                l.setLongitude(longitude);
                l.setAccuracy(100f);
                l.setTime(System.currentTimeMillis());
                l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                param.setResult(l);
            }
        });

        XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Location l = new Location(LocationManager.GPS_PROVIDER);
                l.setLatitude(latitude);
                l.setLongitude(longitude);
                l.setAccuracy(100f);
                l.setTime(System.currentTimeMillis());
                l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                param.setResult(l);
            }
        });
        XposedBridge.hookAllMethods(LocationManager.class, "getProviders", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("gps");
                param.setResult(arrayList);
            }
        });
        XposedHelpers.findAndHookMethod(LocationManager.class, "addGpsStatusListener", GpsStatus.Listener.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null) {
                    XposedHelpers.callMethod(param.args[0], "onGpsStatusChanged", 1);
                    XposedHelpers.callMethod(param.args[0], "onGpsStatusChanged", 3);
                }
            }
        });
        XposedHelpers.findAndHookMethod("android.location.LocationManager", lpparam.classLoader,
                "getGpsStatus", GpsStatus.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        GpsStatus gss = (GpsStatus) param.getResult();
                        if (gss == null)
                            return;

                        Class<?> clazz = GpsStatus.class;
                        Method m = null;
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.getName().equals("setStatus")) {
                                if (method.getParameterTypes().length > 1) {
                                    m = method;
                                    break;
                                }
                            }
                        }
                        if (m == null)
                            return;

                        //access the private setStatus function of GpsStatus
                        m.setAccessible(true);

                        //make the apps belive GPS works fine now
                        int svCount = 5;
                        int[] prns = {1, 2, 3, 4, 5};
                        float[] snrs = {0, 0, 0, 0, 0};
                        float[] elevations = {0, 0, 0, 0, 0};
                        float[] azimuths = {0, 0, 0, 0, 0};
                        int ephemerisMask = 0x1f;
                        int almanacMask = 0x1f;

                        //5 satellites are fixed
                        int usedInFixMask = 0x1f;

                        XposedHelpers.callMethod(gss, "setStatus", svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
                        param.args[0] = gss;
                        param.setResult(gss);
                        try {
                            m.invoke(gss, svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
                            param.setResult(gss);
                        } catch (Exception e) {
                            XposedBridge.log(e);
                        }
                    }
                });


        for (Method method : LocationManager.class.getDeclaredMethods()) {
            if (method.getName().equals("requestLocationUpdates")
                    && !Modifier.isAbstract(method.getModifiers())
                    && Modifier.isPublic(method.getModifiers())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length >= 4 && (param.args[3] instanceof LocationListener)) {

                            LocationListener ll = (LocationListener) param.args[3];

                            Class<?> clazz = LocationListener.class;
                            Method m = null;
                            for (Method method : clazz.getDeclaredMethods()) {
                                if (method.getName().equals("onLocationChanged") && !Modifier.isAbstract(method.getModifiers())) {
                                    m = method;
                                    break;
                                }
                            }
                            Location l = new Location(LocationManager.GPS_PROVIDER);
                            l.setLatitude(latitude);
                            l.setLongitude(longitude);
                            l.setAccuracy(10.00f);
                            l.setTime(System.currentTimeMillis());
                            l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            XposedHelpers.callMethod(ll, "onLocationChanged", l);
                            try {
                                if (m != null) {
                                    m.invoke(ll, l);
                                }
                            } catch (Exception e) {
                                XposedBridge.log(e);
                            }
                        }
                    }
                });
            }
            if (method.getName().equals("requestSingleUpdate ")
                    && !Modifier.isAbstract(method.getModifiers())
                    && Modifier.isPublic(method.getModifiers())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length >= 3 && (param.args[1] instanceof LocationListener)) {

                            LocationListener ll = (LocationListener) param.args[3];

                            Class<?> clazz = LocationListener.class;
                            Method m = null;
                            for (Method method : clazz.getDeclaredMethods()) {
                                if (method.getName().equals("onLocationChanged") && !Modifier.isAbstract(method.getModifiers())) {
                                    m = method;
                                    break;
                                }
                            }

                            try {
                                if (m != null) {
                                    Location l = new Location(LocationManager.GPS_PROVIDER);
                                    l.setLatitude(latitude);
                                    l.setLongitude(longitude);
                                    l.setAccuracy(100f);
                                    l.setTime(System.currentTimeMillis());
                                    l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                                    m.invoke(ll, l);
                                }
                            } catch (Exception e) {
                                XposedBridge.log(e);
                            }
                        }
                    }
                });
            }
        }

    }
}
