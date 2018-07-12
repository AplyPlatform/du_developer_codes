package io.droneplay.droneplaymission.Activity;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.Transition;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import dji.common.util.CommonCallbacks;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import io.droneplay.droneplaymission.DronePlayAPI;
import io.droneplay.droneplaymission.DronePlayMissionApplication;
import io.droneplay.droneplaymission.HelperUtils;
import io.droneplay.droneplaymission.ModuleVerificationUtil;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.ToastUtils;
import io.droneplay.droneplaymission.WaypointManager;
import io.droneplay.droneplaymission.tf.Classifier;
import io.droneplay.droneplaymission.tf.TensorFlowImageClassifier;


public class MissionRunActivity extends FragmentActivity implements DJICodecManager.YuvDataCallback {


    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private boolean bCheck = false;

    private static final String TAG = MissionRunActivity.class.getSimpleName();
    public static final String PARAM_BUTTON_ID = "buttonID";
    private Marker currentDroneMarker;

    private Button testButton;
    private Button recogButton;

    private String buttonID;

    private Timer timer = new Timer();
    private long timeCounter = 0;
    private long hours = 0;
    private long minutes = 0;
    private long seconds = 0;
    private String time = "";

    private TextView textView;
    private TextView anView;
    private MapView mMapView = null;
    private MapboxMap mMap = null;

    private int WAYPOINT_COUNT = 0;

    private DronePlayAPI dapi;

    private int videoWidth;
    private int videoHeight;

    protected FlightController flightController;
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    private WaypointMissionOperator waypointMissionOperator = null;
    private WaypointMissionOperatorListener listener;
    private WaypointManager manager;
    private DJICodecManager mCodecManager = null;

    protected double currentLatitude = 0;
    protected double currentLongitude = 0;
    protected float currentAltitude = 0.0f;

    protected double oldLatitude = 0;
    protected double oldLongitude = 0;
    protected float oldAltitude = 0.0f;

    protected SurfaceView videostreamPreviewSf = null;
    private SurfaceHolder videostreamPreviewSh;
    private SurfaceHolder.Callback surfaceCallback;

    private Transition transition;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;

    private Context mContext;
    private Handler handler;

    private boolean bMissionState = false; //stop
    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        DronePlayMissionApplication.getEventBus().register(mContext);

        setContentView(R.layout.activity_run);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        dapi = new DronePlayAPI(mContext);
        manager = WaypointManager.getInstance();
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();

