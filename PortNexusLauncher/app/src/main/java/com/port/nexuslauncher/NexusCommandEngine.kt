package com.port.nexuslauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/**
 * Motor de comandos determinístico. Não executa shell, root nem automações em outros apps
 * sem uma integração declarada. Ações externas usam as interfaces oficiais do Android.
 */
data class NexusCommandResult(
    val reply: String,
    val suggestions: List<LauncherApp> = emptyList(),
)

object NexusCommandEngine {
    private val locale = Locale("pt", "BR")
    private const val ALIAS_KEY = "nexus_voice_aliases_v24"

    private fun norm(source: String): String = Normalizer.normalize(source.lowercase(locale), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9.+% ]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private fun concise(source: String): String = norm(source)
        .replace(Regex("\\b(por favor|pra mim|para mim|agora|ai|aqui)\\b"), "")
        .replace(Regex("\\s+"), " ").trim()

    private fun cleanAppName(source: String): String = concise(source)
        .replace(Regex("^(o|a|um|uma|no|na|do|da|app|aplicativo|aplicacao|programa|jogo)\\s+"), "")
        .replace(Regex("^(app|aplicativo|aplicacao|programa|jogo)\\s+"), "").trim()

    private fun abbreviations(app: LauncherApp): List<String> {
        val label = norm(app.label)
        val packagePieces = app.packageName.split('.').filter { it.length > 3 }
        return (listOf(label) + packagePieces).distinct()
    }

