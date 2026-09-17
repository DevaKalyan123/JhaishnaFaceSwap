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
fun IncomingCallScreen(
    callId: String,
    navController: NavHostController
) {

    var callData by remember { mutableStateOf(CallData()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("calls")
            .document(callId)
            .get()
            .addOnSuccessListener {
                callData = it.toObject(CallData::class.java) ?: CallData()
            }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("${callData.callerName} is calling...")

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(callId)
                    .update("status", "accepted")

                navController.navigate(
                    "video_call/${callData.channelName}/dummy/$callId"
                )
            }
        ) {
            Text("Accept")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                FirebaseFirestore.getInstance()
                    .collection("calls")
                    .document(callId)
                    .update("status", "rejected")

                navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            }
        ) {
            Text("Reject")

        }
    }
}