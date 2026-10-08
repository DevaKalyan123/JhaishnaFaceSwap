package com.faceswap.myapplication.face

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.net.URL
import java.net.HttpURLConnection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// =============================================================
// ARC FACE TEST RUNNER
// =============================================================
//
// Flow:
//
// selected face URL
//       ↓
// download image
//       ↓
// FaceDetector
//       ↓
// get face + landmarks
//       ↓
// align face to 112 x 112
//       ↓
// ArcFace w600k_r50
//       ↓
// 512D embedding
//
// =============================================================

object ArcFaceTestRunner {

    private const val TAG =
        "ARC_FACE_TEST"

    private const val FACE_SIZE =
        112

    // =========================================================
    // ArcFace 5-point reference landmarks
    // =========================================================
    //
    // Standard ArcFace alignment target.
    //
    // Order:
    // 0 = left eye
    // 1 = right eye
    // 2 = nose
    // 3 = left mouth
    // 4 = right mouth
    //
    // =========================================================

    private val REFERENCE_POINTS =
        arrayOf(

            floatArrayOf(
                38.2946f,
                51.6963f
            ),

            floatArrayOf(
                73.5318f,
                51.5014f
            ),

            floatArrayOf(
                56.0252f,
                71.7366f
            ),

            floatArrayOf(
                41.5493f,
                92.3655f
            ),

            floatArrayOf(
                70.7299f,
                92.2041f
            )
        )

    // =========================================================
    // MAIN TEST
    // =========================================================

    suspend fun test(
        imageUrl: String,
        faceDetector: FaceDetector,
        arcFaceEmbedder: ArcFaceEmbedder
    ): FloatArray? =
        withContext(
            Dispatchers.IO
        ) {

            Log.d(
                TAG,
                "=========================================="
            )

            Log.d(
                TAG,
                "STARTING ARC FACE TEST"
            )

            Log.d(
                TAG,
                "Image URL available = " +
                        imageUrl.isNotBlank()
            )

            // =================================================
            // DOWNLOAD IMAGE
            // =================================================

            val bitmap =
                try {

                    downloadBitmap(
                        imageUrl
                    )

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        TAG,
                        "Failed to download selected face",
                        e
                    )

                    return@withContext null
                }

            if (
                bitmap == null
            ) {

                Log.e(
                    TAG,
                    "Downloaded bitmap is null"
                )

                return@withContext null
            }

            Log.d(
                TAG,
                "Downloaded bitmap = " +
                        bitmap.width +
                        "x" +
                        bitmap.height
            )

