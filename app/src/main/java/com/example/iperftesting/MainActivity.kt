package com.example.iperftesting

import android.Manifest
import android.content.Context
import android.content.Intent

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView

import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {


    private lateinit var resultTextView: TextView
    private lateinit var output: TextView
    private lateinit var stopButton: Button
    private lateinit var duration: TextView
    private lateinit var current: TextView
    private lateinit var backButton: Button
    private lateinit var summaryButton: Button
    private lateinit var scrollOutput: ScrollView
    private var isRunning = false
    private var elapsedTimeSeconds: Int = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private var bitrate = 0
    private var errorFound = false
    private lateinit var allBitrate : ArrayList<Int>

    private var time: Int = 0
    private var transferredData: Int = 0
    private var transferredUnits: String = ""
    private var lostPackets: Int = 0
    private var totalPackets: Int = 0
    private val MY_PERMISSIONS_REQUEST_INTERNET = 123
    private var totalTime: Int = 0
    private var streams: String ?= ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resultTextView = findViewById(R.id.resultTextView)
        output =findViewById(R.id.iperfOutput)
        stopButton = findViewById(R.id.stopButton)
        duration = findViewById(R.id.totalDuration)
        current = findViewById(R.id.currentTime)
        backButton = findViewById(R.id.goBack)
        summaryButton = findViewById(R.id.nextOutput)
        scrollOutput = findViewById(R.id.outputScroll)
        allBitrate = ArrayList()

        resultTextView.text = ""
        output.text = ""
        checkAndRequestInternetPermission()

        summaryButton.setOnClickListener {
            displayFinalOutput()
        }
    }

    private fun startIperf(){

        val path =  applicationInfo.nativeLibraryDir + "/libiperf3.16.so"
        Log.d("path",path)
        val file = File(applicationInfo.nativeLibraryDir, "libiperf3.16.so")
        if (!file.exists()) {
            Log.d("Client", "Cannot find libiperf3.16.so")
        }

        try {
            Os.setenv("TEMP", filesDir.path, true)
            Os.setenv("TMPDIR", filesDir.path, true)
            Os.setenv("TMP", filesDir.path, true)
            Log.d("Client", "TEMP set done")
        } catch (e: ErrnoException) {
            e.printStackTrace()
        }
        Log.d("Path",path)

        resultTextView.text = ""
        output.text= ""
        performIperf(path)
    }
    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    private fun performIperf(path: String): Boolean{
        Log.d("Perform iperf method","Entered the method")
        //"iperf3","-c",ipAddress,"-u","-t","12","-b","800M","R"
        val command = getCommands()
        Log.d("Command",command)
        Toast.makeText(applicationContext,command,Toast.LENGTH_SHORT).show()
        val response = StringBuilder()
        if(command == "-s"){
            duration.text = "Total:${formatDuration(0)}"
        }else{

            if(totalTime == 0){
                time = 300
                duration.text = "Total:${formatDuration(time)}"
            }else{
                time = totalTime
                duration.text = "Total:${formatDuration(time)}"
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val processBuilder = ProcessBuilder(path,*command.split("\\s+".toRegex()).toTypedArray(),"--forceflush","-f","m") // command = "-s"
                val process = processBuilder.start()
                Log.d("hey","started process")
                isRunning = true
                allBitrate.clear()
                val inputStream = process.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                withContext(Dispatchers.Main){

                    backButton.setOnClickListener {
                        process.destroy()
                        inputStream.close()
                        reader.close()
                        isTimerRunning = true
                        Log.d("Ended","Process Terminated")
                        Toast.makeText(applicationContext,"Stopped Testing",Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                var line: String?
                while (reader.readLine().also { line = it } != null) {

                    if(command != "-s"){
                        durationOfTest()
                    }else{
                        durationForServer()
                    }

                    response.append(line).append("\n")
                    Log.d("Output",line.toString())

                    withContext(Dispatchers.Main){
                        output.append(line.toString()+"\n")
                        stopAndRestart(process)
                        scrollOutput.post {
                            scrollOutput.fullScroll(View.FOCUS_DOWN)
                        }
                        displayingOutput(line)
                        getTransferredData(line)
                        getLostNoOfPackets(line)
                    }
                }

                val errorResponse = StringBuilder()
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line).append("\n")

                    Log.d("error Output",line.toString())
                    withContext(Dispatchers.Main){
                        output.append("$line \n")
                        scrollOutput.post {
                            scrollOutput.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }

                if (errorResponse.isNotEmpty()) {

                    Log.d("errorOutput",errorResponse.toString())
                    errorFound = true

               }
                process.waitFor()
                withContext(Dispatchers.Main){
                    stopButton.text = "Restart"
                }


                if(errorFound){
                    Log.d("error","so can't display final output")
                    withContext(Dispatchers.Main){
                        if(!response.contains("server has terminated")){
                            summaryButton.isEnabled = false
                        }
                        stopAndRestart(process)
                    }

                }else{
                    withContext(Dispatchers.Main){
                        summaryButton.isEnabled = true
                        displayFinalOutput()
                    }


                }

                Log.d("Process","Completed")
            } catch (e: IOException) {
                e.printStackTrace()
                "Error: " + e.message
            } catch (e: InterruptedException) {
                e.printStackTrace()
                "Error: " + e.message
            }

        }
        return true

    }

    private fun durationForServer() {

        if(!isTimerRunning){
            timerHandler.post(object : Runnable {
                override fun run() {
                    // Update elapsed time text view
                    current.text = "${formatDuration(elapsedTimeSeconds)}"
                    elapsedTimeSeconds += 1
                    // Check if iperf test is finished (example condition)

                    if(isRunning && !errorFound) {
                        // Continue updating elapsed time
                        timerHandler.postDelayed(this, 1000) // Update every second
                    }else{
                        isTimerRunning = false
                    }
                }
            })
            isTimerRunning = true
        }
    }

    private fun displayFinalOutput() {

        val finalIntent = Intent(applicationContext,FinalOutput::class.java)
        finalIntent.putIntegerArrayListExtra("bitrate list",allBitrate)
        finalIntent.putExtra("transferred data",transferredData)
        finalIntent.putExtra("Units of Data",transferredUnits)
        finalIntent.putExtra("duration",time)
        finalIntent.putExtra("lostPackets",lostPackets)
        finalIntent.putExtra("totalPackets",totalPackets)
        startActivity(finalIntent)
    }
    private fun getLostNoOfPackets(line: String?){

        if(line.toString().contains("receiver")){
            Log.d("packets","Entered packets method")
            val packetsRegex = Regex("""(\d+)/(\d+)""")
            val matchResult = packetsRegex.find(line!!)
            matchResult?.let {
                val (lost,total) = it.destructured
                lostPackets = lost.toInt()
                totalPackets = total.toInt()
                Log.d("lost packets",lostPackets.toString())
                Log.d("Total packets",totalPackets.toString())
            }
        }
    }

    private fun getTransferredData(line: String?) {

        if(line.toString().contains("sender")){
            Log.d("sender","entered sender loop")
            val transferRegex = Regex("""\d+(\.\d+)? [GKM]Bytes""")
            val matchResult = transferRegex.find(line!!)
            matchResult?.let {
                transferredData = it.value.split(" ")[0].toDouble().toInt()
                transferredUnits = it.value.split(" ")[1]
                Log.d("transferred Data",transferredData.toString())
                Log.d("Transferred Units",transferredUnits)
            }
        }
    }

    private fun stopAndRestart(process: Process?){
        stopButton.setOnClickListener {
            if(isRunning){
                process?.destroy()
                stopButton.text = "Restart"
                elapsedTimeSeconds = 0
                timerHandler.removeCallbacksAndMessages(null)
                isRunning = false
                summaryButton.isEnabled = false
                isTimerRunning = false

                Toast.makeText(applicationContext,"Stopped Testing",Toast.LENGTH_SHORT).show()
            }else{
                val path1 =  applicationInfo.nativeLibraryDir + "/libiperf3.16.so"
                stopButton.text = "Stop"
                output.text = ""
                elapsedTimeSeconds = 0
                summaryButton.isEnabled = false
                performIperf(path1)
                Toast.makeText(applicationContext,"Restarted Testing",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun displayingOutput(line: String?){

        if(streams.toString().isNotBlank() && streams.toString().isNotEmpty() && streams?.toInt()!! > 1){
            if(line.toString().contains("[SUM]")){
                Log.d("Sum","entered sum loop")
                bitrate = displayBitRate(line)
                if(bitrate >= 0){
                    resultTextView.text = bitrate.toString()
                }
            }
        } else {
            Log.d("normal","entered normal loop")
            bitrate = displayBitRate(line)
            if(bitrate >=0){
                resultTextView.text = bitrate.toString()
                Log.d("bitrate",bitrate.toString())
            }
        }
    }
    private fun displayBitRate(line: String?): Int{

        if (line.toString().contains("bits/sec")) {
            val bitrateRegex = Regex("""\d+(\.\d+)? [GKM]bits/sec""")
            val matchResult = bitrateRegex.find(line!!)
            matchResult?.let {
                val bitrate = it.value.split(" ")[0].toDouble()
                Log.d("bitsInterval",bitrate.toString())
                allBitrate.add(bitrate.toInt())
                Log.d("allBitrate",allBitrate.toString())
                return bitrate.toInt()
            }
        }
        return bitrate
    }
    private fun durationOfTest(){

        if(!isTimerRunning){
            timerHandler.post(object : Runnable {
                override fun run() {
                    // Update elapsed time text view
                    current.text = formatDuration(elapsedTimeSeconds)
                    elapsedTimeSeconds += 1
                    // Check if iperf test is finished (example condition)
                    val isIperfTestFinished = elapsedTimeSeconds <= time
                    Log.d("total time",time.toString())
                    Log.d("elapsedTime",elapsedTimeSeconds.toString())

                    if (isIperfTestFinished && !errorFound) {
                        // Continue updating elapsed time
                        timerHandler.postDelayed(this, 1000) // Update every second
                    }else{
                        isTimerRunning = false
                    }
                }
            })
            isTimerRunning = true
        }

    }

    private fun checkAndRequestInternetPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            //Toast.makeText(this, "Requesting Internet access", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                MY_PERMISSIONS_REQUEST_INTERNET
            )
        }
        else {
            // Permission is already granted, execute the ping
            //Toast.makeText(this,"Permission is already granted",Toast.LENGTH_SHORT).show()
            startIperf()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_INTERNET -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                    Toast.makeText(this,"Permission is granted and starting ",Toast.LENGTH_SHORT).show()
                    // Permission granted, execute the iperf
                    startIperf()
                } else {
                    // Permission denied, inform the user or take appropriate action
//                    Toast.makeText(this, "Internet permission denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun getCommands(): String{

        var command= ""
        val chosenSystem = intent.getStringExtra("systemChosen")
        Log.d("choosenSystem",chosenSystem.toString())
        val ipAddress = intent.getStringExtra("ipaddress")
        val sharedInputs = applicationContext.getSharedPreferences("getIperfInputs", Context.MODE_PRIVATE)
        val protocol = sharedInputs.getString("protocol","")
        val bandwidth = sharedInputs.getString("bandItem","")
        val portNo = sharedInputs.getString("port value","")
        val ts = sharedInputs.getInt("num secs",0)
        val tm = sharedInputs.getInt("num mins",0)
        val th = sharedInputs.getInt("num hrs",0)
        totalTime = (th*60*60) + (tm*60) + (ts)
        streams = sharedInputs.getString("streams","")
        val mode = sharedInputs.getString("mode","")

        if(chosenSystem == "-s"){
            command = "-s"
            portNo?.takeIf { it.isNotBlank() }?.let { command += " -p $it" }
        }
        else{
            Log.d("protocol",protocol.toString())
            Log.d("bandwidth",bandwidth.toString())
            Log.d("port Number",portNo.toString())
            Log.d("time",totalTime.toString())
            Log.d("No Streams",streams.toString())
            Log.d("mode",mode.toString())

            command = "-c $ipAddress" + if (protocol == "-u") " -u" else ""

            if(protocol == "-u"){
                bandwidth?.takeIf { it.isNotBlank() }?.let { command += " -b $it" }
            }

            portNo?.takeIf { it.isNotBlank() }?.let { command += " -p $it" }

            if(totalTime > 0) command += " -t $totalTime"

            streams?.takeIf { it.isNotBlank() && it.toInt() > 1}?.let { command += " -P $it" }

            if(mode == "R") command += " -R"

            if(command == "-c $ipAddress -b 100M" || command == "-c $ipAddress"){

                command = "-c $ipAddress -u -b 800M -t 300 -R"
            }

            if(command == "-c $ipAddress -u -b 100M"){
                command += " -R"
            }
        }
        return command

    }

}
