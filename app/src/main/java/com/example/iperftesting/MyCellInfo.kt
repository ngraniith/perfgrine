package com.example.iperftesting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MyCellInfo(
    private val context: Context
) {

    data class Result(val pci: Int, val arfcn: Int, val ci: Long?, val rsrp: String, val rsrq: String)

    private val telephonyManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val subscriptionManager: SubscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    private var mySimTelephonyManager : TelephonyManager ?= null
    private var allSubscriptionInfoList : List<SubscriptionInfo> = emptyList()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var latestRsrpSim1: Int? = null
    private var latestRsrqSim1: Int? = null
    private var latestPci:Int = 0
    private var latestCi:Long? = null
    private var latestArfcn: Int = 0

    private var sendRsrp: String = ""
    private var sendRsrq: String = ""

    private val userChoice = SimChoice.getSimChoice()

    private val runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun run() {
            requestCellInfoUpdate()
            handler.postDelayed(this, 500) // 500ms = 1/2 sec
        }
    }

    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 500)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun initialize(requestPermissionsLauncher: ActivityResultLauncher<Array<String>>) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsLauncher.launch(arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                configureTelephonyManager()
                registerTelephonyCallback()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun configureTelephonyManager(){
        val mySubList = getSubInfo()
        Log.d("userChoice",userChoice)
        when(userChoice){
            "SIM1" ->{
                if(mySubList.getOrNull(0)?.simSlotIndex == 0 && mySubList.getOrNull(0)!!.displayName == "SIM1"){

                    mySubList.getOrNull(0)?.subscriptionId?.let {
                        mySimTelephonyManager = telephonyManager.createForSubscriptionId(it)
                        
                        Log.d("createdSub","Success for SIM1")
                    }
                }
            }
            "SIM2" ->{
                if(mySubList.getOrNull(1)?.simSlotIndex == 1 && mySubList.getOrNull(1)!!.displayName == "SIM2"){

                    mySubList.getOrNull(1)?.subscriptionId?.let {
                        mySimTelephonyManager = telephonyManager.createForSubscriptionId(it)
                        Log.d("createdSub","Success for SIM2")
                    }
                }
            }

        }
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun getSubInfo(): List<SubscriptionInfo>{

        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        allSubscriptionInfoList = subscriptionManager.allSubscriptionInfoList
        Log.d("allList",allSubscriptionInfoList.toString())
        return allSubscriptionInfoList
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        val telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onCellInfoChanged(cellInfo: List<CellInfo>) {
                
                Log.d("cellinfo", "entered cellinfo Changed method")
                Log.d("mycellinfo", cellInfo.toString())
                handler.post {
                    processCellInfo(cellInfo)
                }
            }
        }

        mySimTelephonyManager?.registerTelephonyCallback(executor, telephonyCallback)
            ?: telephonyManager.registerTelephonyCallback(executor, telephonyCallback)

        // Request cell info update initially
        requestCellInfoUpdate()

        // Schedule periodic updates
        handler.post(runnable)
        handler.post(uiUpdateRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestCellInfoUpdate() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions not granted, should not be here if initialized properly
            return
        }

        val cellInfoCallback = object : TelephonyManager.CellInfoCallback() {
            override fun onCellInfo(cellInfo: List<CellInfo>) {
                handler.post {
                    processCellInfo(cellInfo)
                }
            }
        }

        mySimTelephonyManager?.requestCellInfoUpdate(executor, cellInfoCallback)
            ?: telephonyManager.requestCellInfoUpdate(executor, cellInfoCallback)
    }



    @RequiresApi(Build.VERSION_CODES.R)
    private fun processCellInfo(cellInfo: List<CellInfo>) {

        Log.d("cellInfoDetails",cellInfo.toString())
        CoroutineScope(Dispatchers.IO).launch {
            for (info in cellInfo) {

                when (val cellIdentity = info.cellIdentity) {
                    is CellIdentityLte -> {
                        // LTE (4G) specific information
                        latestPci = cellIdentity.pci
                        latestArfcn = cellIdentity.earfcn
                        latestCi = cellIdentity.ci.toLong()

                        // Process and use these values as needed
                        println("LTE Cell Info:")
                        println("PCI: $latestPci")
                        println("ARFCN: $latestArfcn")
                        println("NCI: $latestCi")
                    }
                    is CellIdentityNr -> {
                        // NR (5G) specific information
                        latestArfcn = cellIdentity.nrarfcn
                        latestPci= cellIdentity.pci
                        latestCi = cellIdentity.nci

                        // Process and use these values as needed
                        println("NR Cell Info:")
                        println("NRARFCN: $latestArfcn")
                        println("PCI: $latestPci")
                        println("NCI: $latestCi")
                    }
                }

                if (info is CellInfoLte) {
                    val cellSignalStrengthLte = info.cellSignalStrength
                    latestRsrpSim1 = cellSignalStrengthLte.rsrp
                    latestRsrqSim1 = cellSignalStrengthLte.rsrq

                    Log.d("CellInfoLtegf",info.toString())
                    Log.d("Signal", "RSRP: $latestRsrpSim1, RSRQ: $latestRsrqSim1")

                }
                if (info is CellInfoNr) {
                    val cellSignalStrengthNr = info.cellSignalStrength as CellSignalStrengthNr

                    latestRsrpSim1 = cellSignalStrengthNr.ssRsrp
                    latestRsrqSim1 = cellSignalStrengthNr.ssRsrq

                    Log.d("Signal", "RSRP: $latestRsrpSim1, RSRQ: $latestRsrqSim1")
                    Log.d("CellInfoNr",info.toString())
                }
            }

        }

    }

//    fun stopUpdates(){
//        handler.removeCallbacks(runnable)
//        handler.removeCallbacks(uiUpdateRunnable)
//    }

    fun storeRsrp() : String{
        return sendRsrp
    }
    fun storeRsrq() : String{
        return sendRsrq
    }

    fun sendNetworkInfo() : Result{
        println("in send network info")
        println("NRARFCN: $latestArfcn")
        println("PCI: $latestPci")
        println("NCI: $latestCi")
        return Result(latestPci,latestArfcn,latestCi,sendRsrp,sendRsrq)
    }

    private fun updateUI() {
        handler.post{
            latestRsrpSim1?.let {
                if(it != 2147483647){
                    sendRsrp = it.toString()
                    Log.d("sendRSRP",sendRsrp)
                }
            }
            latestRsrqSim1?.let {
                if(it != 2147483647){
                    sendRsrq = it.toString()
                    Log.d("sendRSRQ",sendRsrq)
                }
            }
        }
    }
}