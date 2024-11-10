package com.posed.xpalter;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashSet;
import java.util.Set;

public class AppPermissionTool {

    private static boolean hideUsbDebugging = false;
    private static final Set<String> hiddenAccessibilityPackages = new HashSet<>();
    private static final Set<String> hiddenAppPackages = new HashSet<>();

    public static void setHideUsbDebugging(boolean enable) {
        hideUsbDebugging = enable;
    }

    public static void setHideAccessibilityPermission(String packageName, boolean enable) {
        if (enable) {
            hiddenAccessibilityPackages.add(packageName);
        } else {
            hiddenAccessibilityPackages.remove(packageName);
        }
    }

    public static void setHideAppFromList(String packageName, boolean enable) {
        if (enable) {
            hiddenAppPackages.add(packageName);
        } else {
            hiddenAppPackages.remove(packageName);
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hideUsbDebugging) {
            // Hook系统方法来隐藏USB调试状态
            // 这里的实现会根据具体需求而不同
        }

        // 隐藏无障碍服务权限
        if (!hiddenAccessibilityPackages.isEmpty()) {
            // Hook AccessibilityManager.isEnabled() 来隐藏无障碍权限状态
        }

        // 隐藏指定的应用包
        if (!hiddenAppPackages.isEmpty()) {
            // Hook PackageManager 的 getInstalledApplications 和 getInstalledPackages 方法
        }
    }
}
