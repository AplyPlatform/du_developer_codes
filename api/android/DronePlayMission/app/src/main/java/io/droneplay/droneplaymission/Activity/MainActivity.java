package io.droneplay.droneplaymission.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.mission.waypoint.WaypointMission;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;
import io.droneplay.droneplaymission.model.DronePlayAPIResponse;
import io.droneplay.droneplaymission.DronePlayMissionApplication;
import io.droneplay.droneplaymission.utils.HelperUtils;
import io.droneplay.droneplaymission.model.MainListAdapter;
import io.droneplay.droneplaymission.model.MainListItem;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.utils.ToastUtils;
import io.droneplay.droneplaymission.utils.WaypointManager;

public class MainActivity extends AppCompatActivity implements MainListAdapter.ListBtnClickListener, HelperUtils.titleInputClickListener {

    private ListView listview ;
    private MainListAdapter adapter;
    private Button newMissionBtn;

    private String removeButtonID = "";
    private static ProgressDialog spinner = null;

    private static final String TAG = MainActivity.class.getName();

    private List<String> missingPermission = new ArrayList<>();

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);


    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        adapter = new MainListAdapter(this);
        listview = (ListView) findViewById(R.id.listviewMain);
        listview.setAdapter(adapter);
        newMissionBtn = findViewById(R.id.btnNewMission);
        listview.setVisibility(View.INVISIBLE);

        newMissionBtn.setText("Wait ...");
        newMissionBtn.setEnabled(false);

        listview.setVisibility(View.INVISIBLE);

        spinner = new ProgressDialog(this);
        spinner.setCancelable(false);

        showLoader();
        Init();
    }


    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DronePlayMissionApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        Log.e(TAG, "onResume");
        super.onResume();
    }

    private void deleteButton(String buttonid) {
        removeButtonID = buttonid;
        HelperUtils.getInstance().deleteButtonsFromServer(buttonid, deleteHandler);
    }

    private void Init() {
        newMissionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaseProduct mProduct = DronePlayMissionApplication.getProductInstance();
                if (mProduct == null || mProduct.isConnected() == false) {
                    showToast("Product is not connected !");
                    return;
                }

                HelperUtils.getInstance().showTitleInputDialog(MainActivity.this, MainActivity.this);
            }
        });

        HelperUtils.getInstance().loadButtonsFromServer(requestListHandler);
    }


    @SuppressLint("HandlerLeak")
    private final Handler deleteHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {

            hideLoader();
            switch (message.what) {
                case R.id.req_succeeded:

                    String resultContent = (String) message.obj;
                    try {
                        JSONObject json = new JSONObject(resultContent);
                        String result = (String) json.get("result");
                        if (result != null && result.equalsIgnoreCase("success")) {
                            adapter.removeItem(removeButtonID);
                            adapter.notifyDataSetChanged();
                            showToast("Successfully, removed.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("Failed - "  + e.getMessage());
                    }

                    return;
                case R.id.req_failed:
                    showToast("Failed to remove");
                    break;
            }
        }
    };


    @SuppressLint("HandlerLeak")
    private final Handler requestListHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {

            hideLoader();
            switch (message.what) {
                case R.id.req_succeeded:

                    String resultContent = (String) message.obj;

                    try {
                        if(resultContent != null) {
                            Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                                    .create();
                            Type type = new TypeToken<DronePlayAPIResponse>() { }.getType();
                            DronePlayAPIResponse jsonData = gson.fromJson(resultContent, type);
                            listData(jsonData.data);
                        }
                        else {

                        }
                    } catch (Exception e) {

                    }

                    return;
                case R.id.req_failed:

                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



    @Override
    public void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }


    private void listData(ArrayList<MainListItem> list) {
        if (list == null) return;

        adapter.setItems(list);
        WaypointManager.getInstance().setMainList(list);

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                newMissionBtn.setText("비행 시작");
                newMissionBtn.setEnabled(true);

                listview.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void prepareMission(String buttonID) {
        BaseProduct mProduct = DronePlayMissionApplication.getProductInstance();
        if (mProduct == null || mProduct.isConnected() == false) {
            //if (DJISDKManager.getInstance().startConnectionToProduct() == false) {
                showToast("Failed to connect to product");
                return;
            //}
        }

        WaypointMissionOperator waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        waypointMissionOperator.clearMission();

        WaypointManager.getInstance().setMission(buttonID);
        WaypointMission mission = WaypointManager.getInstance().getWaypointMission();

        if (mission == null) {
            ToastUtils.setResultToToast("Mission is not ready !");
            return;
        }

        Intent intent = new Intent(MainActivity.this, MissionRunActivity.class);
        intent.putExtra(MissionRunActivity.PARAM_BUTTON_ID, WaypointManager.getInstance().getMissionID());
        startActivity(intent);
    }


    public void showYesNoDialog(final MainListAdapter.CLICK_TYPE kind, final String buttonId) {

        String strMsg = "Are you sure ?";
        switch(kind) {
            case RUN:
                strMsg = "Run this mission ?";
                break;

            case DELETE:
                strMsg = "Delete this mission ?";
                break;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int choice) {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        switch(kind) {
                            case RUN:
                                prepareMission(buttonId);
                                break;

                            case DELETE:
                                deleteButton(buttonId);
                                break;
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(strMsg)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    public void onListBtnClick(MainListAdapter.CLICK_TYPE kind, String buttonId) {

        switch(kind) {
            case RUN:
                showYesNoDialog( kind, buttonId);
                break;

            case DELETE:
                showYesNoDialog( kind, buttonId);
                break;

            case EDIT:
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra(MissionRunActivity.PARAM_BUTTON_ID, buttonId);
                startActivity(intent);
                break;
        }

    }

    @Override
    public void onTitileInputClick(String buttonTitle) {
        if (buttonTitle == null || buttonTitle.equalsIgnoreCase("")) {
            finish();
            return;
        }

        Intent intent = new Intent(MainActivity.this, MissionRunActivity.class);
        intent.putExtra(MissionRunActivity.PARAM_BUTTON_ID, "NEW_MISSION");
        intent.putExtra("title", buttonTitle);
        startActivity(intent);
    }

    private void startSDKRegistration() {


        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast( "registering, pls wait...");
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                showToast("Register Success");
                                DJISDKManager.getInstance().startConnectionToProduct();

                            } else {
                                showToast( "Register sdk fails, check network is available");
                            }
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("Product is Disconnected");

                        }
                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                            showToast("Product is Connected");
                        }
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(mDJIComponentListener);
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                            showToast(newComponent + " Component is Changed.");
                        }
                    });
                }
            });
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };




    private void showLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.show();
            }
        });
    }

    private void hideLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.hide();
            }
        });
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }


    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }
    }



    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = DronePlayMissionApplication.getProductInstance();
        TextView txtView = findViewById(R.id.txtProductname);
        String txtMon = "";

        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK: True");

            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";

            if (null != mProduct.getModel()) {
                txtMon = "Status: " + str + " connected / " + mProduct.getModel().getDisplayName();
            } else {
                txtMon = "Status: " + str + " connected";
            }

            txtView.setText(txtMon);
            showToast(txtMon);
        } else {
            Log.v(TAG, "refreshSDK: False");
            txtMon = "Failed to connect to product";
            showToast(txtMon);
            txtView.setText(txtMon);
        }
    }
}
