package com.example.iperftesting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.iperftesting.databinding.ActivityFinalOutputBinding

class FinalOutput : AppCompatActivity() {
    private lateinit var binding: ActivityFinalOutputBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinalOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displaySummary()
        binding.backToMain.setOnClickListener {
            finish()
        }
    }

    private fun displaySummary() {

        val receivedArrayList = intent.getIntegerArrayListExtra("bitrate list")

        if (receivedArrayList != null) {
            binding.minBitrate.append( receivedArrayList.minOrNull().toString() + " Mbps")
            binding.maxBitrate.append(receivedArrayList.maxOrNull().toString() + " Mbps")
            val averageBitrate = receivedArrayList[receivedArrayList.size-1]
            Log.d("Average",averageBitrate.toString())
            binding.avgBitrate.append("$averageBitrate Mbps")
            binding.transferredData.append(intent.getIntExtra("transferred data",0).toString()+ " "+intent.getStringExtra("Units of Data"))
            binding.duration.append(intent.getIntExtra("duration",0).toString()+" secs")
            binding.packets.append(intent.getIntExtra("lostPackets",0).toString()+" / "+intent.getIntExtra("totalPackets",0))
        }
    }
}