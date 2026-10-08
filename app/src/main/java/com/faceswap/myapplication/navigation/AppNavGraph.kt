package com.faceswap.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.faceswap.myapplication.ui.call.FaceSelectionScreen

import com.faceswap.myapplication.ui.auth.LoginScreen
import com.faceswap.myapplication.ui.auth.RegisterScreen

import com.faceswap.myapplication.ui.call.ChatScreen
import com.faceswap.myapplication.ui.call.ContactsScreen
import com.faceswap.myapplication.ui.call.IncomingCallScreen
import com.faceswap.myapplication.ui.call.OutgoingCallScreen
import com.faceswap.myapplication.ui.call.VideoCallScreen

import com.faceswap.myapplication.ui.main.MainScreen

import com.faceswap.myapplication.ui.splash.SplashScreen

import com.faceswap.myapplication.ui.upload.UploadScreen


@Composable
fun AppNavGraph(
    navController: NavHostController
) {

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // =========================================================
        // SPLASH
        // =========================================================

        composable("splash") {

            SplashScreen(
                navController = navController
            )
        }


        // =========================================================
        // LOGIN
        // =========================================================

        composable("login") {

            LoginScreen(
                navController = navController
            )
        }


        // =========================================================
        // REGISTER
        // =========================================================

        composable("register") {

            RegisterScreen(
                navController = navController
            )
        }


        // =========================================================
        // HOME
        // =========================================================

        composable("home") {

            MainScreen(
                rootNavController = navController
            )
        }


        // =========================================================
        // UPLOAD IMAGE
        // =========================================================

        composable("upload") {

            UploadScreen()
        }


        // =========================================================
        // CONTACTS
        // =========================================================

        composable("contacts") {

            ContactsScreen(
                navController = navController
            )
        }


        // =========================================================
        // INCOMING CALL
        // =========================================================

        composable(
            route = "incoming_call/{callId}",

            arguments = listOf(
                navArgument("callId") {
                    type = NavType.StringType
                }
            )
        ) {

            val callId =
                it.arguments?.getString("callId")
                    ?: ""

            IncomingCallScreen(
                callId = callId,
                navController = navController
            )
        }


        // =========================================================
        // OUTGOING CALL
        // =========================================================

        composable(
            route = "outgoing_call/{callId}",

            arguments = listOf(
                navArgument("callId") {
                    type = NavType.StringType
                }
            )
        ) {

            val callId =
                it.arguments?.getString("callId")
                    ?: ""

            OutgoingCallScreen(
                callId = callId,
                navController = navController
            )
        }


        // =========================================================
        // VIDEO CALL
        // =========================================================
        // IMPORTANT:
        // We only pass callId.
        //
        // channelName and faceUrl will be fetched from:
        //
        // calls/{callId}
        //
        // inside VideoCallScreen.
        // =========================================================

        composable(
            route = "video_call/{callId}",

            arguments = listOf(
                navArgument("callId") {
                    type = NavType.StringType
                }
            )
        ) {

            val callId =
                it.arguments?.getString("callId")
                    ?: ""

            VideoCallScreen(
                callId = callId,
                navController = navController
            )
        }
        composable(
            route = "face_select/{friendUid}/{friendName}",
            arguments = listOf(
                navArgument("friendUid") {
                    type = NavType.StringType
                },
                navArgument("friendName") {
                    type = NavType.StringType
                }
            )
        ) {

            val friendUid =
                it.arguments
                    ?.getString("friendUid")
                    ?: ""

            val friendName =
                it.arguments
                    ?.getString("friendName")
                    ?: ""


            FaceSelectionScreen(

                friendUid =
                    friendUid,

                friendName =
                    friendName,

                navController =
                    navController
            )
        }

        // =========================================================
        // CHAT
        // =========================================================

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
                it.arguments?.getString("friendUid")
                    ?: ""

            val name =
                it.arguments
                    ?.getString("friendName")
                    ?.replace("_", " ")
                    ?: ""

            ChatScreen(
                friendUid = uid,
                friendName = name
            )
        }
    }
}