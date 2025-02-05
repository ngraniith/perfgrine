package com.example.iperftesting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.iperftesting.databinding.ActivityPingTestBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class PingTest : AppCompatActivity() {

    private lateinit var binding: ActivityPingTestBinding
    private lateinit var process: Process
    private lateinit var reader: BufferedReader
    private lateinit var response: StringBuilder
    private val latencyPattern = Regex("time=([\\d.]+) ms")
    private val latencyList = mutableListOf<Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPingTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ipAddress = intent.getStringExtra("ipaddress")

        binding.progress.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {

            performPing(ipAddress)

            withContext(Dispatchers.Main){
                binding.progress.visibility = View.GONE
            }
        }
        binding.backToInputs.setOnClickListener {
            finish()
        }
        binding.stopPing.setOnClickListener {
            process.destroy()
            reader.close()
            response.clear()
        }
        binding.restartPing.setOnClickListener {
            response.clear()
            binding.resultTextView.text = ""
            CoroutineScope(Dispatchers.IO).launch {
                performPing(ipAddress)

                withContext(Dispatchers.Main){
                    binding.progress.visibility = View.GONE
                }
            }
        }

    }

    private suspend fun performPing(ipAddress: String?): String {
        return try {
            Log.d("PingTest", "Performing ping for $ipAddress")
            process = Runtime.getRuntime().exec("/system/bin/ping -c 3600 $ipAddress")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {

                response.append(line).append("\n")
                line?.let { processLog(it) }
                Log.d("output",line.toString())

                withContext(Dispatchers.Main){
                    binding.resultTextView.append(line+ "\n")
                    binding.scrollPingOutput.post{
                        binding.scrollPingOutput.fullScroll(View.FOCUS_DOWN)
                    }
                }

            }
            process.waitFor()
            response.toString()

        } catch (e: IOException) {
            e.printStackTrace()
            "Error: " + e.message
        } catch (e: InterruptedException) {
            e.printStackTrace()
            "Error: " + e.message
        }
    }

    private fun parseLatency(log: String): Double? {
        Log.d("extracted latency",latencyPattern.find(log)?.groupValues?.get(1)?.toDoubleOrNull().toString())
        return latencyPattern.find(log)?.groupValues?.get(1)?.toDoubleOrNull()
    }
    private suspend fun processLog(logLine: String) {

        Log.d("logLine",logLine)
        val latency = parseLatency(logLine) ?: return
        Log.d("latency",latency.toString())
        latencyList.add(latency)

        Log.d("latencyList",latencyList.toString())
        val averageLatency = latencyList.average()
        Log.d("avg latency",averageLatency.toString())

        withContext(Dispatchers.Main){
            binding.pingLatencyText.text = String.format("%.5f ms", averageLatency)
        }
    }
}