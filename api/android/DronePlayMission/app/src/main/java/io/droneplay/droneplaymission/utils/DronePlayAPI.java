package io.droneplay.droneplaymission.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.RequestBody;


/**
 * Created by gunman on 2018. 2. 18..
 */

public class DronePlayAPI {
    private String droneplaytoken = "";

    public DronePlayAPI(String droneplaytoken) {
        this.droneplaytoken = droneplaytoken;
    }

    public okhttp3.Response post(String handler, RequestBody body) throws IOException {

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        builder.url(handler)
                .post(body);

        if (droneplaytoken != null && "".equalsIgnoreCase(droneplaytoken) == false) {
            builder.addHeader("droneplay-token", droneplaytoken);
        }

        okhttp3.Request request = builder.build();

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        return client.newCall(request).execute();
    }
}
