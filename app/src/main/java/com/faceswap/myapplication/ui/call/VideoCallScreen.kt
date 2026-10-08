
package com.faceswap.myapplication.ui.call

import android.Manifest
import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.app.ActivityCompat

import androidx.navigation.NavHostController

import com.faceswap.myapplication.face.ArcFaceEmbedder
import com.faceswap.myapplication.face.ArcFaceTestRunner
import com.faceswap.myapplication.face.FaceDetector
import com.faceswap.myapplication.face.FaceSwapper

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

import kotlinx.coroutines.CancellationException


// =============================================================
// VIDEO CALL SCREEN
// =============================================================

@Composable
fun VideoCallScreen(
    callId: String,
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val activity =
        context as? Activity

    // =========================================================
    // AGORA CONFIG
    // =========================================================

    val appId =
        "c1c13aea8ac945beb2c92e03698c571f"

    val token: String? =
        null

    // =========================================================
    // FACE DETECTOR
    // =========================================================

    val faceDetector =
        remember {

            FaceDetector(
                context
            )
        }

    // =========================================================
    // ARC FACE EMBEDDER
    // =========================================================

    val arcFaceEmbedder =
        remember {

            ArcFaceEmbedder()
        }

    // =========================================================
    // FACE SWAPPER
    // =========================================================

    val faceSwapper =
        remember {

            FaceSwapper()
        }

    // =========================================================
    // LIVE FRAME PROCESSOR
    // =========================================================

    val liveFrameProcessor =
        remember {

            LiveFaceFrameProcessor(
                faceDetector,
                faceSwapper
            )
        }

    // =========================================================
    // RAW FRAME OBSERVER
    // =========================================================

    val rawFrameObserver =
        remember {

            AgoraRawFrameObserver(
                liveFrameProcessor
            )
        }

    // =========================================================
    // MODEL STATUS
    // =========================================================

    var faceModelLoaded by remember {

        mutableStateOf(
            false
        )
    }

    var arcFaceReady by remember {

        mutableStateOf(
            false
        )
    }

    var faceSwapperReady by remember {

        mutableStateOf(
            false
        )
    }

    // =========================================================
    // FACE DETECTION STATUS
    // =========================================================

    var faceDetectionRunning by remember {

        mutableStateOf(
            false
        )
    }

    var detectedFaceCount by remember {

        mutableIntStateOf(
            0
        )
    }

    // =========================================================
    // ARC FACE STATUS
    // =========================================================

    var arcFaceTesting by remember {

        mutableStateOf(
            false
        )
    }

    var arcFaceSuccess by remember {

        mutableStateOf(
            false
        )
    }

    var selectedFaceEmbedding by remember {

        mutableStateOf<FloatArray?>(null)
    }

    // =========================================================
    // FACE SWAPPER STATUS
    // =========================================================

    var faceSwapperLoading by remember {

        mutableStateOf(
            false
        )
    }

    // =========================================================
    // CALL DATA
    // =========================================================

    var channelName by remember {

        mutableStateOf(
            ""
        )
    }

    var faceUrl by remember {

        mutableStateOf(
            ""
        )
    }

    var callerName by remember {

        mutableStateOf(
            ""
        )
    }

    // =========================================================
    // CALL ROLE
    // =========================================================

    var isCaller by remember {

        mutableStateOf(
            false
        )
    }

    var isLoadingCall by remember {

        mutableStateOf(
            true
        )
    }

    // =========================================================
    // AGORA ENGINE
    // =========================================================

    var engine by remember {

        mutableStateOf<RtcEngine?>(
            null
        )
    }

    // =========================================================
    // VIDEO VIEWS
    // =========================================================

    val localView =
        remember {

            RtcEngine
                .CreateRendererView(
                    context
                )
        }

    val remoteView =
        remember {

            RtcEngine
                .CreateRendererView(
                    context
                )
        }

    // =========================================================
    // RELEASE LIVE PROCESSOR
    // =========================================================

    DisposableEffect(
        liveFrameProcessor
    ) {

        onDispose {

            Log.d(
                "LIVE_PROCESSOR",
                "Releasing live frame processor..."
            )

            liveFrameProcessor.release()
        }
    }

    // =========================================================
    // LOAD FACE DETECTOR
    // =========================================================

    LaunchedEffect(
        faceDetector
    ) {

        Log.d(
            "FACE_DETECTOR",
            "=========================================="
        )

        Log.d(
            "FACE_DETECTOR",
            "Starting det_10g.onnx..."
        )

        val loaded =
            try {

                faceDetector.loadModel()

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                Log.e(
                    "FACE_DETECTOR",
                    "Model loading exception",
                    e
                )

                false
            }

        faceModelLoaded =
            loaded

        if (
            loaded
        ) {

            Log.d(
                "FACE_DETECTOR",
                "SUCCESS: det_10g.onnx loaded successfully"
            )

            // ---------------------------------------------
            // ENABLE LIVE FACE DETECTION
            // ---------------------------------------------

            liveFrameProcessor
                .setModelReady(
                    true
                )

            Log.d(
                "LIVE_FACE_DETECT",
                "Live face detector enabled for live face swap"
            )

        } else {

            Log.e(
                "FACE_DETECTOR",
                "FAILED: det_10g.onnx"
            )

            liveFrameProcessor
                .setModelReady(
                    false
                )
        }
    }

    // =========================================================
    // LOAD ARC FACE
    // =========================================================

    LaunchedEffect(
        arcFaceEmbedder
    ) {

        Log.d(
            "ARC_FACE",
            "=========================================="
        )

        Log.d(
            "ARC_FACE",
            "STARTING ARC FACE MODEL LOADING"
        )

        val loaded =
            try {

                arcFaceEmbedder
                    .loadModel(
                        context
                    )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                Log.e(
                    "ARC_FACE",
                    "ArcFace model loading exception",
                    e
                )

                false
            }

        arcFaceReady =
            loaded

        if (
            loaded
        ) {

            Log.d(
                "ARC_FACE",
                "SUCCESS: w600k_r50.onnx loaded successfully"
            )

        } else {

            Log.e(
                "ARC_FACE",
                "FAILED: w600k_r50.onnx"
            )
        }

        Log.d(
            "ARC_FACE",
            "ArcFace model ready = $loaded"
        )

        Log.d(
            "ARC_FACE",
            "=========================================="
        )
    }

    // =========================================================
    // LOAD FACE SWAPPER
    // =========================================================

    LaunchedEffect(
        faceSwapper
    ) {

        Log.d(
            "FACE_SWAPPER",
            "=========================================="
        )

        Log.d(
            "FACE_SWAPPER",
            "STARTING INSWAPPER MODEL LOADING"
        )

        faceSwapperLoading =
            true

        faceSwapperReady =
            false

        val loaded =
            try {

                faceSwapper
                    .loadModel(
                        context
                    )

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                Log.e(
                    "FACE_SWAPPER",
                    "FaceSwapper loading exception",
                    e
                )

                false
            }

        faceSwapperLoading =
            false

        faceSwapperReady =
            loaded

        if (
            loaded
        ) {

            Log.d(
                "FACE_SWAPPER",
                "=========================================="
            )

            Log.d(
                "FACE_SWAPPER",
                "SUCCESS: InSwapper is READY"
            )

            Log.d(
                "FACE_SWAPPER",
                "inswapper_128.onnx = READY"
            )

            Log.d(
                "FACE_SWAPPER",
                "emap.bin = READY"
            )

            Log.d(
                "FACE_SWAPPER",
                "=========================================="
            )

        } else {

            Log.e(
                "FACE_SWAPPER",
                "=========================================="
            )

            Log.e(
                "FACE_SWAPPER",
                "FAILED: InSwapper model loading"
            )

            Log.e(
                "FACE_SWAPPER",
                "=========================================="
            )
        }
    }

    // =========================================================
    // LOAD CALL DOCUMENT
    // =========================================================

    LaunchedEffect(
        callId
    ) {

        Log.d(
            "FACE_SWAP",
            "=========================================="
        )

        Log.d(
            "FACE_SWAP",
            "Loading call document"
        )

        Log.d(
            "FACE_SWAP",
            "Call ID = $callId"
        )

        FirebaseFirestore
            .getInstance()
            .collection("calls")
            .document(callId)
            .get()
            .addOnSuccessListener { document ->

                if (
                    document.exists()
                ) {

                    channelName =
                        document.getString(
                            "channelName"
                        ) ?: ""

                    faceUrl =
                        document.getString(
                            "faceUrl"
                        ) ?: ""

                    callerName =
                        document.getString(
                            "callerName"
                        ) ?: ""

                    // ---------------------------------------------
                    // DETERMINE CALL ROLE
                    // Only the caller is allowed to run live face swap.
                    // The receiver always publishes the original camera.
                    // ---------------------------------------------

                    val callCallerId =
                        document.getString(
                            "callerId"
                        ) ?: ""

                    val currentUserId =
                        FirebaseAuth
                            .getInstance()
                            .currentUser
                            ?.uid
                            ?: ""

                    isCaller =
                        callCallerId.isNotBlank() &&
                                currentUserId.isNotBlank() &&
                                callCallerId == currentUserId

                    Log.d(
                        "CALL_ROLE",
                        "=========================================="
                    )
                    Log.d(
                        "CALL_ROLE",
                        "Call callerId = $callCallerId"
                    )
                    Log.d(
                        "CALL_ROLE",
                        "Current userId = $currentUserId"
                    )
                    Log.d(
                        "CALL_ROLE",
                        "IS CALLER = $isCaller"
                    )

                    if (isCaller) {
                        Log.d(
                            "CALL_ROLE",
                            "🔥 THIS DEVICE IS CALLER"
                        )
                        Log.d(
                            "CALL_ROLE",
                            "🔥 LIVE FACE SWAP WILL RUN"
                        )
                    } else {
                        Log.d(
                            "CALL_ROLE",
                            "THIS DEVICE IS RECEIVER"
                        )
                        Log.d(
                            "CALL_ROLE",
                            "LIVE FACE SWAP WILL NOT RUN"
                        )
                    }

                    Log.d(
                        "CALL_ROLE",
                        "=========================================="
                    )

                    Log.d(
                        "FACE_SWAP",
                        "Call document loaded successfully"
                    )

                    Log.d(
                        "FACE_SWAP",
                        "Channel = $channelName"
                    )

                    Log.d(
                        "FACE_SWAP",
                        "Face URL available = " +
                                faceUrl.isNotBlank()
                    )

                    Log.d(
                        "FACE_SWAP",
                        "Caller = $callerName"
                    )

                } else {

                    Log.e(
                        "FACE_SWAP",
                        "Call document does not exist"
                    )
                }

                isLoadingCall =
                    false
            }
            .addOnFailureListener { error ->

                Log.e(
                    "FACE_SWAP",
                    "Failed to load call document",
                    error
                )

                isLoadingCall =
                    false
            }
    }

    // =========================================================
    // DETECT SELECTED FACE
    // =========================================================

    LaunchedEffect(
        faceUrl,
        faceModelLoaded,
        isCaller
    ) {

        if (!isCaller) {
            Log.d(
                "FACE_DETECTOR",
                "Receiver device: selected-face detection skipped"
            )
            return@LaunchedEffect
        }

        if (
            !faceModelLoaded
        ) {

            return@LaunchedEffect
        }

        if (
            faceUrl.isBlank()
        ) {

            Log.d(
                "FACE_DETECTOR",
                "No selected face URL"
            )

            return@LaunchedEffect
        }

        faceDetectionRunning =
            true

        detectedFaceCount =
            0

        Log.d(
            "FACE_DETECTOR",
            "=========================================="
        )

        Log.d(
            "FACE_DETECTOR",
            "STARTING SELECTED FACE DETECTION"
        )

        try {

            val detections =
                faceDetector
                    .detectFromUrl(
                        faceUrl
                    )

            detectedFaceCount =
                detections.size

            if (
                detections.isEmpty()
            ) {

                Log.e(
                    "FACE_DETECTOR",
                    "NO FACE DETECTED"
                )

            } else {

                Log.d(
                    "FACE_DETECTOR",
                    "FACE DETECTED = " +
                            detections.size
                )

                detections
                    .forEachIndexed {
                            index,
                            face ->

                        Log.d(
                            "FACE_DETECTOR",
                            "FACE #$index"
                        )

                        Log.d(
                            "FACE_DETECTOR",
                            "SCORE = " +
                                    face.score
                        )

                        Log.d(
                            "FACE_DETECTOR",
                            "BOX = " +
                                    face.box
                        )

                        Log.d(
                            "FACE_DETECTOR",
                            "LANDMARKS = " +
                                    face.landmarks
                        )
                    }
            }

        } catch (
            e: CancellationException
        ) {

            Log.d(
                "FACE_DETECTOR",
                "Face detection coroutine cancelled"
            )

            throw e

        } catch (
            e: Exception
        ) {

            Log.e(
                "FACE_DETECTOR",
                "Detection exception",
                e
            )

        } finally {

            faceDetectionRunning =
                false
        }
    }

    // =========================================================
    // ARC FACE EMBEDDING TEST
    // =========================================================

    LaunchedEffect(
        faceUrl,
        faceModelLoaded,
        arcFaceReady,
        isCaller
    ) {

        if (!isCaller) {
            selectedFaceEmbedding = null
            Log.d(
                "ARC_FACE_TEST",
                "Receiver device: ArcFace source embedding skipped"
            )
            return@LaunchedEffect
        }

        if (
            !faceModelLoaded
        ) {

            Log.d(
                "ARC_FACE_TEST",
                "Waiting for FaceDetector model..."
            )

            return@LaunchedEffect
        }

        if (
            !arcFaceReady
        ) {

            Log.d(
                "ARC_FACE_TEST",
                "Waiting for ArcFace model..."
            )

            return@LaunchedEffect
        }

        if (
            faceUrl.isBlank()
        ) {

            Log.d(
                "ARC_FACE_TEST",
                "Waiting for selected face URL..."
            )

            return@LaunchedEffect
        }

        Log.d(
            "ARC_FACE_TEST",
            "=========================================="
        )

        Log.d(
            "ARC_FACE_TEST",
            "STARTING SELECTED FACE EMBEDDING TEST"
        )

        Log.d(
            "ARC_FACE_TEST",
            "Face URL available = true"
        )

        arcFaceTesting =
            true

        arcFaceSuccess =
            false

        selectedFaceEmbedding =
            null

        try {

            val embedding =
                ArcFaceTestRunner.test(
                    imageUrl = faceUrl,
                    faceDetector = faceDetector,
                    arcFaceEmbedder = arcFaceEmbedder
                )

            selectedFaceEmbedding =
                embedding

            if (
                embedding != null &&
                embedding.size == 512
            ) {

                arcFaceSuccess =
                    true

                Log.d(
                    "ARC_FACE_TEST",
                    "=========================================="
                )

                Log.d(
                    "ARC_FACE_TEST",
                    "SUCCESS: 512D EMBEDDING GENERATED"
                )

                Log.d(
                    "ARC_FACE_TEST",
                    "Embedding size = " +
                            embedding.size
                )

                Log.d(
                    "ARC_FACE_TEST",
                    "Selected face is ready for swap pipeline"
                )

                Log.d(
                    "ARC_FACE_TEST",
                    "=========================================="
                )

            } else {

                Log.e(
                    "ARC_FACE_TEST",
                    "FAILED: 512D embedding not generated"
                )
            }

        } catch (
            e: CancellationException
        ) {

            Log.d(
                "ARC_FACE_TEST",
                "ArcFace test cancelled"
            )

            throw e

        } catch (
            e: Exception
        ) {

            Log.e(
                "ARC_FACE_TEST",
                "ArcFace test exception",
                e
            )

        } finally {

            arcFaceTesting =
                false
        }
    }


    // =========================================================
    // LIVE FACE SWAP STATE
    // =========================================================

    LaunchedEffect(
        faceModelLoaded,
        faceSwapperReady,
        selectedFaceEmbedding,
        isCaller
    ) {

        Log.d(
            "LIVE_FACE_SWAP",
            "=========================================="
        )

        if (!isCaller) {

            // Receiver publishes ORIGINAL camera only.
            liveFrameProcessor.setSourceEmbedding(null)
            liveFrameProcessor.setFaceSwapEnabled(false)

            Log.d(
                "LIVE_FACE_SWAP",
                "RECEIVER DEVICE"
            )
            Log.d(
                "LIVE_FACE_SWAP",
                "🔥 LIVE FACE SWAP DISABLED"
            )
            Log.d(
                "LIVE_FACE_SWAP",
                "Receiver will publish ORIGINAL CAMERA"
            )
            Log.d(
                "LIVE_FACE_SWAP",
                "=========================================="
            )

            return@LaunchedEffect
        }

        // Caller only.
        liveFrameProcessor.setSourceEmbedding(
            selectedFaceEmbedding
        )

        val enabled =
            faceModelLoaded &&
                    faceSwapperReady &&
                    selectedFaceEmbedding != null &&
                    selectedFaceEmbedding!!.size == 512

        liveFrameProcessor.setFaceSwapEnabled(
            enabled
        )

        Log.d(
            "LIVE_FACE_SWAP",
            "CALLER DEVICE"
        )
        Log.d(
            "LIVE_FACE_SWAP",
            "Detector = $faceModelLoaded"
        )
        Log.d(
            "LIVE_FACE_SWAP",
            "InSwapper = $faceSwapperReady"
        )
        Log.d(
            "LIVE_FACE_SWAP",
            "Embedding = ${selectedFaceEmbedding != null}"
        )
        Log.d(
            "LIVE_FACE_SWAP",
            "Embedding size = ${selectedFaceEmbedding?.size ?: 0}"
        )
        Log.d(
            "LIVE_FACE_SWAP",
            "🔥 LIVE SWAP ENABLED = $enabled"
        )
        Log.d(
            "LIVE_FACE_SWAP",
            "=========================================="
        )
    }

    // =========================================================
    // CAMERA PERMISSION
    // =========================================================

    LaunchedEffect(
        activity
    ) {

        if (
            activity != null
        ) {

            ActivityCompat.requestPermissions(

                activity,

                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),

                100
            )
        }
    }

    // =========================================================
    // AGORA EVENT HANDLER
    // =========================================================

    val eventHandler =
        remember {

            object :
                IRtcEngineEventHandler() {

                // =============================================
                // LOCAL JOIN
                // =============================================

                override fun onJoinChannelSuccess(
                    channel: String?,
                    uid: Int,
                    elapsed: Int
                ) {

                    Log.d(
                        "AGORA",
                        "=========================================="
                    )

                    Log.d(
                        "AGORA",
                        "LOCAL USER JOINED"
                    )

                    Log.d(
                        "AGORA",
                        "Channel = $channel"
                    )

                    Log.d(
                        "AGORA",
                        "UID = $uid"
                    )

                    Log.d(
                        "AGORA",
                        "=========================================="
                    )
                }

                // =============================================
                // REMOTE USER JOIN
                // =============================================

                override fun onUserJoined(
                    uid: Int,
                    elapsed: Int
                ) {

                    Log.d(
                        "AGORA",
                        "REMOTE USER JOINED: $uid"
                    )

                    val currentEngine =
                        engine

                    if (
                        currentEngine != null
                    ) {

                        try {

                            remoteView.layoutParams =
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )

                            currentEngine.setupRemoteVideo(

                                VideoCanvas(
                                    remoteView,
                                    VideoCanvas.RENDER_MODE_HIDDEN,
                                    uid
                                )
                            )

                            Log.d(
                                "AGORA",
                                "REMOTE VIDEO ATTACHED: uid=$uid"
                            )

                        } catch (
                            e: Exception
                        ) {

                            Log.e(
                                "AGORA",
                                "Remote video setup failed",
                                e
                            )
                        }
                    }
                }

                override fun onFirstRemoteVideoDecoded(
                    uid: Int,
                    width: Int,
                    height: Int,
                    elapsed: Int
                ) {

                    Log.d(
                        "AGORA",
                        "FIRST REMOTE VIDEO: uid=$uid ${width}x$height"
                    )

                    val currentEngine =
                        engine

                    if (
                        currentEngine != null
                    ) {

                        try {

                            remoteView.layoutParams =
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )

                            currentEngine.setupRemoteVideo(

                                VideoCanvas(
                                    remoteView,
                                    VideoCanvas.RENDER_MODE_HIDDEN,
                                    uid
                                )
                            )

                            Log.d(
                                "AGORA",
                                "FIRST REMOTE VIDEO ATTACHED"
                            )

                        } catch (
                            e: Exception
                        ) {

                            Log.e(
                                "AGORA",
                                "First remote video setup failed",
                                e
                            )
                        }
                    }
                }

                // =============================================
                // REMOTE USER LEAVE
                // =============================================

                override fun onUserOffline(
                    uid: Int,
                    reason: Int
                ) {

                    Log.d(
                        "AGORA",
                        "REMOTE USER LEFT: $uid"
                    )

                    Log.d(
                        "AGORA",
                        "Reason = $reason"
                    )
                }
            }
        }

    // =========================================================
    // AGORA SETUP
    // =========================================================

    DisposableEffect(
        channelName
    ) {

        if (
            channelName.isBlank()
        ) {

            onDispose {}

        } else {

            var localEngine:
                    RtcEngine? =
                null

            try {

                Log.d(
                    "AGORA",
                    "=========================================="
                )

                Log.d(
                    "AGORA",
                    "CREATING AGORA ENGINE"
                )

                // =============================================
                // CREATE ENGINE
                // =============================================

                val newEngine =
                    RtcEngine.create(
                        context,
                        appId,
                        eventHandler
                    )

                localEngine =
                    newEngine

                engine =
                    newEngine

                // =============================================
                // CHANNEL PROFILE
                // =============================================

                newEngine
                    .setChannelProfile(
                        Constants
                            .CHANNEL_PROFILE_COMMUNICATION
                    )

                // =============================================
                // ROLE
                // =============================================

                newEngine
                    .setClientRole(
                        Constants
                            .CLIENT_ROLE_BROADCASTER
                    )

                // =============================================
                // ENABLE VIDEO
                // =============================================

                newEngine
                    .enableVideo()

                // =============================================
                // RAW VIDEO OBSERVER
                // =============================================

                Log.d(
                    "RAW_FRAME",
                    "Registering video frame observer..."
                )

                val observerResult =
                    newEngine
                        .registerVideoFrameObserver(
                            rawFrameObserver
                        )

                Log.d(
                    "RAW_FRAME",
                    "Video frame observer registered = " +
                            observerResult
                )

                // =============================================
                // ENABLE AUDIO
                // =============================================

                newEngine
                    .enableAudio()

                // =============================================
                // VIDEO CONFIG
                // =============================================

                newEngine
                    .setVideoEncoderConfiguration(

                        VideoEncoderConfiguration(

                            VideoEncoderConfiguration
                                .VD_640x360,

                            VideoEncoderConfiguration
                                .FRAME_RATE
                                .FRAME_RATE_FPS_15,

                            VideoEncoderConfiguration
                                .STANDARD_BITRATE,

                            VideoEncoderConfiguration
                                .ORIENTATION_MODE
                                .ORIENTATION_MODE_ADAPTIVE
                        )
                    )

                // =============================================
                // LOCAL VIEW
                // =============================================

                localView
                    .setZOrderMediaOverlay(
                        true
                    )

                localView.layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )

                remoteView.setZOrderMediaOverlay(false)

                newEngine
                    .setupLocalVideo(

                        VideoCanvas(
                            localView,
                            VideoCanvas
                                .RENDER_MODE_FIT,
                            0
                        )
                    )

                // =============================================
                // PREVIEW
                // =============================================

                newEngine
                    .startPreview()

                Log.d(
                    "AGORA",
                    "Local preview started"
                )

                // =============================================
                // JOIN CHANNEL
                // =============================================

                Log.d(
                    "AGORA",
                    "Joining channel = $channelName"
                )

                val joinResult =
                    newEngine
                        .joinChannel(
                            token,
                            channelName,
                            "",
                            0
                        )

                Log.d(
                    "AGORA",
                    "joinChannel result = " +
                            joinResult
                )

                Log.d(
                    "AGORA",
                    "Agora setup completed"
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    "AGORA",
                    "Agora initialization failed",
                    e
                )
            }

            // =================================================
            // AGORA DISPOSE
            // =================================================

            onDispose {

                Log.d(
                    "AGORA",
                    "=========================================="
                )

                Log.d(
                    "AGORA",
                    "LEAVING AGORA CHANNEL"
                )

                val engineToDispose =
                    localEngine

                // =============================================
                // UNREGISTER RAW VIDEO OBSERVER
                // =============================================

                try {

                    if (
                        engineToDispose != null
                    ) {

                        engineToDispose
                            .registerVideoFrameObserver(
                                null
                            )
                    }

                    Log.d(
                        "RAW_FRAME",
                        "Video frame observer unregistered"
                    )

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        "RAW_FRAME",
                        "Failed to unregister video frame observer",
                        e
                    )
                }

                // =============================================
                // STOP PREVIEW
                // =============================================

                try {

                    engineToDispose
                        ?.stopPreview()

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        "AGORA",
                        "stopPreview error",
                        e
                    )
                }

                // =============================================
                // LEAVE CHANNEL
                // =============================================

                try {

                    engineToDispose
                        ?.leaveChannel()

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        "AGORA",
                        "leaveChannel error",
                        e
                    )
                }

                // =============================================
                // CLEAR ENGINE
                // =============================================

                if (
                    engine ===
                    engineToDispose
                ) {

                    engine =
                        null
                }

                // =============================================
                // DESTROY ENGINE
                // =============================================

                if (
                    engineToDispose != null
                ) {

                    Thread {

                        try {

                            Thread.sleep(
                                300
                            )

                        } catch (_: Exception) {
                        }

                        try {

                            RtcEngine.destroy()

                            Log.d(
                                "AGORA",
                                "Agora engine destroyed"
                            )

                        } catch (
                            e: Exception
                        ) {

                            Log.e(
                                "AGORA",
                                "Agora destroy error",
                                e
                            )
                        }

                    }.start()
                }

                Log.d(
                    "AGORA",
                    "Agora dispose completed"
                )
            }
        }
    }

    // =========================================================
    // FACE DETECTOR DISPOSE
    // =========================================================

    DisposableEffect(
        faceDetector
    ) {

        onDispose {

            Log.d(
                "FACE_DETECTOR",
                "Releasing face detector..."
            )

            try {

                faceDetector.release()

            } catch (
                e: Exception
            ) {

                Log.e(
                    "FACE_DETECTOR",
                    "Face detector release error",
                    e
                )
            }
        }
    }

    // =========================================================
    // ARC FACE DISPOSE
    // =========================================================

    DisposableEffect(
        arcFaceEmbedder
    ) {

        onDispose {

            Log.d(
                "ARC_FACE",
                "Releasing ArcFace embedder..."
            )

            try {

                arcFaceEmbedder.release()

            } catch (
                e: Exception
            ) {

                Log.e(
                    "ARC_FACE",
                    "ArcFace release error",
                    e
                )
            }
        }
    }

    // =========================================================
    // FACE SWAPPER DISPOSE
    // =========================================================

    DisposableEffect(
        faceSwapper
    ) {

        onDispose {

            Log.d(
                "FACE_SWAPPER",
                "Releasing FaceSwapper..."
            )

            try {

                faceSwapper.release()

            } catch (
                e: Exception
            ) {

                Log.e(
                    "FACE_SWAPPER",
                    "FaceSwapper release error",
                    e
                )
            }
        }
    }

    // =========================================================
    // UI
    // =========================================================

    if (
        isLoadingCall
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                "Connecting..."
            )
        }

    } else if (
        channelName.isBlank()
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize(),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                "Call information not available"
            )
        }

    } else {

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            // =================================================
            // REMOTE VIDEO
            // =================================================

            AndroidView(

                factory = {

                    FrameLayout(
                        context
                    ).apply {

                        setBackgroundColor(
                            android.graphics.Color.BLACK
                        )

                        addView(
                            remoteView,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },

                modifier =
                    Modifier.fillMaxSize()
            )

            // =================================================
            // LOCAL VIDEO
            // =================================================

            AndroidView(

                factory = {

                    FrameLayout(
                        context
                    ).apply {

                        setBackgroundColor(
                            android.graphics.Color.BLACK
                        )

                        addView(
                            localView,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },

                modifier =
                    Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
            )

            // =================================================
            // FACE STATUS
            // =================================================

            Text(

                text =
                    when {

                        !isCaller ->
                            "Receiver • Original camera"

                        !faceModelLoaded ->
                            "Face detector loading..."

                        faceUrl.isBlank() ->
                            "No face selected"

                        faceDetectionRunning ->
                            "Detecting selected face..."

                        detectedFaceCount == 0 ->
                            "Face not detected"

                        !arcFaceReady ->
                            "ArcFace loading..."

                        arcFaceTesting ->
                            "Generating face embedding..."

                        !arcFaceSuccess ->
                            "Face embedding pending..."

                        faceSwapperLoading ->
                            "Loading InSwapper..."

                        !faceSwapperReady ->
                            "InSwapper not ready"
                        else ->
                            "Face swap ready ✓"
                    },

                modifier =
                    Modifier
                        .align(
                            Alignment.TopStart
                        )
                        .padding(
                            16.dp
                        )
            )

            // =================================================
            // CALLER NAME
            // =================================================

            if (
                callerName.isNotBlank()
            ) {

                Text(

                    text =
                        callerName,

                    modifier =
                        Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .padding(
                                16.dp
                            )
                )
            }

            // =================================================
            // END CALL
            // =================================================

            Button(

                onClick = {

                    Log.d(
                        "AGORA",
                        "End Call clicked"
                    )

                    // =========================================
                    // FIRESTORE
                    // =========================================

                    FirebaseFirestore
                        .getInstance()
                        .collection("calls")
                        .document(callId)
                        .update(
                            "status",
                            "rejected"
                        )
                        .addOnFailureListener {
                                error ->

                            Log.e(
                                "AGORA",
                                "Failed to update call status",
                                error
                            )
                        }

                    // =========================================
                    // LEAVE
                    // =========================================

                    try {

                        engine
                            ?.leaveChannel()

                    } catch (
                        e: Exception
                    ) {

                        Log.e(
                            "AGORA",
                            "Leave channel error",
                            e
                        )
                    }

                    // =========================================
                    // HOME
                    // =========================================

                    navController
                        .navigate(
                            "home"
                        ) {

                            popUpTo(
                                "home"
                            ) {

                                inclusive =
                                    false
                            }

                            launchSingleTop =
                                true
                        }
                },

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            16.dp
                        )

            ) {

                Text(
                    "End Call"
                )
            }
        }
    }
}
