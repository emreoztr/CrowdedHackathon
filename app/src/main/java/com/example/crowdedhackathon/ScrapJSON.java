package com.example.crowdedhackathon;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class ScrapJSON extends AsyncTask<ArrayList<String>, Void, HashSet<WeightedLatLng>> implements ResponseInterface {
    public abstract void onResponseReceived(Object result);
    protected HashSet<WeightedLatLng> doInBackground(ArrayList<String>... urls) {

        try {
            HashSet<WeightedLatLng> posList = new HashSet<WeightedLatLng>();

            for(int j=0; j < urls[0].size(); ++j) {

                JSONObject jsonObj = URLtoString(urls[0].get(j));
                //gets results array from json of google places api
                if(!jsonObj.getString("status").equals("INVALID_REQUEST")) {


                    JSONArray jsonArr = jsonObj.getJSONArray("results");

                    for (int i = 1; i < jsonArr.length(); ++i) {
                        JSONObject place = jsonArr.getJSONObject(i);
                        JSONObject geo = place.getJSONObject("geometry");
                        JSONObject pos = geo.getJSONObject("location");
                        posList.add(new WeightedLatLng(new LatLng(pos.getDouble("lat"), pos.getDouble("lng")), 3.0));

                    }
                }
            }
            return posList;
        }catch (IOException | JSONException er){
            //pop-up
        }
        return null;
    }

    @Override
    protected void onPostExecute(HashSet<WeightedLatLng> locs){
        onResponseReceived(locs);
    }


    private JSONObject URLtoString(String url) throws IOException, JSONException {
        //building connection with url
        URL placesURL = new URL(url);
        StringBuilder sBuilder = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) placesURL.openConnection();


        BufferedInputStream input = new BufferedInputStream(con.getInputStream());
        BufferedReader buff = new BufferedReader(new InputStreamReader(input));

        //reads JSON line by line
        String line;
        while((line = buff.readLine())!=null){
            sBuilder.append(line);
        }

        con.disconnect();
        JSONObject obj = new JSONObject(sBuilder.toString());

        return obj;
    }
}