        initTensorFlowAndLoadModel();
        initUI();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void setMapStyle() {

        if (mMap == null) return;

        int kind = HelperUtils.readMapStyle(mContext);

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
        testButton = (Button) findViewById(R.id.testButton);
        testButton.setEnabled(true);

        anView = (TextView) findViewById(R.id.anMon);

        recogButton = (Button) findViewById(R.id.recogButton);

        videostreamPreviewSf = (SurfaceView) findViewById(R.id.video_previewer_surface);
        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

//        mCaptureBtn.setOnClickListener(this);
//        mRecordBtn.setOnClickListener(this);
//        mShootPhotoModeBtn.setOnClickListener(this);
//        mRecordVideoModeBtn.setOnClickListener(this);
        recordingTime.setVisibility(View.INVISIBLE);

        recogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bCheck = true;
                mCodecManager.setYuvDataCallback(MissionRunActivity.this);
                mCodecManager.enabledYuvData(true);
            }
        });

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    recordingTime.setVisibility(View.VISIBLE);
                    startRecord();
                } else {
                    recordingTime.setVisibility(View.INVISIBLE);
                    stopRecord();
                }
            }
        });

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {

                mMap = mapboxMap;

                setMapStyle();

                LatLng latLng = loadMissionsToMap();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
                mMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        //                Intent intent = new Intent(MissionRunActivity.this, MissionRunBigMapActivity.class);
                        //                startActivity(intent);
                        //                finish();
                    }
                });

                testButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (bMissionState == false) {
                            startMission();
                        } else {
                            stopMission();
                        }
                    }
                });
            }
        });
    }

    protected void changeDescription(final String newDescription) {
        new Runnable() {
            @Override
            public void run() {
                recordingTime.setText(newDescription);
            }
        };
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

            videostreamPreviewSh = videostreamPreviewSf.getHolder();
            surfaceCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.d(TAG, "real onSurfaceTextureAvailable");
                    int videoViewWidth = videostreamPreviewSf.getWidth();
                    int videoViewHeight = videostreamPreviewSf.getHeight();
                    Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);

                    if (mCodecManager == null) {

                        mCodecManager = new DJICodecManager(getApplicationContext(), holder, videoViewWidth,
                                videoViewHeight);

//                        mCodecManager.enabledYuvData(true);
//                        mCodecManager.setYuvDataCallback(MissionRunActivity.this);
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
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }

    private void uninitPreviewer() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
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
            return mMap.addMarker(nMarker);
        }

        return null;
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void updateWaypointMissionState() {

        if (Double.isNaN(currentLatitude)
                || Double.isNaN(currentLongitude)
                || Float.isNaN(currentAltitude)) return;

        if (checkGpsCoordination(currentLatitude, currentLongitude) == false) return;

        if (currentAltitude == oldAltitude
                && currentLatitude == oldLatitude
                && currentLongitude == oldLongitude) return;

        dapi.sendMyPosition(currentLatitude, currentLongitude, currentAltitude);
        final LatLng latlng = new LatLng(currentLatitude, currentLongitude);

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if (currentDroneMarker != null)
                    mMap.removeMarker(currentDroneMarker);

                currentDroneMarker = addMarkerToMap(latlng, R.mipmap.drone);

                //textView.setText(currentDroneMarkerName);

                oldAltitude = currentAltitude;
                oldLatitude = currentLatitude;
                oldLongitude = currentLongitude;
            }
        });
    }


    //region Not important stuff
    private void setUpListener() {

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
                        currentLatitude = flightControllerState.getAircraftLocation().getLatitude();
                        currentLongitude = flightControllerState.getAircraftLocation().getLongitude();
                        currentAltitude = flightControllerState.getAircraftLocation().getAltitude();

                        updateWaypointMissionState();
                    }
                });

                mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
                    @Override
                    public void onReceive(byte[] videoBuffer, int size) {


                        if (mCodecManager != null) {
                            mCodecManager.sendDataToDecoder(videoBuffer, size, UsbAccessoryService.VideoStreamSource.Camera.getIndex());
                        }
                    }
                };

                handler = new Handler();
            }
        }


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
                if(mMap != null)
                    mMap.clear();
                bMissionState = true;
                mMapView.setClickable(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        testButton.setText("STOP");
                    }
                });
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                ToastUtils.setResultToToast("Execution finished!");
                updateWaypointMissionState();
                mMapView.setClickable(true);
                bMissionState = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        testButton.setText("START");
                    }
                });
            }
        };

        waypointMissionOperator.addListener(listener);
    }


    private LatLng loadMissionsToMap() {
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

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        Intent i = getIntent();
        buttonID = i.getStringExtra(PARAM_BUTTON_ID);
        manager.setMission(buttonID);


        setUpListener();
        initPreviewer();
        uploadMission();
    }

    private void uploadMission() {
        if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
            waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showResultToast(djiError);
                    }

                }
            });
        } else {
            ToastUtils.setResultToToast("Not ready!");
            finish();
        }
    }

    private void loadMission() {
        WaypointManager.getInstance().setMission(buttonID);
        WaypointMission mission = WaypointManager.getInstance().getWaypointMission();
        DJIError djiError = waypointMissionOperator.loadMission(mission);
        if (djiError != null)
            showResultToast(djiError);
    }

    private void startMission() {
        if (waypointMissionOperator.getCurrentState() == WaypointMissionState.EXECUTING) return;

        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showResultToast(djiError);
            }
        });
    }

    private void stopMission() {
        // Example of stopping a Mission
        waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                showResultToast(djiError);
            }
        });

    }

    private void tearDownListener() {
        if (waypointMissionOperator != null && listener != null) {
            // Example of removing listeners
            waypointMissionOperator.removeListener(listener);
        }

        uninitPreviewer();
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


    private void showResultToast(DJIError djiError) {
        ToastUtils.setResultToToast(djiError == null ? "Action started!" : djiError.getDescription());
    }






    @Override
    protected void onDestroy() {
        DronePlayMissionApplication.getEventBus().unregister(mContext);
        super.onDestroy();
    }

//
//    @Override
//    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.btn_capture:{
//                captureAction();
//                break;
//            }
//            case R.id.btn_shoot_photo_mode:{
//                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
//                break;
//            }
//            case R.id.btn_record_video_mode:{
//                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
//                break;
//            }
//            default:
//                break;
//        }
//    }

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

        // Method for taking photo
    private void captureAction() {
        if (!ModuleVerificationUtil.isCameraModuleAvailable()) return;

        final Camera camera = DronePlayMissionApplication.getProductInstance().getCamera();
        if (camera != null) {
            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (handler != null && null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {

                                        } else {
                                            ToastUtils.setResultToToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }

    @Override
    public void onYuvDataReceived(ByteBuffer byteBuffer, int dataSize, final int width, final int height) {
        if (bCheck == true) {
            bCheck = false;
            final byte[] bytes = new byte[dataSize];
            byteBuffer.get(bytes);

            if (mCodecManager != null) {
                mCodecManager.enabledYuvData(false);
                mCodecManager.setYuvDataCallback(null);
            }
            //DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = getYuvDataToBMP(bytes, width, height);

                    if (bitmap == null) return;

                    bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

                    //imageViewResult.setImageBitmap(bitmap);

                    final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                    if (results != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                anView.setText(results.toString());
                            }
                        });

                    }
                }
            });
        }
    }

    private Bitmap getYuvDataToBMP(byte[] bytes, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        return image;
    }
}
