package com.faceswap.myapplication.ui.call

import android.Manifest
import android.app.Activity
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun VideoCallScreen(
    channelName: String,
    faceUrl: String,
    navController: NavHostController,
    callId: String
) {

    val context = LocalContext.current
    val activity = context as Activity

    val appId = "c1c13aea8ac945beb2c92e03698c571f"
    val token: String? = null

    var engine by remember { mutableStateOf<RtcEngine?>(null) }

    val localView = remember {
        RtcEngine.CreateRendererView(context)
    }

    val remoteView = remember {
        RtcEngine.CreateRendererView(context)
    }

    // ✅ CAMERA + MIC PERMISSION
    LaunchedEffect(Unit) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            0
        )
    }

    // ✅ AGORA EVENTS
    val eventHandler = remember {
        object : IRtcEngineEventHandler() {

            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                println("✅ LOCAL JOINED CHANNEL: $channel")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                println("🔥 REMOTE USER JOINED: $uid")

                engine?.setupRemoteVideo(
                    VideoCanvas(
                        remoteView,
                        VideoCanvas.RENDER_MODE_FIT,
                        uid
                    )
                )
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                println("❌ REMOTE USER LEFT")
            }
        }
    }

    // ✅ AGORA ENGINE SETUP
    DisposableEffect(channelName) {

        try {

            engine = RtcEngine.create(context, appId, eventHandler)

            engine?.apply {

                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

                enableVideo()
                enableAudio()

                setVideoEncoderConfiguration(
                    VideoEncoderConfiguration(
                        VideoEncoderConfiguration.VD_640x360,
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
                )

                localView.setZOrderMediaOverlay(true)

                setupLocalVideo(
                    VideoCanvas(
                        localView,
                        VideoCanvas.RENDER_MODE_FIT,
                        0
                    )
                )

                startPreview()

                joinChannel(
                    token,
                    channelName,
                    "",
                    0
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            engine?.stopPreview()
            engine?.leaveChannel()
            RtcEngine.destroy()
            engine = null
        }
    }

    // ✅ ONLY CURRENT CALL LISTENER
    DisposableEffect(callId) {

        val callListener = FirebaseFirestore.getInstance()
            .collection("calls")
            .document(callId)
            .addSnapshotListener { value, _ ->

                val status = value?.getString("status")

                if (status == "rejected") {

                    engine?.leaveChannel()

                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            }

        onDispose {
            callListener.remove()
        }
    }

    Box(Modifier.fillMaxSize()) {

        // 🔥 REMOTE VIDEO
        AndroidView(
            factory = {
                FrameLayout(context).apply {
                    remoteView.parent?.let {
                        (it as FrameLayout).removeView(remoteView)
                    }
                    addView(remoteView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 🔥 LOCAL VIDEO
        AndroidView(
            factory = {
                FrameLayout(context).apply {
                    localView.parent?.let {
                        (it as FrameLayout).removeView(localView)
                    }
                    addView(localView)
                }
            },
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        // 🔥 END CALL
        Button(
            onClick = {

                FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(callId)
                    .update("status", "rejected")

                engine?.leaveChannel()

                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("End Call")
        }
    }
}