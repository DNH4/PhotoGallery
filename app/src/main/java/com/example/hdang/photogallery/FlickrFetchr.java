package com.example.hdang.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hdang on 2/16/2016.
 * Class to handle all the networking
 */
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "a828b03c5cbd1b8a457dfcb45a0e565f"; // API key from dnhjunk@yahoo.ca
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key" , API_KEY)
            .appendQueryParameter("format" , "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();


    public byte[] getUrlBytes(String urlSpec) throws IOException {

        URL url = new URL(urlSpec); //create a URL obj from a string
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();//open a connection using the url

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream(); // HTTP GET

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){ // if url not respond
                throw new IOException(connection.getResponseMessage() +
                    ": with" +
                    urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024]; // get new byte array
            while((bytesRead = in.read(buffer)) > 0){ //while reading byte from inputStream and store them in byte array
                out.write(buffer, 0, bytesRead); //write byte to buffer with offset/starting position 0 and #byte to write bytesRead
            }
            out.close(); // close the writing stream
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{//same thing but return String
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENT_METHOD,null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD,query);
        return downloadGalleryItems(url);
    }

    /*public List<GalleryItem> fetchItems(){*/
    public List<GalleryItem> downloadGalleryItems(String url){
        // building https://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=xxx&format=json&nojsoncallback=1

        List<GalleryItem> items = new ArrayList<>();

        try {
/*            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")//tell Flickr to exclude the enclosing method name and parentheses
                    .appendQueryParameter("extras", "url_s") // tell Flickr to include the URL for the small version of the picture if it's available
                    .build().toString();*/
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);//parse Json using the JSONObject from JSON java API
            parseItems(items, jsonBody);
        } catch (JSONException je){
            Log.e(TAG,"Fail to parse JSON", je);
        } catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        return items;
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);

        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }

        return uriBuilder.build().toString();
    }


    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
        throws IOException, JSONException {
        //JSON format is Obj {}then array [] then value  453
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos"); //get the photos section of the JSON
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");// get the photo array inside photos

        for (int i = 0; i < photoJsonArray.length(); i++){
            JSONObject photoJsonObject  = photoJsonArray.getJSONObject(i);// for each of the array make a JSONObject

            GalleryItem item = new GalleryItem(); //make a new model for the data
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if(!photoJsonObject.has("url_s")){ //check here to ignore images that do not have an image url
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));//get owner id
            items.add(item);
        }
    }


}
