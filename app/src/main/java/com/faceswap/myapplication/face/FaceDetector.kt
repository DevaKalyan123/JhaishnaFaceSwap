package com.faceswap.myapplication.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.Log

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.net.HttpURLConnection
import java.net.URL
import java.nio.FloatBuffer

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min


// =============================================================
// FACE DETECTION RESULT
// =============================================================

data class FaceDetection(
    val box: RectF,
    val score: Float,
    val landmarks: List<Pair<Float, Float>>
)


// =============================================================
// FACE DETECTOR
// =============================================================

class FaceDetector(
    private val context: Context
) {

    companion object {

        private const val TAG =
            "FACE_DETECTOR"

        private const val MODEL_NAME =
            "det_10g.onnx"

        private const val INPUT_SIZE =
            640

        private const val SCORE_THRESHOLD =
            0.50f

        private const val NMS_THRESHOLD =
            0.40f
    }


    // =========================================================
    // ONNX
    // =========================================================

    private var environment:
            OrtEnvironment? = null

    private var session:
            OrtSession? = null


    // =========================================================
    // LIFECYCLE
    // =========================================================

    private val lifecycleLock =
        Any()

    private var activeDetections =
        0

    private var releaseRequested =
        false


    // =========================================================
    // LOAD MODEL
    // =========================================================

    suspend fun loadModel(): Boolean {

        return withContext(
            Dispatchers.Default
        ) {

            synchronized(
                lifecycleLock
            ) {

                if (
                    session != null
                ) {

                    Log.d(
                        TAG,
                        "Model already loaded"
                    )

                    return@withContext true
                }

                releaseRequested =
                    false
            }


            var createdSession:
                    OrtSession? =
                null


            try {

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "Loading $MODEL_NAME..."
                )


                // =================================================
                // ENVIRONMENT
                // =================================================

                val ortEnvironment =
                    OrtEnvironment
                        .getEnvironment()


                // =================================================
                // READ MODEL
                // =================================================

                val modelBytes =
                    context.assets
                        .open(
                            MODEL_NAME
                        )
                        .use { inputStream ->

                            inputStream.readBytes()
                        }


                Log.d(
                    TAG,
                    "Model size = " +
                            "${modelBytes.size / (1024 * 1024)} MB"
                )


                // =================================================
                // CREATE SESSION
                // =================================================

                createdSession =
                    ortEnvironment
                        .createSession(
                            modelBytes
                        )


                // =================================================
                // MODEL INFORMATION
                // =================================================

                Log.d(
                    TAG,
                    "Model input count = " +
                            createdSession
                                .inputNames
                                .size
                )

                Log.d(
                    TAG,
                    "Model output count = " +
                            createdSession
                                .outputNames
                                .size
                )


                createdSession
                    .outputNames
                    .forEachIndexed { index, name ->

                        Log.d(
                            TAG,
                            "Output[$index] name=$name"
                        )
                    }


                // =================================================
                // PUBLISH SESSION
                // =================================================

                synchronized(
                    lifecycleLock
                ) {

                    if (
                        releaseRequested
                    ) {

                        Log.d(
                            TAG,
                            "Release requested while model loading"
                        )

                    } else {

                        environment =
                            ortEnvironment

                        session =
                            createdSession

                        createdSession =
                            null
                    }
                }


                // =================================================
                // SESSION RELEASED BEFORE PUBLISH
                // =================================================

                if (
                    createdSession != null
                ) {

                    closeSessionInBackground(
                        createdSession!!
                    )

                    return@withContext false
                }


                Log.d(
                    TAG,
                    "SUCCESS: $MODEL_NAME loaded successfully"
                )

                Log.d(
                    TAG,
                    "=========================================="
                )


                true

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Failed to load $MODEL_NAME",
                    e
                )


                try {

                    createdSession?.close()

                } catch (_: Exception) {
                }


                false
            }
        }
    }


    // =========================================================
    // MODEL STATUS
    // =========================================================

    fun isModelLoaded(): Boolean {

        synchronized(
            lifecycleLock
        ) {

            return session != null
        }
    }


    // =========================================================
    // DETECT FROM BITMAP
    //
    // Used by LIVE CAMERA frames.
    // =========================================================

    fun detectFromBitmap(
        bitmap: Bitmap
    ): List<FaceDetection> {

        if (
            !beginDetection()
        ) {

            Log.d(
                TAG,
                "Live bitmap detection skipped"
            )

            return emptyList()
        }


        return try {

            detect(
                bitmap
            )

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Live bitmap detection failed",
                e
            )

            emptyList()

        } finally {

            endDetection()
        }
    }


    // =========================================================
    // DETECT FROM URL
    // =========================================================

    suspend fun detectFromUrl(
        imageUrl: String
    ): List<FaceDetection> {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                !beginDetection()
            ) {

                Log.d(
                    TAG,
                    "Detection skipped"
                )

                return@withContext emptyList()
            }


            var connection:
                    HttpURLConnection? =
                null

            var bitmap:
                    Bitmap? =
                null


            try {

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "Downloading uploaded face..."
                )


                // =================================================
                // DOWNLOAD
                // =================================================

                connection =
                    URL(
                        imageUrl
                    )
                        .openConnection()
                            as HttpURLConnection


                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    15000

                connection.requestMethod =
                    "GET"

                connection.connect()


                // =================================================
                // RESPONSE
                // =================================================

                if (
                    connection.responseCode !in
                    200..299
                ) {

                    Log.e(
                        TAG,
                        "Image download failed: " +
                                connection.responseCode
                    )

                    return@withContext emptyList()
                }


                // =================================================
                // DECODE
                // =================================================

                bitmap =
                    connection
                        .inputStream
                        .use { stream ->

                            BitmapFactory
                                .decodeStream(
                                    stream
                                )
                        }


                if (
                    bitmap == null
                ) {

                    Log.e(
                        TAG,
                        "Could not decode downloaded image"
                    )

                    return@withContext emptyList()
                }


                Log.d(
                    TAG,
                    "Image downloaded: " +
                            "${bitmap!!.width}x${bitmap!!.height}"
                )


                // =================================================
                // DETECTION
                // =================================================

                val result =
                    withContext(
                        Dispatchers.Default
                    ) {

                        detect(
                            bitmap!!
                        )
                    }


                result

            } catch (
                e: CancellationException
            ) {

                Log.d(
                    TAG,
                    "Detection coroutine cancelled"
                )

                throw e

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Failed to detect face from URL",
                    e
                )

                emptyList()

            } finally {

                try {

                    bitmap?.recycle()

                } catch (_: Exception) {
                }


                try {

                    connection?.disconnect()

                } catch (_: Exception) {
                }


                endDetection()
            }
        }
    }


    // =========================================================
    // BEGIN DETECTION
    // =========================================================

    private fun beginDetection(): Boolean {

        synchronized(
            lifecycleLock
        ) {

            if (
                releaseRequested
            ) {

                return false
            }


            if (
                session == null
            ) {

                Log.e(
                    TAG,
                    "Session is null"
                )

                return false
            }


            activeDetections++


            Log.d(
                TAG,
                "Detection started. active=$activeDetections"
            )


            return true
        }
    }


    // =========================================================
    // END DETECTION
    // =========================================================

    private fun endDetection() {

        var sessionToClose:
                OrtSession? =
            null


        synchronized(
            lifecycleLock
        ) {

            activeDetections =
                max(
                    0,
                    activeDetections - 1
                )


            Log.d(
                TAG,
                "Detection ended. active=$activeDetections"
            )


            if (
                activeDetections == 0 &&
                releaseRequested
            ) {

                sessionToClose =
                    session

                session =
                    null

                environment =
                    null
            }
        }


        if (
            sessionToClose != null
        ) {

            closeSessionInBackground(
                sessionToClose!!
            )
        }
    }


    // =========================================================
    // DETECT
    // =========================================================

    private fun detect(
        originalBitmap: Bitmap
    ): List<FaceDetection> {

        val ortEnvironment:
                OrtEnvironment?

        val ortSession:
                OrtSession?


        synchronized(
            lifecycleLock
        ) {

            ortEnvironment =
                environment

            ortSession =
                session
        }


        if (
            ortEnvironment == null ||
            ortSession == null
        ) {

            Log.e(
                TAG,
                "Detector session is not available"
            )

            return emptyList()
        }


        var inputBitmap:
                Bitmap? =
            null

        var resizedBitmap:
                Bitmap? =
            null

        var inputTensor:
                OnnxTensor? =
            null

        var results:
                OrtSession.Result? =
            null


        try {

            // =================================================
            // ORIGINAL SIZE
            // =================================================

            val originalWidth =
                originalBitmap
                    .width
                    .toFloat()


            val originalHeight =
                originalBitmap
                    .height
                    .toFloat()


            if (
                originalWidth <= 0f ||
                originalHeight <= 0f
            ) {

                return emptyList()
            }


            Log.d(
                TAG,
                "Original image = " +
                        "${originalWidth}x${originalHeight}"
            )


            // =================================================
            // SCALE
            // =================================================

            val scale =
                min(
                    INPUT_SIZE /
                            originalWidth,

                    INPUT_SIZE /
                            originalHeight
                )


            val resizedWidth =
                max(
                    1,
                    (
                            originalWidth *
                                    scale
                            ).toInt()
                )


            val resizedHeight =
                max(
                    1,
                    (
                            originalHeight *
                                    scale
                            ).toInt()
                )


            Log.d(
                TAG,
                "Resized image = " +
                        "${resizedWidth}x${resizedHeight}"
            )


            // =================================================
            // RESIZE
            // =================================================

            resizedBitmap =
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    resizedWidth,
                    resizedHeight,
                    true
                )


            // =================================================
            // CREATE 640x640 IMAGE
            // =================================================

            inputBitmap =
                Bitmap.createBitmap(
                    INPUT_SIZE,
                    INPUT_SIZE,
                    Bitmap.Config.ARGB_8888
                )


            val canvas =
                Canvas(
                    inputBitmap!!
                )


            canvas.drawColor(
                Color.BLACK
            )


            // =================================================
            // LETTERBOX
            // =================================================

            val paddingLeft =
                0f

            val paddingTop =
                0f


            canvas.drawBitmap(
                resizedBitmap!!,
                0f,
                0f,
                null
            )


            Log.d(
                TAG,
                "Input bitmap created: " +
                        "${INPUT_SIZE}x${INPUT_SIZE}"
            )

            Log.d(
                TAG,
                "Scale = $scale"
            )

            Log.d(
                TAG,
                "Padding left = $paddingLeft"
            )

            Log.d(
                TAG,
                "Padding top = $paddingTop"
            )


            // =================================================
            // FLOAT DATA
            // =================================================

            val planeSize =
                INPUT_SIZE *
                        INPUT_SIZE


            val inputData =
                FloatArray(
                    planeSize * 3
                )


            val pixels =
                IntArray(
                    planeSize
                )


            inputBitmap!!.getPixels(
                pixels,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
            )


            // =================================================
            // RGB NORMALIZATION
            //
            // SCRFD:
            // (pixel - 127.5) / 128.0
            // =================================================

            for (
            y in 0 until INPUT_SIZE
            ) {

                for (
                x in 0 until INPUT_SIZE
                ) {

                    val pixel =
                        pixels[
                            y *
                                    INPUT_SIZE +
                                    x
                        ]


                    val red =
                        Color.red(
                            pixel
                        )


                    val green =
                        Color.green(
                            pixel
                        )


                    val blue =
                        Color.blue(
                            pixel
                        )


                    val index =
                        y *
                                INPUT_SIZE +
                                x


                    inputData[index] =
                        (
                                red -
                                        127.5f
                                ) / 128f


                    inputData[
                        planeSize +
                                index
                    ] =
                        (
                                green -
                                        127.5f
                                ) / 128f


                    inputData[
                        planeSize * 2 +
                                index
                    ] =
                        (
                                blue -
                                        127.5f
                                ) / 128f
                }
            }


            // =================================================
            // TENSOR
            // =================================================

            val inputBuffer =
                FloatBuffer.wrap(
                    inputData
                )


            inputTensor =
                OnnxTensor.createTensor(
                    ortEnvironment,
                    inputBuffer,
                    longArrayOf(
                        1,
                        3,
                        INPUT_SIZE.toLong(),
                        INPUT_SIZE.toLong()
                    )
                )


            // =================================================
            // INPUT NAME
            // =================================================

            val inputName =
                ortSession
                    .inputNames
                    .first()


            Log.d(
                TAG,
                "Input name = $inputName"
            )


            // =================================================
            // INFERENCE
            // =================================================

            Log.d(
                TAG,
                "Running face detection..."
            )


            results =
                ortSession.run(
                    mapOf(
                        inputName to
                                inputTensor
                    )
                )


            Log.d(
                TAG,
                "ONNX inference completed"
            )


            // =================================================
            // OUTPUTS
            // =================================================

            val outputArrays =
                mutableListOf<FloatArray>()


            for (
            outputIndex in
            0 until results.size()
            ) {

                val outputValue =
                    try {

                        results.get(
                            outputIndex
                        )

                    } catch (
                        e: Exception
                    ) {

                        Log.e(
                            TAG,
                            "Failed getting output[$outputIndex]",
                            e
                        )

                        continue
                    }


                if (
                    outputValue !is OnnxTensor
                ) {

                    Log.e(
                        TAG,
                        "Output[$outputIndex] is not OnnxTensor"
                    )

                    continue
                }


                try {

                    val buffer =
                        outputValue
                            .getFloatBuffer()


                    val array =
                        FloatArray(
                            buffer.remaining()
                        )


                    buffer.get(
                        array
                    )


                    outputArrays.add(
                        array
                    )


                    Log.d(
                        TAG,
                        "Output $outputIndex size = " +
                                array.size
                    )

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        TAG,
                        "Failed reading output[$outputIndex]",
                        e
                    )
                }
            }


            // =================================================
            // VALIDATE OUTPUT COUNT
            // =================================================

            if (
                outputArrays.size != 9
            ) {

                Log.e(
                    TAG,
                    "Expected 9 outputs but got " +
                            outputArrays.size
                )

                return emptyList()
            }


            // =================================================
            // SCRFD OUTPUT ORDER
            //
            // 0,1,2 = scores
            // 3,4,5 = bbox
            // 6,7,8 = keypoints
            // =================================================

            val scores =
                listOf(
                    outputArrays[0],
                    outputArrays[1],
                    outputArrays[2]
                )


            val bboxes =
                listOf(
                    outputArrays[3],
                    outputArrays[4],
                    outputArrays[5]
                )


            val keypoints =
                listOf(
                    outputArrays[6],
                    outputArrays[7],
                    outputArrays[8]
                )


            Log.d(
                TAG,
                "Score outputs: " +
                        "${scores[0].size}, " +
                        "${scores[1].size}, " +
                        "${scores[2].size}"
            )


            Log.d(
                TAG,
                "BBox outputs: " +
                        "${bboxes[0].size}, " +
                        "${bboxes[1].size}, " +
                        "${bboxes[2].size}"
            )


            Log.d(
                TAG,
                "KPS outputs: " +
                        "${keypoints[0].size}, " +
                        "${keypoints[1].size}, " +
                        "${keypoints[2].size}"
            )


            // =================================================
            // DECODE
            // =================================================

            val detections =
                mutableListOf<FaceDetection>()


            val strides =
                intArrayOf(
                    8,
                    16,
                    32
                )


            for (
            index in 0..2
            ) {

                decodeScale(
                    scores =
                        scores[index],

                    bbox =
                        bboxes[index],

                    kps =
                        keypoints[index],

                    stride =
                        strides[index],

                    scale =
                        scale,

                    paddingLeft =
                        paddingLeft,

                    paddingTop =
                        paddingTop,

                    detections =
                        detections
                )
            }


            Log.d(
                TAG,
                "Raw detections = " +
                        detections.size
            )


            // =================================================
            // NMS
            // =================================================

            val finalDetections =
                applyNms(
                    detections
                )


            Log.d(
                TAG,
                "Final faces = " +
                        finalDetections.size
            )


            finalDetections
                .forEachIndexed { index, face ->

                    Log.d(
                        TAG,
                        "Face[$index] score=" +
                                face.score
                    )


                    Log.d(
                        TAG,
                        "Face[$index] box=" +
                                face.box
                    )


                    Log.d(
                        TAG,
                        "Face[$index] landmarks=" +
                                face.landmarks
                    )
                }


            return finalDetections

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Face detection failed",
                e
            )

            return emptyList()

        } finally {

            try {

                results?.close()

            } catch (_: Exception) {
            }


            try {

                inputTensor?.close()

            } catch (_: Exception) {
            }


            try {

                inputBitmap?.recycle()

            } catch (_: Exception) {
            }


            try {

                resizedBitmap?.recycle()

            } catch (_: Exception) {
            }
        }
    }


    // =========================================================
    // SCRFD DECODE
    // =========================================================

    private fun decodeScale(
        scores: FloatArray,
        bbox: FloatArray,
        kps: FloatArray,
        stride: Int,
        scale: Float,
        paddingLeft: Float,
        paddingTop: Float,
        detections: MutableList<FaceDetection>
    ) {

        val gridSize =
            INPUT_SIZE / stride


        val anchorCount =
            gridSize *
                    gridSize *
                    2


        if (
            scores.size <
            anchorCount
        ) {

            Log.e(
                TAG,
                "Invalid score output: " +
                        "expected=$anchorCount " +
                        "actual=${scores.size}"
            )

            return
        }


        // =========================================================
        // DEBUG SCORE RANGE
        //
        // THIS IS THE IMPORTANT NEW LOGGING
        // =========================================================

        val maxRawScore =
            scores.maxOrNull()
                ?: Float.NaN

        val minRawScore =
            scores.minOrNull()
                ?: Float.NaN


        Log.d(
            TAG,
            "DEBUG stride=$stride " +
                    "scoreCount=${scores.size} " +
                    "min=$minRawScore " +
                    "max=$maxRawScore"
        )


        // =========================================================
        // COUNT SCORES ABOVE THRESHOLD
        // =========================================================

        var scoresAboveThreshold =
            0


        // =========================================================
        // DECODE
        // =========================================================

        for (
        i in 0 until anchorCount
        ) {

            var score =
                scores[i]


            // =====================================================
            // LOGIT -> PROBABILITY
            //
            // SCRFD normally exports scores in probability form.
            // If the output looks like a logit, convert it.
            // =====================================================

            if (
                score < 0f ||
                score > 1f
            ) {

                score =
                    sigmoid(
                        score
                    )
            }


            if (
                score >=
                SCORE_THRESHOLD
            ) {

                scoresAboveThreshold++
            }


            if (
                score <
                SCORE_THRESHOLD
            ) {

                continue
            }


            // =====================================================
            // GRID LOCATION
            // =====================================================

            val location =
                i / 2


            val gridX =
                location %
                        gridSize


            val gridY =
                location /
                        gridSize


            // =====================================================
            // ANCHOR CENTER
            //
            // Same center generation used by SCRFD.
            // =====================================================

            val anchorX =
                gridX *
                        stride *
                        1f


            val anchorY =
                gridY *
                        stride *
                        1f


            // =====================================================
            // BBOX
            // =====================================================

            val bboxIndex =
                i * 4


            if (
                bboxIndex + 3 >=
                bbox.size
            ) {

                continue
            }


            // SCRFD bbox outputs are distances.
            // They must be multiplied by stride.

            val left =
                bbox[bboxIndex] *
                        stride


            val top =
                bbox[bboxIndex + 1] *
                        stride


            val right =
                bbox[bboxIndex + 2] *
                        stride


            val bottom =
                bbox[bboxIndex + 3] *
                        stride


            var x1 =
                anchorX -
                        left


            var y1 =
                anchorY -
                        top


            var x2 =
                anchorX +
                        right


            var y2 =
                anchorY +
                        bottom


            // =====================================================
            // MAP TO ORIGINAL IMAGE
            // =====================================================

            x1 =
                (
                        x1 -
                                paddingLeft
                        ) / scale


            y1 =
                (
                        y1 -
                                paddingTop
                        ) / scale


            x2 =
                (
                        x2 -
                                paddingLeft
                        ) / scale


            y2 =
                (
                        y2 -
                                paddingTop
                        ) / scale


            // =====================================================
            // CLAMP BOX
            // =====================================================

            val imageWidth =
                INPUT_SIZE /
                        scale

            val imageHeight =
                INPUT_SIZE /
                        scale


            x1 =
                x1.coerceIn(
                    0f,
                    imageWidth
                )


            y1 =
                y1.coerceIn(
                    0f,
                    imageHeight
                )


            x2 =
                x2.coerceIn(
                    0f,
                    imageWidth
                )


            y2 =
                y2.coerceIn(
                    0f,
                    imageHeight
                )


            val box =
                RectF(
                    x1,
                    y1,
                    x2,
                    y2
                )


            // =====================================================
            // LANDMARKS
            // =====================================================

            val landmarks =
                mutableListOf<Pair<Float, Float>>()


            val kpsIndex =
                i * 10


            if (
                kpsIndex + 9 <
                kps.size
            ) {

                for (
                point in 0 until 5
                ) {

                    val rawX =
                        kps[
                            kpsIndex +
                                    point * 2
                        ] *
                                stride


                    val rawY =
                        kps[
                            kpsIndex +
                                    point * 2 +
                                    1
                        ] *
                                stride


                    val finalX =
                        (
                                rawX +
                                        anchorX -
                                        paddingLeft
                                ) / scale


                    val finalY =
                        (
                                rawY +
                                        anchorY -
                                        paddingTop
                                ) / scale


                    landmarks.add(
                        Pair(
                            finalX,
                            finalY
                        )
                    )
                }
            }


            // =====================================================
            // ADD DETECTION
            // =====================================================

            detections.add(

                FaceDetection(
                    box =
                        box,

                    score =
                        score,

                    landmarks =
                        landmarks
                )
            )
        }


        // =========================================================
        // DEBUG RESULT
        // =========================================================

        Log.d(
            TAG,
            "DEBUG stride=$stride " +
                    "scoresAboveThreshold=" +
                    scoresAboveThreshold
        )
    }


    // =========================================================
    // SIGMOID
    // =========================================================

    private fun sigmoid(
        value: Float
    ): Float {

        return 1f /
                (
                        1f +
                                exp(
                                    -value
                                )
                        )
    }


    // =========================================================
    // NMS
    // =========================================================

    private fun applyNms(
        detections:
        List<FaceDetection>
    ): List<FaceDetection> {

        val sorted =
            detections
                .sortedByDescending {
                    it.score
                }
                .toMutableList()


        val selected =
            mutableListOf<FaceDetection>()


        while (
            sorted.isNotEmpty()
        ) {

            val best =
                sorted.removeAt(
                    0
                )


            selected.add(
                best
            )


            val iterator =
                sorted.iterator()


            while (
                iterator.hasNext()
            ) {

                val current =
                    iterator.next()


                val iou =
                    calculateIou(
                        best.box,
                        current.box
                    )


                if (
                    iou >
                    NMS_THRESHOLD
                ) {

                    iterator.remove()
                }
            }
        }


        return selected
    }


    // =========================================================
    // IOU
    // =========================================================

    private fun calculateIou(
        a: RectF,
        b: RectF
    ): Float {

        val left =
            max(
                a.left,
                b.left
            )


        val top =
            max(
                a.top,
                b.top
            )


        val right =
            min(
                a.right,
                b.right
            )


        val bottom =
            min(
                a.bottom,
                b.bottom
            )


        val width =
            max(
                0f,
                right - left
            )


        val height =
            max(
                0f,
                bottom - top
            )


        val intersection =
            width *
                    height


        val areaA =
            max(
                0f,
                a.width()
            ) *
                    max(
                        0f,
                        a.height()
                    )


        val areaB =
            max(
                0f,
                b.width()
            ) *
                    max(
                        0f,
                        b.height()
                    )


        val union =
            areaA +
                    areaB -
                    intersection


        if (
            union <= 0f
        ) {

            return 0f
        }


        return intersection /
                union
    }


    // =========================================================
    // RELEASE
    // =========================================================

    fun release() {

        var sessionToClose:
                OrtSession? =
            null


        synchronized(
            lifecycleLock
        ) {

            releaseRequested =
                true


            if (
                activeDetections == 0
            ) {

                sessionToClose =
                    session

                session =
                    null

                environment =
                    null
            }
        }


        if (
            sessionToClose != null
        ) {

            closeSessionInBackground(
                sessionToClose!!
            )
        }


        Log.d(
            TAG,
            "Face detector release requested"
        )
    }


    // =========================================================
    // CLOSE SESSION
    // =========================================================

    private fun closeSessionInBackground(
        sessionToClose: OrtSession
    ) {

        Thread {

            try {

                sessionToClose.close()


                Log.d(
                    TAG,
                    "OrtSession closed in background"
                )

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Session close failed",
                    e
                )
            }

        }.start()
    }
}