package io.droneplay.droneplaymission.Activity;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import io.droneplay.droneplaymission.HelperUtils;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.WaypointManager;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, HelperUtils.markerDataInputClickListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private Button clearButton;
    private Button saveButton;

    private String buttonID;

    private TextView textView;
    private GoogleMap mMap;

    private WaypointManager manager;

    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();

    private boolean doubleBackToExitPressedOnce = false;
    private SupportMapFragment mapFragment;

    private int markerid = 0;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mContext = this;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(true);
        mapFragment.getMapAsync(this);

        textView = (TextView) findViewById(R.id.mapMon);
        clearButton = (Button) findViewById(R.id.clearButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        manager = WaypointManager.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        Intent i = getIntent();
        buttonID = i.getStringExtra(MissionRunActivity.PARAM_BUTTON_ID);
        manager.setMission(buttonID);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng dockdo = loadMissionsToMap();

        //mMap.addMarker(new MarkerOptions().position(dockdo).title("Marker in Dokdo"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dockdo, 20));

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.clear();
                mMarkers.clear();
                manager.clear();
                textView.setText("");
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manager.saveMissionToFile(mContext, buttonID);
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick");

                addMarkerToClickPoint(latLng);
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
                    doubleBackToExitPressedOnce = true;
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


    private void addMarkerToClickPoint(LatLng latLng) {
        HelperUtils.showMarkerDataInputDialog(mContext, latLng, this);
    }

    @Override
    public void onMarkerDataInputClick(int altitude, LatLng latLng) {
        String markerName = addMarkerToMap(latLng);
        String showText = String.format("Count: %d\nLat: %.4f, Lng: %.4f", mMarkers.size(), latLng.latitude, latLng.longitude);
        textView.setText(showText);
        manager.addAction(markerName,latLng.latitude,latLng.longitude, altitude, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String addMarkerToMap(LatLng latLng) {
        String markerName = markerid + "";
        MarkerOptions nMarker = new MarkerOptions().position(latLng)
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
