package com.faceswap.myapplication.ui.call

data class CallData(
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val channelName: String = "",
    val status: String = "", // calling / accepted / rejected
    val timestamp: Long = 0
)