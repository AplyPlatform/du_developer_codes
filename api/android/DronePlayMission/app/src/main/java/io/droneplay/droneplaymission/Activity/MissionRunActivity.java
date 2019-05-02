package io.droneplay.droneplaymission.Activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.remotecontroller.HardwareState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.useraccount.UserAccountManager;
import io.droneplay.droneplaymission.DronePlayMissionApplication;
import io.droneplay.droneplaymission.model.FlightRecordItem;
import io.droneplay.droneplaymission.utils.HelperUtils;
import io.droneplay.droneplaymission.utils.ModuleVerificationUtil;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.utils.ToastUtils;
import io.droneplay.droneplaymission.utils.WaypointManager;


public class MissionRunActivity extends FragmentActivity {
    private static final String TAG = MissionRunActivity.class.getSimpleName();
    public static final String PARAM_BUTTON_ID = "buttonID";
    private Marker currentDroneMarker;

    private Button stopButton;

    private String buttonID;

    private boolean bWaypointExecuted = false;

    private int allMissionCount = 0;
    private int finPointCount = 0;
    private int finMissionCount = 0;

    private Timer timer = new Timer();
    private Timer dataTimer = new Timer();

    private long timeCounter = 0;
    private long hours = 0;
    private long minutes = 0;
    private long seconds = 0;
    private String time = "";

    private TextView textView;
    private MapView mMapView = null;
    private MapboxMap mMap = null;

    private int WAYPOINT_COUNT = 0;
    protected FlightController flightController = null;
    private RemoteController remoteController;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataCallBack = null;

    private WaypointMissionOperator waypointMissionOperator = null;
    private WaypointMissionOperatorListener listener;
    private WaypointManager manager = null;
    private DJICodecManager mCodecManager = null;

    private BatteryState latestBatteryState;

    protected double currentLatitude = 0;
    protected double currentLongitude = 0;
    protected float currentAltitude = 0.0f;

    protected double oldLatitude = 0;
    protected double oldLongitude = 0;
    protected float oldAltitude = 0.0f;

    protected SurfaceView videostreamPreviewSf = null;
    private SurfaceHolder videostreamPreviewSh = null;
    private SurfaceHolder.Callback surfaceCallback;

    private TextView recordingTime;

    private Context mContext;
    private Handler handler;

    private LinearLayout screenForTouch;

    private boolean bPositionTouched = false;

    private boolean bIsRecording = false;

    private DataTimer recordTask = null;

    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();
    private final ArrayList<FlightRecordItem> mFlightRecord = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        DronePlayMissionApplication.getEventBus().register(mContext);

        setContentView(R.layout.activity_run);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        finPointCount = 0;
        allMissionCount = 0;
        finMissionCount = 0;

