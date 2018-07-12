package io.droneplay.droneplaymission;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.IOException;

import dji.thirdparty.okhttp3.OkHttpClient;
import dji.thirdparty.okhttp3.Request;
import dji.thirdparty.okhttp3.Response;

/**
 * Created by gunman on 2018. 2. 18..
 */

public class DronePlayAPI {
    private final String murl = "http://apis.airpage.org/%s/position/%s/set/%f/%f/%f";
    private OkHttpClient client;
    private Context context;

    public DronePlayAPI(Context mcontext) {
        client = new OkHttpClient();
        context = mcontext;
    }

    public void sendMyPosition(double lat, double lng, float alt) {
        String token = getMetadata(context,"io.droneplay.token");
        String email = getMetadata(context,"io.droneplay.email");

        String url = String.format(murl, token, email, lat, lng, alt);

        final Request request = new Request.Builder()
                    .url(url)
                    .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = client.newCall(request).execute();
                }
                catch(IOException e) {

                }
            }
        }).start();
    }
    // code request code here

    public static String getMetadata(Context context, String name) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getString(name);
            }
        } catch (PackageManager.NameNotFoundException e) {
// if we can’t find it in the manifest, just return null
        }

        return null;
    }

}
