package io.droneplay.droneplaymission.utils;


import android.os.Handler;
import android.os.Message;

import java.io.IOException;

import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.utils.DronePlayAPI;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DronePlayAPISupport extends Thread {

	private String handler_host;
	private Handler handler;
	private RequestBody body;
	private final String murl = "https://api.droneplay.io/v1/";
	public static final MediaType JSON
			= MediaType.parse("application/json");
	private String dronePlayToken;

	public DronePlayAPISupport() {

	}


	public void setAction(String dronePlayToken, String mbody, Handler _handler) {
		this.body = RequestBody.create(JSON, mbody);
		this.handler_host = murl;
		this.handler = _handler;
		this.dronePlayToken = dronePlayToken;
	}


	public void setAction(String mbody, Handler _handler) {
		this.body = RequestBody.create(JSON, mbody);
		this.handler_host = murl;
		this.handler = _handler;
	}
	
	@Override
	public void run() {

		try {
			DronePlayAPI httpReq = new DronePlayAPI(this.dronePlayToken);
			Response response = httpReq.post(handler_host, body);

			if (handler == null) return;

			if (response.isSuccessful()) {
				String jSonString = response.body().string();
				Message message = Message.obtain(handler, R.id.req_succeeded);
				message.obj = jSonString;
				message.sendToTarget();
			} else {
				Message message = Message.obtain(handler, R.id.req_failed);
				message.sendToTarget();
			}
		} catch (IOException ex) {
			if (handler == null) return;
			Message message = Message.obtain(handler, R.id.req_failed);
			message.sendToTarget();
		}
	}
}
