package com.faceswap.myapplication.ui.upload

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@Composable
fun UploadScreen() {

    val context = LocalContext.current

    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Image Picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        imageUri = uri

        uri?.let {

            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream!!.readBytes()
            inputStream.close()

            bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Image Preview
        bitmap?.let {

            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Pick Image Button
        Button(
            onClick = {
                launcher.launch("image/*")
            }
        ) {
            Text("Pick Image")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Upload Button
        Button(

            enabled = imageUri != null && !isUploading,

            onClick = {

                val uri = imageUri ?: return@Button

                isUploading = true

                val fileName = UUID.randomUUID().toString()

                val storageRef =
                    storage.reference.child("faces/$fileName.jpg")

                storageRef.putFile(uri)

                    .addOnSuccessListener {

                        storageRef.downloadUrl
                            .addOnSuccessListener { downloadUrl ->

                                val userId = auth.currentUser?.uid

                                if(userId == null){

                                    Toast.makeText(
                                        context,
                                        "User not logged in",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    isUploading = false
                                    return@addOnSuccessListener
                                }

                                val data = mapOf(
                                    "faceUrl" to downloadUrl.toString()
                                )

                                firestore.collection("users")
                                    .document(userId)
                                    .set(data)

                                    .addOnSuccessListener {

                                        Toast.makeText(
                                            context,
                                            "Face Uploaded Successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        isUploading = false
                                    }

                                    .addOnFailureListener {

                                        Toast.makeText(
                                            context,
                                            "Firestore Save Failed",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        isUploading = false
                                    }

                            }
                    }

                    .addOnFailureListener {

                        Toast.makeText(
                            context,
                            "Upload Failed",
                            Toast.LENGTH_SHORT
                        ).show()

                        isUploading = false
                    }

            }
        ) {

            Text(

                if(isUploading)
                    "Uploading..."
                else
                    "Upload Face"
            )
        }
    }
}