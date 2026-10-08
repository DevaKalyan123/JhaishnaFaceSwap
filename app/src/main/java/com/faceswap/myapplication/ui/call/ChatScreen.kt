package com.faceswap.myapplication.ui.call

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import coil.compose.rememberAsyncImagePainter

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response

import org.json.JSONObject

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


// =============================================================
// MESSAGE MODEL
// =============================================================

data class MessageData(

    val id: String = "",

    val text: String = "",

    val sender: String = "",

    val timestamp: Long = 0L,

    val image: String = "",

    val audio: String = "",

    val fileName: String = "",

    val fileType: String = "",

    val seen: Boolean = false
)


// =============================================================
// CLOUDINARY CONFIG
// =============================================================

private const val CLOUDINARY_CLOUD_NAME =
    "orrrn2qq"

private const val CLOUDINARY_UPLOAD_PRESET =
    "jhaishna_faceswap"


// =============================================================
// CHAT SCREEN
// =============================================================

@Composable
fun ChatScreen(
    friendUid: String,
    friendName: String
) {

    val context =
        LocalContext.current

    val firestore =
        FirebaseFirestore.getInstance()

    val currentUser =
        FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid
            ?: return


    // =========================================================
    // STATES
    // =========================================================

    var messageText by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf(
            listOf<MessageData>()
        )
    }

    var isTyping by remember {
        mutableStateOf(false)
    }

    var userStatus by remember {
        mutableStateOf("Loading...")
    }

    var isUploading by remember {
        mutableStateOf(false)
    }


    val listState =
        rememberLazyListState()


    // =========================================================
    // CHAT ID
    // =========================================================

    val chatId =

        if (
            currentUser < friendUid
        ) {

            "chat_${currentUser}_$friendUid"

        } else {

            "chat_${friendUid}_$currentUser"
        }


    // =========================================================
    // IMAGE PICKER
    // =========================================================

    val imagePicker =

        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }


            uploadImageToCloudinary(

                context = context,

                imageUri = uri,

                chatId = chatId,

                user = currentUser,

                onStart = {

                    isUploading = true
                },

                onSuccess = {

                    isUploading = false

                    Toast.makeText(
                        context,
                        "Photo sent successfully ✓",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onError = { error ->

                    isUploading = false

                    Toast.makeText(
                        context,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }


    // =========================================================
    // FILE PICKER
    // =========================================================

    val filePicker =

        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.GetContent()

        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }


            uploadFileToCloudinary(

                context = context,

                fileUri = uri,

                chatId = chatId,

                user = currentUser,

                onStart = {

                    isUploading = true
                },

                onSuccess = {

                    isUploading = false

                    Toast.makeText(
                        context,
                        "File sent successfully ✓",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onError = { error ->

                    isUploading = false

                    Toast.makeText(
                        context,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }


    // =========================================================
    // USER STATUS
    // =========================================================

    DisposableEffect(
        friendUid
    ) {

        val registration =

            firestore
                .collection("users")
                .document(friendUid)
                .addSnapshotListener { value, error ->

                    if (error != null) {

                        userStatus =
                            "Offline"

                        return@addSnapshotListener
                    }


                    val online =

                        value
                            ?.getBoolean("online")
                            ?: false


                    val lastSeen =

                        value
                            ?.getLong("lastSeen")
                            ?: 0L


                    userStatus =

                        if (online) {

                            "Online 🟢"

                        } else if (
                            lastSeen > 0L
                        ) {

                            val time =

                                SimpleDateFormat(
                                    "hh:mm a",
                                    Locale.getDefault()
                                ).format(
                                    Date(lastSeen)
                                )

                            "Last seen $time"

                        } else {

                            "Offline"
                        }
                }


        onDispose {

            registration.remove()
        }
    }


    // =========================================================
    // LOAD MESSAGES
    // =========================================================

    DisposableEffect(
        chatId
    ) {

        val registration =

            firestore
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { value, error ->

                    if (error != null) {

                        Toast.makeText(
                            context,
                            "Failed to load messages: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }


                    val list =

                        value
                            ?.documents
                            ?.mapNotNull { document ->

                                document
                                    .toObject(
                                        MessageData::class.java
                                    )
                                    ?.copy(
                                        id =
                                            document.id
                                    )
                            }
                            ?: emptyList()


                    messages =
                        list


                    // -----------------------------------------
                    // MARK AS SEEN
                    // -----------------------------------------

                    list
                        .filter {

                            it.sender != currentUser &&
                                    !it.seen
                        }
                        .forEach { message ->

                            firestore
                                .collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .document(message.id)
                                .update(
                                    "seen",
                                    true
                                )
                        }
                }


        onDispose {

            registration.remove()
        }
    }


    // =========================================================
    // AUTO SCROLL
    // =========================================================

    LaunchedEffect(
        messages.size
    ) {

        if (
            messages.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }


    // =========================================================
    // TYPING
    // =========================================================

    DisposableEffect(
        chatId
    ) {

        val registration =

            firestore
                .collection("typing")
                .document(chatId)
                .addSnapshotListener { value, _ ->

                    isTyping =

                        value
                            ?.getBoolean(
                                friendUid
                            )
                            ?: false
                }


        onDispose {

            registration.remove()
        }
    }


    // =========================================================
    // MAIN UI
    // =========================================================

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xFFF5F5F5)
                )
    ) {


        // =====================================================
        // HEADER
        // =====================================================

        Column(

            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(

                text =
                    friendName,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )


            Text(

                text =
                    userStatus,

                color =
                    Color.Gray
            )


            if (isTyping) {

                Text(

                    text =
                        "typing...",

                    color =
                        Color.Gray,

                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        )
                )
            }
        }


        // =====================================================
        // MESSAGE LIST
        // =====================================================

        LazyColumn(

            state =
                listState,

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
        ) {

            items(

                items =
                    messages,

                key = {
                    it.id
                }

            ) { message ->


                val isMe =
                    message.sender ==
                            currentUser


                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            ),

                    horizontalArrangement =

                        if (isMe) {

                            Arrangement.End

                        } else {

                            Arrangement.Start
                        }
                ) {


                    Card(

                        colors =

                            CardDefaults
                                .cardColors(

                                    containerColor =

                                        if (isMe) {

                                            Color(
                                                0xFFD1E7DD
                                            )

                                        } else {

                                            Color.White
                                        }
                                ),

                        modifier =
                            Modifier.padding(
                                4.dp
                            )
                    ) {


                        Column(

                            modifier =
                                Modifier.padding(
                                    10.dp
                                )
                        ) {


                            // =================================
                            // IMAGE MESSAGE
                            // =================================

                            if (
                                message.image
                                    .isNotBlank()
                            ) {

                                Image(

                                    painter =
                                        rememberAsyncImagePainter(
                                            model =
                                                message.image
                                        ),

                                    contentDescription =
                                        "Photo",

                                    modifier =
                                        Modifier
                                            .size(
                                                220.dp
                                            ),

                                    contentScale =
                                        ContentScale.Crop
                                )


                                // =================================
                                // FILE MESSAGE
                                // =================================

                            } else if (

                                message.fileType
                                    .isNotBlank() &&

                                message.text
                                    .startsWith(
                                        "http"
                                    )

                            ) {

                                Button(

                                    onClick = {

                                        openFile(
                                            context,
                                            message.text
                                        )
                                    }

                                ) {

                                    Text(

                                        text =
                                            "📎 " +
                                                    if (
                                                        message.fileName
                                                            .isNotBlank()
                                                    ) {

                                                        message.fileName

                                                    } else {

                                                        "Open File"
                                                    }
                                    )
                                }


                                // =================================
                                // OLD FILE SUPPORT
                                // =================================

                            } else if (

                                message.text
                                    .startsWith(
                                        "http"
                                    )

                            ) {

                                Button(

                                    onClick = {

                                        openFile(
                                            context,
                                            message.text
                                        )
                                    }

                                ) {

                                    Text(
                                        "📎 Open File"
                                    )
                                }


                                // =================================
                                // TEXT
                                // =================================

                            } else {

                                Text(
                                    text =
                                        message.text
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )


                            // =================================
                            // TIME + SEEN
                            // =================================

                            Row {

                                Text(

                                    text =

                                        SimpleDateFormat(
                                            "hh:mm a",
                                            Locale.getDefault()
                                        ).format(
                                            Date(
                                                message.timestamp
                                            )
                                        ),

                                    color =
                                        Color.Gray
                                )


                                if (isMe) {

                                    Text(

                                        text =

                                            if (
                                                message.seen
                                            ) {

                                                " ✔✔"

                                            } else {

                                                " ✔"
                                            },

                                        color =

                                            if (
                                                message.seen
                                            ) {

                                                Color.Blue

                                            } else {

                                                Color.Gray
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        // =====================================================
        // UPLOAD PROGRESS
        // =====================================================

        if (isUploading) {

            LinearProgressIndicator(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp
                        )
            )
        }


        // =====================================================
        // INPUT
        // =====================================================

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        8.dp
                    ),

            verticalAlignment =
                androidx.compose.ui.Alignment
                    .CenterVertically
        ) {


            // =================================================
            // TEXT FIELD
            // =================================================

            TextField(

                value =
                    messageText,

                onValueChange = { value ->

                    messageText =
                        value


                    firestore
                        .collection("typing")
                        .document(chatId)
                        .set(

                            mapOf(

                                currentUser to
                                        value.isNotEmpty()
                            )
                        )
                },

                modifier =
                    Modifier.weight(
                        1f
                    ),

                singleLine =
                    true,

                placeholder = {

                    Text(
                        "Message"
                    )
                }
            )


            Spacer(
                modifier =
                    Modifier.width(
                        4.dp
                    )
            )


            // =================================================
            // SEND
            // =================================================

            Button(

                enabled =

                    messageText
                        .isNotBlank() &&
                            !isUploading,

                onClick = {

                    val textToSend =
                        messageText.trim()


                    if (
                        textToSend.isBlank()
                    ) {

                        return@Button
                    }


                    firestore
                        .collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .add(

                            mapOf(

                                "text" to
                                        textToSend,

                                "sender" to
                                        currentUser,

                                "timestamp" to
                                        System
                                            .currentTimeMillis(),

                                "seen" to
                                        false,

                                "image" to
                                        "",

                                "audio" to
                                        "",

                                "fileName" to
                                        "",

                                "fileType" to
                                        ""
                            )
                        )

                        .addOnSuccessListener {

                            messageText =
                                ""


                            firestore
                                .collection("typing")
                                .document(chatId)
                                .set(

                                    mapOf(

                                        currentUser to
                                                false
                                    )
                                )
                        }

                        .addOnFailureListener { error ->

                            Toast.makeText(

                                context,

                                "Message failed: ${error.message}",

                                Toast.LENGTH_LONG

                            ).show()
                        }
                }

            ) {

                Text(
                    "Send"
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        4.dp
                    )
            )


            // =================================================
            // IMG
            // =================================================

            Button(

                enabled =
                    !isUploading,

                onClick = {

                    imagePicker.launch(
                        "image/*"
                    )
                }

            ) {

                Text(
                    "IMG"
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        4.dp
                    )
            )


            // =================================================
            // FILE
            // =================================================

            Button(

                enabled =
                    !isUploading,

                onClick = {

                    filePicker.launch(
                        "*/*"
                    )
                }

            ) {

                Text(
                    "FILE"
                )
            }
        }
    }
}


// =============================================================
// CLOUDINARY IMAGE UPLOAD
// =============================================================

private fun uploadImageToCloudinary(

    context: Context,

    imageUri: Uri,

    chatId: String,

    user: String,

    onStart: () -> Unit,

    onSuccess: () -> Unit,

    onError: (String) -> Unit

) {

    onStart()


    try {

        val inputStream =

            context
                .contentResolver
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


        val tempFile =

            File(
                context.cacheDir,
                "chat_image_${UUID.randomUUID()}.jpg"
            )


        FileOutputStream(
            tempFile
        ).use { output ->

            inputStream.use { input ->

                input.copyTo(
                    output
                )
            }
        }


        // =====================================================
        // CLOUDINARY IMAGE ENDPOINT
        // =====================================================

        val uploadUrl =

            "https://api.cloudinary.com/v1_1/" +
                    CLOUDINARY_CLOUD_NAME +
                    "/image/upload"


        val requestBody =

            MultipartBody
                .Builder()
                .setType(
                    MultipartBody.FORM
                )

                .addFormDataPart(

                    "file",

                    tempFile.name,

                    tempFile
                        .asRequestBody(
                            "image/*"
                                .toMediaTypeOrNull()
                        )
                )

                .addFormDataPart(

                    "upload_preset",

                    CLOUDINARY_UPLOAD_PRESET
                )

                .build()


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

                        tempFile.delete()

                        onError(

                            "Cloudinary image upload failed: " +
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


                            if (
                                !response.isSuccessful
                            ) {

                                tempFile.delete()

                                onError(

                                    "Cloudinary image upload failed: " +
                                            response.code +
                                            " " +
                                            (
                                                    responseBody
                                                        ?: ""
                                                    )
                                )

                                return
                            }


                            if (
                                responseBody == null
                            ) {

                                tempFile.delete()

                                onError(
                                    "Empty Cloudinary response"
                                )

                                return
                            }


                            try {

                                val json =

                                    JSONObject(
                                        responseBody
                                    )


                                val imageUrl =

                                    json.getString(
                                        "secure_url"
                                    )


                                Log.d(

                                    "CLOUDINARY_CHAT",

                                    "IMAGE URL = $imageUrl"
                                )


                                // =================================
                                // SAVE TO FIRESTORE
                                // =================================

                                val firestore =

                                    FirebaseFirestore
                                        .getInstance()


                                val message =

                                    hashMapOf(

                                        "text" to
                                                "",

                                        "sender" to
                                                user,

                                        "timestamp" to
                                                System
                                                    .currentTimeMillis(),

                                        "seen" to
                                                false,

                                        "image" to
                                                imageUrl,

                                        "audio" to
                                                "",

                                        "fileName" to
                                                tempFile.name,

                                        "fileType" to
                                                "image"
                                    )


                                firestore

                                    .collection(
                                        "chats"
                                    )

                                    .document(
                                        chatId
                                    )

                                    .collection(
                                        "messages"
                                    )

                                    .add(
                                        message
                                    )

                                    .addOnSuccessListener {

                                        tempFile.delete()

                                        Log.d(

                                            "CLOUDINARY_CHAT",

                                            "IMAGE MESSAGE SAVED"
                                        )

                                        onSuccess()
                                    }

                                    .addOnFailureListener { error ->

                                        tempFile.delete()

                                        onError(

                                            "Image uploaded, but Firestore save failed: " +
                                                    (
                                                            error.message
                                                                ?: ""
                                                            )
                                        )
                                    }


                            } catch (
                                e: Exception
                            ) {

                                tempFile.delete()

                                onError(

                                    "Invalid Cloudinary image response: " +
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

    } catch (
        e: Exception
    ) {

        onError(

            "Image upload error: " +
                    (
                            e.message
                                ?: "Unknown error"
                            )
        )
    }
}


// =============================================================
// CLOUDINARY FILE UPLOAD
// =============================================================

private fun uploadFileToCloudinary(

    context: Context,

    fileUri: Uri,

    chatId: String,

    user: String,

    onStart: () -> Unit,

    onSuccess: () -> Unit,

    onError: (String) -> Unit

) {

    onStart()


    try {

        val originalFileName =

            getFileName(
                context,
                fileUri
            )
                ?: "File_${System.currentTimeMillis()}"


        val inputStream =

            context
                .contentResolver
                .openInputStream(
                    fileUri
                )


        if (
            inputStream == null
        ) {

            onError(
                "Unable to read selected file"
            )

            return
        }


        val safeName =

            originalFileName
                .replace(
                    Regex(
                        "[^A-Za-z0-9._-]"
                    ),
                    "_"
                )


        val tempFile =

            File(

                context.cacheDir,

                "chat_file_${UUID.randomUUID()}_$safeName"
            )


        FileOutputStream(
            tempFile
        ).use { output ->

            inputStream.use { input ->

                input.copyTo(
                    output
                )
            }
        }


        // =====================================================
        // CLOUDINARY AUTO ENDPOINT
        // =====================================================

        val uploadUrl =

            "https://api.cloudinary.com/v1_1/" +
                    CLOUDINARY_CLOUD_NAME +
                    "/auto/upload"


        val requestBody =

            MultipartBody
                .Builder()
                .setType(
                    MultipartBody.FORM
                )

                .addFormDataPart(

                    "file",

                    safeName,

                    tempFile
                        .asRequestBody(
                            "application/octet-stream"
                                .toMediaTypeOrNull()
                        )
                )

                .addFormDataPart(

                    "upload_preset",

                    CLOUDINARY_UPLOAD_PRESET
                )

                .build()


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


        Log.d(

            "CLOUDINARY_CHAT",

            "Uploading file = $safeName"
        )


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

                        tempFile.delete()

                        onError(

                            "Cloudinary file upload failed: " +
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


                            if (
                                !response.isSuccessful
                            ) {

                                tempFile.delete()

                                onError(

                                    "Cloudinary file upload failed: " +
                                            response.code +
                                            " " +
                                            (
                                                    responseBody
                                                        ?: ""
                                                    )
                                )

                                return
                            }


                            if (
                                responseBody == null
                            ) {

                                tempFile.delete()

                                onError(
                                    "Empty Cloudinary response"
                                )

                                return
                            }


                            try {

                                val json =

                                    JSONObject(
                                        responseBody
                                    )


                                val fileUrl =

                                    json.getString(
                                        "secure_url"
                                    )


                                Log.d(

                                    "CLOUDINARY_CHAT",

                                    "FILE URL = $fileUrl"
                                )


                                // =================================
                                // SAVE FILE MESSAGE
                                // =================================

                                val firestore =

                                    FirebaseFirestore
                                        .getInstance()


                                val message =

                                    hashMapOf(

                                        "text" to
                                                fileUrl,

                                        "sender" to
                                                user,

                                        "timestamp" to
                                                System
                                                    .currentTimeMillis(),

                                        "seen" to
                                                false,

                                        "image" to
                                                "",

                                        "audio" to
                                                "",

                                        "fileName" to
                                                originalFileName,

                                        "fileType" to
                                                "file"
                                    )


                                firestore

                                    .collection(
                                        "chats"
                                    )

                                    .document(
                                        chatId
                                    )

                                    .collection(
                                        "messages"
                                    )

                                    .add(
                                        message
                                    )

                                    .addOnSuccessListener {

                                        tempFile.delete()

                                        Log.d(

                                            "CLOUDINARY_CHAT",

                                            "FILE MESSAGE SAVED"
                                        )

                                        onSuccess()
                                    }

                                    .addOnFailureListener { error ->

                                        tempFile.delete()

                                        onError(

                                            "File uploaded, but Firestore save failed: " +
                                                    (
                                                            error.message
                                                                ?: ""
                                                            )
                                        )
                                    }


                            } catch (
                                e: Exception
                            ) {

                                tempFile.delete()

                                onError(

                                    "Invalid Cloudinary file response: " +
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

    } catch (
        e: Exception
    ) {

        onError(

            "File upload error: " +
                    (
                            e.message
                                ?: "Unknown error"
                            )
        )
    }
}


// =============================================================
// GET FILE NAME
// =============================================================

private fun getFileName(

    context: Context,

    uri: Uri

): String? {

    return try {

        val cursor =

            context
                .contentResolver
                .query(
                    uri,
                    arrayOf(
                        "_display_name"
                    ),
                    null,
                    null,
                    null
                )


        cursor?.use {

            if (
                it.moveToFirst()
            ) {

                val index =

                    it.getColumnIndex(
                        "_display_name"
                    )


                if (
                    index >= 0
                ) {

                    return it.getString(
                        index
                    )
                }
            }
        }


        uri.path
            ?.substringAfterLast(
                '/'
            )
            ?.takeIf {
                it.isNotBlank()
            }

    } catch (
        e: Exception
    ) {

        null
    }
}


// =============================================================
// OPEN FILE
// =============================================================

private fun openFile(

    context: Context,

    url: String

) {

    try {

        val intent =

            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )


        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )


        context.startActivity(
            intent
        )

    } catch (
        e: Exception
    ) {

        Toast.makeText(

            context,

            "Cannot open this file on this device",

            Toast.LENGTH_SHORT

        ).show()
    }
}