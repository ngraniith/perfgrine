package com.example.iperftesting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.example.iperftesting.databinding.ActivityFinalOutputBinding
import kotlinx.coroutines.delay
import java.io.File

class FinalOutput : AppCompatActivity() {

    private lateinit var binding: ActivityFinalOutputBinding
    private lateinit var doubleList: List<Double>
    private lateinit var graphIntent: Intent
    private lateinit var rvsT: Intent
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinalOutputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        graphIntent = Intent(this,GraphTR::class.java)
        rvsT = Intent(this,RsrpVsThroughputGraph::class.java)

        displaySummary()
        binding.backToMain.setOnClickListener {
            finish()
        }

        if(!intent.getBooleanExtra("saveToFile",false)){
            binding.rsrpButton.isEnabled = false
            binding.rsrpVsThroughputButton.isEnabled = false
            binding.throughputButton.isEnabled = false

        }

        binding.rsrpButton.setOnClickListener {
            graphIntent.putExtra("graph","rsrp")
            graphIntent.putExtra("totalTime",intent.getIntExtra("totalTime",0))
            startActivity(graphIntent)
        }
        binding.throughputButton.setOnClickListener {
            graphIntent.putExtra("graph","throughput")
            graphIntent.putExtra("totalTime",intent.getIntExtra("totalTime",0))
            startActivity(graphIntent)
        }

        binding.filePathButton.setOnClickListener {
            showAlertBox()
        }

        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolume = storageManager.storageVolumes[0]

        val directoryPath = File(storageVolume.directory?.path + "/Download/IperfLogs/")
        val files = directoryPath.listFiles()

        if (files != null) {
            Log.d("totalFiles",files.toString())
        }
        val logsFileName = files?.maxByOrNull {  it.lastModified() }?.name.toString()
        Log.d("directoryPath",directoryPath.toString())
        Log.d("fileName",logsFileName)
        val logFile = File(directoryPath.path + "/$logsFileName")

        binding.rsrpVsThroughputButton.setOnClickListener {

            rvsT.putExtra("totalTime",intent.getIntExtra("totalTime",0))
            ReadValuesFromFile.combinedGraph(logFile,intent.getIntExtra("totalTime",0))
            startActivity(rvsT)
        }
        if(intent.getBooleanExtra("saveToFile",false)){
            promptUserWithFileOptions(logFile)
        }

    }

    private fun promptUserWithFileOptions(logFile: File) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", logFile)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri,"text/plain")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if(intent.resolveActivity(packageManager) != null){
            startActivity(Intent.createChooser(intent,"Open Logs File with"))
        }else{
            Toast.makeText(this,"No suitable app found to open the file",Toast.LENGTH_SHORT).show()
        }

    }


    private fun showAlertBox() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logs File Path")
        builder.setMessage("You can find your logs in Internal Storage /Download/IperfLogs/")
        builder.setPositiveButton("Ok"){ dialog,_ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun displaySummary() {

        val receivedArrayList = intent.getStringArrayListExtra("bitrate graph")
        doubleList = receivedArrayList?.map { it.toDouble() }!!

        if (receivedArrayList.isNotEmpty()) {
            binding.minBitrate.append( doubleList.minOrNull().toString() + " Mbps")
            binding.maxBitrate.append(doubleList.maxOrNull().toString() + " Mbps")

            graphIntent.putExtra("maxThroughput",doubleList.maxOrNull())
            rvsT.putExtra("maxThroughput",doubleList.maxOrNull())

            val average = doubleList.average()

            binding.avgBitrate.append(String.format("%.2f Mbps", average))

            binding.transferredData.append(intent.getIntExtra("transferred data",0).toString()+ " "+intent.getStringExtra("Units of Data"))
            binding.duration.append(intent.getIntExtra("duration",0).toString()+" secs")
            binding.packets.append(intent.getIntExtra("lostPackets",0).toString()+" / "+intent.getIntExtra("totalPackets",0))
        }
    }
}