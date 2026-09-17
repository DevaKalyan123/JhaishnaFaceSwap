package com.faceswap.myapplication.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.faceswap.myapplication.BuildConfig // ✅ ONLY THIS

object Translator {

    fun translate(
        text: String,
        targetLang: String,
        onResult: (String) -> Unit
    ) {

        val client = OkHttpClient()

        val prompt = "Translate this text to $targetLang language: $text"

        val json = JSONObject()
        json.put("model", "gpt-4o-mini")

        val messagesArray = JSONArray()
        val msgObject = JSONObject()
        msgObject.put("role", "user")
        msgObject.put("content", prompt)

        messagesArray.put(msgObject)
        json.put("messages", messagesArray)

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader(
                "Authorization",
                "Bearer ${BuildConfig.OPENAI_API_KEY}"
            )
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {

                Log.e("API_ERROR", e.message.toString())

                Handler(Looper.getMainLooper()).post {
                    onResult(text) // fallback
                }
            }

            override fun onResponse(call: Call, response: Response) {

                try {
                    val res = response.body?.string()
                    Log.d("API_RESPONSE", res ?: "null")

                    val output = JSONObject(res!!)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    Handler(Looper.getMainLooper()).post {
                        onResult(output.trim())
                    }

                } catch (e: Exception) {

                    Log.e("PARSE_ERROR", e.message.toString())

                    Handler(Looper.getMainLooper()).post {
                        onResult(text)
                    }
                }
            }
        })
    }
}