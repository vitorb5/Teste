package com.codelab.example.mapsteste

import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.codelab.example.mapsteste.Common.Common
import com.codelab.example.mapsteste.Model.MyPlaces
import com.codelab.example.mapsteste.Remote.IGoogleAPIService
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap

    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()

    private lateinit var msLastLocation: Location
    private var mMaker: Marker? = null

    //Localização

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
    }


    lateinit var mService:IGoogleAPIService

    internal lateinit var currentPlaces:MyPlaces

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Serviço de inicialização
        mService = Common.googleApiService

        //Solicitação de tempo de reqisição

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkLocationPermission()) {
                buildLocationRequest();
                buildLocationCallBack();

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }
        else {
            buildLocationRequest();
            buildLocationCallBack();

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

        botton_navigation_view.setOnNavigationItemSelectedListener{item->
            when(item.itemId)
            {
                R.id.action_hospital -> nearByPlace("Hospital")
                R.id.action_market -> nearByPlace("Mercado")
                R.id.action_restaurant -> nearByPlace("Restaurante")
                R.id.action_school -> nearByPlace("Escola")

            }
            true
        }

    }

    private fun nearByPlace(typePlace: String) {

        //Clear all market on Map
        mMap.clear()
        //Build URL request base on location
        val url = getUrl(latitude, longitude, typePlace)

        mService.getNearbyPlaces(url)
            .enqueue(object : Callback<MyPlaces>{
                override fun onResponse(call: Call<MyPlaces>, response: Response<MyPlaces>) {
                    currentPlaces = response!!.body()!!
                    if (response!!.isSuccessful)
                    {
                        for (i in 0 until response!!.body()!!.results!!.size)
                        {
                            val marketOptions=MarkerOptions()
                            val googlePlaces = response.body()!!.results!![i]
                            val lat = googlePlaces.geometry!!.location!!.latitude
                            val lng = googlePlaces.geometry!!.location!!.longitude
                            val placeName = googlePlaces.name
                            val latLng = LatLng(lng,lat)

                            marketOptions.position(latLng)
                            marketOptions.title(placeName)
                            if (typePlace.equals("Hospital"))
                                marketOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hospital))
                            else if(typePlace.equals("Mercado"))
                                marketOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_market))
                            else if (typePlace.equals("Restaurante"))
                                marketOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_restaurant))
                            else if(typePlace.equals("Escola"))
                                marketOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_school))
                            else
                                marketOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            marketOptions.snippet(i.toString()) //Assign index for Market


                            //Add market to map
                            mMap!!.addMarker(marketOptions)
                            //Move camera
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)))
                            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))


                        }





                    }
                }

                override fun onFailure(call: Call<MyPlaces>, t: Throwable) {
                    Toast.makeText(baseContext,""+t!!.message,Toast.LENGTH_SHORT).show()
                }

            })


    }

    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {

        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=10000") //10km
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&keyword=cruise&key=AIzaSyBYO9xKvfKjRLZ9CKeUYGygUrqqusjxXSE")

        Log.d("URL_DEBUG", googlePlaceUrl.toString())
        return googlePlaceUrl.toString()

    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                msLastLocation = p0!!.locations.get(p0!!.locations.size - 1)//Para obeter a última localização
                if (mMaker != null) {
                    mMaker!!.remove()
                }

                latitude = msLastLocation.latitude
                longitude = msLastLocation.longitude

                val latLng = LatLng(latitude, longitude)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title("Essa é a sua posição")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                mMaker = mMap!!.addMarker(markerOptions)

                //Mover a câmera
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
            }
        }
    }

    private fun buildLocationRequest() {

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f

    }

    private fun checkLocationPermission(): Boolean {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), MY_PERMISSION_CODE
                )
            else
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), MY_PERMISSION_CODE
                )

            return false
        } else
            return true

    }

    //overraide onRequestPermissionResult
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode)
        {
            MY_PERMISSION_CODE->{
                if (grantResults.size > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
                {
                    if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if (checkLocationPermission()) {
                            buildLocationRequest();
                            buildLocationCallBack();

                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                            mMap!!.isMyLocationEnabled=true
                        }
                }
                else{
                    Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Init Google Play Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap!!.isMyLocationEnabled = true
            }
        }
        else
            mMap!!.isMyLocationEnabled = true

        //Enable Zoom Control
        mMap.uiSettings.isZoomControlsEnabled=true

    }
}
