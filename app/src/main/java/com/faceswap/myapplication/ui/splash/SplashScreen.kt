package com.faceswap.myapplication.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlin.math.*

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val BgDeep       = Color(0xFF030010)
private val BgMid        = Color(0xFF0D0030)
private val BgTop        = Color(0xFF1A0050)
private val CyanGlow     = Color(0xFF00E5FF)
private val CyanDim      = Color(0xFF00B8D4)
private val PinkGlow     = Color(0xFFFF2D78)
private val GoldGlow     = Color(0xFFFFCC00)
private val VioletGlow   = Color(0xFFBB86FC)
private val PureWhite    = Color(0xFFFFFFFF)

@Composable
fun SplashScreen(navController: NavHostController) {

    // ── Navigate after delay ──────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(3500)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            navController.navigate("home") { popUpTo("splash") { inclusive = true } }
        } else {
            navController.navigate("login") { popUpTo("splash") { inclusive = true } }
        }
    }

    val inf = rememberInfiniteTransition(label = "master")

    // Orbit angles
    val orb1 by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "o1")
    val orb2 by inf.animateFloat(360f, 0f,
        infiniteRepeatable(tween(5500, easing = LinearEasing)), label = "o2")
    val orb3 by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "o3")
    val orb4 by inf.animateFloat(360f, 0f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "o4")

    // Holographic face animations
    val scanLine by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "scan")
    val dotPulse by inf.animateFloat(0.4f, 1f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "dot")
    val gridFlicker by inf.animateFloat(0.65f, 1f,
        infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse), label = "flicker")

    // Logo outer glow pulse
    val glow by inf.animateFloat(0.35f, 0.9f,
        infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), label = "glow")
    val pulse by inf.animateFloat(1f, 1.06f,
        infiniteRepeatable(tween(950, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")

    // Tagline shimmer
    val shimmer by inf.animateFloat(0.6f, 1f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse), label = "shimmer")

    // Entry fade-in
    var visible by remember { mutableStateOf(false) }
    val fadeIn by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1300), label = "fade"
    )
    LaunchedEffect(Unit) { visible = true }

    // ── Root container ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgMid, BgTop))),
        contentAlignment = Alignment.Center
    ) {

        // ── Orbiting background circles ───────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Faint ring traces
            drawOrbitRing(cx, cy, 400.dp.toPx(), CyanGlow, 0.05f)
            drawOrbitRing(cx, cy, 320.dp.toPx(), PinkGlow, 0.06f)
            drawOrbitRing(cx, cy, 210.dp.toPx(), VioletGlow, 0.04f)
            drawOrbitRing(cx, cy, 130.dp.toPx(), GoldGlow, 0.07f)

            // Outer orbit
            drawOrbBlob(cx, cy, 400.dp.toPx(), orb3,          CyanGlow,   0.18f, 42.dp.toPx())
            drawOrbBlob(cx, cy, 400.dp.toPx(), orb3 + 90f,    PinkGlow,   0.14f, 30.dp.toPx())
            drawOrbBlob(cx, cy, 400.dp.toPx(), orb3 + 180f,   GoldGlow,   0.12f, 36.dp.toPx())
            drawOrbBlob(cx, cy, 400.dp.toPx(), orb3 + 270f,   VioletGlow, 0.16f, 28.dp.toPx())
            // Mid orbit
            drawOrbBlob(cx, cy, 320.dp.toPx(), orb1,          CyanGlow,   0.20f, 26.dp.toPx())
            drawOrbBlob(cx, cy, 320.dp.toPx(), orb1 + 120f,   PinkGlow,   0.18f, 20.dp.toPx())
            drawOrbBlob(cx, cy, 320.dp.toPx(), orb1 + 240f,   GoldGlow,   0.14f, 18.dp.toPx())
            // Counter orbit
            drawOrbBlob(cx, cy, 210.dp.toPx(), orb2,          VioletGlow, 0.22f, 22.dp.toPx())
            drawOrbBlob(cx, cy, 210.dp.toPx(), orb2 + 180f,   CyanGlow,   0.18f, 16.dp.toPx())
            // Fast inner
            drawOrbBlob(cx, cy, 130.dp.toPx(), orb4,          PinkGlow,   0.25f, 14.dp.toPx())
            drawOrbBlob(cx, cy, 130.dp.toPx(), orb4 + 180f,   GoldGlow,   0.20f, 12.dp.toPx())
        }

        // ── Foreground content ────────────────────────────────────────────────
        Column(
            modifier = Modifier.alpha(fadeIn),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo badge ────────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {

                // Outer cyan glow halo
                Box(
                    modifier = Modifier
                        .size(162.dp)
                        .alpha(glow)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(
                            listOf(CyanGlow.copy(0.50f), CyanDim.copy(0.15f), Color.Transparent)
                        ))
                )

                // Sweep gradient ring border
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(
                            listOf(CyanGlow, PinkGlow, GoldGlow, VioletGlow, CyanGlow)
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner dark circle = holographic face canvas
                    Box(
                        modifier = Modifier
                            .size(122.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000814)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawHolographicFace(scanLine, dotPulse, gridFlicker)
                        }
                    }
                }
            }

            Spacer(Modifier.height(34.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = "Jhaishna",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(6.dp))

            // Animated gradient divider line
            Canvas(modifier = Modifier.size(width = 180.dp, height = 3.dp)) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, CyanGlow, GoldGlow, PinkGlow, Color.Transparent)
                    ),
                    size = size
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "FaceSwap  AI",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = VioletGlow,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Tagline pill
            Surface(shape = RoundedCornerShape(50), color = CyanGlow.copy(0.10f)) {
                Text(
                    text = "✦  Swap Faces. Spark Magic.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                    fontSize = 13.sp,
                    color = CyanGlow.copy(alpha = shimmer),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(72.dp))
            LoadingDots()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HOLOGRAPHIC WIREFRAME FACE
// ══════════════════════════════════════════════════════════════════════════════
private fun DrawScope.drawHolographicFace(
    scanLine: Float,
    dotPulse: Float,
    gridFlicker: Float
) {
    val w  = size.width
    val h  = size.height
    val cx = w / 2f
    val cy = h * 0.50f

    // Face oval dimensions
    val fRx = w * 0.30f   // horizontal radius
    val fRy = h * 0.38f   // vertical radius

    val cyan    = Color(0xFF00E5FF)
    val cyanDim = Color(0xFF00B8D4)
    val sw      = 1.1f

    // ── 1. Face oval outline ──────────────────────────────────────────────────
    drawOval(
        color     = cyan.copy(gridFlicker * 0.95f),
        topLeft   = Offset(cx - fRx, cy - fRy),
        size      = Size(fRx * 2f, fRy * 2f),
        style     = Stroke(sw * 1.8f)
    )

    // ── 2. Horizontal latitude lines ──────────────────────────────────────────
    val numH = 11
    for (i in 1 until numH) {
        val t  = i.toFloat() / numH
        val ny = -1f + t * 2f                          // normalised –1..1
        val y  = cy + ny * fRy
        val xR = fRx * sqrt(max(0f, 1f - ny * ny))
        if (xR < 2f) continue
        val alpha = (0.25f + 0.45f * (1f - abs(ny))) * gridFlicker
        drawLine(cyan.copy(alpha), Offset(cx - xR, y), Offset(cx + xR, y), sw)
    }

    // ── 3. Vertical longitude lines (as paths inside oval) ───────────────────
    val numV = 9
    for (i in 1 until numV) {
        val t  = i.toFloat() / numV
        val nx = -1f + t * 2f                          // normalised –1..1
        val xPos = cx + nx * fRx
        val yFrac = sqrt(max(0f, 1f - nx * nx))
        val yTop  = cy - yFrac * fRy
        val yBot  = cy + yFrac * fRy
        if (yBot - yTop < 4f) continue
        val alpha = (0.22f + 0.35f * (1f - abs(nx))) * gridFlicker
        val path  = Path().apply { moveTo(xPos, yTop); lineTo(xPos, yBot) }
        drawPath(path, cyan.copy(alpha), style = Stroke(sw))
    }

    // ── 4. Eye socket ovals ───────────────────────────────────────────────────
    val eyeY  = cy - fRy * 0.22f
    val eRx   = fRx * 0.24f
    val eRy   = fRy * 0.13f
    val eyeLX = cx - fRx * 0.32f
    val eyeRX = cx + fRx * 0.32f

    drawOval(cyan.copy(0.85f),
        Offset(eyeLX - eRx, eyeY - eRy), Size(eRx * 2f, eRy * 2f),
        style = Stroke(sw * 1.4f))
    drawOval(cyan.copy(0.85f),
        Offset(eyeRX - eRx, eyeY - eRy), Size(eRx * 2f, eRy * 2f),
        style = Stroke(sw * 1.4f))

    // ── 5. Glowing scan points ────────────────────────────────────────────────
    val dr  = w * 0.035f * dotPulse      // dot core radius
    val dgr = dr * 3.0f                  // glow radius

    // Left eye dot
    drawCircle(cyan.copy(0.25f * dotPulse), dgr,  Offset(eyeLX, eyeY))
    drawCircle(cyan.copy(0.55f * dotPulse), dr * 1.5f, Offset(eyeLX, eyeY))
    drawCircle(cyan,                         dr,   Offset(eyeLX, eyeY))
    // Right eye dot
    drawCircle(cyan.copy(0.25f * dotPulse), dgr,  Offset(eyeRX, eyeY))
    drawCircle(cyan.copy(0.55f * dotPulse), dr * 1.5f, Offset(eyeRX, eyeY))
    drawCircle(cyan,                         dr,   Offset(eyeRX, eyeY))
    // Nose dot
    val noseDr = dr * 0.75f
    val noseY  = cy + fRy * 0.12f
    drawCircle(cyan.copy(0.22f * dotPulse), dgr * 0.7f, Offset(cx, noseY))
    drawCircle(cyan.copy(0.5f  * dotPulse), noseDr * 1.5f, Offset(cx, noseY))
    drawCircle(cyan, noseDr, Offset(cx, noseY))

    // ── 6. Corner scan brackets ───────────────────────────────────────────────
    val padX  = cx - fRx - w * 0.08f
    val padY  = cy - fRy - h * 0.05f
    val padX2 = cx + fRx + w * 0.08f
    val padY2 = cy + fRy + h * 0.05f
    val bLen  = w * 0.13f
    val bStk  = sw * 2.6f
    val bCol  = cyan

    val l = padX.coerceAtLeast(1f)
    val r = padX2.coerceAtMost(w - 1f)
    val t = padY.coerceAtLeast(1f)
    val b = padY2.coerceAtMost(h - 1f)

    // Top-left
    drawLine(bCol, Offset(l, t + bLen), Offset(l, t), bStk)
    drawLine(bCol, Offset(l, t), Offset(l + bLen, t), bStk)
    // Top-right
    drawLine(bCol, Offset(r, t + bLen), Offset(r, t), bStk)
    drawLine(bCol, Offset(r, t), Offset(r - bLen, t), bStk)
    // Bottom-left
    drawLine(bCol, Offset(l, b - bLen), Offset(l, b), bStk)
    drawLine(bCol, Offset(l, b), Offset(l + bLen, b), bStk)
    // Bottom-right
    drawLine(bCol, Offset(r, b - bLen), Offset(r, b), bStk)
    drawLine(bCol, Offset(r, b), Offset(r - bLen, b), bStk)

    // ── 7. Animated horizontal scan line ─────────────────────────────────────
    val scanY  = (cy - fRy) + scanLine * (fRy * 2f)
    val nyS    = (scanY - cy) / fRy
    val scanXR = if (abs(nyS) < 1f) fRx * sqrt(max(0f, 1f - nyS * nyS)) else 0f

    if (scanXR > 2f) {
        // Glow trail below scan line
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, cyan.copy(0.07f)),
                startY = (cy - fRy), endY = scanY
            ),
            topLeft = Offset(cx - fRx, cy - fRy),
            size    = Size(fRx * 2f, scanY - (cy - fRy))
        )
        // The bright scan line itself
        val scanPath = Path().apply {
            moveTo(cx - scanXR, scanY)
            lineTo(cx + scanXR, scanY)
        }
        drawPath(
            path  = scanPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, cyan.copy(0.9f), PureWhite, cyan.copy(0.9f), Color.Transparent),
                startX = cx - scanXR, endX = cx + scanXR
            ),
            style = Stroke(sw * 2.0f)
        )
    }
}

