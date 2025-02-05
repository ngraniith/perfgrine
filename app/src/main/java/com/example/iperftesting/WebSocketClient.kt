package com.example.iperftesting

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WebSocketClient {

    private val _throughput = MutableStateFlow("")
    val throughput: StateFlow<String> = _throughput

    fun getThroughput(throughput: String){
        _throughput.value = throughput
        Log.d("THROUGHPUT in Socket",_throughput.value)
    }

    suspend fun sendLiveData( dataByteArray: ByteArray,hostIP: String) {

        withContext(Dispatchers.IO){
            try {
                val destinationAddress = InetAddress.getByName("192.168.1.53")
                val port = 8000
                // 10.1.103.230 venkat sir
                //10.1.104.253 my system
                println("the data: $dataByteArray")
                println("The dataBytes: ${dataByteArray.size}")
                val myPacket = DatagramPacket(dataByteArray, dataByteArray.size, destinationAddress, port)
                DatagramSocket().use { socket ->
                    socket.send(myPacket)
                    Log.d("packet", myPacket.length.toString())
                    Log.d("Sending packet", "Sent Successfully to $10.1.103.230:$port")
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}