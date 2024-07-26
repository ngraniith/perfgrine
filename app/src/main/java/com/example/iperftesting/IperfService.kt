package com.example.iperftesting

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.storage.StorageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class IperfService : Service() {


    private val notificationChannelId = "IperfServiceChannel"
    private val notificationId = 1234
    private lateinit var notificationManager: NotificationManager

    private var command: String = ""
    private val response = StringBuilder()
    private var errorFound = false
    private var chosenSystem: String = ""

    private var process: Process? = null
    private var inputStream: InputStream? = null
    private var reader: BufferedReader? = null
    private var saveToFile: Boolean? = false

    private val actionListener = IperfServiceManager.actionListener
    private val callback = IperfServiceManager.callback

    private lateinit var logFileWriter: BufferedWriter
    private lateinit var directory: File

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        command = intent?.getStringExtra("command").toString()
        val path = intent?.getStringExtra("path")
        saveToFile = intent?.getBooleanExtra("saveToFile",false)
        chosenSystem = intent?.getStringExtra("chosenSystem").toString()

        Log.d("saveToFileService",saveToFile.toString())
        createNotificationChannel()


        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Iperf Service")
            .setContentText("Running iperf test...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)
        if(saveToFile == true){
            Log.d("saveToFileServiceTrue",saveToFile.toString())
            createLogFile()
        }



        CoroutineScope(Dispatchers.IO).launch {
            try {
                val processBuilder = ProcessBuilder(
                    path,
                    *command.split("\\s+".toRegex()).toTypedArray(),
                    "--forceflush",
                    "-f",
                    "m"
                ) // command = "-s"
                delay(1000)
                process = processBuilder.start()
                Log.d("hey", "started process")
                inputStream = process?.inputStream
                reader = BufferedReader(InputStreamReader(inputStream))
                val errorReader = BufferedReader(InputStreamReader(process?.errorStream))

                withContext(Dispatchers.Main) {

                    actionListener?.onBackButtonClicked(process!!, inputStream!!, reader!!)
                    actionListener?.onStopRestartClicked(process)
                }


                var line: String?
                while (reader?.readLine().also { line = it } != null) {

                    withContext(Dispatchers.Main){
                        if (chosenSystem != "-s") {
                            actionListener?.durationOfClient()
                        } else {
                            actionListener?.durationOfServer()
                        }
                    }


                    Log.d("Output", line.toString())
                    response.append(line).append("\n")
                    withContext(Dispatchers.Main) {
                        callback?.onResultReceived(line)
                    }

                    parseIperfOutput(line.toString())
                }

                val errorResponse = StringBuilder()
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line).append("\n")

                    Log.d("error Output", line.toString())
                    withContext(Dispatchers.Main) {
                        callback?.onResultReceived(line)
                    }
                    logErrorOutput(line.toString())
                }

                if (errorResponse.isNotEmpty()) {
                    Log.d("errorOutput", errorResponse.toString())
                    errorFound = true

                }
                process?.waitFor()

                withContext(Dispatchers.Main) {
                    actionListener?.stopToRestart()
                }

                if (errorFound) {
                    Log.d("error", "so can't display final output")
                    withContext(Dispatchers.Main) {
                        actionListener?.onSummaryButtonEnabled(false)
                        actionListener?.onStopRestartClicked(process)
                        actionListener?.stopIperfTest(process!!,inputStream!!,reader!!)
                        actionListener?.errorFound = true
                        actionListener?.toStopService(true)
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        actionListener?.onSummaryButtonEnabled(true)
                        actionListener?.onDisplayingFinalOutput()
                    }

                }

                Log.d("Process", "Completed")


                withContext(Dispatchers.Main){
                    if(saveToFile==true && !errorFound)
                    Toast.makeText(applicationContext,"File created successfully, Download/IperfLogs",Toast.LENGTH_SHORT).show()
                }


            } catch (e: IOException) {
                e.printStackTrace()
                "Error: " + e.message
            } catch (e: InterruptedException) {
                e.printStackTrace()
                "Error: " + e.message
            }
        }
        return START_STICKY
    }

