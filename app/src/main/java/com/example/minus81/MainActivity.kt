package com.example.minus81

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.minus81.ui.theme.Minus81Theme
import java.io.File
import java.io.FileOutputStream

// --- Глобальный цвет акцента ---
val LocalAccentColor = compositionLocalOf { Color(0xFF8140D6) }

// --- Менеджер настроек ---
class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("minus81_prefs", Context.MODE_PRIVATE)

    var pin: String?
        get() = prefs.getString("user_pin", null)
        set(value) = prefs.edit().putString("user_pin", value).apply()

    var isPrivateTabEnabled: Boolean
        get() = prefs.getBoolean("private_tab_enabled", false)
        set(value) = prefs.edit().putBoolean("private_tab_enabled", value).apply()

    var lastExitTime: Long
        get() = prefs.getLong("last_exit_time", 0)
        set(value) = prefs.edit().putLong("last_exit_time", value).apply()

    var accentColor: Int
        get() = prefs.getInt("accent_color", 0xFF8140D6.toInt())
        set(value) = prefs.edit().putInt("accent_color", value).apply()

    // Сохранение кастомных ссылок
    fun getCustomLinks(type: String): List<LinkItem> {
        val raw = prefs.getString("custom_links_$type", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(";;;").mapNotNull {
            val parts = it.split("|~|")
            if (parts.size == 2) LinkItem(parts[0], parts[1], isCustom = true) else null
        }
    }

    fun saveCustomLinks(type: String, links: List<LinkItem>) {
        val raw = links.filter { it.isCustom }.joinToString(";;;") { "${it.name}|~|${it.url}" }
        prefs.edit().putString("custom_links_$type", raw).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Minus81Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    AppEntryPoint()
                }
            }
        }
    }
}

@Composable
fun AppEntryPoint() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLocked by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf(prefs.pin) }
    var currentAccent by remember { mutableIntStateOf(prefs.accentColor) }

    // Автоблокировка
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                prefs.lastExitTime = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_START) {
                val timeDiff = System.currentTimeMillis() - prefs.lastExitTime
                if (currentPin != null && timeDiff > 60000) {
                    isLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Применяем выбранный цвет ко всему приложению
    CompositionLocalProvider(LocalAccentColor provides Color(currentAccent)) {
        if (currentPin == null) {
            PinLockScreen(
                title = "Придумайте ПИН",
                isSetup = true,
                onSuccess = { newPin ->
                    prefs.pin = newPin
                    currentPin = newPin
                    isLocked = false
                }
            )
        } else {
            if (isLocked) {
                PinLockScreen(
                    title = "Введите ПИН",
                    correctPin = currentPin,
                    onSuccess = { isLocked = false }
                )
            } else if (showSettings) {
                SettingsScreen(
                    prefs = prefs,
                    currentAccent = currentAccent,
                    onAccentChanged = { newColor ->
                        currentAccent = newColor
                        prefs.accentColor = newColor
                    },
                    onBack = { showSettings = false }
                )
            } else {
                MainScreen(
                    prefs = prefs,
                    onOpenSettings = { showSettings = true }
                )
            }
        }
    }
}

// --- Экран ПИН-кода ---
@Composable
fun PinLockScreen(title: String, isSetup: Boolean = false, correctPin: String? = null, onSuccess: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val accent = LocalAccentColor.current

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(20.dp)) {
            repeat(4) { index ->
                val active = index < input.length
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (active) accent else Color.DarkGray))
            }
        }

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = Color.Red, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.width(300.dp)
        ) {
            items(keys) { key ->
                if (key.isEmpty()) Spacer(Modifier.size(80.dp))
                else if (key == "del") {
                    PinButton(text = "", icon = Icons.Outlined.Backspace) {
                        if (input.isNotEmpty()) input = input.dropLast(1)
                        error = ""
                    }
                } else {
                    PinButton(text = key, icon = null) {
                        if (input.length < 4) {
                            input += key
                            if (input.length == 4) {
                                if (isSetup) onSuccess(input)
                                else if (input == correctPin) onSuccess(input)
                                else {
                                    input = ""
                                    error = "Неверный ПИН"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White)
        else Text(text, fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// --- Главный Экран ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(prefs: PrefsManager, onOpenSettings: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isPrivateEnabled by remember { mutableStateOf(prefs.isPrivateTabEnabled) }
    val accent = LocalAccentColor.current

    LaunchedEffect(prefs.isPrivateTabEnabled) {
        isPrivateEnabled = prefs.isPrivateTabEnabled
        if (selectedTab == 2 && !isPrivateEnabled) selectedTab = 0
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("-81", fontWeight = FontWeight.Black, color = Color.White) },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0014)) {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Порно") },
                    colors = navColors(accent)
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Male, null) }, label = { Text("Гей") },
                    colors = navColors(accent)
                )
                if (isPrivateEnabled) {
                    NavigationBarItem(
                        selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Lock, null) }, label = { Text("Мое") },
                        colors = navColors(accent)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize().background(Color.Black)) {
            when (selectedTab) {
                0 -> LinksTab(type = "STRAIGHT", prefs = prefs)
                1 -> LinksTab(type = "GAY", prefs = prefs)
                2 -> PrivateContentTab()
            }
        }
    }
}

