package com.example.iperftesting

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.example.iperftesting.databinding.ActivityIperfInputsBinding
import java.util.regex.Pattern

class IperfInputs : AppCompatActivity() {

    private lateinit var binding: ActivityIperfInputsBinding
    private lateinit var ipAddress: EditText
    private lateinit var radioGroup: RadioGroup

    private var checkedSystem: Int = -1
//    private var appInBackground = false
    private lateinit var choosedButton: RadioButton
    private var serverSelected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIperfInputsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ipAddress = binding.ipaddress
        radioGroup = binding.system


        binding.ipbutton.setOnClickListener{

            val iperfIntent = Intent(this,MainActivity::class.java)

            if(radioGroup.checkedRadioButtonId == R.id.client){

                iperfIntent.putExtra("systemChosen","-c")

                if(isValidIp(ipAddress.text.toString())){
                    //Toast.makeText(this,"Valid Ipaddress",Toast.LENGTH_SHORT).show()
                    iperfIntent.putExtra("ipaddress",ipAddress.text.toString())
                    startActivity(iperfIntent)

                }
                else{
                    Toast.makeText(this,"Invalid Ipaddress",Toast.LENGTH_SHORT).show()
                }
            }else{
                iperfIntent.putExtra("systemChosen","-s")
                startActivity(iperfIntent)


            }
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            choosedButton = findViewById(checkedId)
            serverSelected = choosedButton.id == R.id.server
            Log.d("serverSelected",serverSelected.toString())
            if(serverSelected){
                binding.command.text = "iperf3 -s"
            }else{
                binding.command.text = "iperf3 -c ipaddress -u -b 800M -t 3600"
            }
        }


        binding.moreOptions.setOnClickListener {
            val bottomSheet = MoreInputs()
            val bundle = Bundle().apply {
                putBoolean("selectedServer",serverSelected)
            }

            bottomSheet.arguments = bundle
            bottomSheet.show(supportFragmentManager,bottomSheet.tag)
        }

//        val callback = object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                // Do nothing to prevent going back to SplashActivity
//                moveTaskToBack(true)
//            }
//        }
//        this.onBackPressedDispatcher.addCallback(this, callback)

        val floatInAnimation = AnimationUtils.loadAnimation(this,R.anim.float_anim)
        val alphaAnimation = AnimationUtils.loadAnimation(this,R.anim.alpha_anim)
        binding.command.startAnimation(alphaAnimation)
        binding.moreText.startAnimation(floatInAnimation)

    }

    private fun isValidIp(ip: String): Boolean{
        val pattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        val matcher = pattern.matcher(ip)
        return matcher.matches()
    }

    override fun onPause() {
        super.onPause()
//        appInBackground = true
        val sharedPreferences = getSharedPreferences("getInputs",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("ipaddress",ipAddress.text.toString())
        editor.putInt("system",radioGroup.indexOfChild(findViewById(radioGroup.checkedRadioButtonId)))
        Log.d("system",radioGroup.indexOfChild(findViewById(radioGroup.checkedRadioButtonId)).toString())

        editor.apply()
    }

    override fun onResume() {
        super.onResume()
//        if (appInBackground) {
//            startActivity(Intent(this, IperfInputs::class.java))
//            finish()
//        }
        val sharedPreferences = getSharedPreferences("getInputs",Context.MODE_PRIVATE)

        checkedSystem = sharedPreferences.getInt("system",-1)
        Log.d("checkedSystem",checkedSystem.toString())
        if(checkedSystem != -1){
            val systemButton = radioGroup.getChildAt(checkedSystem).id
            radioGroup.check(systemButton)

        }
        ipAddress.setText(sharedPreferences.getString("ipaddress",""))

//        appInBackground = false


        }
    }