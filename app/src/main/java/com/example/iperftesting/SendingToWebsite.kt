package com.example.iperftesting

//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.provider.Settings
//import android.util.Log
//import android.widget.Toast
//import androidx.core.app.ActivityCompat
//import com.example.iperftesting.databinding.ActivityIperfInputsBinding
//import com.example.iperftesting.databinding.ActivitySendingToWebsiteBinding
//import com.example.protobuf.DataOuterClass
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import org.json.JSONObject
//
//class SendingToWebsite : AppCompatActivity() {
//
//    private lateinit var locationHelper: LocationHelper
//    private lateinit var ipFetcher: IPFetcher
//    private lateinit var webSocketClient: WebSocketClient
//    private lateinit var myCellInfo: MyCellInfo
//    private lateinit var binding: ActivitySendingToWebsiteBinding
//    private var liveDataJob: Job? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivitySendingToWebsiteBinding.inflate(layoutInflater)
//        setContentView(binding.root)
////
////        myCellInfo = MyApp.myCellInfoInstance
////
////        locationHelper = LocationHelper(this)
////        ipFetcher = IPFetcher()
////        webSocketClient = MyApp.myWebSocketInstance
////
////        binding.startButton.setOnClickListener {
////            if(checkLocationPermissions()){
////                if(locationHelper.isLocationEnabled()){
////                    locationHelper.initializeLocationUpdates()
////                    locationHelper.startLocationUpdates()
////                    CoroutineScope(Dispatchers.IO).launch {
////                        startSendingLiveData()
////                    }
////                }else{
////                    Toast.makeText(this, "Turn on Location", Toast.LENGTH_SHORT).show()
////                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
////                    startActivity(intent)
////                }
////            }else{
////                requestLocationPermissions()
////            }
////        }
////        binding.endButton.setOnClickListener {
////            stopSendingLiveData()
////        }
//
//    }
//
//    companion object {
//        private const val PERMISSION_LOCATION = 101
//    }
//    private fun requestLocationPermissions() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ),
//            PERMISSION_LOCATION
//        )
//    }
//
//    private fun checkLocationPermissions(): Boolean {
//        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
//    }
//
//    private fun startSendingLiveData(){
//
//        val networkInfo = myCellInfo.sendNetworkInfo()
//        val host = binding.websiteIp.text.toString()
//
//        liveDataJob = CoroutineScope(Dispatchers.IO).launch{
//            while (isActive){
//                ipFetcher.startFetchingIPAddress()
//                val latitude = locationHelper.latitude
//                val longitude = locationHelper.longitude
//                val ueIpAddress = ipFetcher.ipAddress.toString()
//                val throughput = webSocketClient.throughput.value
//
//
//                Log.d("PCI,ARFCN,CI","${networkInfo.pci} ${networkInfo.arfcn} ${networkInfo.ci}")
//                Log.d("updated Throughput",throughput)
//
////                val data = JSONObject().apply {
////                    put("throughput",throughput)
////                    put("rsrp", networkInfo.rsrp)
////                    put("rsrq", networkInfo.rsrq)
////                    put("latitude", latitude)
////                    put("longitude", longitude)
////                    put("ip", ueIpAddress)
////                    put("arfcn",networkInfo.arfcn)
////                    put("pci",networkInfo.pci)
//////                    put("Ci",networkInfo.ci)
////                }
//                val data = DataOuterClass.Data.newBuilder()
//                    .setThroughput(throughput)
//                    .setRsrp(networkInfo.rsrp)
//                    .setRsrq(networkInfo.rsrq)
//                    .setLatitude(latitude)
//                    .setLongitude(longitude)
//                    .setIp(ueIpAddress)
//                    .setArfcn(networkInfo.arfcn)
//                    .setPci(networkInfo.pci)
//                    .build()
//
//                val dataByteArray = data.toByteArray()
//                Log.d("sending data","Throughput: $throughput\n  RSRP: ${networkInfo.rsrp}\n RSRQ:${networkInfo.rsrq} \n Latitude:$latitude \n  Longitude:$longitude\n  Ipaddress: $ueIpAddress\n ARFCN: ${networkInfo.arfcn}\n pci: ${networkInfo.pci} ")
//                // CI: ${networkInfo.ci}
//                webSocketClient.sendLiveData(dataByteArray,host)
//                delay(1000)
//            }
//        }
//    }
//
//    private fun stopSendingLiveData() {
//        liveDataJob?.cancel() // Cancel the coroutine
//        liveDataJob = null    // Reset the Job reference
//        Log.d("LiveData", "Stopped sending live data")
//    }
//
//}