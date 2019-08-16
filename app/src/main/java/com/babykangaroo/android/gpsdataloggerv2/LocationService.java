package com.babykangaroo.android.gpsdataloggerv2;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.babykangaroo.android.mylocationlibrary.LocationAccess;

public class LocationService extends LocationAccess {
    /**
     * public constructor. instantiate this object in the required activity
     * all location services are handled in this object and data can be called with the getter methods defined below
     *
     * @param context                of the instantiating activity
     * @param locationUpdateListener
     */
    public LocationService(Context context, @Nullable LocationUpdateListener locationUpdateListener) {
        super(context, locationUpdateListener);
    }
public LocationService(){}
    @Override
    protected void startLocationUpdates() {

        super.startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Start Sticky should restart killed services
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        //set service to run as foreground service
        Intent notificationIntent = new Intent(mContext, LoggingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext,0,notificationIntent,0);
        Notification notification = new NotificationCompat.Builder(mContext)
                .setContentTitle("Serial Relay")
                .setContentText("Serial Relay is running")
                .setContentIntent(pendingIntent)
                .build();
        this.startForeground(9998, notification);
        //end of "start foreground service" block
        super.onCreate();
    }

    @Override
    protected void stopUpdates() {
        try {
            stopForeground(true);
        }catch (Exception e){}
        super.stopUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
