package com.faceswap.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import com.faceswap.myapplication.navigation.AppNavGraph
import com.faceswap.myapplication.ui.theme.JhaishnaFaceSwapTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openIncoming =
            intent.getBooleanExtra("openIncoming", false)

        val callId =
            intent.getStringExtra("callId")

        // ✅ CAMERA + MIC
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            1
        )

        // ✅ ANDROID 13+ NOTIFICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                2
            )
        }

        // ✅ FCM TOKEN SAVE
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                val uid =
                    FirebaseAuth.getInstance().currentUser?.uid

                uid?.let {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(it)
                        .update("fcmToken", token)
                }

                println("🔥 FCM TOKEN: $token")
            }

        setContent {
            JhaishnaFaceSwapTheme {
                MyApp(
                    openIncoming = openIncoming,
                    callId = callId
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid

        uid?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .update("online", true)
        }
    }

    override fun onPause() {
        super.onPause()

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid

        uid?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .update(
                    mapOf(
                        "online" to false,
                        "lastSeen" to System.currentTimeMillis()
                    )
                )
        }
    }
}

@Composable
fun MyApp(
    openIncoming: Boolean = false,
    callId: String? = null
) {

    val navController = rememberNavController()

    val uid =
        FirebaseAuth.getInstance().currentUser?.uid

    // ✅ APP CLOSED → NOTIFICATION CLICK
    LaunchedEffect(openIncoming) {
        if (openIncoming && callId != null) {
            navController.navigate("incoming_call/$callId")
        }
    }

    // ✅ APP OPEN → FIRESTORE LISTENER
    LaunchedEffect(uid) {

        uid?.let { currentUid ->

            FirebaseFirestore.getInstance()
                .collection("calls")
                .whereEqualTo("receiverId", currentUid)
                .whereEqualTo("status", "calling")
                .addSnapshotListener { value, _ ->

                    val callDoc =
                        value?.documents?.firstOrNull()

                    if (callDoc != null) {

                        val incomingCallId =
                            callDoc.id

                        navController.navigate(
                            "incoming_call/$incomingCallId"
                        )
                    }
                }
        }
    }

    AppNavGraph(navController = navController)
}