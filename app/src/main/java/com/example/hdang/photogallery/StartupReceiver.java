package com.example.hdang.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by hdang on 2/22/2016.
 * Receiver to listen to when System boot up
 */
public class StartupReceiver extends BroadcastReceiver {

    private  static final String TAG = "StartupReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean isON = QueryPreferences.isAlarmOn(context); // get if the poll was previously ON
        PollService.setServiceAlarm(context, isON); // start the service alarm if it was
    }
}