    private fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        a.indices.forEach { i ->
            cur[0] = i + 1
            b.indices.forEach { j ->
                cur[j + 1] = minOf(cur[j] + 1, prev[j + 1] + 1, prev[j] + if (a[i] == b[j]) 0 else 1)
            }
            val temp = prev
            prev = cur
            cur = temp
        }
        return prev[b.length]
    }

    private fun aliases(prefs: SharedPreferences): JSONObject =
        runCatching { JSONObject(prefs.getString(ALIAS_KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())

    fun aliasCount(prefs: SharedPreferences): Int = aliases(prefs).length()

    private fun rankedApps(query: String, apps: List<LauncherApp>, prefs: SharedPreferences): List<Pair<LauncherApp, Int>> {
        val q = cleanAppName(query)
        if (q.isBlank()) return emptyList()
        val aliased = aliases(prefs).optString(q, "")
        if (aliased.isNotBlank()) {
            apps.firstOrNull { it.component.flattenToString() == aliased }?.let { return listOf(it to 120) }
        }
        return apps.mapNotNull { app ->
            val label = norm(app.label)
            val tokens = label.split(' ')
            val queryTokens = q.split(' ')
            val score = when {
                label == q -> 100
                label.replace(" ", "") == q.replace(" ", "") -> 98
                tokens.any { it == q } -> 94
                label.startsWith("$q ") || label.startsWith(q) -> 90
                label.contains(" $q") -> 86
                label.contains(q) -> 82
                queryTokens.size > 1 && queryTokens.all { label.contains(it) } -> 79
                abbreviations(app).drop(1).any { it == q } -> 76
                tokens.any { q.length >= 4 && it.startsWith(q) } -> 70
                q.length >= 4 && distance(label, q) <= max(1, q.length / 5) -> 66
                else -> 0
            }
            if (score < 66) null else app to score
        }.sortedWith(compareByDescending<Pair<LauncherApp, Int>> { it.second }.thenBy { it.first.label.length })
    }

    private fun appResult(
        query: String, apps: List<LauncherApp>, prefs: SharedPreferences,
        onLaunchApp: (LauncherApp) -> Unit,
    ): NexusCommandResult {
        val matches = rankedApps(query, apps, prefs)
        if (matches.isEmpty()) return NexusCommandResult(
            "Não encontrei "$query" entre os aplicativos acessíveis pelo launcher. Atualize a lista ou confira o nome."
        )
        val top = matches[0]
        val ties = matches.filter { it.second >= 80 && top.second - it.second <= 3 }
        if (ties.size > 1) return NexusCommandResult(
            "Encontrei mais de um aplicativo parecido. Escolha o desejado:",
            ties.take(5).map { it.first },
        )
        onLaunchApp(top.first)
        return NexusCommandResult("Abrindo ${top.first.label}.")
    }

    private fun start(context: Context, intent: Intent, success: String): NexusCommandResult {
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            NexusCommandResult(success)
        }.getOrElse { NexusCommandResult("O Android não encontrou um aplicativo compatível para essa ação.") }
    }

    private fun useMedia(context: Context, action: String): NexusCommandResult {
        if (!NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
            return NexusCommandResult("Para controlar música, autorize o acesso às notificações no Nexus Signal.")
        }
        return runCatching {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(context, NexusNotificationListener::class.java)
            val sessions = manager.getActiveSessions(component)
            val controller = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
                ?: return NexusCommandResult("Não encontrei nenhuma sessão de mídia ativa.")
            when (action) {
                "pause" -> controller.transportControls.pause()
                "play" -> controller.transportControls.play()
                "next" -> controller.transportControls.skipToNext()
                "previous" -> controller.transportControls.skipToPrevious()
            }
            NexusCommandResult(when (action) {
                "pause" -> "Enviei o comando para pausar a música."
                "play" -> "Enviei o comando para reproduzir a música."
                "next" -> "Avançando para a próxima faixa."
                else -> "Voltando para a faixa anterior."
            })
        }.getOrElse { NexusCommandResult("O player não permitiu controlar esta sessão.") }
    }

    private fun setVolume(context: Context, command: String): NexusCommandResult {
        return runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxValue = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val percent = Regex("(\\d{1,3})\\s*%").find(command)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("volume (?:em|para|no) (\\d{1,3})").find(command)?.groupValues?.get(1)?.toIntOrNull()
            when {
                percent != null -> {
                    if (percent !in 0..100) return NexusCommandResult("Escolha um volume de zero a cem por cento.")
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, ((maxValue * percent) / 100f).toInt(), 0)
                    NexusCommandResult("Volume de mídia ajustado para aproximadamente $percent por cento.")
                }
                command.contains("mudo") || command.contains("silencio") || command.contains("mutar") ||
                    command.contains("silenciar") -> {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    NexusCommandResult("Som de mídia silenciado.")
                }
                else -> {
                    val raising = listOf("aumentar", "aumente", "mais volume", "subir").any(command::contains)
                    audio.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        if (raising) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0
                    )
                    NexusCommandResult(if (raising) "Aumentei o volume de mídia." else "Diminuí o volume de mídia.")
                }
            }
        }.getOrElse { NexusCommandResult("Não consegui alterar o volume de mídia.") }
    }

    private fun alarm(context: Context, command: String): NexusCommandResult {
        val pattern = Regex("(\\d{1,2})\\s*(?:h|:|horas?)\\s*(\\d{0,2})")
        val match = pattern.find(command)
        val hour = match?.groupValues?.get(1)?.toIntOrNull()
        val minute = match?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        if (hour != null && hour in 0..23 && minute in 0..59) {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minute)
                .putExtra(AlarmClock.EXTRA_MESSAGE, "Nexus Voice")
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            return start(context, intent, "Abrindo a configuração de alarme para %02d:%02d.".format(hour, minute))
        }
        return start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS), "Abrindo seus alarmes.")
    }

    private fun timer(context: Context, command: String): NexusCommandResult {
        val match = Regex("(\\d{1,3})\\s*(horas?|minutos?|min|segundos?|seg)").find(command)
            ?: return NexusCommandResult("Diga a duração. Por exemplo: temporizador de dez minutos, ou de 10 minutos.")
        val count = match.groupValues[1].toIntOrNull() ?: 0
        val unit = match.groupValues[2]
        val seconds = count * when {
            unit.startsWith("hora") -> 3600
            unit.startsWith("seg") -> 1
            else -> 60
        }
        if (seconds !in 1..86400) return NexusCommandResult("A duração deve estar entre um segundo e vinte e quatro horas.")
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "Nexus Voice")
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        return start(context, intent, "Abrindo temporizador de $count $unit.")
    }

    private fun parseOpenTarget(command: String): String? {
        val prefixes = listOf(
            "quero que voce abra ", "eu quero abrir ", "voce pode abrir ", "pode abrir ",
            "abra o aplicativo ", "abre o aplicativo ", "abrir o aplicativo ",
            "abra o app ", "abre o app ", "abrir o app ",
            "abre ", "abra ", "abrir ", "inicia ", "iniciar ", "execute ",
            "executar ", "acessa ", "acessar ", "entra no ", "entrar no ",
            "ir para ", "vai para ", "mostrar ", "mostra ", "quero usar ", "use "
        )
        return prefixes.firstOrNull(command::startsWith)?.let { cleanAppName(command.removePrefix(it)) }
    }

    fun run(
        phrase: String,
        context: Context,
        apps: List<LauncherApp>,
        prefs: SharedPreferences,
        onMode: (NexusMode) -> Unit,
        onSystemAction: (NexusAction) -> Unit,
        onLaunchApp: (LauncherApp) -> Unit,
        onAudio: () -> Unit,
        onChatGpt: () -> Unit,
        onApps: (String) -> Unit,
        onNotifications: () -> Unit,
        onArchive: () -> Unit,
    ): NexusCommandResult {
        var command = concise(phrase)
        for (prefix in listOf("ei nexus ", "ok nexus ", "ola nexus ", "hey nexus ", "nexus ")) {
            if (command.startsWith(prefix)) { command = command.removePrefix(prefix).trim(); break }
        }
        if (command.isBlank() || command == "nexus") return NexusCommandResult("Estou ouvindo. Qual é o comando?")

        // Apelidos são locais e vinculados a um aplicativo instalado.
        val teach = Regex("^(?:chame|chama|apelide) (?:o |a |app |aplicativo )?(.+?) (?:de|como) (.+)$").find(command)
        if (teach != null) {
            val target = cleanAppName(teach.groupValues[1])
            val alias = cleanAppName(teach.groupValues[2])
            if (alias.length !in 2..32) return NexusCommandResult("O apelido deve ter de 2 a 32 caracteres.")
            val matches = rankedApps(target, apps, prefs)
            if (matches.isEmpty()) return NexusCommandResult("Não achei o aplicativo $target para ensinar o apelido.")
            if (matches.size > 1 && matches[0].second == matches[1].second)
                return NexusCommandResult("Encontrei mais de um aplicativo. Diga o nome completo.", matches.take(5).map { it.first })
            val map = aliases(prefs)
            map.put(alias, matches[0].first.component.flattenToString())
            prefs.edit().putString(ALIAS_KEY, map.toString()).apply()
            return NexusCommandResult("Entendido. Vou chamar ${matches[0].first.label} de $alias.")
        }
        if (command.startsWith("esqueca o apelido ") || command.startsWith("remova o apelido ")) {
            val alias = cleanAppName(command.substringAfter("apelido "))
            val map = aliases(prefs)
            if (!map.has(alias)) return NexusCommandResult("Não encontrei esse apelido.")
            map.remove(alias)
            prefs.edit().putString(ALIAS_KEY, map.toString()).apply()
            return NexusCommandResult("Apelido removido.")
        }
        if (command == "ajuda" || command.contains("o que voce consegue fazer") || command == "comandos") {
            return NexusCommandResult("Posso abrir aplicativos, usar apelidos, trocar modos, controlar mídia, configurar alarmes e timers, abrir mapas, pesquisar na web e anotar lembretes. Ações protegidas ainda exigem sua autorização.")
        }
        if (command.contains("que horas") || command == "horas" || command == "hora") {
            return NexusCommandResult("Agora são ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}.")
        }
        if (command.contains("que dia e") || command == "data de hoje" || command == "qual a data") {
            return NexusCommandResult("Hoje é ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}.")
        }
        if (command.contains("bateria") && (command.contains("quanto") || command.contains("nivel") ||
                command.contains("porcentagem") || command.contains("status"))) {
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return NexusCommandResult(if (percent in 0..100) "A bateria está em $percent por cento."
                else "Não consegui ler a bateria.")
        }

        if (command.startsWith("modo ") || command.startsWith("ativar modo ") ||
            command.startsWith("ativa modo ") || command.startsWith("mudar para modo ") ||
            command.startsWith("protocolo ")) {
            val candidate = NexusMode.entries.firstOrNull { command.contains(norm(it.title)) || command.contains(norm(it.name)) }
            return if (candidate != null) {
                onMode(candidate)
                NexusCommandResult("Protocolo ${candidate.title} ativado.")
            } else NexusCommandResult("Não reconheci esse modo. Experimente Arcano, Música, Estudo ou Angelical.")
        }

        if (command.contains("temporizador") || command.contains("cronometro") ||
            command.startsWith("timer ") || command.contains("contagem regressiva")) {
            return timer(context, command)
        }
        if (command.contains("alarme") || command.contains("despertador") ||
            command.startsWith("me acorde ")) return alarm(context, command)

        if (command.startsWith("anote ") || command.startsWith("anotar ") ||
            command.startsWith("guarde nota ") || command.startsWith("criar nota ")) {
            val note = command.removePrefix("anote ").removePrefix("anotar ")
                .removePrefix("guarde nota ").removePrefix("criar nota ").trim()
            if (note.length < 3) return NexusCommandResult("Diga o que deseja anotar.")
            val before = prefs.getString("v2_notes", "").orEmpty()
            prefs.edit().putString("v2_notes", (before + if (before.isEmpty()) "" else "\n" + note)
                .takeLast(20000)).apply()
            onArchive()
            return NexusCommandResult("Anotado no Archive.")
        }

        if (command.contains("volume") || command == "silenciar" || command.contains("silenciar audio") ||
            command.contains("som no mudo")) return setVolume(context, command)

        if (listOf("pausar musica", "pause a musica", "pausa musica", "parar musica", "pause musica")
                .any(command::contains)) return useMedia(context, "pause")
        if (listOf("tocar musica", "retomar musica", "continuar musica", "reproduzir musica")
                .any(command::contains)) return useMedia(context, "play")
        if (listOf("proxima musica", "proxima faixa", "pular musica", "avancar musica")
                .any(command::contains)) return useMedia(context, "next")
        if (listOf("musica anterior", "faixa anterior", "voltar musica")
                .any(command::contains)) return useMedia(context, "previous")

        if (command.contains("notificac") || command == "avisos" ||
            command == "mensagens recebidas") {
            onNotifications()
            return NexusCommandResult("Abrindo o Nexus Signal.")
        }
        if (command.contains("equalizador") || command.contains("audio booster") ||
            command.contains("estudio de audio") || command == "audio") {
            onAudio()
            return NexusCommandResult("Abrindo o Port Audio.")
        }

        // Ações de baixo risco, feitas pelo aplicativo apropriado, sem enviar mensagens sozinho.
        val searchWeb = Regex("^(?:pesquisar na internet|buscar na internet|pesquisar na web|procurar na web|pesquisar no google|pesquise|busque)\\s+(.+)$")
            .find(command)?.groupValues?.get(1)
        if (!searchWeb.isNullOrBlank()) {
            return start(context,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(searchWeb))),
                "Abrindo a pesquisa na web por $searchWeb.")
        }

        val directions = listOf("navegar para ", "rota para ", "como chegar em ", "como chegar a ",
            "me leve para ", "ir ate ", "direcoes para ").firstOrNull(command::startsWith)
        if (directions != null) {
            val place = command.removePrefix(directions).trim()
            return start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(place))),
                "Abrindo navegação para $place.")
        }

        if (command.startsWith("ligar para ") || command.startsWith("discar ")) {
            val phone = command.removePrefix("ligar para ").removePrefix("discar ").filter { it.isDigit() || it == '+' }
            if (phone.length < 3) return NexusCommandResult("Para discar, diga um número de telefone.")
            return start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))),
                "Abrindo o discador. Confirme a ligação pelo telefone.")
        }
        if (command.startsWith("enviar sms para ") || command.startsWith("mandar sms para ")) {
            val number = command.substringAfter("para ").filter { it.isDigit() || it == '+' }
            if (number.length < 3) return NexusCommandResult("Diga o número para escrever o SMS.")
            return start(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))),
                "Abrindo uma mensagem para revisão. Nada foi enviado.")
        }
        if (command.startsWith("enviar email para ") || command.startsWith("escrever email para ")) {
            val mail = command.substringAfter("para ").trim().replace(" arroba ", "@").replace(" ponto ", ".")
            if (!mail.contains("@")) return NexusCommandResult("Diga ou digite um e-mail válido.")
            return start(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(mail))),
                "Abrindo um e-mail para revisão. Nada foi enviado.")
        }

        if (command.contains("wi fi") || command.contains("wifi") || command.contains("internet do celular")) {
            onSystemAction(NexusAction.WIFI_SETTINGS)
            return NexusCommandResult("Abrindo as opções de Wi-Fi. O Android controla a conexão.")
        }
        if (command.contains("bluetooth")) {
            onSystemAction(NexusAction.BLUETOOTH_SETTINGS)
            return NexusCommandResult("Abrindo as opções de Bluetooth.")
        }
        if (command.contains("abrir camera") || command == "camera" ||
            command.contains("tirar uma foto")) {
            return start(context, Intent("android.media.action.STILL_IMAGE_CAMERA"), "Abrindo a câmera.")
        }
        if (command.contains("chatgpt") || command.contains("chat gpt")) {
            // Preferência para o aplicativo oficial caso ele apareça na gaveta.
            val match = rankedApps("chatgpt", apps, prefs).firstOrNull()
            if (match != null) {
                onLaunchApp(match.first)
                return NexusCommandResult("Abrindo ${match.first.label}.")
            }
            onChatGpt()
            return NexusCommandResult("Abrindo o portal ChatGPT.")
        }
        if (command.contains("configurac") || command == "ajustes" || command == "sistema") {
            onSystemAction(NexusAction.ANDROID_SETTINGS)
            return NexusCommandResult("Abrindo as configurações do Android.")
        }
        if (command.contains("lanterna") || command.contains("ativar modo aviao") ||
            command.contains("desativar modo aviao") || command.contains("desligar celular") ||
            command.contains("reiniciar celular") || command.contains("root")) {
            return NexusCommandResult("Essa ação exige permissão ou integração adicional. O Nexus não altera funções protegidas sem autorização.")
        }
        if (command.startsWith("buscar aplicativo ") || command.startsWith("procurar aplicativo ")) {
            val q = command.substringAfter("aplicativo ").trim()
            onApps(q)
            return NexusCommandResult("Abrindo a pesquisa de aplicativos por $q.")
        }

        // Universal app recognition for all LAUNCHER activities visible to the Nexus.
        val target = parseOpenTarget(command)
        if (target != null && target.isNotBlank()) {
            return appResult(target, apps, prefs, onLaunchApp)
        }
        // An app name spoken by itself also works.
        val matches = rankedApps(command, apps, prefs)
        if (matches.isNotEmpty() && matches[0].second >= 80) {
            return appResult(command, apps, prefs, onLaunchApp)
        }

        return NexusCommandResult("Ainda não consigo executar esse pedido pelo Android sem integração. Diga 'Nexus, ajuda' para ver os comandos disponíveis, ou ensine um apelido para um aplicativo.")
    }
}
