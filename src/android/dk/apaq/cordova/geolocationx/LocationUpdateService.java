package dk.apaq.cordova.geolocationx;

import de.greenrobot.event.EventBus;

import java.util.List;
import java.util.Iterator;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;

import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import android.util.Log;
import android.widget.Toast;

import static java.lang.Math.*;

public class LocationUpdateService extends Service implements LocationListener {
    private static final String TAG = "LocationUpdateService";
    public static final String ACTION_START = "dk.apaq.cordova.geolocationx.START";
    public static final String ACTION_STOP = "dk.apaq.cordova.geolocationx.STOP";
    public static final String ACTION_CONFIGURE = "dk.apaq.cordova.geolocationx.CONFIGURE";
    public static final String ACTION_SET_MINIMUM_DISTANCE = "dk.apaq.cordova.geolocationx.SET_MINIMUM_DISTANCE";
    public static final String ACTION_SET_MINIMUM_INTERVAL = "dk.apaq.cordova.geolocationx.SET_MINIMUM_INTERVAL";
    public static final String ACTION_SET_PRECISION = "dk.apaq.cordova.geolocationx.SET_PRECISION";


    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private Location lastLocation;

    private Boolean isDebugging = false;

    private String notificationTitle = "";
    private String notificationText = "";
    private Long locationTimeout;
    private String activityType;

    private LocationManager locationManager;
    private NotificationManager notificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "OnBind" + intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "OnCreate");

        locationManager         = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        notificationManager     = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null) {

            Log.d(TAG, "Action: " + intent.getAction());

            // debug intent values values
            Bundle bundle = intent.getExtras();
            if(bundle != null) {
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    Log.d(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
                }
            }


            if(intent.getAction().equals(ACTION_START)) {
                this.startRecording();

                // Build a Notification required for running service in foreground.
                Intent main = new Intent(this, BackgroundGpsPlugin.class);
                main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, main,  PendingIntent.FLAG_UPDATE_CURRENT);

                Notification.Builder builder = new Notification.Builder(this);
                builder.setContentTitle(notificationTitle);
                builder.setContentText(notificationText);
                builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
                builder.setContentIntent(pendingIntent);
                Notification notification;
                notification = builder.build();
                notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
                startForeground(startId, notification);
            }

            if(intent.getAction().equals(ACTION_CONFIGURE)) {
                locationTimeout = Long.parseLong(intent.getStringExtra("locationTimeout"));
                isDebugging = Boolean.parseBoolean(intent.getStringExtra("isDebugging"));
                notificationTitle = intent.getStringExtra("notificationTitle");
                notificationText = intent.getStringExtra("notificationText");
                activityType = intent.getStringExtra("activityType");

                Log.i(TAG, "- notificationTitle: "  + notificationTitle);
                Log.i(TAG, "- notificationText: "   + notificationText);
            }

            if(intent.getAction().equals(ACTION_SET_MINIMUM_DISTANCE)) {
                // TODO
                Log.i(TAG, "- minimumDistance: "  + intent.getStringExtra("value"));
            }

            if(intent.getAction().equals(ACTION_SET_MINIMUM_INTERVAL)) {
                // TODO
                Log.i(TAG, "- minimumInterval: "  + intent.getStringExtra("value"));
            }

            if(intent.getAction().equals(ACTION_SET_PRECISION)) {
                // TODO
                Log.i(TAG, "- precision: "  + intent.getStringExtra("value"));
            }

        }

        //We want this service to continue running until it is explicitly stopped
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "------------------------------------------ Destroyed Location update Service");
        cleanUp();
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        this.stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        cleanUp();
        if (isDebugging) {
            Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent);
    }

    /**
     * Start recording aggresively from all found providers
     */
    private void startRecording() {
        Log.i(TAG, "startRecording");

        locationManager.removeUpdates(this);

        // Turn on all providers aggressively
        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider: matchingProviders) {
            if (provider != LocationManager.PASSIVE_PROVIDER) {
                locationManager.requestLocationUpdates(provider, 0, 0, this);
            }
        }
    }

    private void cleanUp() {
        locationManager.removeUpdates(this);
        stopForeground(true);
    }

     /** Determines whether one Location reading is better than the current Location fix
      * @param location  The new Location that you want to evaluate
      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
      */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
          return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    //  ------------------  LOCATION LISTENER INTERFACE -------------------------

    public void onLocationChanged(Location location) {
        Log.d(TAG, "- onLocationChanged: " + location.getLatitude() + "," + location.getLongitude() + ", accuracy: " + location.getAccuracy() + ", speed: " + location.getSpeed());
        if(isDebugging){
            Toast.makeText(this, "acy:"+location.getAccuracy()+",v:"+location.getSpeed(), Toast.LENGTH_LONG).show();
        }

        if(isBetterLocation(location, lastLocation)){
            Log.d(TAG, "Location is better");
            lastLocation = location;
            // send it via bus to activity
            try{
                JSONObject pos = new JSONObject();
            JSONObject loc = new JSONObject();
                loc.put("latitude", location.getLatitude());
                loc.put("longitude", location.getLongitude());
                loc.put("accuracy", location.getAccuracy());
                loc.put("speed", location.getSpeed());
                loc.put("bearing", location.getBearing());
                loc.put("altitude", location.getAltitude());
                pos.put("coords", loc);
                pos.put("timestamp", new Date().getTime());

                EventBus.getDefault().post(pos);
                Log.d(TAG, "posting location to bus");


            }catch(JSONException e){
                Log.e(TAG, "could not parse location");
            }

        }else{
            Log.d(TAG, "Location is no better than current");
        }
    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        Log.d(TAG, "- onProviderDisabled: " + provider);
    }
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        Log.d(TAG, "- onProviderEnabled: " + provider);
    }
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        Log.d(TAG, "- onStatusChanged: " + provider + ", status: " + status);
    }

    // -------------------------- LOCATION LISTENER INTERFACE END -------------------------

}
