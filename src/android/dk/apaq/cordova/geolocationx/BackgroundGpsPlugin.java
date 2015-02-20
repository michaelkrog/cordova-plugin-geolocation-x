package dk.apaq.cordova.geolocationx;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import de.greenrobot.event.EventBus;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class BackgroundGpsPlugin extends CordovaPlugin {
    private static final String TAG = "BackgroundGpsPlugin";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_MIN_DISTANCE = "setMinimumDistance";
    public static final String ACTION_SET_MIN_INTERVAL = "setMinimumInterval";
    public static final String ACTION_SET_PRECISION = "setPrecision";

    private Boolean isEnabled = false;

    private String isDebugging;
    private String locationTimeout;
    private String activityType;
    private String notificationTitle;
    private String notificationText;
    private String stopOnTerminate;

    private CallbackContext callback;

    @Override
    protected void pluginInitialize() {
        Log.d("BUS","registering");
        EventBus.getDefault().register(this);
    }

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Log.d(TAG, "execute / action : " + action);
        if(data !=null){
            Log.d(TAG, "execute / data : " + data.toString());
        }

        Activity activity = this.cordova.getActivity();
        Boolean result = false;


        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;

            activity.startService(new Intent(LocationUpdateService.ACTION_START, null, activity, LocationUpdateService.class));

            isEnabled = true;

        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;

            activity.stopService(new Intent(activity, LocationUpdateService.class));

            callbackContext.success();

        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {
                // Params.
                //[locationTimeout, debug, notificationTitle, notificationText, activityType, stopOnTerminate]
                Intent intent = new Intent(LocationUpdateService.ACTION_CONFIGURE, null, activity, LocationUpdateService.class);
                intent.putExtra("locationTimeout", data.getString(0));
                intent.putExtra("isDebugging", data.getString(1));
                intent.putExtra("notificationTitle", data.getString(2));
                intent.putExtra("notificationText", data.getString(3));
                intent.putExtra("activityType", data.getString(4));

                activity.startService(intent);

                this.callback = callbackContext;
                Log.i(TAG, "- configured: "     + data.toString());
            } catch (JSONException e) {
                callbackContext.error("Invalid parameters: " + e.getMessage());
            }
        } else if (ACTION_SET_MIN_DISTANCE.equalsIgnoreCase(action)) {
            callSetter(activity, LocationUpdateService.ACTION_SET_MINIMUM_DISTANCE, data, callbackContext);
        } else if (ACTION_SET_MIN_INTERVAL.equalsIgnoreCase(action)) {
            callSetter(activity, LocationUpdateService.ACTION_SET_MINIMUM_INTERVAL, data, callbackContext);
        } else if (ACTION_SET_PRECISION.equalsIgnoreCase(action)) {
            callSetter(activity, LocationUpdateService.ACTION_SET_PRECISION, data, callbackContext);
        }

        return result;
    }

    private void callSetter(Activity activity, String action, JSONArray data, CallbackContext callbackContext) {
        try {
            String value = data.getString(0);
            Intent setterIntent = new Intent(action, null, activity, LocationUpdateService.class);
            setterIntent.putExtra("value", value);

            activity.startService(setterIntent);
        } catch (JSONException e) {
            callbackContext.error("Unable to parse value: " + e.getMessage());
        }
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(new Intent(this.cordova.getActivity(), LocationUpdateService.class));
        }
    }

    public void onEventMainThread(JSONObject loc){
        Log.d("BUS received : ",loc.toString());
        PluginResult result = new PluginResult(PluginResult.Status.OK, loc);
        result.setKeepCallback(true);
        if(callback != null){
            callback.sendPluginResult(result);
        }
    }
}
