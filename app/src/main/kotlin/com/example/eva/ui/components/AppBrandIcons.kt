package com.example.eva.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AppBrandIcon(serviceId: String, modifier: Modifier = Modifier.size(34.dp)) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (serviceId) {
            "instagram" -> InstagramIcon()
            "whatsapp" -> WhatsAppIcon()
            "youtube" -> YouTubeIcon()
            "ytmusic" -> YouTubeMusicIcon()
            "spotify" -> SpotifyIcon()
            "linkedin" -> LinkedInIcon()
            "facebook" -> FacebookIcon()
            "twitter" -> XTwitterIcon()
            "reddit" -> RedditIcon()
            else -> DefaultAppIcon()
        }
    }
}

@Composable
private fun InstagramIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Outer rounded rect
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
            style = Stroke(width = w * 0.08f)
        )

        // Center lens circle
        drawCircle(
            color = Color.White,
            radius = w * 0.19f,
            center = center,
            style = Stroke(width = w * 0.08f)
        )

        // Top right flash dot
        drawCircle(
            color = Color.White,
            radius = w * 0.045f,
            center = Offset(w * 0.70f, h * 0.30f),
            style = Fill
        )
    }
}

@Composable
private fun WhatsAppIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Speech bubble path
        val bubblePath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(w * 0.1f, h * 0.1f, w * 0.9f, h * 0.9f))
            moveTo(w * 0.22f, h * 0.75f)
            lineTo(w * 0.12f, h * 0.92f)
            lineTo(w * 0.32f, h * 0.85f)
            close()
        }
        drawPath(bubblePath, Color.White, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))

        // Phone handset curve
        val phonePath = Path().apply {
            moveTo(w * 0.36f, h * 0.40f)
            cubicTo(w * 0.38f, h * 0.55f, w * 0.45f, h * 0.62f, w * 0.60f, h * 0.64f)
            lineTo(w * 0.66f, h * 0.58f)
            lineTo(w * 0.58f, h * 0.50f)
            lineTo(w * 0.52f, h * 0.54f)
            cubicTo(w * 0.48f, h * 0.52f, w * 0.48f, h * 0.52f, w * 0.46f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.42f)
            close()
        }
        drawPath(phonePath, Color.White, style = Fill)
    }
}

@Composable
private fun YouTubeIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Rounded TV rectangle
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.08f, h * 0.20f),
            size = Size(w * 0.84f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.16f, h * 0.16f),
            style = Fill
        )

        // Center Play Triangle
        val triangle = Path().apply {
            moveTo(w * 0.42f, h * 0.36f)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.64f)
            close()
        }
        drawPath(triangle, Color(0xFFCC0000), style = Fill)
    }
}

@Composable
private fun YouTubeMusicIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Outer circle
        drawCircle(
            color = Color.White,
            radius = w * 0.42f,
            center = center,
            style = Stroke(width = w * 0.07f)
        )

        // Inner circle
        drawCircle(
            color = Color.White,
            radius = w * 0.28f,
            center = center,
            style = Stroke(width = w * 0.06f)
        )

        // Play triangle
        val triangle = Path().apply {
            moveTo(w * 0.43f, h * 0.40f)
            lineTo(w * 0.60f, h * 0.50f)
            lineTo(w * 0.43f, h * 0.60f)
            close()
        }
        drawPath(triangle, Color.White, style = Fill)
    }
}

@Composable
private fun SpotifyIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Solid circular background
        drawCircle(color = Color.White, radius = w * 0.44f, center = center)

        // 3 Sound frequency arcs (drawn in dark Spotify green/black)
        val strokeColor = Color(0xFF191414)
        val arcStroke1 = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
        val arcStroke2 = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
        val arcStroke3 = Stroke(width = w * 0.065f, cap = StrokeCap.Round)

        drawArc(
            color = strokeColor,
            startAngle = 200f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.22f),
            size = Size(w * 0.56f, h * 0.56f),
            style = arcStroke1
        )
        drawArc(
            color = strokeColor,
            startAngle = 202f,
            sweepAngle = 96f,
            useCenter = false,
            topLeft = Offset(w * 0.26f, h * 0.34f),
            size = Size(w * 0.48f, h * 0.48f),
            style = arcStroke2
        )
        drawArc(
            color = strokeColor,
            startAngle = 204f,
            sweepAngle = 92f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.46f),
            size = Size(w * 0.40f, h * 0.40f),
            style = arcStroke3
        )
    }
}