private fun logErrorOutput(line: String) {
    if (!this::logFileWriter.isInitialized) {
        Log.e("IperfErrorOutput", "logFileWriter is not initialized")
        return
    }

    val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val errorLineToWrite = String.format("%-20s %-12s\n", timeStamp, "ERROR: $line")
    logFileWriter.write(errorLineToWrite)
    logFileWriter.flush()
}
private fun parseIperfOutput(line: String) {

    if (!this::logFileWriter.isInitialized) {
        Log.e("IperfOutput", "logFileWriter is not initialized")
        return
    }

    if(!line.contains("sender") && !line.contains("receiver")){
        val throughputPattern = Regex("""(\d+(\.\d+)?)\s+([GMK]bits/sec)""")
        val packetLossPattern = Regex("""(\d+)/(\d+)""")

        val datagramsPattern = Regex("""\s+\d+(\.\d+)?\s+[GMK]?Bytes\s+\d+(\.\d+)?\s+Mbits/sec\s+(\d+)""")
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val throughputMatch = throughputPattern.find(line)
        val throughput = throughputMatch?.groupValues?.get(1) ?: ""
        val throughputUnit = throughputMatch?.groupValues?.get(3) ?: ""

        val packetLossMatch = packetLossPattern.find(line)
        val lostPackets = packetLossMatch?.groupValues?.get(1) ?: ""
        val totalPackets = packetLossMatch?.groupValues?.get(2) ?: ""

        val datagramMatch = datagramsPattern.find(line)
        val totalDatagrams = datagramMatch?.groupValues?.get(3) ?: ""
        Log.d("totalDatagrams",totalDatagrams)

        val rsrp = actionListener?.getRsrp() ?: ""

        if(chosenSystem !="-s"){
            if (throughput.isNotEmpty()) {
                val lineToWrite = String.format(
                    "%-20s %-20s %-15s %s\n",
                    timeStamp,
//            interval,
//            "$dataPushed $dataPushedUnit",
                    "$throughput $throughputUnit",
                    if (lostPackets.isBlank() || totalPackets.isBlank()) "0/0" else "$lostPackets/$totalPackets",
                    rsrp
                )
                logFileWriter.write(lineToWrite)
                logFileWriter.flush()
            }
        }else{
            if (throughput.isNotEmpty()) {
                val lineToWrite = String.format(
                    "%-20s %-20s %-15s %s\n",
                    timeStamp,
//            interval,
//            "$dataPushed $dataPushedUnit",
                    "$throughput $throughputUnit",
                    totalDatagrams.ifBlank { "0" },
                    rsrp
                )
                logFileWriter.write(lineToWrite)
                logFileWriter.flush()
            }
        }
    }


}
    private fun addTableHeader(writer: BufferedWriter) {
        val header = String.format(
            "%-20s %-20s %-16s %s\n",
            "Time Stamp",
//            "Interval",
//            "Data Pushed",
            "Throughput",
            if(chosenSystem == "-s") "Total Datagrams" else "Packet Loss",
            "RSRP"
        )
        val separator = "-".repeat(header.length) + "\n"
        writer.write(header)
        writer.write(separator)
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun createLogFile() {
        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolume = storageManager.storageVolumes[0]
        directory = File(storageVolume.directory?.path + "/Download/IperfLogs" )
        if(!directory.exists()){
            val dirCreated = directory.mkdirs()
            Log.d("DirectoryCreation", "Directory created: $dirCreated")
        }

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "IperfTest $timeStamp.txt"

        val file = File(directory,fileName)
        try{
            val fileCreated = file.createNewFile()
            Log.d("FileCreation", "File created: $fileCreated")
           logFileWriter = BufferedWriter(FileWriter(file))
            addTableHeader(logFileWriter)
            Log.d("FileCreation", "BufferedWriter initialized and header added")
        }catch(e: IOException){
            e.printStackTrace()
            Log.e("FileCreation", "Error creating file: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Iperf Service"
            val descriptionText = "Running iperf test"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(notificationChannelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        logFileWriter.close()
        actionListener?.stopIperfTest(process!!,inputStream!!,reader!!)
        super.onTaskRemoved(rootIntent)
    }
}