@Composable
fun navColors(accent: Color) = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.White,
    selectedTextColor = accent,
    indicatorColor = accent.copy(alpha = 0.6f),
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray
)

// --- Настройки ---
@Composable
fun SettingsScreen(prefs: PrefsManager, currentAccent: Int, onAccentChanged: (Int) -> Unit, onBack: () -> Unit) {
    var isPrivateEnabled by remember { mutableStateOf(prefs.isPrivateTabEnabled) }
    var changePinMode by remember { mutableStateOf(false) }
    val accent = LocalAccentColor.current

    // Палитра цветов
    val colors = listOf(
        Color(0xFF8140D6), // Фиолетовый (по умолчанию)
        Color(0xFFE53935), // Красный
        Color(0xFF1E88E5), // Синий
        Color(0xFF43A047), // Зеленый
        Color(0xFFE91E63), // Розовый
        Color(0xFFFFB300)  // Оранжевый
    )

    if (changePinMode) {
        PinLockScreen(
            title = "Новый ПИН", isSetup = true,
            onSuccess = { newPin -> prefs.pin = newPin; changePinMode = false }
        )
        BackHandler { changePinMode = false }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                Text("Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(32.dp))

            // Цвет акцента
            Text("Цвет акцента", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(colors) { color ->
                    val isSelected = currentAccent == color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) Color.White else Color.Transparent, shape = CircleShape)
                            .clickable { onAccentChanged(color.toArgb()) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider(color = Color.DarkGray)

            // Скрытый контент
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Вкладка «Мое»", color = Color.White, fontSize = 18.sp)
                    Text("Скрытый контент", color = Color.Gray, fontSize = 14.sp)
                }
                Switch(
                    checked = isPrivateEnabled,
                    onCheckedChange = { isPrivateEnabled = it; prefs.isPrivateTabEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.5f))
                )
            }
            Divider(color = Color.DarkGray)

            // Смена ПИН
            Row(
                modifier = Modifier.fillMaxWidth().clickable { changePinMode = true }.padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Сменить ПИН-код", color = Color.White, fontSize = 18.sp)
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }
}

// --- Вкладки со ссылками ---
data class LinkItem(val name: String, val url: String, val isMain: Boolean = false, val isCustom: Boolean = false)

