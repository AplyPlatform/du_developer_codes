package io.droneplay.droneplaymission;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;


public class MissionRunActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MissionRunActivity.class.getSimpleName();

    private Button testButton;

    private String buttonID;

    private TextView textView;
    private GoogleMap mMap;

    private int WAYPOINT_COUNT = 0;

    private DronePlayAPI dapi;

    protected FlightController flightController;
    private WaypointMissionOperator waypointMissionOperator = null;
    private WaypointMission mission;
    private WaypointMissionOperatorListener listener;
    private WaypointManager manager;

    private boolean bMissionState = false; //stop

    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();

    private boolean doubleBackToExitPressedOnce = false;

    protected double homeLatitude = 181;
    protected double homeLongitude = 181;
    final float baseAltitude = 10.0f;
    protected FlightMode flightState = null;
    private SupportMapFragment mapFragment;

    private int markerid = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DronePlayMissionApplication.getEventBus().register(this);

        setContentView(R.layout.activity_run);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(true);
        mapFragment.getMapAsync(this);

        textView = (TextView) findViewById(R.id.mapMon);
        testButton = (Button) findViewById(R.id.testButton);
        testButton.setEnabled(false);

        dapi = new DronePlayAPI(this);

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        Intent i = getIntent();
        buttonID = i.getStringExtra("buttonID");
        manager = new WaypointManager(buttonID);

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

    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        return null;
    }

    private void uploadMission() {
        if (waypointMissionOperator == null) {
            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        }

        if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
            waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                    if (djiError != null)
                        showResultToast(djiError);
                    else {
                        testButton.setEnabled(true);
                        manager.saveMissionToFile(buttonID);
                    }
                }
            });
        } else {
            ToastUtils.setResultToToast("Not ready!");
        }
    }

    private void prepareMission() {
        mission = manager.getWaypointMission();

        if (mission == null) {
            ToastUtils.setResultToToast("Mission is not ready !");
            return;
        }

        if (waypointMissionOperator == null) {
            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        }

        WAYPOINT_COUNT = mission.getWaypointCount();
        DJIError djiError = waypointMissionOperator.loadMission(mission);
        if (djiError != null)
            showResultToast(djiError);
        else
            uploadMission();
    }

    private void startMission() {
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

        LatLng dockdo = null;

        Location lc = getCurrentLocation();
        if (lc != null) {
            dockdo = new LatLng(lc.getLatitude(), lc.getLongitude());
        }

        if(dockdo == null){
            dockdo = new LatLng(37.2412061, 131.8617358);
        }

        //mMap.addMarker(new MarkerOptions().position(dockdo).title("Marker in Dokdo"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dockdo, 20));

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bMissionState == false) {
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

                String markerName = markerid + "";
                final MarkerOptions nMarker = new MarkerOptions().position(latLng)
                        .title(markerName)
                        .snippet(markerName);
                mMarkers.put(markerName, nMarker);
                mMap.addMarker(nMarker);

                String showText = String.format("Count: %d\nLat: %.4f, Lng: %.4f", mMarkers.size(), latLng.latitude, latLng.longitude);
                textView.setText(showText);
                manager.addAction(markerName,latLng.latitude,latLng.longitude, baseAltitude, 0);
                markerid++;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(TAG, "onMarkerClick");
                if (doubleBackToExitPressedOnce) {
                    mMarkers.remove(marker.getTitle());
                    manager.removeAction(marker.getTitle());
                    marker.remove();
                    String showText = String.format("Count: %d", mMarkers.size());
                    textView.setText(showText);
                } else {
                    MissionRunActivity.this.doubleBackToExitPressedOnce = true;
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

                updateWaypointMissionState();
            }

            @Override
            public void onExecutionStart() {
                ToastUtils.setResultToToast("Execution started!");
                updateWaypointMissionState();
                mMap.clear();
                mapFragment.getView().setClickable(false);
                bMissionState = true;
                testButton.setText("STOP");
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                ToastUtils.setResultToToast("Execution finished!");
                updateWaypointMissionState();
                mapFragment.getView().setClickable(true);
                bMissionState = false;
                testButton.setText("GO");
            }
        };

        if (waypointMissionOperator != null && listener != null) {
            // Example of adding listeners
            waypointMissionOperator.addListener(listener);
        }
    }

    private void updateWaypointMissionState(){
        dapi.sendMyPosition(homeLatitude, homeLongitude, baseAltitude);
        String markerName = mMarkers.size() + ":" + Math.round(homeLatitude) + "," + Math.round(homeLongitude);
        final MarkerOptions nMarker = new MarkerOptions().position(new LatLng(homeLatitude, homeLongitude))
                .title(markerName)
                .snippet(markerName);
        mMap.addMarker(nMarker);
        textView.setText(markerName);
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
