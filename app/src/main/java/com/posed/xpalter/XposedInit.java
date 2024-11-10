package com.posed.xpalter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 加载并初始化 AppPermissionTool，隐藏 USB 调试、无障碍权限、应用列表信息
        AppPermissionTool.init(lpparam);

        // 加载并初始化 DeviceSpoofTool，伪造系统设备信息
        DeviceSpoofTool deviceSpoofTool = new DeviceSpoofTool();
        deviceSpoofTool.handleLoadPackage(lpparam);
    }
}
