package com.example.iperftesting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.Inet4Address
import java.net.NetworkInterface

class IPFetcher {

    var ipAddress: String? = ""

    fun startFetchingIPAddress(){
        CoroutineScope(Dispatchers.IO).launch {
            while(isActive){
                withContext(Dispatchers.Main){
                    ipAddress = getDeviceIPv4Address()
                }
                delay(5000)
            }
        }
    }
    private fun getDeviceIPv4Address(): String? {

        try{
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for(networkInterface in interfaces){
                val addresses = networkInterface.inetAddresses
                for(address in addresses){
                    if(!address.isLoopbackAddress && address is Inet4Address){

                        return address.hostAddress
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }
}