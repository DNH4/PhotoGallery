package com.example.hdang.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by hdang on 2/23/2016.
 * Generic fragment that hides foreground notification
 */
// abstract so that it cannot be instantiate
public abstract class VisibleFragment extends Fragment {
    private static final String TAG = "VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION); //intent filter as in Manifest. filter for own broadcast intent
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PERM_PRIVATE, null); // register broadcast receiver for own broadcast
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }
    // this will be call when there's new result
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if receive this --> visible --> cancel notification
            Log.i(TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);// change the result code of current broadcast
        }
    };
}
