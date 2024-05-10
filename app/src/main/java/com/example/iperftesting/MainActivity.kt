package com.example.iperftesting

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent

import android.content.pm.PackageManager
import android.os.Build
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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Serializable


class MainActivity : AppCompatActivity(),  IperfActionListener, IperfCallback{


    private lateinit var resultTextView: TextView
    private lateinit var output: TextView
    private lateinit var stopButton: Button
    private lateinit var duration: TextView
    private lateinit var current: TextView
    private lateinit var backButton: Button
    private lateinit var summaryButton: Button
    private lateinit var scrollOutput: ScrollView
    private val FOREGROUND_PERMISSION_REQUEST = 124


    private var isRunning = false
    private var elapsedTimeSeconds: Int = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private var bitrate = 0
    private var errorFound = false
    private var onBackPressed = false
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

        IperfServiceManager.actionListener = this
        IperfServiceManager.callback = this

        checkAndRequestInternetPermission()


        resultTextView.text = ""
        output.text = ""

        summaryButton.setOnClickListener {
            onDisplayingFinalOutput()
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
        performIperf()
    }
    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    private fun performIperf(): Boolean{
        Log.d("Perform iperf method","Entered the method")
        //"iperf3","-c",ipAddress,"-u","-t","12","-b","800M","R"
        val command = onGetCommand()
        Log.d("Command",command)
        Toast.makeText(applicationContext,command,Toast.LENGTH_SHORT).show()
        isTimerRunning = false


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

        startIperfService(false)
        return true
    }

    private fun startIperfService(serviceRunning: Boolean) {

        isRunning = true
        allBitrate.clear()
        val serviceIntent = Intent(this, IperfService::class.java)
        val command = onGetCommand()
        serviceIntent.putExtra("command",command)
        serviceIntent.putExtra("path",applicationInfo.nativeLibraryDir + "/libiperf3.16.so")
        if(!serviceRunning){
            Toast.makeText(applicationContext,"Started Service",Toast.LENGTH_SHORT).show()
            startService(serviceIntent)
        }else{
            Toast.makeText(applicationContext,"Stopped Service",Toast.LENGTH_SHORT).show()
            stopService(serviceIntent)
        }

    }

    override fun toStopService(value: Boolean) {
        startIperfService(value)
    }

    override fun stopToRestart() {
        stopButton.text = "Restart"
    }

    override fun onResultReceived(line: String?) {

        Log.d("Output",line.toString())
        output.append(line.toString()+"\n")
        scrollOutput.post {
            scrollOutput.fullScroll(View.FOCUS_DOWN)
        }
        displayingOutput(line)
        getTransferredData(line)
        getLostNoOfPackets(line)
    }

    override fun onSummaryButtonEnabled(value: Boolean) {
        summaryButton.isEnabled = value
    }

    override fun durationOfServer() {
        if(!isTimerRunning){
            timerHandler.post(object : Runnable {
                override fun run() {
                    // Update elapsed time text view
                    current.text = "${formatDuration(elapsedTimeSeconds)}"
                    elapsedTimeSeconds += 1
                    // Check if iperf test is finished (example condition)

                    if(isRunning && !errorFound && !onBackPressed) {
                        // Continue updating elapsed time
                        timerHandler.postDelayed(this, 1000) // Update every second
                    }else{
                        isTimerRunning = true
                    }
                }
            })
            isTimerRunning = true
        }
    }

    override fun onStopRestartClicked(process: Process?) {
        stopButton.setOnClickListener {
            if(isRunning && stopButton.text == "Stop"){
                    process?.destroy()
                    stopButton.text = "Restart"
                    elapsedTimeSeconds = 0
                    timerHandler.removeCallbacksAndMessages(null)
                    toStopService(true)
                    isRunning = false
                    summaryButton.isEnabled = false
                    isTimerRunning = false

                    Toast.makeText(applicationContext,"Stopped Testing",Toast.LENGTH_SHORT).show()
                }
            else{
                if(stopButton.text == "Restart"){
                    stopButton.text = "Stop"
                    output.text = ""
                    elapsedTimeSeconds = 0
                    summaryButton.isEnabled = false
                    performIperf()
                    Toast.makeText(applicationContext,"Restarted Testing",Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    override fun onBackButtonClicked(process: Process,inputStream: InputStream,reader: BufferedReader) {
        backButton.setOnClickListener {
            onBackPressed = true
            process.destroy()
            inputStream.close()
            reader.close()
            isTimerRunning = true
            toStopService(true)
            Log.d("Ended","Process Terminated")
            Toast.makeText(applicationContext,"Stopped Testing",Toast.LENGTH_SHORT).show()
            startActivity(Intent(applicationContext, IperfInputs::class.java))
            finish()
        }
    }

    override fun durationOfClient() {
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

                    if (isIperfTestFinished && !errorFound && !onBackPressed) {
                        // Continue updating elapsed time
                        timerHandler.postDelayed(this, 1000) // Update every second
                    }else{
                        isTimerRunning = true
                    }
                }
            })
            isTimerRunning = true
        }
    }

    override fun onDisplayingFinalOutput() {

        val finalIntent = Intent(applicationContext,FinalOutput::class.java)

        finalIntent.putIntegerArrayListExtra("bitrate list",allBitrate)
        finalIntent.putExtra("transferred data",transferredData)
        finalIntent.putExtra("Units of Data",transferredUnits)
        finalIntent.putExtra("duration",time)
        finalIntent.putExtra("lostPackets",lostPackets)
        finalIntent.putExtra("totalPackets",totalPackets)
        startActivity(finalIntent)
    }

    override fun getLostNoOfPackets(line: String?) {
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

    override fun getTransferredData(line: String?) {
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

    override fun displayingOutput(line: String?) {
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
        else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            Log.d("Requesting FS","Requesting Foreground service")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                FOREGROUND_PERMISSION_REQUEST
            )
        }
        else {
            // Permission is already granted
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

            FOREGROUND_PERMISSION_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("Granted FS","Granted Foreground service")
                    // Permission granted, execute the iperf
                    startIperf()
                } else {
                    // Permission denied, inform the user or take appropriate action
                    Toast.makeText(this, "FS permission denied", Toast.LENGTH_SHORT).show()
                    Log.d("Denied FS","Denied Foreground service")
                }
                return
            }
        }
    }

    override fun onGetCommand(): String {

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

        Log.d("Command At method",command)
        return command
    }

}
