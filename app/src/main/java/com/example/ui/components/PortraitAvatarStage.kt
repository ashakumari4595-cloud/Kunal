package com.example.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhotoCameraFront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.avatar.AvatarFaceAnchor
import com.example.data.local.AvatarVisualMode
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeonOrchid
import com.example.voice.AssistantVoiceState
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PortraitAvatarStage(
    customPortraitPath: String?,
    avatarVisualMode: AvatarVisualMode,
    faceAnchor: AvatarFaceAnchor,
    voiceState: AssistantVoiceState,
    micAmplitude: Float,
    speechAmplitude: Float,
    partialTranscript: String,
    onToggleAvatarMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_ambient")

    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathing"
    )

    val orbitalAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_angle"
    )

    // Natural eye blink cadence
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3200L..5000L).random())
            isBlinking = true
            delay(130L)
            isBlinking = false
        }
    }

    val blinkAlpha by animateFloatAsState(
        targetValue = if (isBlinking) 0.52f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "blink_alpha"
    )

    val smoothMicAmp by animateFloatAsState(
        targetValue = micAmplitude,
        animationSpec = spring(stiffness = 450f),
        label = "mic_amp"
    )

    val smoothSpeechAmp by animateFloatAsState(
        targetValue = speechAmplitude,
        animationSpec = spring(stiffness = 550f),
        label = "speech_amp"
    )

    val activeEnergy = when (voiceState) {
        AssistantVoiceState.LISTENING -> smoothMicAmp.coerceAtLeast(0.18f)
        AssistantVoiceState.SPEAKING -> smoothSpeechAmp.coerceAtLeast(0.22f)
        AssistantVoiceState.THINKING -> 0.5f
        AssistantVoiceState.IDLE, AssistantVoiceState.MUTED -> 0.08f
    }

    val primaryGlowColor by animateColorAsState(
        targetValue = when (voiceState) {
            AssistantVoiceState.LISTENING -> ElectricCyan
            AssistantVoiceState.THINKING -> LuminousViolet
            AssistantVoiceState.SPEAKING -> NeonOrchid
            AssistantVoiceState.MUTED -> CrimsonAlert.copy(alpha = 0.7f)
            AssistantVoiceState.IDLE -> CyberBlue
        },
        animationSpec = tween(durationMillis = 350),
        label = "primary_glow"
    )

    val secondaryGlowColor by animateColorAsState(
        targetValue = when (voiceState) {
            AssistantVoiceState.LISTENING -> EmeraldLive
            AssistantVoiceState.THINKING -> ElectricCyan
            AssistantVoiceState.SPEAKING -> ElectricCyan
            AssistantVoiceState.MUTED -> LuminousViolet.copy(alpha = 0.4f)
            AssistantVoiceState.IDLE -> LuminousViolet
        },
        animationSpec = tween(durationMillis = 350),
        label = "secondary_glow"
    )

    val breathScale = 1.0f + 0.012f * sin(breathingPhase)
    val stateScale = when (voiceState) {
        AssistantVoiceState.LISTENING -> 1.025f + smoothMicAmp * 0.025f
        AssistantVoiceState.SPEAKING -> 1.015f + smoothSpeechAmp * 0.018f
        AssistantVoiceState.THINKING -> 1.008f
        else -> 1.0f
    }

    val imageModel = remember(customPortraitPath) {
        val file = customPortraitPath?.let { File(it) }
        if (file != null && file.exists()) {
            ImageRequest.Builder(context)
                .data(file)
                .crossfade(true)
                .build()
        } else {
            ImageRequest.Builder(context)
                .data(R.drawable.default_avatar_portrait)
                .crossfade(true)
                .build()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 268.dp)
                .fillMaxWidth(0.68f)
                .aspectRatio(0.84f),
            contentAlignment = Alignment.Center
        ) {
            // Outer Futuristic Holographic Rings & Listening Ripples
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.47f

                // Ambient radial aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = 0.24f + activeEnergy * 0.18f),
                            secondaryGlowColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.maxDimension * 0.56f
                    ),
                    center = center,
                    radius = size.maxDimension * 0.56f
                )

                // Listening / Speaking acoustic halo ring
                if (voiceState == AssistantVoiceState.LISTENING ||
                    voiceState == AssistantVoiceState.SPEAKING
                ) {
                    val rippleRadius = baseRadius * (1.03f + activeEnergy * 0.11f)
                    drawCircle(
                        color = primaryGlowColor.copy(alpha = 0.35f + activeEnergy * 0.35f),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Thinking rotating orbital arcs
                if (voiceState == AssistantVoiceState.THINKING) {
                    val arcSize = Size(baseRadius * 2.1f, baseRadius * 2.1f)
                    val topLeft = Offset(center.x - arcSize.width / 2f, center.y - arcSize.height / 2f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ElectricCyan,
                                LuminousViolet,
                                Color.Transparent,
                                ElectricCyan
                            ),
                            center = center
                        ),
                        startAngle = orbitalAngle,
                        sweepAngle = 220f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Personal Portrait Container (Preserves User's Real Face)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        val combined = breathScale * stateScale
                        scaleX = combined
                        scaleY = combined
                        translationY = when (voiceState) {
                            AssistantVoiceState.SPEAKING -> -smoothSpeechAmp * 4f
                            AssistantVoiceState.LISTENING -> -3f
                            else -> sin(breathingPhase) * 2.5f
                        }
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(MidnightSurface)
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(primaryGlowColor, secondaryGlowColor)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Crossfade(
                    targetState = avatarVisualMode,
                    label = "avatar_visual_mode"
                ) { mode ->
                    if (mode == AvatarVisualMode.ANIMATED_CHARACTER) {
                        AvyaktAnimatedPortraitAvatar(
                            voiceState = voiceState,
                            micAmplitude = smoothMicAmp,
                            speechAmplitude = smoothSpeechAmp,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = stringResource(R.string.cd_user_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        // Subtle jaw/speaking micro-stretch synchronized to audio
                                        if (voiceState == AssistantVoiceState.SPEAKING) {
                                            scaleY = 1f + smoothSpeechAmp * 0.016f
                                        }
                                    }
                            )

                            // Realistic subtle facial micro-overlays aligned to detected FaceDetector landmarks
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Subtle top/bottom vignette for futuristic depth
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            AbyssBlack.copy(alpha = 0.55f)
                                        )
                                    )
                                )

                                // Natural periodic eye-blink softness over eye midpoint
                                if (blinkAlpha > 0.01f) {
                                    val eyeX = size.width * faceAnchor.eyeCenterNormalized.x
                                    val eyeY = size.height * faceAnchor.eyeCenterNormalized.y
                                    val eyeW = size.width * faceAnchor.eyeSpanRatio * 1.35f
                                    val eyeH = size.height * 0.025f
                                    drawRoundRect(
                                        color = Color(0xFF1A1412).copy(alpha = blinkAlpha),
                                        topLeft = Offset(eyeX - eyeW / 2f, eyeY - eyeH / 2f),
                                        size = Size(eyeW, eyeH),
                                        cornerRadius = CornerRadius(eyeH / 2f, eyeH / 2f)
                                    )
                                }

                                // Audio-synchronized lip-sync & vocal resonance modulation at real mouth anchor
                                if (voiceState == AssistantVoiceState.SPEAKING && smoothSpeechAmp > 0.08f) {
                                    val mouthX = size.width * faceAnchor.mouthCenterNormalized.x
                                    val mouthY = size.height * faceAnchor.mouthCenterNormalized.y
                                    val mouthW = size.width * faceAnchor.mouthWidthRatio
                                    val mouthOpenH = (size.height * 0.036f) * smoothSpeechAmp

                                    // Soft natural inner mouth shadow modulation (preserves user face)
                                    drawOval(
                                        color = Color(0xFF1A0D0B).copy(alpha = 0.55f * smoothSpeechAmp),
                                        topLeft = Offset(mouthX - mouthW * 0.42f, mouthY - mouthOpenH * 0.35f),
                                        size = Size(mouthW * 0.84f, mouthOpenH.coerceAtLeast(2.dp.toPx()))
                                    )
                                    // Subtle holographic voice resonance halo below chin
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                ElectricCyan.copy(alpha = 0.20f * smoothSpeechAmp),
                                                Color.Transparent
                                            ),
                                            center = Offset(mouthX, mouthY + size.height * 0.05f),
                                            radius = mouthW * 1.4f
                                        ),
                                        center = Offset(mouthX, mouthY + size.height * 0.05f),
                                        radius = mouthW * 1.4f
                                    )
                                }
                            }
                        }
                    }
                }

                // Top-left quick switch between Photo Portrait & Animated Character Avatar
                Surface(
                    color = AbyssBlack.copy(alpha = 0.72f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, primaryGlowColor.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleAvatarMode)
                        .testTag("avatar_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (avatarVisualMode == AvatarVisualMode.ANIMATED_CHARACTER) {
                                Icons.Rounded.Face
                            } else {
                                Icons.Rounded.PhotoCameraFront
                            },
                            contentDescription = "Switch avatar mode",
                            tint = primaryGlowColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = avatarVisualMode.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Bottom-aligned live audio waveform & state pill inside portrait frame
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (voiceState == AssistantVoiceState.SPEAKING ||
                        voiceState == AssistantVoiceState.LISTENING
                    ) {
                        AudioSpectrumBars(
                            amplitude = activeEnergy,
                            primaryColor = primaryGlowColor,
                            secondaryColor = secondaryGlowColor,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    AvatarStateBadge(
                        voiceState = voiceState,
                        accentColor = primaryGlowColor
                    )
                }
            }
        }

        if (partialTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MidnightSurface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "“$partialTranscript”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun AvatarStateBadge(
    voiceState: AssistantVoiceState,
    accentColor: Color
) {
    val label = when (voiceState) {
        AssistantVoiceState.IDLE -> stringResource(R.string.state_idle)
        AssistantVoiceState.LISTENING -> stringResource(R.string.state_listening)
        AssistantVoiceState.THINKING -> stringResource(R.string.state_thinking)
        AssistantVoiceState.SPEAKING -> stringResource(R.string.state_speaking)
        AssistantVoiceState.MUTED -> stringResource(R.string.state_muted)
    }

    val icon = when (voiceState) {
        AssistantVoiceState.LISTENING -> Icons.Rounded.Mic
        AssistantVoiceState.MUTED -> Icons.Rounded.MicOff
        AssistantVoiceState.SPEAKING -> Icons.Rounded.GraphicEq
        else -> Icons.Rounded.AutoAwesome
    }

    Surface(
        color = AbyssBlack.copy(alpha = 0.78f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AudioSpectrumBars(
    amplitude: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_shift"
    )

    Canvas(modifier = modifier) {
        val barCount = 15
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.48f).coerceAtLeast(2.dp.toPx())

        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f)
            val envelope = 1f - distFromCenter * 0.55f
            val dynamic = 0.35f + 0.65f * kotlin.math.abs(sin(waveShift + i * 0.6f))
            val barH = (size.height * amplitude * envelope * dynamic)
                .coerceIn(3.dp.toPx(), size.height)

            val x = i * spacing + (spacing - barWidth) / 2f
            val y = (size.height - barH) / 2f
            val color = if (i % 2 == 0) primaryColor else secondaryColor

            drawRoundRect(
                color = color.copy(alpha = 0.92f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
