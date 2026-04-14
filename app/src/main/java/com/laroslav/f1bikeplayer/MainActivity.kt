package com.laroslav.f1bikeplayer

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class BuiltInSong(
    val title: String,
    @RawRes val rawResId: Int
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
    val scope = rememberCoroutineScope()

    var currentSongTitle by rememberSaveable { mutableStateOf("Ничего не играет") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val songs = remember {
        listOf(
            BuiltInSong("Max Verstappen", R.raw.maxverstappen),
            BuiltInSong("Fernando Alonso", R.raw.fernandoalnoso),
            BuiltInSong("Super Max", R.raw.supermax),
            BuiltInSong("Lando Norris", R.raw.landonorris),
            BuiltInSong("Lewis Hamilton", R.raw.lewishamilton),
            BuiltInSong("Charles Leclerc", R.raw.charlesleclerc),
            BuiltInSong("Schumacher", R.raw.schumacher),
            BuiltInSong("Oscar Piastri", R.raw.oscarpiastri)
        )
    }

    fun stopCurrent() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun playBuiltIn(song: BuiltInSong) {
        stopCurrent()
        mediaPlayer = MediaPlayer.create(context, song.rawResId)?.apply {
            setOnCompletionListener {
                currentSongTitle = "Ничего не играет"
                stopCurrent()
            }
            start()
        }
        currentSongTitle = song.title
    }

    fun playFromUri(uri: Uri) {
        stopCurrent()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(context, uri)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                currentSongTitle = "Ничего не играет"
                stopCurrent()
            }
            prepareAsync()
        }
        currentSongTitle = "Пользовательский трек"
    }

    val customSongPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        scope.launch { playFromUri(uri) }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    DisposableEffect(Unit) {
        onDispose { stopCurrent() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "F1 Bike Player",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Divider(color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)

        Text(
            text = "Сейчас: $currentSongTitle",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { customSongPicker.launch(arrayOf("audio/*")) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("Добавить свою песню")
            }

            Button(
                onClick = {
                    stopCurrent()
                    currentSongTitle = "Ничего не играет"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("Стоп")
            }
        }

        Divider(color = Color.White.copy(alpha = 0.55f), thickness = 0.8.dp)

        Text(
            text = "Треки из приложения",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(songs) { song ->
                SongRow(song = song, onClick = { playBuiltIn(song) })
            }
        }

        Divider(color = Color.White.copy(alpha = 0.75f), thickness = 1.dp)

        Button(
            onClick = {
                val bluetoothIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                context.startActivity(bluetoothIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("Подключить Bluetooth")
        }

        Button(
            onClick = {
                val captureIntent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                videoLauncher.launch(captureIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("Начать съемку видео")
        }

        Text(
            text = "Музыка продолжает играть во время открытия камеры.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SongRow(song: BuiltInSong, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Column {
            Text(text = song.title, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = Color.White.copy(alpha = 0.45f), thickness = 0.6.dp)
        }
    }
}

@Composable
private fun F1BikePlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
