package com.faceswap.myapplication.ui.auth

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*

// ✅ Same colors as LoginScreen
val RegBg          = Color(0xFFEDE8F5)   // lavender - same as login
val RegCardBg      = Color(0xFFFFFFFF)   // white card - same as login
val RegDarkText    = Color(0xFF1A1A2E)   // dark text - same as login
val RegSubText     = Color(0xFF9E9E9E)   // grey text - same as login
val RegOrange      = Color(0xFFFF6B35)   // orange - same as login
val RegDeepPurple  = Color(0xFF5C35CC)   // purple - same as login
val RegDivider     = Color(0xFFF0F0F0)   // light divider
val RegFieldIcon   = Color(0xFF9E9E9E)   // icon color
val RegErrRed      = Color(0xFFE53935)   // error red - same as login

// ✅ Same wave colors as LoginScreen
val RegWave1 = Color(0xFFB39DDB).copy(alpha = 0.9f)
val RegWave2 = Color(0xFF9575CD).copy(alpha = 0.75f)
val RegWave3 = Color(0xFF7E57C2).copy(alpha = 0.6f)

@Composable
fun RegisterScreen(navController: NavController) {

    val context = LocalContext.current
    val auth    = FirebaseAuth.getInstance()
    val db      = FirebaseFirestore.getInstance()

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage    by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }

    // ✅ Same wave animation as LoginScreen
    val infiniteTransition = rememberInfiniteTransition(label = "regWaves")

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rWave1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rWave2"
    )
    val wavePhase3 by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rWave3"
    )

    // ✅ SAME LAVENDER BACKGROUND as LoginScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RegBg)
    ) {

        // ✅ ANIMATED PURPLE WAVES - same as LoginScreen
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height

            // Wave 1 - back
            val path1 = Path()
            path1.moveTo(0f, h * 0.50f)
            var x = 0f
            while (x <= w) {
                val y = h * 0.50f +
                        sin(x / w * 2f * PI.toFloat() * 2f + wavePhase1) * 30f
                path1.lineTo(x, y)
                x += 4f
            }
            path1.lineTo(w, h); path1.lineTo(0f, h); path1.close()
            drawPath(path1, color = RegWave1)

            // Wave 2 - mid
            val path2 = Path()
            path2.moveTo(0f, h * 0.62f)
            x = 0f
            while (x <= w) {
                val y = h * 0.62f +
                        sin(x / w * 2f * PI.toFloat() * 2.5f + wavePhase2 + 1f) * 24f
                path2.lineTo(x, y)
                x += 4f
            }
            path2.lineTo(w, h); path2.lineTo(0f, h); path2.close()
            drawPath(path2, color = RegWave2)

            // Wave 3 - front
            val path3 = Path()
            path3.moveTo(0f, h * 0.74f)
            x = 0f
            while (x <= w) {
                val y = h * 0.74f +
                        sin(x / w * 2f * PI.toFloat() * 3f + wavePhase3 + 2f) * 18f
                path3.lineTo(x, y)
                x += 4f
            }
            path3.lineTo(w, h); path3.lineTo(0f, h); path3.close()
            drawPath(path3, color = RegWave3)
        }

        // ✅ SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(52.dp))

            // ✅ Back Arrow - same style as login
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RegDeepPurple.copy(alpha = 0.10f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = RegDeepPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ✅ Title - same font style as login
            Text(
                text = "Register",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = RegDarkText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // ✅ Subtitle - same style as login's "Sign In"
            Text(
                text = "Create your new account",
                fontSize = 14.sp,
                color = RegSubText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal
            )

            Spacer(Modifier.height(8.dp))

            // ✅ Login link - same style as login's "Don't have account"
            Text(
                text = "Already have an account? Login",
                color = RegOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    navController.navigate("login")
                }
            )

            Spacer(Modifier.height(28.dp))

            // ✅ WHITE CARD - same as login fields style
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RegCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    RegField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Full Name",
                        icon = Icons.Default.Person,
                        showDivider = true,
                        keyboardType = KeyboardType.Text
                    )
                    RegField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        icon = Icons.Default.Email,
                        showDivider = true,
                        keyboardType = KeyboardType.Email
                    )
                    // ✅ Phone: digits only, max 10
                    RegField(
                        value = phone,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.length <= 10) phone = filtered
                        },
                        placeholder = "Phone Number",
                        icon = Icons.Default.Phone,
                        showDivider = true,
                        keyboardType = KeyboardType.Number
                    )
                    RegField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        showDivider = true,
                        keyboardType = KeyboardType.Password
                    )
                    RegField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        showPassword = showConfirm,
                        onTogglePassword = { showConfirm = !showConfirm },
                        showDivider = false,
                        keyboardType = KeyboardType.Password
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ✅ Error message - same as login
            if (errorMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x26E53935))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = RegErrRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(10.dp))

            // ✅ REGISTER BUTTON - same orange as login's Sign In button
            Button(
                onClick = {
                    errorMessage = ""
                    if (name.isBlank() || email.isBlank() || phone.isBlank() ||
                        password.isBlank() || confirmPassword.isBlank()
                    ) {
                        errorMessage = "All fields are required"
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Invalid email format"
                        return@Button
                    }
                    if (phone.length != 10) {
                        errorMessage = "Phone must be exactly 10 digits"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser!!.uid
                                val userMap = hashMapOf(
                                    "uid"   to userId,
                                    "name"  to name,
                                    "email" to email,
                                    "phone" to phone
                                )
                                db.collection("users").document(userId).set(userMap)
                                Toast.makeText(context, "Registered Successfully",
                                    Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            } else {
                                errorMessage = task.exception?.message ?: "Registration Failed"
                            }
                        }
                },
                shape  = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RegOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Register",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(120.dp)) // space above waves
        }
    }
}

// ✅ Field component matching Login style
@Composable
fun RegField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    showDivider: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = placeholder,
                tint = RegFieldIcon,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(text = placeholder, color = RegSubText, fontSize = 14.sp)
                },
                visualTransformation = if (isPassword && !showPassword)
                    PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                trailingIcon = {
                    if (isPassword && onTogglePassword != null) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle",
                            tint = RegFieldIcon,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onTogglePassword() }
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = RegDarkText,
                    unfocusedTextColor      = RegDarkText,
                    cursorColor             = RegOrange
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        if (showDivider) {
            HorizontalDivider(color = RegDivider, thickness = 1.dp)
        }
    }
}