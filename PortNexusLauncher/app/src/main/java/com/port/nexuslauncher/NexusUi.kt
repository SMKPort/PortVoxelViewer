package com.port.nexuslauncher

import android.content.SharedPreferences
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Ink = Color(0xFF09070D)
private val Muted = Color(0xFFC4B9C3)
private val Gold = Color(0xFFE8BB75)
private val PtBr = Locale("pt", "BR")

enum class NexusTheme(val title: String, val subtitle: String, val primary: Color, val secondary: Color) {
    PRIME("Nexus Prime", "Vermelho • dourado", Color(0xFFEB3D55), Color(0xFFE8BB75)),
    INFERNAL("Infernal Core", "Carmesim • brasa", Color(0xFFFF563A), Color(0xFFFFAC57)),
    CELESTIAL("Celestial Crown", "Dourado • celestial", Color(0xFFE8BB75), Color(0xFFFFE8AD)),
    INDUSTRIAL("Arcane Engine", "Vermelho • aço", Color(0xFFEE445A), Color(0xFF9CC4D3)),
}

private enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Início", Icons.Rounded.Home),
    APPS("Apps", Icons.Rounded.Apps),
    NEXUS("Nexus", Icons.Rounded.AutoAwesome),
    VISUAL("Visual", Icons.Rounded.Palette),
}

@Composable
fun PortNexusUi(
    apps: List<LauncherApp>,
    prefs: SharedPreferences,
    batteryPercent: Int,
    onLaunchApp: (LauncherApp) -> Unit,
    onSystemAction: (NexusAction) -> Unit,
    onReloadApps: () -> Unit,
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var theme by remember {
        mutableStateOf(NexusTheme.entries.firstOrNull {
            it.name == prefs.getString("theme", NexusTheme.PRIME.name)
        } ?: NexusTheme.PRIME)
    }
    var favoritePackages by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
    val toggleFavorite: (LauncherApp) -> Unit = { app ->
        val updated = favoritePackages.toMutableSet()
        if (!updated.add(app.packageName)) updated.remove(app.packageName)
        favoritePackages = updated.toSet()
        prefs.edit().putStringSet("favorites", favoritePackages).apply()
    }
    val setTheme: (NexusTheme) -> Unit = { selected ->
        theme = selected
        prefs.edit().putString("theme", selected.name).apply()
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = theme.primary,
        secondary = theme.secondary,
        background = Ink,
        surface = Color(0xFF17111B),
        onBackground = Color.White,
        onSurface = Color.White,
    )) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF09070D), Color(0xFF260B17), Ink))
            )
        ) {
            NexusBackground(theme.primary)
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0D0A11)) {
                        Screen.entries.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Ink,
                                    selectedTextColor = theme.secondary,
                                    indicatorColor = theme.secondary,
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted,
                                ),
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (currentScreen) {
                        Screen.HOME -> HomeScreen(
                            apps = apps,
                            favorites = favoritePackages,
                            theme = theme,
                            batteryPercent = batteryPercent,
                            onLaunch = onLaunchApp,
                            onOpenApps = { currentScreen = Screen.APPS },
                            onOpenNexus = { currentScreen = Screen.NEXUS },
                        )
                        Screen.APPS -> AppDrawerScreen(
                            apps = apps,
                            favorites = favoritePackages,
                            query = searchText,
                            onQueryChange = { searchText = it },
                            onToggleFavorite = toggleFavorite,
                            onLaunch = onLaunchApp,
                            onReload = onReloadApps,
                            theme = theme,
                        )
                        Screen.NEXUS -> NexusDashboard(
                            appCount = apps.size,
                            favoritesCount = favoritePackages.size,
                            batteryPercent = batteryPercent,
                            theme = theme,
                            onOpenApps = { currentScreen = Screen.APPS },
                            onSystemAction = onSystemAction,
                        )
                        Screen.VISUAL -> VisualSettings(
                            theme = theme,
                            onSelectTheme = setTheme,
                            onSystemAction = onSystemAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusBackground(color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val spacing = 54.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(color.copy(alpha = 0.035f), Offset(x, 0f), Offset(x, size.height), 1f)
            x += spacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color.copy(alpha = 0.035f), Offset(0f, y), Offset(size.width, y), 1f)
            y += spacing
        }
        drawCircle(color.copy(alpha = 0.045f), radius = size.width * 0.85f, center = Offset(size.width / 2, size.height * .42f), style = Stroke(2f))
    }
}

