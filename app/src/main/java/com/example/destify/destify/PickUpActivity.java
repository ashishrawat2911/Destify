package com.example.destify.destify;
//F19470

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.destify.destify.models.PlaceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PickUpActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "PickUpActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final float DEFAULT_ZOOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private static final PatternItem DOT = new Dot();
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);
    private boolean mLocationPermissionGranted = false;
    private boolean gps_enabled = false;
    private boolean network_enabled = false;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private GoogleMap mMap;
    LocationManager lm;
    private AutoCompleteTextView mSearchEditText;
    ImageView myLocationPointer;
    PlaceInfo mPlace;
    RelativeLayout modeLayout, enableLLayout, enableNlayout;
    PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private Marker mMarker;
    Location myLocation;
    LatLng mDestination;
    Toolbar toolbar;
    String mode = "mode=driving";
    ImageView walkingModeImage, drivingModeImage;
    Snackbar bar;
    NavigationView navigationView;
    LocationRequest mLocationRequest;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private BroadcastReceiver broadcastReceiver;
    boolean move = false;
    MyBroadcastReceiver myBroadcastReceiver;
    IntentFilter intentFilter;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage() + connectionResult.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_up);
        move = true;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        walkingModeImage = findViewById(R.id.walking_mode);
        drivingModeImage = findViewById(R.id.driving_mode);
        bar = Snackbar.make(findViewById(R.id.constlayoyut), "Loading", 1000);
        modeLayout = findViewById(R.id.modelayout);
        displayLocationSettingsRequest(PickUpActivity.this);
        //     mLocationRequest = new LocationRequest();
        getLocationPermission();
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mSearchEditText = findViewById(R.id.search);
        myLocationPointer = findViewById(R.id.my_location_pointer);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        init();
        enableLLayout = findViewById(R.id.enable_location);
        enableNlayout = findViewById(R.id.enable_network);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        intentFilter = new IntentFilter();
        myBroadcastReceiver = new MyBroadcastReceiver();
        intentFilter.addAction(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);

    }


    private void updatePrograssBar() {
    }


    public void notificationBuilder(View view) {
        Intent intent = new Intent(this, PickUpActivity.class);
        intent.putExtra("not1", "");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1")
                .setSmallIcon(R.drawable.ic_track_location)
                .setContentTitle("Tracking Location")
                .setContentText("Here you go")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(pendingIntent);


        // notificationId is a unique int for each notification that you must define
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.notify(0, mBuilder.build());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mDestination);
            super.onSaveInstanceState(outState);
        }
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onDrawerClicked(View view) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
    }

    private void init() {


        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);
        mSearchEditText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchEditText.setOnItemClickListener((mAutoItemClickListener));
        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    geoLocate();
                    hideKeyBoard();
                    return true;
                }
                return false;
            }
        });
        myLocationPointer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocationSettingsRequest(PickUpActivity.this);
                getDeviceAccessLocation();
                hideKeyBoard();
                modeLayout.setVisibility(View.INVISIBLE);
                mSearchEditText.setText("");
            }
        });

    }


    private void geoLocate() {
        String searchString = mSearchEditText.getText().toString();
        Geocoder geocoder = new Geocoder(PickUpActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);

        } catch (Exception e) {
            Log.d(TAG, "geoLocate: ioexception");
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: " + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOOM, address.getAddressLine(0));
        }
    }


    private void hideKeyBoard() {
        LinearLayout mainLayout;
        // Get your layout set up, this is just an example
        mainLayout = (LinearLayout) findViewById(R.id.constlayoyut);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(mainLayout.getWindowToken(), 0);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                if (mLocationPermissionGranted) {
                    displayLocationSettingsRequest(PickUpActivity.this);
                    getDeviceAccessLocation();
                    if (ActivityCompat.checkSelfPermission(PickUpActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(PickUpActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    myLocation = mMap.getMyLocation();

                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    init();
                    //  mMap.setTrafficEnabled(true);
                    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            mMap.clear();
                            MarkerOptions options = new MarkerOptions()
                                    .position(latLng)
                                    .title("Selected Location");
                            mMap.addMarker(options);
                            mDestination = latLng;
                            showLine(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), mDestination, mode);
                        }
                    });
                }
            }
        });
    }

    private void getDeviceAccessLocation() {
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location");
                            Location currentlocation = (Location) task.getResult();
                            myLocation = currentlocation;
                            if (move) {
                                moveCamera(new LatLng(currentlocation.getLatitude(), currentlocation.getLongitude()), DEFAULT_ZOOOM);
                                move = false;
                            } else
                                moveCamera(new LatLng(currentlocation.getLatitude(), currentlocation.getLongitude()), DEFAULT_ZOOOM, "My Location");
                        } else
                            Log.d(TAG, "onComplete: didnt find out location");
                    }
                });
            } else
                displayLocationSettingsRequest(PickUpActivity.this);
        } catch (Exception e) {
            Log.e(TAG, "Security exception");
        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

    }

    ;

    private void moveCamera(LatLng latLng, float zoom, String title) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.clear();
        if (!title.equals("My Location")) {

            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
            mDestination = latLng;
        }


    }

    protected void createLocationRequest() {


        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void createLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
            }
        });
    }

    private void displayLocationSettingsRequest(Context context) {

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        if (!gps_enabled) {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(10000 / 2);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);
        }
    }

    private void getLocationPermission() {
        String[] permissions = {FINE_LOCATION, COURSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }

        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            return;
                        }

                    }
                    mLocationPermissionGranted = true;
                    initMap();
                }
        }

    }


    //  Auto compl google Api auto complete sugestions
    private AdapterView.OnItemClickListener mAutoItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideKeyBoard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(position);
            final String placeId;

            assert item != null;
            placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);

            placeResult.setResultCallback(mUpdatePlaceDetailCallBack);
        }
    };
    ResultCallback<PlaceBuffer> mUpdatePlaceDetailCallBack = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
            }
            final Place place = places.get(0);
            try {
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());
                mPlace.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: address: " + place.getAddress());