@Composable
fun LinksTab(type: String, prefs: PrefsManager) {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    var customLinks by remember { mutableStateOf(prefs.getCustomLinks(type)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<LinkItem?>(null) } // Если не null - режим редактирования

    // Базовые ссылки (Обновлены с подгузниками)
    val defaultLinks = if (type == "GAY") {
        listOf(
            LinkItem("ГЛАВНАЯ", "https://rt.pornhub.com/gayporn", true),
            LinkItem("Подгузники", "https://rt.pornhub.com/gay/video/search?search=%D0%BF%D0%BE%D0%B4%D0%B3%D1%83%D0%B7%D0%BD%D0%B8%D0%BA%D0%B8"),
            LinkItem("Фембои", "https://rt.pornhub.com/gay/video/search?search=femboy"),
            LinkItem("Секс машина", "https://rt.pornhub.com/gay/video/search?search=sex+machine"),
            LinkItem("БДСМ", "https://rt.pornhub.com/gay/video/search?search=bdsm"),
            LinkItem("Дилдо", "https://rt.pornhub.com/gay/video/search?search=dildo")
        )
    } else {
        listOf(
            LinkItem("ГЛАВНАЯ", "https://rt.pornhub.com/", true),
            LinkItem("Подгузники", "https://rt.pornhub.com/video/search?search=girl+pampers+diaper"),
            LinkItem("Секс машина", "https://rt.pornhub.com/video/search?search=%D1%81%D0%B5%D0%BA%D1%81+%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D0%B0"),
            LinkItem("Попка", "https://rt.pornhub.com/video/search?search=%D0%BF%D0%BE%D0%BF%D0%BA%D0%B0"),
            LinkItem("Киска", "https://rt.pornhub.com/video/search?search=%D0%BA%D0%B8%D1%81%D0%BA%D0%B0"),
            LinkItem("Трусики", "https://rt.pornhub.com/video/search?search=%D1%82%D1%80%D1%83%D1%81%D0%B8%D0%BA%D0%B8")
        )
    }

    val allLinks = defaultLinks + customLinks

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allLinks) { link ->
                FancyLinkCard(
                    link = link,
                    onClick = { openUrl(context, link.url) },
                    onEdit = if (link.isCustom) { { editItem = link } } else null,
                    onDelete = if (link.isCustom) {
                        {
                            val newList = customLinks.filter { it != link }
                            customLinks = newList
                            prefs.saveCustomLinks(type, newList)
                        }
                    } else null
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = accent
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
        }
    }

    // Диалог Добавления/Редактирования
    if (showAddDialog || editItem != null) {
        val isEditing = editItem != null
        AddEditLinkDialog(
            initialName = editItem?.name ?: "",
            initialUrl = editItem?.url ?: "",
            title = if (isEditing) "Редактировать сайт" else "Добавить сайт",
            onDismiss = { showAddDialog = false; editItem = null },
            onSave = { name, url ->
                if (isEditing) {
                    val newList = customLinks.map { if (it == editItem) LinkItem(name, url, isCustom = true) else it }
                    customLinks = newList
                    prefs.saveCustomLinks(type, newList)
                } else {
                    val newItem = LinkItem(name, url, isCustom = true)
                    val newList = customLinks + newItem
                    customLinks = newList
                    prefs.saveCustomLinks(type, newList)
                }
                showAddDialog = false
                editItem = null
            }
        )
    }
}

@Composable
fun AddEditLinkDialog(initialName: String, initialUrl: String, title: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    val accent = LocalAccentColor.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text(title, color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = accent, unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Ссылка (https://...)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = accent, unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && url.isNotBlank()) onSave(name, url) },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Color.Gray) }
        }
    )
}

@Composable
fun FancyLinkCard(link: LinkItem, onClick: () -> Unit, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    val height = if (link.isMain) 140.dp else 70.dp
    val fontSize = if (link.isMain) 28.sp else 20.sp

    Card(
        modifier = Modifier.fillMaxWidth().height(height).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.linearGradient(
                    colors = if (link.isMain) listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                    else listOf(Color(0xFF230B38), Color(0xFF381460))
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(link.name.uppercase(), fontSize = fontSize, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)

            // Иконки редактирования и удаления для кастомных ссылок
            if (link.isCustom) {
                Row(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = Color.White) }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
                    }
                }
            }
        }
    }
}

// --- Вкладка "Твой контент" ---
@Composable
fun PrivateContentTab() {
    val context = LocalContext.current
    val accent = LocalAccentColor.current
    var mediaFiles by remember { mutableStateOf(listOf<File>()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    fun refreshFiles() {
        val dir = File(context.filesDir, "private_media")
        if (!dir.exists()) dir.mkdirs()
        mediaFiles = dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    LaunchedEffect(Unit) { refreshFiles() }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val dir = File(context.filesDir, "private_media")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "img_${System.currentTimeMillis()}_${(0..1000).random()}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
            }
            refreshFiles()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (mediaFiles.isEmpty()) {
            Text("Пусто", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(mediaFiles) { file ->
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { selectedFile = file },
                        contentScale = ContentScale.Crop
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = accent
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
        }
    }

    if (selectedFile != null) {
        Dialog(onDismissRequest = { selectedFile = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = selectedFile, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                )
                IconButton(onClick = { selectedFile = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = CustomTabsIntent.Builder().setToolbarColor(0xFF000000.toInt()).build()
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }
}