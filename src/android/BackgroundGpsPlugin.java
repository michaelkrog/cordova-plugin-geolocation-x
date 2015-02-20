package com.tenforwardconsulting.cordova.bgloc;

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
    
    private Intent updateServiceIntent;

    private Boolean isEnabled = false;
    
    private String isDebugging = "false";

    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";

    private String stopOnTerminate = "false";
    

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
        updateServiceIntent = new Intent(activity, LocationUpdateService.class);
                
        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;

            updateServiceIntent.putExtra("isDebugging", isDebugging);
            updateServiceIntent.putExtra("notificationTitle", notificationTitle);
            updateServiceIntent.putExtra("notificationText", notificationText);            

            activity.startService(updateServiceIntent);
            isEnabled = true;

        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
            
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {
                // Params.
                //    0       1       2           3               4                5               6            7           8                9               10              11
                //[params, headers, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy, debug, notificationTitle, notificationText, activityType, stopOnTerminate]
                
                this.isDebugging = data.getString(7);
                this.notificationTitle = data.getString(8);
                this.notificationText = data.getString(9);
                this.stopOnTerminate = data.getString(11);                           

                this.callback = callbackContext;

                Log.i(TAG, "- stopOnTerminate: "     + stopOnTerminate);
                Log.i(TAG, "- isDebugging: "    + isDebugging);        
                Log.i(TAG, "- notificationTitle: "  + notificationTitle);
                Log.i(TAG, "- notificationText: "   + notificationText);     

            } catch (JSONException e) {
                callbackContext.error("authToken/url required as parameters: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
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
