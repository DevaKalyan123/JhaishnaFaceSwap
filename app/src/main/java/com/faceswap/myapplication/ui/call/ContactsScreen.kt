package com.faceswap.myapplication.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

data class Contact(
    val name: String = "",
    val uid: String = ""
)

@Composable
fun ContactsScreen(navController: NavHostController) {

    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var users by remember { mutableStateOf(listOf<Contact>()) }
    var searchText by remember { mutableStateOf("") }

    // 🔥 FETCH USERS
    LaunchedEffect(Unit) {

        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->

                val list = mutableListOf<Contact>()

                for (doc in result) {

                    val uid = doc.getString("uid") ?: ""
                    val name = doc.getString("name") ?: ""

                    if (uid.isNotEmpty() && uid != currentUser) {
                        list.add(Contact(name, uid))
                    }
                }

                users = list
            }
    }

    val filteredUsers = users.filter {
        it.name.contains(searchText, ignoreCase = true)
    }

    Column {

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Search users") }
        )

        LazyColumn {

            items(filteredUsers) { user ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(user.name)

                    Row {

                        // 💬 CHAT
                        IconButton(
                            onClick = {
                                navController.navigate(
                                    "chat/${user.uid}/${user.name.replace(" ", "_")}"
                                )
                            }
                        ) {
                            Icon(Icons.Default.Message, contentDescription = "")
                        }

                        // 📹 VIDEO CALL (🔥 FIXED)
                        IconButton(
                            onClick = {

                                val channelName =
                                    if (currentUser < user.uid)
                                        "${currentUser}_${user.uid}"
                                    else
                                        "${user.uid}_${currentUser}"

                                val callId = FirebaseFirestore.getInstance()
                                    .collection("calls")
                                    .document().id

                                FirebaseFirestore.getInstance()
                                    .collection("calls")
                                    .document(callId)
                                    .set(
                                        CallData(
                                            callerId = currentUser,
                                            callerName = "Caller",
                                            receiverId = user.uid,
                                            channelName = channelName,
                                            status = "calling",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )

                                navController.navigate("outgoing_call/$callId")
                            }
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = "")
                        }
                    }
                }
            }
        }
    }
}