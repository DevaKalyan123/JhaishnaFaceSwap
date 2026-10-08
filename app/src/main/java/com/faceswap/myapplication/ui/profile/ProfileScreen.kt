package com.faceswap.myapplication.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth


// ------------------------------------------------------------
// SAME COLORS AS CONTACTS SCREEN
// ------------------------------------------------------------

private val AppSkyBlue = Color(0xFF42A5E8)
private val AppLightBlue = Color(0xFFEAF6FF)
private val AppBackground = Color(0xFFF6FBFF)
private val AppText = Color(0xFF18212B)
private val AppSecondaryText = Color(0xFF6F7C88)
private val AppBorder = Color(0xFFD7EAF8)
private val AppWhite = Color.White
private val AppGreen = Color(0xFF43A047)


// ------------------------------------------------------------
// PROFILE SCREEN
// ------------------------------------------------------------

@Composable
fun ProfileScreen(rootNavController: NavHostController) {

    val user = FirebaseAuth.getInstance().currentUser

    val userName =
        user?.displayName?.takeIf { it.isNotBlank() }
            ?: "No Name"

    val userEmail =
        user?.email ?: "No Email"


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {

        // ----------------------------------------------------
        // HEADER
        // ----------------------------------------------------

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppWhite)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 18.dp,
                    bottom = 18.dp
                )
        ) {

            Text(
                text = "Profile",
                color = AppText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Manage your account",
                color = AppSecondaryText,
                fontSize = 13.sp
            )
        }


        // ----------------------------------------------------
        // PROFILE CONTENT
        // ----------------------------------------------------

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                )
        ) {


            // ------------------------------------------------
            // PROFILE CARD
            // ------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(18.dp)
                    )
                    .background(AppWhite)
                    .border(
                        width = 1.dp,
                        color = AppBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                // --------------------------------------------
                // PROFILE AVATAR
                // --------------------------------------------

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(AppLightBlue)
                        .border(
                            width = 1.dp,
                            color = AppSkyBlue,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = userName
                            .firstOrNull()
                            ?.uppercase()
                            ?: "U",
                        color = AppSkyBlue,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }


                Spacer(
                    modifier = Modifier.height(14.dp)
                )


                // --------------------------------------------
                // NAME
                // --------------------------------------------

                Text(
                    text = userName,
                    color = AppText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier = Modifier.height(5.dp)
                )


                // --------------------------------------------
                // STATUS
                // --------------------------------------------

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppGreen)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text = "Active account",
                        color = AppGreen,
                        fontSize = 12.sp
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            // ------------------------------------------------
            // ACCOUNT INFORMATION
            // ------------------------------------------------

            Text(
                text = "Account Information",
                color = AppText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = 4.dp,
                    bottom = 8.dp
                )
            )


            // ------------------------------------------------
            // EMAIL CARD
            // ------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(AppWhite)
                    .border(
                        width = 1.dp,
                        color = AppBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppLightBlue),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = AppSkyBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Email",
                        color = AppSecondaryText,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = userEmail,
                        color = AppText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // ------------------------------------------------
            // USER ID CARD
            // ------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(AppWhite)
                    .border(
                        width = 1.dp,
                        color = AppBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppLightBlue),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = AppSkyBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Account",
                        color = AppSecondaryText,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Jhaishna Face Call",
                        color = AppText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // ------------------------------------------------
            // VERIFIED ACCOUNT CARD
            // ------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(AppWhite)
                    .border(
                        width = 1.dp,
                        color = AppBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppLightBlue),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified",
                        tint = AppSkyBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Account Status",
                        color = AppSecondaryText,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Account is active",
                        color = AppGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // ------------------------------------------------
            // LOGOUT SECTION
            // Replaced Settings / App Settings
            // ------------------------------------------------

            Text(
                text = "Account",
                color = AppText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = 4.dp,
                    bottom = 8.dp
                )
            )


            // ------------------------------------------------
            // LOGOUT CARD
            // ------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(AppWhite)
                    .border(
                        width = 1.dp,
                        color = AppBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Logout icon

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppLightBlue),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = AppSkyBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                // Logout text

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Logout",
                        color = AppText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Sign out from your account",
                        color = AppSecondaryText,
                        fontSize = 12.sp
                    )
                }


                // Logout action

                androidx.compose.material3.TextButton(
                    onClick = {

                        FirebaseAuth
                            .getInstance()
                            .signOut()

                        rootNavController.navigate("login") {
                            popUpTo("home") {
                                inclusive = true
                            }
                        }
                    }
                ) {

                    Text(
                        text = "Logout",
                        color = AppSkyBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}