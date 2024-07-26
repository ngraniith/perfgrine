package com.example.iperftesting

import android.util.Log

object SimChoice {

    private var choice: String = ""
    fun setSimChoice(choice: String){

        this.choice = choice
        Log.d("SimChoice",this.choice)

    }

    fun getSimChoice() : String{
        Log.d("getSim",this.choice)
        return this.choice
    }
}