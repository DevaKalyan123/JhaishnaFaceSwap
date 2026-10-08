package com.faceswap.myapplication.ui.upload

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import android.util.Log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

import org.json.JSONObject

import java.io.IOException
import java.util.UUID


// ============================================================
// APP COLORS
// Same styling as your friend's Upload Screen
// ============================================================

private val AppSkyBlue =
    Color(0xFF42A5E8)

private val AppSkyBlueDark =
    Color(0xFF2196E3)

private val AppLightBlue =
    Color(0xFFEAF6FF)

private val AppBackground =
    Color(0xFFF6FBFF)

private val AppText =
    Color(0xFF18212B)

private val AppSecondaryText =
    Color(0xFF6F7C88)

private val AppBorder =
    Color(0xFFD7EAF8)

private val AppWhite =
    Color.White

private val AppGreen =
    Color(0xFF43A047)


// ============================================================
// CLOUDINARY CONFIG
// ============================================================

private const val CLOUDINARY_CLOUD_NAME =
    "orrrn2qq"

private const val CLOUDINARY_UPLOAD_PRESET =
    "jhaishna_faceswap"


// ============================================================
// UPLOAD SCREEN
// ============================================================

@Composable
fun UploadScreen() {

    val context =
        LocalContext.current

    val firestore =
        FirebaseFirestore.getInstance()

    val auth =
        FirebaseAuth.getInstance()


    // ========================================================
    // SELECTED IMAGE
    // ========================================================

    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }


    var bitmap by remember {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }


    // ========================================================
    // UPLOAD STATUS
    // ========================================================

    var isUploading by remember {
        mutableStateOf(false)
    }


    var uploadProgress by remember {
        mutableFloatStateOf(0f)
    }


    // ========================================================
    // UPLOADED IMAGE URL
    // ========================================================

    var uploadedImageUrl by remember {
        mutableStateOf<String?>(null)
    }


    // ========================================================
    // IMAGE PICKER
    // ========================================================

    val launcher =

        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri ->


            if (uri == null) {

                return@rememberLauncherForActivityResult
            }


            imageUri =
                uri


            uploadedImageUrl =
                null


            try {

                val inputStream =

                    context
                        .contentResolver
                        .openInputStream(
                            uri
                        )


                val bytes =
                    inputStream?.readBytes()


                inputStream?.close()


                if (bytes != null) {

                    bitmap =
                        BitmapFactory.decodeByteArray(

                            bytes,

                            0,

                            bytes.size
                        )
                }


            } catch (e: Exception) {

                Toast.makeText(

                    context,

                    "Unable to open image",

                    Toast.LENGTH_SHORT

                ).show()
            }
        }


    // ========================================================
    // MAIN SCREEN
    // ========================================================

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    AppBackground
                )
    ) {


        // ====================================================
        // HEADER
        // ====================================================

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        AppWhite
                    )
                    .padding(

                        start = 20.dp,

                        end = 20.dp,

                        top = 18.dp,

                        bottom = 16.dp
                    )
        ) {

            Text(

                text =
                    "Upload Face",

                color =
                    AppText,

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(

                text =
                    "Add your face to connect with people",

                color =
                    AppSecondaryText,

                fontSize =
                    13.sp
            )
        }


        // ====================================================
        // CONTENT
        // ====================================================

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(

                        horizontal = 16.dp,

                        vertical = 14.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            // =================================================
            // UPLOAD CARD
            // =================================================

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                18.dp
                            )
                        )
                        .background(
                            AppWhite
                        )
                        .border(

                            width = 1.dp,

                            color =
                                AppBorder,

                            shape =
                                RoundedCornerShape(
                                    18.dp
                                )
                        )
                        .padding(
                            18.dp
                        ),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {


                // =============================================
                // ICON
                // =============================================

                Box(

                    modifier =
                        Modifier
                            .size(
                                64.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                AppLightBlue
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.PhotoCamera,

                        contentDescription =
                            "Upload Face",

                        tint =
                            AppSkyBlue,

                        modifier =
                            Modifier.size(
                                30.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                // =============================================
                // TITLE
                // =============================================

                Text(

                    text =
                        "Upload Your Face",

                    color =
                        AppText,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )


                Text(

                    text =
                        "Choose a clear photo of your face",

                    color =
                        AppSecondaryText,

                    fontSize =
                        13.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                // =============================================
                // IMAGE PREVIEW
                // =============================================

                if (bitmap != null) {


                    Box(

                        modifier =
                            Modifier
                                .size(
                                    210.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                )
                                .background(
                                    AppLightBlue
                                )
                                .border(

                                    width = 1.dp,

                                    color =
                                        AppBorder,

                                    shape =
                                        RoundedCornerShape(
                                            16.dp
                                        )
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Image(

                            bitmap =
                                bitmap!!.asImageBitmap(),

                            contentDescription =
                                "Selected face",

                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(
                                            16.dp
                                        )
                                    ),

                            contentScale =
                                ContentScale.Crop
                        )
                    }


                } else {


                    // =========================================
                    // EMPTY IMAGE AREA
                    // =========================================

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    210.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                )
                                .background(
                                    AppLightBlue
                                )
                                .border(

                                    width = 1.dp,

                                    color =
                                        AppBorder,

                                    shape =
                                        RoundedCornerShape(
                                            16.dp
                                        )
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Image,

                                contentDescription =
                                    "No image",

                                tint =
                                    AppSkyBlue,

                                modifier =
                                    Modifier.size(
                                        45.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )


                            Text(

                                text =
                                    "No image selected",

                                color =
                                    AppSecondaryText,

                                fontSize =
                                    14.sp
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                // =============================================
                // PICK IMAGE BUTTON
                // =============================================

                OutlinedButton(

                    enabled =
                        !isUploading,

                    onClick = {

                        launcher.launch(
                            "image/*"
                        )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                48.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),

                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(

                                contentColor =
                                    AppSkyBlue
                            ),

                    border =
                        BorderStroke(

                            width = 1.dp,

                            color =
                                AppSkyBlue
                        )
                ) {


                    Icon(

                        imageVector =
                            Icons.Default.PhotoCamera,

                        contentDescription =
                            "Pick Image",

                        modifier =
                            Modifier.size(
                                20.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )


                    Text(

                        text =

                            if (
                                bitmap == null
                            ) {

                                "Pick Image"

                            } else {

                                "Change Image"
                            },

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                // =============================================
                // UPLOAD BUTTON
                // =============================================

                Button(

                    enabled =

                        imageUri != null &&
                                !isUploading,

                    onClick = {


                        val uri =
                            imageUri
                                ?: return@Button


                        val userId =
                            auth
                                .currentUser
                                ?.uid


                        if (
                            userId == null
                        ) {

                            Toast.makeText(

                                context,

                                "User not logged in",

                                Toast.LENGTH_SHORT

                            ).show()


                            return@Button
                        }


                        // -------------------------------------
                        // START UPLOAD
                        // -------------------------------------

                        isUploading =
                            true


                        uploadProgress =
                            0f


                        uploadedImageUrl =
                            null


                        // -------------------------------------
                        // CLOUDINARY UPLOAD
                        // -------------------------------------

                        uploadImageToCloudinary(

                            context =
                                context,

                            imageUri =
                                uri,

                            onProgress = {
                                    progress ->

                                uploadProgress =
                                    progress
                            },

                            onSuccess = {

                                    imageUrl,

                                    publicId ->


                                // =============================
                                // IMAGE ID
                                // =============================

                                val imageId =

                                    UUID
                                        .randomUUID()
                                        .toString()


                                // =============================
                                // IMAGE HISTORY
                                // =============================

                                val imageData =

                                    hashMapOf<String, Any>(

                                        "imageId" to
                                                imageId,

                                        "imageUrl" to
                                                imageUrl,

                                        "publicId" to
                                                publicId,

                                        "userId" to
                                                userId,

                                        "uploadedAt" to
                                                FieldValue
                                                    .serverTimestamp()
                                    )


                                // =============================
                                // SAVE IMAGE HISTORY
                                // =============================

                                firestore

                                    .collection(
                                        "users"
                                    )

                                    .document(
                                        userId
                                    )

                                    .collection(
                                        "images"
                                    )

                                    .document(
                                        imageId
                                    )

                                    .set(
                                        imageData
                                    )

                                    .addOnSuccessListener {


                                        // =========================
                                        // SELECTED FACE DATA
                                        // =========================

                                        val userFaceData =

                                            hashMapOf<String, Any>(

                                                "faceUrl" to
                                                        imageUrl,

                                                "selectedFaceUrl" to
                                                        imageUrl,

                                                "selectedFaceImageId" to
                                                        imageId,

                                                "facePublicId" to
                                                        publicId,

                                                "faceUpdatedAt" to
                                                        FieldValue
                                                            .serverTimestamp()
                                            )


                                        // =========================
                                        // SAVE SELECTED FACE
                                        // =========================

                                        firestore

                                            .collection(
                                                "users"
                                            )

                                            .document(
                                                userId
                                            )

                                            .set(

                                                userFaceData,

                                                SetOptions.merge()
                                            )

                                            .addOnSuccessListener {


                                                isUploading =
                                                    false


                                                uploadProgress =
                                                    1f


                                                uploadedImageUrl =
                                                    imageUrl


                                                // =================
                                                // SUCCESS
                                                // =================

                                                Toast.makeText(

                                                    context,

                                                    "Face image uploaded and selected successfully",

                                                    Toast.LENGTH_LONG

                                                ).show()


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "=========================================="
                                                )


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "FACE IMAGE UPLOADED"
                                                )


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "User ID = $userId"
                                                )


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "Image ID = $imageId"
                                                )


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "Face URL = $imageUrl"
                                                )


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "Selected face saved successfully"
                                                )


                                                Log.d(

                                                    "FACE_UPLOAD",

                                                    "=========================================="
                                                )
                                            }

                                            .addOnFailureListener { error ->


                                                isUploading =
                                                    false


                                                Toast.makeText(

                                                    context,

                                                    "Image uploaded but selected face save failed",

                                                    Toast.LENGTH_LONG

                                                ).show()


                                                Log.e(

                                                    "FACE_UPLOAD",

                                                    "Failed to save selected face",

                                                    error
                                                )
                                            }
                                    }

                                    .addOnFailureListener { error ->


                                        isUploading =
                                            false


                                        Toast.makeText(

                                            context,

                                            "Image uploaded but image history save failed",

                                            Toast.LENGTH_LONG

                                        ).show()


                                        Log.e(

                                            "FACE_UPLOAD",

                                            "Failed to save image history",

                                            error
                                        )
                                    }
                            },

                            onError = { error ->


                                isUploading =
                                    false


                                Toast.makeText(

                                    context,

                                    error,

                                    Toast.LENGTH_LONG

                                ).show()


                                Log.e(

                                    "FACE_UPLOAD",

                                    error
                                )
                            }
                        )
                    },


                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                48.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),

                    colors =
                        ButtonDefaults
                            .buttonColors(

                                containerColor =
                                    AppSkyBlue,

                                contentColor =
                                    Color.White,

                                disabledContainerColor =
                                    Color(
                                        0xFFDCEAF4
                                    ),

                                disabledContentColor =
                                    Color(
                                        0xFF8B98A3
                                    )
                            )
                ) {


                    if (isUploading) {


                        CircularProgressIndicator(

                            modifier =
                                Modifier.size(
                                    20.dp
                                ),

                            color =
                                Color.White,

                            strokeWidth =
                                2.dp
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Text(

                            text =
                                "Uploading...",

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )


                    } else {


                        Icon(

                            imageVector =
                                Icons.Default.CloudUpload,

                            contentDescription =
                                "Upload",

                            modifier =
                                Modifier.size(
                                    20.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Text(

                            text =
                                "Upload Face",

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            // =================================================
            // PROGRESS
            // =================================================

            if (isUploading) {


                Column(

                    modifier =
                        Modifier.fillMaxWidth()
                ) {


                    LinearProgressIndicator(

                        progress = {
                            uploadProgress
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        10.dp
                                    )
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Text(

                        text =
                            "${(uploadProgress * 100).toInt()}%",

                        color =
                            AppSecondaryText,

                        fontSize =
                            12.sp
                    )
                }
            }


            // =================================================
            // SUCCESS
            // =================================================

            if (
                uploadedImageUrl != null
            ) {


                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )


                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .background(
                                Color(
                                    0xFFE8F5E9
                                )
                            )
                            .padding(
                                12.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    Icon(

                        imageVector =
                            Icons.Default.CheckCircle,

                        contentDescription =
                            "Success",

                        tint =
                            AppGreen,

                        modifier =
                            Modifier.size(
                                24.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
                            )
                    )


                    Text(

                        text =
                            "Face image selected successfully ✓",

                        color =
                            AppText,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            // =================================================
            // INFORMATION CARD
            // =================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            AppLightBlue
                        )
                        .padding(
                            14.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Box(

                    modifier =
                        Modifier
                            .size(
                                40.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                AppWhite
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {


                    Icon(

                        imageVector =
                            Icons.Default.CheckCircle,

                        contentDescription =
                            "Information",

                        tint =
                            AppGreen,

                        modifier =
                            Modifier.size(
                                22.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )


                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {


                    Text(

                        text =
                            "Use a clear photo",

                        color =
                            AppText,

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(

                        text =
                            "Make sure your face is clearly visible for better results.",

                        color =
                            AppSecondaryText,

                        fontSize =
                            12.sp
                    )
                }
            }
        }
    }
}


// ============================================================
// CLOUDINARY IMAGE UPLOAD
// ============================================================

private fun uploadImageToCloudinary(

    context: android.content.Context,

    imageUri: Uri,

    onProgress: (Float) -> Unit,

    onSuccess: (String, String) -> Unit,

    onError: (String) -> Unit

) {


    // ========================================================
    // CLOUDINARY CONFIG
    // ========================================================

    val cloudName =
        CLOUDINARY_CLOUD_NAME


    val uploadPreset =
        CLOUDINARY_UPLOAD_PRESET


    val contentResolver =
        context.contentResolver


    try {


        // ====================================================
        // READ IMAGE
        // ====================================================

        val inputStream =

            contentResolver
                .openInputStream(
                    imageUri
                )


        if (
            inputStream == null
        ) {

            onError(
                "Unable to read selected image"
            )

            return
        }


        val imageBytes =
            inputStream.readBytes()


        inputStream.close()


        if (
            imageBytes.isEmpty()
        ) {

            onError(
                "Selected image is empty"
            )

            return
        }


        // ====================================================
        // IMAGE REQUEST BODY
        // ====================================================

        val imageBody =

            RequestBody.create(

                "image/*"
                    .toMediaTypeOrNull(),

                imageBytes
            )


        // ====================================================
        // MULTIPART
        // ====================================================

        val requestBody =

            MultipartBody

                .Builder()

                .setType(
                    MultipartBody.FORM
                )

                .addFormDataPart(

                    "file",

                    "face_${UUID.randomUUID()}.jpg",

                    imageBody
                )

                .addFormDataPart(

                    "upload_preset",

                    uploadPreset
                )

                .build()


        // ====================================================
        // CLOUDINARY URL
        // ====================================================

        val uploadUrl =

            "https://api.cloudinary.com/v1_1/" +
                    cloudName +
                    "/image/upload"


        val request =

            Request

                .Builder()

                .url(
                    uploadUrl
                )

                .post(
                    requestBody
                )

                .build()


        val client =
            OkHttpClient()


        // ====================================================
        // START PROGRESS
        // ====================================================

        onProgress(
            0.1f
        )


        // ====================================================
        // UPLOAD
        // ====================================================

        client

            .newCall(
                request
            )

            .enqueue(

                object : Callback {


                    override fun onFailure(

                        call: Call,

                        e: IOException

                    ) {


                        onError(

                            "Cloudinary upload failed: " +
                                    (
                                            e.message
                                                ?: "Unknown error"
                                            )
                        )
                    }


                    override fun onResponse(

                        call: Call,

                        response: Response

                    ) {


                        response.use {


                            val responseBody =

                                response
                                    .body
                                    ?.string()


                            // =================================
                            // FAILED RESPONSE
                            // =================================

                            if (
                                !response.isSuccessful
                            ) {


                                onError(

                                    "Cloudinary upload failed: " +
                                            response.code +
                                            if (
                                                !responseBody.isNullOrBlank()
                                            ) {

                                                "\n$responseBody"

                                            } else {

                                                ""
                                            }
                                )


                                return
                            }


                            if (
                                responseBody == null
                            ) {


                                onError(
                                    "Empty Cloudinary response"
                                )


                                return
                            }


                            try {


                                // =============================
                                // JSON RESPONSE
                                // =============================

                                val json =

                                    JSONObject(
                                        responseBody
                                    )


                                val imageUrl =

                                    json.getString(
                                        "secure_url"
                                    )


                                val publicId =

                                    json.getString(
                                        "public_id"
                                    )


                                // =============================
                                // COMPLETE
                                // =============================

                                onProgress(
                                    1f
                                )


                                onSuccess(

                                    imageUrl,

                                    publicId
                                )


                            } catch (e: Exception) {


                                onError(

                                    "Invalid Cloudinary response: " +
                                            (
                                                    e.message
                                                        ?: ""
                                                    )
                                )
                            }
                        }
                    }
                }
            )


    } catch (e: Exception) {


        onError(

            "Unable to upload image: " +
                    (
                            e.message
                                ?: "Unknown error"
                            )
        )
    }
}