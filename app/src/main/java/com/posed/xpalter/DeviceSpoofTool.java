package com.posed.xpalter;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DeviceSpoofTool implements IXposedHookLoadPackage {

    // 模拟的设备信息
    private static String fakeDeviceModel = "Pixel 5";
    private static String fakeBrand = "Google";
    private static String fakeManufacturer = "Google";
    private static String fakeDevice = "redfin";
    private static String fakeSerial = "FAKE123456789";
    private static String fakeAndroidVersion = "12";
    private static String fakeIMEI = "012345678912345";
    private static String fakeSimSerialNumber = "89014103211118510720";
    private static String fakeAndroidID = "abcdef1234567890";

    public static void setFakeDeviceInfo(String model, String brand, String manufacturer, String device, String serial) {
        fakeDeviceModel = model;
        fakeBrand = brand;
        fakeManufacturer = manufacturer;
        fakeDevice = device;
        fakeSerial = serial;
    }

    public static void setFakeAndroidInfo(String version, String androidID) {
        fakeAndroidVersion = version;
        fakeAndroidID = androidID;
    }

    public static void setFakeTelephonyInfo(String imei, String simSerialNumber) {
        fakeIMEI = imei;
        fakeSimSerialNumber = simSerialNumber;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        // 模拟设备信息
        XposedHelpers.setStaticObjectField(Build.class, "MODEL", fakeDeviceModel);
        XposedHelpers.setStaticObjectField(Build.class, "BRAND", fakeBrand);
        XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", fakeManufacturer);
        XposedHelpers.setStaticObjectField(Build.class, "DEVICE", fakeDevice);

        // 模拟 Android 版本
        XposedHelpers.setStaticObjectField(Build.VERSION.class, "RELEASE", fakeAndroidVersion);

        // 替换 Android ID
        XposedHelpers.findAndHookMethod(Settings.Secure.class, "getString", ContentResolver.class, String.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws InvocationTargetException, IllegalAccessException {
                        String name = (String) param.args[1];
                        if (Settings.Secure.ANDROID_ID.equals(name)) {
                            return fakeAndroidID;
                        }
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                    }
                });

        // 模拟 IMEI
        XposedHelpers.findAndHookMethod(TelephonyManager.class, "getDeviceId", XC_MethodReplacement.returnConstant(fakeIMEI));

        // 模拟 SIM 序列号
        XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimSerialNumber", XC_MethodReplacement.returnConstant(fakeSimSerialNumber));

        // 模拟硬件序列号
        XposedHelpers.findAndHookMethod(Build.class, "getSerial", XC_MethodReplacement.returnConstant(fakeSerial));
    }
}
