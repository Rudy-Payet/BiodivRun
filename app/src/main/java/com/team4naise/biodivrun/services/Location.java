package com.team4naise.biodivrun.services;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.team4naise.biodivrun.data.Zone;

public class Location {

    private final AppCompatActivity activity;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private int rayonKm = 10;

    private final ActivityResultLauncher<String> permissionLauncher;

    public Location(AppCompatActivity activity) {
        this.activity = activity;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        startLocationUpdates();
                    }
                });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                android.location.Location loc = result.getLastLocation();
                if (loc != null) {
                    calculerZone(loc);
                }
            }
        };

        demanderPermission();
    }

    private void demanderPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000).build();

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void calculerZone(android.location.Location centre) {
        double degLat = (double) rayonKm / 111.0;
        double degLon = (double) rayonKm / (111.0 * Math.cos(Math.toRadians(centre.getLatitude())));

        double latMin = centre.getLatitude() - degLat;
        double latMax = centre.getLatitude() + degLat;
        double lonMin = centre.getLongitude() - degLon;
        double lonMax = centre.getLongitude() + degLon;

        // Bounding box prête à être utilisée
    }

    public void stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}