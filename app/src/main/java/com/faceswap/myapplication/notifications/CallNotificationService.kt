package com.faceswap.myapplication.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.faceswap.myapplication.MainActivity
import com.faceswap.myapplication.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CallNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val callerName =
            remoteMessage.data["callerName"] ?: "Incoming Call"

        val callId =
            remoteMessage.data["callId"] ?: ""

        showIncomingCallNotification(callerName, callId)
    }

    private fun showIncomingCallNotification(
        callerName: String,
        callId: String
    ) {

        val channelId = "incoming_calls"

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("callId", callId)
            putExtra("openIncoming", true)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_RINGTONE
            )

        val manager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.setSound(
                ringtoneUri,
                null
            )

            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("📞 $callerName")
                .setContentText("Incoming video call...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setSound(ringtoneUri)
                .build()

        manager.notify(999, notification)
    }
}