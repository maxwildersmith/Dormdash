package com.example.drago.dormdash;

import android.Manifest;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {


    public static final String BASE_URL="https://maps.googleapis.com/maps/api/directions/json?origin=", API="&key=AIzaSyDQ3MYRpEFdHYU84eRGt8g6Q4eFYmz88T4";//TODO: ADD API KEY
    public static final String DISTANCE_URL="https://maps.googleapis.com/maps/api/distancematrix/json?origins=";

    private GoogleMap mMap;
    private List<PickupRequest> quests;
    private List<Polyline> onMap;
    private RequestQueue queue;
    private Circle radiusCircle;
    private MapView mapview;
    private double range = 300;
    private LatLng[] coors = {new LatLng(34.042317, -118.255994),new LatLng(34.041906, -118.252327), new LatLng(34.041666, -118.258459)};
    private SharedPreferences preferences;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Add stuff here", Toast.LENGTH_SHORT).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        queue = Volley.newRequestQueue(this);

        onMap = new ArrayList<>();
        quests = new ArrayList<>();
        quests.add(new PickupRequest(coors[0],coors[2],queue,"Pick up a sandwich for me",2.5));
//        destinations.add(new PickupRequest(coors[1],coors[0],queue));

        pullQuests();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Dialog settingsDialog = new Dialog(this);
            LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.settings_layout, (ViewGroup)findViewById(R.id.root));
            settingsDialog.setContentView(layout);
            final TextView seekText = (TextView)layout.findViewById(R.id.seektext);
            SeekBar seek = (SeekBar)layout.findViewById(R.id.seekBar);
            seek.setProgress((int)(range*32.8084));
            seek.setMax(150000);
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    range=progress/32.8084;
                    seekText.setText("Scanning Radius (ft): "+(progress/10));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            settingsDialog.create();
            settingsDialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void pullQuests(){
        db.collection("quests")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String,Object> data = document.getData();
                        quests.add(new PickupRequest(toLatLng((GeoPoint)data.get("pickup")),toLatLng((GeoPoint)data.get("dropoff")),
                                queue,(long)data.get("delay"),(String)data.get("task"),(double)data.get("pay")));
                    }
                } else {
                    Log.w("asdf", "Error getting documents.", task.getException());
                }
                Log.d("asdf", "onComplete: "+quests.size());
            }
        });
    }

    public LatLng toLatLng(GeoPoint geo){
        return new LatLng(geo.getLatitude(),geo.getLongitude());
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.ear_deliverer){

        } else if(id == R.id.eye_user){

        } else if(id == R.id.pay){

        } else if(id == R.id.settings){

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    9);
            return;
        }
        mMap.setMyLocationEnabled(true);
//        mMap.addMarker(new MarkerOptions().position(coors[0]).title("Destination 1"));
//        mMap.addMarker(new MarkerOptions().position(coors[1]).title("Destination 2"));
//        mMap.addMarker(new MarkerOptions().position(coors[2]).title("Destination 2"));

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                for(PickupRequest r: quests)
                    if(r.equals(polyline)){
                        Toast.makeText(MainActivity.this, r.getTask(), Toast.LENGTH_SHORT).show();
                    }

            }
        });
        // Check if we were successful in obtaining the map.
        if (mMap != null) {

            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                //
                @Override
                public void onMyLocationChange(Location arg0) {


                    mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(arg0.getLatitude(), arg0.getLongitude())));
                    if (radiusCircle != null) {
                        radiusCircle.setCenter(new LatLng(arg0.getLatitude(), arg0.getLongitude()));
                        radiusCircle.setRadius(range);
//                        double rad = radiusCircle.getRadius();
//                        double lat=radiusCircle.getCenter().latitude-rad,lng=radiusCircle.getCenter().longitude-rad;
//                        mMap.setLatLngBoundsForCameraTarget(new LatLngBounds(new LatLng(lat,lng),new LatLng(lat+2*rad,lng+2*rad)));
                    }
                    else
                        radiusCircle = mMap.addCircle(new CircleOptions().center(new LatLng(arg0.getLatitude(), arg0.getLongitude())).radius(range).visible(true).strokeColor(Color.BLUE));

                    inRange(arg0.getLatitude(), arg0.getLongitude());
                }
            });

        }
    }

    public void addRequests(List<PickupRequest> inputs){
        Log.d("asdf", "addRequests: "+inputs.size());
        if(mMap!=null){
            for(Polyline line:onMap)
                inputs.remove(line);
            Log.d("asdf", "addRequests: "+inputs.size());
            for(PickupRequest r: inputs) {
                r.makePolyLine(mMap,onMap);
            }
        }
    }

    public void inRange(double lat, double lng){
        String url=DISTANCE_URL+lat+","+lng+"&destinations=";
        for(PickupRequest r: quests)
            url+=r.getLatLng()+"|";
        url.substring(0,url.length()-1);
        url+="&mode=walking&units=imperial"+API;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray values = new JSONObject(response).getJSONArray("rows").getJSONObject(0).getJSONArray("elements");
                            List<PickupRequest> inRange = new ArrayList<>();
                            for(int i=0;i<quests.size();i++)
                                if(values.getJSONObject(i).getJSONObject("distance").optInt("value",-1)<=range) {
                                    inRange.add(quests.get(i));
                                }
                            addRequests(inRange);
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
}
