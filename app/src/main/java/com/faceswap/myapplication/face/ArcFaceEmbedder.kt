package com.faceswap.myapplication.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.sqrt

// =============================================================
// ARC FACE EMBEDDER
// =============================================================
//
// Model:
//     w600k_r50.onnx
//
// Input:
//     112 x 112 RGB
//
// Output:
//     512 dimensional face embedding
//
// IMPORTANT:
//
// We DO NOT use:
//
//     inputStream.readBytes()
//
// because w600k_r50.onnx is very large.
//
// Instead:
//
//     assets
//        ↓
//     cache file
//        ↓
//     ONNX Runtime file path
//
// This avoids creating a huge Java ByteArray.
// =============================================================

class ArcFaceEmbedder {

    companion object {

        private const val TAG =
            "ARC_FACE"

        private const val MODEL_NAME =
            "w600k_r50.onnx"

        private const val INPUT_SIZE =
            112

        private const val EMBEDDING_SIZE =
            512
    }

    // =========================================================
    // ONNX ENVIRONMENT
    // =========================================================

    private var ortEnvironment:
            OrtEnvironment? = null

    // =========================================================
    // ONNX SESSION
    // =========================================================

    private var ortSession:
            OrtSession? = null

    // =========================================================
    // MODEL FILE
    // =========================================================

    private var modelFile:
            File? = null

    // =========================================================
    // LOAD MODEL
    // =========================================================

    suspend fun loadModel(
        context: Context
    ): Boolean =
        withContext(
            Dispatchers.IO
        ) {

            try {

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "STARTING ARC FACE MODEL LOAD"
                )

                Log.d(
                    TAG,
                    "Model = $MODEL_NAME"
                )

                // =================================================
                // PREVENT DOUBLE LOAD
                // =================================================

                if (
                    ortSession != null
                ) {

                    Log.d(
                        TAG,
                        "ArcFace model already loaded"
                    )

                    return@withContext true
                }

                // =================================================
                // CREATE ONNX ENVIRONMENT
                // =================================================

                Log.d(
                    TAG,
                    "Creating ONNX Runtime environment..."
                )

                val environment =
                    OrtEnvironment.getEnvironment()

                ortEnvironment =
                    environment

                // =================================================
                // CREATE CACHE DIRECTORY
                // =================================================

                val cacheDirectory =
                    File(
                        context.cacheDir,
                        "onnx_models"
                    )

                if (
                    !cacheDirectory.exists()
                ) {

                    val created =
                        cacheDirectory.mkdirs()

                    Log.d(
                        TAG,
                        "Cache directory created = $created"
                    )
                }

                // =================================================
                // MODEL DESTINATION
                // =================================================

                val destination =
                    File(
                        cacheDirectory,
                        MODEL_NAME
                    )

                modelFile =
                    destination

                // =================================================
                // COPY MODEL FROM ASSETS
                // =================================================

                if (
                    !destination.exists() ||
                    destination.length() == 0L
                ) {

                    Log.d(
                        TAG,
                        "Copying model from assets..."
                    )

                    Log.d(
                        TAG,
                        "This may take some time..."
                    )

                    context.assets
                        .open(
                            MODEL_NAME
                        )
                        .use { input ->

                            FileOutputStream(
                                destination
                            ).use { output ->

                                // ---------------------------------
                                // 1 MB COPY BUFFER
                                // ---------------------------------

                                val buffer =
                                    ByteArray(
                                        1024 * 1024
                                    )

                                var totalBytes =
                                    0L

                                var lastLoggedMB =
                                    -1L

                                while (true) {

                                    val count =
                                        input.read(
                                            buffer
                                        )

                                    if (
                                        count <= 0
                                    ) {

                                        break
                                    }

                                    output.write(
                                        buffer,
                                        0,
                                        count
                                    )

                                    totalBytes +=
                                        count

                                    // ---------------------------------
                                    // LOG EVERY ~10 MB
                                    // ---------------------------------

                                    val currentMB =
                                        totalBytes /
                                                (
                                                        1024L *
                                                                1024L
                                                        )

                                    if (
                                        currentMB >= 10 &&
                                        currentMB != lastLoggedMB &&
                                        currentMB % 10L == 0L
                                    ) {

                                        Log.d(
                                            TAG,
                                            "Copied = " +
                                                    currentMB +
                                                    " MB"
                                        )

                                        lastLoggedMB =
                                            currentMB
                                    }
                                }

                                output.flush()
                            }
                        }

                    Log.d(
                        TAG,
                        "Model copied successfully"
                    )

                    Log.d(
                        TAG,
                        "Model file size = " +
                                destination.length() +
                                " bytes"
                    )

                } else {

                    Log.d(
                        TAG,
                        "Using cached ArcFace model"
                    )

                    Log.d(
                        TAG,
                        "Cached file size = " +
                                destination.length() +
                                " bytes"
                    )
                }

                // =================================================
                // CREATE ONNX SESSION
                // =================================================
                //
                // IMPORTANT:
                //
                // We pass the FILE PATH.
                //
                // We do NOT do:
                //
                // inputStream.readBytes()
                //
                // That was causing the ~174 MB allocation
                // and OutOfMemoryError.
                //
                // =================================================

                Log.d(
                    TAG,
                    "Creating ONNX Runtime session..."
                )

                val sessionOptions =
                    OrtSession.SessionOptions()

                // =================================================
                // GRAPH OPTIMIZATION
                // =================================================

                try {

                    sessionOptions.setOptimizationLevel(
                        OrtSession
                            .SessionOptions
                            .OptLevel
                            .BASIC_OPT
                    )

                } catch (
                    e: Exception
                ) {

                    Log.w(
                        TAG,
                        "Could not set optimization level",
                        e
                    )
                }

                // =================================================
                // CREATE SESSION FROM FILE PATH
                // =================================================

                val session =
                    environment.createSession(
                        destination.absolutePath,
                        sessionOptions
                    )

                ortSession =
                    session

                // =================================================
                // LOG MODEL INFORMATION
                // =================================================

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "SUCCESS: w600k_r50.onnx loaded"
                )

