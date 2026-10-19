package com.lavkushedits

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Bg = Color(0xFF090B16)
private val Panel = Color(0xFF15192B)
private val Purple = Color(0xFF8B5CF6)
private val Mint = Color(0xFF42E8C4)
private val Soft = Color(0xFFB7BDD2)

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Purple, secondary = Mint, background = Bg,
                    surface = Panel, onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                EditorHome()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun EditorHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(10f) }
    var status by remember { mutableStateOf("Choose a video to start your project.") }
    var busy by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf("1080p") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            trimStart = 0f
            trimEnd = 10f
            status = "Video loaded. Set trim points and export."
        }
    }

    fun exportVideo() {
        val uri = selectedUri
        if (uri == null) {
            status = "Please choose a video first."
            return
        }
        if (trimEnd <= trimStart) {
            status = "End time must be greater than start time."
            return
        }
        busy = true
        status = "Preparing export…"
        scope.launch {
            try {
                val outputFile = withContext(Dispatchers.IO) {
                    File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                        ?: context.filesDir, "lavkush-edits-${System.currentTimeMillis()}.mp4")
                }
                val clip = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs((trimStart * 1000).toLong())
                            .setEndPositionMs((trimEnd * 1000).toLong())
                            .build()
                    ).build()
                val edited = EditedMediaItem.Builder(clip).build()
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: androidx.media3.transformer.Composition,
                            exportResult: ExportResult
                        ) {
                            busy = false
                            status = "Export complete! Saved inside app Movies folder."
                            Toast.makeText(context, "Export complete", Toast.LENGTH_LONG).show()
                        }
                        override fun onError(
                            composition: androidx.media3.transformer.Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            busy = false
                            status = "Export failed: ${exportException.message ?: "unsupported video"}"
                        }
                    }).build()
                transformer.start(edited, outputFile.absolutePath)
            } catch (e: Exception) {
                busy = false
                status = "Could not export: ${e.message ?: "unknown error"}"
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).background(Purple, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) { Text("L", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("lavkush edits", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("CREATE  •  EDIT  •  SHARE", color = Mint, fontSize = 11.sp, letterSpacing = 1.5.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("YOUR CREATIVE STUDIO", color = Soft, fontSize = 12.sp, letterSpacing = 1.4.sp)
                Text("Make every frame count.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Import a clip, choose a section, and export your first edit.", color = Soft)
                Button(
                    onClick = { picker.launch(arrayOf("video/*")) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text(if (selectedUri == null) "＋  Choose video" else "✓  Change video") }
                if (selectedUri != null) {
                    Text("Video selected", color = Mint, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text("QUICK TOOLS", color = Soft, fontSize = 12.sp, letterSpacing = 1.6.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("✂", "Trim", "Select a clip")
            ToolCard("♫", "Audio", "Coming next")
            ToolCard("✦", "Effects", "Coming next")
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TRIM CLIP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Start: ${trimStart.toInt()} sec", color = Soft)
                Slider(value = trimStart, onValueChange = {
                    trimStart = it.coerceAtMost(trimEnd - 1f)
                }, valueRange = 0f..59f)
                Text("End: ${trimEnd.toInt()} sec", color = Soft)
                Slider(value = trimEnd, onValueChange = {
                    trimEnd = it.coerceAtLeast(trimStart + 1f)
                }, valueRange = 1f..60f)
                Text("Prototype trim range: 1–60 seconds. Actual source duration may be shorter.", color = Soft, fontSize = 12.sp)
                Text("EXPORT QUALITY", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("720p", "1080p").forEach { item ->
                        FilterChip(
                            selected = quality == item,
                            onClick = { quality = item },
                            label = { Text(item) }
                        )
                    }
                }
                Text("Quality selection is a UI preview only; export currently uses Media3 defaults.", color = Soft, fontSize = 12.sp)
                Button(
                    onClick = { exportVideo() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Export video  →")
                }
                Text(status, color = if (status.contains("failed", true) || status.contains("Could not", true)) Color(0xFFFF8585) else Mint)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COMING NEXT", fontWeight = FontWeight.Bold)
                FeatureLine("▰", "Timeline with multiple clips")
                FeatureLine("♫", "Add music and voice-over")
                FeatureLine("T", "Text, captions and stickers")
                FeatureLine("✦", "Filters, transitions and speed")
                FeatureLine("↗", "Save directly to Gallery and share")
            }
        }
        Text("Lavkush Edits • Early prototype • v1.0", color = Soft, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun RowScope.ToolCard(symbol: String, title: String, subtitle: String) {
    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(symbol, color = Mint, fontSize = 22.sp)
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Soft, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FeatureLine(symbol: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(symbol, color = Mint, fontWeight = FontWeight.Bold)
        Text(title, color = Soft)
    }
}
