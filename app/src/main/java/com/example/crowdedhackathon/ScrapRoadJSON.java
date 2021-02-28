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

public abstract class ScrapRoadJSON extends AsyncTask<String, Void, ArrayList<LatLng>> implements ResponseInterface {
    public abstract void onResponseReceived(Object result);
    protected ArrayList<LatLng> doInBackground(String... urls) {
        try {
            ArrayList<LatLng> returnVal = new ArrayList<>();
            JSONObject obj = URLtoString(urls[0]);
            Log.d("urlroad", urls[0]);
            JSONArray roads = obj.getJSONArray("snappedPoints");
            for(int i = 0; i < roads.length(); ++i){
                JSONObject road = roads.getJSONObject(i);
                JSONObject loc = road.getJSONObject("location");
                returnVal.add(new LatLng(loc.getDouble("latitude"), loc.getDouble("longitude")));
            }
            return returnVal;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<LatLng> locs){
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

    public double calcStandardDev(double aver, ArrayList<Integer> elements){
        double returnVal = 0.0;
        for(int i = 0; i < elements.size(); ++i)
            returnVal += Math.pow(elements.get(i) - aver, 2.0);

        returnVal /= elements.size();
        returnVal = Math.sqrt(returnVal);

        return returnVal;
    }
}
