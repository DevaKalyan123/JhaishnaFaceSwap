package com.faceswap.myapplication.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.dp


@Composable
fun ProfileScreen(rootNavController: NavHostController) {

    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Text("Name: ${user?.displayName ?: "No Name"}")
        Text("Email: ${user?.email}")

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()

                rootNavController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        ) {
            Text("Logout")
        }
    }
}