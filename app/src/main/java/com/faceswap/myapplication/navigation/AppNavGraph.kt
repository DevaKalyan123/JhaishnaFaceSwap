package com.faceswap.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.faceswap.myapplication.ui.auth.*
import com.faceswap.myapplication.ui.call.*
import com.faceswap.myapplication.ui.main.MainScreen
import com.faceswap.myapplication.ui.splash.SplashScreen

@Composable
fun AppNavGraph(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        composable("splash") {
            SplashScreen(navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController)
        }

        composable("home") {
            MainScreen(navController)
        }

        composable("contacts") {
            ContactsScreen(navController)
        }

        // 🔥 INCOMING CALL
        composable(
            route = "incoming_call/{callId}",
            arguments = listOf(
                navArgument("callId") {
                    type = NavType.StringType
                }
            )
        ) {
            val callId = it.arguments?.getString("callId") ?: ""

            IncomingCallScreen(
                callId = callId,
                navController = navController
            )
        }

        // 🔥 OUTGOING CALL
        composable(
            route = "outgoing_call/{callId}",
            arguments = listOf(
                navArgument("callId") {
                    type = NavType.StringType
                }
            )
        ) {
            val callId = it.arguments?.getString("callId") ?: ""

            OutgoingCallScreen(
                callId = callId,
                navController = navController
            )
        }

        // 🔥 VIDEO CALL
        composable(
            route = "video_call/{channelName}/{faceUrl}/{callId}",
            arguments = listOf(
                navArgument("channelName") {
                    type = NavType.StringType
                },
                navArgument("faceUrl") {
                    type = NavType.StringType
                },
                navArgument("callId") {
                    type = NavType.StringType
                }
            )
        ) {

            val channelName =
                it.arguments?.getString("channelName") ?: ""

            val faceUrl =
                it.arguments?.getString("faceUrl") ?: ""

            val callId =
                it.arguments?.getString("callId") ?: ""

            VideoCallScreen(
                channelName = channelName,
                faceUrl = faceUrl,
                navController = navController,
                callId = callId
            )
        }

        // 🔥 CHAT
        composable(
            route = "chat/{friendUid}/{friendName}",
            arguments = listOf(
                navArgument("friendUid") {
                    type = NavType.StringType
                },
                navArgument("friendName") {
                    type = NavType.StringType
                }
            )
        ) {

            val uid =
                it.arguments?.getString("friendUid") ?: ""

            val name =
                it.arguments?.getString("friendName")
                    ?.replace("_", " ") ?: ""

            ChatScreen(
                friendUid = uid,
                friendName = name
            )
        }
    }
}