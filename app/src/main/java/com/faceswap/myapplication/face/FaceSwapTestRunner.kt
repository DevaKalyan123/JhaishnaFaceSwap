package com.faceswap.myapplication.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FaceSwapTestRunner {

    private const val TAG = "FACE_SWAP_TEST"

    suspend fun testFromAsset(
        context: Context,
        sourceEmbedding: FloatArray,
        faceDetector: FaceDetector,
        faceSwapper: FaceSwapper
    ): Bitmap? = withContext(Dispatchers.Default) {

        try {

            Log.d(TAG, "==========================================")
            Log.d(TAG, "STARTING STATIC FACE SWAP TEST")
            Log.d(TAG, "==========================================")

            // ---------------------------------------------------------
            // 1. CHECK SOURCE EMBEDDING
            // ---------------------------------------------------------

            if (sourceEmbedding.size != 512) {
                Log.e(
                    TAG,
                    "Invalid source embedding size = ${sourceEmbedding.size}"
                )
                return@withContext null
            }

            Log.d(
                TAG,
                "Source embedding = 512D"
            )

            // ---------------------------------------------------------
            // 2. CHECK SWAPPER
            // ---------------------------------------------------------

            if (!faceSwapper.isReady()) {
                Log.e(
                    TAG,
                    "FaceSwapper is NOT ready"
                )
                return@withContext null
            }

            Log.d(
                TAG,
                "FaceSwapper = READY"
            )

            // ---------------------------------------------------------
            // 3. LOAD TARGET IMAGE
            // ---------------------------------------------------------

            Log.d(
                TAG,
                "Loading test_target.jpg..."
            )

            val targetBitmap = context.assets
                .open("test_target.jpg")
                .use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }

            if (targetBitmap == null) {

                Log.e(
                    TAG,
                    "Could not decode test_target.jpg"
                )

                return@withContext null
            }

            Log.d(
                TAG,
                "Target image loaded = " +
                        "${targetBitmap.width}x${targetBitmap.height}"
            )

            // ---------------------------------------------------------
            // 4. RESIZE TARGET
            // ---------------------------------------------------------

            val resizedTarget =
                resizeForTesting(targetBitmap)

            if (resizedTarget !== targetBitmap) {
                targetBitmap.recycle()
            }

            Log.d(
                TAG,
                "Target resized = " +
                        "${resizedTarget.width}x${resizedTarget.height}"
            )

            // ---------------------------------------------------------
            // 5. FACE DETECTION
            // ---------------------------------------------------------

            Log.d(
                TAG,
                "Detecting target face..."
            )

            val faces =
                faceDetector.detectFromBitmap(resizedTarget)

            Log.d(
                TAG,
                "Target faces detected = ${faces.size}"
            )

            if (faces.isEmpty()) {

                Log.e(
                    TAG,
                    "No face detected in target image"
                )

                resizedTarget.recycle()

                return@withContext null
            }

            // ---------------------------------------------------------
            // 6. SELECT BEST FACE
            // ---------------------------------------------------------

            val targetFace =
                faces.maxByOrNull { it.score }

            if (targetFace == null) {

                Log.e(
                    TAG,
                    "Could not select target face"
                )

                resizedTarget.recycle()

                return@withContext null
            }

            Log.d(
                TAG,
                "Target face selected"
            )

            Log.d(
                TAG,
                "Face score = ${targetFace.score}"
            )

            Log.d(
                TAG,
                "Face box = ${targetFace.box}"
            )

            Log.d(
                TAG,
                "Landmarks = ${targetFace.landmarks}"
            )

            // ---------------------------------------------------------
            // 7. RUN INSWAPPER
            // ---------------------------------------------------------

            Log.d(
                TAG,
                "Calling FaceSwapper.swap()..."
            )

            val result =
                faceSwapper.swap(
                    targetBitmap = resizedTarget,
                    targetFace = targetFace,
                    sourceEmbedding = sourceEmbedding
                )

            // ---------------------------------------------------------
            // 8. RESULT
            // ---------------------------------------------------------

            if (result == null) {

                Log.e(
                    TAG,
                    "FACE SWAP FAILED"
                )

                resizedTarget.recycle()

                return@withContext null
            }

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
                "Result = ${result.width}x${result.height}"
            )

            Log.d(
                TAG,
                "=========================================="
            )

            resizedTarget.recycle()

            result

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Static face swap test failed",
                e
            )

            null
        }
    }

    private fun resizeForTesting(
        bitmap: Bitmap
    ): Bitmap {

        val maxSize = 1280

        val width = bitmap.width
        val height = bitmap.height

        if (
            width <= maxSize &&
            height <= maxSize
        ) {
            return bitmap
        }

        val scale =
            minOf(
                maxSize.toFloat() / width,
                maxSize.toFloat() / height
            )

        val newWidth =
            (width * scale).toInt()

        val newHeight =
            (height * scale).toInt()

        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        )
    }
}