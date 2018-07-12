package io.droneplay.droneplaymission.Activity;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import io.droneplay.droneplaymission.HelperUtils;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.WaypointManager;


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

    private boolean doubleBackToExitPressedOnce = false;

    private int markerid = 0;

    private Context mContext;

    private int mapKind = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mContext = this;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        textView = (TextView) findViewById(R.id.mapMon);
        clearButton = (Button) findViewById(R.id.clearButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        manager = WaypointManager.getInstance();

        mMapView.setStyleUrl(Style.DARK);
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
                        HelperUtils.saveMapStyle(mContext, mapKind);
                        manager.saveMissionToFile(mContext, buttonID);
                    }
                });

                mapButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeMapStyle();
                    }
                });

                mMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                                               @Override
                                               public void onMapClick(@NonNull LatLng point) {
                                                   Log.d(TAG, "onMapClick");

                                                   addMarkerToClickPoint(point);
                                               }
                });

                mMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        Log.d(TAG, "onMarkerClick");
                        if (doubleBackToExitPressedOnce) {
                            mMarkers.remove(marker.getTitle());
                            manager.removeAction(marker.getTitle());
                            marker.remove();
                            String showText = String.format("Count: %d", mMarkers.size());
                            textView.setText(showText);
                        } else {
                            doubleBackToExitPressedOnce = true;
                            new Handler().postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    doubleBackToExitPressedOnce = false;
                                }
                            }, 1500);
                        }

                        return false;
                    }
                });
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
        mMapView.onStart();

        Intent i = getIntent();
        buttonID = i.getStringExtra(MissionRunActivity.PARAM_BUTTON_ID);
        manager.setMission(buttonID);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        mMapView.onStart();
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

        for (Waypoint pt : mList) {
            LatLng ltlng = new LatLng(pt.coordinate.getLatitude(), pt.coordinate.getLongitude());
            addMarkerToMap(ltlng);
            dockdo = ltlng;
            markerid++;
        }

        return dockdo;
    }

    private void addMarkerToClickPoint(LatLng latLng) {
        HelperUtils.showMarkerDataInputDialog(mContext, latLng, this);
    }

    @Override
    public void onMarkerDataInputClick(int altitude, LatLng latLng) {
        String markerName = addMarkerToMap(latLng);
        String showText = String.format("Count: %d\nLat: %.4f, Lng: %.4f", mMarkers.size(), latLng.getLatitude(), latLng.getLongitude());
        textView.setText(showText);
        manager.addAction(markerName,latLng.getLatitude(),latLng.getLongitude(), altitude, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onStart();
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
        //CameraPosition pos = mMap.getCameraPosition();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
    }

    private String addMarkerToMap(LatLng latLng) {
        String markerName = markerid + "";
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
        markerid++;

        return markerName;
    }
}
