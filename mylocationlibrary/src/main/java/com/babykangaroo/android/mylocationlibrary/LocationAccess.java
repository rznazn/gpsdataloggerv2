package com.babykangaroo.android.mylocationlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/**
 * Created by sport on 7/12/2017.
 */

public class LocationAccess {
    /**
     * location service variables
     */
    private FusedLocationProviderClient mLocationClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 9999;

    private Context mContext;

    /**
     * variables for location aspects
     */
    private double mLatitude;
    private double mLongitude;
    private double mAltitude;
    private long mGPSTime;
    private double mAccuracy;
    private float mBearing;
    private double mSpeed;


    /**
     * public constructor. instantiate this object in the required activity
     * all location services are handled in this object and data can be called with the getter methods defined below
     * @param context of the instantiating activity
     */
    public LocationAccess(Context context){
        mContext = context;
        /**
         * initiate the FusedLocationProviderClient that will provide the last known locations.
         */
        mLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        /**
         * request location permission
         */
        ActivityCompat.requestPermissions((Activity) mContext,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);

        /**
         * initialize LocationCallback
         * define what to do when the locationrequest receives a reply
         */
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mLocation = location;
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    mAltitude = location.getAltitude();
                    mGPSTime = location.getTime();
                    mAccuracy = location.getAccuracy();
                    mBearing = location.getBearing();
                    mSpeed = location.getSpeed();
                }
            }
        };

        /**
         * create location request
         */

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startLocationUpdates();

//        if (ActivityCompat.checkSelfPermission( mContext,
//                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            mLocationClient.getLastLocation()
//                    .addOnSuccessListener((Activity) mContext, new OnSuccessListener<Location>() {
//                        @Override
//                        public void onSuccess(Location location) {
//                            // Got last known location. In some rare situations this can be null.
//                            if (location != null) {
//                                mLocation = location;
//                            }
//                        }
//                    });
//        }

    }

    /**
     * begin recieving location updates as defined by mLocationRequest
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission( mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
        }
    }
    /**
     * getter methods
     */
    public Location getLastKnownLocation(){
        return mLocation;
    }

    public double getmLatitude(){return mLatitude;}
    public double getmLongitude(){return mLongitude;}
    public double getmAltitude(){return mAltitude;}
    public long getmGPSTime(){return mGPSTime;} //time is of the last received location not current
    public double getmAccuracy(){return mAccuracy;}
    public float getmBearing(){return mBearing;}
    public double getmSpeed(){return mSpeed;}
}
