package com.example.crowdedhackathon;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap _map;
    private LatLng _userPos = null;
    private FusedLocationProviderClient _user;
    private ArrayList<LatLng> _markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Gets location service for user
        _user = LocationServices.getFusedLocationProviderClient(this);

        _markers = new ArrayList<>();

        giveFeatureToButtons();
        //if user already gave location permission
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
        else{
            //asks for permission
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 11);
        }


    }

    //adds button features
    private void giveFeatureToButtons(){
        Button getLoc = findViewById(R.id.get_location);
        getLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_userPos != null) {
                    //clears map form markers
                    _map.clear();
                    _markers.clear();
                    _markers.add(_userPos);

                    //puts marker to current location
                    _map.addMarker(new MarkerOptions().position(_userPos).title("Your Location"));
                    goLocation(_userPos);
                }
            }
        });

        Button addHMap = findViewById(R.id.add_heat_map);
        addHMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_markers.size() == 2){
                    try {
                        new HeatMapCalc(_map, _markers, 150, 30);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
        _map = googleMap;
        mapOnClickBehaviour();
    }

    private void mapOnClickBehaviour(){
        _map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                if(_markers.size() >= 2){
                    _map.clear();
                    _markers.clear();
                }
                _markers.add(point);
                _map.addMarker(new MarkerOptions().position(point).title("User Marker"));
            }
        });
    }

    //zooms camera to location
    private void goLocation(LatLng loc){
        if(loc != null){
            _map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 10));
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = _user.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    _userPos = new LatLng(location.getLatitude(), location.getLongitude());

                }
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == 11){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation(); //takes location if permission granted
            }
        }
    }
}