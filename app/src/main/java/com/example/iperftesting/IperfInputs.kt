package com.example.iperftesting


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Secure
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.iperftesting.databinding.ActivityIperfInputsBinding
import com.example.protobuf.DataOuterClass
//import com.example.protobuf.DataOuterClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.regex.Pattern


class IperfInputs : AppCompatActivity() {

    private lateinit var binding: ActivityIperfInputsBinding
    private lateinit var ipAddress: EditText
    private lateinit var radioGroup: RadioGroup

    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var myCellInfo: MyCellInfo
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var locationHelper: LocationHelper
    private lateinit var ipFetcher: IPFetcher
    private lateinit var webSocketClient: WebSocketClient
    private var rsrp: String = ""
    private var rsrq: String = ""

    private var checkedSystem: Int = -1
//    private var appInBackground = false
    private lateinit var choosedButton: RadioButton
    private val PERMISSION_REQUEST_CODE = 1
    private var serverSelected: Boolean = false

    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 500)
        }
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIperfInputsBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val androidId = Secure.getString(
//            this.getContentResolver(),
//            Secure.ANDROID_ID
//        )
//
//        Log.d("android_ID",androidId)
        ipAddress = binding.ipaddress
        radioGroup = binding.system

        binding.saveLogsFile.isChecked = true

        binding.SimRadioGroup.setOnCheckedChangeListener { _, checkedId ->


            when (checkedId) {
                R.id.Sim1RadioButton -> {

                    SimChoice.setSimChoice("SIM1")
                }
                R.id.Sim2RadioButton -> {
                    SimChoice.setSimChoice("SIM2")
                }
                else -> {
                    SimChoice.setSimChoice("")
                }
            }
        }

        myCellInfo = MyApp.myCellInfoInstance

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val granted = permissions[Manifest.permission.READ_PHONE_STATE] == true &&
                        permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                if (granted) {
                    myCellInfo.initialize(requestPermissionsLauncher)
                } else {
                    // Handle the case where permissions are not granted
                }
            }
        }

        myCellInfo.initialize(requestPermissionsLauncher)
        handler.post(uiUpdateRunnable)


        val currentSelection = binding.networkTest.checkedRadioButtonId
        handleSelectedTest(currentSelection)

        binding.saveLogsFile.setOnCheckedChangeListener{_,isChecked ->
            if(!isChecked){
                showAlertDialog()
            }
        }

        binding.networkTest.setOnCheckedChangeListener{_,checkedId ->
            handleSelectedTest(checkedId)
        }

        getSystemSelection()

        binding.moreOptions.setOnClickListener {
            val bottomSheet = MoreInputs()
            val bundle = Bundle().apply {
                putBoolean("selectedServer",serverSelected)
            }
            bottomSheet.arguments = bundle
            bottomSheet.show(supportFragmentManager,bottomSheet.tag)
        }

        val floatInAnimation = AnimationUtils.loadAnimation(this,R.anim.float_anim)
        val alphaAnimation = AnimationUtils.loadAnimation(this,R.anim.alpha_anim)
        binding.command.startAnimation(alphaAnimation)
        binding.moreText.startAnimation(floatInAnimation)

        locationHelper = LocationHelper(this)
        ipFetcher = IPFetcher()
        webSocketClient = MyApp.myWebSocketInstance

//        if(checkLocationPermissions()){
//            if(locationHelper.isLocationEnabled()){
//                locationHelper.initializeLocationUpdates()
//                locationHelper.startLocationUpdates()
//                CoroutineScope(Dispatchers.IO).launch {
//                    startSendingLiveData()
//                }
//            }else{
//                Toast.makeText(this, "Turn on Location", Toast.LENGTH_SHORT).show()
//                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                startActivity(intent)
//            }
//        }else{
//            requestLocationPermissions()
//        }
    }

    //                val data = JSONObject().apply {
