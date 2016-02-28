package com.example.hdang.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by hdang on 2/19/2016.
 */
public class PollService extends IntentService {
    private static final String TAG = "PollService";
    public static final String ACTION_SHOW_NOTIFICATION = "com.example.hdang.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.example.hdang.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICAITON";


    private static final int POLL_INTERVAL = 1000 * 60; //60s (min) poll interval for testing purpose
    //private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class); // any component that wants to start this service should use this method
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0); //flag 0 ?

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),POLL_INTERVAL,pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context,isOn); // save it to pref. for broadcast intent

    }

    public static boolean isServiceAlarmOn (Context context){//cannot use for broadcast because of short lifespan
        Intent i  = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);// PendingIntent if not exist then return null
        return pi !=null; //return false if PendingIntent not exist
    }

    public PollService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(!isNetworkAvailableAndConnected()){
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);// getting the stored query
        String lastResultId = QueryPreferences.getLastResultId(this); // getting the last result id
        List<GalleryItem> items;

        if(query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();// get the last photo using AsyncTask
        } else {
            items = new FlickrFetchr().searchPhotos(query);// get the queried photos using AsyncTask
        }

        if (items.size() == 0){
            return;//if no result
        }

        String resultId = items.get(0).getId(); // get the last photo Id
        if(resultId.equals(lastResultId)){
            Log.i(TAG, "Got an old result: " + resultId);
        } else { // if got new  result
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = getResources(); //getting ref for getting app resources
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0); //pending intent to get activity to pass to when user click notification

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_picture_title)) //text that is displayed in the status bar when the notification first arrives
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_picture_title))
                    .setContentText(resources.getString(R.string.new_picture_text))
                    .setContentIntent(pi) // PendingIntent to send when the notification is clicked
                    .setAutoCancel(true) //auto cancel when use click notification
                    .build();

            /*NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this); // get an instance of NotificationManagerCompat from the current context
            notificationManager.notify(0, notification); // identifier 0, should be unique

            // Send own broadcast intent in order to turn off/on notification
            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);*/
            showBackgroundNotification(0,notification);
        }

        QueryPreferences.setLastResultId(this, resultId);

    }

    private void showBackgroundNotification(int requestCode, Notification notification){
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode); // send with intent
        i.putExtra(NOTIFICATION, notification); // send with intent
        sendOrderedBroadcast(i,PERM_PRIVATE,null,null, Activity.RESULT_OK,null,null);
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() !=null;// return null if background network is disabled
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected(); // check if current network is fullly connected

        return isNetworkConnected;//return true if network is available and connected
    }
}
