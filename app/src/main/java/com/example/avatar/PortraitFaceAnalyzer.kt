package com.example.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.media.FaceDetector
import android.util.Log
import androidx.annotation.DrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AvatarFaceAnchor(
    val mouthCenterNormalized: PointF = PointF(0.5f, 0.56f),
    val mouthWidthRatio: Float = 0.16f,
    val eyeCenterNormalized: PointF = PointF(0.5f, 0.38f),
    val eyeSpanRatio: Float = 0.22f,
    val detected: Boolean = false
)

object PortraitFaceAnalyzer {
    private const val TAG = "PortraitFaceAnalyzer"

    suspend fun analyzePortrait(
        context: Context,
        customPortraitPath: String?,
        @DrawableRes fallbackDrawableRes: Int
    ): AvatarFaceAnchor = withContext(Dispatchers.IO) {
        try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inSampleSize = 2
            }
            val rawBitmap = if (!customPortraitPath.isNullOrBlank() && File(customPortraitPath).exists()) {
                BitmapFactory.decodeFile(customPortraitPath, opts)
            } else {
                BitmapFactory.decodeResource(context.resources, fallbackDrawableRes, opts)
            } ?: return@withContext AvatarFaceAnchor()

            // FaceDetector requires even width and RGB_565 config
            val evenWidth = (rawBitmap.width / 2) * 2
            val rgb565Bitmap = if (rawBitmap.config != Bitmap.Config.RGB_565 || rawBitmap.width != evenWidth) {
                Bitmap.createScaledBitmap(rawBitmap, evenWidth.coerceAtLeast(2), rawBitmap.height, true)
                    .copy(Bitmap.Config.RGB_565, false)
            } else {
                rawBitmap
            }

            val faces = arrayOfNulls<FaceDetector.Face>(1)
            val detector = FaceDetector(rgb565Bitmap.width, rgb565Bitmap.height, 1)
            val found = detector.findFaces(rgb565Bitmap, faces)
            if (found > 0 && faces[0] != null) {
                val face = faces[0]!!
                val mid = PointF()
                face.getMidPoint(mid)
                val eyeDist = face.eyesDistance()
                val w = rgb565Bitmap.width.toFloat().coerceAtLeast(1f)
                val h = rgb565Bitmap.height.toFloat().coerceAtLeast(1f)

                val eyeX = (mid.x / w).coerceIn(0.25f, 0.75f)
                val eyeY = (mid.y / h).coerceIn(0.15f, 0.65f)
                val eyeSpan = (eyeDist / w).coerceIn(0.08f, 0.45f)

                // Human anthropometric ratio: mouth sits ~1.05 * eyeDistance below eye midpoint
                val mouthY = ((mid.y + eyeDist * 1.05f) / h).coerceIn(0.25f, 0.85f)
                val mouthWidth = (eyeDist * 0.85f / w).coerceIn(0.08f, 0.35f)

                if (rgb565Bitmap !== rawBitmap) rgb565Bitmap.recycle()
                rawBitmap.recycle()

                return@withContext AvatarFaceAnchor(
                    mouthCenterNormalized = PointF(eyeX, mouthY),
                    mouthWidthRatio = mouthWidth,
                    eyeCenterNormalized = PointF(eyeX, eyeY),
                    eyeSpanRatio = eyeSpan,
                    detected = true
                )
            }

            if (rgb565Bitmap !== rawBitmap) rgb565Bitmap.recycle()
            rawBitmap.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "Face landmark analysis fallback used", e)
        }
        AvatarFaceAnchor()
    }
}
