package com.example.hdang.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hdang on 2/16/2016.
 */
//public class PhotoGalleryFragment extends Fragment {
public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);//retain fragment after destroy (e.g. rotating)
        setHasOptionsMenu(true);//register the fragment to receive menu callbacks
        updateItems(); // run the background thread

/*        Intent i = PollService.newIntent(getActivity());//getting PollService intent
        getActivity().startService(i);//start new services*/
        //Test code
        //PollService.setServiceAlarm(getActivity(),true);

        Handler responseHandler = new Handler();// a handler in the main thread
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);//send handler to BG thread
        //update UI once the image is downloaded
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                        target.bindDrawable(drawable);
                }
        });
        mThumbnailDownloader.start(); // start BG thread -- Make sure that onLooperPrepared has been called
        mThumbnailDownloader.getLooper(); // get the looper of the BG thread
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();//remove all background msg because view is rotated and ViewHolder are reseted
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery_fragment, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                searchView.onActionViewCollapsed();//collapse search view
                searchView.clearFocus();//hide keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);//set query text but don't submit


            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){ //if service is on then show text as stop polling
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();//call to ensures that images displayed in RecyclerView reflect the most recent search query
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());//check if alarm already running
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);// if alarm is running then turn service off else turn on
                getActivity().invalidateOptionsMenu();//invalidate to call onCreateOptionsMenu again to update the textview
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();//fetch new data using Asynctask (theses will be used to update view from updating adapter)
    }

    private void setupAdapter(){
        if(isAdded()){ //Return true if the fragment is currently added to its activity.
            mPhotoRecyclerView.setAdapter(new photoAdapter(mItems));
        }
    }

    public void closeSoftKeyBoard(View view){

    }

    //add view holder
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        //private TextView mTitleTextView;
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            //mTitleTextView = (TextView) itemView;
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view); // get the image view item from layout
            mItemImageView.setOnClickListener(this);
        }

/*        public void bindGalleryItem(GalleryItem item){
            mTitleTextView.setText(item.toString());
        }*/
        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);//set image to whatever was put in from adapter
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {
            //Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }

    //add adapter
    private class photoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public photoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
/*            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);*/
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item,parent,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position); // get position of the item
            holder.bindGalleryItem(galleryItem); //bind to view holder

            Drawable placeholder = getResources().getDrawable(R.drawable.image_holder); //tempo picture for image holder
            holder.bindDrawable(placeholder);
            // when bind viewHolder image then download image
            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());//sending queue msg to looper main thread - using a dedicated BG thread
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }


    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{ //3rd parameter give the result
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
 /*           try{
                String result = new FlickrFetchr().getUrlString("https://bignerdranch.com");
                Log.i(TAG, "Fetched data from contents of URL: " + result);
            }catch (IOException ioe){
                Log.e(TAG, "Fail to fetch URL: ", ioe);
            }*/
            //return new FlickrFetchr().fetchItems();

            //String query = "robot"; //just for testing

            if(mQuery == null){
                return new FlickrFetchr().fetchRecentPhotos();// make a new BG threads to get photos url
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);// make a new BG thread to get searched photos url
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) { //this is run on the main thread
            mItems = galleryItems;
            setupAdapter();//update the view after finish downloading new stuff
        }
    }

}
