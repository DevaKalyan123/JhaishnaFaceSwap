package com.faceswap.myapplication.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

import com.faceswap.myapplication.ui.home.HomeScreen
import com.faceswap.myapplication.ui.call.ContactsScreen
import com.faceswap.myapplication.ui.upload.UploadScreen
import com.faceswap.myapplication.ui.profile.ProfileScreen

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MainScreen(rootNavController: NavHostController) {

    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.VideoCall,
        BottomNavItem.Upload,
        BottomNavItem.Profile
    )

    // 🔥 ONLINE / LAST SEEN LOGIC
    DisposableEffect(Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        uid?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .update("online", true)
        }

        onDispose {
            uid?.let {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(it)
                    .update(
                        mapOf(
                            "online" to false,
                            "lastSeen" to System.currentTimeMillis()
                        )
                    )
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {

                items.forEach { item ->

                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            navController.navigate(item.route)
                        },
                        icon = { Icon(item.icon, item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {

            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }

            // 🔥 IMPORTANT FIX (already correct)
            composable(BottomNavItem.VideoCall.route) {
                ContactsScreen(rootNavController)
            }

            composable(BottomNavItem.Upload.route) {
                UploadScreen()
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(rootNavController)
            }
        }
    }
}