package com.faceswap.myapplication.ui.call

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// ---------------------------------------------------------
// CONTACT MODEL
// ---------------------------------------------------------

data class Contact(
    val name: String = "",
    val uid: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L
)

// ---------------------------------------------------------
// COLORS
// ---------------------------------------------------------

private val SkyBlue = Color(0xFF42A5E8)
private val SkyBlueLight = Color(0xFFEAF6FF)
private val SkyBlueVeryLight = Color(0xFFF5FBFF)

private val DarkText = Color(0xFF17212B)
private val SecondaryText = Color(0xFF66717C)

private val White = Color.White

// ---------------------------------------------------------
// CONTACTS SCREEN
// ---------------------------------------------------------

@Composable
fun ContactsScreen(
    navController: NavHostController
) {

    val firestore = FirebaseFirestore.getInstance()

    val currentUser =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var users by remember {
        mutableStateOf<List<Contact>>(emptyList())
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    // -----------------------------------------------------
    // FETCH USERS FROM FIRESTORE
    // -----------------------------------------------------

    LaunchedEffect(Unit) {

        firestore
            .collection("users")
            .get()
            .addOnSuccessListener { result ->

                val contactList = mutableListOf<Contact>()

                for (document in result.documents) {

                    // If uid field exists, use it.
                    // Otherwise use Firestore document ID.
                    val uid =
                        document.getString("uid")
                            ?: document.id

                    val name =
                        document.getString("name")
                            ?: document.getString("displayName")
                            ?: "Unknown User"

                    val online =
                        document.getBoolean("online")
                            ?: false

                    val lastSeen =
                        document.getLong("lastSeen")
                            ?: 0L

                    // Don't show current logged-in user
                    if (
                        uid.isNotEmpty() &&
                        uid != currentUser
                    ) {

                        contactList.add(
                            Contact(
                                name = name,
                                uid = uid,
                                online = online,
                                lastSeen = lastSeen
                            )
                        )
                    }
                }

                // Sort alphabetically
                users = contactList.sortedBy {
                    it.name.lowercase()
                }

                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    // -----------------------------------------------------
    // SEARCH
    // -----------------------------------------------------

    val filteredUsers = users.filter { user ->

        user.name.contains(
            searchText,
            ignoreCase = true
        )
    }

    // -----------------------------------------------------
    // SCREEN
    // -----------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBlueVeryLight)
    ) {

        // -------------------------------------------------
        // TOP HEADER
        // -------------------------------------------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(
                    horizontal = 18.dp,
                    vertical = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Contacts",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Connect with your people",
                    fontSize = 13.sp,
                    color = SecondaryText
                )
            }

            // Contact count
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SkyBlueLight),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = filteredUsers.size.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlue
                )
            }
        }

        // -------------------------------------------------
        // SEARCH BAR
        // -------------------------------------------------

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),

            leadingIcon = {

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SkyBlue
                )
            },

            placeholder = {

                Text(
                    text = "Search contacts",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkyBlue,
                unfocusedBorderColor = Color(0xFFD4E7F4),
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                cursorColor = SkyBlue
            )
        )

        // -------------------------------------------------
        // CONTACT TITLE
        // -------------------------------------------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "All Contacts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${filteredUsers.size} people",
                fontSize = 12.sp,
                color = SecondaryText
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        // -------------------------------------------------
        // LOADING
        // -------------------------------------------------

        if (loading) {

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = SkyBlue
                )
            }

        } else {

            // -------------------------------------------------
            // CONTACT LIST
            // -------------------------------------------------

            if (filteredUsers.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(SkyBlueLight),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Person,
                                contentDescription =
                                    "No contacts",
                                tint = SkyBlue,
                                modifier =
                                    Modifier.size(35.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        Text(
                            text = "No contacts found",
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color = DarkText
                        )

                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "Try searching for another name",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 14.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = filteredUsers,
                        key = {
                            it.uid
                        }
                    ) { user ->

                        ContactCard(
                            user = user,
                            currentUser = currentUser,
                            navController = navController
                        )
                    }

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(90.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// CONTACT CARD
// ---------------------------------------------------------

@Composable
private fun ContactCard(
    user: Contact,
    currentUser: String,
    navController: NavHostController
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                // Clicking the contact itself opens chat
                navController.navigate(
                    "chat/${user.uid}/${user.name.replace(" ", "_")}"
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // -------------------------------------------------
            // AVATAR
            // -------------------------------------------------

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SkyBlueLight)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFD3EAF8),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text =
                        user.name
                            .trim()
                            .firstOrNull()
                            ?.uppercase()
                            ?: "?",

                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlue
                )

                // Online dot
                if (user.online) {

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF35C759)
                            )
                            .border(
                                width = 2.dp,
                                color = White,
                                shape = CircleShape
                            )
                            .align(
                                Alignment.BottomEnd
                            )
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            // -------------------------------------------------
            // USER INFORMATION
            // -------------------------------------------------

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                if (user.online) {

                    Text(
                        text = "Available",
                        fontSize = 12.sp,
                        color = Color(0xFF2E9E45)
                    )

                } else {

                    Text(
                        text = "Offline",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            // -------------------------------------------------
            // MESSAGE BUTTON
            // -------------------------------------------------

            IconButton(
                onClick = {

                    navController.navigate(
                        "chat/${user.uid}/${user.name.replace(" ", "_")}"
                    )
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SkyBlueLight)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Message,
                    contentDescription =
                        "Message ${user.name}",
                    tint = SkyBlue,
                    modifier =
                        Modifier.size(21.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            // -------------------------------------------------
            // VIDEO CALL BUTTON
            // -------------------------------------------------

            IconButton(
                onClick = {

                    val encodedName = Uri.encode(user.name)

                    navController.navigate(
                        "face_select/${user.uid}/$encodedName"
                    )
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SkyBlueLight)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.VideoCall,
                    contentDescription =
                        "Video call ${user.name}",
                    tint = SkyBlue,
                    modifier =
                        Modifier.size(22.dp)
                )
            }
        }
    }
}
