package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AvyaktTheme
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonOrchid
import com.example.voice.AssistantVoiceState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure Jetpack Compose animated portrait avatar for AVYAKT AI.
 * Renders an expressive futuristic character with breathing motion, natural blinking,
 * dynamic eye gaze, holographic neural rings, and audio-synchronized mouth lip-sync.
 */
@Composable
fun AvyaktAnimatedPortraitAvatar(
    voiceState: AssistantVoiceState,
    micAmplitude: Float,
    speechAmplitude: Float,
    modifier: Modifier = Modifier,
    contentDescriptionText: String = "AVYAKT AI animated character portrait avatar"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avyakt_character")

    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_phase"
    )

    val orbitalAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_angle"
    )

    val gazeSway by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gaze_sway"
    )

    // Natural eye blinking
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2800L..4800L).random())
            isBlinking = true
            delay(120L)
            isBlinking = false
        }
    }

    val eyeOpenFactor by animateFloatAsState(
        targetValue = if (isBlinking) 0.08f else 1f,
        animationSpec = tween(durationMillis = 85),
        label = "eye_open"
    )

    val smoothMic by animateFloatAsState(
        targetValue = micAmplitude.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 480f),
        label = "smooth_mic"
    )

    val smoothSpeech by animateFloatAsState(
        targetValue = speechAmplitude.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 620f),
        label = "smooth_speech"
    )

    val accentPrimary by animateColorAsState(
        targetValue = when (voiceState) {
            AssistantVoiceState.LISTENING -> ElectricCyan
            AssistantVoiceState.THINKING -> LuminousViolet
            AssistantVoiceState.SPEAKING -> NeonOrchid
            AssistantVoiceState.MUTED -> CrimsonAlert
            AssistantVoiceState.IDLE -> CyberBlue
        },
        animationSpec = tween(durationMillis = 320),
        label = "accent_primary"
    )

    val accentSecondary by animateColorAsState(
        targetValue = when (voiceState) {
            AssistantVoiceState.LISTENING -> EmeraldLive
            AssistantVoiceState.THINKING -> ElectricCyan
            AssistantVoiceState.SPEAKING -> ElectricCyan
            AssistantVoiceState.MUTED -> LuminousViolet.copy(alpha = 0.5f)
            AssistantVoiceState.IDLE -> LuminousViolet
        },
        animationSpec = tween(durationMillis = 320),
        label = "accent_secondary"
    )

    val headTiltDegrees = when (voiceState) {
        AssistantVoiceState.LISTENING -> -2.2f + smoothMic * 1.4f
        AssistantVoiceState.THINKING -> 2.8f * gazeSway
        AssistantVoiceState.SPEAKING -> sin(breathPhase * 2f) * 1.5f
        else -> sin(breathPhase) * 0.8f
    }

    Box(
        modifier = modifier.semantics {
            contentDescription = contentDescriptionText
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val breathOffset = sin(breathPhase) * (h * 0.01f)
            val energy = when (voiceState) {
                AssistantVoiceState.LISTENING -> smoothMic.coerceAtLeast(0.2f)
                AssistantVoiceState.SPEAKING -> smoothSpeech.coerceAtLeast(0.25f)
                AssistantVoiceState.THINKING -> 0.55f
                else -> 0.1f
            }

            // 1. Deep Obsidian Studio Backdrop + Holographic Neural Halo
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AbyssBlack,
                        MidnightSurface,
                        Color(0xFF101836)
                    )
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = 0.28f + energy * 0.18f),
                        accentSecondary.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(cx, h * 0.42f),
                    radius = w * 0.52f
                ),
                center = Offset(cx, h * 0.42f),
                radius = w * 0.52f
            )

            // Rotating holographic HUD ring behind head
            val haloCenter = Offset(cx, h * 0.41f + breathOffset * 0.5f)
            val haloRadius = w * 0.34f
            drawCircle(
                color = accentPrimary.copy(alpha = 0.18f + energy * 0.20f),
                radius = haloRadius * (1f + energy * 0.06f),
                center = haloCenter,
                style = Stroke(width = 1.6.dp.toPx())
            )

            if (voiceState == AssistantVoiceState.THINKING || voiceState == AssistantVoiceState.SPEAKING) {
                val arcSize = Size(haloRadius * 2.18f, haloRadius * 2.18f)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            accentPrimary,
                            accentSecondary,
                            Color.Transparent,
                            accentPrimary
                        ),
                        center = haloCenter
                    ),
                    startAngle = orbitalAngle,
                    sweepAngle = if (voiceState == AssistantVoiceState.THINKING) 230f else 150f,
                    useCenter = false,
                    topLeft = Offset(haloCenter.x - arcSize.width / 2f, haloCenter.y - arcSize.height / 2f),
                    size = arcSize,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 2. Shoulders, Futuristic Blazer & Glowing Chest Core
            val shoulderTopY = h * 0.65f + breathOffset
            val torsoPath = Path().apply {
                moveTo(cx - w * 0.42f, h)
                cubicTo(
                    cx - w * 0.40f, shoulderTopY + h * 0.04f,
                    cx - w * 0.24f, shoulderTopY,
                    cx - w * 0.11f, shoulderTopY - h * 0.02f
                )
                lineTo(cx + w * 0.11f, shoulderTopY - h * 0.02f)
                cubicTo(
                    cx + w * 0.24f, shoulderTopY,
                    cx + w * 0.40f, shoulderTopY + h * 0.04f,
                    cx + w * 0.42f, h
                )
                close()
            }
            drawPath(
                path = torsoPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF171E35),
                        Color(0xFF0C1020)
                    ),
                    startY = shoulderTopY,
                    endY = h
                )
            )
            // Blazer collar trim
            drawPath(
                path = Path().apply {
                    moveTo(cx - w * 0.12f, shoulderTopY - h * 0.01f)
                    lineTo(cx, shoulderTopY + h * 0.14f)
                    lineTo(cx + w * 0.12f, shoulderTopY - h * 0.01f)
                },
                brush = Brush.linearGradient(listOf(accentPrimary, accentSecondary)),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Inner shirt / resonant core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = 0.8f),
                        accentSecondary.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(cx, shoulderTopY + h * 0.18f),
                    radius = w * 0.08f
                ),
                center = Offset(cx, shoulderTopY + h * 0.18f),
                radius = w * 0.08f
            )

            // Neck
            val neckW = w * 0.14f
            val neckH = h * 0.13f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFC2896B), Color(0xFFD69F80))
                ),
                topLeft = Offset(cx - neckW / 2f, h * 0.54f + breathOffset),
                size = Size(neckW, neckH),
                cornerRadius = CornerRadius(neckW * 0.35f, neckW * 0.35f)
            )

            // 3. Animated Head Group (Tilts & Moves with Speech/Breathing)
            val headCenter = Offset(cx, h * 0.41f + breathOffset * 1.1f)
            rotate(degrees = headTiltDegrees, pivot = headCenter) {
                translate(
                    left = 0f,
                    top = if (voiceState == AssistantVoiceState.SPEAKING) -smoothSpeech * h * 0.012f else 0f
                ) {
                    drawAnimatedCharacterHead(
                        headCenter = headCenter,
                        headWidth = w * 0.34f,
                        headHeight = h * 0.31f,
                        voiceState = voiceState,
                        eyeOpenFactor = eyeOpenFactor,
                        gazeSway = gazeSway,
                        micAmplitude = smoothMic,
                        speechAmplitude = smoothSpeech,
                        accentPrimary = accentPrimary,
                        accentSecondary = accentSecondary
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAnimatedCharacterHead(
    headCenter: Offset,
    headWidth: Float,
    headHeight: Float,
    voiceState: AssistantVoiceState,
    eyeOpenFactor: Float,
    gazeSway: Float,
    micAmplitude: Float,
    speechAmplitude: Float,
    accentPrimary: Color,
    accentSecondary: Color
) {
    val cx = headCenter.x
    val cy = headCenter.y

    // Acoustic ear / neural receiver nodes (pulse when listening or speaking)
    val earNodeRadius = headWidth * (0.09f + if (voiceState == AssistantVoiceState.LISTENING) micAmplitude * 0.05f else 0f)
    val leftEar = Offset(cx - headWidth * 0.52f, cy + headHeight * 0.04f)
    val rightEar = Offset(cx + headWidth * 0.52f, cy + headHeight * 0.04f)

    listOf(leftEar, rightEar).forEach { ear ->
        drawCircle(
            color = Color(0xFFD19777),
            radius = earNodeRadius,
            center = ear
        )
        drawCircle(
            color = accentPrimary.copy(
                alpha = if (voiceState == AssistantVoiceState.LISTENING) 0.55f + micAmplitude * 0.45f else 0.45f
            ),
            radius = earNodeRadius * 1.28f,
            center = ear,
            style = Stroke(width = 1.6.dp.toPx())
        )
    }

    // Face contour (warm balanced skin tone + subtle futuristic rim highlight)
    val facePath = Path().apply {
        moveTo(cx, cy - headHeight * 0.50f)
        cubicTo(
            cx + headWidth * 0.52f, cy - headHeight * 0.50f,
            cx + headWidth * 0.50f, cy + headHeight * 0.14f,
            cx + headWidth * 0.34f, cy + headHeight * 0.36f
        )
        cubicTo(
            cx + headWidth * 0.18f, cy + headHeight * 0.52f,
            cx - headWidth * 0.18f, cy + headHeight * 0.52f,
            cx - headWidth * 0.34f, cy + headHeight * 0.36f
        )
        cubicTo(
            cx - headWidth * 0.50f, cy + headHeight * 0.14f,
            cx - headWidth * 0.52f, cy - headHeight * 0.50f,
            cx, cy - headHeight * 0.50f
        )
        close()
    }
    drawPath(
        path = facePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE5B190),
                Color(0xFFD69A78),
                Color(0xFFC18262)
            ),
            startY = cy - headHeight * 0.5f,
            endY = cy + headHeight * 0.5f
        )
    )
    drawPath(
        path = facePath,
        brush = Brush.linearGradient(
            colors = listOf(accentPrimary.copy(alpha = 0.4f), accentSecondary.copy(alpha = 0.2f))
        ),
        style = Stroke(width = 1.4.dp.toPx())
    )

    // Stylized Modern Hair with subtle Cyan/Violet Rim Accent
    val hairPath = Path().apply {
        moveTo(cx - headWidth * 0.51f, cy - headHeight * 0.06f)
        cubicTo(
            cx - headWidth * 0.56f, cy - headHeight * 0.42f,
            cx - headWidth * 0.28f, cy - headHeight * 0.64f,
            cx + headWidth * 0.04f, cy - headHeight * 0.60f
        )
        cubicTo(
            cx + headWidth * 0.38f, cy - headHeight * 0.58f,
            cx + headWidth * 0.55f, cy - headHeight * 0.36f,
            cx + headWidth * 0.50f, cy - headHeight * 0.06f
        )
        cubicTo(
            cx + headWidth * 0.42f, cy - headHeight * 0.25f,
            cx + headWidth * 0.18f, cy - headHeight * 0.34f,
            cx - headWidth * 0.05f, cy - headHeight * 0.30f
        )
        cubicTo(
            cx - headWidth * 0.28f, cy - headHeight * 0.27f,
            cx - headWidth * 0.42f, cy - headHeight * 0.20f,
            cx - headWidth * 0.51f, cy - headHeight * 0.06f
        )
        close()
    }
    drawPath(
        path = hairPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF121629),
                Color(0xFF1F2744),
                Color(0xFF0E1220)
            )
        )
    )
    // Futuristic hair highlight streak
    drawArc(
        color = accentPrimary.copy(alpha = 0.5f),
        startAngle = 210f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(cx - headWidth * 0.40f, cy - headHeight * 0.56f),
        size = Size(headWidth * 0.80f, headHeight * 0.45f),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // Eyebrows (expressive lift in Listening / Thinking)
    val browLift = when (voiceState) {
        AssistantVoiceState.LISTENING -> -headHeight * 0.025f
        AssistantVoiceState.THINKING -> -headHeight * 0.02f
        AssistantVoiceState.SPEAKING -> -headHeight * 0.01f * speechAmplitude
        else -> 0f
    }
    val eyeSpan = headWidth * 0.22f
    val eyeY = cy - headHeight * 0.03f
    val browY = eyeY - headHeight * 0.105f + browLift

    listOf(-1f, 1f).forEach { side ->
        val bx = cx + side * eyeSpan
        val browPath = Path().apply {
            moveTo(bx - headWidth * 0.10f, browY + (if (side < 0) 2f else 0f))
            quadraticTo(
                bx,
                browY - headHeight * 0.025f,
                bx + headWidth * 0.10f,
                browY + (if (side > 0) 2f else 0f)
            )
        }
        drawPath(
            path = browPath,
            color = Color(0xFF1A1D2B),
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Eyes with natural blink + dynamic gaze
    val eyeW = headWidth * 0.17f
    val eyeH = (headHeight * 0.085f) * eyeOpenFactor
    val gazeX = when (voiceState) {
        AssistantVoiceState.THINKING -> gazeSway * eyeW * 0.18f
        AssistantVoiceState.IDLE -> gazeSway * eyeW * 0.08f
        else -> 0f
    }
    val gazeY = if (voiceState == AssistantVoiceState.THINKING) -eyeH * 0.16f else 0f

    listOf(-1f, 1f).forEach { side ->
        val ex = cx + side * eyeSpan
        // Sclera
        drawOval(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(ex - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH.coerceAtLeast(1.5.dp.toPx()))
        )
        if (eyeOpenFactor > 0.25f) {
            val irisR = (eyeH * 0.44f).coerceAtLeast(2.dp.toPx())
            val irisCenter = Offset(ex + gazeX, eyeY + gazeY)
            // Iris with subtle futuristic cyan-brown depth
            drawCircle(
                color = Color(0xFF231F20),
                radius = irisR,
                center = irisCenter
            )
            drawCircle(
                color = accentPrimary.copy(alpha = 0.35f),
                radius = irisR * 0.92f,
                center = irisCenter,
                style = Stroke(width = 1.dp.toPx())
            )
            // Pupil
            drawCircle(
                color = AbyssBlack,
                radius = irisR * 0.52f,
                center = irisCenter
            )
            // Catchlight reflection
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = irisR * 0.24f,
                center = Offset(irisCenter.x - irisR * 0.28f, irisCenter.y - irisR * 0.28f)
            )
        }
    }

    // Subtle Nose Bridge & Tip Shadow
    val nosePath = Path().apply {
        moveTo(cx, cy - headHeight * 0.01f)
        lineTo(cx - headWidth * 0.025f, cy + headHeight * 0.12f)
        quadraticTo(cx, cy + headHeight * 0.145f, cx + headWidth * 0.035f, cy + headHeight * 0.12f)
    }
    drawPath(
        path = nosePath,
        color = Color(0xFF9C6245).copy(alpha = 0.55f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Audio-Synchronized Expressive Mouth (Lip-Sync)
    val mouthY = cy + headHeight * 0.255f
    val mouthW = headWidth * 0.28f

    when {
        voiceState == AssistantVoiceState.SPEAKING -> {
            val openH = (headHeight * 0.105f * speechAmplitude).coerceIn(2.dp.toPx(), headHeight * 0.11f)
            // Outer lips / dark mouth cavity
            drawOval(
                color = Color(0xFF42171A),
                topLeft = Offset(cx - mouthW * 0.45f, mouthY - openH * 0.45f),
                size = Size(mouthW * 0.90f, openH)
            )
            // Subtle upper teeth highlight when mouth opens wider
            if (speechAmplitude > 0.45f) {
                drawRoundRect(
                    color = Color(0xFFF8FAFC).copy(alpha = 0.85f),
                    topLeft = Offset(cx - mouthW * 0.28f, mouthY - openH * 0.32f),
                    size = Size(mouthW * 0.56f, openH * 0.24f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
            // Lower lip contour
            drawArc(
                color = Color(0xFF9E4F49),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - mouthW * 0.45f, mouthY - openH * 0.45f),
                size = Size(mouthW * 0.90f, openH.coerceAtLeast(3.dp.toPx())),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Vocal resonance aura around chin when speaking
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentPrimary.copy(alpha = 0.22f * speechAmplitude),
                        Color.Transparent
                    ),
                    center = Offset(cx, mouthY + headHeight * 0.08f),
                    radius = mouthW * 0.9f
                ),
                center = Offset(cx, mouthY + headHeight * 0.08f),
                radius = mouthW * 0.9f
            )
        }

        voiceState == AssistantVoiceState.LISTENING -> {
            // Attentive slight smile
            val mouthPath = Path().apply {
                moveTo(cx - mouthW * 0.42f, mouthY)
                quadraticTo(cx, mouthY + headHeight * 0.032f, cx + mouthW * 0.42f, mouthY)
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF8C4642),
                style = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        voiceState == AssistantVoiceState.THINKING -> {
            // Subtle contemplative curve
            val mouthPath = Path().apply {
                moveTo(cx - mouthW * 0.34f, mouthY + headHeight * 0.01f)
                quadraticTo(
                    cx + headWidth * 0.03f,
                    mouthY - headHeight * 0.01f,
                    cx + mouthW * 0.34f,
                    mouthY + headHeight * 0.005f
                )
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF8C4642),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        else -> {
            // Calm confident smile (IDLE / MUTED)
            val mouthPath = Path().apply {
                moveTo(cx - mouthW * 0.44f, mouthY - headHeight * 0.005f)
                quadraticTo(cx, mouthY + headHeight * 0.045f, cx + mouthW * 0.44f, mouthY - headHeight * 0.005f)
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF8C4642),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // Subtle futuristic biometric jawline marker
    val chinY = cy + headHeight * 0.44f
    drawLine(
        color = accentPrimary.copy(alpha = 0.45f),
        start = Offset(cx - headWidth * 0.08f, chinY),
        end = Offset(cx + headWidth * 0.08f, chinY),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF060913)
@Composable
private fun AvyaktAnimatedPortraitAvatarSpeakingPreview() {
    AvyaktTheme {
        AvyaktAnimatedPortraitAvatar(
            voiceState = AssistantVoiceState.SPEAKING,
            micAmplitude = 0f,
            speechAmplitude = 0.72f,
            modifier = Modifier.size(220.dp, 260.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060913)
@Composable
private fun AvyaktAnimatedPortraitAvatarListeningPreview() {
    AvyaktTheme {
        AvyaktAnimatedPortraitAvatar(
            voiceState = AssistantVoiceState.LISTENING,
            micAmplitude = 0.65f,
            speechAmplitude = 0f,
            modifier = Modifier.size(220.dp, 260.dp)
        )
    }
}
