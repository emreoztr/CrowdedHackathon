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

public abstract class ScrapJSON extends AsyncTask<ArrayList<String>, Void, ArrayList<WeightedLatLng>> implements ResponseInterface {
    public abstract void onResponseReceived(Object result);
    protected ArrayList<WeightedLatLng> doInBackground(ArrayList<String>... urls) {

        try {
            ArrayList<Integer> ratingCount = new ArrayList<>(60);
            double ratingAver = 0;
            ArrayList<LatLng> posList = new ArrayList<LatLng>();

            for(int j=0; j < urls[0].size(); ++j) {

                JSONObject jsonObj = URLtoString(urls[0].get(j));
                //gets results array from json of google places api

                if(!jsonObj.getString("status").equals("INVALID_REQUEST")) {
                    int page_num = 0;

                    JSONArray jsonArr = jsonObj.getJSONArray("results");
                    do{
                        if(page_num != 0){
                            StringBuilder nextUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                            nextUrl.append("pagetoken=").append(jsonObj.getString("next_page_token")).append("&");
                            nextUrl.append("key=").append(App.getContext().getResources().getString(R.string.google_maps_key));
                            jsonObj = URLtoString(nextUrl.toString());
                            jsonArr = jsonObj.getJSONArray("results");
                        }
                        for (int i = 0; i < jsonArr.length(); ++i) {
                            JSONObject place = jsonArr.getJSONObject(i);
                            JSONObject geo = place.getJSONObject("geometry");
                            JSONObject pos = geo.getJSONObject("location");
                            LatLng loc = new LatLng(pos.getDouble("lat"), pos.getDouble("lng"));
                            if(!posList.contains(loc)){
                                if(place.has("user_ratings_total")){
                                    posList.add(loc);
                                    int placeRateCount = place.getInt("user_ratings_total");
                                    ratingCount.add(placeRateCount);
                                    ratingAver += placeRateCount;
                                }
                            }
                        }
                        page_num++;
                    }while(jsonObj.has("next_page_token"));
                }

            }

            ratingAver /= ratingCount.size();
            double sd = calcStandardDev(ratingAver, ratingCount);
            double highTHold = ratingAver + sd;
            double midTHold = ratingAver - sd;
            double lowTHold = 15.0;
            ArrayList<WeightedLatLng> weightedPosList = new ArrayList<>(60);
            for(int i = 0; i < posList.size() ; ++i){
                int rate = ratingCount.get(i);
                if(rate > lowTHold){
                    if(rate >= highTHold)
                        weightedPosList.add(new WeightedLatLng(posList.get(i), 6.0));
                    else if(rate >= midTHold)
                        weightedPosList.add(new WeightedLatLng(posList.get(i), 4.0));
                    else
                        weightedPosList.add(new WeightedLatLng(posList.get(i), 2.0));
                }
                else
                    weightedPosList.add(new WeightedLatLng(posList.get(i), 1.0));
            }
            return weightedPosList;
        }catch (IOException | JSONException er){
            //pop-up
        }
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<WeightedLatLng> locs){
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
