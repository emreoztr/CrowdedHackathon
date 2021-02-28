package com.example.crowdedhackathon;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;
import java.util.HashSet;

public class HeatMapCalc{
    private LatLng marker_1, marker_2;
    private ArrayList<String> _URLs;
    private double _rad;
    private double correctLongitude;
    private final double correctLatitude = 110574;

    HeatMapCalc(GoogleMap map, ArrayList<LatLng> markers, double rad, double collVal) throws Exception {
        if (markers.size() != 2 || collVal > rad || map == null)
            throw (new Exception());
        if (markers.get(0).latitude > markers.get(1).latitude) {
            marker_1 = markers.get(1);
            marker_2 = markers.get(0);
        } else {
            marker_1 = markers.get(0);
            marker_2 = markers.get(1);
        }
        correctLongitude = calcCorrectLongitude(marker_1.latitude);
        _rad = rad;
        //alpha is angle between x-axis and square
        double alpha = Math.atan(calcSlope());
        if(alpha < 0)
            alpha = Math.toRadians(180) + alpha;

        //finds the length of the square from one corner to other
        double crossLen = findLength(marker_2, marker_1);

        //divides the length between markers to sqrt 2, because we want a square area
        double edgelen = crossLen / Math.sqrt(2.0);

        //calculates length between centers of circles
        double lenbtCircles = 2 * rad - collVal;

        ArrayList<LatLng> circleLoc = new ArrayList<>();
        for (int i = 0; lenbtCircles * i < crossLen + 2*rad; ++i) { //
            double relEdgeLen = (edgelen * (crossLen - (lenbtCircles) * i)) / (crossLen);
            for (int j = 0; (lenbtCircles) * j < relEdgeLen + 2*rad; ++j) {
                //adds search circles to one side of square
                circleLoc.add(new LatLng(
                        marker_1.latitude + (lenbtCircles * i) * Math.sin(-Math.toRadians(45.0) + alpha)/correctLatitude,
                        marker_1.longitude + (lenbtCircles * j) * Math.cos(-Math.toRadians(45.0) + alpha)/correctLongitude));

                //adds to other side
                circleLoc.add(new LatLng(
                        marker_1.latitude + (lenbtCircles * i) * Math.sin(Math.toRadians(45.0) + alpha)/correctLatitude,
                        marker_1.longitude + (lenbtCircles * j) * Math.cos(Math.toRadians(45.0) + alpha)/correctLongitude));

            }
        }

        ArrayList<String> URLs = turnToURL(circleLoc);

        ScrapJSON background = new ScrapJSON() {
            @Override
            public void onResponseReceived(Object result) {
                if(result instanceof HashSet){
                    HeatmapTileProvider provide = new HeatmapTileProvider.Builder().weightedData((HashSet)result).build();
                    TileOverlay overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provide));
                }
            }
        };
        background.execute(URLs);
    }

    public double calcSlope() {
        return (((marker_2.latitude - marker_1.latitude)*correctLatitude) / ((marker_2.longitude - marker_1.longitude)*correctLongitude));
    }

    public double findLength(LatLng loc1, LatLng loc2) {
        return (Math.sqrt(Math.pow((loc1.longitude - loc2.longitude)*correctLongitude, 2.0)
                +
                Math.pow((loc1.latitude - loc2.latitude)*correctLatitude, 2.0)));
    }

    public ArrayList<String> turnToURL(ArrayList<LatLng> locs) {
        ArrayList<String> URLs = new ArrayList<>();

        for (int i = 0; i < locs.size(); ++i) {
            StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
            url.append("location=").append(locs.get(i).latitude).append(",").append(locs.get(i).longitude).append("&");
            url.append("radius=").append((int)_rad).append("&");
            url.append("opennow&").append("key=").append(App.getContext().getResources().getString(R.string.google_maps_key));
            URLs.add(url.toString());
        }

        return URLs;
    }

    public double calcCorrectLongitude(double lat){
        return 111320*Math.cos(Math.toRadians(lat));
    }
}