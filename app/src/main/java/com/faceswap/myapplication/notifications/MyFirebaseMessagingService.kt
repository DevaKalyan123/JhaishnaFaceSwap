package com.faceswap.myapplication.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // 🔥 TOKEN SAVE
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)

        Log.d("FCM", "Token saved: $token")
    }

    // 🔥 RECEIVE NOTIFICATION
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.data["title"] ?: "New Message"
        val body = remoteMessage.data["body"] ?: ""

        Log.d("FCM", "Message Received: $body")

        showLocalNotification(this, title, body)
    }
}