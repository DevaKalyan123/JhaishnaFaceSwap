package com.faceswap.myapplication.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
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
// FACE SWAPPER
// =============================================================
//
// Model:
//     inswapper_128.onnx
//
// EMAP:
//     emap.bin
//
// Input:
//     target face = 1 x 3 x 128 x 128
//     latent      = 1 x 512
//
// Output:
//     1 x 3 x 128 x 128
//
// Pipeline:
//
// target bitmap
//      ↓
// target face landmarks
//      ↓
// similarity alignment
//      ↓
// 128 x 128
//      ↓
// RGB / 255
//      ↓
// INSwapper
//      +
// source 512D embedding
//      ↓
// EMAP projection
//      ↓
// swapped 128 x 128
//      ↓
// inverse transform
//      ↓
// blend into target image
//
// =============================================================

class FaceSwapper {

    companion object {

        private const val TAG = "FACE_SWAPPER"

        private const val MODEL_NAME =
            "inswapper_128.onnx"

        private const val EMAP_NAME =
            "emap.bin"

        private const val SWAP_SIZE =
            128

        private const val EMBEDDING_SIZE =
            512

        private const val EMAP_SIZE =
            512 * 512

        private const val MODEL_STD =
            255.0f
    }

    // =========================================================
    // ONNX
    // =========================================================

    private var ortEnvironment:
            OrtEnvironment? = null

    private var ortSession:
            OrtSession? = null

    // =========================================================
    // FILES
    // =========================================================

    private var modelFile:
            File? = null

    private var emapFile:
            File? = null

    // =========================================================
    // EMAP
    // =========================================================

    private var emap:
            FloatArray? = null

    // =========================================================
    // MODEL READY
    // =========================================================

    fun isReady(): Boolean {
        return ortSession != null &&
                emap != null
    }

    // =========================================================
    // LOAD MODEL
    // =========================================================

    suspend fun loadModel(
        context: Context
    ): Boolean = withContext(
        Dispatchers.IO
    ) {

        try {

            Log.d(
                TAG,
                "=========================================="
            )

            Log.d(
                TAG,
                "STARTING INSWAPPER MODEL LOADING"
            )

            Log.d(
                TAG,
                "Model = $MODEL_NAME"
            )

            Log.d(
                TAG,
                "EMAP = $EMAP_NAME"
            )

            // =================================================
            // ALREADY LOADED
            // =================================================

            if (
                ortSession != null &&
                emap != null
            ) {

                Log.d(
                    TAG,
                    "InSwapper already loaded"
                )

                return@withContext true
            }

            // =================================================
            // ENVIRONMENT
            // =================================================

            val environment =
                OrtEnvironment.getEnvironment()

            ortEnvironment =
                environment

            // =================================================
            // CACHE DIRECTORY
            // =================================================

            val cacheDirectory =
                File(
                    context.cacheDir,
                    "onnx_models"
                )

            if (
                !cacheDirectory.exists()
            ) {

                cacheDirectory.mkdirs()
            }

            // =================================================
            // MODEL FILE
            // =================================================

            val destination =
                File(
                    cacheDirectory,
                    MODEL_NAME
                )

            modelFile =
                destination

            // =================================================
            // COPY INSWAPPER
            // =================================================

            if (
                !destination.exists() ||
                destination.length() == 0L
            ) {

                Log.d(
                    TAG,
                    "Copying InSwapper model..."
                )

                context.assets
                    .open(
                        MODEL_NAME
                    )
                    .use { input ->

                        FileOutputStream(
                            destination
                        ).use { output ->

                            val buffer =
                                ByteArray(
                                    1024 * 1024
                                )

                            var totalBytes =
                                0L

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

                                val currentMB =
                                    totalBytes /
                                            (
                                                    1024L *
                                                            1024L
                                                    )

                                if (
                                    totalBytes %
                                    (
                                            10L *
                                                    1024L *
                                                    1024L
                                            ) <
                                    1024L *
                                    1024L
                                ) {

                                    Log.d(
                                        TAG,
                                        "Copied = ${currentMB} MB"
                                    )
                                }
                            }

                            output.flush()
                        }
                    }

                Log.d(
                    TAG,
                    "InSwapper model copied"
                )

            } else {

                Log.d(
                    TAG,
                    "Using cached InSwapper model"
                )
            }

            Log.d(
                TAG,
                "InSwapper file size = " +
                        destination.length() +
                        " bytes"
            )

            // =================================================
            // EMAP FILE
            // =================================================

            val emapDestination =
                File(
                    cacheDirectory,
                    EMAP_NAME
                )

            emapFile =
                emapDestination

            if (
                !emapDestination.exists() ||
                emapDestination.length() == 0L
            ) {

                Log.d(
                    TAG,
                    "Copying emap.bin..."
                )

                context.assets
                    .open(
                        EMAP_NAME
                    )
                    .use { input ->

                        FileOutputStream(
                            emapDestination
                        ).use { output ->

                            val buffer =
                                ByteArray(
                                    64 * 1024
                                )

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
                            }

                            output.flush()
                        }
                    }

                Log.d(
                    TAG,
                    "emap.bin copied"
                )

            } else {

                Log.d(
                    TAG,
                    "Using cached emap.bin"
                )
            }

