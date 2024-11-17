package com.posed.xpalter.usb;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.posed.xpalter.BuildConfig;

import org.json.JSONArray;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Rui Li on 12/5/2016.
 */
public class XposedMain implements IXposedHookLoadPackage {
	public static final String TICKED_USB_APPS="ticked_usb_apps";
	public static final String PREFERENCE_KEY_NEVER_SHOW_DISCLAIMER = "never_show_disclaimer";

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
		if (loadPackageParam.packageName.equals(BuildConfig.APPLICATION_ID)) {
			Log.d("XposedMain", "hiderRoot: 0");
			XposedHelpers.findAndHookMethod(
					"com.posed.xpalter.hider.ui.HiderRootActivity", loadPackageParam.classLoader,
					"isEnabled", XC_MethodReplacement.returnConstant(true)
			);
		}
//		Log.d("XposedMain", "hiderRoot: 1");
		XposedBridge.log("XposedMain   hiderRoot");
		XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				Context context = (Context) param.args[0];
				if (context != null) {
					loadPackageParam.classLoader = context.getClassLoader();
					try {
						invokeHandleHookMethod(
								context, BuildConfig.APPLICATION_ID,
								BuildConfig.APPLICATION_ID + ".XposedHook",
								"handleLoadPackage", loadPackageParam);
					} catch (Throwable error) {
						error.printStackTrace();
					}
				}
			}
		});
		XposedBridge.log("hideUSBDebugging: hook " + loadPackageParam.packageName);
		// 隐藏开发者模式
		XposedHelpers.findAndHookMethod("android.provider.Settings$Global", loadPackageParam.classLoader,
				"getInt", android.content.ContentResolver.class, String.class, int.class, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String setting = (String) param.args[1];
						if (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.equals(setting)) {
							XposedBridge.log("隐藏开发者模式状态，返回关闭状态");
							Log.d("XposedInit", "beforeHookedMethod: 隐藏开发者模式状态，返回关闭状态");
							param.setResult(0);  // 强制返回 0，表示开发者模式未开启
						}
					}
				});
		//隐藏无障碍
		XposedHelpers.findAndHookMethod("android.view.accessibility.AccessibilityManager", loadPackageParam.classLoader,
				"isEnabled", XC_MethodReplacement.returnConstant(false));
		//隐藏USB调试
		XposedHelpers.findAndHookMethod("android.provider.Settings.Global", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("hideUSBDebugging: hook Settings.Global.getInt(3) method");
				if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});
		//隐藏USB调试
		XposedHelpers.findAndHookMethod("android.provider.Settings.Global", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("hideUSBDebugging: hook Settings.Global.getInt(2) method");
				if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});
		//隐藏USB调试
		XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//				XposedBridge.log("hideUSBDebugging: hook Settings.Secure.getInt(3) method");
				if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});
		//隐藏USB调试
		XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: ADB_ENABLED");
				if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});
		//隐藏USB调试
		XposedHelpers.findAndHookMethod("android.provider.Settings$Global", loadPackageParam.classLoader,
				"getInt", android.content.ContentResolver.class, String.class, int.class, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String setting = (String) param.args[1];
						if (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.equals(setting)) {
							XposedBridge.log("隐藏开发者模式状态，返回关闭状态");
							Log.d("XposedInit", "beforeHookedMethod: 隐藏开发者模式状态，返回关闭状态");
							param.setResult(0);  // 强制返回 0，表示开发者模式未开启
						}
					}
				});


		// 隐藏 su 二进制文件
		XposedHelpers.findAndHookMethod(
				"java.io.File",
				loadPackageParam.classLoader,
				"exists",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

						String path = ((File) param.thisObject).getAbsolutePath();
						if (path.contains("/su") || path.contains("/busybox")) {
							Log.d("XposedInit", "beforeHookedMethod: 隐藏root "+param.toString());
							param.setResult(false); // 隐藏 Root 文件
						}
					}
				}
		);

		// 隐藏 Root 检测命令
		XposedHelpers.findAndHookMethod(
				"java.lang.Runtime",
				loadPackageParam.classLoader,
				"exec",
				String[].class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

						String[] commands = (String[]) param.args[0];
						for (String command : commands) {
							if (command.contains("su") || command.contains("which su")) {
								Log.d("XposedInit", "beforeHookedMethod: 隐藏root检测命令");
								param.setThrowable(new RuntimeException("Command not allowed"));
							}
						}
					}
				}
		);

		// 隐藏系统属性
		XposedHelpers.findAndHookMethod(
				"android.os.SystemProperties",
				loadPackageParam.classLoader,
				"get",
				String.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

						String key = (String) param.args[0];
						if ("ro.debuggable".equals(key) || "ro.secure".equals(key)) {
							Log.d("XposedInit", "beforeHookedMethod: 隐藏系统属性");
							param.setResult("0"); // 模拟为非 Root 状态
						}
					}
				}
		);

		// 拦截 Runtime.exec("su")
		XposedHelpers.findAndHookMethod(
				"java.lang.Runtime",
				loadPackageParam.classLoader,
				"exec",
				String.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String command = (String) param.args[0];
						if (command.equals("su")) {
							// 阻止 root 检测
							param.setThrowable(new Exception("Root access denied"));
						}
					}
				}
		);
		XposedHelpers.findAndHookMethod(
				File.class,
				"exists",
				new XC_MethodReplacement() {
					@Override
					protected Object replaceHookedMethod(MethodHookParam param) throws InvocationTargetException, IllegalAccessException {
						String path = ((File) param.thisObject).getPath();
						if (path.contains("su")) {
							return false;
						}
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
				}
		);
		XposedBridge.hookAllMethods(File.class, "exists", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				String path = ((File) param.thisObject).getAbsolutePath();
				if (path.contains("/su") || path.contains("/busybox")) {
					Log.d("XposedInit", "beforeHookedMethod: 隐藏root"+param.toString());
					param.setResult(false);
				}
			}
		});
	}

	private void hiderRoot(XC_LoadPackage.LoadPackageParam loadPackageParam){

	}

	private void invokeHandleHookMethod(
			Context context,
			String modulePackageName,
			String handleHookClass,
			String handleHookMethod,
			XC_LoadPackage.LoadPackageParam loadPackageParam
	) throws Throwable {
		// 原来的两种方式不是很好,改用这种新的方式
		File apkFile = findApkFile(context, modulePackageName);
		if (apkFile == null) {
			throw new RuntimeException("Cannot find the module APK.");
		}
		// 加载指定的hook逻辑处理类，并调用它的handleHook方法
		PathClassLoader pathClassLoader =
				new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
		Class<?> cls = Class.forName(handleHookClass, true, pathClassLoader);
		Object instance = cls.newInstance();
		Method method = cls.getDeclaredMethod(handleHookMethod,
				Context.class, XC_LoadPackage.LoadPackageParam.class);
		method.invoke(instance, context, loadPackageParam);
	}

	/**
	 * 根据包名构建目标Context,并调用getPackageCodePath()来定位apk
	 *
	 * @param context           context参数
	 * @param modulePackageName 当前模块包名
	 * @return return apk file
	 */
	private File findApkFile(Context context, String modulePackageName) {
		if (context == null) {
			return null;
		}
		try {
			Context moduleContext = context.createPackageContext(
					modulePackageName,
					Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
			String apkPath = moduleContext.getPackageCodePath();
			return new File(apkPath);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}
