package com.example.iperftesting

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.Manifest
import android.location.LocationManager
import androidx.core.content.getSystemService

class LocationHelper(private val context: Context) {

    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    var latitude: String = ""
    var longitude: String = ""

    fun initializeLocationUpdates(){
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).setMinUpdateIntervalMillis(5000).build()

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if(location != null){
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                }


            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun isLocationEnabled(): Boolean{
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}