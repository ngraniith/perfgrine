package com.example.iperftesting

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class IperfService : Service() {


    private val notificationChannelId = "IperfServiceChannel"
    private val notificationId = 1234
    private lateinit var notificationManager: NotificationManager

    var command: String = ""
    private val response = StringBuilder()
    private var errorFound = false
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val actionListener = IperfServiceManager.actionListener
        val callback = IperfServiceManager.callback

        command = intent?.getStringExtra("command").toString()
        val path = intent?.getStringExtra("path")

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val processBuilder = ProcessBuilder(
                    path,
                    *command.split("\\s+".toRegex()).toTypedArray(),
                    "--forceflush",
                    "-f",
                    "m"
                ) // command = "-s"
                val process = processBuilder.start()
                Log.d("hey", "started process")
                val inputStream = process.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                withContext(Dispatchers.Main) {

                    actionListener?.onBackButtonClicked(process, inputStream, reader)
                    actionListener?.onStopRestartClicked(process)
                }

                var line: String?
                while (reader.readLine().also { line = it } != null) {

                    withContext(Dispatchers.Main){
                        if (command != "-s") {
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
                }

                val errorResponse = StringBuilder()
                while (errorReader.readLine().also { line = it } != null) {
                    errorResponse.append(line).append("\n")

                    Log.d("error Output", line.toString())
                    withContext(Dispatchers.Main) {
                        callback?.onResultReceived(line)
                    }
                }

                if (errorResponse.isNotEmpty()) {
                    Log.d("errorOutput", errorResponse.toString())
                    errorFound = true

                }
                process.waitFor()

                withContext(Dispatchers.Main) {
                    actionListener?.stopToRestart()
                }

                if (errorFound) {
                    Log.d("error", "so can't display final output")
                    withContext(Dispatchers.Main) {
                        if (!response.contains("server has terminated")) {
                            actionListener?.onSummaryButtonEnabled(false)
                        }
                        actionListener?.onStopRestartClicked(process)
                        actionListener?.toStopService(true)
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        actionListener?.onSummaryButtonEnabled(true)
                        actionListener?.onDisplayingFinalOutput()
                    }

                }

                Log.d("Process", "Completed")


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

    fun createNotificationChannel() {
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


}