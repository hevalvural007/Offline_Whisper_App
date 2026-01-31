package com.hevalvural.whisper_task //

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Permission needed for this app.Please give permission from settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            WhisperAppTheme {
                WhisperScreen()
            }
        }
    }
}

val DarkBg = Color(0xFF121212)
val CardBg = Color(0xFF1E1E1E)
val PrimaryAccent = Color(0xFFBB86FC)
val TextWhite = Color(0xFFEEEEEE)

@Composable
fun WhisperAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = DarkBg, surface = CardBg, primary = PrimaryAccent),
        content = content
    )
}

@Composable
fun WhisperScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    var modelStatus by remember { mutableStateOf("Loading...") }

    val whisperLib = remember { WhisperLib() }
    val recorder = remember { AudioRecorder() }

    val audioParts = remember { Collections.synchronizedList(ArrayList<ShortArray>()) }

    var contextPtr by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val modelPath = Utils.getModelPath(context, "ggml-tiny.bin")
            contextPtr = whisperLib.initContext(modelPath)
        }
        modelStatus = if (contextPtr != 0L) "Model is Ready (Tiny)" else "Model cannot be Loaded!!"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "pulse"
    )

    Scaffold(containerColor = DarkBg) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Whisper Offline", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(color = Color(0xFF2C2C2C), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                Row(modifier = Modifier.padding(12.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (contextPtr != 0L) Color.Green else Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(modelStatus, color = Color.Gray, fontSize = 12.sp)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBg).padding(20.dp)) {
                if (transcribedText.isEmpty() && !isTranscribing) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Mic, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                        Text("Tap to Talk...", color = Color.Gray.copy(alpha = 0.5f))
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                        Text(transcribedText, color = TextWhite, fontSize = 18.sp)
                    }
                }
                if (isTranscribing) {
                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        Text("Converting...", color = PrimaryAccent, fontSize = 12.sp)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(contentAlignment = Alignment.Center) {
                if (isRecording) {
                    Box(modifier = Modifier.size(100.dp).scale(pulseScale).background(Brush.radialGradient(listOf(PrimaryAccent.copy(alpha = 0.5f), Color.Transparent)), CircleShape))
                }
                FloatingActionButton(
                    onClick = {
                        if (contextPtr == 0L) return@FloatingActionButton

                        if (isRecording) {
                            isRecording = false
                            recorder.stopRecording()
                            modelStatus = "Processing..."
                            isTranscribing = true

                            scope.launch(Dispatchers.Default) {
                                try {
                                    val totalSize = audioParts.sumOf { it.size }
                                    val finalBuffer = FloatArray(totalSize)
                                    var offset = 0

                                    for (part in audioParts) {
                                        for (sample in part) {
                                            finalBuffer[offset++] = sample / 32768.0f
                                        }
                                    }

                                    if (finalBuffer.isNotEmpty()) {
                                        val result = whisperLib.fullTranscribe(contextPtr, finalBuffer)
                                        withContext(Dispatchers.Main) { transcribedText = result.trim() }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { transcribedText = "Error: ${e.message}" }
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isTranscribing = false
                                        modelStatus = "Model is Ready"
                                        audioParts.clear()
                                    }
                                }
                            }
                        } else {
                            audioParts.clear()
                            transcribedText = ""
                            isRecording = true
                            modelStatus = "Listening..."

                            recorder.startRecording { chunk ->
                                audioParts.add(chunk.clone())
                            }
                        }
                    },
                    containerColor = if (isRecording) Color.Red else PrimaryAccent,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape
                ) {
                    Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, "Record", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}