package com.port.nexuslauncher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A integração envia apenas a transcrição depois que o usuário ativa a IA.
 * A chave OpenAI não está presente no aplicativo. Apenas o token de acesso do usuário.
 */
data class NexusAiStep(val action: String, val target: String, val value: Int, val text: String) {
    val summary: String get() = when (action) {
        "OPEN_APP" -> "Abrir aplicativo: $target"
        "OPEN_CHATGPT" -> "Abrir ChatGPT"
        "OPEN_AUDIO" -> "Abrir Port Audio"
        "SET_MODE" -> "Ativar modo: $target"
        "SET_VOLUME" -> "Volume de mídia: $value%"
        "SET_TIMER" -> "Configurar temporizador: $value segundos"
        "SET_ALARM" -> "Configurar alarme: $target"
        "WEB_SEARCH" -> "Pesquisar: $text"
        "OPEN_MAPS" -> "Abrir mapas: $text"
        "CREATE_NOTE" -> "Adicionar nota: $text"
        "OPEN_NOTIFICATIONS" -> "Abrir notificações"
        "OPEN_ARCHIVE" -> "Abrir Archive"
        "OPEN_SETTINGS" -> "Abrir configurações do Android"
        "PLAY_MEDIA" -> "Reproduzir mídia"
        "PAUSE_MEDIA" -> "Pausar mídia"
        "NEXT_TRACK" -> "Próxima faixa"
        "PREVIOUS_TRACK" -> "Faixa anterior"
        "OPEN_WIFI" -> "Abrir ajustes de Wi-Fi"
        "OPEN_BLUETOOTH" -> "Abrir ajustes de Bluetooth"
        else -> "Não suportado: $text"
    }
}

data class NexusAiPlan(val reply: String, val steps: List<NexusAiStep>)

object NexusAiPlanner {
    const val ENABLED = "nexus_ai_v3_enabled"
    const val URL_KEY = "nexus_ai_v3_url"
    const val TOKEN_KEY = "nexus_ai_v3_token"

    private val ALLOWED = setOf(
        "OPEN_APP", "OPEN_CHATGPT", "OPEN_AUDIO", "SET_MODE", "SET_VOLUME",
        "SET_TIMER", "SET_ALARM", "WEB_SEARCH", "OPEN_MAPS", "CREATE_NOTE",
        "OPEN_NOTIFICATIONS", "OPEN_ARCHIVE", "OPEN_SETTINGS",
        "PLAY_MEDIA", "PAUSE_MEDIA", "NEXT_TRACK", "PREVIOUS_TRACK",
        "OPEN_WIFI", "OPEN_BLUETOOTH", "UNSUPPORTED"
    )

    private fun validateEndpoint(endpoint: String): String {
        val address = endpoint.trim().trimEnd('/')
        val parsed = URL(address)
        require(parsed.protocol.equals("https", true)) { "Use somente uma URL HTTPS." }
        require(parsed.host.isNotBlank() && parsed.userInfo == null && parsed.query == null &&
            parsed.ref == null && (parsed.path.isEmpty() || parsed.path == "/")) {
            "Use a URL HTTPS base do servidor, sem caminho ou parâmetros."
        }
        return address
    }

