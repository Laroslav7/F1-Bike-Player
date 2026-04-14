package com.laroslav.f1bikeplayer

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Song(
    val title: String,
    val rawResId: Int? = null,
    val uri: Uri? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            F1BikePlayerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    PlayerScreen()
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val songs = remember {
        mutableStateListOf(
            Song("Max Verstappen", rawResId = R.raw.maxverstappen),
            Song("Fernando Alonso", rawResId = R.raw.fernandoalnoso),
            Song("Super Max", rawResId = R.raw.supermax),
            Song("Lando Norris", rawResId = R.raw.landonorris),
            Song("Lewis Hamilton", rawResId = R.raw.lewishamilton),
            Song("Charles Leclerc", rawResId = R.raw.charlesleclerc),
            Song("Schumacher", rawResId = R.raw.schumacher),
            Song("Oscar Piastri", rawResId = R.raw.oscarpiastri)
        )
    }

    var currentSongIndex by rememberSaveable { mutableIntStateOf(-1) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopAndRelease() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun playSong(index: Int) {
        if (index !in songs.indices) return
        val song = songs[index]
        stopAndRelease()

        mediaPlayer = when {
            song.rawResId != null -> MediaPlayer.create(context, song.rawResId)
            song.uri != null -> MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, song.uri)
                prepare()
            }
            else -> null
        }

        mediaPlayer?.setOnCompletionListener {
            val nextIndex = (currentSongIndex + 1).mod(songs.size)
            playSong(nextIndex)
        }

        mediaPlayer?.start()
        currentSongIndex = index
        isPaused = false
    }

    fun pauseOrResume() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            isPaused = true
        } else {
            player.start()
            isPaused = false
        }
    }

    fun playNext() {
        if (songs.isEmpty()) return
        val nextIndex = if (currentSongIndex == -1) 0 else (currentSongIndex + 1).mod(songs.size)
        playSong(nextIndex)
    }

    val customSongPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        songs.add(Song(title = "Custom Track ${songs.size - 7}", uri = uri))
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "F1 Bike Player",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Race Mode Audio",
            color = Color(0xFFFF2B2B),
            style = MaterialTheme.typography.bodyMedium
        )
        Divider(color = Color.White, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        Divider(color = Color(0xFFFF2B2B), thickness = 2.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (currentSongIndex in songs.indices) "Now: ${songs[currentSongIndex].title}" else "Now: Silence",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = { customSongPicker.launch(arrayOf("audio/*")) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2B2B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(width = 44.dp, height = 36.dp)
            ) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(songs) { index, song ->
                SongRow(
                    title = song.title,
                    active = index == currentSongIndex,
                    onClick = { playSong(index) }
                )
            }
        }

        BottomControlPanel(
            isPaused = isPaused,
            onPauseResume = { pauseOrResume() },
            onNext = { playNext() },
            onVideo = { videoLauncher.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE)) },
            onBluetooth = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        )
    }
}

@Composable
private fun SongRow(title: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) Color(0x1AFFFFFF) else Color.Black, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = if (active) Color(0xFFFF2B2B) else Color.White,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 17.sp
        )
        Divider(color = Color.White.copy(alpha = 0.3f), thickness = 0.8.dp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun BottomControlPanel(
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onNext: () -> Unit,
    onVideo: () -> Unit,
    onBluetooth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0C), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Divider(color = Color.White, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(label = if (isPaused) "Play" else "Pause", onClick = onPauseResume, modifier = Modifier.weight(1f))
            ControlButton(label = "Next", onClick = onNext, modifier = Modifier.weight(1f))
            ControlButton(label = "Video", onClick = onVideo, modifier = Modifier.weight(1f))
            ControlButton(label = "BT", onClick = onBluetooth, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label)
    }
}

@Composable
private fun F1BikePlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
