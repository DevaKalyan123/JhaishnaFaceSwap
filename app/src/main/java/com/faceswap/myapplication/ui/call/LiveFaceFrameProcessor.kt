package com.faceswap.myapplication.ui.call

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import com.faceswap.myapplication.face.FaceDetector
import com.faceswap.myapplication.face.FaceSwapper
import io.agora.base.NV21Buffer
import io.agora.base.VideoFrame
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live face-swap processor.
 *
 * IMPORTANT:
 * The Agora frame callback must stay fast. InSwapper is expensive, so we
 * never run detector/swapper inside the Agora callback.
 *
 * Flow:
 * Agora callback
 *      -> copy I420 to NV21
 *      -> immediately publish the latest processed frame (if available)
 *      -> background worker processes the copied frame
 *      -> latest swapped NV21 is stored
 *      -> next Agora callback publishes it
 *
 * This keeps the video call itself smooth. Face-swap updates are intentionally
 * slower than the camera FPS because the InSwapper model is heavy.
 */
class LiveFaceFrameProcessor(
    private val faceDetector: FaceDetector,
    private val faceSwapper: FaceSwapper
) {

    companion object {
        private const val TAG = "LIVE_PROCESSOR"
        private const val FACE_TAG = "LIVE_FACE_SWAP"

        // The face swap worker is allowed to start at most once per interval.
        // Camera/video remains at the Agora frame rate.
        private const val FRAME_INTERVAL_MS = 700L
    }

    private data class CameraFrame(
        val nv21: ByteArray,
        val width: Int,
        val height: Int,
        val rotation: Int
    )

    private data class ProcessedFrame(
        val nv21: ByteArray,
        val width: Int,
        val height: Int,
        val rotation: Int
    )

    @Volatile
    private var detectorReady = false

    @Volatile
    private var faceSwapEnabled = false

    @Volatile
    private var liveSwapRequested = false

    @Volatile
    private var sourceEmbedding: FloatArray? = null

    private val processing =
        AtomicBoolean(false)

    private val released =
        AtomicBoolean(false)

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(
                runnable,
                "FaceSwapWorker"
            ).apply {
                isDaemon = true
            }
        }

    @Volatile
    private var latestProcessedFrame: ProcessedFrame? = null

    @Volatile
    private var lastProcessRequestTime = 0L

    @Volatile
    private var totalFrames = 0

    @Volatile
    private var swappedFrames = 0

    @Volatile
    private var detectedFrames = 0

    // =========================================================
    // MODEL READY
    // =========================================================

    fun setModelReady(
        ready: Boolean
    ) {

        detectorReady =
            ready

        Log.d(
            FACE_TAG,
            "Detector ready = $ready"
        )

        refreshFaceSwapState()
    }

    // =========================================================
    // DETECTOR READY
    // =========================================================

    fun isDetectorReady(): Boolean =
        detectorReady

    // =========================================================
    // STATUS
    // =========================================================

    fun getLiveSwapStatus(): String {

        return "detectorReady=$detectorReady, " +
                "liveSwapRequested=$liveSwapRequested, " +
                "faceSwapEnabled=$faceSwapEnabled, " +
                "faceSwapperReady=${faceSwapper.isReady()}, " +
                "sourceEmbeddingReady=${sourceEmbedding != null}, " +
                "backgroundProcessing=${processing.get()}, " +
                "processedFrameReady=${latestProcessedFrame != null}"
    }

    // =========================================================
    // SOURCE EMBEDDING
    // =========================================================

    fun setSourceEmbedding(
        embedding: FloatArray?
    ) {

        if (embedding == null) {

            sourceEmbedding =
                null

            latestProcessedFrame =
                null

            Log.d(
                FACE_TAG,
                "SOURCE EMBEDDING CLEARED"
            )

            refreshFaceSwapState()

            return
        }

        if (embedding.size != 512) {

            sourceEmbedding =
                null

            latestProcessedFrame =
                null

            Log.e(
                FACE_TAG,
                "INVALID SOURCE EMBEDDING SIZE = ${embedding.size}"
            )

            refreshFaceSwapState()

            return
        }

        sourceEmbedding =
            embedding.copyOf()

        Log.d(
            FACE_TAG,
            "SOURCE EMBEDDING READY: ${embedding.size}D"
        )

        refreshFaceSwapState()
    }

    // =========================================================
    // REQUEST LIVE FACE SWAP
    // =========================================================

    fun setFaceSwapEnabled(
        enabled: Boolean
    ) {

        liveSwapRequested =
            enabled

        if (!enabled) {

            faceSwapEnabled =
                false

            latestProcessedFrame =
                null

            Log.d(
                FACE_TAG,
                "LIVE FACE SWAP DISABLED"
            )

            return
        }

        refreshFaceSwapState()
    }

    // =========================================================
    // REFRESH STATE
    // =========================================================

    private fun refreshFaceSwapState() {

        val embedding =
            sourceEmbedding

        val newEnabled =
            liveSwapRequested &&
                    detectorReady &&
                    faceSwapper.isReady() &&
                    embedding != null &&
                    embedding.size == 512 &&
                    !released.get()

        faceSwapEnabled =
            newEnabled

        Log.d(
            FACE_TAG,
            "STATE: detector=$detectorReady " +
                    "swapper=${faceSwapper.isReady()} " +
                    "embedding=${embedding?.size ?: 0} " +
                    "requested=$liveSwapRequested " +
                    "enabled=$newEnabled"
        )
    }

    // =========================================================
    // AGORA CALLBACK
    // =========================================================

    /**
     * Called directly by Agora.
     *
     * This method intentionally does NOT run FaceDetector or InSwapper.
     */
    fun processFrameForAgora(
        videoFrame: VideoFrame
    ): Boolean {

        if (released.get()) {
            return false
        }

        totalFrames++

        if (!faceSwapEnabled) {
            return false
        }

        // -----------------------------------------------------
        // 1. Publish the newest completed swap immediately.
        // -----------------------------------------------------

        val processed =
            latestProcessedFrame

        if (processed != null) {

            try {

                val outputBuffer =
                    NV21Buffer(
                        processed.nv21.copyOf(),
                        processed.width,
                        processed.height,
                        null
                    )

                videoFrame.replaceBuffer(
                    outputBuffer,
                    processed.rotation,
                    videoFrame.timestampNs
                )

                swappedFrames++

                // Consume this result. The worker will produce the next one.
                latestProcessedFrame =
                    null

                Log.d(
                    FACE_TAG,
                    "Published swapped frame #$swappedFrames"
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    FACE_TAG,
                    "Failed to publish processed frame",
                    e
                )
            }
        }

        // -----------------------------------------------------
        // 2. Copy the current camera frame quickly.
        // -----------------------------------------------------

        val now =
            System.currentTimeMillis()

        val shouldStartWorker =
            now - lastProcessRequestTime >=
                    FRAME_INTERVAL_MS &&
                    processing.compareAndSet(
                        false,
                        true
                    )

        if (!shouldStartWorker) {
            return processed != null
        }

        lastProcessRequestTime =
            now

        val cameraFrame =
            try {

                copyCameraFrame(
                    videoFrame
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Camera frame copy failed",
                    e
                )

                processing.set(false)

                null
            }

        if (cameraFrame == null) {
            return processed != null
        }

        // -----------------------------------------------------
        // 3. Heavy work runs outside the Agora callback.
        // -----------------------------------------------------

        val embedding =
            sourceEmbedding?.copyOf()

        executor.execute {

            try {

                processInBackground(
                    cameraFrame,
                    embedding
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    FACE_TAG,
                    "Background face swap error",
                    e
                )

            } finally {

                processing.set(false)
            }
        }

        return processed != null
    }

    // =========================================================
    // COPY CAMERA FRAME
    // =========================================================

    private fun copyCameraFrame(
        videoFrame: VideoFrame
    ): CameraFrame? {

        var i420Buffer:
                VideoFrame.I420Buffer? =
            null

        return try {

            val buffer =
                videoFrame.buffer

            i420Buffer =
                buffer.toI420()

            val width =
                i420Buffer.width

            val height =
                i420Buffer.height

            if (
                width <= 0 ||
                height <= 0
            ) {
                return null
            }

            val dataY =
                i420Buffer.dataY

            val dataU =
                i420Buffer.dataU

            val dataV =
                i420Buffer.dataV

            val strideY =
                i420Buffer.strideY

            val strideU =
                i420Buffer.strideU

            val strideV =
                i420Buffer.strideV

            val chromaWidth =
                (width + 1) / 2

            val chromaHeight =
                (height + 1) / 2

            val ySize =
                width * height

            val nv21 =
                ByteArray(
                    ySize +
                            chromaWidth *
                            chromaHeight *
                            2
                )

            // Y
            var destinationY =
                0

            for (
            y in 0 until height
            ) {

                val sourceY =
                    y * strideY

                for (
                x in 0 until width
                ) {

                    nv21[
                        destinationY + x
                    ] =
                        dataY[
                            sourceY + x
                        ]
                }

                destinationY +=
                    width
            }

            // VU
            var destinationUV =
                ySize

            for (
            y in 0 until chromaHeight
            ) {

                val sourceV =
                    y * strideV

                val sourceU =
                    y * strideU

                for (
                x in 0 until chromaWidth
                ) {

                    nv21[
                        destinationUV++
                    ] =
                        dataV[
                            sourceV + x
                        ]

                    nv21[
                        destinationUV++
                    ] =
                        dataU[
                            sourceU + x
                        ]
                }
            }

            CameraFrame(
                nv21 = nv21,
                width = width,
                height = height,
                rotation = videoFrame.rotation
            )

        } finally {

            try {
                i420Buffer?.release()
            } catch (_: Exception) {
            }
        }
    }

    // =========================================================
    // BACKGROUND PROCESSING
    // =========================================================

    private fun processInBackground(
        cameraFrame: CameraFrame,
        embedding: FloatArray?
    ) {

        if (released.get()) {
            return
        }

        if (!faceSwapEnabled) {
            return
        }

        if (
            embedding == null ||
            embedding.size != 512
        ) {
            Log.d(
                FACE_TAG,
                "Background skip: source embedding unavailable"
            )

            return
        }

        var bitmap:
                Bitmap? =
            null

        var swappedBitmap:
                Bitmap? =
            null

        var outputBitmap:
                Bitmap? =
            null

        try {

            Log.d(
                FACE_TAG,
                "Background processing ${cameraFrame.width}x${cameraFrame.height}, rotation=${cameraFrame.rotation}"
            )

            // -------------------------------------------------
            // NV21 -> Bitmap + rotation
            // -------------------------------------------------

            bitmap =
                nv21ToBitmap(
                    cameraFrame.nv21,
                    cameraFrame.width,
                    cameraFrame.height,
                    cameraFrame.rotation
                )

            if (bitmap == null) {

                Log.e(
                    FACE_TAG,
                    "Background NV21 -> Bitmap failed"
                )

                return
            }

            // -------------------------------------------------
            // FACE DETECTION
            // -------------------------------------------------

            val detections =
                faceDetector.detectFromBitmap(
                    bitmap
                )

            if (
                detections.isEmpty()
            ) {

                Log.d(
                    FACE_TAG,
                    "No face detected in camera frame"
                )

                return
            }

            detectedFrames++

            val targetFace =
                detections.maxByOrNull {
                    it.score
                }
                    ?: return

            Log.d(
                FACE_TAG,
                "Face detected score=${targetFace.score}"
            )

            // -------------------------------------------------
            // INSWAPPER
            // -------------------------------------------------

            val swapStart =
                System.currentTimeMillis()

            swappedBitmap =
                runBlocking {
                    faceSwapper.swap(
                        targetBitmap = bitmap,
                        targetFace = targetFace,
                        sourceEmbedding = embedding
                    )
                }

            val swapTime =
                System.currentTimeMillis() -
                        swapStart

            if (
                swappedBitmap == null
            ) {

                Log.e(
                    FACE_TAG,
                    "InSwapper returned null"
                )

                return
            }

            Log.d(
                FACE_TAG,
                "Face swap completed in ${swapTime}ms"
            )

            // -------------------------------------------------
            // ROTATE BACK TO CAMERA ORIENTATION
            // -------------------------------------------------

            outputBitmap =
                rotateBitmapBack(
                    swappedBitmap,
                    cameraFrame.rotation
                )

            // -------------------------------------------------
            // BITMAP -> NV21
            // -------------------------------------------------

            val outputNv21 =
                bitmapToNv21(
                    outputBitmap
                )
                    ?: return

            // -------------------------------------------------
            // STORE ONLY THE LATEST RESULT
            // -------------------------------------------------

            latestProcessedFrame =
                ProcessedFrame(
                    nv21 = outputNv21,
                    width = outputBitmap.width,
                    height = outputBitmap.height,
                    rotation = cameraFrame.rotation
                )

        } catch (
            e: Exception
        ) {

            Log.e(
                FACE_TAG,
                "Background face swap failed",
                e
            )

        } finally {

            try {

                if (
                    bitmap != null &&
                    !bitmap.isRecycled
                ) {

                    bitmap.recycle()
                }

            } catch (_: Exception) {
            }

            try {

                if (
                    swappedBitmap != null &&
                    swappedBitmap !== outputBitmap &&
                    !swappedBitmap.isRecycled
                ) {

                    swappedBitmap.recycle()
                }

            } catch (_: Exception) {
            }

            try {

                if (
                    outputBitmap != null &&
                    !outputBitmap.isRecycled
                ) {

                    outputBitmap.recycle()
                }

            } catch (_: Exception) {
            }
        }
    }

    // =========================================================
    // NV21 -> BITMAP
    // =========================================================

    private fun nv21ToBitmap(
        nv21: ByteArray,
        width: Int,
        height: Int,
        rotation: Int
    ): Bitmap? {

        return try {

            val yuvImage =
                YuvImage(
                    nv21,
                    android.graphics.ImageFormat.NV21,
                    width,
                    height,
                    null
                )

            val outputStream =
                ByteArrayOutputStream()

            yuvImage.compressToJpeg(
                Rect(
                    0,
                    0,
                    width,
                    height
                ),
                90,
                outputStream
            )

            val jpegBytes =
                outputStream.toByteArray()

            outputStream.close()

            if (
                jpegBytes.isEmpty()
            ) {
                return null
            }

            val bitmap =
                BitmapFactory.decodeByteArray(
                    jpegBytes,
                    0,
                    jpegBytes.size
                )
                    ?: return null

            if (
                rotation == 0
            ) {
                return bitmap
            }

            val matrix =
                Matrix()

            matrix.postRotate(
                rotation.toFloat()
            )

            val rotatedBitmap =
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )

            if (
                rotatedBitmap !== bitmap
            ) {
                bitmap.recycle()
            }

            rotatedBitmap

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "NV21 -> Bitmap failed",
                e
            )

            null
        }
    }

    // =========================================================
    // ROTATE BACK
    // =========================================================

    private fun rotateBitmapBack(
        bitmap: Bitmap,
        rotation: Int
    ): Bitmap {

        if (
            rotation == 0
        ) {
            return bitmap
        }

        val matrix =
            Matrix()

        matrix.postRotate(
            -rotation.toFloat()
        )

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    // =========================================================
    // BITMAP -> NV21
    // =========================================================

    private fun bitmapToNv21(
        bitmap: Bitmap
    ): ByteArray? {

        return try {

            val width =
                bitmap.width

            val height =
                bitmap.height

            if (
                width <= 0 ||
                height <= 0
            ) {
                return null
            }

            val chromaWidth =
                (width + 1) / 2

            val chromaHeight =
                (height + 1) / 2

            val ySize =
                width * height

            val output =
                ByteArray(
                    ySize +
                            chromaWidth *
                            chromaHeight *
                            2
                )

            val pixels =
                IntArray(
                    width * height
                )

            bitmap.getPixels(
                pixels,
                0,
                width,
                0,
                0,
                width,
                height
            )

            // -------------------------------------------------
            // Y
            // -------------------------------------------------

            var yIndex =
                0

            for (
            y in 0 until height
            ) {

                val row =
                    y * width

                for (
                x in 0 until width
                ) {

                    val pixel =
                        pixels[
                            row + x
                        ]

                    val r =
                        Color.red(pixel)

                    val g =
                        Color.green(pixel)

                    val b =
                        Color.blue(pixel)

                    val yValue =
                        (
                                66 * r +
                                        129 * g +
                                        25 * b +
                                        128
                                ) shr 8

                    output[
                        yIndex++
                    ] =
                        (
                                yValue + 16
                                )
                            .coerceIn(
                                0,
                                255
                            )
                            .toByte()
                }
            }

            // -------------------------------------------------
            // VU
            // -------------------------------------------------

            var uvIndex =
                ySize

            for (
            y in 0 until height step 2
            ) {

                for (
                x in 0 until width step 2
                ) {

                    var redTotal =
                        0

                    var greenTotal =
                        0

                    var blueTotal =
                        0

                    var count =
                        0

                    for (
                    dy in 0..1
                    ) {

                        val py =
                            y + dy

                        if (
                            py >= height
                        ) {
                            continue
                        }

                        for (
                        dx in 0..1
                        ) {

                            val px =
                                x + dx

                            if (
                                px >= width
                            ) {
                                continue
                            }

                            val pixel =
                                pixels[
                                    py * width + px
                                ]

                            redTotal +=
                                Color.red(pixel)

                            greenTotal +=
                                Color.green(pixel)

                            blueTotal +=
                                Color.blue(pixel)

                            count++
                        }
                    }

                    if (
                        count == 0
                    ) {
                        continue
                    }

                    val r =
                        redTotal / count

                    val g =
                        greenTotal / count

                    val b =
                        blueTotal / count

                    val vValue =
                        (
                                112 * r -
                                        94 * g -
                                        18 * b +
                                        128
                                ) shr 8

                    val uValue =
                        (
                                -38 * r -
                                        74 * g +
                                        112 * b +
                                        128
                                ) shr 8

                    output[
                        uvIndex++
                    ] =
                        (
                                vValue + 128
                                )
                            .coerceIn(
                                0,
                                255
                            )
                            .toByte()

                    output[
                        uvIndex++
                    ] =
                        (
                                uValue + 128
                                )
                            .coerceIn(
                                0,
                                255
                            )
                            .toByte()
                }
            }

            output

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Bitmap -> NV21 failed",
                e
            )

            null
        }
    }

    // =========================================================
    // RELEASE
    // =========================================================

    fun release() {

        if (
            !released.compareAndSet(
                false,
                true
            )
        ) {
            return
        }

        Log.d(
            FACE_TAG,
            "Releasing LiveFaceFrameProcessor"
        )

        faceSwapEnabled =
            false

        liveSwapRequested =
            false

        detectorReady =
            false

        sourceEmbedding =
            null

        latestProcessedFrame =
            null

        processing.set(
            false
        )

        try {
            executor.shutdownNow()
        } catch (_: Exception) {
        }

        lastProcessRequestTime =
            0L

        totalFrames =
            0

        swappedFrames =
            0

        detectedFrames =
            0

        Log.d(
            FACE_TAG,
            "LiveFaceFrameProcessor released"
        )
    }
}
