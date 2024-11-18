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
		XposedHelpers.findAndHookMethod(
				"android.app.Application",
				loadPackageParam.classLoader,
				"attach",
				Context.class,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Log.d("XposedMain", "exists: hook loadPackageParam.packageName "+loadPackageParam.packageName);
						Context context = (Context) param.args[0];
						String packageName = context.getPackageName();

						if (packageName.equals("your.app.package")) {
							XposedBridge.log("Target app attached in hidden mode");
							// Hook 目标方法
							XposedHelpers.findAndHookMethod(
									File.class,
									"exists",
									new XC_MethodHook() {
										@Override
										protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
											XposedBridge.log("File.exists() was called in hidden mode.");
											param.setResult(false);
										}
									}
							);
						}
					}
				}
		);

		XposedBridge.log("exists: hook ");
		Log.d("XposedMain", "exists: hook");
		hiderSU(loadPackageParam);
		hiderRoot(loadPackageParam);
		hiderDevelop(loadPackageParam);
		hiderAccessible(loadPackageParam);
		hiderUSB(loadPackageParam);

		hiderSystemNature(loadPackageParam);
	}

	/**
	 * 隐藏系统属性
	 * @param loadPackageParam
	 */
	private void hiderSystemNature(XC_LoadPackage.LoadPackageParam loadPackageParam){
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
	}

	/**
	 * 隐藏su文件
	 * @param loadPackageParam
	 */
	private void hiderSU(XC_LoadPackage.LoadPackageParam loadPackageParam){
		if(!loadPackageParam.packageName.equals("com.example.testroot2")){
			return;
		}
		XposedBridge.log("exists: hook-调用hiderSU ");
		Log.d("XposedMain", "exists: hook-调用hiderSU ");
		// 隐藏 su 二进制文件
		XposedHelpers.findAndHookMethod(
				"java.io.File",
				loadPackageParam.classLoader,
				"exists",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						XposedBridge.log("exists: hook1-外 ");
						Log.d("XposedMain", "exists: hook1-外 ");
						String path = ((File) param.thisObject).getAbsolutePath();
						if (path.contains("/su") || path.contains("/busybox")) {
							XposedBridge.log("exists: hook1-内 ");
							Log.d("XposedMain", "exists: hook1-内");
							param.setResult(false); // 隐藏 Root 文件
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
						XposedBridge.log("exists: hook2-外 ");
						Log.d("XposedMain", "exists: hook2-外 ");
						if (command.equals("su")) {
							XposedBridge.log("exists: hook2-内 ");
							Log.d("XposedMain", "exists: hook2-内 ");
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
						XposedBridge.log("exists: hook3-外 ");
						Log.d("XposedMain", "exists: hook3-外 ");
						String path = ((File) param.thisObject).getPath();
						if (path.contains("su")) {
							Log.d("XposedMain", "exists: hook3-内");
							XposedBridge.log("exists: hook3-内");
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
				XposedBridge.log("exists: hook4-外 ");
				Log.d("XposedMain", "exists: hook4-外 ");
				if (path.contains("/su") || path.contains("/busybox")) {
					XposedBridge.log("exists: hook4-内 ");
					Log.d("XposedMain", "exists: hook4-内 ");
					param.setResult(false);
				}
			}
		});
		// 拦截 File.exists 方法
		XposedHelpers.findAndHookMethod(
				File.class, // Hook java.io.File 类
				"exists",   // 拦截 exists 方法
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						File file = (File) param.thisObject;
						String filePath = file.getAbsolutePath();
						XposedBridge.log("exists: hook5-外 ");
						Log.d("XposedMain", "exists: hook5-外 ");
						// 如果路径是 su 文件，则返回 false
						if (filePath != null && isSuPath(filePath)) {
							XposedBridge.log("exists: hook5-内 ");
							Log.d("XposedMain", "exists: hook5-内 ");
							param.setResult(false);
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
						XposedBridge.log("exists: hook6-外 ");
						Log.d("XposedMain", "exists: hook6-外 ");
						String[] commands = (String[]) param.args[0];
						for (String command : commands) {
							if (command.contains("su") || command.contains("which su")) {
								XposedBridge.log("exists: hook6-内 ");
								Log.d("XposedMain", "exists: hook6-内 ");
								param.setThrowable(new RuntimeException("Command not allowed"));
							}
						}
					}
				}
		);


	}

	/**
	 * 隐藏usb
	 * @param loadPackageParam
	 */
	private void hiderUSB(XC_LoadPackage.LoadPackageParam loadPackageParam){
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
	}


	/**
	 * 隐藏无障碍
	 * @param loadPackageParam
	 */
	private void hiderAccessible(XC_LoadPackage.LoadPackageParam loadPackageParam){
		//隐藏无障碍
		XposedHelpers.findAndHookMethod("android.view.accessibility.AccessibilityManager", loadPackageParam.classLoader,
				"isEnabled", XC_MethodReplacement.returnConstant(false));
	}

	/**
	 * 隐藏root
	 * @param loadPackageParam
	 */
	private void hiderRoot(XC_LoadPackage.LoadPackageParam loadPackageParam){
		if (loadPackageParam.packageName.equals(BuildConfig.APPLICATION_ID)) {
			Log.d("XposedMain", "hiderRoot: 0");
			XposedHelpers.findAndHookMethod(
					"com.posed.xpalter.hider.ui.HiderRootActivity", loadPackageParam.classLoader,
					"isEnabled", XC_MethodReplacement.returnConstant(true)
			);
		}
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
	}

	/**
	 * 隐藏开发者
	 * @param loadPackageParam
	 */
	private void hiderDevelop(XC_LoadPackage.LoadPackageParam loadPackageParam){
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
	// 判断是否是 su 文件路径
	private boolean isSuPath(String path) {
		String[] suPaths = {
				"/system/app/Superuser.apk",
				"/sbin/su",
				"/system/bin/su",
				"/system/xbin/su",
				"/data/local/xbin/su",
				"/data/local/bin/su",
				"/system/sd/xbin/su",
				"/system/bin/failsafe/su",
				"/data/local/su"
		};
		for (String suPath : suPaths) {
			if (path.equals(suPath)) {
				return true;
			}
		}
		return false;
	}
}
