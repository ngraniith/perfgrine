package com.example.iperftesting

import java.io.Serializable

interface IperfCallback : Serializable {
    fun onResultReceived(result: String?)
}