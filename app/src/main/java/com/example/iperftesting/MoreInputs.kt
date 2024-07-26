package com.example.iperftesting

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import com.example.iperftesting.databinding.FragmentMoreInputsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MoreInputs : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentMoreInputsBinding

    private var radioButtonSelectedIndex: Int = -1
    private var reverseCheckBox: Boolean = false
    private var spinnerSelectedItemPosition: Int = 0
    private var streamSelectedItemPosition: Int = 0
    private var customBandwidth: String = ""
    private lateinit var bandwidthItems: MutableList<String>
    private lateinit var bandwidthAdapter: ArrayAdapter<String>

    private val helpTextMap = mapOf(
        "Protocol" to "TCP ensures reliable data transmission with connection setup." +
                "UDP offers fast but unreliable data transmission without connection setup.",
        "Time" to "Time in seconds to transmit for (default 10 secs)",
        "Parallel" to "Number of parallel client streams to run. iperf3 will spawn off a separate thread for each test stream. " +
                "Using multiple streams may result in higher throughput than a single stream.",
        "Reverse" to "Reverse the direction of a test, so that the server sends data to the client",
        "Port" to "Port number option (-p or --port) allows specifying the port on which the server listens for incoming connections or the client connects to the server.",
        "Bandwidth" to "Set target bitrate to n bits/sec (default 1 Mbps for UDP, unlimited for TCP)"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("MoreInputs", "onCreateView called")
        binding = FragmentMoreInputsBinding.inflate(inflater,container,false)

        binding.numHrs.minValue = 0
        binding.numHrs.maxValue = 24

        binding.numMins.minValue = 0
        binding.numMins.maxValue = 60

        binding.numSecs.minValue = 0
        binding.numSecs.maxValue = 59

        val trail = arguments?.getBoolean("selectedServer") ?: false
        Log.d("hello",trail.toString())

        if (trail){
            disableOptions()
        }
        setItemsAtCenter()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        view.post {
            val behavior = (dialog as BottomSheetDialog).behavior
            behavior.peekHeight = view.height
        }
        binding.clear.setOnClickListener {
            clearInputs()
        }
        binding.protocolGroup.setOnCheckedChangeListener { _, checkedId ->

            val choosedButton = view.findViewById<RadioButton>(checkedId)
            binding.bandSpinner.isEnabled = choosedButton?.id != R.id.tcp_button
        }
        binding.back.setOnClickListener {
            dismiss()
        }

        binding.protHelp.setOnClickListener{
            showHelpDialog("Protocol")
        }
        binding.bandHelp.setOnClickListener{
            showHelpDialog("Bandwidth")
        }
        binding.portHelp.setOnClickListener {
            showHelpDialog("Port")
        }
        binding.timeHelp.setOnClickListener {
            showHelpDialog("Time")
        }
        binding.paraHelp.setOnClickListener {
            showHelpDialog("Parallel")
        }
        binding.reverseHelp.setOnClickListener {
            showHelpDialog("Reverse")
        }

        binding.saveChanges.setOnClickListener {

            saveInputs()
            Toast.makeText(context,"Saved the Inputs",Toast.LENGTH_SHORT).show()
            dismiss()
        }

    }

    private fun saveInputs() {
        val sharedPreferences = requireContext().getSharedPreferences("getIperfInputs",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("port value",binding.portNumber.text.toString())
        radioButtonSelectedIndex = binding.protocolGroup.indexOfChild(requireView().findViewById(binding.protocolGroup.checkedRadioButtonId))
        editor.putInt("radioButtonSelectedIndex",radioButtonSelectedIndex)

        if(radioButtonSelectedIndex !=-1){
            when (binding.protocolGroup.getChildAt(radioButtonSelectedIndex).id) {
                R.id.udp_button -> {
                    editor.putString("protocol","-u")
                }
                R.id.tcp_button -> {
                    editor.putString("protocol","")
                }
                else -> {
                    editor.putString("protocol","")
                }
            }
        }
        reverseCheckBox = binding.reverseModeCheck.isChecked
        editor.putBoolean("reverseModeIsSelected",reverseCheckBox)
        if(binding.reverseModeCheck.isChecked){
            editor.putString("mode","R")
        }else{
            editor.putString("mode","")
        }

        spinnerSelectedItemPosition = binding.bandSpinner.selectedItemPosition

        editor.putInt("selectedBandwidth",spinnerSelectedItemPosition)
        Log.d("selectedItemPosition", spinnerSelectedItemPosition.toString())

        val bandItem = binding.bandSpinner.getItemAtPosition(spinnerSelectedItemPosition).toString()
        editor.putString("bandItem",bandItem)
        Log.d("bandwidthItems",bandwidthItems.toString())

        Log.d("changed bandwidth",bandItem)

        streamSelectedItemPosition = binding.parallelStreams.selectedItemPosition
        editor.putInt("selectedStreams",streamSelectedItemPosition)

        val noStreams =binding.parallelStreams.getItemAtPosition(streamSelectedItemPosition).toString()
        editor.putString("streams",noStreams)

        editor.putInt("num hrs",binding.numHrs.value)
        editor.putInt("num mins",binding.numMins.value)
        editor.putInt("num secs",binding.numSecs.value)
        editor.apply()
    }

    private fun showHelpDialog(parameter: String) {

        val helpText = helpTextMap[parameter]
        helpText?.let {
            AlertDialog.Builder(requireContext())
                .setTitle("$parameter Help")
                .setMessage(it)
                .setPositiveButton("OK",null)
                .show()
        }
    }

    private fun setItemsAtCenter(){
        bandwidthItems = resources.getStringArray(R.array.bandwidth).toMutableList()
        bandwidthAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            bandwidthItems
        )
        bandwidthAdapter.setDropDownViewResource(R.layout.spinner_item)
        binding.bandSpinner.adapter = bandwidthAdapter

        binding.bandSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position)
                if(selectedItem == "Other"){
                    showCustomInputDialog()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        val streamsAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.streams,
            R.layout.spinner_item
        )
        streamsAdapter.setDropDownViewResource(R.layout.spinner_item)
        binding.parallelStreams.adapter = streamsAdapter
    }

    private fun showCustomInputDialog() {

        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Enter Bandwidth in MB")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                customBandwidth = editText.text.toString()+"M"
                bandwidthItems.add(customBandwidth)
                bandwidthAdapter.notifyDataSetChanged()
                Log.d("bandwidthList",bandwidthItems.size.toString())
                binding.bandSpinner.setSelection(bandwidthItems.indexOf(customBandwidth))
                Log.d("indexofCustom",bandwidthItems.indexOf(customBandwidth).toString())

                dialog.dismiss()
            }
            .setNegativeButton("Cancel"){ dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun disableOptions(){

            clearInputs()
            binding.tcpButton.isEnabled = false
            binding.udpButton.isEnabled = false
            binding.bandSpinner.isEnabled = false
            binding.numHrs.isEnabled = false
            binding.numMins.isEnabled = false
            binding.numSecs.isEnabled = false
            binding.parallelStreams.isEnabled = false
            binding.reverseModeCheck.isEnabled = false

    }
    private fun clearInputs(){

        binding.portNumber.text.clear()
        binding.protocolGroup.clearCheck()
        binding.bandSpinner.setSelection(0)
        binding.numHrs.value = 0
        binding.numMins.value = 0
        binding.numSecs.value = 0
        binding.parallelStreams.setSelection(0)
        val sharedPreferences = requireContext().getSharedPreferences("getIperfInputs",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear().apply()
    }
    override fun onResume() {
        super.onResume()

        val sharedPreferences = requireContext().getSharedPreferences("getIperfInputs",Context.MODE_PRIVATE)

        val portNum = sharedPreferences.getString("port value","")
        binding.portNumber.setText(portNum)


        radioButtonSelectedIndex = sharedPreferences.getInt("radioButtonSelectedIndex",-1)

        if(radioButtonSelectedIndex !=-1){
            val protocolButtonId = binding.protocolGroup.getChildAt(radioButtonSelectedIndex).id
            binding.protocolGroup.check(protocolButtonId)
        }

        Log.d("protocol selected",sharedPreferences.getString("protocol","").toString())


        reverseCheckBox = sharedPreferences.getBoolean("reverseModeIsSelected",false)

        if(reverseCheckBox){
            binding.reverseModeCheck.isChecked = true
        }
        Log.d("reverse mode",sharedPreferences.getString("mode","").toString())

        spinnerSelectedItemPosition = sharedPreferences.getInt("selectedBandwidth",0)
        Log.d("bandwidthItems",bandwidthItems.toString())
        Log.d("spinnerSelectedItemPos",spinnerSelectedItemPosition.toString())
        Log.d("bandwidthSizeR",bandwidthItems.size.toString())
        if(spinnerSelectedItemPosition < bandwidthItems.size){
            binding.bandSpinner.setSelection(spinnerSelectedItemPosition)
        }else {
            // If the selected position exceeds the size of bandwidthItems, add the custom bandwidth
            val bandItem = sharedPreferences.getString("bandItem","")
            if (!bandwidthItems.contains(bandItem)) {
                if (bandItem != null) {
                    bandwidthItems.add(bandItem)
                }
                bandwidthAdapter.notifyDataSetChanged()
            }
            // Update spinner selection
            binding.bandSpinner.setSelection(bandwidthItems.indexOf(bandItem))
        }
//        val bandItem = sharedPreferences.getString("bandItem","")
//        binding.bandSpinner.

        streamSelectedItemPosition = sharedPreferences.getInt("selectedStreams",0)
        binding.parallelStreams.setSelection(streamSelectedItemPosition)


        binding.numHrs.value = sharedPreferences.getInt("num hrs",0)
        binding.numMins.value = sharedPreferences.getInt("num mins",0)
        binding.numSecs.value = sharedPreferences.getInt("num secs",0)

    }

}