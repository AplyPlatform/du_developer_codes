package io.droneplay.droneplaymission;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private Button clearButton;
    private Button saveButton;

    private String buttonID;

    private TextView textView;
    private GoogleMap mMap;

    private WaypointManager manager;

    private final Map<String, MarkerOptions> mMarkers = new ConcurrentHashMap<String, MarkerOptions>();

    private boolean doubleBackToExitPressedOnce = false;

    final float baseAltitude = 10.0f;
    private SupportMapFragment mapFragment;

    private int markerid = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(true);
        mapFragment.getMapAsync(this);

        textView = (TextView) findViewById(R.id.mapMon);
        clearButton = (Button) findViewById(R.id.clearButton);
        saveButton = (Button) findViewById(R.id.saveButton);

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        Intent i = getIntent();
        buttonID = i.getStringExtra("buttonID");
        manager = new WaypointManager(buttonID);
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
                manager.saveMissionToFile(buttonID);
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
        super.onDestroy();
    }
}
