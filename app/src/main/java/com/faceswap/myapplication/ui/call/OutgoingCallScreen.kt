package com.faceswap.myapplication.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun OutgoingCallScreen(
    callId: String,
    navController: NavHostController
) {

    var status by remember {
        mutableStateOf("Calling...")
    }

    LaunchedEffect(callId) {

        FirebaseFirestore
            .getInstance()
            .collection("calls")
            .document(callId)
            .addSnapshotListener { value, _ ->

                val call =
                    value?.toObject(
                        CallData::class.java
                    )

                when (call?.status) {

                    "accepted" -> {

                        status = "Connected"

                        navController.navigate(
                            "video_call/${callId}"
                        ) {
                            popUpTo(
                                "outgoing_call/$callId"
                            ) {
                                inclusive = true
                            }
                        }
                    }

                    "rejected" -> {

                        status = "Call rejected"

                        navController.navigate("home") {
                            popUpTo("home") {
                                inclusive = false
                            }
                        }
                    }
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(status)

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                FirebaseFirestore
                    .getInstance()
                    .collection("calls")
                    .document(callId)
                    .update(
                        "status",
                        "rejected"
                    )

                navController.navigate("home") {
                    popUpTo("home") {
                        inclusive = false
                    }
                }
            }
        ) {

            Text("Cancel")
        }
    }
}