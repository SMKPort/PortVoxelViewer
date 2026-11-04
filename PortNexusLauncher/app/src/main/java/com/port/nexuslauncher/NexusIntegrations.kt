package com.port.nexuslauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val DeepSpace = Color(0xFF0C0912)
private val Frost = Color(0xFFF9EFF2)
private val Faded = Color(0xFFBCAEBB)

private data class NexusAudioPreset(val name: String, val summary: String, val bands: List<Int>)
private val AudioPresets = listOf(
    NexusAudioPreset("Paredão", "Graves marcantes e ataque", listOf(88, 74, 50, 64, 78)),
    NexusAudioPreset("Grave Extremo", "Foco nas frequências baixas", listOf(96, 89, 49, 36, 48)),
    NexusAudioPreset("Voz Limpa", "Prioridade para definição vocal", listOf(48, 44, 61, 81, 73)),
    NexusAudioPreset("Funk", "Subgrave e presença", listOf(94, 80, 51, 66, 71)),
)

private data class SessionInfo(
    val title: String = "Nenhuma faixa ativa",
    val artist: String = "Abra seu player para começar",
    val app: String = "",
    val playing: Boolean = false,
    val available: Boolean = false,
)

private fun activeController(context: Context, allowed: Boolean): MediaController? {
    if (!allowed) return null
    return runCatching {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val listener = ComponentName(context, NexusNotificationListener::class.java)
        val controllers = manager.getActiveSessions(listener)
        controllers.firstOrNull { it.packageName == "com.spotify.music" }
            ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
    }.getOrNull()
}

private fun readSession(context: Context, allowed: Boolean): SessionInfo {
    val controller = activeController(context, allowed) ?: return SessionInfo()
    val metadata = controller.metadata
    return SessionInfo(
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: "Mídia em execução",
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: "Sessão do player",
        app = controller.packageName.substringAfterLast('.').uppercase(),
        playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
        available = true,
    )
}

private fun mediaAction(context: Context, allowed: Boolean, action: String) {
    val control = activeController(context, allowed)?.transportControls ?: return
    runCatching {
        when (action) {
            "previous" -> control.skipToPrevious()
            "next" -> control.skipToNext()
            "pause" -> control.pause()
            "play" -> control.play()
        }
    }
}

private fun locateAudioBooster(apps: List<LauncherApp>): LauncherApp? =
    apps.firstOrNull {
        val text = (it.label + " " + it.packageName).lowercase()
        text.contains("port audio booster") || (text.contains("port") && text.contains("audio") && text.contains("booster"))
    } ?: apps.firstOrNull { it.label.contains("Audio Booster", ignoreCase = true) }

