package com.faceswap.myapplication.ui.call

import android.content.Context
import android.view.SurfaceView
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class AgoraManager(private val context: Context) {

    private val appId = "c1c13aea8ac945beb2c92e03698c571f"
    private var rtcEngine: RtcEngine? = null

    fun initialize(onRemoteUserJoined: (Int) -> Unit) {

        rtcEngine = RtcEngine.create(
            context,
            appId,
            object : IRtcEngineEventHandler() {

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    onRemoteUserJoined(uid)
                }
            }
        )

        rtcEngine?.apply {
            enableVideo()

            // 🔥 IMPORTANT FIX
            startPreview()

            setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        }
    }

    fun joinChannel(channelName: String) {
        rtcEngine?.joinChannel(null, channelName, "", 0)
    }

    fun setupLocalVideo(surfaceView: SurfaceView) {
        rtcEngine?.setupLocalVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )
    }

    fun setupRemoteVideo(surfaceView: SurfaceView, uid: Int) {
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }

    fun destroy() {
        RtcEngine.destroy()
        rtcEngine = null
    }
}