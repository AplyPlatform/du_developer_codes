package io.droneplay.droneplaymission.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
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
import io.droneplay.droneplaymission.ModuleVerificationUtil;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.ToastUtils;
import io.droneplay.droneplaymission.WaypointManager;


public class MissionRunActivity extends FragmentActivity implements OnMapReadyCallback
        , TextureView.SurfaceTextureListener
        , View.OnClickListener{

    private static final String TAG = MissionRunActivity.class.getSimpleName();
    public static final String PARAM_BUTTON_ID = "buttonID";

    private Button testButton;

    private String buttonID;



    private Timer timer = new Timer();
    private long timeCounter = 0;
    private long hours = 0;
    private long minutes = 0;
    private long seconds = 0;
    private String time = "";

    private TextView textView;
    private GoogleMap  mMap = null;

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

    protected double currentLatitude = 181;
    protected double currentLongitude = 181;
    protected float currentAltitude = 0.0f;
    protected TextureView mVideoSurface = null;

    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;
    private SupportMapFragment mapFragment;

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

        dapi = new DronePlayAPI(mContext);
        manager = WaypointManager.getInstance();
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();

        initUI();
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
                    else {
                        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                showResultToast(djiError);
                            }
                        });
                    }
                }
            });
        } else {
            ToastUtils.setResultToToast("Not ready!");
        }
    }

    private void initUI() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(true);
        mapFragment.getMapAsync(this);

        textView = (TextView) findViewById(R.id.mapMon);
        testButton = (Button) findViewById(R.id.testButton);
        testButton.setEnabled(true);

        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        mVideoSurface.setSurfaceTextureListener(this);
        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);
        recordingTime.setVisibility(View.INVISIBLE);
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

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this
                    , surface, width, height
                    , UsbAccessoryService.VideoStreamSource.Camera);
        }
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }


    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (videoHeight != mCodecManager.getVideoHeight() || videoWidth != mCodecManager.getVideoWidth()) {
            videoWidth = mCodecManager.getVideoWidth();
            videoHeight = mCodecManager.getVideoHeight();
            adjustAspectRatio(videoWidth, videoHeight);
        }
    }

    /**
     * This method should not to be called until the size of `TextureView` is fixed.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {

        int viewWidth = mVideoSurface.getWidth();
        int viewHeight = mVideoSurface.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;

        Matrix txform = new Matrix();
        mVideoSurface.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        mVideoSurface.setTransform(txform);
    }

    private void initPreviewer() {
        BaseProduct product = DronePlayMissionApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            ToastUtils.setResultToToast("Disconnected");
        } else {

            mVideoSurface.setSurfaceTextureListener(this);

            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }

    private void uninitPreviewer() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
    }

    private void updateWaypointMissionState() {
        dapi.sendMyPosition(currentLatitude, currentLongitude, currentAltitude);
        String markerName = mMarkers.size() + ":" + Math.round(currentLatitude) + "," + Math.round(currentLongitude);
        final MarkerOptions nMarker = new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude))
                .title(markerName)
                .snippet(markerName);
        if(mMap != null)
            mMap.addMarker(nMarker);
        textView.setText(markerName);
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
                testButton.setText("START");
            }
        };

        waypointMissionOperator.addListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        Intent i = getIntent();
        buttonID = i.getStringExtra(PARAM_BUTTON_ID);
        manager.setMission(buttonID);

        setUpListener();
        initPreviewer();
        startMission();
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


    private void startMission() {
        if (waypointMissionOperator.getCurrentState() == WaypointMissionState.EXECUTING) return;

        uploadMission();
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

    }


    @Override
    protected void onDestroy() {
        DronePlayMissionApplication.getEventBus().unregister(mContext);
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            default:
                break;
        }
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
                                            ToastUtils.setResultToToast("take photo: success");
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
}
