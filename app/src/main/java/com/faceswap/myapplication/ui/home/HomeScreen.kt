package com.faceswap.myapplication.ui.home

import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.VideoCall

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// JHAISHNA FACE CALL - SKY BLUE THEME
// ============================================================

// Main background
private val BackgroundColor = Color(0xFFF3F9FD)

// Main sky blue
private val PrimaryBlue = Color(0xFF42A5E8)

// Dark blue for readable text/icons
private val DarkBlue = Color(0xFF0D4770)

// Light sky blue
private val LightBlue = Color(0xFFDDF2FC)

// Very light blue
private val SoftBlue = Color(0xFFEEF8FD)

// Main text
private val TextDark = Color(0xFF17232D)

// Secondary text
private val TextGray = Color(0xFF667580)

// White
private val White = Color.White


// ============================================================
// CONTACT MODEL
// ============================================================

data class HomeContact(
    val name: String,
    val phone: String,
    val initials: String,
    val status: String = "Available",
    val isFavorite: Boolean = false
)


// ============================================================
// HOME SCREEN
// ============================================================

@Composable
fun HomeScreen(
    onFreeCallClick: () -> Unit = {},
    onContactChatClick: (HomeContact) -> Unit = {},
    onContactVideoCallClick: (HomeContact) -> Unit = {},
    onSeeAllContactsClick: () -> Unit = {},
    onSeeAllFavoritesClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {

    val context = LocalContext.current

    // ========================================================
    // CONTACTS
    // ========================================================

    val contacts = remember {

        listOf(

            HomeContact(
                name = "Mani Rakesh",
                phone = "+91 98765 43210",
                initials = "M",
                status = "Available",
                isFavorite = true
            ),

            HomeContact(
                name = "Mani Rakesh Vasamsetti",
                phone = "+91 91234 56789",
                initials = "M",
                status = "Available",
                isFavorite = true
            ),

            HomeContact(
                name = "sunil",
                phone = "+91 99887 66554",
                initials = "S",
                status = "Available",
                isFavorite = true
            ),

            HomeContact(
                name = "raju",
                phone = "+91 90123 45678",
                initials = "R",
                status = "Away"
            ),

            HomeContact(
                name = "deva",
                phone = "+91 98765 12345",
                initials = "D",
                status = "Available"
            ),

            HomeContact(
                name = "Eepi Devakalyan",
                phone = "+91 87654 32109",
                initials = "E",
                status = "Available"
            ),

            HomeContact(
                name = "Devakalyan Eepi",
                phone = "+91 76543 21098",
                initials = "D",
                status = "Available"
            ),

            HomeContact(
                name = "Vamsi kamana",
                phone = "+91 93456 78901",
                initials = "V",
                status = "Away"
            )
        )
    }

    val favoriteContacts = remember(contacts) {
        contacts.filter {
            it.isFavorite
        }
    }


    // ========================================================
    // CALENDAR
    // ========================================================

    val calendarDays = remember {

        listOf(
            "MON" to "21",
            "TUE" to "22",
            "WED" to "23",
            "THU" to "24",
            "FRI" to "25",
            "SAT" to "26",
            "SUN" to "27"
        )
    }

    var selectedDate by remember {
        mutableStateOf("21")
    }


    // ========================================================
    // MAIN HOME SCREEN
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 18.dp)
    ) {

        Spacer(
            modifier = Modifier.height(14.dp)
        )


        // ====================================================
        // HEADER
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Profile circle

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LightBlue),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "J",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
            }


            Spacer(
                modifier = Modifier.width(11.dp)
            )


            // User name

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Jhaishna",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Good Morning!",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }


            // Notification button

            Surface(
                modifier = Modifier.size(43.dp),
                shape = CircleShape,
                color = White,
                shadowElevation = 2.dp
            ) {

                IconButton(
                    onClick = {

                        onNotificationClick()

                        Toast.makeText(
                            context,
                            "Notifications",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {

                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = DarkBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // ====================================================
        // JHAISHNA FACE CALL CARD
        // ====================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clickable {

                    onFreeCallClick()
                },

            shape = RoundedCornerShape(25.dp),

            colors = CardDefaults.cardColors(
                containerColor = PrimaryBlue
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {


                // ------------------------------------------------
                // LEFT SIDE
                // ------------------------------------------------

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "JHAISHNA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = White.copy(alpha = 0.85f)
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )


                    // MAIN APP HEADING

                    Text(
                        text = "Jhaishna Face Call",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )


                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )


                    Text(
                        text = "Connect with your people instantly",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = White.copy(alpha = 0.92f)
                    )


                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    // Start Call button

                    Surface(
                        modifier = Modifier.clickable {

                            onFreeCallClick()
                        },

                        shape = RoundedCornerShape(30.dp),

                        color = White
                    ) {

                        Text(
                            text = "Start a Free Call",

                            modifier = Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 6.dp
                            ),

                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.width(10.dp)
                )


                // ------------------------------------------------
                // FACE CALL LOGO
                // ------------------------------------------------

                Surface(
                    modifier = Modifier
                        .size(70.dp)
                        .clickable {

                            onFreeCallClick()
                        },

                    shape = CircleShape,

                    color = White.copy(
                        alpha = 0.22f
                    )
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),

                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.VideoCall,

                            contentDescription =
                                "Jhaishna Face Call",

                            tint = White,

                            modifier = Modifier.size(39.dp)
                        )
                    }
                }
            }
        }


        Spacer(
            modifier = Modifier.height(17.dp)
        )


        // ====================================================
        // CALENDAR HEADER
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Your Calendar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Choose a day for your calls",
                    fontSize = 10.sp,
                    color = TextGray
                )
            }


            // Calendar icon

            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clickable {

                        Toast.makeText(
                            context,
                            "Selected date: $selectedDate",
                            Toast.LENGTH_SHORT
                        ).show()
                    },

                shape = CircleShape,

                color = LightBlue
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.CalendarMonth,

                        contentDescription =
                            "Calendar",

                        tint = DarkBlue,

                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        // ====================================================
        // CALENDAR DAYS
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),

            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {

            calendarDays.forEach { day ->

                CalendarDay(
                    day = day.first,
                    date = day.second,
                    selected =
                        selectedDate == day.second,

                    onClick = {

                        selectedDate = day.second

                        Toast.makeText(
                            context,
                            "Selected ${day.first} ${day.second}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }


        Spacer(
            modifier = Modifier.height(17.dp)
        )


        // ====================================================
        // FAVORITES HEADER
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier.weight(1f),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Favorite,

                    contentDescription =
                        "Favorites",

                    tint = PrimaryBlue,

                    modifier = Modifier.size(19.dp)
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "Favorites",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }


            Text(
                text = "See All",

                modifier = Modifier.clickable {

                    onSeeAllFavoritesClick()

                    Toast.makeText(
                        context,
                        "Opening Favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )
        }


        Spacer(
            modifier = Modifier.height(9.dp)
        )


        // ====================================================
        // FAVORITES
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),

            horizontalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {

            favoriteContacts.forEach { contact ->

                FavoriteContact(
                    contact = contact,

                    onChat = {

                        onContactChatClick(
                            contact
                        )
                    },

                    onVideoCall = {

                        onContactVideoCallClick(
                            contact
                        )
                    }
                )
            }
        }


        Spacer(
            modifier = Modifier.height(17.dp)
        )


        // ====================================================
        // ALL CONTACTS HEADER
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier.weight(1f),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Contacts,

                    contentDescription =
                        "All Contacts",

                    tint = PrimaryBlue,

                    modifier = Modifier.size(20.dp)
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "All Contacts",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }


            Text(
                text = "See All",

                modifier = Modifier.clickable {

                    onSeeAllContactsClick()

                    Toast.makeText(
                        context,
                        "Opening All Contacts",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )
        }


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        // ====================================================
        // CONTACTS
        // ====================================================

        contacts.take(4).forEach { contact ->

            ContactRow(

                contact = contact,

                onChat = {

                    onContactChatClick(
                        contact
                    )
                },

                onVideoCall = {

                    onContactVideoCallClick(
                        contact
                    )
                },

                onOpen = {

                    onContactChatClick(
                        contact
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )
        }


        Spacer(
            modifier = Modifier.height(15.dp)
        )
    }
}


// ============================================================
// CALENDAR DAY
// ============================================================

@Composable
private fun CalendarDay(
    day: String,
    date: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(

        modifier = Modifier
            .width(54.dp)
            .height(66.dp)

            .clip(
                RoundedCornerShape(16.dp)
            )

            .background(
                if (selected) {
                    PrimaryBlue
                } else {
                    White
                }
            )

            .clickable {
                onClick()
            }

            .padding(
                vertical = 8.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = day,

            fontSize = 9.sp,

            fontWeight =
                FontWeight.SemiBold,

            color =
                if (selected) {
                    White.copy(alpha = 0.9f)
                } else {
                    TextGray
                }
        )


        Spacer(
            modifier = Modifier.height(3.dp)
        )


        Text(
            text = date,

            fontSize = 16.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                if (selected) {
                    White
                } else {
                    TextDark
                }
        )
    }
}


// ============================================================
// FAVORITE CONTACT CARD
// ============================================================

@Composable
private fun FavoriteContact(
    contact: HomeContact,
    onChat: () -> Unit,
    onVideoCall: () -> Unit
) {

    Card(

        modifier = Modifier.width(145.dp),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier = Modifier.padding(11.dp)
        ) {


            // Avatar + online status

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LightBlue),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = contact.initials,

                        fontSize = 15.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = DarkBlue
                    )
                }


                Spacer(
                    modifier = Modifier.width(7.dp)
                )


                // Online dot

                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(

                            if (
                                contact.status ==
                                "Available"
                            ) {
                                PrimaryBlue
                            } else {
                                TextGray
                            }
                        )
                )
            }


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // Name

            Text(
                text = contact.name,

                fontSize = 12.sp,

                fontWeight =
                    FontWeight.Bold,

                color = TextDark,

                maxLines = 1
            )


            Spacer(
                modifier = Modifier.height(2.dp)
            )


            Text(
                text = contact.status,

                fontSize = 9.sp,

                color = TextGray
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // Buttons

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End
            ) {

                // Chat button

                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {

                            onChat()
                        },

                    shape = CircleShape,

                    color = LightBlue
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Chat,

                            contentDescription =
                                "Chat ${contact.name}",

                            tint = DarkBlue,

                            modifier =
                                Modifier.size(16.dp)
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.width(6.dp)
                )


                // Video call button

                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {

                            onVideoCall()
                        },

                    shape = CircleShape,

                    color = LightBlue
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.VideoCall,

                            contentDescription =
                                "Video call ${contact.name}",

                            tint = DarkBlue,

                            modifier =
                                Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// ALL CONTACT ROW
// ============================================================

@Composable
private fun ContactRow(
    contact: HomeContact,
    onChat: () -> Unit,
    onVideoCall: () -> Unit,
    onOpen: () -> Unit
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 11.dp,
                    vertical = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            // ------------------------------------------------
            // AVATAR
            // ------------------------------------------------

            Box(

                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftBlue),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = contact.initials,

                    fontSize = 15.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color = DarkBlue
                )
            }


            Spacer(
                modifier = Modifier.width(9.dp)
            )


            // ------------------------------------------------
            // NAME + PHONE
            // ------------------------------------------------

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = contact.name,

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color = TextDark,

                    maxLines = 1
                )


                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )


                Text(
                    text = contact.phone,

                    fontSize = 9.sp,

                    color = TextGray
                )
            }


            // ------------------------------------------------
            // CHAT
            // ------------------------------------------------

            Surface(

                modifier = Modifier
                    .size(30.dp)
                    .clickable {

                        onChat()
                    },

                shape = CircleShape,

                color = LightBlue
            ) {

                Box(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Chat,

                        contentDescription =
                            "Chat ${contact.name}",

                        tint = DarkBlue,

                        modifier =
                            Modifier.size(15.dp)
                    )
                }
            }


            Spacer(
                modifier = Modifier.width(5.dp)
            )


            // ------------------------------------------------
            // VIDEO CALL
            // ------------------------------------------------

            Surface(

                modifier = Modifier
                    .size(30.dp)
                    .clickable {

                        onVideoCall()
                    },

                shape = CircleShape,

                color = LightBlue
            ) {

                Box(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.VideoCall,

                        contentDescription =
                            "Video call ${contact.name}",

                        tint = DarkBlue,

                        modifier =
                            Modifier.size(15.dp)
                    )
                }
            }


            Spacer(
                modifier = Modifier.width(5.dp)
            )


            // ------------------------------------------------
            // OPEN CONTACT
            // ------------------------------------------------

            Surface(

                modifier = Modifier
                    .size(30.dp)
                    .clickable {

                        onOpen()
                    },

                shape = CircleShape,

                color = LightBlue
            ) {

                Box(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.ArrowForward,

                        contentDescription =
                            "Open ${contact.name}",

                        tint = DarkBlue,

                        modifier =
                            Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}