    fun isConfigured(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(ENABLED, false) &&
            prefs.getString(URL_KEY, "").orEmpty().isNotBlank() &&
            prefs.getString(TOKEN_KEY, "").orEmpty().isNotBlank()

    private fun parse(raw: String): NexusAiPlan {
        val obj = JSONObject(raw)
        val reply = obj.getString("reply").take(400)
        val array = obj.getJSONArray("steps")
        require(array.length() <= 6) { "Plano contém ações demais." }
        val steps = (0 until array.length()).map { index ->
            val step = array.getJSONObject(index)
            val action = step.getString("action")
            val target = step.getString("target")
            val text = step.getString("text")
            val value = step.getInt("value")
            require(action in ALLOWED && target.length <= 150 && text.length <= 400) {
                "Ação não autorizada pelo Nexus."
            }
            require(action != "SET_VOLUME" || value in 0..100) { "Volume inválido." }
            require(action != "SET_TIMER" || value in 1..86400) { "Temporizador inválido." }
            require(action != "SET_ALARM" || Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(target)) {
                "Alarme inválido."
            }
            NexusAiStep(action, target, value, text)
        }
        return NexusAiPlan(reply, steps)
    }

    suspend fun request(utterance: String, endpoint: String, token: String): NexusAiPlan =
        withContext(Dispatchers.IO) {
            require(utterance.isNotBlank() && utterance.length <= 1200) {
                "O pedido deve ter de 1 a 1200 caracteres."
            }
            require(token.length in 12..256) { "Token de acesso inválido ou não configurado." }
            val base = validateEndpoint(endpoint)
            val connection = (URL("$base/v1/plan").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 28_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                val bytes = JSONObject().put("utterance", utterance).toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { it.write(bytes) }
                val status = connection.responseCode
                if (status != 200) {
                    val detailedError = runCatching {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText().take(4000) }
                        errorBody?.let { JSONObject(it).optString("error").take(250) }
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                    throw IllegalStateException(detailedError ?: when (status) {
                        401 -> "Token pessoal do Nexus incorreto."
                        402 -> "Verifique os créditos ou limites de gastos da API OpenAI."
                        424 -> "A chave da OpenAI ou o modelo configurado foi recusado."
                        429 -> "Limite temporário de solicitações da IA."
                        503 -> "Servidor da IA ainda não configurado com a chave da API."
                        else -> "Falha ao consultar IA (HTTP $status)."
                    })
                }
                val body = connection.inputStream.bufferedReader().use { it.readText().take(30_000) }
                parse(body)
            } finally { connection.disconnect() }
        }

    /**
     * Só invocar a partir do botão de CONFIRMAR. Nenhum passo executa código arbitrário.
     * Os limites adicionais de cada comando continuam impostos pelo roteador local.
     */
    fun execute(
        plan: NexusAiPlan,
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
    ): String {
        val outputs = mutableListOf<String>()
        for (step in plan.steps.take(6)) {
            val voice = when (step.action) {
                "OPEN_APP" -> "abrir ${step.target}"
                "SET_MODE" -> "modo ${step.target}"
                "SET_VOLUME" -> "volume em ${step.value}%"
                "SET_TIMER" -> "temporizador de ${step.value} segundos"
                "SET_ALARM" -> "alarme as ${step.target}"
                "WEB_SEARCH" -> "pesquisar na web ${step.text}"
                "OPEN_MAPS" -> "navegar para ${step.text}"
                "CREATE_NOTE" -> "anote ${step.text}"
                "PLAY_MEDIA" -> "tocar musica"
                "PAUSE_MEDIA" -> "pausar musica"
                "NEXT_TRACK" -> "proxima faixa"
                "PREVIOUS_TRACK" -> "faixa anterior"
                else -> null
            }
            val reply = when (step.action) {
                "OPEN_CHATGPT" -> { onChatGpt(); "Abrindo ChatGPT." }
                "OPEN_AUDIO" -> { onAudio(); "Abrindo Port Audio." }
                "OPEN_NOTIFICATIONS" -> { onNotifications(); "Abrindo notificações." }
                "OPEN_ARCHIVE" -> { onArchive(); "Abrindo Archive." }
                "OPEN_SETTINGS" -> { onSystemAction(NexusAction.ANDROID_SETTINGS); "Abrindo configurações." }
                "OPEN_WIFI" -> { onSystemAction(NexusAction.WIFI_SETTINGS); "Abrindo ajustes de Wi-Fi." }
                "OPEN_BLUETOOTH" -> { onSystemAction(NexusAction.BLUETOOTH_SETTINGS); "Abrindo ajustes de Bluetooth." }
                "UNSUPPORTED" -> "Não executado: ${step.text.ifBlank { "ação não suportada" }}"
                else -> {
                    if (voice == null) "Ação não permitida."
                    else NexusCommandEngine.run(
                        phrase = voice, context = context, apps = apps, prefs = prefs,
                        onMode = onMode, onSystemAction = onSystemAction,
                        onLaunchApp = onLaunchApp, onAudio = onAudio,
                        onChatGpt = onChatGpt, onApps = onApps,
                        onNotifications = onNotifications, onArchive = onArchive,
                    ).reply
                }
            }
            outputs.add(reply)
        }
        return if (outputs.isEmpty()) "Nenhuma ação para executar." else outputs.joinToString(" ")
    }
}
