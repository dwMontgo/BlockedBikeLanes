package com.example.android.blockedbikelanes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.blockedbikelanes.model.MarkerData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Where the map data lives in Firebase
    public static final String DB_PATH = "/uploads/";
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int zoomDefault = 15;
    private static final int FINE_LOCATION_PERMISSION = 1;
    private static final String MAP_CAMERA = "map_camera";
    private static final String LOCATION = "location";
    private final LatLng latLngDefault = new LatLng(37.422001, -122.084055);
    private GoogleMap map;
    private CameraPosition cameraPosition;
    private GoogleApiClient googleApiClient;
    private boolean locationPermissionBoolean;
    private Location location;
    // A cache of map points displayed on the map
    private HashMap<String, Uri> markers = new HashMap<>();

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            location = savedInstanceState.getParcelable(LOCATION);
            cameraPosition = savedInstanceState.getParcelable(MAP_CAMERA);
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(DB_PATH);


        //Show/hide button - retrieve fragment
        FragmentManager fragmentManager = getFragmentManager();
        addShowHideListener(R.id.frag1hide, fragmentManager.findFragmentById(R.id.fragment1));
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(MAP_CAMERA, map.getCameraPosition());
            outState.putParcelable(LOCATION, location);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Get SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.getUiSettings().setMyLocationButtonEnabled(false);

        updateLocation();

        getDeviceLocation();

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                LatLng myLatLon = dataSnapshot.getValue(MarkerData.class).toLatLng();
                String address = dataSnapshot.getValue(MarkerData.class).getAddress();
                String date = dataSnapshot.getValue(MarkerData.class).getDate();
                String time = dataSnapshot.getValue(MarkerData.class).getTime();
                String comment = dataSnapshot.getValue(MarkerData.class).getComment();
                String url = dataSnapshot.getValue(MarkerData.class).getUrl();
                Uri imageUri = Uri.parse(url);


                placeMarker(myLatLon, address, date, time, comment, imageUri);

            }



            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                LatLng myLatLon = dataSnapshot.getValue(MarkerData.class).toLatLng();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void placeMarker(LatLng myLatLon,
                             String address, String date, String time, String comment, Uri imageUri) {

        Marker marker =
                map.addMarker(new MarkerOptions().position(myLatLon)
                        .title("placeholder")
                        .snippet("Address:  " + address + "\n" + "Date:  " + date + "\n"
                                + "Time:  " + time + "\n" + "Comment:  " + comment)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.alert)));

        if (imageUri != null) {
            markers.put(marker.getId(),
                    imageUri);
        }

        map.setInfoWindowAdapter(new InfoWindowInfo(this,
                getLayoutInflater(),
                markers));
    }


    private void getDeviceLocation() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionBoolean = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (locationPermissionBoolean) {
            location = LocationServices.FusedLocationApi
                    .getLastLocation(googleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (location != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(),
                            location.getLongitude()), zoomDefault));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngDefault, zoomDefault));
            map.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }


    private void updateLocation() {
        if (map == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionBoolean = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION);
        }

        if (locationPermissionBoolean) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            map.setMyLocationEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            location = null;
        }
    }


    /**
     * Show/hide button for fragment
     */

    void addShowHideListener(int buttonId, final Fragment fragment) {
        final Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(android.R.animator.fade_in,
                        android.R.animator.fade_out);
                Intent i = new Intent(MapsActivity.this, MarkMapActivity.class);
                startActivity(i);
            }

//            }
//        });
        });
    }

    /**
     * Initializes the activity menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Select an option from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_about:
                // Marker chose the "About" action, go to the about activity
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;


            default:
                // If we got here, the time's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Show/hide fragment for submit incident
     */

    public static class ReportFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.labeled_text_edit, container, false);
            View tv = v.findViewById(R.id.geocoded_address);
            ((TextView) tv).setText(R.string.msg);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.hide(this);
            fragmentTransaction.commit();

            return v;
        }
    }

}