package com.pi.cartubesafe;

import android.app.Application;

public final class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogStore.init(this);
        LogStore.i("App", "Application started");
        LogStore.syncDriveBestEffort();
    }
}
