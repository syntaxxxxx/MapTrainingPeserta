package com.pakhendri.tracking;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pakhendri.tracking.helper.DirectionMapsV2;
import com.pakhendri.tracking.helper.GPStrack;
import com.pakhendri.tracking.helper.HeroHelper;
import com.pakhendri.tracking.model.Distance;
import com.pakhendri.tracking.model.Duration;
import com.pakhendri.tracking.model.LegsItem;
import com.pakhendri.tracking.model.ResponseWaypoint;
import com.pakhendri.tracking.model.RoutesItem;
import com.pakhendri.tracking.network.ApiService;
import com.pakhendri.tracking.network.RetrofitConfig;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION = 1;
    private static final int REQAWAL = 1;
    private static final int REQAKHIR = 2;
    @BindView(R.id.edtawal)
    EditText edtawal;
    @BindView(R.id.edtakhir)
    EditText edtakhir;
    @BindView(R.id.textjarak)
    TextView textjarak;
    @BindView(R.id.textwaktu)
    TextView textwaktu;
    @BindView(R.id.textharga)
    TextView textharga;
    @BindView(R.id.linearLayout)
    LinearLayout linearLayout;
    @BindView(R.id.btnlokasiku)
    Button btnlokasiku;
    @BindView(R.id.btnpanorama)
    Button btnpanorama;
    @BindView(R.id.linearbottom)
    LinearLayout linearbottom;
    @BindView(R.id.spinmode)
    Spinner spinmode;
    @BindView(R.id.relativemap)
    RelativeLayout relativemap;
    @BindView(R.id.frame1)
    FrameLayout frame1;
    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private GPStrack gps;
    private double lat;
    private double lon;
    private String nama_lokasi;
    private LatLng lokasisaya;
    private Intent intent;
    private double latawal;
    private double lonawal;
    private LatLng lokasiawal;
    private double latakhir;
    private double lonakhir;
    private List<RoutesItem> routes;
    private List<LegsItem> legs;
    private Distance distance;
    private Duration duration;
    private String datapoly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        cekstatusGps();
    }

    private void cekstatusGps() {
        final LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Gps already enabled", Toast.LENGTH_SHORT).show();
            //     finish();
        }
        // Todo 3 Location Already on  ... end

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Gps not enabled", Toast.LENGTH_SHORT).show();
            //todo 4 menampilkan popup untuk mengaktifkan GPS (allow or not)
            enableLoc();
        }
    }

    private void enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(MapsActivity.this, REQUEST_LOCATION);
                                finish();
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                        }, 100
                );
            }
            return;
        }else {
            mMap = googleMap;

            akseslokasiku();
        }
        }

    private void akseslokasiku() {
        gps = new GPStrack(this);
        if (gps.canGetLocation() && mMap != null) {
            lat = gps.getLatitude();
            lon = gps.getLongitude();
            nama_lokasi = convertlocation(lat, lon);
            Toast.makeText(this, "lat:" + lat + "\nlon:" + lon, Toast.LENGTH_SHORT).show();
            lokasisaya = new LatLng(lat, lon);
            mMap.addMarker(new MarkerOptions().position(lokasisaya).title(nama_lokasi)).setIcon(
                    BitmapDescriptorFactory.fromResource(R.mipmap.ic_map));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lokasisaya, 16));
            mMap.getUiSettings().isCompassEnabled();
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.getUiSettings().isMyLocationButtonEnabled();
            edtawal.setText(nama_lokasi);
        }
    }

    private String convertlocation(double lat, double lon) {
        nama_lokasi = null;
        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        try {
            List<Address> list = geocoder.getFromLocation(lat, lon, 1);
            if (list != null && list.size() > 0) {
                nama_lokasi = list.get(0).getAddressLine(0) + "" + list.get(0).getCountryName();

                //fetch data from addresses
            } else {
                Toast.makeText(this, "kosong", Toast.LENGTH_SHORT).show();
                //display Toast message
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nama_lokasi;
    }

    @OnClick({R.id.edtawal, R.id.edtakhir, R.id.btnlokasiku, R.id.btnpanorama})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.edtawal:
                try{
                    intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(
                            MapsActivity.this);
                    startActivityForResult(intent,REQAWAL);
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.edtakhir:
                try{
                    intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(
                            MapsActivity.this);
                    startActivityForResult(intent,REQAKHIR);
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btnlokasiku:
                akseslokasiku();
                break;
            case R.id.btnpanorama:
                aksespanorama();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Place p = PlaceAutocomplete.getPlace(this,data);
        if (requestCode==REQAWAL&&resultCode==RESULT_OK){
            latawal =p.getLatLng().latitude;
            lonawal =p.getLatLng().longitude;
            nama_lokasi =p.getName().toString();
            edtawal.setText(nama_lokasi);
            mMap.clear();
            addmarker(latawal,lonawal);
        }else if (requestCode==REQAKHIR&&resultCode==RESULT_OK){
            latakhir =p.getLatLng().latitude;
            lonakhir =p.getLatLng().longitude;
            nama_lokasi =p.getName().toString();
            edtakhir.setText(nama_lokasi);
            addmarker(latakhir,lonakhir);
            aksesrute();
        }
    }

    private void aksesrute() {
        final ProgressDialog dialog = ProgressDialog.show(this,"proses get rute","loading . .. ");
        ApiService apiService = RetrofitConfig.getInstanceRetrofit();
        String api =getText(R.string.google_maps_key).toString();
        Call<ResponseWaypoint> waypointCall = apiService.request_route(
              edtawal.getText().toString(),
              edtakhir.getText().toString(),
                api

        );
        waypointCall.enqueue(new Callback<ResponseWaypoint>() {
            @Override
            public void onResponse(Call<ResponseWaypoint> call, Response<ResponseWaypoint> response) {
                dialog.dismiss();
                if (response.isSuccessful()){
                    String status =response.body().getStatus();
                    if (status.equals("OK")){
                    routes = response.body().getRoutes();
                    legs =routes.get(0).getLegs();
                    distance =legs.get(0).getDistance();
                    duration =legs.get(0).getDuration();
                  textjarak.setText(distance.getText().toString());
                  textwaktu.setText(duration.getText().toString());
                  double harga = Math.ceil(Double.valueOf(distance.getValue()/1000));
                  double total = harga*1000;
                  textharga.setText("Rp."+HeroHelper.toRupiahFormat2(String.valueOf(total)));
                        DirectionMapsV2 mapsV2 = new DirectionMapsV2(MapsActivity.this);
                        datapoly =routes.get(0).getOverviewPolyline().getPoints();
                        mapsV2.gambarRoute(mMap,datapoly);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseWaypoint> call, Throwable t) {

            }
        });

    }

    private void addmarker(double lat, double lon) {
        lokasiawal = new LatLng(lat, lon);
        nama_lokasi = convertlocation(lat, lon);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lokasiawal, 15));
        mMap.addMarker(new MarkerOptions().position(lokasiawal).title(nama_lokasi));

    }

    private void aksespanorama() {
        relativemap.setVisibility(View.GONE);
        frame1.setVisibility(View.VISIBLE);
        SupportStreetViewPanoramaFragment panoramaFragment =(SupportStreetViewPanoramaFragment)
                getSupportFragmentManager().findFragmentById(R.id.panorama);
        panoramaFragment.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
            @Override
            public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
                streetViewPanorama.setPosition(lokasisaya);
            }
        });

    }
}