@Composable
private fun LinkedInIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Rounded badge
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.10f, h * 0.10f),
            size = Size(w * 0.80f, h * 0.80f),
            cornerRadius = CornerRadius(w * 0.18f, h * 0.18f),
            style = Fill
        )

        val inColor = Color(0xFF0077B5)

        // Dot of 'i'
        drawCircle(
            color = inColor,
            radius = w * 0.05f,
            center = Offset(w * 0.32f, h * 0.34f)
        )

        // Body of 'i'
        drawRect(
            color = inColor,
            topLeft = Offset(w * 0.27f, h * 0.44f),
            size = Size(w * 0.10f, h * 0.32f)
        )

        // Left stem of 'n'
        drawRect(
            color = inColor,
            topLeft = Offset(w * 0.45f, h * 0.44f),
            size = Size(w * 0.10f, h * 0.32f)
        )

        // Arc and right stem of 'n'
        val nPath = Path().apply {
            moveTo(w * 0.45f, h * 0.54f)
            cubicTo(w * 0.47f, h * 0.42f, w * 0.65f, h * 0.42f, w * 0.66f, h * 0.54f)
            lineTo(w * 0.66f, h * 0.76f)
            lineTo(w * 0.56f, h * 0.76f)
            lineTo(w * 0.56f, h * 0.56f)
            cubicTo(w * 0.56f, h * 0.50f, w * 0.48f, h * 0.50f, w * 0.48f, h * 0.56f)
            close()
        }
        drawPath(nPath, inColor, style = Fill)
    }
}

@Composable
private fun FacebookIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Circle background
        drawCircle(color = Color.White, radius = w * 0.44f, center = center)

        // 'f' character
        val fbBlue = Color(0xFF1877F2)
        val fPath = Path().apply {
            moveTo(w * 0.52f, h * 0.88f)
            lineTo(w * 0.52f, h * 0.56f)
            lineTo(w * 0.63f, h * 0.56f)
            lineTo(w * 0.65f, h * 0.44f)
            lineTo(w * 0.52f, h * 0.44f)
            lineTo(w * 0.52f, h * 0.36f)
            cubicTo(w * 0.52f, h * 0.30f, w * 0.56f, h * 0.26f, w * 0.64f, h * 0.26f)
            lineTo(w * 0.68f, h * 0.26f)
            lineTo(w * 0.68f, h * 0.16f)
            cubicTo(w * 0.62f, h * 0.15f, w * 0.54f, h * 0.15f, w * 0.48f, h * 0.20f)
            cubicTo(w * 0.42f, h * 0.25f, w * 0.40f, h * 0.33f, w * 0.40f, h * 0.44f)
            lineTo(w * 0.32f, h * 0.44f)
            lineTo(w * 0.32f, h * 0.56f)
            lineTo(w * 0.40f, h * 0.56f)
            lineTo(w * 0.40f, h * 0.88f)
            close()
        }
        drawPath(fPath, fbBlue, style = Fill)
    }
}

@Composable
private fun XTwitterIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Modern 𝕏 stylized geometry
        val xPath1 = Path().apply {
            moveTo(w * 0.20f, h * 0.20f)
            lineTo(w * 0.48f, h * 0.56f)
            lineTo(w * 0.20f, h * 0.84f)
            lineTo(w * 0.30f, h * 0.84f)
            lineTo(w * 0.53f, h * 0.60f)
            lineTo(w * 0.72f, h * 0.84f)
            lineTo(w * 0.84f, h * 0.84f)
            lineTo(w * 0.57f, h * 0.50f)
            lineTo(w * 0.82f, h * 0.20f)
            lineTo(w * 0.72f, h * 0.20f)
            lineTo(w * 0.51f, h * 0.45f)
            lineTo(w * 0.32f, h * 0.20f)
            close()
        }
        drawPath(xPath1, Color.White, style = Fill)
    }
}

@Composable
private fun RedditIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height

        // Solid circular face
        drawCircle(color = Color.White, radius = w * 0.36f, center = Offset(center.x, center.y + 2f))

        val orangeRed = Color(0xFFFF4500)

        // Ears
        drawCircle(color = Color.White, radius = w * 0.10f, center = Offset(w * 0.20f, h * 0.42f))
        drawCircle(color = Color.White, radius = w * 0.10f, center = Offset(w * 0.80f, h * 0.42f))

        // Eyes
        drawCircle(color = orangeRed, radius = w * 0.055f, center = Offset(w * 0.38f, h * 0.52f))
        drawCircle(color = orangeRed, radius = w * 0.055f, center = Offset(w * 0.62f, h * 0.52f))

        // Smile
        drawArc(
            color = orangeRed,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.56f),
            size = Size(w * 0.24f, h * 0.14f),
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
        )

        // Antenna
        drawLine(
            color = Color.White,
            start = Offset(w * 0.50f, h * 0.22f),
            end = Offset(w * 0.66f, h * 0.12f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color.White, radius = w * 0.06f, center = Offset(w * 0.66f, h * 0.12f))
    }
}

@Composable
private fun DefaultAppIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        drawCircle(color = Color.White, radius = size.width * 0.40f, center = center)
    }
}
