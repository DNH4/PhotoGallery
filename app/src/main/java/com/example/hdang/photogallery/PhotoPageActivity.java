package com.example.hdang.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by DNH on 2/24/2016.
 */
public class PhotoPageActivity extends SingleFragmentActivity {
    //encapsulate work instead of return to activity and put extra on intent. No reason other activity need to know the what this activity expected as extra on its intent
    public static Intent newIntent(Context context, Uri photoPageUri){
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());//get the intent that start this activity and its data (from newIntent())
    }
}