                Log.d(
                    TAG,
                    "Model path = " +
                            destination.absolutePath
                )

                Log.d(
                    TAG,
                    "Model file size = " +
                            destination.length()
                )

                Log.d(
                    TAG,
                    "Input names = " +
                            session.inputNames
                )

                Log.d(
                    TAG,
                    "Output names = " +
                            session.outputNames
                )

                // -------------------------------------------------
                // OrtSession inputInfo/outputInfo are Maps.
                // Kotlin Map uses .size property.
                // -------------------------------------------------

                Log.d(
                    TAG,
                    "Input count = " +
                            session.inputInfo.size
                )

                Log.d(
                    TAG,
                    "Output count = " +
                            session.outputInfo.size
                )

                Log.d(
                    TAG,
                    "=========================================="
                )

                // =================================================
                // CLOSE SESSION OPTIONS
                // =================================================

                try {

                    sessionOptions.close()

                } catch (
                    _: Exception
                ) {
                }

                true

            } catch (
                e: OutOfMemoryError
            ) {

                Log.e(
                    TAG,
                    "=========================================="
                )

                Log.e(
                    TAG,
                    "ARC FACE OUT OF MEMORY"
                )

                Log.e(
                    TAG,
                    "w600k_r50.onnx could not be initialized"
                )

                Log.e(
                    TAG,
                    "==========================================",
                    e
                )

                false

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "=========================================="
                )

                Log.e(
                    TAG,
                    "ARC FACE MODEL LOAD FAILED"
                )

                Log.e(
                    TAG,
                    "==========================================",
                    e
                )

                false
            }
        }

    // =========================================================
    // MODEL READY
    // =========================================================

    fun isReady(): Boolean {

        return ortSession != null
    }

    // =========================================================
    // PREPARE 112 x 112 BITMAP
    // =========================================================

    private fun prepareInputBitmap(
        bitmap: Bitmap
    ): Bitmap {

        val resized =
            Bitmap.createBitmap(
                INPUT_SIZE,
                INPUT_SIZE,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(
                resized
            )

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )

        canvas.drawColor(
            Color.BLACK
        )

        canvas.drawBitmap(
            bitmap,
            null,
            android.graphics.Rect(
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
            ),
            paint
        )

        return resized
    }

    // =========================================================
    // BITMAP -> FLOAT ARRAY
    // =========================================================
    //
    // ArcFace normalization:
    //
    //     (pixel - 127.5) / 127.5
    //
    // Layout:
    //
    //     CHW
    //
    //     [R plane]
    //     [G plane]
    //     [B plane]
    //
    // Shape:
    //
    //     1 x 3 x 112 x 112
    //
    // =========================================================

    private fun bitmapToFloatArray(
        bitmap: Bitmap
    ): FloatArray {

        val pixels =
            IntArray(
                INPUT_SIZE *
                        INPUT_SIZE
            )

        bitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        val planeSize =
            INPUT_SIZE *
                    INPUT_SIZE

        val input =
            FloatArray(
                3 *
                        planeSize
            )

        for (
        index in pixels.indices
        ) {

            val pixel =
                pixels[index]

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

            // =================================================
            // RED
            // =================================================

            input[index] =
                (
                        red -
                                127.5f
                        ) /
                        127.5f

            // =================================================
            // GREEN
            // =================================================

            input[
                planeSize +
                        index
            ] =
                (
                        green -
                                127.5f
                        ) /
                        127.5f

            // =================================================
            // BLUE
            // =================================================

            input[
                (2 * planeSize) +
                        index
            ] =
                (
                        blue -
                                127.5f
                        ) /
                        127.5f
        }

        return input
    }

    // =========================================================
    // GENERATE EMBEDDING
    // =========================================================

    suspend fun getEmbedding(
        bitmap: Bitmap
    ): FloatArray? =
        withContext(
            Dispatchers.Default
        ) {

            val session =
                ortSession

            if (
                session == null
            ) {

                Log.e(
                    TAG,
                    "Cannot generate embedding: model not ready"
                )

                return@withContext null
            }

            var inputBitmap:
                    Bitmap? =
                null

            var inputTensor:
                    OnnxTensor? =
                null

            var results:
                    OrtSession.Result? =
                null

            try {

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "STARTING ARC FACE INFERENCE"
                )

                Log.d(
                    TAG,
                    "Source bitmap = " +
                            bitmap.width +
                            "x" +
                            bitmap.height
                )

                // =================================================
                // PREPARE 112 x 112
                // =================================================

                inputBitmap =
                    prepareInputBitmap(
                        bitmap
                    )

                Log.d(
                    TAG,
                    "Prepared bitmap = " +
                            inputBitmap.width +
                            "x" +
                            inputBitmap.height
                )

                // =================================================
                // CONVERT TO FLOAT
                // =================================================

                val inputData =
                    bitmapToFloatArray(
                        inputBitmap
                    )

                Log.d(
                    TAG,
                    "Input float size = " +
                            inputData.size
                )

                // =================================================
                // CREATE ENVIRONMENT
                // =================================================

                val environment =
                    ortEnvironment
                        ?: OrtEnvironment.getEnvironment()

                // =================================================
                // CREATE INPUT TENSOR
                // =================================================

                inputTensor =
                    OnnxTensor.createTensor(
                        environment,
                        FloatBuffer.wrap(
                            inputData
                        ),
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
                    session
                        .inputNames
                        .firstOrNull()

                if (
                    inputName == null
                ) {

                    Log.e(
                        TAG,
                        "ArcFace input name not found"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Input name = " +
                            inputName
                )

                // =================================================
                // RUN INFERENCE
                // =================================================

                Log.d(
                    TAG,
                    "Running ArcFace ONNX inference..."
                )

                results =
                    session.run(
                        mapOf(
                            inputName to
                                    inputTensor
                        )
                    )

                // =================================================
                // IMPORTANT
                // =================================================
                //
                // OrtSession.Result uses size()
                // function, NOT size property.
                //
                // =================================================

                if (
                    results.size() == 0
                ) {

                    Log.e(
                        TAG,
                        "ArcFace returned no output"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "ArcFace output count = " +
                            results.size()
                )

                // =================================================
                // GET FIRST OUTPUT
                // =================================================

                val output =
                    results[0]

                val value =
                    output.value

                // =================================================
                // EXTRACT FLOAT ARRAY
                // =================================================

                val embedding:
                        FloatArray? =

                    when (
                        value
                    ) {

                        is FloatArray -> {

                            value
                        }

                        is Array<*> -> {

                            val first =
                                value
                                    .firstOrNull()

                            when (
                                first
                            ) {

                                is FloatArray ->
                                    first

                                else ->
                                    null
                            }
                        }

                        else -> {

                            null
                        }
                    }

                // =================================================
                // CHECK OUTPUT
                // =================================================

                if (
                    embedding == null
                ) {

                    Log.e(
                        TAG,
                        "ArcFace output format not recognized"
                    )

                    Log.e(
                        TAG,
                        "Output class = " +
                                value?.javaClass
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Raw embedding size = " +
                            embedding.size
                )

                // =================================================
                // CHECK 512 DIMENSION
                // =================================================

                if (
                    embedding.size !=
                    EMBEDDING_SIZE
                ) {

                    Log.e(
                        TAG,
                        "Unexpected embedding size = " +
                                embedding.size
                    )

                    return@withContext null
                }

                // =================================================
                // NORMALIZE
                // =================================================

                val normalized =
                    normalizeEmbedding(
                        embedding
                    )

                Log.d(
                    TAG,
                    "Normalized embedding size = " +
                            normalized.size
                )

                // =================================================
                // FIRST 10 VALUES
                // =================================================

                Log.d(
                    TAG,
                    "First 10 values = " +
                            normalized
                                .take(10)
                                .joinToString(", ")
                )

                // =================================================
                // SUCCESS
                // =================================================

                Log.d(
                    TAG,
                    "SUCCESS: ArcFace embedding generated"
                )

                Log.d(
                    TAG,
                    "Embedding size = " +
                            normalized.size
                )

                Log.d(
                    TAG,
                    "=========================================="
                )

                normalized

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "ArcFace inference failed",
                    e
                )

                null

            } finally {

                // =================================================
                // CLOSE RESULT
                // =================================================

                try {

                    results?.close()

                } catch (
                    _: Exception
                ) {
                }

                // =================================================
                // CLOSE INPUT TENSOR
                // =================================================

                try {

                    inputTensor?.close()

                } catch (
                    _: Exception
                ) {
                }

                // =================================================
                // RECYCLE INPUT BITMAP
                // =================================================

                try {

                    if (
                        inputBitmap != null &&
                        !inputBitmap.isRecycled
                    ) {

                        inputBitmap.recycle()
                    }

                } catch (
                    _: Exception
                ) {
                }
            }
        }

    // =========================================================
    // L2 NORMALIZATION
    // =========================================================

    private fun normalizeEmbedding(
        embedding: FloatArray
    ): FloatArray {

        var sum =
            0.0

        for (
        value in embedding
        ) {

            sum +=
                value.toDouble() *
                        value.toDouble()
        }

        val norm =
            sqrt(
                sum
            ).toFloat()

        if (
            norm <= 0f
        ) {

            Log.w(
                TAG,
                "Embedding norm is zero"
            )

            return embedding.copyOf()
        }

        val result =
            FloatArray(
                embedding.size
            )

        for (
        index in embedding.indices
        ) {

            result[index] =
                embedding[index] /
                        norm
        }

        return result
    }

    // =========================================================
    // RELEASE
    // =========================================================

    fun release() {

        Log.d(
            TAG,
            "=========================================="
        )

        Log.d(
            TAG,
            "Releasing ArcFace model..."
        )

        try {

            ortSession?.close()

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Failed to close ArcFace session",
                e
            )
        }

        ortSession =
            null

        // =====================================================
        // DON'T CLOSE GLOBAL OrtEnvironment HERE
        // =====================================================
        //
        // OrtEnvironment.getEnvironment() is shared by ONNX
        // Runtime. We only clear our reference.
        //
        // =====================================================

        ortEnvironment =
            null

        modelFile =
            null

        Log.d(
            TAG,
            "ArcFace model released"
        )

        Log.d(
            TAG,
            "=========================================="
        )
    }
}