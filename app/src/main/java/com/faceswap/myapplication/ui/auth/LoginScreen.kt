package com.faceswap.myapplication.ui.auth

import android.app.Activity
import android.util.Log
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController

import com.faceswap.myapplication.R

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

import kotlin.math.*


// ============================================================
// JHAISHNA CHAT APP - LOGIN SCREEN
// ============================================================

// Login Screen Colors
val LavenderBg = Color(0xFFEDE8F5)

val DeepPurple = Color(0xFF5C35CC)

val OrangePrimary = Color(0xFFFF6B35)

val DarkText = Color(0xFF1A1A2E)

val SubText = Color(0xFF9E9E9E)

val WhiteCard = Color(0xFFFFFFFF)

val ErrorRed = Color(0xFFE53935)


// ============================================================
// WAVE COLORS
// ============================================================

val LoginWave1 = Color(0xFFB39DDB).copy(alpha = 0.9f)

val LoginWave2 = Color(0xFF9575CD).copy(alpha = 0.75f)

val LoginWave3 = Color(0xFF7E57C2).copy(alpha = 0.6f)


// ============================================================
// LOGIN SCREEN
// ============================================================

@Composable
fun LoginScreen(navController: NavController) {

    // --------------------------------------------------------
    // CONTEXT
    // --------------------------------------------------------

    val context = LocalContext.current

    val activity = context as Activity

    // --------------------------------------------------------
    // FIREBASE
    // --------------------------------------------------------

    val auth = FirebaseAuth.getInstance()

    val firestore = FirebaseFirestore.getInstance()


    // --------------------------------------------------------
    // LOGIN STATES
    // --------------------------------------------------------

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    // ========================================================
    // PASSWORD VISIBILITY STATE
    // ========================================================

    var passwordVisible by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // WAVE ANIMATIONS
    // ========================================================

    val infiniteTransition = rememberInfiniteTransition(
        label = "loginWaves"
    )


    // --------------------------------------------------------
    // WAVE 1
    // --------------------------------------------------------

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),

        label = "loginWave1"
    )


    // --------------------------------------------------------
    // WAVE 2
    // --------------------------------------------------------

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),

        label = "loginWave2"
    )


    // --------------------------------------------------------
    // WAVE 3
    // --------------------------------------------------------

    val wavePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),

        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),

        label = "loginWave3"
    )


    // ========================================================
    // GOOGLE SIGN IN
    // ========================================================

    val googleSignInClient = remember {

        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(
                "495405298079-o6casm2aip0857009a63l1o1jm5if050.apps.googleusercontent.com"
            )
            .requestEmail()
            .build()

        GoogleSignIn.getClient(
            context,
            gso
        )
    }


    // ========================================================
    // GOOGLE LOGIN RESULT
    // ========================================================

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            val task =
                GoogleSignIn.getSignedInAccountFromIntent(
                    result.data
                )

            try {

                val account =
                    task.getResult(
                        ApiException::class.java
                    )


                val credential =
                    GoogleAuthProvider.getCredential(
                        account.idToken,
                        null
                    )


                auth.signInWithCredential(
                    credential
                )
                    .addOnCompleteListener { authTask ->

                        if (authTask.isSuccessful) {

                            val user =
                                auth.currentUser!!


                            val userMap =
                                hashMapOf(

                                    "uid" to user.uid,

                                    "name" to (
                                            user.displayName
                                                ?: "No Name"
                                            ),

                                    "email" to (
                                            user.email
                                                ?: ""
                                            )
                                )


                            firestore
                                .collection("users")
                                .document(user.uid)
                                .set(userMap)

                                .addOnSuccessListener {

                                    Toast.makeText(
                                        context,
                                        "Google Login Success",
                                        Toast.LENGTH_SHORT
                                    ).show()


                                    navController.navigate(
                                        "home"
                                    ) {

                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }

                                .addOnFailureListener {

                                    Log.e(
                                        "FIRESTORE",
                                        it.message.toString()
                                    )
                                }

                        } else {

                            Log.e(
                                "GOOGLE",
                                "Firebase Auth Failed"
                            )
                        }
                    }

            } catch (e: ApiException) {

                Log.e(
                    "GOOGLE",
                    "Google Sign In Failed: ${e.message}"
                )
            }
        }
    }


    // ========================================================
    // MAIN SCREEN
    // ========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LavenderBg)
    ) {


        // ====================================================
        // ANIMATED WAVES
        // ====================================================

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
        ) {

            val w = size.width

            val h = size.height


            // ------------------------------------------------
            // WAVE 1
            // ------------------------------------------------

            val path1 = Path()

            path1.moveTo(
                0f,
                h * 0.50f
            )

            var x = 0f

            while (x <= w) {

                val y =
                    h * 0.50f +
                            sin(
                                x / w *
                                        2f *
                                        PI.toFloat() *
                                        2f +
                                        wavePhase1
                            ) * 30f

                path1.lineTo(
                    x,
                    y
                )

                x += 4f
            }

            path1.lineTo(
                w,
                h
            )

            path1.lineTo(
                0f,
                h
            )

            path1.close()

            drawPath(
                path1,
                color = LoginWave1
            )


            // ------------------------------------------------
            // WAVE 2
            // ------------------------------------------------

            val path2 = Path()

            path2.moveTo(
                0f,
                h * 0.62f
            )

            x = 0f

            while (x <= w) {

                val y =
                    h * 0.62f +
                            sin(
                                x / w *
                                        2f *
                                        PI.toFloat() *
                                        2.5f +
                                        wavePhase2 +
                                        1f
                            ) * 24f

                path2.lineTo(
                    x,
                    y
                )

                x += 4f
            }

            path2.lineTo(
                w,
                h
            )

            path2.lineTo(
                0f,
                h
            )

            path2.close()

            drawPath(
                path2,
                color = LoginWave2
            )


            // ------------------------------------------------
            // WAVE 3
            // ------------------------------------------------

            val path3 = Path()

            path3.moveTo(
                0f,
                h * 0.74f
            )

            x = 0f

            while (x <= w) {

                val y =
                    h * 0.74f +
                            sin(
                                x / w *
                                        2f *
                                        PI.toFloat() *
                                        3f +
                                        wavePhase3 +
                                        2f
                            ) * 18f

                path3.lineTo(
                    x,
                    y
                )

                x += 4f
            }

            path3.lineTo(
                w,
                h
            )

            path3.lineTo(
                0f,
                h
            )

            path3.close()

            drawPath(
                path3,
                color = LoginWave3
            )
        }


        // ====================================================
        // LOGIN CONTENT
        // ====================================================

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 28.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            Spacer(
                Modifier.height(60.dp)
            )


            // =================================================
            // LOGO
            // =================================================

            Image(

                painter =
                    painterResource(
                        id = R.drawable.svg
                    ),

                contentDescription =
                    "Jhaishna Logo",

                modifier =
                    Modifier.size(90.dp)
            )


            Spacer(
                Modifier.height(20.dp)
            )


            // =================================================
            // TITLE
            // =================================================

            Text(

                text =
                    "Welcome To Jhaishna chat app",

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    DarkText,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                Modifier.height(6.dp)
            )


            // =================================================
            // SIGN IN TITLE
            // =================================================

            Text(

                text =
                    "Sign In",

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    DarkText
            )


            Spacer(
                Modifier.height(8.dp)
            )


            // =================================================
            // REGISTER LINK
            // =================================================

            Text(

                text =
                    "Don't have an account? Register",

                color =
                    OrangePrimary,

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.Medium,

                modifier =
                    Modifier.clickable {

                        navController.navigate(
                            "register"
                        )
                    }
            )


            Spacer(
                Modifier.height(32.dp)
            )


            // =================================================
            // EMAIL FIELD
            // =================================================

            OutlinedTextField(

                value =
                    email,

                onValueChange = {

                    email = it

                    errorMessage = ""
                },

                placeholder = {

                    Text(
                        "Enter your email",

                        color =
                            SubText,

                        fontSize =
                            14.sp
                    )
                },

                leadingIcon = {

                    Icon(

                        imageVector =
                            Icons.Default.Email,

                        contentDescription =
                            "Email Icon",

                        tint =
                            SubText
                    )
                },

                shape =
                    RoundedCornerShape(
                        30.dp
                    ),

                colors =
                    OutlinedTextFieldDefaults.colors(

                        focusedContainerColor =
                            WhiteCard,

                        unfocusedContainerColor =
                            WhiteCard,

                        focusedBorderColor =
                            OrangePrimary,

                        unfocusedBorderColor =
                            Color.Transparent,

                        focusedTextColor =
                            DarkText,

                        unfocusedTextColor =
                            DarkText
                    ),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
            )


            Spacer(
                Modifier.height(16.dp)
            )


            // =================================================
            // PASSWORD FIELD
            // =================================================
            // PASSWORD VISIBILITY ADDED HERE
            // =================================================

            OutlinedTextField(

                value =
                    password,

                onValueChange = {

                    password = it

                    errorMessage = ""
                },

                placeholder = {

                    Text(

                        text =
                            "Enter your password",

                        color =
                            SubText,

                        fontSize =
                            14.sp
                    )
                },


                // -------------------------------------------------
                // PASSWORD LOCK ICON
                // -------------------------------------------------

                leadingIcon = {

                    Icon(

                        imageVector =
                            Icons.Default.Lock,

                        contentDescription =
                            "Password Icon",

                        tint =
                            SubText
                    )
                },


                // -------------------------------------------------
                // 👁 PASSWORD VISIBILITY BUTTON
                // -------------------------------------------------

                trailingIcon = {

                    IconButton(

                        onClick = {

                            passwordVisible =
                                !passwordVisible
                        }
                    ) {

                        Icon(

                            imageVector =
                                if (passwordVisible) {

                                    Icons.Default.VisibilityOff

                                } else {

                                    Icons.Default.Visibility
                                },

                            contentDescription =
                                if (passwordVisible) {

                                    "Hide password"

                                } else {

                                    "Show password"
                                },

                            tint =
                                SubText
                        )
                    }
                },


                // -------------------------------------------------
                // PASSWORD TRANSFORMATION
                // -------------------------------------------------

                visualTransformation =
                    if (passwordVisible) {

                        VisualTransformation.None

                    } else {

                        PasswordVisualTransformation()
                    },


                shape =
                    RoundedCornerShape(
                        30.dp
                    ),


                colors =
                    OutlinedTextFieldDefaults.colors(

                        focusedContainerColor =
                            WhiteCard,

                        unfocusedContainerColor =
                            WhiteCard,

                        focusedBorderColor =
                            OrangePrimary,

                        unfocusedBorderColor =
                            Color.Transparent,

                        focusedTextColor =
                            DarkText,

                        unfocusedTextColor =
                            DarkText
                    ),


                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
            )


            Spacer(
                Modifier.height(14.dp)
            )


            // =================================================
            // ERROR MESSAGE
            // =================================================

            if (errorMessage.isNotEmpty()) {

                Box(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                            .background(
                                Color(
                                    0x26E53935
                                )
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                ) {

                    Text(

                        text =
                            errorMessage,

                        color =
                            ErrorRed,

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        textAlign =
                            TextAlign.Center,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }


                Spacer(
                    Modifier.height(10.dp)
                )
            }


            Spacer(
                Modifier.height(10.dp)
            )


            // =================================================
            // SIGN IN BUTTON
            // =================================================

            Button(

                onClick = {

                    if (
                        email.isEmpty() ||
                        password.isEmpty()
                    ) {

                        errorMessage =
                            "Please fill all fields"

                        return@Button
                    }


                    auth
                        .signInWithEmailAndPassword(
                            email,
                            password
                        )

                        .addOnCompleteListener { task ->

                            if (
                                task.isSuccessful
                            ) {

                                navController.navigate(
                                    "home"
                                ) {

                                    popUpTo(
                                        "login"
                                    ) {

                                        inclusive =
                                            true
                                    }
                                }

                            } else {

                                errorMessage =
                                    "Invalid credentials"
                            }
                        }
                },


                shape =
                    RoundedCornerShape(
                        30.dp
                    ),


                colors =
                    ButtonDefaults.buttonColors(

                        containerColor =
                            OrangePrimary
                    ),


                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
            ) {

                Text(

                    text =
                        "Sign In",

                    color =
                        Color.White,

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                Modifier.height(16.dp)
            )


            // =================================================
            // GOOGLE SIGN IN BUTTON
            // =================================================

            Button(

                onClick = {

                    googleSignInClient.signOut()

                    launcher.launch(
                        googleSignInClient.signInIntent
                    )
                },


                shape =
                    RoundedCornerShape(
                        30.dp
                    ),


                colors =
                    ButtonDefaults.buttonColors(

                        containerColor =
                            DeepPurple
                    ),


                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
            ) {

                Icon(

                    painter =
                        painterResource(
                            id =
                                R.drawable.google_icon_icon_lg
                        ),

                    contentDescription =
                        "Google Icon",

                    tint =
                        Color.Unspecified,

                    modifier =
                        Modifier.size(22.dp)
                )


                Spacer(
                    Modifier.width(10.dp)
                )


                Text(

                    text =
                        "Sign in with Google",

                    color =
                        Color.White,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}