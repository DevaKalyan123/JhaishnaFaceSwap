package com.faceswap.myapplication.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    object Home : BottomNavItem(
        route = "home_tab",
        title = "Home",
        icon = Icons.Default.Home
    )

    object VideoCall : BottomNavItem(
        route = "call",
        title = "Call",
        icon = Icons.Default.VideoCall
    )

    object Upload : BottomNavItem(
        route = "upload_tab",
        title = "Upload",
        icon = Icons.Default.CloudUpload
    )

    object Profile : BottomNavItem(
        route = "profile_tab",
        title = "Profile",
        icon = Icons.Default.Person
    )
}