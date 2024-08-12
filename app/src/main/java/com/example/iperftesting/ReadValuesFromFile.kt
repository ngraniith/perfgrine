package com.example.iperftesting

import android.util.Log
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.File

object ReadValuesFromFile {

    private lateinit var lineDataSet: LineDataSet
    private lateinit var lineDataSetR: LineDataSet
    private lateinit var lineDataSetTh: LineDataSet
    var units: String = "Mbps"
    fun getValuesThRS(filePath: File, chosenGraph: String): LineDataSet {

        if(filePath.exists()){

            Log.d("filePath",filePath.toString())
            val lines = filePath.readLines()
            var index = 1f

            lineDataSet = LineDataSet(ArrayList<Entry>(), chosenGraph)

            for (line in lines) {
                if (line.isNotBlank()) {

                    Log.d("fileLine:",line)
                    val metrics = extractThroughput(line)
                    val throughputValue = metrics?.throughput
                    val rsrpValue = metrics?.rsrp

                    Log.d("graphValues", "$throughputValue $rsrpValue")

                    if(throughputValue!= null && rsrpValue!= null){

                        Log.d("index",index.toString())


                        if (chosenGraph == "rsrp") {

                            lineDataSet.addEntry(Entry(index, rsrpValue))
                            Log.d("lineDataSet",lineDataSet.toString())
                        }
                        else if (chosenGraph == "throughput") {

                            if(throughputValue < 1){
                                units = "Kbps"
                                lineDataSet.addEntry(Entry(index, (throughputValue * 1000)))
                            }else{
                                lineDataSet.addEntry(Entry(index, throughputValue))
                            }

                            Log.d("lineDataSet",lineDataSet.toString())
                        }

                        index++
                    }
                }
            }

        }
        return lineDataSet
    }

    fun combinedGraph(filePath: File, totalTime: Int){

        lineDataSetR = LineDataSet(ArrayList<Entry>(), "RSRP")
        lineDataSetTh = LineDataSet(ArrayList<Entry>(), "Throughput")

        Log.d("TimeGraph",totalTime.toString())

        if(filePath.exists()){

            Log.d("filePath",filePath.toString())
            val lines = filePath.readLines()
            var index = 1f

            for (line in lines) {
                if (line.isNotBlank()) {

                    Log.d("fileLine:",line)
                    val metrics = extractThroughput(line)
                    val throughputValue = metrics?.throughput
                    val rsrpValue = metrics?.rsrp

                    Log.d("graphValues", "$throughputValue $rsrpValue")

                    if(throughputValue!= null && rsrpValue!= null){

                        Log.d("index",index.toString())

                        if(throughputValue < 1) {
                            units = "Kbps"
                            lineDataSetTh.addEntry(Entry(index, (throughputValue * 1000)))
                        }
                        else
                            lineDataSetTh.addEntry(Entry(index,throughputValue))


                        lineDataSetR.addEntry(Entry(index,rsrpValue))

                        index++
                    }
                }
            }

        }
    }
    fun getThroughputSeries(): LineDataSet {

        return lineDataSetTh
    }

    fun getRsrpSeries(): LineDataSet {

        return lineDataSetR
    }

    data class NetworkMetrics(val throughput: Float?, val rsrp: Float?)
    private fun extractThroughput(line: String): NetworkMetrics? {
        return try {
            val parts = line.split("\\s+".toRegex())
            Log.d("parts",parts.toString())
            val throughputValue = parts[1].toFloat()
            var rsrpValue = 0f
            val lastIndex = parts.lastIndex
            if(parts[lastIndex].isNotBlank()){
                rsrpValue = parts[lastIndex].toFloat()
            }

            Log.d("iperfValues","$throughputValue $rsrpValue")

            return NetworkMetrics(throughputValue,rsrpValue)

        } catch (e: Exception) {
            Log.e("GraphTR", "Error parsing line: $line", e)
            null
        }
    }
}
