package com.example.iperftesting


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.iperftesting.databinding.ActivityIperfInputsBinding
import java.util.regex.Pattern

class IperfInputs : AppCompatActivity() {

    private lateinit var binding: ActivityIperfInputsBinding
    private lateinit var ipAddress: EditText
    private lateinit var radioGroup: RadioGroup

    private var checkedSystem: Int = -1
//    private var appInBackground = false
    private lateinit var choosedButton: RadioButton
    private val PERMISSION_REQUEST_CODE = 1
    private var serverSelected: Boolean = false


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIperfInputsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.saveLogsFile.setOnCheckedChangeListener{_,isChecked ->
            if(!isChecked){
                showAlertDialog()
            }
        }

        binding.networkTest.setOnCheckedChangeListener{_,checkedId ->

            when(checkedId){
                R.id.iperfTest ->{

                    if(arePermissionsGranted()){
                        Log.d("Phone and Location","Granted")
                    }else{
                        requestPermissions()
                    }
                    getSystemSelection()
                    onStartButtonPressedIperf()
                }
                R.id.ping -> {
                    startPingTest()
                }
            }
        }

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

    private fun arePermissionsGranted(): Boolean {

        return (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
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
    }