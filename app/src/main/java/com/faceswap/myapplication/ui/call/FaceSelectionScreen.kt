package com.faceswap.myapplication.ui.call

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// =========================================================
// FACE IMAGE DATA
// =========================================================

data class FaceImageItem(
    val imageId: String = "",
    val imageUrl: String = "",
    val publicId: String = ""
)

// =========================================================
// FACE SELECTION SCREEN
// =========================================================

@Composable
fun FaceSelectionScreen(
    friendUid: String,
    friendName: String,
    navController: NavHostController
) {

    val context = LocalContext.current

    val auth =
        FirebaseAuth.getInstance()

    val firestore =
        FirebaseFirestore.getInstance()

    val currentUid =
        auth.currentUser?.uid

    // =========================================================
    // STATE
    // =========================================================

    var images by remember {
        mutableStateOf<List<FaceImageItem>>(
            emptyList()
        )
    }

    var selectedImage by remember {
        mutableStateOf<FaceImageItem?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var creatingCall by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // LOAD USER FACE IMAGES
    // =========================================================

    LaunchedEffect(currentUid) {

        if (currentUid == null) {

            Log.e(
                "FACE_SELECTION",
                "Current user UID is null"
            )

            loading = false

            Toast.makeText(
                context,
                "Please login again",
                Toast.LENGTH_SHORT
            ).show()

            return@LaunchedEffect
        }

        Log.d(
            "FACE_SELECTION",
            "Loading face images for UID = $currentUid"
        )

        firestore
            .collection("users")
            .document(currentUid)
            .collection("images")
            .get()
            .addOnSuccessListener { snapshot ->

                images =
                    snapshot.documents.mapNotNull { document ->

                        val imageUrl =
                            document.getString(
                                "imageUrl"
                            )
                                ?: return@mapNotNull null

                        FaceImageItem(
                            imageId = document.id,

                            imageUrl = imageUrl,

                            publicId =
                                document.getString(
                                    "publicId"
                                )
                                    ?: ""
                        )
                    }

                loading = false

                Log.d(
                    "FACE_SELECTION",
                    "Loaded ${images.size} face images"
                )

                if (images.isEmpty()) {

                    Log.d(
                        "FACE_SELECTION",
                        "No face images found"
                    )
                }
            }
            .addOnFailureListener { error ->

                loading = false

                Log.e(
                    "FACE_SELECTION",
                    "Failed to load face images",
                    error
                )

                Toast.makeText(
                    context,
                    "Failed to load face images",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =========================================================
    // MAIN UI
    // =========================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        // =====================================================
        // TITLE
        // =====================================================

        Text(
            text = "Select Face"
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Choose the face for your call with $friendName"
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // =====================================================
        // LOADING
        // =====================================================

        if (loading) {

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }

        }

        // =====================================================
        // NO IMAGES
        // =====================================================

        else if (images.isEmpty()) {

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "No face images found.\nPlease upload a face first."
                )
            }

        }

        // =====================================================
        // FACE GRID
        // =====================================================

        else {

            LazyVerticalGrid(

                columns =
                    GridCells.Fixed(2),

                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),

                contentPadding =
                    PaddingValues(
                        bottom = 12.dp
                    )
            ) {

                items(
                    items = images,

                    key = {
                        it.imageId
                    }
                ) { image ->

                    val isSelected =
                        selectedImage?.imageId ==
                                image.imageId

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    selectedImage =
                                        image

                                    Log.d(
                                        "FACE_SELECTION",
                                        "Face selected"
                                    )

                                    Log.d(
                                        "FACE_SELECTION",
                                        "Image ID = ${image.imageId}"
                                    )

                                    Log.d(
                                        "FACE_SELECTION",
                                        "Image URL = ${image.imageUrl}"
                                    )
                                }
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            // ---------------------------------
                            // FACE IMAGE
                            // ---------------------------------

                            AsyncImage(

                                model =
                                    image.imageUrl,

                                contentDescription =
                                    "Face",

                                contentScale =
                                    ContentScale.Crop,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            // ---------------------------------
                            // SELECT STATUS
                            // ---------------------------------

                            Text(

                                text =
                                    if (isSelected) {

                                        "✓ Selected"

                                    } else {

                                        "Select"
                                    },

                                modifier =
                                    Modifier.padding(
                                        bottom = 10.dp
                                    )
                            )
                        }
                    }
                }
            }
        }

        // =====================================================
        // SELECTED FACE INFO
        // =====================================================

        selectedImage?.let { face ->

            Text(
                text =
                    "Selected face: ${face.imageId}",

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 8.dp
                        )
            )
        }

        // =====================================================
        // START CALL BUTTON
        // =====================================================

        Button(

            enabled =
                selectedImage != null &&
                        !creatingCall,

            onClick = {

                Log.d(
                    "FACE_SELECTION",
                    "=========================================="
                )

                Log.d(
                    "FACE_SELECTION",
                    "CALL BUTTON CLICKED"
                )

                // ---------------------------------------------
                // CHECK USER
                // ---------------------------------------------

                val currentUserId =
                    currentUid

                if (currentUserId == null) {

                    Log.e(
                        "FACE_SELECTION",
                        "Current UID is null"
                    )

                    Toast.makeText(
                        context,
                        "Please login again",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                // ---------------------------------------------
                // CHECK SELECTED FACE
                // ---------------------------------------------

                val face =
                    selectedImage

                if (face == null) {

                    Log.e(
                        "FACE_SELECTION",
                        "No face selected"
                    )

                    Toast.makeText(
                        context,
                        "Please select a face",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                // ---------------------------------------------
                // PREVENT DOUBLE CLICK
                // ---------------------------------------------

                creatingCall =
                    true

                Log.d(
                    "FACE_SELECTION",
                    "Caller UID = $currentUserId"
                )

                Log.d(
                    "FACE_SELECTION",
                    "Receiver UID = $friendUid"
                )

                Log.d(
                    "FACE_SELECTION",
                    "Selected face URL = ${face.imageUrl}"
                )

                // =================================================
                // SAVE SELECTED FACE TO USER
                // =================================================

                val selectedFaceData =
                    hashMapOf<String, Any>(

                        "faceUrl" to
                                face.imageUrl,

                        "selectedFaceUrl" to
                                face.imageUrl,

                        "selectedFaceImageId" to
                                face.imageId,

                        "facePublicId" to
                                face.publicId,

                        "faceUpdatedAt" to
                                System.currentTimeMillis()
                    )

                firestore
                    .collection("users")
                    .document(currentUserId)
                    .set(
                        selectedFaceData,
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {

                        Log.d(
                            "FACE_SELECTION",
                            "Selected face saved successfully"
                        )

                        // =========================================
                        // CREATE DETERMINISTIC AGORA CHANNEL
                        // =========================================

                        val channelName =
                            listOf(
                                currentUserId,
                                friendUid
                            )
                                .sorted()
                                .joinToString("_")

                        // =========================================
                        // CREATE UNIQUE CALL ID
                        // =========================================

                        val callId =
                            "${currentUserId}_${friendUid}_${System.currentTimeMillis()}"

                        // =========================================
                        // CALLER NAME
                        // =========================================

                        val callerName =
                            auth.currentUser
                                ?.displayName
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: auth.currentUser
                                    ?.email
                                    ?.substringBefore("@")
                                ?: "User"

                        // =========================================
                        // CALL DATA
                        // =========================================

                        val callData =
                            hashMapOf<String, Any>(

                                "callerId" to
                                        currentUserId,

                                "callerName" to
                                        callerName,

                                "receiverId" to
                                        friendUid,

                                "channelName" to
                                        channelName,

                                "faceUrl" to
                                        face.imageUrl,

                                "status" to
                                        "calling",

                                "timestamp" to
                                        System.currentTimeMillis()
                            )

                        Log.d(
                            "FACE_SELECTION",
                            "=========================================="
                        )

                        Log.d(
                            "FACE_SELECTION",
                            "CREATING CALL"
                        )

                        Log.d(
                            "FACE_SELECTION",
                            "Call ID = $callId"
                        )

                        Log.d(
                            "FACE_SELECTION",
                            "Channel = $channelName"
                        )

                        Log.d(
                            "FACE_SELECTION",
                            "Caller = $currentUserId"
                        )

                        Log.d(
                            "FACE_SELECTION",
                            "Receiver = $friendUid"
                        )

                        Log.d(
                            "FACE_SELECTION",
                            "Face URL = ${face.imageUrl}"
                        )

                        // =========================================
                        // CREATE FIRESTORE CALL DOCUMENT
                        // =========================================

                        firestore
                            .collection("calls")
                            .document(callId)
                            .set(callData)
                            .addOnSuccessListener {

                                Log.d(
                                    "FACE_SELECTION",
                                    "=========================================="
                                )

                                Log.d(
                                    "FACE_SELECTION",
                                    "CALL CREATED SUCCESSFULLY"
                                )

                                Log.d(
                                    "FACE_SELECTION",
                                    "Call ID = $callId"
                                )

                                Log.d(
                                    "FACE_SELECTION",
                                    "Navigating to outgoing call..."
                                )

                                creatingCall =
                                    false

                                Toast.makeText(
                                    context,
                                    "Calling $friendName...",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // =================================
                                // GO TO OUTGOING CALL
                                // =================================

                                navController.navigate(
                                    "outgoing_call/$callId"
                                )
                            }
                            .addOnFailureListener { error ->

                                creatingCall =
                                    false

                                Log.e(
                                    "FACE_SELECTION",
                                    "FAILED TO CREATE CALL",
                                    error
                                )

                                Toast.makeText(
                                    context,
                                    "Failed to start call: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { error ->

                        creatingCall =
                            false

                        Log.e(
                            "FACE_SELECTION",
                            "FAILED TO SAVE SELECTED FACE",
                            error
                        )

                        Toast.makeText(
                            context,
                            "Failed to save selected face: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
        ) {

            if (creatingCall) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    "Starting Call..."
                )

            } else {

                Text(
                    if (selectedImage != null) {
                        "Call with Selected Face"
                    } else {
                        "Select a Face First"
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )
    }
}