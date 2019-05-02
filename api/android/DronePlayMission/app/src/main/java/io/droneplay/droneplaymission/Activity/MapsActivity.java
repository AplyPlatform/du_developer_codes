package io.droneplay.droneplaymission.Activity;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import io.droneplay.droneplaymission.model.WaypointData;
import io.droneplay.droneplaymission.utils.HelperUtils;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.utils.WaypointManager;


public class MapsActivity extends AppCompatActivity implements HelperUtils.markerDataInputClickListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private Button clearButton;
    private Button saveButton;
    private Button mapButton;

    private String buttonID;

    private TextView textView;
    private MapView mMapView;
    private MapboxMap mMap;

    private WaypointManager manager;

    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();

    private Context mContext;

    private int mapKind = 0;
    private int markerIndex = 0;


    private static ProgressDialog spinner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mContext = this;

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        textView = (TextView) findViewById(R.id.mapMon);
        clearButton = (Button) findViewById(R.id.clearButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        manager = WaypointManager.getInstance();

        getSupportActionBar().hide();


        spinner = new ProgressDialog(this);
        spinner.setCancelable(false);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {

                mMap = mapboxMap;
                mMap.setStyle(getString(R.string.mapbox_style_satellite_streets));
                // Customize map with markers, polylines, etc.

                LatLng dockdo = loadMissionsToMap();
                showCurrentPos(dockdo);

                clearButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMap.clear();
                        mMarkers.clear();
                        manager.clear();
                        textView.setText("");

                        LatLng dockdo = loadDefaultPosition();
                        showCurrentPos(dockdo);
                    }
                });

                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showLoader();
                        HelperUtils.getInstance().saveMapStyle(mContext, mapKind);
                        manager.saveMissionToServer(mContext, buttonID, saveHandler);
                    }
                });

                mapButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeMapStyle();
                    }
                });

                mMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        Log.d(TAG, "onMapClick");
                        addMarker(4, point);
                    }
                });


                mMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        Log.d(TAG, "onMarkerClick");

                        String strTitle = marker.getTitle();
                        WaypointData d = manager.getData(strTitle);
                        modifyMarkerFromClickPoint(strTitle, (int) d.alt, d.act, d.actparam, d.speed);
                        return false;
                    }
                });
            }
        });
    }


    @SuppressLint("HandlerLeak")
    private final Handler saveHandler = new Handler() {
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
                            showToast("Successfully, saved.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("Failed - "  + e.getMessage());
                    }

                    return;
                case R.id.req_failed:
                    showToast("Failed to save");
                    break;
            }
        }
    };


    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();

            }
        });
    }



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





    private void changeMapStyle() {

        if (mMap == null) return;

        mapKind++;
        if(mapKind > 5) mapKind = 0;

        String strKind = getString(R.string.mapbox_style_satellite_streets);;
        switch(mapKind) {
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

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        mMapView.onResume();

        Intent i = getIntent();
        buttonID = i.getStringExtra(MissionRunActivity.PARAM_BUTTON_ID);
        manager.setMission(buttonID);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        mMapView.onPause();
    }


    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        return null;
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

        markerIndex = 0;
        for (Waypoint pt : mList) {
            LatLng ltlng = new LatLng(pt.coordinate.getLatitude(), pt.coordinate.getLongitude());
            addMarkerToMap(ltlng);
            dockdo = ltlng;
        }

        return dockdo;
    }

    private void modifyMarkerFromClickPoint(String markerName, int altitude, int act, int actParam, int speed) {
        HelperUtils.getInstance().showMarkerDataInputDialog(this, markerName, altitude, act, actParam, speed, this);
    }

    private void modifyMarker(String markerName, int altitude, int act, int actParam, int speed) {
        manager.modifyAction(markerName, altitude, act, actParam, speed);
    }

    private void deleteMarker(String markerName) {
        List<Marker> markers = mMap.getMarkers();

        for(Marker marker : markers) {
            if (marker.getTitle().equalsIgnoreCase(markerName)) {
                marker.remove();
                break;
            }
        }

        mMarkers.remove(markerName);
        manager.removeAction(markerName);
    }

    private void addMarker(int altitude, LatLng latLng) {
        String markerName = addMarkerToMap(latLng);
        String showText = String.format("Count: %d\nLat: %.4f, Lng: %.4f", mMarkers.size(), latLng.getLatitude(), latLng.getLongitude());
        textView.setText(showText);
        manager.addAction(markerName,latLng.getLatitude(),latLng.getLongitude(), altitude, 0, 0, 5);
    }

    @Override
    public void onMarkerDataInputClick(String markerName, int nHow, int altitude, int act, int actParam, int speed) {

        switch(nHow) {
            case 0:
                deleteMarker(markerName);
                break;
            case 1:
                modifyMarker(markerName, altitude, act, actParam, speed);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();

        if (spinner != null) {
            spinner.dismiss();
            spinner = null;
        }
    }

    private void showCurrentPos(LatLng latLng) {
        if (mMap == null) return;

        String markerName = "CurMarker";
        IconFactory iconFactory = IconFactory.getInstance(mContext);
        Icon icon = iconFactory.fromResource(R.mipmap.drone);
        MarkerOptions nMarker = new MarkerOptions().position(latLng)
                .icon(icon)
                .title(markerName)
                .snippet(markerName);
        mMap.addMarker(nMarker);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
    }

    private String addMarkerToMap(LatLng latLng) {
        String markerName = "mid-" + markerIndex;
        IconFactory iconFactory = IconFactory.getInstance(mContext);
        Icon icon = iconFactory.fromResource(R.mipmap.mission_flag);
        MarkerOptions nMarker = new MarkerOptions().position(latLng)
                .icon(icon)
                .title(markerName)
                .snippet(markerName);
        mMarkers.put(markerName, nMarker);

        mMap.addMarker(nMarker);
        CameraPosition pos = mMap.getCameraPosition();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, pos.zoom));
        markerIndex++;

        return markerName;
    }
}
