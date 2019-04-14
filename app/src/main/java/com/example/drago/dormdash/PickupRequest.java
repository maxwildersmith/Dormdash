package com.example.drago.dormdash;

import android.graphics.Color;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PickupRequest {
    private LatLng pickup, dropoff;
    private long sendTime;
    private int id=0;
    private LatLng[] polyLine;
    private double pay;
    private String task;

    public PickupRequest(LatLng pickup, LatLng dropoff, RequestQueue queue, String task, double pay){
        this(pickup,dropoff,queue,Calendar.getInstance().getTimeInMillis(),task, pay);
    }

    private String getID(){
        return sendTime+task;
    }

    public PickupRequest(LatLng pickup, LatLng dropoff, RequestQueue queue, long requestTime, String task, double pay){
        this.task=task;
        this.pay=pay;
        this.sendTime=requestTime;
        sendTime = Calendar.getInstance().getTimeInMillis();
        this.pickup=pickup;
        this.dropoff=dropoff;
        String url = MainActivity.BASE_URL+pickup.latitude+","+pickup.longitude+"&destination="+dropoff.latitude+","+dropoff.longitude+"&mode=walking"+MainActivity.API;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response).getJSONArray("routes").getJSONObject(0);
                            setPolyLine(json.getJSONObject("overview_polyline").getString("points"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("asdf", "onErrorResponse: mlem mlem mlem mlem mlem mlem mlem");
            }
        });

        queue.add(stringRequest);
    }


    public long getSendTime(){
        return sendTime;
    }

    public String getLatLng(){
        return pickup.latitude+","+pickup.longitude;
    }

    public long getDelay(){
        return Calendar.getInstance().getTimeInMillis()-sendTime;
    }

    public LatLng[] getPolyLine(){
        return polyLine;
    }

    public void makePolyLine(GoogleMap map, List<Polyline> onMap, boolean mission){

        long delaymin = TimeUnit.MILLISECONDS.toMinutes(Calendar.getInstance().getTimeInMillis()-sendTime);
        onMap.add(0,map.addPolyline(new PolylineOptions().add(getPolyLine()).clickable(true).color(mission?Color.YELLOW:delaymin <=1? Color.CYAN:delaymin<=5?Color.MAGENTA:Color.RED).endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.ic_money),10)).startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.ic_mail),10))));
//        onMap.get(0).setStartCap(new Cap);

    }

    public double getPay(){
        return pay;
    }

    public String getTask(){
        return task;
    }


    public boolean equals(PickupRequest obj) {
        return obj.getLatLng().equals(getLatLng());
    }

    public boolean equals(Polyline obj){
        return Arrays.equals(obj.getPoints().toArray(new LatLng[obj.getPoints().size()]),polyLine);
    }

    public void setPolyLine(String encoded){
        List<LatLng> list = PolyUtil.decode(encoded);
        polyLine=list.toArray(new LatLng[list.size()]);
    }
}
