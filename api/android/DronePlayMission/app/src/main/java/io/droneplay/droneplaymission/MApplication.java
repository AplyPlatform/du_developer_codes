package io.droneplay.droneplaymission;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

public class MApplication extends Application {
    private DronePlayMissionApplication fpvDemoApplication;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (fpvDemoApplication == null) {
            fpvDemoApplication = new DronePlayMissionApplication();
            fpvDemoApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fpvDemoApplication.onCreate();
    }
}
