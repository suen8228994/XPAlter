package com.posed.xpalter;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrivacyTool implements IXposedHookLoadPackage {

    // 需要隐藏的应用包名集合
    private static final Set<String> hiddenApps = new HashSet<>();
    private static boolean hideUSBEnabled = false;
    private static boolean hideAccessibilityEnabled = false;

    // 方法1：启用/禁用 USB 调试隐藏
    public static void enableHideUSB(boolean enable) {
        hideUSBEnabled = enable;
    }

    // 方法2：启用/禁用无障碍服务隐藏
    public static void enableHideAccessibility(boolean enable, String packageName) {
        hideAccessibilityEnabled = enable;
        if (enable) {
            hiddenApps.add(packageName);
        } else {
            hiddenApps.remove(packageName);
        }
    }

    // 方法3：启用/禁用隐藏应用
    public static void hideApp(boolean enable, String packageName) {
        if (enable) {
            hiddenApps.add(packageName);
        } else {
            hiddenApps.remove(packageName);
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        // 隐藏 USB 调试信息
        if (hideUSBEnabled) {
            XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader,
                    "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String setting = (String) param.args[1];
                            if ("adb_enabled".equals(setting) || "development_settings_enabled".equals(setting)) {
                                param.setResult(0);  // 返回关闭状态
                            }
                        }
                    });
        }

        // 隐藏指定应用的无障碍服务状态
        if (hideAccessibilityEnabled && hiddenApps.contains(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod("android.view.accessibility.AccessibilityManager", lpparam.classLoader,
                    "isEnabled", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(false); // 强制返回 false
                        }
                    });
        }

        // 隐藏指定的应用
        if (!hiddenApps.isEmpty()) {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader,
                    "getInstalledApplications", int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            @SuppressWarnings("unchecked")
                            List<ApplicationInfo> originalList = (List<ApplicationInfo>) param.getResult();
                            List<ApplicationInfo> modifiedList = new ArrayList<>();
                            for (ApplicationInfo appInfo : originalList) {
                                if (!hiddenApps.contains(appInfo.packageName)) {
                                    modifiedList.add(appInfo);
                                }
                            }
                            param.setResult(modifiedList);
                        }
                    });

            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader,
                    "getInstalledPackages", int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            @SuppressWarnings("unchecked")
                            List<PackageInfo> originalList = (List<PackageInfo>) param.getResult();
                            List<PackageInfo> modifiedList = new ArrayList<>();
                            for (PackageInfo pkgInfo : originalList) {
                                if (!hiddenApps.contains(pkgInfo.packageName)) {
                                    modifiedList.add(pkgInfo);
                                }
                            }
                            param.setResult(modifiedList);
                        }
                    });
        }
    }
}
