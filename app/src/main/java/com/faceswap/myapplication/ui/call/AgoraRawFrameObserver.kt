package com.faceswap.myapplication.ui.call

import android.util.Log
import io.agora.base.VideoFrame
import io.agora.rtc2.video.IVideoFrameObserver

class AgoraRawFrameObserver(
    private val processor: LiveFaceFrameProcessor
) : IVideoFrameObserver {

    companion object {
        private const val TAG = "RAW_FRAME"
    }

    // =========================================================
    // CAMERA CAPTURE FRAME
    // =========================================================

    override fun onCaptureVideoFrame(
        sourceType: Int,
        videoFrame: VideoFrame
    ): Boolean {

        Log.e(
            TAG,
            "=========================================="
        )

        Log.e(
            TAG,
            "🔥🔥🔥 onCaptureVideoFrame() ENTERED 🔥🔥🔥"
        )

        Log.e(
            TAG,
            "Camera frame received"
        )

        Log.e(
            TAG,
            "Source type = $sourceType"
        )

        Log.e(
            TAG,
            "Width = ${videoFrame.buffer.width}"
        )

        Log.e(
            TAG,
            "Height = ${videoFrame.buffer.height}"
        )

        Log.e(
            TAG,
            "Rotation = ${videoFrame.rotation}"
        )

        Log.e(
            TAG,
            "Buffer type = ${videoFrame.buffer.javaClass.simpleName}"
        )

        Log.e(
            TAG,
            "Calling processFrameForAgora()..."
        )

        return try {

            val processed =
                processor.processFrameForAgora(
                    videoFrame
                )

            Log.e(
                TAG,
                "processFrameForAgora() returned = $processed"
            )

            if (processed) {

                Log.e(
                    TAG,
                    "✅ FRAME WAS PROCESSED / REPLACED"
                )

            } else {

                Log.e(
                    TAG,
                    "⚠️ FRAME WAS NOT PROCESSED"
                )
            }

            Log.e(
                TAG,
                "=========================================="
            )

            processed

        } catch (e: Exception) {

            Log.e(
                TAG,
                "❌ CALLBACK ERROR"
            )

            Log.e(
                TAG,
                "Error message = ${e.message}"
            )

            Log.e(
                TAG,
                "Callback ERROR",
                e
            )

            Log.e(
                TAG,
                "=========================================="
            )

            false
        }
    }

    // =========================================================
    // PRE-ENCODE FRAME
    // =========================================================

    override fun onPreEncodeVideoFrame(
        sourceType: Int,
        videoFrame: VideoFrame
    ): Boolean {

        Log.d(
            TAG,
            "=========================================="
        )

        Log.d(
            TAG,
            "PRE-ENCODE FRAME RECEIVED"
        )

        Log.d(
            TAG,
            "Source type = $sourceType"
        )

        Log.d(
            TAG,
            "Width = ${videoFrame.buffer.width}"
        )

        Log.d(
            TAG,
            "Height = ${videoFrame.buffer.height}"
        )

        Log.d(
            TAG,
            "Rotation = ${videoFrame.rotation}"
        )

        Log.d(
            TAG,
            "=========================================="
        )

        return false
    }

    // =========================================================
    // MEDIA PLAYER FRAME
    // =========================================================

    override fun onMediaPlayerVideoFrame(
        videoFrame: VideoFrame,
        sourceType: Int
    ): Boolean {

        Log.d(
            TAG,
            "Media player video frame received"
        )

        Log.d(
            TAG,
            "Source type = $sourceType"
        )

        return false
    }

    // =========================================================
    // REMOTE RENDER FRAME
    // =========================================================

    override fun onRenderVideoFrame(
        channelId: String,
        uid: Int,
        videoFrame: VideoFrame
    ): Boolean {

        Log.d(
            TAG,
            "Remote render video frame received"
        )

        Log.d(
            TAG,
            "Channel ID = $channelId"
        )

        Log.d(
            TAG,
            "Remote UID = $uid"
        )

        return false
    }

    // =========================================================
    // PROCESS MODE
    // =========================================================

    override fun getVideoFrameProcessMode(): Int {

        Log.d(
            TAG,
            "getVideoFrameProcessMode()"
        )

        Log.d(
            TAG,
            "Returning PROCESS_MODE_READ_WRITE"
        )

        return IVideoFrameObserver.PROCESS_MODE_READ_WRITE
    }

    // =========================================================
    // VIDEO FORMAT
    // =========================================================

    override fun getVideoFormatPreference(): Int {

        // 1 = I420
        Log.d(
            TAG,
            "getVideoFormatPreference() = I420"
        )

        return 1
    }

    // =========================================================
    // ROTATION
    // =========================================================

    override fun getRotationApplied(): Boolean {

        Log.d(
            TAG,
            "getRotationApplied() = false"
        )

        return false
    }

    // =========================================================
    // MIRROR
    // =========================================================

    override fun getMirrorApplied(): Boolean {

        Log.d(
            TAG,
            "getMirrorApplied() = false"
        )

        return false
    }

    // =========================================================
    // OBSERVED FRAME POSITION
    // =========================================================

    override fun getObservedFramePosition(): Int {

        Log.d(
            TAG,
            "getObservedFramePosition()"
        )

        Log.d(
            TAG,
            "Position = POSITION_POST_CAPTURER"
        )

        return IVideoFrameObserver.POSITION_POST_CAPTURER
    }
}