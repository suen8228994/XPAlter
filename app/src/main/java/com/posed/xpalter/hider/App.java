package com.posed.xpalter.hider;

import android.app.Application;

import com.posed.xpalter.hider.util.Crashlytics;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Crashlytics(this));
    }
}