@Composable
fun NexusAudioTab(
    mode: NexusMode,
    apps: List<LauncherApp>,
    prefs: SharedPreferences,
    hasNotificationAccess: Boolean,
    onLaunchApp: (LauncherApp) -> Unit,
    onSpotify: () -> Unit,
    onGrantMedia: () -> Unit,
    onReloadApps: () -> Unit,
) {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember(audio) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var session by remember { mutableStateOf(SessionInfo()) }
    var selected by remember {
        mutableStateOf(AudioPresets.firstOrNull { it.name == prefs.getString("v22_audio_preset", "Paredão") } ?: AudioPresets[0])
    }
    var ab by remember { mutableStateOf(prefs.getBoolean("v22_audio_ab", false)) }
    val booster = locateAudioBooster(apps)
    LaunchedEffect(hasNotificationAccess) {
        while (true) {
            session = readSession(context, hasNotificationAccess)
            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            delay(1800L)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(19.dp))
        NexusModuleHeading("PORT / AUDIO", "ESTÚDIO NEXUS 2.2", mode)
        Spacer(Modifier.height(17.dp))
        NexusModuleCard(mode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(66.dp).background(mode.primary.copy(alpha = .2f), CircleShape)
                    .border(1.dp, mode.gold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Headphones, null, tint = mode.gold, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("PORT AUDIO BOOSTER", color = Frost, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(if (booster == null) "Aplicativo não detectado" else "Aplicativo encontrado no dispositivo",
                        color = Faded, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = { if (booster != null) onLaunchApp(booster) else onReloadApps() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = mode.primary),
            ) {
                Icon(if (booster == null) Icons.Rounded.OpenInNew else Icons.Rounded.MusicNote, null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (booster != null) "ABRIR EQUALIZADOR" else "PROCURAR AUDIO BOOSTER", fontWeight = FontWeight.Bold)
            }
            Text(
                "O Nexus abre o Port Audio Booster instalado. Para aplicar presets do launcher ao áudio, " +
                    "o Booster precisará expor um comando de integração próprio.",
                color = Faded, fontSize = 11.sp, lineHeight = 17.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        NexusModuleCaption("PAINEL DE PERFIS", mode)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AudioPresets.forEach { preset ->
                val active = selected.name == preset.name
                Box(Modifier.background(if (active) mode.primary.copy(alpha = .29f) else DeepSpace,
                    RoundedCornerShape(12.dp)).border(1.dp, if (active) mode.gold else mode.gold.copy(alpha = .24f),
                    RoundedCornerShape(12.dp)).clickable {
                        selected = preset
                        prefs.edit().putString("v22_audio_preset", preset.name).apply()
                    }.padding(horizontal = 12.dp, vertical = 11.dp)) {
                    Text(preset.name, color = if (active) mode.gold else Frost,
                        fontSize = 11.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        NexusModuleCard(mode) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(selected.name.uppercase(), fontWeight = FontWeight.Black, color = mode.gold)
                Text(selected.summary, fontSize = 10.sp, color = Faded)
            }
            Spacer(Modifier.height(11.dp))
            val shown = if (ab) AudioPresets[0] else selected
            Canvas(Modifier.fillMaxWidth().height(118.dp)) {
                val labels = shown.bands
                val w = size.width / labels.size
                labels.forEachIndexed { index, gain ->
                    val x = w * (index + .5f)
                    val range = size.height * .7f
                    val height = range * gain / 100f
                    drawLine(
                        color = if (index % 2 == 0) mode.gold else mode.primary,
                        start = Offset(x, size.height - height),
                        end = Offset(x, size.height - 8f),
                        strokeWidth = w * .42f, cap = StrokeCap.Round,
                    )
                }
                drawLine(mode.gold.copy(alpha = .26f), Offset(0f, size.height * .5f),
                    Offset(size.width, size.height * .5f), 1f)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("60 Hz", "230 Hz", "910 Hz", "3,6 kHz", "14 kHz").forEach {
                    Text(it, color = Faded, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("COMPARAÇÃO VISUAL A/B", color = Faded, fontSize = 10.sp)
                Text(if (ab) "A · PAREDÃO" else "B · ${selected.name.uppercase()}",
                    modifier = Modifier.clickable {
                        ab = !ab
                        prefs.edit().putBoolean("v22_audio_ab", ab).apply()
                    }.background(mode.primary.copy(alpha = .25f), RoundedCornerShape(8.dp))
                        .padding(9.dp), color = mode.gold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Text("As curvas são referências visuais. Selecioná-las não modifica o som de outros aplicativos.",
                color = Faded, fontSize = 10.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(18.dp))
        NexusModuleCaption("ÁUDIO DO DISPOSITIVO", mode)
        Spacer(Modifier.height(9.dp))
        NexusModuleCard(mode) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeUp, null, tint = mode.gold)
                Spacer(Modifier.width(10.dp))
                Text("Volume de mídia", color = Frost, modifier = Modifier.weight(1f))
                Text("${(volume / maxVolume * 100).roundToInt()}%", color = mode.gold)
            }
            Slider(
                value = volume.coerceIn(0f, maxVolume.toFloat()),
                valueRange = 0f..maxVolume.toFloat(),
                onValueChange = { value ->
                    val v = value.roundToInt().coerceIn(0, maxVolume)
                    runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0) }
                    volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                },
            )
        }
        Spacer(Modifier.height(17.dp))
        NexusModuleCaption("SESSÃO DE MÍDIA", mode)
        Spacer(Modifier.height(9.dp))
        NexusModuleCard(mode) {
            Text(if (session.available) session.app else "PLAYER NÃO CONECTADO",
                fontSize = 10.sp, color = mode.gold, fontWeight = FontWeight.Bold)
            Text(session.title, fontSize = 17.sp, color = Frost, fontWeight = FontWeight.Black,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(session.artist, fontSize = 12.sp, color = Faded, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(enabled = session.available, onClick = { mediaAction(context, hasNotificationAccess, "previous") }) {
                    Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = if (session.available) mode.gold else Faded)
                }
                IconButton(enabled = session.available, onClick = {
                    mediaAction(context, hasNotificationAccess, if (session.playing) "pause" else "play")
                    session = readSession(context, hasNotificationAccess)
                }) {
                    Icon(if (session.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "Reproduzir ou pausar", tint = if (session.available) mode.gold else Faded,
                        modifier = Modifier.size(31.dp))
                }
                IconButton(enabled = session.available, onClick = { mediaAction(context, hasNotificationAccess, "next") }) {
                    Icon(Icons.Rounded.SkipNext, "Próxima", tint = if (session.available) mode.gold else Faded)
                }
            }
            Button(onClick = onSpotify, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = mode.primary)) {
                Text("ABRIR SPOTIFY")
            }
            if (!hasNotificationAccess) {
                Text("Para ler e controlar sessões compatíveis, autorize o acesso às notificações.",
                    color = Faded, fontSize = 11.sp)
                Text("AUTORIZAR CONTROLE DE MÍDIA",
                    modifier = Modifier.clickable(onClick = onGrantMedia).padding(vertical = 10.dp),
                    color = mode.gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun NexusChatGptTab(
    mode: NexusMode,
    installed: Boolean,
    onOpenApp: () -> Unit,
    onOpenWeb: () -> Unit,
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    val quick = listOf(
        "Resolver uma questão" to "Explique passo a passo esta questão que vou enviar.",
        "Criar item arcano" to "Crie um item arcano inédito para meu universo de roleplay.",
        "Estudar engenharia" to "Monte um resumo organizado para revisar engenharia mecatrônica.",
        "Desenvolver aplicativo" to "Vamos planejar e implementar uma melhoria no meu aplicativo Android.",
    )
    val pulse by rememberInfiniteTransition(label = "portal-pulse").animateFloat(
        initialValue = .4f, targetValue = .95f,
        animationSpec = infiniteRepeatable(tween(2300), RepeatMode.Reverse), label = "pulse"
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(19.dp))
        NexusModuleHeading("CHATGPT / PORTAL", "NEXUS COGNITIVE ACCESS", mode)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(194.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(188.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(mode.primary.copy(alpha = .17f), radius = size.width * .45f, center = center)
                drawCircle(mode.gold.copy(alpha = pulse), radius = size.width * .42f, center = center,
                    style = Stroke(width = 3.dp.toPx()))
                drawCircle(mode.primary.copy(alpha = .7f), radius = size.width * .31f, center = center,
                    style = Stroke(width = 1.7.dp.toPx()))
                repeat(8) { i ->
                    val a = (Math.PI * i / 4).toFloat()
                    val r = size.width * .36f
                    drawLine(mode.gold.copy(alpha = .55f),
                        Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r),
                        Offset(center.x + kotlin.math.cos(a) * (r + 17f),
                            center.y + kotlin.math.sin(a) * (r + 17f)), 2f)
                }
            }
            Icon(Icons.Rounded.AutoAwesome, null, tint = mode.gold, modifier = Modifier.size(56.dp))
        }
        Text("PORTAL DE CONVERSAÇÃO", color = Frost, fontSize = 17.sp,
            fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("ChatGPT oficial, sem armazenar credenciais no Nexus.", color = Faded,
            fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))
        NexusModuleCard(mode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Chat, null, tint = mode.gold, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("CHATGPT", color = Frost, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(if (installed) "Aplicativo oficial detectado" else "Aplicativo não localizado",
                        color = Faded, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = onOpenApp, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = mode.primary),
            ) { Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(if (installed) "ABRIR CHATGPT" else "ACESSAR CHATGPT", fontWeight = FontWeight.Bold)
            }
            Text(
                "ABRIR PELO NAVEGADOR",
                color = mode.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .clickable(onClick = onOpenWeb).padding(vertical = 10.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        NexusModuleCaption("PREPARAR MENSAGEM", mode)
        Spacer(Modifier.height(9.dp))
        NexusModuleCard(mode) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Escreva o que deseja perguntar") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 6, shape = RoundedCornerShape(15.dp),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Mensagem para o ChatGPT", prompt))
                    onOpenApp()
                },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = mode.primary),
            ) {
                Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("COPIAR E ABRIR CHATGPT", fontWeight = FontWeight.Bold)
            }
            Text("O Nexus copia a mensagem para você colar no ChatGPT. Não envia texto automaticamente nem usa sua conta.",
                color = Faded, fontSize = 10.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(18.dp))
        NexusModuleCaption("ACESSOS RÁPIDOS", mode)
        Spacer(Modifier.height(9.dp))
        quick.forEach { (title, template) ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .background(DeepSpace.copy(alpha = .95f), RoundedCornerShape(13.dp))
                    .border(1.dp, mode.gold.copy(alpha = .29f), RoundedCornerShape(13.dp))
                    .clickable { prompt = template }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = mode.gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(title, color = Frost, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("USAR", color = mode.gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun NexusModuleCard(mode: NexusMode, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xF11B1420)),
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, mode.gold.copy(alpha = .29f)),
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = { content() }) }
}

@Composable
private fun NexusModuleHeading(title: String, subtitle: String, mode: NexusMode) {
    Text(title, color = Frost, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Text(subtitle, color = mode.gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
    Box(Modifier.width(62.dp).height(2.dp).background(mode.primary))
}

@Composable
private fun NexusModuleCaption(title: String, mode: NexusMode) {
    Text(title, color = mode.gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
}
