package com.port.nexuslauncher

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val V2Ink = Color(0xFF08070C)
private val V2Panel = Color(0xF019131E)
private val V2Muted = Color(0xFFC0AFB9)
private val V2White = Color(0xFFFFF7F3)
private val V2Locale = Locale("pt", "BR")

private enum class NexusPage(val text: String, val icon: ImageVector) {
    HOME("Início", Icons.Rounded.Home),
    APPS("Arsenal", Icons.Rounded.Apps),
    HUB("Nexus", Icons.Rounded.AutoAwesome),
    SETTINGS("Visual", Icons.Rounded.Palette)
}

private enum class SidePanel { NONE, COMMAND, ARCHIVE, SIGNAL }

@Composable
fun PortNexusV2(
    apps: List<LauncherApp>,
    prefs: SharedPreferences,
    batteryPercent: Int,
    onLaunchApp: (LauncherApp) -> Unit,
    onSystemAction: (NexusAction) -> Unit,
    onReloadApps: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    hasNotificationAccess: Boolean,
    onNotificationSettings: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(NexusPage.HOME) }
    var panel by remember { mutableStateOf(SidePanel.NONE) }
    var mode by remember { mutableStateOf(NexusMode.entries.firstOrNull { it.name == prefs.getString("v2_mode", "ARCANO") } ?: NexusMode.ARCANO) }
    var drawerView by remember { mutableStateOf(DrawerView.entries.firstOrNull { it.name == prefs.getString("v2_drawer", "GRADE") } ?: DrawerView.GRADE) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var hidden by remember { mutableStateOf(prefs.getStringSet("v2_hidden", emptySet())?.toSet() ?: emptySet()) }
    var recent by remember { mutableStateOf(readRecent(prefs)) }
    var notes by remember { mutableStateOf(prefs.getString("v2_notes", "") ?: "") }
    var animations by remember { mutableStateOf(prefs.getBoolean("v2_animations", true)) }
    var brightness by remember { mutableFloatStateOf(prefs.getFloat("v2_brightness", 0.7f)) }
    var iconScale by remember { mutableFloatStateOf(prefs.getFloat("v2_icon", 54f)) }
    var drawerQuery by rememberSaveable { mutableStateOf("") }

    val changeMode: (NexusMode) -> Unit = { chosen ->
        mode = chosen
        prefs.edit().putString("v2_mode", chosen.name).apply()
    }
    val changeDrawer: (DrawerView) -> Unit = { chosen ->
        drawerView = chosen
        prefs.edit().putString("v2_drawer", chosen.name).apply()
    }
    val toggleFavorite: (LauncherApp) -> Unit = { app ->
        val next = favorites.toMutableSet()
        if (!next.add(app.packageName)) next.remove(app.packageName)
        favorites = next
        prefs.edit().putStringSet("favorites", next).apply()
    }
    val toggleHidden: (LauncherApp) -> Unit = { app ->
        val next = hidden.toMutableSet()
        if (!next.add(app.packageName)) next.remove(app.packageName)
        hidden = next
        prefs.edit().putStringSet("v2_hidden", next).apply()
    }
    val openApp: (LauncherApp) -> Unit = { app ->
        recent = (listOf(app.component.flattenToString()) + recent.filter { it != app.component.flattenToString() }).take(32)
        prefs.edit().putString("v2_recent", JSONArray(recent).toString()).apply()
        onLaunchApp(app)
    }
    val openSearch: () -> Unit = { page = NexusPage.APPS; panel = SidePanel.NONE }

    MaterialTheme(colorScheme = darkColorScheme(primary = mode.primary, secondary = mode.gold, background = V2Ink, surface = V2Panel, onSurface = V2White, onBackground = V2White)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(V2Ink, mode.primary.copy(alpha = .16f).compositeOver(V2Ink), V2Ink)))) {
            AmbientGrid(mode)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (page) {
                        NexusPage.HOME -> HomeV2(apps, favorites, recent, mode, batteryPercent, animations,
                            onOpenCommand = { panel = SidePanel.COMMAND },
                            onOpenArchive = { panel = SidePanel.ARCHIVE },
                            onOpenSignal = { panel = SidePanel.SIGNAL },
                            onOpenSearch = openSearch,
                            onOpenHub = { page = NexusPage.HUB },
                            onMode = changeMode,
                            onLaunch = openApp)
                        NexusPage.APPS -> DrawerV2(apps, favorites, hidden, recent, mode, drawerView, drawerQuery, iconScale,
                            onQuery = { drawerQuery = it }, onMode = changeDrawer, onFavorite = toggleFavorite,
                            onHidden = toggleHidden, onLaunch = openApp, onReload = onReloadApps)
                        NexusPage.HUB -> HubV2(apps, recent, mode, onLaunch = openApp, onMode = changeMode,
                            onSystemAction = onSystemAction, onSearch = { query -> drawerQuery = query; openSearch() },
                            onCommandDeck = { panel = SidePanel.COMMAND }, onArchive = { panel = SidePanel.ARCHIVE })
                        NexusPage.SETTINGS -> AppearanceV2(mode, animations, iconScale,
                            onMode = changeMode,
                            onAnimations = { animations = it; prefs.edit().putBoolean("v2_animations", it).apply() },
                            onIconSize = { iconScale = it; prefs.edit().putFloat("v2_icon", it).apply() },
                            onSystemAction = onSystemAction)
                    }
                }
                BottomDockV2(page, mode, onPage = { page = it; panel = SidePanel.NONE }, onCommand = { panel = SidePanel.COMMAND })
            }
            AnimatedVisibility(
                visible = panel != SidePanel.NONE,
                enter = fadeIn(tween(200)), exit = fadeOut(tween(160))
            ) {
                Box(Modifier.fillMaxSize().background(Color(0xB9000000)).clickable { panel = SidePanel.NONE })
            }
            AnimatedVisibility(
                visible = panel == SidePanel.COMMAND,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(310)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(240)),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    CommandPanelV2(mode, brightness, onBrightness = {
                        brightness = it
                        prefs.edit().putFloat("v2_brightness", it).apply()
                        onBrightnessChange(it)
                    }, onClose = { panel = SidePanel.NONE }, onAction = onSystemAction,
                        onNotifications = { panel = SidePanel.SIGNAL },
                        onApp = { app -> panel = SidePanel.NONE; openApp(app) }, apps = apps)
                }
            }
            AnimatedVisibility(
                visible = panel == SidePanel.ARCHIVE,
                enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(310)),
                exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(240)),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    ArchivePanelV2(mode, recent, apps, notes, onNotes = { notes = it; prefs.edit().putString("v2_notes", it).apply() },
                        onClose = { panel = SidePanel.NONE }, onLaunch = { app -> panel = SidePanel.NONE; openApp(app) }, onAction = onSystemAction)
                }
            }
            AnimatedVisibility(
                visible = panel == SidePanel.SIGNAL,
                enter = fadeIn(tween(230)),
                exit = fadeOut(tween(180)),
            ) {
                NexusNotificationPanel(
                    mode = mode,
                    hasPermission = hasNotificationAccess,
                    isConnected = NexusNotificationStore.connected,
                    notices = NexusNotificationStore.notices,
                    onClose = { panel = SidePanel.NONE },
                    onGrant = onNotificationSettings,
                    onOpen = { key -> if (NexusNotificationStore.open(key)) panel = SidePanel.NONE },
                    onDismiss = NexusNotificationStore::dismiss,
                    onDismissAll = NexusNotificationStore::dismissClearable,
                    onSystemAction = onSystemAction,
                )
            }
        }
    }
}

