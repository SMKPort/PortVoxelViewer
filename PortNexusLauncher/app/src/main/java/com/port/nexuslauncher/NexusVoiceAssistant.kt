package com.port.nexuslauncher

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VoiceText = Color(0xFFFFF5ED)
private val VoiceMuted = Color(0xFFC8B7C4)
private val VoiceLocale = Locale("pt", "BR")

private fun normalizeVoice(input: String): String =
    Normalizer.normalize(input.lowercase(VoiceLocale), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ").trim()

@Composable
fun NexusVoicePanel(
    mode: NexusMode,
    apps: List<LauncherApp>,
    prefs: SharedPreferences,
    onClose: () -> Unit,
    onMode: (NexusMode) -> Unit,
    onSystemAction: (NexusAction) -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onAudio: () -> Unit,
    onChatGpt: () -> Unit,
    onApps: (String) -> Unit,
    onNotifications: () -> Unit,
    onArchive: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Nexus em espera. Toque no microfone para falar.") }
    var lastCommand by remember { mutableStateOf("") }
    var suggestedApps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var typed by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var speechEnabled by remember { mutableStateOf(true) }
    var ttsReady by remember { mutableStateOf(false) }

    val engine = remember(context) {
        TextToSpeech(context) { result -> ttsReady = result == TextToSpeech.SUCCESS }
    }
    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            engine.language = VoiceLocale
            engine.setSpeechRate(.95f)
            engine.setPitch(.91f)
        }
    }
    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context)
        else null
    }

    val handleCommand: (String) -> Unit = { raw ->
        lastCommand = raw
        val result = NexusCommandEngine.run(
            phrase = raw, context = context, apps = apps, prefs = prefs,
            onMode = onMode, onSystemAction = onSystemAction,
            onLaunchApp = onLaunchApp, onAudio = onAudio, onChatGpt = onChatGpt,
            onApps = onApps, onNotifications = onNotifications, onArchive = onArchive,
        )
        status = result.reply
        suggestedApps = result.suggestions
        if (speechEnabled && ttsReady) {
            engine.speak(result.reply, TextToSpeech.QUEUE_FLUSH, null, "nexus-voice-answer")
        }
    }
    val currentHandleCommand by rememberUpdatedState(handleCommand)

    DisposableEffect(recognizer, engine) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                status = "Escutando. Pode falar."
            }
            override fun onBeginningOfSpeech() {
                status = "Reconhecendo sua voz..."
            }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                listening = false
                status = "Processando comando..."
            }
            override fun onError(error: Int) {
                listening = false
                status = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "Não consegui entender. Toque novamente no microfone."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        "O Android não autorizou o microfone."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "O serviço de reconhecimento está indisponível. Verifique a conexão."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        "O reconhecimento está ocupado. Tente novamente."
                    else -> "Não consegui escutar. Tente novamente (código $error)."
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (phrase.isBlank()) status = "Não ouvi nenhum comando."
                else currentHandleCommand(phrase)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (partial.isNotEmpty()) lastCommand = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
            engine.stop()
            engine.shutdown()
        }
    }

    val beginListening: () -> Unit = {
        if (recognizer == null) {
            status = "Este Android não disponibiliza um reconhecedor de voz."
        } else {
            try {
                engine.stop()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                listening = true
                status = "Preparando o microfone..."
                recognizer.startListening(intent)
            } catch (_: Exception) {
                listening = false
                status = "Falha ao iniciar o microfone."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginListening()
        else status = "Sem permissão de microfone. O modo de texto continua disponível."
    }

    val runMicrophone: () -> Unit = {
        if (listening) {
            recognizer?.stopListening()
            listening = false
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) {
            beginListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val beat by rememberInfiniteTransition(label = "nexus-voice-pulse").animateFloat(
        initialValue = .60f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "nexus-voice-pulse-factor",
    )

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF270D1A), Color(0xFF0A0810), Color(0xFF0B070E))))
            .verticalScroll(rememberScrollState()).padding(horizontal = 19.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("N E X U S / V O I C E", color = mode.gold, fontSize = 11.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("ASSISTENTE DE VOZ", color = VoiceText, fontSize = 19.sp, fontWeight = FontWeight.Light)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, "Fechar", tint = mode.gold)
            }
        }
        Spacer(Modifier.height(15.dp))
        Box(Modifier.size(234.dp).clickable(onClick = runMicrophone), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * .5f, size.height * .5f)
                val radius = size.minDimension * .45f
                drawCircle(mode.primary.copy(alpha = if (listening) .21f else .09f), radius * beat, center)
                drawCircle(mode.gold.copy(alpha = if (listening) 1f else .45f),
                    radius = radius * beat, center = center,
                    style = Stroke(width = 3.dp.toPx()))
                drawCircle(mode.primary.copy(alpha = if (listening) .9f else .4f),
                    radius = radius * .73f, center = center, style = Stroke(2.dp.toPx()))
                repeat(12) { step ->
                    val theta = Math.toRadians(step * 30.0)
                    val x = kotlin.math.cos(theta).toFloat()
                    val y = kotlin.math.sin(theta).toFloat()
                    drawLine(mode.gold.copy(alpha = .6f),
                        Offset(center.x + x * radius * .82f, center.y + y * radius * .82f),
                        Offset(center.x + x * radius * .94f, center.y + y * radius * .94f),
                        strokeWidth = 2.dp.toPx())
                }
            }
            Box(
                Modifier.size(111.dp).background(mode.primary.copy(alpha = .22f), CircleShape)
                    .border(1.dp, mode.gold, CircleShape), contentAlignment = Alignment.Center,
            ) {
                Icon(if (listening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    contentDescription = if (listening) "Parar reconhecimento" else "Começar a ouvir",
                    tint = mode.gold, modifier = Modifier.size(55.dp))
            }
        }
        Text(if (listening) "ESCUTANDO" else "AGUARDANDO COMANDO", color = mode.gold,
            fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(9.dp))
        Text(status, color = VoiceText, fontSize = 15.sp, textAlign = TextAlign.Center,
            lineHeight = 22.sp)
        if (lastCommand.isNotBlank()) {
            Spacer(Modifier.height(11.dp))
            Text("VOCÊ: $lastCommand", color = VoiceMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
        if (suggestedApps.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("QUAL APLICATIVO?", color = mode.gold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            suggestedApps.forEach { option ->
                Row(Modifier.fillMaxWidth().padding(top = 5.dp)
                    .background(Color(0xFF241724), RoundedCornerShape(10.dp))
                    .border(1.dp, mode.gold.copy(alpha = .30f), RoundedCornerShape(10.dp))
                    .clickable {
                        suggestedApps = emptyList()
                        status = "Abrindo ${option.label}."
                        onLaunchApp(option)
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = mode.gold, fontSize = 17.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(option.label, color = VoiceText, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = runMicrophone,
            colors = ButtonDefaults.buttonColors(containerColor = mode.primary),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Mic, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (listening) "FINALIZAR ESCUTA" else "FALAR COM O NEXUS", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(11.dp))
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF241724), RoundedCornerShape(13.dp))
                .border(1.dp, mode.gold.copy(alpha = .28f), RoundedCornerShape(13.dp))
                .clickable { speechEnabled = !speechEnabled }.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.VolumeUp, null, tint = mode.gold)
            Spacer(Modifier.width(9.dp))
            Text("RESPOSTA FALADA", color = VoiceText, modifier = Modifier.weight(1f), fontSize = 12.sp)
            Text(if (speechEnabled) "LIGADA" else "DESLIGADA", color = mode.gold,
                fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("COMANDO POR TEXTO", color = mode.gold, fontWeight = FontWeight.Black,
            fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(value = typed, onValueChange = { typed = it },
            placeholder = { Text("Nexus, abrir Spotify", color = VoiceMuted) },
            modifier = Modifier.fillMaxWidth(), maxLines = 2, singleLine = false,
            shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(7.dp))
        Button(
            onClick = { currentHandleCommand(typed) },
            enabled = typed.isNotBlank(), modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = mode.primary.copy(alpha = .6f)),
        ) {
            Icon(Icons.Rounded.Send, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("EXECUTAR TEXTO")
        }
        Spacer(Modifier.height(17.dp))
        Text("EXPERIMENTE DIZER", color = mode.gold, fontSize = 11.sp,
            fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        listOf("Nexus, abre o Minecraft", "Nexus, iniciar calculadora",
            "Nexus, volume em 50%", "Nexus, alarme às 07:30",
            "Nexus, temporizador de 10 minutos", "Nexus, pesquisar na web sobre eletrônica",
            "Nexus, chame Minecraft de meu jogo", "Nexus, pausa a música",
            "Nexus, modo Angelical", "Nexus, ajuda").forEach { example ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    .background(Color(0xFF1C131F), RoundedCornerShape(10.dp))
                    .border(1.dp, mode.gold.copy(alpha = .21f), RoundedCornerShape(10.dp))
                    .clickable { currentHandleCommand(example) }.padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✦", color = mode.gold, fontSize = 18.sp)
                Spacer(Modifier.width(9.dp))
                Text(example, color = VoiceText, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        Text("O Nexus pode abrir todos os aplicativos que aparecem na gaveta do launcher. " +
            "Você pode ensinar apelidos por voz e usar comandos do Android disponíveis sem root. " +
            "O reconhecimento pode usar processamento em nuvem do serviço de voz do aparelho. " +
            "Não são feitos comandos irrestritos, nem escuta contínua com a tela apagada.",
            color = VoiceMuted, fontSize = 10.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(34.dp))
    }
}
