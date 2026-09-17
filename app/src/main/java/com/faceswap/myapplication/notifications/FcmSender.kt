package com.faceswap.myapplication.notifications

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject

fun sendCallNotification(
    receiverToken: String,
    callerName: String,
    callId: String
) {

    val client = OkHttpClient()

    val json = JSONObject()

    json.put("to", receiverToken)

    val data = JSONObject()
    data.put("callerName", callerName)
    data.put("callId", callId)

    json.put("data", data)

    val body = RequestBody.create(
        "application/json".toMediaTypeOrNull(),
        json.toString()
    )

    val request = Request.Builder()
        .url("https://fcm.googleapis.com/fcm/send")
        .post(body)
        .addHeader(
            "Authorization",
            "key=YOUR_SERVER_KEY"
        )
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body?.string())
        }
    })
}