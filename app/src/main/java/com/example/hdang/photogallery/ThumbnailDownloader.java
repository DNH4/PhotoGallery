package com.example.hdang.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by hdang on 2/17/2016.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler; // Handler responsible for queueing download request and process download request msg when they are pulled off the queue
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>(); //ConcurrentHashMap is a thread-safe version of HashMap
    private Handler mResponseHandler;
    private ThumbnailDownloadListener mThumbnailDownloadListener;



    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);// will be called when an image has been fully downloaded and ready to add to UI
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {// BG thread constructor take the Handler from main thread
        super(TAG);
        mResponseHandler = responseHandler;
    }

    //initialize mRequestHandler and define what that Handler will do when downloaded messages are pulled off the queue and passed to it.
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() { //create the handler
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj; // get the obj that was sent with the message
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    public void queueThumbnail(T target, String url){ //target here is just the viewHolder where image will be pass and display
        Log.i(TAG, "Got a URL: " + url);

        if (url == null){
            mRequestMap.remove(target);// remove msg from msg queue if no url found
        } else {
            mRequestMap.put(target, url); // put url and the viewHolder into a hashmap
            //obtainMessage(what,obj)
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget(); // Build msg then send msg(the viewHolder) to it's Handler
        }
    }

    //Clear the queue when phone is rotated
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target){//helper method where downloading happens. Was passed view holder
        try{
            final String url = mRequestMap.get(target);

            if(url == null){ //2nd layer check
                return;
            }

            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() { // this is not route back to Handler but instead run directly --> in the in the associated thread (main)
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url){// double check the requestMap (because RecyclerView recycle its views so url might changed)
                        return;
                    }

                    mRequestMap.remove(target);//remove photoHolder URL mapping from request map
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,bitmap);// set bitmap
                }
            });

        }catch (IOException ioe){
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}