        initUI();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };


    private void onProductConnectionChange()
    {
        setUpListener();
    }

    private void setMapStyle() {

        if (mMap == null) return;

        int kind = HelperUtils.getInstance().readMapStyle(mContext);

        String strKind = getString(R.string.mapbox_style_satellite_streets);;
        switch(kind) {
            case 0:
                break;

            case 1:
                strKind = getString(R.string.mapbox_style_satellite);
                break;

            case 2:
                strKind = getString(R.string.mapbox_style_outdoors);
                break;

            case 3:
                strKind = getString(R.string.mapbox_style_mapbox_streets);
                break;

            case 4:
                strKind = getString(R.string.mapbox_style_traffic_day);
                break;

            case 5:
                strKind = getString(R.string.mapbox_style_traffic_night);
                break;
        }

        mMap.setStyle(strKind);
    }

    private void initUI() {
        View decorView = getWindow().getDecorView();
// Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
// Remember that you should never show the action bar if the
// status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.hide();


        textView = (TextView) findViewById(R.id.mapMon);
        textView.setVisibility(View.VISIBLE);
        stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setEnabled(false);

        videostreamPreviewSf = (SurfaceView) findViewById(R.id.video_previewer_surface);
        recordingTime = (TextView) findViewById(R.id.timer);
        recordingTime.setVisibility(View.VISIBLE);

        screenForTouch = findViewById(R.id.screenForTouch);

        screenForTouch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bPositionTouched = true;
            }
        });

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {

                mMap = mapboxMap;

                setMapStyle();

                LatLng latLng = loadMissionsToMap();
                if (latLng != null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
                else {
                    LatLng ltlng = new LatLng(37,123, 127.123);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ltlng, 20));
                }

                stopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        stopButton.setEnabled(false);
                        stopMission();

                        if (flightController != null) {
                            flightController.getSimulator().stop(null);
                            flightController.setStateCallback(null);
                        }

                        stopDataScheduler();
                        stopRecord();
                        tearDownListener();
                        HelperUtils.getInstance().uploadFlightRecord(buttonID, mFlightRecord, uploadHandler);

                        ToastUtils.setResultToToast("기록이 중지 되었습니다.");
                    }
                });
            }
        });
    }

    protected void changeDescription(final String newDescription) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                recordingTime.setText(newDescription);
            }
        });
    }


    // Method for starting recording
    private void startRecord(){
        if (!ModuleVerificationUtil.isCameraModuleAvailable()) return;

        final Camera camera = DronePlayMissionApplication.getProductInstance().getCamera();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        ToastUtils.setResultToToast("Record video: success");
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                timeCounter = timeCounter + 1;
                                hours = TimeUnit.MILLISECONDS.toHours(timeCounter);
                                minutes =
                                        TimeUnit.MILLISECONDS.toMinutes(timeCounter) - (hours * 60);
                                seconds = TimeUnit.MILLISECONDS.toSeconds(timeCounter) - ((hours
                                        * 60
                                        * 60) + (minutes * 60));
                                time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                                changeDescription(time);
                            }
                        }, 0, 1);
                    } else {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){
        if (!ModuleVerificationUtil.isCameraModuleAvailable()) return;

        Camera camera = DronePlayMissionApplication.getProductInstance().getCamera();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        ToastUtils.setResultToToast("Stop recording: success");
                        changeDescription("00:00:00");
                        timer.cancel();
                        timeCounter = 0;
                    }else {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }



    private void initPreviewer() {
        BaseProduct product = DronePlayMissionApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            ToastUtils.setResultToToast("Disconnected");
        } else {
            if (videostreamPreviewSh != null) return;

            videostreamPreviewSh = videostreamPreviewSf.getHolder();
            surfaceCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.d(TAG, "real onSurfaceTextureAvailable");
                    int videoViewWidth = videostreamPreviewSf.getWidth();
                    int videoViewHeight = videostreamPreviewSf.getHeight();
                    Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);

                    if (mCodecManager == null) {

                        mCodecManager = new DJICodecManager(getApplicationContext(), holder, videoViewWidth, videoViewHeight);

                    }

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    if (mCodecManager != null) {
                        mCodecManager.cleanSurface();
                        mCodecManager.destroyCodec();
                        mCodecManager = null;
                    }
                }
            };

            videostreamPreviewSh.addCallback(surfaceCallback);

            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataCallBack);
            }
        }
    }

    private void uninitPreviewer() {
        if (mReceivedVideoDataCallBack == null) return;
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataCallBack);
        mReceivedVideoDataCallBack = null;
    }

    private Marker addMarkerToMap(LatLng latLng, int id) {
        String markerName = mMarkers.size() + ":" + Math.round(currentLatitude) + "," + Math.round(currentLongitude);
        MarkerOptions nMarker = new MarkerOptions().position(latLng)
                .title(markerName)
                .snippet(markerName);

        if (id >= 0) {
            IconFactory iconFactory = IconFactory.getInstance(mContext);
            Icon icon = iconFactory.fromResource(id);
            nMarker.icon(icon);
        }

        mMarkers.put(markerName, nMarker);
        if(mMap != null) {
            double curZoom = mMap.getCameraPosition().zoom;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, curZoom));
            return mMap.addMarker(nMarker);
        }

        return null;
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void getBatteryInfo() {
        try {
            DronePlayMissionApplication.getProductInstance()
                    .getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    latestBatteryState = batteryState;
                }
            });
        } catch (Exception ignored) {

        }
    }

    private void updateWaypointMissionState() {

        if (Double.isNaN(currentLatitude)
                || Double.isNaN(currentLongitude)
                || Float.isNaN(currentAltitude)) return;

        if (checkGpsCoordination(currentLatitude, currentLongitude) == false) return;

        if (currentAltitude == oldAltitude
                && currentLatitude == oldLatitude
                && currentLongitude == oldLongitude) return;

        getBatteryInfo();

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                textView.setText(finMissionCount + "/" + allMissionCount + "/H:" + String.format("%.3f", currentAltitude)
                        + "/La:" + String.format("%.3f", currentLatitude)
                        + "/Ln:" + String.format("%.3f", currentLongitude)
                        + (latestBatteryState == null ? "" : ("/Bt:" + String.valueOf(latestBatteryState.getChargeRemainingInPercent())) + "%"));
            }
        });
    }


    // 첫 번째 TimerTask 를 이용한 방법
    class DataTimer extends TimerTask{
        @Override
        public void run() {
            sendDataToServer(0);
        }
    }

    private void startDataScheduler() {
        if (recordTask != null) return;

        recordTask = new DataTimer();
        dataTimer.schedule(recordTask, 0, 2000);
    }

    private void stopDataScheduler() {
        dataTimer.cancel();
        recordTask = null;
    }


    private void sendDataToServer(int currentAction) {
        if (Double.isNaN(currentLatitude)
                || Double.isNaN(currentLongitude)
                || Float.isNaN(currentAltitude)) return;

        if (checkGpsCoordination(currentLatitude, currentLongitude) == false) return;

        FlightRecordItem item = new FlightRecordItem();
        item.act = currentAction;
        item.actparam = 0;
        item.speed = 5;
        item.alt = currentAltitude;
        item.lat = String.valueOf(currentLatitude);
        item.lng = String.valueOf(currentLongitude);
        if (latestBatteryState != null)
            item.etc.battery = String.valueOf(latestBatteryState.getChargeRemainingInPercent());
        else
            item.etc.battery = "-";

        item.etc.marked = bPositionTouched ? "true" : "false";
        item.dsec = String.valueOf(timeCounter);
        item.dtimestamp = HelperUtils.getInstance().getTimeStamp();
        mFlightRecord.add(item);

        final LatLng latlng = new LatLng(currentLatitude, currentLongitude);

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if (currentDroneMarker != null)
                    mMap.removeMarker(currentDroneMarker);

                HelperUtils.getInstance().sendMyPosition(item);

                currentDroneMarker = addMarkerToMap(latlng, R.mipmap.drone);

                finPointCount++;

                TextView textCount = findViewById(R.id.pointCount);
                textCount.setText(finPointCount + " recorded.");
            }
        });

        oldAltitude = currentAltitude;
        oldLatitude = currentLatitude;
        oldLongitude = currentLongitude;

        if (bPositionTouched == true) {
            bPositionTouched = false;
            ToastUtils.setResultToToast("Position is marked.");
        }
    }


    //region Not important stuff
    private void setUpListener() {

        BaseProduct product = DronePlayMissionApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            ToastUtils.setResultToToast("Disconnected");
            return;
        } else {

            if (ModuleVerificationUtil.isRemoteControllerAvailable()) {
                remoteController = ((Aircraft) product).getRemoteController();
                if (remoteController == null) {
                    ToastUtils.setResultToToast("Remotecontroller is not connected");
                    return;
                }

                remoteController.setHardwareStateCallback(new HardwareState.HardwareStateCallback() {
                    @Override
                    public void onUpdate(@NonNull HardwareState rcHardwareState) {
                        HardwareState.Button btn = rcHardwareState.getShutterButton();
                        if (btn != null && btn.isClicked()) {
                            sendDataToServer(1);
                            return;
                        }

                        HardwareState.Button btnR = rcHardwareState.getRecordButton();
                        if (btnR != null && btnR.isClicked()) {

                            if (bIsRecording == false)
                                sendDataToServer(2); // START_RECORD
                            else
                                sendDataToServer(3); // STOP_RECORD

                            bIsRecording = !bIsRecording;
                        }
                    }
                });
            }

            if (flightController != null) return;

            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }

            if (flightController != null) {
                flightController.setMaxFlightRadiusLimitationEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });

                flightController.setStateCallback(new FlightControllerState.Callback() {
                    @Override
                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                        currentLatitude = flightControllerState.getAircraftLocation().getLatitude();
                        currentLongitude = flightControllerState.getAircraftLocation().getLongitude();
                        currentAltitude = flightControllerState.getAircraftLocation().getAltitude();

                        updateWaypointMissionState();
                    }
                });

                mReceivedVideoDataCallBack = new VideoFeeder.VideoDataListener() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {


                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size);
                        }
                    }
                };

                handler = new Handler();
            }
        }
    }

    private void setUpWaypointListener() {

        manager = WaypointManager.getInstance();
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();

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

                finMissionCount--;

            }

            @Override
            public void onExecutionStart() {
                mFlightRecord.clear();
                ToastUtils.setResultToToast("Execution started!");

                startDataScheduler();
                startRecord();

                updateWaypointMissionState();
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        if(mMap != null)
                            mMap.clear();

                        mMapView.setClickable(false);
                        stopButton.setEnabled(true);
                        bWaypointExecuted = true;
                    }
                });
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                HelperUtils.getInstance().uploadFlightRecord(buttonID, mFlightRecord, uploadHandler);
                ToastUtils.setResultToToast("Execution finished!");
                updateWaypointMissionState();
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        mMapView.setClickable(true);
                        stopButton.setEnabled(false);
                        bWaypointExecuted = false;
                    }
                });
            }
        };

        waypointMissionOperator.addListener(listener);
    }


    @SuppressLint("HandlerLeak")
    private final Handler uploadHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case R.id.req_succeeded:

                    String resultContent = (String) message.obj;
                    try {
                        JSONObject json = new JSONObject(resultContent);
                        String result = json.getString("result");
                        if (result != null && result.equalsIgnoreCase("success")) {
                            mFlightRecord.clear();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        if(resultContent != null) {

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


    private LatLng loadMissionsToMap() {
        if (manager == null) return null;

        LatLng dockdo = null;
        WaypointMission mission = manager.getWaypointMission();

        if (mission == null)
            return loadDefaultPosition();

        List<Waypoint> mList = mission.getWaypointList();
        if (mList == null || mList.size() == 0)
            return loadDefaultPosition();

        if (mMap != null)
            mMap.clear();

        mMarkers.clear();
        textView.setText("");

        for (Waypoint pt : mList) {
            LatLng ltlng = new LatLng(pt.coordinate.getLatitude(), pt.coordinate.getLongitude());
            addMarkerToMap(ltlng, R.mipmap.mission_flag);
            dockdo = ltlng;
        }

        return dockdo;
    }

    private LatLng loadDefaultPosition() {
        LatLng dockdo = null;

        Location lc = getCurrentLocation();
        if (lc != null) {
            dockdo = new LatLng(lc.getLatitude(), lc.getLongitude());
        }

        if(dockdo == null){
            dockdo = new LatLng(37.2412061, 131.8617358);
        }

        return dockdo;
    }


    private void startNewMission() {

        setUpListener();
        initPreviewer();
        startDataScheduler();
        startRecord();

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if(mMap != null)
                    mMap.clear();

                mMapView.setClickable(false);
                stopButton.setEnabled(true);
            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first


        IntentFilter filter = new IntentFilter();
        filter.addAction(DronePlayMissionApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        Intent i = getIntent();
        String param = i.getStringExtra(PARAM_BUTTON_ID);
        if (param != null && param.equalsIgnoreCase("NEW_MISSION")) {
            buttonID = i.getStringExtra("title");
            startNewMission();
        }
        else {
            setUpListener();
            setUpWaypointListener();
            initPreviewer();
            loadMission();
        }
    }


    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        unregisterReceiver(mReceiver);

        stopMission();

        if (flightController != null) {
            flightController.getSimulator().stop(null);
            flightController.setStateCallback(null);
        }

        stopDataScheduler();
        stopRecord();
        tearDownListener();

        if (bWaypointExecuted == true) {
            HelperUtils.getInstance().uploadFlightRecord(buttonID, mFlightRecord, uploadHandler);
            bWaypointExecuted = false;
        }
    }


    private void uploadMission() {
        if (waypointMissionOperator == null) return;

        if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
            waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        waypointMissionOperator.retryUploadMission(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null)
                                    showResultToast(djiError);
                                else
                                    startMission();
                            }
                        });
                    }
                    else {
                        startMission();
                    }

                }
            });
        } else if (WaypointMissionState.EXECUTING.equals(waypointMissionOperator.getCurrentState())) {

        }
        else if (WaypointMissionState.EXECUTION_PAUSED.equals(waypointMissionOperator.getCurrentState())) {
            waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showResultToast(djiError);
                    }
                    else {

                    }
                }
            });
        }
    }

    private void loadMission() {
        if (waypointMissionOperator == null) return;

        WaypointManager.getInstance().setMission(buttonID);
        WaypointMission mission = WaypointManager.getInstance().getWaypointMission();

        finMissionCount = allMissionCount = mission.getWaypointCount();

        DJIError djiError = waypointMissionOperator.loadMission(mission);
        if (djiError != null) {
            showResultToast(djiError);
        }
        else {
            uploadMission();
        }
    }

    private void startMission() {
        if (waypointMissionOperator == null) return;
//        if (waypointMissionOperator.getCurrentState() != WaypointMissionState.READY_TO_EXECUTE) {
//            ToastUtils.setResultToToast("Not ready to execute ! Why ??");
//            return;
//        }

        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {

                }
                else {
                    showResultToast(djiError);
                }
            }
        });
    }

    private void stopMission() {
        if (waypointMissionOperator == null) return;
        // Example of stopping a Mission
        waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    stopDataScheduler();
                    stopRecord();
                }
                else {
                    showResultToast(djiError);
                }
            }
        });

    }

    private void tearDownListener() {

        if (waypointMissionOperator != null && listener != null) {
            // Example of removing listeners
            waypointMissionOperator.removeListener(listener);

            listener = null;
        }

        uninitPreviewer();
    }

    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        return null;
    }


    private void showResultToast(DJIError djiError) {
        ToastUtils.setResultToToast(djiError == null ? "Action started!" : djiError.getDescription());
        Log.d(TAG, djiError.getDescription());
    }


    @Override
    protected void onDestroy() {
        DronePlayMissionApplication.getEventBus().unregister(mContext);
        super.onDestroy();
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        if (!ModuleVerificationUtil.isCameraModuleAvailable()) return;

        Camera camera = DronePlayMissionApplication.getProductInstance().getCamera();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        ToastUtils.setResultToToast("Switch Camera Mode Succeeded");
                    } else {
                        ToastUtils.setResultToToast(error.getDescription());
                    }
                }
            });
        }
    }
}