//                mPlace.setAttributions(place.getAttributions().toString());
//                Log.d(TAG, "onResult: attributions: " + place.getAttributions());
                mPlace.setId(place.getId());
                Log.d(TAG, "onResult: id:" + place.getId());
                mPlace.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                mPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: rating: " + place.getRating());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());

                Log.d(TAG, "onResult: place: " + mPlace.toString());
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }
            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOOM, mPlace.getName());

            places.release();
        }
    };


    public void OnClick_Speech_Detector(View v) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Up");
        startActivityForResult(i, 336);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 336 && resultCode == RESULT_OK) {
            //if user says the phrase "poptarts"
            //call function poptart

            //if user says the phrase "banana"
            //call function banana
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            mSearchEditText.setText(result.get(0));
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void showLine(LatLng start, LatLng end, String mode) {

        // Assign your origin and destination
        // These points are your markers coordinates
        modeLayout.setVisibility(View.VISIBLE);
        mMap.clear();
        MarkerOptions options = new MarkerOptions()
                .position(mDestination)
                .title("Selected Location");
        mMap.addMarker(options);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(start, end, mode);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }


    private String getDirectionsUrl(LatLng origin, LatLng dest, String mode) {


        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        String units = "units=metric";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + units + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service

        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }


    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Pick up activity", "Exception while downloading url" + e.toString());
        } finally {
            assert iStream != null;
            iStream.close();
            //  mGoogleApiClient.disconnect();
        }
        return data;
    }

    public void clearMap(View view) {
        mMap.clear();
        modeLayout.setVisibility(View.INVISIBLE);


    }

    public void modeSet(View view) {
        int id = view.getId();
        try {
            if (id == R.id.driving_mode) {
                mode = "mode=driving";
                drivingModeImage.setVisibility(View.GONE);
                walkingModeImage.setVisibility(View.VISIBLE);
                showLine(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), mDestination, mode);
                Log.i(TAG, "modeSet: driving_mode");
            } else if (id == R.id.walking_mode) {
                mode = "mode=walking";
                walkingModeImage.setVisibility(View.GONE);
                drivingModeImage.setVisibility(View.VISIBLE);
                Log.i(TAG, "modeSet: walking_mode");
                showLine(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), mDestination, mode);

            }
        } catch (Exception e) {
            Toast.makeText(PickUpActivity.this, "Not working", Toast.LENGTH_SHORT).show();
        }
    }


    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Snackbar.SnackbarLayout snack_view = (Snackbar.SnackbarLayout) bar.getView();
            snack_view.addView(new ProgressBar(PickUpActivity.this));
            bar.show();
        }

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

 /*   @Override
    protected void onResume() {
        super.onResume();
        if (!isLocationOnline(PickUpActivity.this)) {
            enableLLayout.setVisibility(View.VISIBLE);
        } else enableLLayout.setVisibility(View.INVISIBLE);
        if (!isOnline(PickUpActivity.this)) {
            enableNlayout.setVisibility(View.VISIBLE);
        } else enableNlayout.setVisibility(View.INVISIBLE);

        registerReceiver(myBroadcastReceiver, intentFilter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(myBroadcastReceiver);
    }
*/
    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(20);
                lineOptions.color(Color.parseColor("#08A69F"));

            }

            // Drawing polyline in the Google Map for the i-th route
            try {
                if (mode.equals("mode=walking")) {
                    mMap.addPolyline(lineOptions).setPattern(PATTERN_POLYLINE_DOTTED);
                } else if (mode.equals("mode=driving")) {
                    mMap.addPolyline(lineOptions).setPattern(null);
                }
            } catch (Exception e) {
                if (mode.equals("mode=walking")) {
                    Toast.makeText(PickUpActivity.this, " Walk Route not found", Toast.LENGTH_SHORT).show();

                } else if (mode.equals("mode=driving")) {
                    Toast.makeText(PickUpActivity.this, " Driving Route not found", Toast.LENGTH_SHORT).show();
                }

            }
            bar.dismiss();
        }
    }


    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isLocationOnline(PickUpActivity.this)) {
                enableLLayout.setVisibility(View.VISIBLE);
            }
            if (!isOnline(PickUpActivity.this)) {
                enableNlayout.setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean isLocationOnline(Context context) {

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return gps_enabled;
    }

    public boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }
}
