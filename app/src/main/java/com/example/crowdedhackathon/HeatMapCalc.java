package com.example.crowdedhackathon;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;
import java.util.HashSet;

public class HeatMapCalc {
    private LatLng marker_1, marker_2;
    private ArrayList<String> _URLs;

    HeatMapCalc(GoogleMap map, ArrayList<LatLng> markers, double rad, double collVal) throws Exception{
        if(markers.size() != 2 || collVal > rad || map == null)
            throw(new Exception());
        if(markers.get(0).longitude > markers.get(1).longitude){
            marker_1 = markers.get(1);
            marker_2 = markers.get(0);
        }
        else{
            marker_1 = markers.get(0);
            marker_2 = markers.get(1);
        }

        //alpha is angle between x-axis and square
        double alpha = Math.atan(calcSlope()) - Math.toRadians(45.0);

        //finds the length of the square from one corner to other
        double crossLen = findLength(marker_2, marker_1);

        //divides the length between markers to sqrt 2, because we want a square area
        double edgelen = crossLen / Math.sqrt(2.0);

        //calculates length between centers of circles
        double lenbtCircles = 2*rad - collVal;

        ArrayList<LatLng> circleLoc = new ArrayList<>();
        for(int i=0; lenbtCircles*i < crossLen + rad; ++i){ //
            double relEdgeLen = (edgelen*(crossLen - (lenbtCircles)*i)) / (crossLen);
            for(int j = 0; (lenbtCircles)*j < relEdgeLen + rad; ++j){
                //adds search circles to one side of square
                circleLoc.add(new LatLng(
                        marker_1.latitude + (lenbtCircles*j)*Math.cos(Math.toRadians(45.0) + alpha),
                        marker_1.longitude + (lenbtCircles*j)*Math.sin(Math.toRadians(45.0) + alpha)));

                //adds to other side
                circleLoc.add(new LatLng(
                        marker_1.latitude + (lenbtCircles*j)*Math.cos(Math.toRadians(135.0) + alpha),
                        marker_1.longitude + (lenbtCircles*j)*Math.sin(Math.toRadians(135.0) + alpha)));
            }
        }
    }

    public double calcSlope(){
        return ((marker_2.longitude - marker_1.longitude) / (marker_2.latitude - marker_1.latitude));
    }

    public double findLength(LatLng loc1, LatLng loc2){
        return(Math.sqrt(Math.pow(loc1.longitude - loc2.longitude, 2.0)
                +
                Math.pow(loc1.latitude - loc2.latitude, 2.0)));
    }

    //not completed
    public ArrayList<String> turnToURL(ArrayList<LatLng> locs){
        return new ArrayList<String>();
    }
}
