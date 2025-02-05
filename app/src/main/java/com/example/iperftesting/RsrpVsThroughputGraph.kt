package com.example.iperftesting

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.iperftesting.databinding.ActivityRsrpVsThroughputGraphBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RsrpVsThroughputGraph : AppCompatActivity() {

    private lateinit var binding: ActivityRsrpVsThroughputGraphBinding

    private lateinit var lineDataSet: LineDataSet
    private lateinit var lineDataSetR: LineDataSet
    private lateinit var lineDataSetTh: LineDataSet

    private var totalTime: Int = 0
    private var choosenGraph: String = ""

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRsrpVsThroughputGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalTime = intent.getIntExtra("totalTime",0)

        getValues()
//        binding.screenRotation.setOnClickListener {
//
//            val currentOrientation = resources.configuration.orientation
//
//            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
//                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//
//            } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//            }
//        }

        binding.screenRotation.setOnClickListener {
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                // Perform actions for landscape mode, but don’t lock orientation
                Toast.makeText(this, "Switching to landscape UI", Toast.LENGTH_SHORT).show()
            } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Reset UI for portrait mode or default layout
                Toast.makeText(this, "Switching to portrait UI", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getValues(){

        choosenGraph = intent.getStringExtra("graph").toString()

        lineDataSetTh = ReadValuesFromFile.getThroughputSeries()
        lineDataSetR = ReadValuesFromFile.getRsrpSeries()

        lineChartGraph(lineGraph =binding.lineChartTh,"Throughput","Throughput vs Time",lineDataSetTh)
        lineChartGraph(lineGraph =binding.lineChartR,"RSRP","Rsrp vs Time",lineDataSetR)

        binding.saveGraph.setOnClickListener {
            captureScreen(binding.root)
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureScreen(view: View) {

//        when (view.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
//            Configuration.UI_MODE_NIGHT_YES -> Log.d("Theme", "Dark mode is active during capture")
//            Configuration.UI_MODE_NIGHT_NO -> Log.d("Theme", "Light mode is active during capture")
//        }
//        view.context.setTheme(R.style.Base_Theme_IperfTesting_Light)
        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolume = storageManager.storageVolumes[0]
        val directory = File(storageVolume.directory?.path + "/Download/IperfLogs" )

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "Rsrp vs TH $timeStamp.png"

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper()) // Create a handler to run the PixelCopy request on the main thread

        PixelCopy.request(window, Rect(0, 0, view.width, view.height), bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                // Successfully captured the screen
                val outputFile = saveBitmapAsImage(bitmap,directory.path,fileName)
                if(outputFile.exists()){
                    Toast.makeText(this, "File is saved in Download/IperfLogs",Toast.LENGTH_SHORT).show()
                    Log.d("screenshot",outputFile.path)
                }
            } else {
                // Handle error if capture fails
                Toast.makeText(this, "Screen capture failed", Toast.LENGTH_SHORT).show()
            }
        }, handler)
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
    }
    private fun saveBitmapAsImage(bitmap: Bitmap, directoryPath: String, fileName: String): File {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            Log.e("SaveFile", "Directory does not exist: $directoryPath")
            return File("")
        }

        val file = File(directory, fileName) // Combine directory and file name
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) // Save as PNG
            }
            Log.d("SaveFile", "File saved successfully: ${file.path}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SaveFile", "Error saving file: ${e.message}")
        }
        return file
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun lineChartGraph(lineGraph: LineChart, graphName: String, graphDes: String,dataSet: LineDataSet) {

        lineDataSet = dataSet
        lineDataSet.color = resources.getColor(R.color.purple, null) // Assuming you have defined purple color
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawCircles(false)
        lineDataSet.lineWidth = 1.2f

        Log.d("entires",lineDataSet.toString())
        val lineData = LineData(lineDataSet)
        lineGraph.data = lineData

        val description = Description()
        description.text = graphDes
        lineGraph.description = description
        lineGraph.axisRight.setDrawLabels(false)

        val xAxis: XAxis = lineGraph.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisMinimum = 0f
//        xAxis.axisMaximum = totalTime.toFloat()
        xAxis.axisLineWidth = 4f

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}s" // Append 's' to denote seconds
            }
        }

        val yAxis: YAxis = lineGraph.axisLeft

//        if (graphName == "RSRP") {
//            yAxis.axisMinimum = -140f // Set to accommodate negative values
//            yAxis.axisMaximum = -30f // Assuming RSRP values won't be positive
//        } else {
//            yAxis.axisMinimum = 0f
//
//            Log.d("maxThroughput",intent.getDoubleExtra("maxThroughput",0.0).toFloat().toString())
//        }
        if(graphName == "Throughput"){
            yAxis.axisMinimum = 0f
        }

        yAxis.axisLineWidth = 4f


        if (graphName == "Throughput") {
            yAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
//                    return "${value.toInt()} Mbps" // Append 'Mbps' to denote throughput
                    return if(value < 1){
                        String.format("%.2f Mbps", value)
                    }else
                        "${value.toInt()} Mbps"

                }
            }
        } else {
            yAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()} dBm" // Display RSRP values
                }
            }
        }

        if (lineDataSet.entryCount == 0) {
            Log.d("Graph", "No data to display for $graphName")
        }

        lineGraph.axisRight.isEnabled = false
        lineGraph.invalidate() // refresh
    }
}
