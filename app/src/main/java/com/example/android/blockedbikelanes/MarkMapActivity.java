package com.example.android.blockedbikelanes;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.blockedbikelanes.model.MarkerData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class MarkMapActivity extends AppCompatActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {


    public static final String FIREBASE_STORAGE_PATH = "uploads/";
    public static final String FIREBASE_DATABASE_PATH = "uploads";
    private static final String TAG = MarkMapActivity.class.getSimpleName();
    private static final int zoomDefault = 15;
    private static final int FINE_LOCATION_PERMISSION = 1;
    private static final String MAP_CAMERA = "map_camera";
    private static final String LOCATION = "location";
    private static final int IMAGE_INT = 3;
    private final LatLng latLngDefault = new LatLng(37.422001, -122.084055);
    private GoogleMap map;
    private CameraPosition cameraPosition;
    private GoogleApiClient googleApiClient;
    private boolean locationPermissionBoolean;
    private Location lastLocation;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Marker mMarker;
    private LatLng latLng;
    private Button mSubmitButton;
    private EditText comment;
    private TextView time;
    private TextView date;
    private TextView address;
    private ImageView imageView;
    private Uri filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_map);

        // Retrieve lastLocation and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastLocation = savedInstanceState.getParcelable(LOCATION);
            cameraPosition = savedInstanceState.getParcelable(MAP_CAMERA);
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();


        mSubmitButton = findViewById(R.id.submit_report);
        comment = findViewById(R.id.comment_text);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);

        time = findViewById(R.id.time_text);
        date = findViewById(R.id.date_text);

        ImageButton imageButton = findViewById(R.id.buttonChoose);
        imageView = findViewById(R.id.imageView);

        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DATABASE_PATH);

        if (minute < 10) {
            time.setText(hour + ":0" + minute);
        } else {
            time.setText(hour + ":" + minute);
        }

        String mDate = DateFormat.getDateInstance().format(new Date());
        date.setText(mDate);

        mMarker = null;
        submitButtonListener();

        imageButton.setOnClickListener(this);


        //Show/hide button - retrieve fragment
        FragmentManager fm = getFragmentManager();
        addShowHideListener(R.id.frag1hide, fm.findFragmentById(R.id.fragment1));
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_INT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_INT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadFile() {
        //checking if file is available
        if (filePath != null) {
            //displaying progress dialog while image is uploading
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(R.string.uploading);
            progressDialog.show();

            //getting the storage reference
            StorageReference storageReference = this.storageReference.child(FIREBASE_STORAGE_PATH + System.currentTimeMillis() + "." + getFileExtension(filePath));

            //adding the file to reference
            storageReference.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //dismissing the progress dialog
                            progressDialog.dismiss();

                            //displaying success toast
                            Toast.makeText(getApplicationContext(), R.string.uploaded_toast, Toast.LENGTH_LONG).show();


                            //here
                            Task<Uri> getUrl = taskSnapshot.getStorage().getDownloadUrl();
                            while (!getUrl.isSuccessful());
                            {
                                Uri downloadUrl = getUrl.getResult();

                                final String urlString = String.valueOf(downloadUrl);

                                //creating the upload object to store uploaded image details
                                MarkerData markerData = new MarkerData(comment.getText().toString(), time.getText().toString(), date.getText().toString(), address.getText().toString(), latLng, urlString);

                                //adding an upload to firebase databasesel
                                String uploadString = databaseReference.push().getKey();
                                databaseReference.child(uploadString).setValue(markerData);

                            }

                            Intent i = new Intent(MarkMapActivity.this, MapsActivity.class);
                            startActivity(i);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //displaying the upload progress
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(MAP_CAMERA, map.getCameraPosition());
            outState.putParcelable(LOCATION, lastLocation);
            super.onSaveInstanceState(outState);
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a mMarker near Sydney, Australia.
     * If Google Play services is not installed on the device, the time will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the time has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        updateLocation();

        getDeviceLocation();

        map.getUiSettings().setMyLocationButtonEnabled(false);

        Toast.makeText(getApplicationContext(),
                R.string.marker_toast, Toast.LENGTH_LONG).show();

        // Setting a click event handler for the map
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {

                // Getting the Latitude and Longitude of the touched location
                latLng = arg0;

                // Clears the previously touched position
                map.clear();

                // Animating to the touched position
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                // Creating a mMarker
                mMarker = map.addMarker(new MarkerOptions()
                        .position(latLng));


                // Adding Marker on the touched location with address
                new ReverseGeocodingTask(getBaseContext()).execute(latLng);

            }
        });
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

        if (locationPermissionBoolean) {
            lastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(googleApiClient);
        }


        if (cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (lastLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lastLocation.getLatitude(),
                            lastLocation.getLongitude()), zoomDefault));
        } else {
//            Log.d(TAG, "Current lastLocation is null. Using defaults.");
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngDefault, zoomDefault));
            map.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        locationPermissionBoolean = false;
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionBoolean = true;
                }
            }
        }
        updateLocation();
    }

    private void updateLocation() {
        if (map == null) {
            return;
        }

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
            lastLocation = null;
        }
    }

    /**
     * Show/hide button for fragment
     */
    //7.25
    void addShowHideListener(int buttonId, final Fragment fragment) {
        final Button button = findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(android.R.animator.fade_in,
                        android.R.animator.fade_out);
                if (fragment.isHidden()) {
                    ft.show(fragment);
                    button.setText(R.string.report_incident);
                } else {
                    ft.hide(fragment);
                    button.setText(R.string.hide);
                }
                ft.commit();
            }
        });
    }

    // Submit button in labeled text edit
    private void submitButtonListener() {
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();

            }
        });
    }

    //    Attach image button
    @Override
    public void onClick(View view) {
        showFileChooser();
    }

    /**
     * Show/hide fragment for submit incident
     */
    //7.25
    public static class ReportFragment extends Fragment {
        TextView mTextView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.labeled_text_edit, container, false);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.show(this);
            fragmentTransaction.commit();

            return v;
        }
    }

    private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
        Context mContext;

        public ReverseGeocodingTask(Context context) {
            super();
            mContext = context;
        }

        // Find address using reverse geocoding
        @Override
        protected String doInBackground(LatLng... params) {
            double latitude = params[0].latitude;
            double longitude = params[0].longitude;
            StringBuilder result = new StringBuilder();
            try {
                Geocoder geocoder = new Geocoder(mContext);
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses.size() > 0) {
                    android.location.Address address = addresses.get(0);
                    result.append(address.getAddressLine(0));
                }
            } catch (IOException e) {
                Log.e("tag", e.getMessage());
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(String addressText) {

            mMarker = map.addMarker(new MarkerOptions()
                    .position(latLng));

            address = findViewById(R.id.geocoded_address);
            address.setText(addressText);

        }
    }


}

