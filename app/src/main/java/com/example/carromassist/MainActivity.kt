package com.example.carromassist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.carromassist.model.*
import com.example.carromassist.capture.ProjectionSessionService
import com.example.carromassist.physics.ShotPlanner
import com.example.carromassist.vision.SyntheticBoard
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val captureConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Projection consent is single-session only; never persist or reuse the result.
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ProjectionSessionService::class.java).apply {
                putExtra(ProjectionSessionService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ProjectionSessionService.EXTRA_RESULT_DATA, result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarromApp(
            onCapture = {
                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                captureConsent.launch(mgr.createScreenCaptureIntent())
            },
            onOverlay = {
                if (!Settings.canDrawOverlays(this)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        ) }
    }
}

@Composable
private fun CarromApp(onCapture: () -> Unit, onOverlay: () -> Unit) {
    var demo by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(0) }
    val board = remember(demo) { if (demo) SyntheticBoard.create() else null }
    val shots = remember(board) { board?.let { ShotPlanner().recommend(it) }.orEmpty() }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF79E0C3), background = Color(0xFF101613), surface = Color(0xFF1A2420))) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("JARVIS · SIMPLE SHOTS", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF79E0C3))
                Text("Direct pocket shots · no bank shots", color = Color.LightGray)
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2420))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Simple-shot practice board", style = MaterialTheme.typography.titleMedium)
                        Text("Test straight pot-shot recommendations on a sample board. Live game detection is not connected yet.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { demo = true }) { Text("Demo mode") }
                            OutlinedButton(onClick = onCapture) { Text("Request capture") }
                        }
                    }
                }
                if (board != null) {
                    BoardPreview(board, shots.getOrNull(selected))
                    Text("Direct-shot candidates: ${shots.size} · Demo board", color = Color.LightGray)
                    if (shots.isNotEmpty()) {
                        Text("Recommended: ${shots[selected.coerceIn(0, shots.lastIndex)].targetId}")
                        Text(shots[selected.coerceIn(0, shots.lastIndex)].summary)
                        Text("Geometry score: ${(shots[selected.coerceIn(0, shots.lastIndex)].confidence * 100).toInt()}% · not a success probability")
                        val currentShot = shots[selected.coerceIn(0, shots.lastIndex)]
                        Text("Aim angle: ${"%.1f".format(Math.toDegrees(currentShot.angleRadians))}°")
                        val strikerTravel = board.striker.center.distance(currentShot.contactPoint)
                        val powerEstimate = (35.0 + 55.0 * (strikerTravel / (board.width * 0.55)).coerceIn(0.0, 1.0)).toInt()
                        Text("Starting power estimate: $powerEstimate% · tune in-game")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selected = (selected - 1).coerceAtLeast(0) }, enabled = selected > 0) { Text("Previous") }
                            Button(onClick = { selected = (selected + 1).coerceAtMost(shots.lastIndex) }, enabled = selected < shots.lastIndex) { Text("Next shot") }
                        }
                    } else Text("No reliable shot found.")
                }
                OutlinedButton(onClick = onOverlay, modifier = Modifier.fillMaxWidth()) { Text("Overlay permission settings") }
                Text("Next step: connect screen capture to live board/puck detection. For now, recommendations use the sample board only; no shots are sent to Carrom Pool.", color = Color(0xFFFFD18A), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BoardPreview(board: BoardState, shot: ShotRecommendation?) {
    Canvas(Modifier.fillMaxWidth().height(280.dp).background(Color(0xFFB9854E), RoundedCornerShape(12.dp)).padding(12.dp)) {
        val scale = minOf(size.width / board.width.toFloat(), size.height / board.height.toFloat())
        val ox = (size.width - board.width.toFloat() * scale) / 2f
        val oy = (size.height - board.height.toFloat() * scale) / 2f
        fun p(v: Vec2) = Offset(ox + v.x.toFloat() * scale, oy + v.y.toFloat() * scale)
        drawRect(Color(0xFFE9C99A), topLeft = Offset(ox, oy), size = androidx.compose.ui.geometry.Size(board.width.toFloat()*scale, board.height.toFloat()*scale))
        board.pockets.forEach { drawCircle(Color(0xFF25221D), it.radius.toFloat()*scale, p(it.center)) }
        board.coins.forEach { coin ->
            drawCircle(if (coin.id.startsWith("white")) Color.White else if (coin.id.startsWith("red")) Color.Red else Color(0xFF262626), coin.radius.toFloat()*scale, p(coin.center))
        }
        drawCircle(Color(0xFF4C9BE8), board.striker.radius.toFloat()*scale, p(board.striker.center))
        shot?.let {
            drawLine(Color(0xFF13A88A), p(board.striker.center), p(it.contactPoint), strokeWidth = 4f, cap = StrokeCap.Round)
            drawCircle(Color(0xFF13A88A), 7f, p(it.contactPoint))
            it.pocket?.let { pocket -> drawLine(Color(0xFFEDC85C), p(it.path.last()), p(pocket.center), strokeWidth = 3f, cap = StrokeCap.Round) }
        }
        drawRect(Color(0xFF4C3020), topLeft = Offset(ox, oy), size = androidx.compose.ui.geometry.Size(board.width.toFloat()*scale, board.height.toFloat()*scale), style = Stroke(3f))
    }
}