private fun readRecent(prefs: SharedPreferences): List<String> = runCatching {
    val array = JSONArray(prefs.getString("v2_recent", "[]"))
    (0 until array.length()).map { array.getString(it) }
}.getOrDefault(emptyList())

@Composable
private fun AmbientGrid(mode: NexusMode) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 48.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(mode.gold.copy(alpha = .037f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
        var y = 0f
        while (y < size.height) { drawLine(mode.primary.copy(alpha = .052f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
        drawCircle(mode.primary.copy(alpha = .075f), radius = size.width * .7f,
            center = Offset(size.width * .5f, size.height * .35f), style = Stroke(width = 2f))
    }
}

@Composable
private fun HomeV2(
    apps: List<LauncherApp>, favorites: Set<String>, recent: List<String>, mode: NexusMode,
    battery: Int, animations: Boolean,
    onOpenCommand: () -> Unit, onOpenArchive: () -> Unit, onOpenSignal: () -> Unit, onOpenSearch: () -> Unit,
    onOpenHub: () -> Unit, onMode: (NexusMode) -> Unit, onLaunch: (LauncherApp) -> Unit,
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }
    var signalDrag by remember { mutableFloatStateOf(0f) }
    val dockApps = remember(apps, favorites, recent, mode) {
        val favoritesFirst = apps.filter { it.packageName in favorites }
        val recentFirst = recent.mapNotNull { id -> apps.firstOrNull { it.component.flattenToString() == id } }
        val relevant = apps.filter { app -> when (mode) {
            NexusMode.MUSICA -> classifyApp(app) == AppGroup.MUSICA
            NexusMode.ESTUDO -> classifyApp(app) == AppGroup.ESTUDO
            NexusMode.PERFORMANCE -> classifyApp(app) == AppGroup.JOGOS
            NexusMode.INDUSTRIAL -> classifyApp(app) == AppGroup.FERRAMENTAS
            else -> false
        } }
        (favoritesFirst + relevant + recentFirst + apps).distinctBy { it.packageName }.take(5)
    }
    val clock = rememberLiveClock()
    Column(
        Modifier.fillMaxSize().pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { dragAmount = 0f },
                onHorizontalDrag = { change, amount -> dragAmount += amount; change.consume() },
                onDragEnd = {
                    if (dragAmount > 105f) onOpenArchive()
                    if (dragAmount < -105f) onOpenCommand()
                    dragAmount = 0f
                }, onDragCancel = { dragAmount = 0f }
            )
        }.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(25.dp).pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { signalDrag = 0f },
                    onVerticalDrag = { change, amount -> if (amount > 0) signalDrag += amount; change.consume() },
                    onDragEnd = { if (signalDrag > 55f) onOpenSignal(); signalDrag = 0f },
                    onDragCancel = { signalDrag = 0f },
                )
            }.clickable(onClick = onOpenSignal),
            contentAlignment = Alignment.Center,
        ) {
            Text("⌄   N E X U S   S I G N A L   ⌄", color = mode.gold.copy(alpha = .75f),
                fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("P O R T   /   N E X U S", color = mode.gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("PERSONAL OPERATING INTERFACE", color = V2Muted, fontSize = 9.sp, letterSpacing = .8.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.clip(RoundedCornerShape(14.dp))
                    .border(1.dp, mode.primary.copy(alpha = .85f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenSignal).padding(horizontal = 9.dp, vertical = 10.dp)) {
                    Text("◉  ${NexusNotificationStore.notices.size}", color = mode.gold,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.clip(RoundedCornerShape(14.dp))
                    .border(1.dp, mode.gold.copy(alpha = .6f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenCommand).padding(horizontal = 10.dp, vertical = 10.dp)) {
                    Text("⌘ DECK", color = mode.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(clock.format(DateTimeFormatter.ofPattern("HH:mm")), color = V2White, fontSize = 57.sp, fontWeight = FontWeight.Light, lineHeight = 60.sp)
                Text(clock.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", V2Locale)).replaceFirstChar { it.titlecase(V2Locale) },
                    color = V2Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (battery in 0..100) "$battery%" else "--", color = mode.gold, fontSize = 27.sp, fontWeight = FontWeight.Light)
                Text("BATERIA  ◉", color = V2Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        ReactorV2(mode, animations, onOpenHub)
        Text("N E X U S    /    C O R E", color = mode.gold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Text(mode.caption, color = V2Muted, fontSize = 10.sp, letterSpacing = 1.3.sp)
        Spacer(Modifier.height(17.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TinyActionV2("◁  ARCHIVE", mode, onOpenArchive)
            TinyActionV2("⌕  PESQUISAR", mode, onOpenSearch)
            TinyActionV2("DECK  ▷", mode, onOpenCommand)
        }
        Spacer(Modifier.height(23.dp))
        SectionV2("PROTOCOLOS DISPONÍVEIS", mode)
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            NexusMode.entries.forEach { candidate ->
                val selected = candidate == mode
                Box(Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (selected) mode.primary.copy(alpha = .25f) else V2Panel)
                    .border(1.dp, if (selected) mode.gold else mode.gold.copy(alpha = .18f), RoundedCornerShape(12.dp))
                    .clickable { onMode(candidate) }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("${candidate.glyph}  ${candidate.title}", color = if (selected) mode.gold else V2Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(21.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionV2("DOCK MUTÁVEL", mode)
            Text("ABRIR ARSENAL  ›", color = mode.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.clickable(onClick = onOpenSearch))
        }
        Spacer(Modifier.height(10.dp))
        GlassV2(mode, Modifier.fillMaxWidth()) {
            if (dockApps.isEmpty()) Text("Os aplicativos aparecerão aqui.", color = V2Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                dockApps.forEach { app ->
                    Column(Modifier.width(60.dp).clip(RoundedCornerShape(12.dp)).clickable { onLaunch(app) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        V2AppIcon(app, 42)
                        Spacer(Modifier.height(7.dp))
                        Text(app.label, color = V2White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxWidth()) {
            QuickModuleV2("⌘", "COMMAND DECK", "Controle local", mode, Modifier.weight(1f), onOpenCommand)
            QuickModuleV2("◇", "ARCHIVE", "Notas e histórico", mode, Modifier.weight(1f), onOpenArchive)
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun ReactorV2(mode: NexusMode, animations: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "nexus-v2-core")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "spin")
    val reverse by transition.animateFloat(360f, 0f, infiniteRepeatable(tween(32000, easing = LinearEasing), RepeatMode.Restart), label = "reverse")
    Box(Modifier.size(276.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(252.dp).graphicsLayer { rotationZ = if (animations) spin else 0f }) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = min(size.width, size.height) * .45f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(mode.primary.copy(alpha = .08f), radius * 1.03f)
                drawCircle(mode.gold.copy(alpha = .4f), radius, center, style = Stroke(1.dp.toPx()))
                drawCircle(mode.primary.copy(alpha = .6f), radius * .78f, center, style = Stroke(1.5.dp.toPx()))
                for (i in 0 until 9) {
                    val start = i * 40f
                    drawArc(mode.gold.copy(alpha = if (i % 2 == 0) .88f else .25f), start, 20f, false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
                repeat(24) { index ->
                    val theta = Math.toRadians(index * 15.0)
                    val start = Offset(center.x + cos(theta).toFloat() * radius * .90f, center.y + sin(theta).toFloat() * radius * .90f)
                    val end = Offset(center.x + cos(theta).toFloat() * radius, center.y + sin(theta).toFloat() * radius)
                    drawLine(mode.gold.copy(alpha = .48f), start, end, if (index % 3 == 0) 2.dp.toPx() else 1.dp.toPx())
                }
            }
        }
        Canvas(Modifier.size(190.dp).graphicsLayer { rotationZ = if (animations) reverse else 0f }) {
            val radius = size.minDimension * .46f
            drawCircle(mode.gold.copy(alpha = .22f), radius = radius, style = Stroke(1.dp.toPx()))
            for (i in 0..5) drawArc(mode.primary.copy(alpha = .8f), i * 60f, 34f, false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        }
        Box(Modifier.size(124.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(mode.primary.copy(alpha = .53f), Color(0xFF230B18), V2Ink)))
            .border(1.5.dp, mode.gold, CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mode.glyph, color = mode.gold, fontSize = 42.sp, lineHeight = 46.sp)
                Text("NEXUS", color = V2White, fontSize = 15.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
                Text("V 2 . 0", color = mode.gold, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
        }
        Text("✦", color = mode.gold, modifier = Modifier.align(Alignment.TopCenter).offset(y = 4.dp), fontSize = 19.sp)
        Text("✧", color = mode.primary, modifier = Modifier.align(Alignment.CenterStart).offset(x = 4.dp), fontSize = 16.sp)
        Text("✧", color = mode.primary, modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-4).dp), fontSize = 16.sp)
    }
}

@Composable
private fun DrawerV2(
    apps: List<LauncherApp>, favorites: Set<String>, hidden: Set<String>, recent: List<String>,
    mode: NexusMode, drawerMode: DrawerView, query: String, iconSize: Float,
    onQuery: (String) -> Unit, onMode: (DrawerView) -> Unit,
    onFavorite: (LauncherApp) -> Unit, onHidden: (LauncherApp) -> Unit,
    onLaunch: (LauncherApp) -> Unit, onReload: () -> Unit,
) {
    var category by remember { mutableStateOf<AppGroup?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    val source = remember(apps, hidden, showHidden, query, drawerMode, favorites, recent, category) {
        var list = apps.filter { (showHidden || it.packageName !in hidden) && it.label.contains(query.trim(), ignoreCase = true) }
        if (drawerMode == DrawerView.ARSENAL && category != null) list = list.filter { classifyApp(it) == category }
        if (drawerMode == DrawerView.INTELIGENTE) {
            list = list.sortedWith(compareBy<LauncherApp> {
                when {
                    it.component.flattenToString() in recent -> recent.indexOf(it.component.flattenToString())
                    it.packageName in favorites -> 35
                    else -> 200
                }
            }.thenBy { it.label.lowercase() })
        }
        list
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 17.dp)) {
        Spacer(Modifier.height(19.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TitleV2("O ARSENAL", mode)
                Text("${source.size} aplicativos • ${apps.size} instalados", color = V2Muted, fontSize = 11.sp)
            }
            IconButton(onClick = onReload) { Icon(Icons.Rounded.Refresh, "Atualizar", tint = mode.gold) }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Localizar aplicativo") }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = mode.gold) },
            shape = RoundedCornerShape(17.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DrawerView.entries.forEach { option ->
                FilterPillV2(option.title, option == drawerMode, mode, Modifier.weight(1f)) { onMode(option); category = null }
            }
        }
        if (drawerMode == DrawerView.ARSENAL) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPillV2("Todos", category == null, mode) { category = null }
                AppGroup.entries.forEach { group -> FilterPillV2("${group.glyph} ${group.title}", category == group, mode) { category = group } }
            }
        }
        if (drawerMode == DrawerView.INTELIGENTE) {
            Spacer(Modifier.height(10.dp))
            Text("Ordem: apps abertos pelo Nexus, favoritos e demais apps. Sem rastrear seu uso fora do launcher.", color = V2Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("APLICATIVOS", color = mode.gold, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            Text(if (showHidden) "◉ EXIBINDO OCULTOS" else "◌ MOSTRAR OCULTOS", color = V2Muted, fontSize = 10.sp,
                modifier = Modifier.clickable { showHidden = !showHidden }.padding(8.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (source.isEmpty()) {
            GlassV2(mode, Modifier.fillMaxWidth()) { Text("Nenhum aplicativo para este filtro. Altere a busca ou mostre os ocultos.", color = V2Muted) }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(source, key = { it.component.flattenToString() }) { app ->
                    AppTileV2(app, mode, app.packageName in favorites, app.packageName in hidden, iconSize,
                        onLaunch = { onLaunch(app) }, onFavorite = { onFavorite(app) }, onHidden = { onHidden(app) })
                }
            }
        }
    }
}

@Composable
private fun AppTileV2(app: LauncherApp, mode: NexusMode, favorite: Boolean, hidden: Boolean, iconSize: Float,
    onLaunch: () -> Unit, onFavorite: () -> Unit, onHidden: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = V2Panel, border = BorderStroke(1.dp, mode.gold.copy(alpha = if (favorite) .6f else .16f))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clickable(onClick = onLaunch).padding(4.dp)) { V2AppIcon(app, iconSize.toInt()) }
            Text(app.label, color = V2White, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(33.dp).clickable(onClick = onLaunch))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        if (favorite) "Desfavoritar" else "Favoritar", tint = if (favorite) mode.gold else V2Muted, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onHidden, modifier = Modifier.size(32.dp)) {
                    Icon(if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        if (hidden) "Reexibir" else "Ocultar", tint = V2Muted, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun V2AppIcon(app: LauncherApp, size: Int) {
    val context = LocalContext.current
    val image = remember(app.component) {
        runCatching { context.packageManager.getActivityIcon(app.component).toBitmap(width = 96, height = 96).asImageBitmap() }.getOrNull()
    }
    if (image != null) Image(bitmap = image, contentDescription = app.label, modifier = Modifier.size(size.dp))
    else Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFF4E2534)), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Apps, contentDescription = app.label, tint = Color(0xFFECC17F))
    }
}

@Composable
private fun HubV2(
    apps: List<LauncherApp>, recent: List<String>, mode: NexusMode, onLaunch: (LauncherApp) -> Unit,
    onMode: (NexusMode) -> Unit, onSystemAction: (NexusAction) -> Unit,
    onSearch: (String) -> Unit, onCommandDeck: () -> Unit, onArchive: () -> Unit,
) {
    var command by rememberSaveable { mutableStateOf("") }
    var response by remember { mutableStateOf("NEXUS pronto. Comandos locais ativos.") }
    val process: () -> Unit = {
        val input = command.trim().lowercase(V2Locale)
        val targetMode = NexusMode.entries.firstOrNull { input.contains(it.title.lowercase(V2Locale)) }
        val targetApp = apps.firstOrNull { it.label.equals(input.removePrefix("abrir "), true) }
            ?: apps.firstOrNull { input.startsWith("abrir ") && it.label.contains(input.removePrefix("abrir "), true) }
        when {
            input.isEmpty() -> response = "Digite um comando para o Nexus."
            targetMode != null -> { onMode(targetMode); response = "Protocolo ${targetMode.title} ativado." }
            targetApp != null -> { response = "Abrindo ${targetApp.label}."; onLaunch(targetApp) }
            input.contains("wifi") || input.contains("wi-fi") -> { onSystemAction(NexusAction.WIFI_SETTINGS); response = "Abrindo configurações de Wi-Fi." }
            input.contains("bluetooth") -> { onSystemAction(NexusAction.BLUETOOTH_SETTINGS); response = "Abrindo configurações de Bluetooth." }
            input.contains("alarme") || input.contains("relogio") || input.contains("relógio") -> { onSystemAction(NexusAction.CLOCK); response = "Abrindo alarmes." }
            input.contains("spotify") -> { onSystemAction(NexusAction.SPOTIFY); response = "Abrindo áudio." }
            input.contains("configura") -> { onSystemAction(NexusAction.ANDROID_SETTINGS); response = "Abrindo configurações." }
            else -> { response = "Pesquisando aplicativos por: $command"; onSearch(command.removePrefix("abrir ")) }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(19.dp)) {
        Spacer(Modifier.height(6.dp))
        TitleV2("NEXUS / COGNIÇÃO", mode)
        Text("Console local de comandos e protocolos", color = V2Muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        GlassV2(mode, Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◇", color = mode.gold, fontSize = 35.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("INTERFACE RESPONDENDO", color = mode.gold, fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                    Text(response, color = V2White, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = command, onValueChange = { command = it }, singleLine = true,
                label = { Text("Ex.: modo música / abrir Spotify") }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(10.dp))
            Button(onClick = process, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = mode.primary)) { Text("EXECUTAR COMANDO", color = V2White, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(16.dp))
        SectionV2("COMANDOS DE ACESSO", mode)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickModuleV2("⌘", "COMMAND DECK", "Conectividade e brilho", mode, Modifier.weight(1f), onCommandDeck)
            QuickModuleV2("◇", "ARCHIVE", "Histórico e notas", mode, Modifier.weight(1f), onArchive)
        }
        Spacer(Modifier.height(18.dp))
        SectionV2("MUDANÇA DE PROTOCOLO", mode)
        Spacer(Modifier.height(10.dp))
        NexusMode.entries.forEach { candidate ->
            ActionBarV2(candidate.glyph, candidate.title, candidate.caption, mode, candidate == mode) { onMode(candidate) }
        }
        Spacer(Modifier.height(13.dp))
        Text("Motor local: não utiliza IA remota nem executa comandos de sistema sem autorização do Android.",
            color = V2Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun AppearanceV2(mode: NexusMode, animations: Boolean, iconSize: Float,
    onMode: (NexusMode) -> Unit, onAnimations: (Boolean) -> Unit, onIconSize: (Float) -> Unit,
    onSystemAction: (NexusAction) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(19.dp)) {
        TitleV2("O ATELIÊ", mode)
        Text("Personalize a assinatura do Nexus", color = V2Muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        SectionV2("PROTOCOLOS VISUAIS", mode)
        Spacer(Modifier.height(12.dp))
        NexusMode.entries.forEach { candidate ->
            Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable { onMode(candidate) },
                color = V2Panel, shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (mode == candidate) candidate.gold else candidate.primary.copy(alpha = .25f))) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(candidate.primary.copy(alpha = .22f))
                        .border(1.dp, candidate.gold, CircleShape), contentAlignment = Alignment.Center) {
                        Text(candidate.glyph, color = candidate.gold, fontSize = 23.sp)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(candidate.title, color = V2White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(candidate.caption, color = V2Muted, fontSize = 10.sp)
                    }
                    if (mode == candidate) Icon(Icons.Rounded.CheckCircle, "Selecionado", tint = candidate.gold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionV2("ACABAMENTO DA INTERFACE", mode)
        Spacer(Modifier.height(10.dp))
        GlassV2(mode, Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Animações do núcleo", color = V2White, fontWeight = FontWeight.Bold)
                    Text(if (animations) "Rotação arcana ativa" else "Modo estático / economia", color = V2Muted, fontSize = 11.sp)
                }
                FilterPillV2(if (animations) "LIGADO" else "DESLIGADO", animations, mode) { onAnimations(!animations) }
            }
            Spacer(Modifier.height(15.dp))
            Text("Tamanho dos ícones  ${iconSize.toInt()} dp", color = V2White, fontSize = 13.sp)
            Slider(value = iconSize, onValueChange = onIconSize, valueRange = 38f..70f)
        }
        Spacer(Modifier.height(17.dp))
        SectionV2("SISTEMA OPERACIONAL", mode)
        Spacer(Modifier.height(9.dp))
        GlassV2(mode, Modifier.fillMaxWidth()) {
            Text("Defina o Nexus como tela inicial padrão. Você pode voltar ao launcher anterior quando quiser.", color = V2White, fontSize = 12.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(11.dp))
            Button(onClick = { onSystemAction(NexusAction.HOME_SETTINGS) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = mode.primary)) {
                Icon(Icons.Rounded.Home, null); Spacer(Modifier.width(9.dp)); Text("DEFINIR TELA INICIAL")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CommandPanelV2(mode: NexusMode, brightness: Float, onBrightness: (Float) -> Unit,
    onClose: () -> Unit, onAction: (NexusAction) -> Unit,
    onNotifications: () -> Unit, onApp: (LauncherApp) -> Unit, apps: List<LauncherApp>) {
    val spotify = apps.firstOrNull { it.packageName == "com.spotify.music" }
    Column(Modifier.fillMaxHeight().fillMaxWidth(.91f).background(Brush.verticalGradient(listOf(Color(0xFF23141D), V2Ink)))
        .border(BorderStroke(1.dp, mode.gold.copy(alpha = .55f))).verticalScroll(rememberScrollState()).padding(19.dp)) {
        PanelHeaderV2("COMMAND DECK", "CONTROLE DO DISPOSITIVO", mode, onClose)
        Spacer(Modifier.height(26.dp))
        SectionV2("LUMINOSIDADE DESTA INTERFACE", mode)
        Spacer(Modifier.height(13.dp))
        GlassV2(mode, Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brilho local", color = V2White)
                Text("${(brightness * 100).toInt()}%", color = mode.gold)
            }
            Slider(value = brightness, onValueChange = onBrightness, valueRange = .05f..1f)
            Text("Ajusta apenas a janela do Nexus. O brilho global continua controlado pelo Android.", color = V2Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(18.dp))
        SectionV2("ACESSO AO SISTEMA", mode)
        Spacer(Modifier.height(9.dp))
        ActionBarV2("◉", "Nexus Signal", "Central de notificações personalizada", mode, onNotifications)
        ActionBarV2("◉", "Wi-Fi", "Abrir controles do Android", mode) { onAction(NexusAction.WIFI_SETTINGS) }
        ActionBarV2("ᛒ", "Bluetooth", "Gerenciar dispositivos", mode) { onAction(NexusAction.BLUETOOTH_SETTINGS) }
        ActionBarV2("☼", "Tela e brilho", "Configurações de exibição", mode) { onAction(NexusAction.DISPLAY_SETTINGS) }
        ActionBarV2("♫", "Áudio", "Volumes e sons do sistema", mode) { onAction(NexusAction.SOUND_SETTINGS) }
        ActionBarV2("◷", "Alarmes", "Relógio do sistema", mode) { onAction(NexusAction.CLOCK) }
        ActionBarV2("⚙", "Configurações", "Gerenciar o Android", mode) { onAction(NexusAction.ANDROID_SETTINGS) }
        Spacer(Modifier.height(14.dp))
        SectionV2("PORT MUSIC", mode)
        Spacer(Modifier.height(8.dp))
        ActionBarV2("♫", "Spotify", if (spotify == null) "Abrir player compatível" else "Abrir Spotify", mode) {
            if (spotify != null) onApp(spotify) else onAction(NexusAction.SPOTIFY)
        }
        Text("Atalhos respeitam as permissões do Android. Não é possível alternar todos os controles diretamente sem privilégios adicionais.",
            color = V2Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ArchivePanelV2(mode: NexusMode, recent: List<String>, apps: List<LauncherApp>, notes: String,
    onNotes: (String) -> Unit, onClose: () -> Unit, onLaunch: (LauncherApp) -> Unit, onAction: (NexusAction) -> Unit) {
    val recentApps = recent.mapNotNull { id -> apps.firstOrNull { it.component.flattenToString() == id } }.take(7)
    Column(Modifier.fillMaxHeight().fillMaxWidth(.91f).background(Brush.verticalGradient(listOf(Color(0xFF201321), V2Ink)))
        .border(BorderStroke(1.dp, mode.gold.copy(alpha = .55f))).verticalScroll(rememberScrollState()).padding(19.dp)) {
        PanelHeaderV2("ARCHIVE", "MEMÓRIA LOCAL NEXUS", mode, onClose)
        Spacer(Modifier.height(25.dp))
        SectionV2("NOTAS RÁPIDAS", mode)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(value = notes, onValueChange = onNotes, modifier = Modifier.fillMaxWidth().height(155.dp),
            label = { Text("Anotações salvas no dispositivo") }, maxLines = 6, shape = RoundedCornerShape(16.dp))
        Text("Salvamento automático local", color = V2Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(23.dp))
        SectionV2("ÚLTIMOS ACESSOS PELO NEXUS", mode)
        Spacer(Modifier.height(10.dp))
        if (recentApps.isEmpty()) Text("Nenhum acesso registrado ainda.", color = V2Muted, fontSize = 13.sp)
        recentApps.forEach { app -> ActionBarV2("◈", app.label, classifyApp(app).title, mode) { onLaunch(app) } }
        Spacer(Modifier.height(19.dp))
        SectionV2("ACESSO RÁPIDO", mode)
        Spacer(Modifier.height(9.dp))
        ActionBarV2("◷", "Agenda", "Abrir calendário do Android", mode) { onAction(NexusAction.CALENDAR) }
        ActionBarV2("◇", "Armazenamento", "Ver uso do armazenamento", mode) { onAction(NexusAction.STORAGE_SETTINGS) }
    }
}

@Composable
private fun PanelHeaderV2(title: String, subtitle: String, mode: NexusMode, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            TitleV2(title, mode)
            Text(subtitle, color = V2Muted, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Fechar", tint = mode.gold) }
    }
}

@Composable
private fun BottomDockV2(page: NexusPage, mode: NexusMode, onPage: (NexusPage) -> Unit, onCommand: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xF408080C))
        .border(BorderStroke(1.dp, mode.gold.copy(alpha = .17f))).padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        NexusPage.entries.forEach { entry ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                .background(if (page == entry) mode.primary.copy(alpha = .27f) else Color.Transparent)
                .clickable { onPage(entry) }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(entry.icon, entry.text, tint = if (page == entry) mode.gold else V2Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp))
                Text(entry.text, color = if (page == entry) mode.gold else V2Muted, fontSize = 10.sp, fontWeight = if (page == entry) FontWeight.Bold else FontWeight.Normal)
            }
        }
        Box(Modifier.clip(CircleShape).border(1.dp, mode.gold.copy(alpha = .7f), CircleShape)
            .background(mode.primary.copy(alpha = .27f)).clickable(onClick = onCommand).padding(14.dp), contentAlignment = Alignment.Center) {
            Text("⌘", color = mode.gold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ActionBarV2(glyph: String, title: String, subtitle: String, mode: NexusMode, selected: Boolean = false, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick),
        color = if (selected) mode.primary.copy(alpha = .24f) else V2Panel,
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, mode.gold.copy(alpha = if (selected) .65f else .17f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(mode.primary.copy(alpha = .19f)), contentAlignment = Alignment.Center) {
                Text(glyph, color = mode.gold, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = V2White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = V2Muted, fontSize = 10.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = mode.gold.copy(alpha = .75f), modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun GlassV2(mode: NexusMode, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, color = V2Panel, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, mode.gold.copy(alpha = .28f))) {
        Column(Modifier.padding(14.dp), content = { content() })
    }
}

@Composable
private fun QuickModuleV2(glyph: String, title: String, subtitle: String, mode: NexusMode, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.clickable(onClick = onClick), color = V2Panel, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, mode.primary.copy(alpha = .45f))) {
        Column(Modifier.padding(14.dp)) {
            Text(glyph, color = mode.gold, fontSize = 26.sp)
            Spacer(Modifier.height(7.dp))
            Text(title, color = V2White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = .5.sp)
            Text(subtitle, color = V2Muted, fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun TinyActionV2(title: String, mode: NexusMode, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(V2Panel)
        .border(1.dp, mode.gold.copy(alpha = .3f), RoundedCornerShape(10.dp))
        .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp)) {
        Text(title, fontSize = 10.sp, color = mode.gold, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilterPillV2(title: String, selected: Boolean, mode: NexusMode, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(11.dp))
        .background(if (selected) mode.primary.copy(alpha = .3f) else V2Panel)
        .border(1.dp, if (selected) mode.gold else mode.gold.copy(alpha = .2f), RoundedCornerShape(11.dp))
        .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(title, color = if (selected) mode.gold else V2Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionV2(title: String, mode: NexusMode) {
    Text(title, color = mode.gold, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 11.sp)
}

@Composable
private fun TitleV2(title: String, mode: NexusMode) {
    Text(title, color = V2White, fontWeight = FontWeight.Black, fontSize = 27.sp, letterSpacing = .8.sp)
    Box(Modifier.width(50.dp).height(2.dp).background(mode.gold))
}

@Composable
private fun rememberLiveClock(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(10_000); now = LocalDateTime.now() } }
    return now
}
