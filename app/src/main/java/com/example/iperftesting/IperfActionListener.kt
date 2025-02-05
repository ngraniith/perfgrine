package com.example.iperftesting

import java.io.BufferedReader
import java.io.InputStream
import java.io.Serializable

interface IperfActionListener : Serializable{

    var errorFound: Boolean
    fun onStopRestartClicked(process: Process?)
    fun onBackButtonClicked(process: Process,inputStream: InputStream,reader: BufferedReader)
    fun displayingOutput(line: String?)
    fun getTransferredData(line: String?)
    fun getLostNoOfPackets(line: String?)
    fun onDisplayingFinalOutput()
    fun durationOfClient()
    fun durationOfServer()
    fun onGetCommand(): String

    fun onSummaryButtonEnabled(value: Boolean)
    fun stopToRestart()
    fun toStopService(value: Boolean)
    fun stopIperfTest(process: Process,inputStream: InputStream,reader: BufferedReader)
    fun getRsrp(): String
}