@Composable
private fun HomeScreen(
    apps: List<LauncherApp>,
    favorites: Set<String>,
    theme: NexusTheme,
    batteryPercent: Int,
    onLaunch: (LauncherApp) -> Unit,
    onOpenApps: () -> Unit,
    onOpenNexus: () -> Unit,
) {
    val favoriteApps = remember(apps, favorites) {
        apps.filter { it.packageName in favorites }.distinctBy { it.packageName }.take(5)
    }
    val dock = remember(apps, favorites) {
        if (favoriteApps.isNotEmpty()) favoriteApps.take(4)
        else {
            val preferred = listOf(
                "com.google.android.dialer", "com.android.dialer", "com.google.android.apps.messaging",
                "com.android.chrome", "com.android.camera2", "com.google.android.GoogleCamera"
            )
            apps.distinctBy { it.packageName }
                .sortedWith(compareBy<LauncherApp> {
                    preferred.indexOf(it.packageName).takeIf { idx -> idx >= 0 } ?: 999
                }.thenBy { it.label })
                .take(4)
        }
    }
    val now = rememberNexusClock()
    val hour = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val date = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr))
            .replaceFirstChar { it.titlecase(PtBr) }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PORT  /  NEXUS", color = theme.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("INTERFACE ARCANA  •  0.1", color = Muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
            }
            Text("${if (batteryPercent in 0..100) "$batteryPercent%" else "--"}  ◉", color = theme.secondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(34.dp))
        Text(hour, fontSize = 62.sp, color = Color.White, fontWeight = FontWeight.Light)
        Text(date, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        NexusCore(theme = theme, onClick = onOpenNexus)
        Text("NÚCLEO NEXUS", color = theme.secondary, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Text("Toque no núcleo para acessar os comandos", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(29.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LabelHeading("ACESSO RÁPIDO", theme.secondary)
            Text("VER TODOS", color = theme.primary, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onOpenApps))
        }
        Spacer(Modifier.height(12.dp))
        NexusPanel(Modifier.fillMaxWidth()) {
            if (dock.isEmpty()) {
                Text("Carregando aplicativos...", color = Muted)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    dock.forEach { app ->
                        AppShortcut(app = app, onLaunch = onLaunch)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Adicione favoritos pela estrela na gaveta de apps", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun NexusCore(theme: NexusTheme, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "nexus-core")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring-rotation",
    )
    Box(Modifier.size(245.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle }) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) * 0.42f
            drawCircle(theme.primary.copy(alpha = 0.27f), r, center, style = Stroke(2.dp.toPx()))
            drawCircle(theme.secondary.copy(alpha = 0.7f), r * .79f, center, style = Stroke(1.5.dp.toPx()))
            drawCircle(theme.primary.copy(alpha = 0.14f), r * .59f, center, style = Stroke(9.dp.toPx()))
            repeat(12) { i ->
                val rad = (i * 30.0 * Math.PI / 180.0).toFloat()
                val a = Offset(center.x + cos(rad) * (r * 0.88f), center.y + sin(rad) * (r * 0.88f))
                val b = Offset(center.x + cos(rad) * r, center.y + sin(rad) * r)
                drawLine(theme.secondary.copy(alpha = .76f), a, b, 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Box(
            Modifier.size(132.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF481022), Ink)))
                .border(2.dp, theme.primary, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Abrir Nexus", tint = theme.secondary, modifier = Modifier.size(44.dp))
                Text("NEXUS", color = theme.secondary, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun AppDrawerScreen(
    apps: List<LauncherApp>,
    favorites: Set<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onReload: () -> Unit,
    theme: NexusTheme,
) {
    val filtered = remember(apps, query) { apps.filter { it.label.contains(query.trim(), ignoreCase = true) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LabelHeading("ARQUIVO DE APLICATIVOS", theme.secondary)
                Text("${filtered.size} aplicativos", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onReload) {
                Icon(Icons.Rounded.Refresh, "Atualizar aplicativos", tint = theme.secondary)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Pesquisar aplicativo...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            shape = RoundedCornerShape(15.dp),
        )
        Spacer(Modifier.height(18.dp))
        if (filtered.isEmpty()) {
            Text("Nenhum aplicativo encontrado.", color = Muted)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(86.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.component.flattenToString() }) { app ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Color(0xA01A1420)).padding(vertical = 9.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(Modifier.clickable { onLaunch(app) }.padding(4.dp)) {
                                AppIcon(app = app, size = 49)
                            }
                        }
                        Text(
                            app.label, fontSize = 11.sp, color = Color.White,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.height(31.dp).fillMaxWidth().clickable { onLaunch(app) },
                        )
                        IconButton(onClick = { onToggleFavorite(app) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = if (app.packageName in favorites) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (app.packageName in favorites) "Remover favorito" else "Adicionar favorito",
                                tint = if (app.packageName in favorites) Gold else Muted,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShortcut(app: LauncherApp, onLaunch: (LauncherApp) -> Unit) {
    Column(
        Modifier.width(67.dp).clip(RoundedCornerShape(14.dp)).clickable { onLaunch(app) }.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, size = 46)
        Spacer(Modifier.height(7.dp))
        Text(app.label, fontSize = 10.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppIcon(app: LauncherApp, size: Int) {
    val context = LocalContext.current
    val image = remember(app.component) {
        runCatching {
            context.packageManager.getActivityIcon(app.component)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    if (image != null) {
        Image(bitmap = image, contentDescription = app.label, modifier = Modifier.size(size.dp))
    } else {
        Box(Modifier.size(size.dp).background(Color(0xFF352030), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Apps, contentDescription = app.label, tint = Gold)
        }
    }
}

@Composable
private fun NexusDashboard(
    appCount: Int,
    favoritesCount: Int,
    batteryPercent: Int,
    theme: NexusTheme,
    onOpenApps: () -> Unit,
    onSystemAction: (NexusAction) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        LabelHeading("CENTRAL NEXUS", theme.secondary)
        Spacer(Modifier.height(5.dp))
        Text("Controle de acesso e atalhos do dispositivo", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        NexusPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatusNumber("${appCount}", "APPS", theme.secondary)
                StatusNumber("${favoritesCount}", "FAVORITOS", theme.secondary)
                StatusNumber(if (batteryPercent in 0..100) "$batteryPercent%" else "--", "BATERIA", theme.secondary)
            }
        }
        Spacer(Modifier.height(23.dp))
        LabelHeading("COMANDOS DISPONÍVEIS", theme.secondary)
        Spacer(Modifier.height(10.dp))
        ActionEntry(Icons.Rounded.Apps, "Todos os aplicativos", "Abrir gaveta de apps", theme, onOpenApps)
        ActionEntry(Icons.Rounded.Wifi, "Configurações de Wi-Fi", "Abrir opções do Android", theme) { onSystemAction(NexusAction.WIFI_SETTINGS) }
        ActionEntry(Icons.Rounded.Bluetooth, "Configurações de Bluetooth", "Abrir opções do Android", theme) { onSystemAction(NexusAction.BLUETOOTH_SETTINGS) }
        ActionEntry(Icons.Rounded.MusicNote, "Spotify / música", "Abrir player disponível", theme) { onSystemAction(NexusAction.SPOTIFY) }
        ActionEntry(Icons.Rounded.AccessTime, "Alarmes", "Abrir aplicativo de relógio", theme) { onSystemAction(NexusAction.CLOCK) }
        ActionEntry(Icons.Rounded.Settings, "Configurações Android", "Acesso às opções do sistema", theme) { onSystemAction(NexusAction.ANDROID_SETTINGS) }
        Spacer(Modifier.height(16.dp))
        Text("NEXUS PROTOCOL 0.1  •  Execução local", fontSize = 10.sp, letterSpacing = 1.sp, color = theme.secondary)
    }
}

@Composable
private fun VisualSettings(
    theme: NexusTheme,
    onSelectTheme: (NexusTheme) -> Unit,
    onSystemAction: (NexusAction) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        LabelHeading("PERSONALIZAÇÃO", theme.secondary)
        Spacer(Modifier.height(6.dp))
        Text("Altere a assinatura visual do Nexus", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        NexusTheme.entries.forEach { option ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp).clickable { onSelectTheme(option) },
                colors = CardDefaults.cardColors(containerColor = Color(0xE01B1420)),
                shape = RoundedCornerShape(17.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (theme == option) option.primary else Color(0xFF3C2A38)),
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(option.primary).border(3.dp, option.secondary, CircleShape))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(option.subtitle, color = Muted, fontSize = 12.sp)
                    }
                    if (theme == option) Icon(Icons.Rounded.CheckCircle, "Tema ativo", tint = option.secondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LabelHeading("TELA INICIAL PADRÃO", theme.secondary)
        Spacer(Modifier.height(10.dp))
        NexusPanel(Modifier.fillMaxWidth()) {
            Text("Para substituir a tela inicial do Android, selecione Port Nexus Launcher nas configurações de app de início.", color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSystemAction(NexusAction.HOME_SETTINGS) },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
            ) {
                Icon(Icons.Rounded.Home, null)
                Spacer(Modifier.width(8.dp))
                Text("Definir como tela inicial")
            }
            Text("É possível voltar ao launcher anterior a qualquer momento pelas configurações do Android.", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun ActionEntry(icon: ImageVector, title: String, subtitle: String, theme: NexusTheme, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable(onClick = onClick),
        color = Color(0xD51B1420), shape = RoundedCornerShape(15.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(27.dp), tint = theme.secondary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White)
                Text(subtitle, fontSize = 11.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun NexusPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        color = Color(0xD01C1420),
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x774C303D)),
    ) {
        Column(Modifier.padding(15.dp), content = { content() })
    }
}

@Composable
private fun LabelHeading(text: String, color: Color) {
    Text(text, color = color, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusNumber(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Light)
        Text(label, color = color, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun rememberNexusClock(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            now = LocalDateTime.now()
        }
    }
    return now
}
