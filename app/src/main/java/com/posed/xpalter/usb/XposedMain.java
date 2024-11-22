package com.posed.xpalter.usb;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.posed.xpalter.BuildConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Rui Li on 12/5/2016.
 */
public class XposedMain implements IXposedHookLoadPackage , IXposedHookZygoteInit {
	public static final String TICKED_USB_APPS="ticked_usb_apps";
	public static final String PREFERENCE_KEY_NEVER_SHOW_DISCLAIMER = "never_show_disclaimer";

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

		XposedBridge.log("exists: hook ");
		hiderSystemNature(loadPackageParam);
		hiderSU(loadPackageParam);
		hiderRoot(loadPackageParam);
		hiderDevelop(loadPackageParam);
		hiderAccessible(loadPackageParam);
		hiderUSB(loadPackageParam);


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
							param.setResult("0"); // 模拟为非 Root 状态
						}
					}
				}
		);
		try {
			// 找到 android.os.Build 类
			Class<?> buildClass = XposedHelpers.findClass("android.os.Build", loadPackageParam.classLoader);

			// 使用反射修改 TAGS 字段
			Field tagsField = buildClass.getDeclaredField("TAGS");
			tagsField.setAccessible(true); // 设置为可访问

			// 修改字段值为 "release-keys"
			tagsField.set(null, "release-keys");

			XposedBridge.log("android.os.Build Successfully hooked Build.TAGS to return 'release-keys'");
		} catch (Throwable t) {
			XposedBridge.log("android.os.Build Failed to hook Build.TAGS: " + t.getMessage());
		}

		// Hook isOriginalSystem 方法
		try {
			XposedHelpers.findAndHookMethod(
					"com.example.util.SystemUtils", // 替换为包含 isOriginalSystem 方法的类
					loadPackageParam.classLoader,
					"isOriginalSystem", // 方法名
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							XposedBridge.log("Before isOriginalSystem Hooked"+loadPackageParam.packageName);
						}

						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							// 修改返回值
							param.setResult(true);
							XposedBridge.log("isOriginalSystem return value modified to true"+loadPackageParam.packageName);
						}
					}
			);


		} catch (Throwable t) {
			XposedBridge.log("Failed to hook isOriginalSystem: " + t.getMessage());
		}
		try {
			// Hook Runtime.exec 方法
			XposedHelpers.findAndHookMethod(
					"java.lang.Runtime",
					loadPackageParam.classLoader,
					"exec",
					String.class, // exec(String command)
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							String command = (String) param.args[0];

							if (command.contains("logcat -d")) {
								// 拦截 logcat -d 命令
								XposedBridge.log("Blocked logcat command: " + command);

								// 模拟一个空结果
								param.setResult(Runtime.getRuntime().exec("echo ''"));
							}
						}
					}
			);

			XposedHelpers.findAndHookMethod(
					"java.lang.Runtime",
					loadPackageParam.classLoader,
					"exec",
					String[].class, // exec(String[] cmdArray)
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							String[] command = (String[]) param.args[0];

							if (command != null && command.length > 0 && command[0].contains("logcat")) {
								// 拦截 logcat 命令
								XposedBridge.log("Blocked logcat array command: " + String.join(" ", command));

								// 模拟一个空结果
								param.setResult(Runtime.getRuntime().exec("echo ''"));
							}
						}
					}
			);

			XposedBridge.log("Hooked Runtime.exec to block logcat detection.");
		} catch (Throwable t) {
			XposedBridge.log("Failed to hook Runtime.exec: " + t.getMessage());
		}
	}
	private void hookExecMethod(XC_LoadPackage.LoadPackageParam loadPackageParam){
		try {
			// Hook System.getenv(String name)
			XposedHelpers.findAndHookMethod(
					System.class, // 类
					"getenv", // 方法名
					String.class, // 参数类型
					new XC_MethodHook() {
						@Override
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							String key = (String) param.args[0];
							XposedBridge.log("System.getenv called with key: " + key);
							// 如果 key 是 "PATH"，则返回空字符串
							if ("PATH".equals(key)) {
								param.setResult("");
								XposedBridge.log("System.getenv(\"PATH\") bypassed with empty string");
							}
						}
					}
			);


		}catch (Exception e){
			Log.d("XposedMain", "hookExecMethod: "+e.getMessage());
		}
	}

	/**
	 * 隐藏su文件
	 * @param loadPackageParam
	 */
	private void hiderSU(XC_LoadPackage.LoadPackageParam loadPackageParam){
		if(loadPackageParam.packageName.equals("com.posed.xpalter")){
			return;
		}
		hookExecMethod(loadPackageParam);

		// Hook 所有 App 的 Runtime.exec 方法
		XposedHelpers.findAndHookMethod(
				Runtime.class,
				"exec",
				String.class,
				String[].class,
				File.class,
				new XC_MethodHook() {

					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String command = (String) param.args[0];

						// 判断是否是检测命令
						if (isRootCheckCommand(command)) {
							XposedBridge.log("Hooked root check command: " + command);

							// 伪造执行结果，防止检测 Root
							param.setResult(createFakeProcess());
						}
					}
				}
		);
		// 隐藏 su 二进制文件
		XposedHelpers.findAndHookMethod(
				"java.io.File",
				loadPackageParam.classLoader,
				"exists",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String path = ((File) param.thisObject).getAbsolutePath();
						if (path.contains("/su") || path.contains("/busybox")||path.contains("/magisk")) {
							XposedBridge.log("exists: hook1-内 "+loadPackageParam.packageName);
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
						if (command.equals("su")||command.equals("mysu")||command.contains("magisk")) {
							XposedBridge.log("exists: hook2-内 "+loadPackageParam.packageName);
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
							XposedBridge.log("exists: hook3-内"+loadPackageParam.packageName);
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
				if (path.contains("/su") || path.contains("/busybox")||path.contains("/magisk")) {
					XposedBridge.log("exists: hook4-内 "+loadPackageParam.packageName);
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
						// 如果路径是 su 文件，则返回 false
						if (filePath != null && isSuPath(filePath)) {
							XposedBridge.log("exists: hook5-内 "+loadPackageParam.packageName);
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
						String[] commands = (String[]) param.args[0];
						for (String command : commands) {
							if (command.contains("su") || command.contains("which su")||command.contains("magisk")) {
								XposedBridge.log("exists: hook6-内 "+loadPackageParam.packageName);
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
							XposedBridge.log("隐藏开发者模式状态，返回关闭状态"+loadPackageParam.packageName);
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
		XposedHelpers.findAndHookMethod(
				"android.provider.Settings$Secure",
				loadPackageParam.classLoader,
				"getInt",
				android.content.ContentResolver.class,
				String.class,
				int.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String key = (String) param.args[1];
						if ("accessibility_enabled".equals(key)) {
							param.setResult(0); // 强制返回未启用状态
						}
					}
				}
		);

		XposedHelpers.findAndHookMethod(
				"android.provider.Settings$Secure",
				loadPackageParam.classLoader,
				"getString",
				android.content.ContentResolver.class,
				String.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String key = (String) param.args[1];
						if ("enabled_accessibility_services".equals(key)) {
							param.setResult(""); // 强制返回空服务列表
						}
					}
				}
		);

	}

	/**
	 * 隐藏root
	 * @param loadPackageParam
	 */
	private void hiderRoot(XC_LoadPackage.LoadPackageParam loadPackageParam){
		if (loadPackageParam.packageName.equals(BuildConfig.APPLICATION_ID)) {
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
							XposedBridge.log("隐藏开发者模式状态，返回关闭状态"+loadPackageParam.packageName);
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
	// 检查是否存在 su 文件
	private static boolean checkForSuBinary() {
		String[] paths = {
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
		for (String path : paths) {
			if (new File(path).exists()) {
				return true;
			}
		}
		return false;
	}
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		// 在 Zygote 初始化阶段的全局 Hook
		XposedBridge.log("initZygote called");

		// 示例：隐藏某些系统属性
		XposedHelpers.findAndHookMethod(
				"android.os.SystemProperties",
				null,
				"get",
				String.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String key = (String) param.args[0];
						if ("ro.dalvik.vm.native.bridge".equals(key)) {
							param.setResult(""); // 清空返回值
						}
					}
				}
		);
		XposedHelpers.findAndHookMethod(
				"android.app.Application",
				null,
				"attach",
				Context.class,
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						Context context = (Context) param.args[0];
						String packageName = context.getPackageName();
						if (packageName.equals("com.example.testroot2")) {
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
		// 隐藏 su 二进制文件
		XposedHelpers.findAndHookMethod(
				"java.io.File",
				null,
				"exists",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						String path = ((File) param.thisObject).getAbsolutePath();
						if (path.contains("/su") || path.contains("/busybox")) {
							XposedBridge.log("exists: hook1-内 ");
							param.setResult(false); // 隐藏 Root 文件
						}
					}
				}
		);

	}

	// 判断是否为 Root 检测命令
	private boolean isRootCheckCommand(String command) {
		return command.contains("which su") ||
				command.contains("which busybox") ||
				command.contains("ls /data/");
	}

	// 创建一个伪造的 Process 返回值
	private Process createFakeProcess() {
		return new Process() {
			@Override
			public OutputStream getOutputStream() {
				return null;
			}

			@Override
			public InputStream getInputStream() {
				// 模拟命令没有输出
				return new ByteArrayInputStream("".getBytes());
			}

			@Override
			public InputStream getErrorStream() {
				// 模拟命令没有错误
				return new ByteArrayInputStream("".getBytes());
			}

			@Override
			public int waitFor() {
				// 模拟正常退出状态
				return 0;
			}

			@Override
			public int exitValue() {
				return 0;
			}

			@Override
			public void destroy() {
			}
		};
	}
}
