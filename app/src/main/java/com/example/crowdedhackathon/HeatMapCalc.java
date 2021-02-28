package com.example.crowdedhackathon;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.PolyUtil;
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

    HeatMapCalc(GoogleMap map, ArrayList<LatLng> markers, double rad, double collVal, int mode) throws Exception {
        final int option = mode;
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
        if(crossLen < 2000) {
            ArrayList<LatLng> circleLoc = new ArrayList<>();
            for (int i = 0; lenbtCircles * i < crossLen + 2 * rad; ++i) { //
                double relEdgeLen = (edgelen * (crossLen - (lenbtCircles) * i)) / (crossLen);
                circleLoc.add(new LatLng(
                        marker_1.latitude + (lenbtCircles * i) * Math.sin(alpha) / correctLatitude,
                        marker_1.longitude + (lenbtCircles * i) * Math.cos(alpha) / correctLongitude));
                for (int j = 0; (lenbtCircles) * j < relEdgeLen + 2 * rad; ++j) {
                    //adds search circles to one side of square
                    circleLoc.add(new LatLng(
                            marker_1.latitude + (lenbtCircles * i) * Math.sin(-Math.toRadians(45.0) + alpha) / correctLatitude,
                            marker_1.longitude + (lenbtCircles * j) * Math.cos(-Math.toRadians(45.0) + alpha) / correctLongitude));

                    //adds to other side
                    circleLoc.add(new LatLng(
                            marker_1.latitude + (lenbtCircles * i) * Math.sin(Math.toRadians(45.0) + alpha) / correctLatitude,
                            marker_1.longitude + (lenbtCircles * j) * Math.cos(Math.toRadians(45.0) + alpha) / correctLongitude));

                }
            }
            double dist = (1.0 / Math.cos(Math.toRadians(22.5))) * (crossLen / 2.0);
            final LatLng checkRight = new LatLng((marker_1.latitude + dist * Math.sin(alpha - Math.toRadians(22.5)) / correctLatitude),
                    marker_1.longitude + dist * Math.cos(alpha - Math.toRadians(22.5)) / correctLongitude);
            final LatLng checkLeft = new LatLng((marker_1.latitude + dist * Math.sin(alpha + Math.toRadians(22.5)) / correctLatitude),
                    marker_1.longitude + dist * Math.cos(alpha + Math.toRadians(22.5)) / correctLongitude);
            final LatLng checkMid = new LatLng(marker_1.latitude + (crossLen / 2.0) * Math.sin(alpha) / correctLatitude,
                    marker_1.longitude + (crossLen / 2.0) * Math.cos(alpha / correctLongitude));

            final LatLng circleRad = new LatLng((crossLen / 6.0) / correctLatitude, (crossLen / 6.0) / correctLongitude);

            ArrayList<String> URLs = turnToURL(circleLoc);

            ScrapJSON background = new ScrapJSON() {
                @Override
                public void onResponseReceived(Object result) {
                    if (result instanceof ArrayList) {
                        ArrayList<WeightedLatLng> points = (ArrayList<WeightedLatLng>) result;
                        HeatmapTileProvider provide = new HeatmapTileProvider.Builder().weightedData(points).build();
                        TileOverlay overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provide));
                        ScrapRoadJSON road_background = new ScrapRoadJSON() {
                            @Override
                            public void onResponseReceived(Object result) {
                                if (result != null && (result instanceof ArrayList)) {
                                    ArrayList<LatLng> roadList = (ArrayList<LatLng>) result;
                                    ArrayList<Double> roadPopul = new ArrayList<>();
                                    for (int i = 0; i < roadList.size(); ++i) {
                                        double popul = 0.0;
                                        for (int j = 0; j < points.size(); ++j) {
                                            if (points.get(j).getPoint().x > roadList.get(i).latitude - circleRad.latitude
                                                    && points.get(j).getPoint().x < roadList.get(i).latitude + circleRad.latitude
                                                    && points.get(j).getPoint().y > roadList.get(i).longitude - circleRad.longitude
                                                    && points.get(j).getPoint().y < roadList.get(i).longitude + circleRad.longitude) {
                                                popul += points.get(j).getIntensity();
                                            }
                                        }
                                        roadPopul.add(popul);
                                    }
                                    int ind = -1;
                                    if (option == 0) {
                                        double max = -1.0;
                                        for (int i = 0; i < roadPopul.size(); ++i) {
                                            if (roadPopul.get(i) > max) {
                                                max = roadPopul.get(i);
                                                ind = i;
                                            }
                                        }
                                    } else if (option == 1) {
                                        double min = roadPopul.get(0);
                                        ind = 0;
                                        for (int i = 0; i < roadPopul.size(); ++i) {
                                            if (roadPopul.get(i) < min) {
                                                min = roadPopul.get(i);
                                                ind = i;
                                            }
                                        }
                                    }
                                    ScrapDirectionJSON direction = new ScrapDirectionJSON() {
                                        @Override
                                        public void onResponseReceived(Object result) {
                                            map.addPolyline(new PolylineOptions().addAll(((ArrayList<LatLng>) result)).visible(true).color(Color.BLACK));
                                        }
                                    };
                                    direction.execute(turnToDirectionURL(marker_1, marker_2, roadList.get(ind)));
                                }
                            }
                        };
                        ArrayList<LatLng> checkPoints = new ArrayList<>();
                        checkPoints.add(checkLeft);
                        checkPoints.add(checkRight);
                        checkPoints.add(checkMid);
                        road_background.execute(turnToRoadURL(checkPoints));
                    }
                }
            };
            background.execute(URLs);
        }
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

    public static String turnToRoadURL(ArrayList<LatLng> locs){
        StringBuilder url = new StringBuilder("https://roads.googleapis.com/v1/nearestRoads?points=");
        for(int i = 0; i < locs.size(); ++i) {
            if(i != 0)
                url.append("|");
            url.append(locs.get(i).latitude).append(",").append(locs.get(i).longitude);
        }
        url.append("&key=").append(App.getContext().getResources().getString(R.string.google_maps_key));
        return url.toString();
    }

    public static String turnToDirectionURL(LatLng orig, LatLng dest, LatLng wpoint){

        StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        url.append("origin=").append(orig.latitude).append(",").append(orig.longitude).append("&");
        url.append("destination=").append(dest.latitude).append(",").append(orig.longitude).append("&");
        url.append("waypoints=").append(wpoint.latitude).append("%2C").append(wpoint.longitude).append("&");
        url.append("mode=").append("walking").append("&");
        url.append("key=").append(App.getContext().getResources().getString(R.string.google_maps_key));

        return url.toString();
    }

    public double calcCorrectLongitude(double lat){
        return 111320*Math.cos(Math.toRadians(lat));
    }
}