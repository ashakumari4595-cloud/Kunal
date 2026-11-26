package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import java.util.concurrent.Executors

@Composable
fun CameraVisionCard(
    useFrontCamera: Boolean,
    onFlipCamera: () -> Unit,
    onCloseCamera: () -> Unit,
    onFrameSampled: (Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProvider?.unbindAll() }
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(useFrontCamera) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                var lastAnalyzedMillis = 0L
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                val now = System.currentTimeMillis()
                                if (now - lastAnalyzedMillis >= 750L) {
                                    lastAnalyzedMillis = now
                                    val rawBitmap = imageProxy.toBitmap()
                                    val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
                                    val maxDim = 320f
                                    val scale = minOf(
                                        maxDim / rawBitmap.width.coerceAtLeast(1),
                                        maxDim / rawBitmap.height.coerceAtLeast(1),
                                        1f
                                    )
                                    val matrix = Matrix().apply {
                                        postScale(scale, scale)
                                        if (rotation != 0f) postRotate(rotation)
                                    }
                                    val compactBitmap = Bitmap.createBitmap(
                                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                    )
                                    if (compactBitmap !== rawBitmap) rawBitmap.recycle()
                                    onFrameSampled(compactBitmap)
                                }
                            } catch (e: Exception) {
                                Log.w("CameraVisionCard", "Frame sampling error", e)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e("CameraVisionCard", "Camera binding failed", e)
                onCameraError("Camera unavailable on this device: ${e.localizedMessage ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AbyssBlack)
            .border(1.5.dp, ElectricCyan.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Clear Live Camera Status Indicator (no secret access)
        Surface(
            color = AbyssBlack.copy(alpha = 0.75f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(EmeraldLive)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.camera_on_indicator),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Camera Lens Flip & Close controls (48dp accessible touch targets)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("camera_flip_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cameraswitch,
                    contentDescription = "Switch camera lens",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onCloseCamera,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("camera_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Turn off camera",
                    tint = Color.White
                )
            }
        }
    }
}
