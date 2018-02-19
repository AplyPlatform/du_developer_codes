package io.droneplay.droneplaymission;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;

import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
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

    private Button mapClearButton;
    private Button goButton;
    private TextView textView;
    private GoogleMap mMap;

    private int WAYPOINT_COUNT = 0;

    private DronePlayAPI dapi;

    protected FlightController flightController;
    private WaypointMissionOperator waypointMissionOperator = null;
    private WaypointMission mission = null;
    private WaypointMissionOperatorListener listener;
    private WaypointManager manager;

    private boolean bMissionState = false; //stop

    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();

    private boolean doubleBackToExitPressedOnce = false;

    protected double homeLatitude = 181;
    protected double homeLongitude = 181;
    final float baseAltitude = 20.0f;
    protected FlightMode flightState = null;
    private SupportMapFragment mapFragment;

    private List<String> missingPermission = new ArrayList<>();

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
            notifyStatusChange();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAndRequestPermissions();
        DronePlayMissionApplication.getEventBus().register(this);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(true);
        mapFragment.getMapAsync(this);

        textView = (TextView) findViewById(R.id.mapMon);
        mapClearButton = (Button) findViewById(R.id.mapClearButton);
        goButton = (Button) findViewById(R.id.goButton);

        dapi = new DronePlayAPI(this);
        manager = new WaypointManager();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        BaseProduct product = DronePlayMissionApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            ToastUtils.setResultToToast("Disconnect");
            return;
        } else {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }

            if (flightController != null) {

                flightController.setStateCallback(new FlightControllerState.Callback() {
                    @Override
                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                        homeLatitude = flightControllerState.getHomeLocation().getLatitude();
                        homeLongitude = flightControllerState.getHomeLocation().getLongitude();
                        flightState = flightControllerState.getFlightMode();

                        updateWaypointMissionState();
                    }
                });

            }
        }
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        setUpListener();
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        tearDownListener();
        if (flightController != null) {
            flightController.getSimulator().stop(null);
            flightController.setStateCallback(null);
        }

    }


    private void prepareMission() {
        if (waypointMissionOperator == null) {
            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        }

        mission = manager.getWaypointMission();
        WAYPOINT_COUNT = mission.getWaypointCount();
        DJIError djiError = waypointMissionOperator.loadMission(mission);
        showResultToast(djiError);
    }

    private void startMission() {
        if (waypointMissionOperator == null) {
            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        }

        if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
            waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showResultToast(djiError);
                }
            });
        } else {
            ToastUtils.setResultToToast("Not ready!");
        }

        if (mission != null) {
            waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    showResultToast(djiError);
                }
            });

        } else {
            ToastUtils.setResultToToast("Prepare Mission First!");
        }
    }

    private void stopMission() {
        if (waypointMissionOperator == null) {
            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        }

        // Example of stopping a Mission
        waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showResultToast(djiError);
            }
        });

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
            Toast.makeText(getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }


    //region Registration n' Permissions Helpers

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
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.setResultToToast(MapsActivity.this.getString(R.string.sdk_registration_doing_message));
                    DJISDKManager.getInstance().registerApp(MapsActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                ToastUtils.setResultToToast(MapsActivity.this.getString(R.string.sdk_registration_success_message));
                            } else {
                                ToastUtils.setResultToToast(MapsActivity.this.getString(R.string.sdk_registration_message));
                            }
                            Log.v(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
                            Log.d(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
                            notifyStatusChange();
                        }
                    });
                }
            });
        }
    }

    private void notifyStatusChange() {
        DronePlayMissionApplication.getEventBus().post(new ConnectivityChangeEvent());
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng dockdo = new LatLng(37.2412061, 131.8617358);
        //mMap.addMarker(new MarkerOptions().position(dockdo).title("Marker in Dokdo"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dockdo, 20));


        mapClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.clear();
                mMarkers.clear();
                textView.setText("");
            }
        });

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bMissionState == false) {
                    manager.clear();
                    Set entrySet = mMarkers.entrySet();
                    // Obtaining an iterator for the entry set
                    Iterator it = entrySet.iterator();
                    while (it.hasNext()) {
                        Map.Entry me = (Map.Entry) it.next();
                        MarkerOptions m = (MarkerOptions) me.getValue();
                        manager.addWaypointMission(m.getPosition().latitude, m.getPosition().longitude, baseAltitude, 1);
                    }

                    prepareMission();
                    startMission();
                }
                else {
                    stopMission();
                }
            }
        });


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick");

                String markerName = mMarkers.size() + ":" + Math.round(latLng.latitude) + "," + Math.round(latLng.longitude);
                final MarkerOptions nMarker = new MarkerOptions().position(latLng)
                        .title(markerName)
                        .snippet(markerName);
                mMarkers.put(markerName, nMarker);
                mMap.addMarker(nMarker);
                textView.setText(markerName);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "onMarkerClick");
                if (doubleBackToExitPressedOnce) {
                    mMarkers.remove(marker.getTitle());
                    marker.remove();
                    textView.setText("");
                } else {
                    MapsActivity.this.doubleBackToExitPressedOnce = true;
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce = false;
                        }
                    }, 1500);
                }

                return true;
            }


        });
    }

    @Override
    protected void onDestroy() {
        DronePlayMissionApplication.getEventBus().unregister(this);
        super.onDestroy();
    }

    @Subscribe
    public void onConnectivityChange(ConnectivityChangeEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //refreshTitle();
            }
        });
    }

    public static class ConnectivityChangeEvent {
    }

    //region Not important stuff
    private void setUpListener() {
        // Example of Listener
        listener = new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
                // Example of Download Listener
                if (waypointMissionDownloadEvent.getProgress() != null
                        && waypointMissionDownloadEvent.getProgress().isSummaryDownloaded
                        && waypointMissionDownloadEvent.getProgress().downloadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
                    ToastUtils.setResultToToast("Download successful!");
                }
                updateWaypointMissionState();
            }

            @Override
            public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
                // Example of Upload Listener
                if (waypointMissionUploadEvent.getProgress() != null
                        && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                        && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
                    ToastUtils.setResultToToast("Upload successful!");
                }
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
                // Example of Execution Listener
                Log.d(TAG,
                        (waypointMissionExecutionEvent.getPreviousState() == null
                                ? ""
                                : waypointMissionExecutionEvent.getPreviousState().getName())
                                + ", "
                                + waypointMissionExecutionEvent.getCurrentState().getName()
                                + (waypointMissionExecutionEvent.getProgress() == null
                                ? ""
                                : waypointMissionExecutionEvent.getProgress().targetWaypointIndex));

                int idx = waypointMissionExecutionEvent.getProgress().targetWaypointIndex;
                List<Waypoint> wlist = mission.getWaypointList();
                Waypoint w = wlist.get(idx);
                LatLng latLng = new LatLng(w.coordinate.getLatitude(), w.coordinate.getLongitude());

                dapi.sendMyPosition(latLng.latitude, latLng.longitude, w.altitude);

                String markerName = mMarkers.size() + ":" + Math.round(w.coordinate.getLatitude()) + "," + Math.round(w.coordinate.getLongitude());
                final MarkerOptions nMarker = new MarkerOptions().position(latLng)
                        .title(markerName)
                        .snippet(markerName);
                mMap.addMarker(nMarker);
                textView.setText(markerName);

                updateWaypointMissionState();
            }

            @Override
            public void onExecutionStart() {
                ToastUtils.setResultToToast("Execution started!");
                updateWaypointMissionState();
                mMap.clear();
                mapFragment.getView().setClickable(false);
                bMissionState = true;
                goButton.setText("STOP");
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                ToastUtils.setResultToToast("Execution finished!");
                updateWaypointMissionState();
                mapFragment.getView().setClickable(true);
                bMissionState = false;
                goButton.setText("GO");
            }
        };

        if (waypointMissionOperator != null && listener != null) {
            // Example of adding listeners
            waypointMissionOperator.addListener(listener);
        }
    }

    private void updateWaypointMissionState(){
        if (waypointMissionOperator != null && waypointMissionOperator.getCurrentState() != null) {
            /*ToastUtils.setResultToText(FCPushInfoTV,
                    "home point latitude: "
                            + homeLatitude
                            + "\nhome point longitude: "
                            + homeLongitude
                            + "\nFlight state: "
                            + flightState.name()
                            + "\nCurrent Waypointmission state : "
                            + waypointMissionOperator.getCurrentState().getName());*/
        } else {
            /*ToastUtils.setResultToText(FCPushInfoTV,
                    "home point latitude: "
                            + homeLatitude
                            + "\nhome point longitude: "
                            + homeLongitude
                            + "\nFlight state: "
                            + flightState.name());*/
        }
    }

    private void tearDownListener() {
        if (waypointMissionOperator != null && listener != null) {
            // Example of removing listeners
            waypointMissionOperator.removeListener(listener);
        }
    }

    private void showResultToast(DJIError djiError) {
        ToastUtils.setResultToToast(djiError == null ? "Action started!" : djiError.getDescription());
    }

}
