package com.faceswap.myapplication.ui.call

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

data class MessageData(
    val id: String = "",
    val text: String = "",
    val sender: String = "",
    val timestamp: Long = 0,
    val image: String = "",
    val audio: String = "",
    val seen: Boolean = false
)

@Composable
fun ChatScreen(friendUid: String, friendName: String) {

    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<MessageData>()) }
    var isTyping by remember { mutableStateOf(false) }
    var userStatus by remember { mutableStateOf("Loading...") }

    val listState = rememberLazyListState()

    val chatId = if (currentUser < friendUid)
        "chat_${currentUser}_$friendUid"
    else
        "chat_${friendUid}_$currentUser"

    // ✅ IMAGE PICKER
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImage(it, chatId, currentUser) }
    }

    // ✅ FILE PICKER
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadFile(it, chatId, currentUser) }
    }

    // 🔥 USER STATUS
    LaunchedEffect(Unit) {
        firestore.collection("users")
            .document(friendUid)
            .addSnapshotListener { value, _ ->
                val online = value?.getBoolean("online") ?: false
                val lastSeen = value?.getLong("lastSeen") ?: 0L

                userStatus = if (online) {
                    "Online 🟢"
                } else {
                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(Date(lastSeen))
                    "Last seen $time"
                }
            }
    }

    // 🔥 LOAD MESSAGES
    LaunchedEffect(Unit) {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { value, _ ->

                val list = value?.documents?.mapNotNull {
                    it.toObject(MessageData::class.java)?.copy(id = it.id)
                } ?: emptyList()

                messages = list

                list.filter { it.sender != currentUser && !it.seen }
                    .forEach { msg ->
                        firestore.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .document(msg.id)
                            .update("seen", true)
                    }
            }
    }

    // 🔥 AUTO SCROLL
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 🔥 TYPING
    LaunchedEffect(Unit) {
        firestore.collection("typing")
            .document(chatId)
            .addSnapshotListener { value, _ ->
                isTyping = value?.getBoolean(friendUid) ?: false
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))
    ) {

        Column(Modifier.padding(16.dp)) {
            Text(friendName, style = MaterialTheme.typography.titleLarge)
            Text(userStatus, color = Color.Gray)
        }

        if (isTyping) {
            Text("typing...", color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f)
        ) {

            items(messages) { msg ->

                val isMe = msg.sender == currentUser

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (isMe) Arrangement.End else Arrangement.Start
                ) {

                    if (!isMe) {
                        Image(
                            painter = rememberAsyncImagePainter("https://i.pravatar.cc/150?u=${msg.sender}"),
                            contentDescription = "",
                            modifier = Modifier.size(36.dp).padding(4.dp)
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (isMe) Color(0xFFD1E7DD)
                                else Color.White
                        ),
                        modifier = Modifier.padding(6.dp)
                    ) {

                        Column(Modifier.padding(10.dp)) {

                            if (msg.image.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(msg.image),
                                    contentDescription = "",
                                    modifier = Modifier.size(200.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (msg.audio.isNotEmpty()) {
                                Button(onClick = {
                                    val player = MediaPlayer()
                                    player.setDataSource(msg.audio)
                                    player.prepare()
                                    player.start()
                                }) {
                                    Text("Play")
                                }
                            } else {
                                if (msg.text.startsWith("http")) {
                                    Button(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.text))
                                        context.startActivity(intent)
                                    }) {
                                        Text("Open File")
                                    }
                                } else {
                                    Text(msg.text)
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Row {
                                Text(
                                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        .format(Date(msg.timestamp))
                                )

                                if (isMe) {
                                    Text(
                                        if (msg.seen) " ✔✔" else " ✔",
                                        color = if (msg.seen) Color.Blue else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.padding(8.dp)) {

            TextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                    firestore.collection("typing")
                        .document(chatId)
                        .set(mapOf(currentUser to it.isNotEmpty()))
                },
                modifier = Modifier.weight(1f)
            )

            Button(onClick = {

                if (messageText.isBlank()) return@Button

                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(
                        mapOf(
                            "text" to messageText,
                            "sender" to currentUser,
                            "timestamp" to System.currentTimeMillis(),
                            "seen" to false,
                            "image" to "",
                            "audio" to ""
                        )
                    )

                messageText = ""

                firestore.collection("typing")
                    .document(chatId)
                    .set(mapOf(currentUser to false))

            }) {
                Text("Send")
            }

            Button(onClick = { imagePicker.launch("image/*") }) {
                Text("IMG")
            }

            Button(onClick = { filePicker.launch("*/*") }) {
                Text("FILE")
            }
        }
    }
}

// ✅ UPLOAD IMAGE
fun uploadImage(uri: Uri, chatId: String, user: String) {
    val ref = FirebaseStorage.getInstance().reference
        .child("images/${System.currentTimeMillis()}.jpg")

    ref.putFile(uri).addOnSuccessListener {
        ref.downloadUrl.addOnSuccessListener { url ->
            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .add(
                    mapOf(
                        "image" to url.toString(),
                        "sender" to user,
                        "timestamp" to System.currentTimeMillis(),
                        "seen" to false,
                        "text" to "",
                        "audio" to ""
                    )
                )
        }
    }
}

// ✅ UPLOAD FILE
fun uploadFile(uri: Uri, chatId: String, user: String) {
    val ref = FirebaseStorage.getInstance().reference
        .child("files/${System.currentTimeMillis()}")

    ref.putFile(uri).addOnSuccessListener {
        ref.downloadUrl.addOnSuccessListener { url ->
            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .collection("messages")
                .add(
                    mapOf(
                        "text" to url.toString(),
                        "sender" to user,
                        "timestamp" to System.currentTimeMillis(),
                        "seen" to false,
                        "image" to "",
                        "audio" to ""
                    )
                )
        }
    }
}