//                    put("throughput",throughput)
//                    put("rsrp", rsrp)
//                    put("rsrq", rsrq)
//                    put("latitude", latitude)
//                    put("longitude", longitude)
//                    put("ip", ueIpAddress)
//                    put("arfcn",networkInfo.arfcn)
//                    put("pci",networkInfo.pci)
////                    put("Ci",networkInfo.ci)
//                }
    private fun startSendingLiveData(){

        CoroutineScope(Dispatchers.IO).launch{
            while (isActive){
                val networkInfo = myCellInfo.sendNetworkInfo()
                ipFetcher.startFetchingIPAddress()
                val latitude = locationHelper.latitude
                val longitude = locationHelper.longitude
                val ueIpAddress = ipFetcher.ipAddress.toString()
                val throughput = webSocketClient.throughput.value

                val data = DataOuterClass.Data.newBuilder()
                    .setThroughput(throughput)
                    .setRsrp(networkInfo.rsrp)
                    .setRsrq(networkInfo.rsrq)
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setIp(ueIpAddress)
                    .setArfcn(networkInfo.arfcn)
                    .setPci(networkInfo.pci)
                    .build()

                Log.d("proto data",data.toString())
                val dataByteArray = data.toByteArray()


                Log.d("sending data","Throughput: $throughput\n  RSRP: $rsrp\n RSRQ:$rsrq \n Latitude:$latitude \n  Longitude:$longitude\n  Ipaddress: $ueIpAddress\n ARFCN: ${networkInfo.arfcn}\n pci: ${networkInfo.pci} ")
                // CI: ${networkInfo.ci}
//                webSocketClient.sendLiveData(dataByteArray,"")
                delay(1000)
            }
        }
    }
    companion object {
        private const val PERMISSION_LOCATION = 101
    }
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_LOCATION
        )
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    private fun startPingTest(){

        val pingIntent = Intent(this,PingTest::class.java)

        binding.startButton.setOnClickListener {
            pingIntent.putExtra("ipaddress",ipAddress.text.toString())
            startActivity(pingIntent)
        }
    }
    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Warning")
        builder.setMessage("Deselecting this option will prevent graphs from being drawn. Are you sure you want to proceed?")
        builder.setPositiveButton("Yes") { dialog,_ ->
            dialog.dismiss()
        }
        builder.setNegativeButton("No"){ dialog,_ ->
            binding.saveLogsFile.isChecked = true
            dialog.dismiss()
        }
        builder.setOnCancelListener {
            binding.saveLogsFile.isChecked = true
        }
        builder.show()
    }

    private fun onStartButtonPressedIperf() {
        binding.startButton.setOnClickListener{

            val iperfIntent = Intent(this,MainActivity::class.java)

            if(radioGroup.checkedRadioButtonId == R.id.client){

                iperfIntent.putExtra("systemChosen","-c")
                iperfIntent.putExtra("saveToFile",binding.saveLogsFile.isChecked)

                if(isValidIp(ipAddress.text.toString())){
                    //Toast.makeText(this,"Valid Ipaddress",Toast.LENGTH_SHORT).show()
                    iperfIntent.putExtra("ipaddress",ipAddress.text.toString())
                    startActivity(iperfIntent)
                    finish()
                }
                else{
                    Toast.makeText(this,"Invalid Ipaddress",Toast.LENGTH_SHORT).show()
                }
            }else{
                iperfIntent.putExtra("systemChosen","-s")
                iperfIntent.putExtra("saveToFile",binding.saveLogsFile.isChecked)
                startActivity(iperfIntent)
                finish()
            }
        }
    }

    private fun handleSelectedTest(checkedId: Int){

        when(checkedId){
            R.id.iperfTest ->{

                if(checkLocationPermissions()){
                    Log.d("Phone and Location","Granted")
                }else{
                    requestLocationPermissions()
                }
                onStartButtonPressedIperf()
            }
            R.id.ping -> {
                startPingTest()
            }
        }
    }

    private fun getSystemSelection() {

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            choosedButton = findViewById(checkedId)
            serverSelected = choosedButton.id == R.id.server
            Log.d("serverSelected",serverSelected.toString())
            if(serverSelected){
                binding.command.text = "iperf3 -s"
                binding.ipaddress.isEnabled = false
            }
            else{
                binding.command.text = "iperf3 -c ipaddress -u -b 800M -t 3600"
                binding.ipaddress.isEnabled = true
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {

                Toast.makeText(this, "Permissions is granted.", Toast.LENGTH_SHORT).show()
            } else {
                // Handle the case where permissions were not granted
                // You might want to inform the user that the app cannot function without these permissions
                // and possibly close the app or disable certain features.
                // For this example, we'll show a Toast message.
                Toast.makeText(this, "Permissions not granted. The app cannot function properly.", Toast.LENGTH_LONG).show()
                requestLocationPermissions()
            }
        }
    }
    private fun isValidIp(ip: String): Boolean{
        val pattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        val matcher = pattern.matcher(ip)
        return matcher.matches()
    }
    override fun onPause() {
        super.onPause()
//        appInBackground = true
        val sharedPreferences = getSharedPreferences("getInputs",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ipaddress",ipAddress.text.toString())
        editor.putBoolean("saveLogsFile", binding.saveLogsFile.isChecked)
        editor.putInt("system",radioGroup.indexOfChild(findViewById(radioGroup.checkedRadioButtonId)))

        Log.d("system",radioGroup.indexOfChild(findViewById(radioGroup.checkedRadioButtonId)).toString())

        editor.apply()

//        if(checkLocationPermissions() && locationHelper.isLocationEnabled()){
//            locationHelper.stopLocationUpdates()
//        }
    }
    override fun onResume() {
        super.onResume()
    //        if (appInBackground) {
    //            startActivity(Intent(this, IperfInputs::class.java))
    //            finish()
    //        }
        val sharedPreferences = getSharedPreferences("getInputs",Context.MODE_PRIVATE)

        checkedSystem = sharedPreferences.getInt("system",-1)
        Log.d("checkedSystem",checkedSystem.toString())
        if(checkedSystem != -1){
            val systemButton = radioGroup.getChildAt(checkedSystem).id
            radioGroup.check(systemButton)

        }
        binding.saveLogsFile.isChecked = sharedPreferences.getBoolean("saveLogsFile", true)

        ipAddress.setText(sharedPreferences.getString("ipaddress",""))

    //        appInBackground = false
    }
    private fun updateUI(){
        handler.post{
            getNetworkInfo()
        }
    }
    private fun getNetworkInfo(){

        rsrp = myCellInfo.storeRsrp()
        rsrq = myCellInfo.storeRsrq()

        binding.rsrpValue.text = "RSRP: $rsrp"
        binding.rsrqValue.text = "RSRQ: $rsrq"

        Log.d("Rsrp,rsrq IperfInputs","$rsrp $rsrq")
    }
}