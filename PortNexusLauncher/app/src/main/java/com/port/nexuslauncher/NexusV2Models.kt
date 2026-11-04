package com.port.nexuslauncher

import androidx.compose.ui.graphics.Color

/** Tema de interface e perfil de uso local. */
enum class NexusMode(val title: String, val caption: String, val primary: Color, val gold: Color, val glyph: String) {
    ARCANO("Arcano", "PROTOCOLO ARCANO", Color(0xFFEB4159), Color(0xFFECC17F), "✦"),
    INFERNAL("Infernal", "NÚCLEO INFERNAL", Color(0xFFFF573E), Color(0xFFFFA774), "✶"),
    ANGELICAL("Angelical", "COROA CELESTIAL", Color(0xFFE7BA73), Color(0xFFFFF0C9), "✧"),
    INDUSTRIAL("Industrial", "ENGENHARIA ATIVA", Color(0xFFFF6844), Color(0xFFB5CDD5), "⌬"),
    MUSICA("Música", "ESTÚDIO NEXUS", Color(0xFFEF3A7C), Color(0xFFFFB98B), "♫"),
    ESTUDO("Estudo", "MENTE ANALÍTICA", Color(0xFF7B9CF4), Color(0xFFEACB9B), "∑"),
    PERFORMANCE("Performance", "SISTEMA DE POTÊNCIA", Color(0xFF66D8C2), Color(0xFFEBC482), "⚡")
}

enum class DrawerView(val title: String) {
    GRADE("Grade"), ARSENAL("Arsenal"), INTELIGENTE("Nexus AI")
}

enum class AppGroup(val title: String, val glyph: String) {
    COMUNICACAO("Comunicação", "◉"),
    MUSICA("Música e mídia", "♫"),
    ESTUDO("Estudo", "∑"),
    CRIACAO("Criação", "✦"),
    JOGOS("Jogos", "✕"),
    FERRAMENTAS("Ferramentas", "⌘"),
    SISTEMA("Sistema", "⚙"),
    OUTROS("Outros", "◇")
}

/** Classificação heurística e local, editável em versões futuras. */
fun classifyApp(app: LauncherApp): AppGroup {
    val text = "${app.label} ${app.packageName}".lowercase()
    return when {
        listOf("whatsapp", "telegram", "discord", "signal", "messag", "telefone", "phone", "dialer", "contacts", "mensag", "sms", "gmail", "outlook").any(text::contains) -> AppGroup.COMUNICACAO
        listOf("spotify", "music", "música", "audio", "youtube", "netflix", "primevideo", "vlc", "tiktok", "deezer", "video", "rádio").any(text::contains) -> AppGroup.MUSICA
        listOf("calculator", "calculadora", "matem", "classroom", "notion", "docs", "sheets", "drive", "pdf", "study", "estudo", "algebra", "caldo", "circuit").any(text::contains) -> AppGroup.ESTUDO
        listOf("camera", "câmera", "photo", "photos", "editor", "capcut", "blender", "blockbench", "canva", "voxel", "paint").any(text::contains) -> AppGroup.CRIACAO
        listOf("minecraft", "roblox", "game", "jogo", "steam", "epicgames", "supercell", "mojang", "playgames").any(text::contains) -> AppGroup.JOGOS
        listOf("settings", "configur", "system", "launcher", "android", "security", "segurança", "arquivos", "files", "packageinstaller").any(text::contains) -> AppGroup.SISTEMA
        listOf("clock", "relógio", "alarm", "calendar", "calendário", "maps", "mapas", "browser", "chrome", "firefox", "navegador", "scanner", "tradutor", "translate").any(text::contains) -> AppGroup.FERRAMENTAS
        else -> AppGroup.OUTROS
    }
}