// ── Orbit ring trace ──────────────────────────────────────────────────────────
private fun DrawScope.drawOrbitRing(cx: Float, cy: Float, r: Float, color: Color, alpha: Float) {
    drawCircle(color.copy(alpha), r, Offset(cx, cy), style = Stroke(1.2f))
}

// ── Single orbiting blob ──────────────────────────────────────────────────────
private fun DrawScope.drawOrbBlob(
    cx: Float, cy: Float,
    orbitR: Float, angleDeg: Float,
    color: Color, alpha: Float, dotR: Float
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val x   = cx + orbitR * cos(rad).toFloat()
    val y   = cy + orbitR * sin(rad).toFloat()
    drawCircle(color.copy(alpha * 0.28f), dotR * 2.2f, Offset(x, y))
    drawCircle(color.copy(alpha),         dotR,          Offset(x, y))
}

// ── Tri-color animated loading dots ──────────────────────────────────────────
@Composable
private fun LoadingDots() {
    val inf = rememberInfiniteTransition(label = "dots")

    @Composable
    fun dot(delayMs: Int): Float {
        val v by inf.animateFloat(0.3f, 1f,
            infiniteRepeatable(
                tween(500, delayMillis = delayMs, easing = EaseInOutSine),
                RepeatMode.Reverse), label = "d$delayMs")
        return v
    }

    val a1 = dot(0); val a2 = dot(170); val a3 = dot(340)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(a1 to CyanGlow, a2 to PinkGlow, a3 to GoldGlow).forEach { (alpha, color) ->
            Box(
                Modifier.size(11.dp).alpha(alpha).clip(CircleShape).background(color)
            )
        }
    }
}