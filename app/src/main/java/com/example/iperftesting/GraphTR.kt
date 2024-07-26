package com.example.iperftesting

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.iperftesting.databinding.ActivityGraphTrBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.File

class GraphTR : AppCompatActivity() {

    private lateinit var binding: ActivityGraphTrBinding
    private var logsFileName: String = ""
    private lateinit var lineDataSet: LineDataSet

    private var totalTime: Int = 0
    private var choosenGraph: String = ""

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGraphTrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        choosenGraph = intent.getStringExtra("graph").toString()
        totalTime = intent.getIntExtra("totalTime",0)

        lineDataSet = LineDataSet(ArrayList<Entry>(), choosenGraph)

        getValues()
        binding.screenRotation.setOnClickListener {

            val currentOrientation = resources.configuration.orientation

            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

        }
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getValues(){

        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolume = storageManager.storageVolumes[0]

        val directoryPath = File(storageVolume.directory?.path + "/Download/IperfLogs/")
        val files = directoryPath.listFiles()

        logsFileName = files?.maxByOrNull {  it.lastModified() }?.name.toString()
        Log.d("directoryPath",directoryPath.toString())

        val filePath = File(directoryPath.path + "/$logsFileName")

        lineDataSet = ReadValuesFromFile.getValuesThRS(filePath,choosenGraph)


        Log.d("lineData",lineDataSet.toString())

        if(lineDataSet.entryCount != 0){

            if(choosenGraph == "rsrp"){
                lineChartGraph(lineGraph =binding.lineChart,"RSRP", "RSRP vs Time")
            }else if (choosenGraph == "throughput"){
                lineChartGraph(lineGraph =binding.lineChart,"Throughput", "Throughput vs Time")
            }
        }
        else{
            Toast.makeText(this,"Iperf Logs file doesn't exists",Toast.LENGTH_SHORT).show()
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun lineChartGraph(lineGraph: LineChart, graphName: String, graphDes: String) {

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
//        }
        yAxis.axisLineWidth = 4f

        if (graphName == "Throughput") {
            yAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()} Mbps" // Append 'Mbps' to denote throughput
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