            Log.d(
                TAG,
                "emap file size = " +
                        emapDestination.length() +
                        " bytes"
            )

            // =========================================================
// LOAD EMAP
// =========================================================

            Log.d(
                TAG,
                "=========================================="
            )

            Log.d(
                TAG,
                "LOADING EMAP"
            )

            val emapBytes = emapDestination.readBytes()

            Log.d(
                TAG,
                "EMAP byte size = ${emapBytes.size}"
            )

            val actualFloatCount =
                emapBytes.size / 4

            Log.d(
                TAG,
                "Actual EMAP float count = $actualFloatCount"
            )

            Log.d(
                TAG,
                "Required EMAP float count = $EMAP_SIZE"
            )

            if (actualFloatCount < EMAP_SIZE) {

                Log.e(
                    TAG,
                    "EMAP file is too small"
                )

                return@withContext false
            }

// ---------------------------------------------------------
// Use ONLY first 512 x 512 values.
// The file contains 2 extra floats.
// ---------------------------------------------------------

            val emapBuffer =
                java.nio.ByteBuffer
                    .wrap(emapBytes)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN)

            val loadedEmap =
                FloatArray(
                    EMAP_SIZE
                )

            emapBuffer
                .asFloatBuffer()
                .get(
                    loadedEmap,
                    0,
                    EMAP_SIZE
                )

            emap =
                loadedEmap

            Log.d(
                TAG,
                "EMAP loaded successfully"
            )

            Log.d(
                TAG,
                "EMAP float count = ${loadedEmap.size}"
            )

            Log.d(
                TAG,
                "EMAP rows = 512"
            )

            Log.d(
                TAG,
                "EMAP columns = 512"
            )

            Log.d(
                TAG,
                "EMAP extra floats ignored = ${actualFloatCount - EMAP_SIZE}"
            )

            Log.d(
                TAG,
                "First 10 EMAP values = " +
                        loadedEmap
                            .take(10)
                            .joinToString(", ")
            )

            // =================================================
            // CREATE ONNX SESSION
            // =================================================

            Log.d(
                TAG,
                "=========================================="
            )

            Log.d(
                TAG,
                "CREATING INSWAPPER ONNX SESSION"
            )

            val sessionOptions =
                OrtSession.SessionOptions()

            try {

                sessionOptions.setOptimizationLevel(
                    OrtSession.SessionOptions.OptLevel.BASIC_OPT
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

            val session =
                environment.createSession(
                    destination.absolutePath,
                    sessionOptions
                )

            ortSession =
                session

            sessionOptions.close()

            // =================================================
            // LOG SESSION
            // =================================================

            Log.d(
                TAG,
                "=========================================="
            )

            Log.d(
                TAG,
                "SUCCESS: INSWAPPER MODEL LOADED"
            )

            Log.d(
                TAG,
                "Input names = ${session.inputNames}"
            )

            Log.d(
                TAG,
                "Output names = ${session.outputNames}"
            )

            Log.d(
                TAG,
                "Input count = ${session.inputInfo.size}"
            )

            Log.d(
                TAG,
                "Output count = ${session.outputInfo.size}"
            )

            Log.d(
                TAG,
                "=========================================="
            )

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
                "INSWAPPER OUT OF MEMORY"
            )

            Log.e(
                TAG,
                "The 555 MB model needs significant memory."
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
                "INSWAPPER MODEL LOAD FAILED"
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
    // ALIGN FACE
    // =========================================================
    //
    // Uses 5 landmarks:
    //
    // 0 = left eye
    // 1 = right eye
    // 2 = nose
    // 3 = left mouth
    // 4 = right mouth
    //
    // =========================================================

    fun alignFace(
        bitmap: Bitmap,
        face: FaceDetection,
        outputSize: Int
    ): Bitmap? {

        if (
            face.landmarks.size < 5
        ) {

            Log.e(
                TAG,
                "Need 5 landmarks for alignment"
            )

            return null
        }

        val source =
            Array(5) { index ->

                floatArrayOf(
                    face.landmarks[index].first,
                    face.landmarks[index].second
                )
            }

        val destination =
            arcFaceTemplate(
                outputSize
            )

        val matrixValues =
            calculateSimilarityTransform(
                source,
                destination
            )
                ?: return null

        val matrix =
            Matrix()

        matrix.setValues(
            matrixValues
        )

        val result =
            Bitmap.createBitmap(
                outputSize,
                outputSize,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(result)

        canvas.drawColor(
            Color.BLACK
        )

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )

        canvas.drawBitmap(
            bitmap,
            matrix,
            paint
        )

        return result
    }

    // =========================================================
    // ARC FACE / INSWAPPER TEMPLATE
    // =========================================================

    private fun arcFaceTemplate(
        size: Int
    ): Array<FloatArray> {

        val scale =
            size.toFloat() / 112.0f

        return arrayOf(

            floatArrayOf(
                38.2946f * scale,
                51.6963f * scale
            ),

            floatArrayOf(
                73.5318f * scale,
                51.5014f * scale
            ),

            floatArrayOf(
                56.0252f * scale,
                71.7366f * scale
            ),

            floatArrayOf(
                41.5493f * scale,
                92.3655f * scale
            ),

            floatArrayOf(
                70.7299f * scale,
                92.2041f * scale
            )
        )
    }

    // =========================================================
    // SIMILARITY TRANSFORM
    // =========================================================

    private fun calculateSimilarityTransform(
        source: Array<FloatArray>,
        destination: Array<FloatArray>
    ): FloatArray? {

        if (
            source.size != 5 ||
            destination.size != 5
        ) {
            return null
        }

        var sourceCenterX =
            0.0

        var sourceCenterY =
            0.0

        var destinationCenterX =
            0.0

        var destinationCenterY =
            0.0

        for (
        index in 0 until 5
        ) {

            sourceCenterX +=
                source[index][0]

            sourceCenterY +=
                source[index][1]

            destinationCenterX +=
                destination[index][0]

            destinationCenterY +=
                destination[index][1]
        }

        sourceCenterX /= 5.0
        sourceCenterY /= 5.0

        destinationCenterX /= 5.0
        destinationCenterY /= 5.0

        var sumXX =
            0.0

        var sumXY =
            0.0

        var dot =
            0.0

        var cross =
            0.0

        for (
        index in 0 until 5
        ) {

            val sx =
                source[index][0] -
                        sourceCenterX

            val sy =
                source[index][1] -
                        sourceCenterY

            val dx =
                destination[index][0] -
                        destinationCenterX

            val dy =
                destination[index][1] -
                        destinationCenterY

            sumXX +=
                sx * sx +
                        sy * sy

            dot +=
                sx * dx +
                        sy * dy

            cross +=
                sx * dy -
                        sy * dx
        }

        if (
            sumXX <= 0.000001
        ) {
            return null
        }

        val magnitude =
            sqrt(
                dot * dot +
                        cross * cross
            )

        if (
            magnitude <= 0.000001
        ) {
            return null
        }

        val cos =
            dot /
                    magnitude

        val sin =
            cross /
                    magnitude

        val scale =
            magnitude /
                    sumXX

        // x' = a*x + b*y + tx
        // y' = c*x + d*y + ty

        val a =
            scale * cos

        val b =
            -scale * sin

        val c =
            scale * sin

        val d =
            scale * cos

        val tx =
            destinationCenterX -
                    a * sourceCenterX -
                    b * sourceCenterY

        val ty =
            destinationCenterY -
                    c * sourceCenterX -
                    d * sourceCenterY

        return floatArrayOf(

            a.toFloat(),
            b.toFloat(),
            tx.toFloat(),

            c.toFloat(),
            d.toFloat(),
            ty.toFloat(),

            0f,
            0f,
            1f
        )
    }

    // =========================================================
    // BITMAP -> INSWAPPER TENSOR
    // =========================================================

    private fun bitmapToInput(
        bitmap: Bitmap
    ): FloatArray {

        val pixels =
            IntArray(
                SWAP_SIZE *
                        SWAP_SIZE
            )

        bitmap.getPixels(
            pixels,
            0,
            SWAP_SIZE,
            0,
            0,
            SWAP_SIZE,
            SWAP_SIZE
        )

        val planeSize =
            SWAP_SIZE *
                    SWAP_SIZE

        val data =
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

            // RGB / 255
            data[index] =
                red /
                        MODEL_STD

            data[
                planeSize +
                        index
            ] =
                green /
                        MODEL_STD

            data[
                2 * planeSize +
                        index
            ] =
                blue /
                        MODEL_STD
        }

        return data
    }

    // =========================================================
    // SOURCE EMBEDDING -> LATENT
    // =========================================================

    private fun createLatent(
        embedding: FloatArray
    ): FloatArray? {

        if (
            embedding.size !=
            EMBEDDING_SIZE
        ) {

            Log.e(
                TAG,
                "Invalid embedding size = ${embedding.size}"
            )

            return null
        }

        val matrix =
            emap
                ?: return null

        // =====================================================
        // latent = embedding x EMAP
        // =====================================================

        val latent =
            FloatArray(
                EMBEDDING_SIZE
            )

        for (
        column in 0 until EMBEDDING_SIZE
        ) {

            var sum =
                0.0

            for (
            row in 0 until EMBEDDING_SIZE
            ) {

                sum +=
                    embedding[row].toDouble() *
                            matrix[
                                row *
                                        EMBEDDING_SIZE +
                                        column
                            ].toDouble()
            }

            latent[column] =
                sum.toFloat()
        }

        // =====================================================
        // L2 NORMALIZE
        // =====================================================

        var normSquared =
            0.0

        for (
        value in latent
        ) {

            normSquared +=
                value.toDouble() *
                        value.toDouble()
        }

        val norm =
            sqrt(
                normSquared
            )

        if (
            norm <= 0.0000001
        ) {

            Log.e(
                TAG,
                "Latent norm is zero"
            )

            return null
        }

        for (
        index in latent.indices
        ) {

            latent[index] =
                (
                        latent[index] /
                                norm
                        ).toFloat()
        }

        Log.d(
            TAG,
            "Latent generated"
        )

        Log.d(
            TAG,
            "Latent size = ${latent.size}"
        )

        Log.d(
            TAG,
            "Latent L2 norm = " +
                    sqrt(
                        latent.sumOf {
                            it.toDouble() *
                                    it.toDouble()
                        }
                    )
        )

        Log.d(
            TAG,
            "First 10 latent values = " +
                    latent
                        .take(10)
                        .joinToString(", ")
        )

        return latent
    }

    // =========================================================
    // SWAP FACE
    // =========================================================

    suspend fun swap(
        targetBitmap: Bitmap,
        targetFace: FaceDetection,
        sourceEmbedding: FloatArray
    ): Bitmap? =
        withContext(
            Dispatchers.Default
        ) {

            if (
                !isReady()
            ) {

                Log.e(
                    TAG,
                    "FaceSwapper is not ready"
                )

                return@withContext null
            }

            var alignedTarget:
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
                    "STARTING FACE SWAP"
                )

                Log.d(
                    TAG,
                    "Target = " +
                            targetBitmap.width +
                            "x" +
                            targetBitmap.height
                )

                Log.d(
                    TAG,
                    "Target face score = " +
                            targetFace.score
                )

                // =================================================
                // ALIGN TARGET FACE
                // =================================================

                Log.d(
                    TAG,
                    "Aligning target face to 128x128..."
                )

                alignedTarget =
                    alignFace(
                        targetBitmap,
                        targetFace,
                        SWAP_SIZE
                    )

                if (
                    alignedTarget == null
                ) {

                    Log.e(
                        TAG,
                        "Target face alignment failed"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Target aligned = " +
                            alignedTarget.width +
                            "x" +
                            alignedTarget.height
                )

                // =================================================
                // LATENT
                // =================================================

                val latent =
                    createLatent(
                        sourceEmbedding
                    )

                if (
                    latent == null
                ) {

                    Log.e(
                        TAG,
                        "Could not create latent"
                    )

                    return@withContext null
                }

                // =================================================
                // IMAGE INPUT
                // =================================================

                val imageData =
                    bitmapToInput(
                        alignedTarget
                    )

                Log.d(
                    TAG,
                    "Image tensor values = " +
                            imageData.size
                )

                // =================================================
                // SESSION
                // =================================================

                val environment =
                    ortEnvironment
                        ?: OrtEnvironment.getEnvironment()

                val session =
                    ortSession
                        ?: return@withContext null

                val inputNames =
                    session.inputNames.toList()

                if (
                    inputNames.size < 2
                ) {

                    Log.e(
                        TAG,
                        "Expected 2 InSwapper inputs"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Input[0] = ${inputNames[0]}"
                )

                Log.d(
                    TAG,
                    "Input[1] = ${inputNames[1]}"
                )

                // =================================================
                // IMAGE TENSOR
                // =================================================

                inputTensor =
                    OnnxTensor.createTensor(
                        environment,
                        FloatBuffer.wrap(
                            imageData
                        ),
                        longArrayOf(
                            1,
                            3,
                            SWAP_SIZE.toLong(),
                            SWAP_SIZE.toLong()
                        )
                    )

                // =================================================
                // LATENT TENSOR
                // =================================================

                val latentTensor =
                    OnnxTensor.createTensor(
                        environment,
                        FloatBuffer.wrap(
                            latent
                        ),
                        longArrayOf(
                            1,
                            EMBEDDING_SIZE.toLong()
                        )
                    )

                // =================================================
                // RUN MODEL
                // =================================================

                Log.d(
                    TAG,
                    "Running InSwapper inference..."
                )

                results =
                    session.run(
                        mapOf(
                            inputNames[0] to
                                    inputTensor,

                            inputNames[1] to
                                    latentTensor
                        )
                    )

                Log.d(
                    TAG,
                    "InSwapper inference completed"
                )

                Log.d(
                    TAG,
                    "Output count = ${results.size()}"
                )

                if (
                    results.size() <= 0
                ) {

                    Log.e(
                        TAG,
                        "No output from InSwapper"
                    )

                    latentTensor.close()

                    return@withContext null
                }

                val output =
                    results[0]

                val outputValue =
                    output.value

                // =================================================
                // OUTPUT
                // =================================================

                val outputData =
                    extractOutput(
                        outputValue
                    )

                if (
                    outputData == null
                ) {

                    Log.e(
                        TAG,
                        "Could not parse InSwapper output"
                    )

                    latentTensor.close()

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Output float count = " +
                            outputData.size
                )

                if (
                    outputData.size <
                    3 *
                    SWAP_SIZE *
                    SWAP_SIZE
                ) {

                    Log.e(
                        TAG,
                        "Invalid InSwapper output size"
                    )

                    latentTensor.close()

                    return@withContext null
                }

                // =================================================
                // OUTPUT -> BITMAP
                // =================================================

                val swappedFace =
                    outputToBitmap(
                        outputData
                    )

                Log.d(
                    TAG,
                    "Swapped face bitmap created"
                )

                // =================================================
                // INVERSE TRANSFORM
                // =================================================

                val sourceMatrix =
                    createTransformMatrix(
                        targetFace,
                        SWAP_SIZE
                    )

                val inverseMatrix =
                    invertMatrix(
                        sourceMatrix
                    )

                if (
                    inverseMatrix == null
                ) {

                    Log.e(
                        TAG,
                        "Could not invert alignment matrix"
                    )

                    swappedFace.recycle()

                    latentTensor.close()

                    return@withContext null
                }

                // =================================================
                // PASTE BACK
                // =================================================

                Log.d(
                    TAG,
                    "Blending swapped face back..."
                )

                val result =
                    pasteBack(
                        targetBitmap,
                        swappedFace,
                        inverseMatrix
                    )

                swappedFace.recycle()

                latentTensor.close()

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "SUCCESS: FACE SWAP COMPLETED"
                )

                Log.d(
                    TAG,
                    "=========================================="
                )

                result

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Face swap failed",
                    e
                )

                null

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
                    alignedTarget?.recycle()
                } catch (_: Exception) {
                }
            }
        }

    // =========================================================
    // CREATE TRANSFORM MATRIX
    // =========================================================

    private fun createTransformMatrix(
        face: FaceDetection,
        size: Int
    ): FloatArray {

        val source =
            Array(5) { index ->

                floatArrayOf(
                    face.landmarks[index].first,
                    face.landmarks[index].second
                )
            }

        val destination =
            arcFaceTemplate(size)

        return calculateSimilarityTransform(
            source,
            destination
        )
            ?: floatArrayOf(
                1f, 0f, 0f,
                0f, 1f, 0f,
                0f, 0f, 1f
            )
    }

    // =========================================================
    // MATRIX INVERSE
    // =========================================================

    private fun invertMatrix(
        matrix: FloatArray
    ): FloatArray? {

        val a =
            matrix[0].toDouble()

        val b =
            matrix[1].toDouble()

        val tx =
            matrix[2].toDouble()

        val c =
            matrix[3].toDouble()

        val d =
            matrix[4].toDouble()

        val ty =
            matrix[5].toDouble()

        val determinant =
            a * d -
                    b * c

        if (
            kotlin.math.abs(determinant) <
            0.0000001
        ) {
            return null
        }

        val invA =
            d /
                    determinant

        val invB =
            -b /
                    determinant

        val invC =
            -c /
                    determinant

        val invD =
            a /
                    determinant

        val invTx =
            -(
                    invA * tx +
                            invB * ty
                    )

        val invTy =
            -(
                    invC * tx +
                            invD * ty
                    )

        return floatArrayOf(

            invA.toFloat(),
            invB.toFloat(),
            invTx.toFloat(),

            invC.toFloat(),
            invD.toFloat(),
            invTy.toFloat(),

            0f,
            0f,
            1f
        )
    }

    // =========================================================
    // OUTPUT PARSER
    // =========================================================

    private fun extractOutput(
        value: Any?
    ): FloatArray? {

        // InSwapper output is normally shaped like:
        // [1, 3, 128, 128]
        // ONNX Runtime may expose this as nested Java/Kotlin arrays.
        // Do NOT take only firstOrNull(), because that would lose the
        // green/blue channels. Flatten the complete nested output.

        val values = ArrayList<Float>()

        fun collect(item: Any?) {

            when (item) {

                null -> {
                    // Nothing to collect
                }

                is FloatArray -> {
                    for (number in item) {
                        values.add(number)
                    }
                }

                is Array<*> -> {
                    for (element in item) {
                        collect(element)
                    }
                }

                is Float -> {
                    values.add(item)
                }

                is Double -> {
                    values.add(item.toFloat())
                }

                else -> {
                    // Handle any other Java primitive array type safely.
                    val javaClass = item.javaClass

                    if (javaClass.isArray) {
                        val length =
                            java.lang.reflect.Array.getLength(item)

                        for (index in 0 until length) {
                            collect(
                                java.lang.reflect.Array.get(
                                    item,
                                    index
                                )
                            )
                        }
                    }
                }
            }
        }

        collect(value)

        if (values.isEmpty()) {
            Log.e(
                TAG,
                "InSwapper output is empty or unsupported: ${value?.javaClass}"
            )
            return null
        }

        val result =
            FloatArray(values.size)

        for (index in values.indices) {
            result[index] = values[index]
        }

        Log.d(
            TAG,
            "Flattened output size = ${result.size}"
        )

        return result
    }

    // =========================================================
    // OUTPUT -> BITMAP
    // =========================================================
    //
    // Model output:
    //     1 x 3 x 128 x 128
    //
    // values:
    //     0.0 ... 1.0
    //
    // =========================================================

    private fun outputToBitmap(
        data: FloatArray
    ): Bitmap {

        val planeSize =
            SWAP_SIZE *
                    SWAP_SIZE

        val bitmap =
            Bitmap.createBitmap(
                SWAP_SIZE,
                SWAP_SIZE,
                Bitmap.Config.ARGB_8888
            )

        val pixels =
            IntArray(
                planeSize
            )

        for (
        index in 0 until planeSize
        ) {

            val red =
                (
                        data[index] *
                                255.0f
                        )
                    .coerceIn(
                        0f,
                        255f
                    )
                    .toInt()

            val green =
                (
                        data[
                            planeSize +
                                    index
                        ] *
                                255.0f
                        )
                    .coerceIn(
                        0f,
                        255f
                    )
                    .toInt()

            val blue =
                (
                        data[
                            2 *
                                    planeSize +
                                    index
                        ] *
                                255.0f
                        )
                    .coerceIn(
                        0f,
                        255f
                    )
                    .toInt()

            pixels[index] =
                Color.argb(
                    255,
                    red,
                    green,
                    blue
                )
        }

        bitmap.setPixels(
            pixels,
            0,
            SWAP_SIZE,
            0,
            0,
            SWAP_SIZE,
            SWAP_SIZE
        )

        return bitmap
    }

    // =========================================================
    // PASTE BACK
    // =========================================================

    private fun pasteBack(
        target: Bitmap,
        swappedFace: Bitmap,
        inverseMatrixValues: FloatArray
    ): Bitmap {

        val width =
            target.width

        val height =
            target.height

        val result =
            target.copy(
                Bitmap.Config.ARGB_8888,
                true
            )

        val warpedFace =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        val warpedMask =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ALPHA_8
            )

        val matrix =
            Matrix()

        matrix.setValues(
            inverseMatrixValues
        )

        // =====================================================
        // WARP FACE
        // =====================================================

        val faceCanvas =
            Canvas(
                warpedFace
            )

        faceCanvas.drawColor(
            Color.TRANSPARENT
        )

        val facePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )

        faceCanvas.drawBitmap(
            swappedFace,
            matrix,
            facePaint
        )

        // =====================================================
        // CREATE SOFT ELLIPSE MASK
        // =====================================================

        val mask128 =
            Bitmap.createBitmap(
                SWAP_SIZE,
                SWAP_SIZE,
                Bitmap.Config.ALPHA_8
            )

        val maskCanvas =
            Canvas(
                mask128
            )

        maskCanvas.drawColor(
            Color.TRANSPARENT
        )

        val maskPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        maskPaint.color =
            Color.WHITE

        val padding =
            8f

        maskCanvas.drawOval(
            RectF(
                padding,
                padding,
                SWAP_SIZE - padding,
                SWAP_SIZE - padding
            ),
            maskPaint
        )

        // =====================================================
        // WARP MASK
        // =====================================================

        val maskCanvasFull =
            Canvas(
                warpedMask
            )

        maskCanvasFull.drawColor(
            Color.TRANSPARENT
        )

        maskCanvasFull.drawBitmap(
            mask128,
            matrix,
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )
        )

        mask128.recycle()

        // =====================================================
        // BLEND
        // =====================================================

        val targetPixels =
            IntArray(
                width *
                        height
            )

        val swappedPixels =
            IntArray(
                width *
                        height
            )

        val maskPixels =
            ByteArray(
                width *
                        height
            )

        result.getPixels(
            targetPixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        warpedFace.getPixels(
            swappedPixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        warpedMask.copyPixelsToBuffer(
            java.nio.ByteBuffer.wrap(
                maskPixels
            )
        )

        for (
        index in targetPixels.indices
        ) {

            val alpha =
                (
                        maskPixels[index].toInt() and
                                0xFF
                        ) /
                        255.0f

            if (
                alpha <= 0.01f
            ) {
                continue
            }

            val targetPixel =
                targetPixels[index]

            val swapPixel =
                swappedPixels[index]

            val targetRed =
                Color.red(
                    targetPixel
                )

            val targetGreen =
                Color.green(
                    targetPixel
                )

            val targetBlue =
                Color.blue(
                    targetPixel
                )

            val swapRed =
                Color.red(
                    swapPixel
                )

            val swapGreen =
                Color.green(
                    swapPixel
                )

            val swapBlue =
                Color.blue(
                    swapPixel
                )

            val red =
                (
                        targetRed *
                                (1f - alpha) +
                                swapRed *
                                alpha
                        )
                    .toInt()
                    .coerceIn(0, 255)

            val green =
                (
                        targetGreen *
                                (1f - alpha) +
                                swapGreen *
                                alpha
                        )
                    .toInt()
                    .coerceIn(0, 255)

            val blue =
                (
                        targetBlue *
                                (1f - alpha) +
                                swapBlue *
                                alpha
                        )
                    .toInt()
                    .coerceIn(0, 255)

            targetPixels[index] =
                Color.rgb(
                    red,
                    green,
                    blue
                )
        }

        result.setPixels(
            targetPixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        warpedFace.recycle()
        warpedMask.recycle()

        return result
    }

    // =========================================================
    // RELEASE
    // =========================================================

    fun release() {

        Log.d(
            TAG,
            "Releasing InSwapper..."
        )

        try {

            ortSession?.close()

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Failed to close InSwapper session",
                e
            )
        }

        ortSession =
            null

        ortEnvironment =
            null

        emap =
            null

        modelFile =
            null

        emapFile =
            null

        Log.d(
            TAG,
            "InSwapper released"
        )
    }
}
