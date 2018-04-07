package com.example.pc.currentlocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private MapView mapView;
    private ImageView imageView;
    private GoogleMap googleMap;
    private TextView textView;
    private GoogleApiClient mGoogleApiClient;
    private static Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private Activity activity;
    private Geocoder geocoder;
    List<Address> addresses;

    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String knownName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.mapView);
        textView = (TextView) findViewById(R.id.txt);
        imageView = (ImageView) findViewById(R.id.location);

        mapView.onCreate(savedInstanceState);
        mapView.onResume(); // needed to get the map to display immediately

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
            }
        });

        geocoder = new Geocoder(this, Locale.getDefault());


        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkPermission();

                final Location loc = getmCurrentLocation();

                if (loc != null) {

                    LatLng latLng = new LatLng(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude());

                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(latLng).title("You are here").snippet("Marker Description").draggable(true));

                    CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(10).build();
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    try {
                        addresses = geocoder.getFromLocation(getmCurrentLocation().getLatitude(), getmCurrentLocation().getLongitude(), 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getAddress();

                    textView.setText("Lat : " + getmCurrentLocation().getLatitude() + "\nLng : " + getmCurrentLocation().getLongitude() + " \nAddress : " + address +
                            " City : " + city + " State : " + state + " Country : " + country + " Postalcode : " + postalCode);

                } else {
                    Toast.makeText(getApplicationContext(), "Current Location not found", Toast.LENGTH_SHORT).show();
                }

                googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

                    @Override
                    public void onMarkerDragStart(Marker marker) {
                        // marker.setDraggable(true);
                        Log.d("System out", "onMarkerDragStart..." + marker.getPosition().latitude + "..." + marker.getPosition().longitude);
                    }

                    @Override
                    public void onMarkerDrag(Marker marker) {
                        //marker.setDraggable(true);
                        Log.d("System out", "onMarkerDrag..." + marker.getPosition().latitude + "..." + marker.getPosition().longitude);
                    }

                    @Override
                    public void onMarkerDragEnd(Marker marker) {

                        Log.d("System out", "onMarkerDragEnd..." + marker.getPosition().latitude + "..." + marker.getPosition().longitude);
                        LatLng latLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
                        try {
                            addresses = geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        getAddress();
                        googleMap.clear();

                        googleMap.addMarker(new MarkerOptions().position(latLng).title("You are here").snippet("Marker Description").draggable(true));
                        textView.setText("Lat : " + marker.getPosition().latitude + "\nLng : " + marker.getPosition().longitude + " \nAddress : " + address +
                                " City : " + city + " State : " + state + " Country : " + country + " Postalcode : " + postalCode);

                    }
                });

            }
        });

    }

    public void getAddress() {


        address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        city = addresses.get(0).getLocality();
        state = addresses.get(0).getAdminArea();
        country = addresses.get(0).getCountryName();
        postalCode = addresses.get(0).getPostalCode();
        knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

    }


    public void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkLocationPermission()) {
                createLocationRequest();
                mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build();
                mGoogleApiClient.connect();
                settingRequest();
            }

        } else {
            createLocationRequest();
            mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
            mGoogleApiClient.connect();
            settingRequest();
        }
    }

    public boolean checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            return false;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (mGoogleApiClient == null) {
                        createLocationRequest();
                        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
                        mGoogleApiClient.connect();
                    }

                } else {

                    Toast.makeText(MainActivity.this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }


    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    protected void startLocationUpdates() {

        @SuppressLint("MissingPermission") PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MainActivity.this);

        @SuppressLint("MissingPermission") android.location.Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        if (mLastLocation != null) {

            Log.d("CurrentLatlong==", "Lat:" + mLastLocation.getLatitude() + " Lng:" + mLastLocation.getLongitude());
            setmCurrentLocation(mLastLocation);
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;

        if (mCurrentLocation != null) {
            setmCurrentLocation(mCurrentLocation);
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) MainActivity.this);
    }


    public static Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    public void setmCurrentLocation(Location mCurrentLocation) {
        this.mCurrentLocation = mCurrentLocation;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
            }
        }
    }

    /*Method to get the enable location settings dialog*/
    public void settingRequest() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

            @Override
            public void onResult(@NonNull LocationSettingsResult result) {

                final Status status = result.getStatus();

                switch (status.getStatusCode()) {

                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, MY_PERMISSIONS_REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:

                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made

                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, "GPS not Enabled", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
        }
    }
}