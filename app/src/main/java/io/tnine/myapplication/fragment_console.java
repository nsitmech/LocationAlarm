package io.tnine.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class fragment_console extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener {


    //declaration of app constituents
    private Button alarmbutton;
    private boolean result = true;

    //if result is true, destination fragment is seen
    //if result is false, alarm settings fragment is seen

    private GoogleApiClient mGoogleApiClient ;


    View frag1;
    View frag2;
    public GoogleMap map;

    double destlat;
    double destlng;

    double latilast,longitlast;
    double updLat, updLng;

    public Marker marker;
    public Marker locmarker;
    public Marker locmarker2;
    EditText current_distance;

    Location mLastLocation;
    LocationRequest mLocationRequest;
    boolean mRequestingLocationUpdates = true;
    LatLng locll;
    LatLng destloc;

    boolean alarm = false;



    public Circle circle;
    double rad = 800;

    Vibrator v;
    Ringtone r;










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_console);




        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        final PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.


                try {
                    geoLocate(place.getAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.

            }
        });


        //declarations
        alarmbutton = (Button) findViewById(R.id.setalarm_button);
        frag1 = findViewById(R.id.destination_fragment);
        frag2 = findViewById(R.id.alarmsetfragment);
        final EditText radius = (EditText) findViewById(R.id.radiusValue);
        current_distance = (EditText) findViewById(R.id.currentDistance);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);


        //code to obtain google map named map
        try {
            if (map == null) {
                map = ((MapFragment) getFragmentManager()
                        .findFragmentById(R.id.map)).getMap();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button OKButton = (Button) findViewById(R.id.OKButton);

        //code to turn some instance variables invisible when program starts
        alarmbutton.setVisibility(View.GONE);
        frag2.setVisibility(View.GONE);
        OKButton.setVisibility(View.GONE);
        current_distance.setVisibility(View.GONE);

        //Initial On click listeners
        alarmbutton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        switchfrags();
                        switchalarmbutton();
                        setCircle(800,destloc);
                    }
                }
        );

        radius.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (radius.getText().toString().length() != 0) {
                    String no = radius.getText().toString();
                    rad = Integer.parseInt(no);
                }

                setCircle(rad, destloc);
            }
        });


        OKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchfrags();
                Toast.makeText(fragment_console.this, "The alarm has been set", Toast.LENGTH_LONG).show();
                alarm = true;
            }
        });

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                destlng = latLng.longitude;
                destlat = latLng.latitude;

                if (frag1.getVisibility() == View.GONE){
                    switchfrags();
                }

                destloc = new LatLng(destlat,destlng);

                if (marker != null) {
                    marker.remove();
                }
                if (circle != null){
                    circle.remove();
                    circle = null;
                }

                MarkerOptions options = new MarkerOptions()
                        .title("your destination")
                        .position(destloc);

                marker = map.addMarker(options);

                if (r.isPlaying() == true ){
                    r.stop();
                    v.cancel();
                }

                alarm = false;
                if(alarmbutton.getVisibility() == View.GONE){
                    alarmbutton.setVisibility(View.VISIBLE);
                }
            }
        });


    }




    //switch methods
    public void switchfrags() {

        Button OKButton = (Button) findViewById(R.id.OKButton);
        current_distance = (EditText) findViewById(R.id.currentDistance);

        if (result == true) {
            frag1.setVisibility(View.GONE);
            frag2.setVisibility(View.VISIBLE);
            OKButton.setVisibility(View.VISIBLE);
            current_distance.setVisibility(View.VISIBLE);

        } else {
            frag1.setVisibility(View.VISIBLE);
            frag2.setVisibility(View.GONE);
            OKButton.setVisibility(View.VISIBLE);
            current_distance.setVisibility(View.GONE);
        }
        result = !result;
    }

    public void switchalarmbutton() {
        if (alarmbutton.getVisibility() == View.GONE) {
            alarmbutton.setVisibility(View.VISIBLE);
        } else {
            alarmbutton.setVisibility(View.GONE);
        }
    }


    public void geoLocate(String location) throws IOException {

        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(location, 1);
        Address add = list.get(0);
        String Locality = add.getLocality();
        Toast.makeText(this, Locality, Toast.LENGTH_LONG).show();

        destlat = add.getLatitude();
        destlng = add.getLongitude();

        destloc = new LatLng(destlat,destlng);

        if (frag1.getVisibility() == View.GONE){
            switchfrags();
        }

        gotoLocation(destlat, destlng, 14);
        String locality = add.getLocality();
        if (marker != null) {
            marker.remove();
        }
        if (circle != null){
            circle.remove();
            circle = null;
        }

        MarkerOptions options = new MarkerOptions()
                .title(locality)
                .position(destloc);

        marker = map.addMarker(options);

        if (r.isPlaying()){
            r.stop();
            v.cancel();
        }

        if (alarmbutton.getVisibility() == View.GONE){
            switchalarmbutton();
        }
        alarm = false;
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
       if (map != null){
           try{
               mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                       mGoogleApiClient);
               if (mLastLocation != null) {

                   latilast = mLastLocation.getLatitude();
                   longitlast = mLastLocation.getLongitude();
                   locll = new LatLng(latilast,longitlast);

                   MarkerOptions locoptions = new MarkerOptions()
                           .title("You are here")
                           .position(locll)
                           .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed))
                           .anchor(0.5f, 0.5f);
                   if (locmarker != null){
                       locmarker.remove();
                   }
                   locmarker = map.addMarker(locoptions);
                   gotoLocation(latilast, longitlast, 14);
               }
           }catch(SecurityException e){
               //do nothing
           }
       }
        if(mRequestingLocationUpdates){
            createLocationRequest();
            startLocationUpdates();
        }
    }



    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void gotoLocation(double lat, double lng, int Zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, Zoom);
        map.animateCamera(update);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient .disconnect();
        super.onStop();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {

        createLocationRequest();

        try{
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);

        }catch (SecurityException e){
            //code to ask for permission
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        updLat = location.getLatitude();
        updLng = location.getLongitude();
        locll = new LatLng(updLat,updLng);

            MarkerOptions locoptions = new MarkerOptions()
                    .title("You are here")
                    .position(locll)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_gps_fixed))
                    .anchor(0.5f, 0.5f);
        if(locmarker != null){
            locmarker2 = locmarker;
            locmarker2 = map.addMarker(locoptions);
            locmarker.remove();
            locmarker = null;
        }
        locmarker = map.addMarker(locoptions);
        if(locmarker2 != null){
            locmarker2.remove();
            locmarker2 = null;
        }





        if (alarm == true){

            double distance;
            Location locationA = new Location("");
            locationA.setLatitude(destlat);
            locationA.setLongitude(destlng);
            Location locationB = new Location("");
            locationB.setLatitude(updLat);
            locationB.setLongitude(updLng);
            distance = locationA.distanceTo(locationB);
            if(distance<rad){


                alertUser();
                Toast.makeText(fragment_console.this,"Destination is now within your range",Toast.LENGTH_LONG).show();

            }

        }

        if (frag2.getVisibility() == View.VISIBLE){
            double distance;
            Location locationA = new Location("");
            locationA.setLatitude(destlat);
            locationA.setLongitude(destlng);
            Location locationB = new Location("");
            locationB.setLatitude(updLat);
            locationB.setLongitude(updLng);
            distance = locationA.distanceTo(locationB);


            EditText current_distance = (EditText)findViewById(R.id.currentDistance);
            current_distance.setText("Current distance: " + Math.round(distance) + " m");
        }

        startLocationUpdates();



    }
    //stopping location updates
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    //resuming
    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }







    public void setCircle(double radius, LatLng t) {
        CircleOptions cOptions = new CircleOptions()
                .center(t)
                .radius(radius)
                .strokeWidth(5)
                .strokeColor(Color.RED)
                .fillColor(0x33FF0000);


        if (circle != null){
            circle.remove();
            circle = null;
        }

        circle = map.addCircle(cOptions);
    }

    public void alertUser(){



        if (!r.isPlaying()){
            r.play();
            long[] pattern = {0, 600, 1000};
            v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(pattern, 0);
        }

        alarm = false;

    }






}




