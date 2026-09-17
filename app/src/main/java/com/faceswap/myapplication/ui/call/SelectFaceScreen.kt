package com.faceswap.myapplication.ui.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

data class FaceItem(
    val url:String
)

@Composable
fun SelectFaceScreen(
    navController: NavHostController,
    friendId: String
) {

    val firestore = FirebaseFirestore.getInstance()

    var faces by remember { mutableStateOf(listOf<FaceItem>()) }

    var selectedFace by remember { mutableStateOf<String?>(null) }

    val channelName = UUID.randomUUID().toString()

    LaunchedEffect(Unit) {

        firestore.collection("users")
            .get()
            .addOnSuccessListener {

                val list = mutableListOf<FaceItem>()

                for(doc in it){

                    val url = doc.getString("faceUrl")

                    if(url != null){

                        list.add(FaceItem(url))
                    }
                }

                faces = list
            }
    }

    Column {

        Text(
            text = "Call : $friendId",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            items(faces){ face ->

                AsyncImage(
                    model = face.url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                        .clickable {

                            selectedFace = face.url
                        }
                )
            }
        }

        Button(

            onClick = {

                selectedFace?.let {

                    navController.navigate(
                        "video_call/$channelName/$it"
                    )

                }

            },

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)

        ) {

            Text("Start Call")

        }
    }
}