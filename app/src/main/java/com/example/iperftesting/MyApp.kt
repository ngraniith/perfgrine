package com.example.iperftesting

import android.app.Application

class MyApp : Application() {
    companion object {
        private lateinit var instance: MyApp
        private val myCellInfo by lazy { MyCellInfo(instance.applicationContext) }
        private val myWebSocketClient by lazy { WebSocketClient() }

        // Provides global access to MyCellInfo
        val myCellInfoInstance: MyCellInfo
            get() = myCellInfo

        val myWebSocketInstance: WebSocketClient
            get() = myWebSocketClient

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}