            try {

                // =================================================
                // FACE DETECTION
                // =================================================

                Log.d(
                    TAG,
                    "Running FaceDetector..."
                )

                val detections =
                    faceDetector
                        .detectFromBitmap(
                            bitmap
                        )

                Log.d(
                    TAG,
                    "Detected faces = " +
                            detections.size
                )

                if (
                    detections.isEmpty()
                ) {

                    Log.e(
                        TAG,
                        "NO FACE DETECTED"
                    )

                    return@withContext null
                }

                // =================================================
                // SELECT BEST FACE
                // =================================================

                val face =
                    detections
                        .maxByOrNull {
                            it.score
                        }

                if (
                    face == null
                ) {

                    Log.e(
                        TAG,
                        "Could not select detected face"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Selected face score = " +
                            face.score
                )

                Log.d(
                    TAG,
                    "Selected face box = " +
                            face.box
                )

                Log.d(
                    TAG,
                    "Selected landmarks = " +
                            face.landmarks
                )

                // =================================================
                // CHECK LANDMARKS
                // =================================================

                if (
                    face.landmarks.size < 5
                ) {

                    Log.e(
                        TAG,
                        "Need 5 landmarks for ArcFace alignment"
                    )

                    Log.e(
                        TAG,
                        "Landmark count = " +
                                face.landmarks.size
                    )

                    return@withContext null
                }

                // =================================================
                // ALIGN FACE
                // =================================================

                Log.d(
                    TAG,
                    "Aligning face to 112x112..."
                )

                val alignedFace =
                    alignFace(
                        bitmap,
                        face
                    )

                if (
                    alignedFace == null
                ) {

                    Log.e(
                        TAG,
                        "Face alignment failed"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Aligned face = " +
                            alignedFace.width +
                            "x" +
                            alignedFace.height
                )

                // =================================================
                // ARC FACE INFERENCE
                // =================================================

                Log.d(
                    TAG,
                    "Calling ArcFace getEmbedding()..."
                )

                val embedding =
                    arcFaceEmbedder
                        .getEmbedding(
                            alignedFace
                        )

                if (
                    embedding == null
                ) {

                    Log.e(
                        TAG,
                        "ArcFace returned null embedding"
                    )

                    return@withContext null
                }

                Log.d(
                    TAG,
                    "Embedding returned"
                )

                Log.d(
                    TAG,
                    "Embedding size = " +
                            embedding.size
                )

                // =================================================
                // VERIFY 512D
                // =================================================

                if (
                    embedding.size != 512
                ) {

                    Log.e(
                        TAG,
                        "Unexpected embedding size = " +
                                embedding.size
                    )

                    return@withContext null
                }

                // =================================================
                // LOG FIRST VALUES
                // =================================================

                Log.d(
                    TAG,
                    "First 10 embedding values = " +
                            embedding
                                .take(10)
                                .joinToString(", ")
                )

                // =================================================
                // VERIFY VECTOR
                // =================================================

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
                    )

                Log.d(
                    TAG,
                    "Embedding L2 norm = " +
                            norm
                )

                // =================================================
                // SUCCESS
                // =================================================

                Log.d(
                    TAG,
                    "=========================================="
                )

                Log.d(
                    TAG,
                    "SUCCESS: 512D EMBEDDING GENERATED"
                )

                Log.d(
                    TAG,
                    "Selected face is ready for swap pipeline"
                )

                Log.d(
                    TAG,
                    "=========================================="
                )

                embedding

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "ArcFace test failed",
                    e
                )

                null

            } finally {

                // =================================================
                // RECYCLE ORIGINAL BITMAP
                // =================================================

                try {

                    if (
                        !bitmap.isRecycled
                    ) {

                        bitmap.recycle()
                    }

                } catch (
                    _: Exception
                ) {
                }
            }
        }

    // =========================================================
    // DOWNLOAD BITMAP
    // =========================================================

    private fun downloadBitmap(
        imageUrl: String
    ): Bitmap? {

        var connection:
                HttpURLConnection? =
            null

        try {

            Log.d(
                TAG,
                "Downloading image..."
            )

            val url =
                URL(
                    imageUrl
                )

            connection =
                url.openConnection()
                        as HttpURLConnection

            connection.connectTimeout =
                15000

            connection.readTimeout =
                20000

            connection.requestMethod =
                "GET"

            connection.instanceFollowRedirects =
                true

            connection.connect()

            val responseCode =
                connection.responseCode

            Log.d(
                TAG,
                "HTTP response = " +
                        responseCode
            )

            if (
                responseCode !in 200..299
            ) {

                Log.e(
                    TAG,
                    "Image download failed: HTTP " +
                            responseCode
                )

                return null
            }

            connection
                .inputStream
                .use { input ->

                    return android.graphics
                        .BitmapFactory
                        .decodeStream(
                            input
                        )
                }

        } finally {

            connection?.disconnect()
        }
    }

    // =========================================================
    // ALIGN FACE
    // =========================================================

    private fun alignFace(
        source: Bitmap,
        face: FaceDetection
    ): Bitmap? {

        if (
            face.landmarks.size < 5
        ) {

            return null
        }

        // =====================================================
        // SOURCE LANDMARKS
        // =====================================================

        val sourcePoints =
            Array(
                5
            ) { index ->

                floatArrayOf(
                    face.landmarks[index].first,
                    face.landmarks[index].second
                )
            }

        // =====================================================
        // LOG LANDMARKS
        // =====================================================

        Log.d(
            TAG,
            "Source landmark 0 = " +
                    sourcePoints[0].contentToString()
        )

        Log.d(
            TAG,
            "Source landmark 1 = " +
                    sourcePoints[1].contentToString()
        )

        Log.d(
            TAG,
            "Source landmark 2 = " +
                    sourcePoints[2].contentToString()
        )

        Log.d(
            TAG,
            "Source landmark 3 = " +
                    sourcePoints[3].contentToString()
        )

        Log.d(
            TAG,
            "Source landmark 4 = " +
                    sourcePoints[4].contentToString()
        )

        // =====================================================
        // ESTIMATE SIMILARITY TRANSFORM
        // =====================================================

        val transform =
            estimateSimilarityTransform(
                sourcePoints,
                REFERENCE_POINTS
            )

        if (
            transform == null
        ) {

            Log.e(
                TAG,
                "Could not estimate face transform"
            )

            return null
        }

        // =====================================================
        // CREATE OUTPUT
        // =====================================================

        val aligned =
            Bitmap.createBitmap(
                FACE_SIZE,
                FACE_SIZE,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            android.graphics.Canvas(
                aligned
            )

        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG or
                        android.graphics.Paint.FILTER_BITMAP_FLAG
            )

        // =====================================================
        // APPLY TRANSFORM
        // =====================================================

        val matrix =
            Matrix()

        matrix.setValues(
            floatArrayOf(

                transform[0],
                transform[1],
                transform[2],

                transform[3],
                transform[4],
                transform[5],

                transform[6],
                transform[7],
                transform[8]
            )
        )

        // =====================================================
        // DRAW
        // =====================================================

        canvas.drawBitmap(
            source,
            matrix,
            paint
        )

        return aligned
    }

    // =========================================================
    // SIMILARITY TRANSFORM
    // =========================================================
    //
    // Returns 3x3 affine matrix:
    //
    // [ a  -b  tx ]
    // [ b   a  ty ]
    // [ 0   0   1 ]
    //
    // =========================================================

    private fun estimateSimilarityTransform(
        source: Array<FloatArray>,
        target: Array<FloatArray>
    ): FloatArray? {

        if (
            source.size != 5 ||
            target.size != 5
        ) {

            return null
        }

        // =====================================================
        // CENTROIDS
        // =====================================================

        var sourceCx =
            0.0

        var sourceCy =
            0.0

        var targetCx =
            0.0

        var targetCy =
            0.0

        for (
        i in 0 until 5
        ) {

            sourceCx +=
                source[i][0]

            sourceCy +=
                source[i][1]

            targetCx +=
                target[i][0]

            targetCy +=
                target[i][1]
        }

        sourceCx /=
            5.0

        sourceCy /=
            5.0

        targetCx /=
            5.0

        targetCy /=
            5.0

        // =====================================================
        // CENTERED COORDINATES
        // =====================================================

        var numA =
            0.0

        var numB =
            0.0

        var den =
            0.0

        for (
        i in 0 until 5
        ) {

            val sx =
                source[i][0] -
                        sourceCx.toFloat()

            val sy =
                source[i][1] -
                        sourceCy.toFloat()

            val tx =
                target[i][0] -
                        targetCx.toFloat()

            val ty =
                target[i][1] -
                        targetCy.toFloat()

            numA +=
                sx * tx +
                        sy * ty

            numB +=
                sx * ty -
                        sy * tx

            den +=
                sx * sx +
                        sy * sy
        }

        if (
            den <= 0.000001
        ) {

            return null
        }

        // =====================================================
        // ROTATION + SCALE
        // =====================================================

        val a =
            numA / den

        val b =
            numB / den

        // =====================================================
        // TRANSLATION
        // =====================================================

        val tx =
            targetCx -
                    (
                            a *
                                    sourceCx -
                                    b *
                                    sourceCy
                            )

        val ty =
            targetCy -
                    (
                            b *
                                    sourceCx +
                                    a *
                                    sourceCy
                            )

        // =====================================================
        // MATRIX
        // =====================================================

        return floatArrayOf(

            a.toFloat(),
            (-b).toFloat(),
            tx.toFloat(),

            b.toFloat(),
            a.toFloat(),
            ty.toFloat(),

            0f,
            0f,
            1f
        )
    }
}