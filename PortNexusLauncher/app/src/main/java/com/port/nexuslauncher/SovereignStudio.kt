package com.port.nexuslauncher

import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val muted = Color(0xFFC4B7C5)

@Composable
fun SovereignStudio(mode: NexusMode, prefs: SharedPreferences, apps: List<LauncherApp>, onLaunch: (LauncherApp) -> Unit,
    onClose: () -> Unit, onRestored: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf("Coleções") }
    var revision by remember { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF0D0911)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NEXUS / SOVEREIGN", color = mode.gold, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text("Seu universo.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                TextButton(onClick = onClose) { Text("Voltar") }
            }
            Text("Organize a rotina. Assine cada detalhe.", color = muted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Coleções", "Missões", "Foco", "Identidade", "Backup").forEach {
                    FilterPillV2(it, tab == it, mode) { tab = it }
                }
            }
            key(revision) {
                when (tab) {
                    "Coleções" -> CollectionStudio(mode, prefs, apps, onLaunch)
                    "Missões" -> MissionStudio(mode, prefs)
                    "Foco" -> FocusStudio(mode, prefs)
                    "Identidade" -> IdentityStudio(mode, prefs)
                    "Backup" -> BackupStudio(mode, prefs) { revision++; onRestored() }
                }
            }
        }
    }
}

@Composable
private fun CollectionStudio(mode: NexusMode, prefs: SharedPreferences, apps: List<LauncherApp>, onLaunch: (LauncherApp) -> Unit) {
    var collections by remember { mutableStateOf(runCatching { SovereignCodec.collections(prefs.getString("v3_collections", null) ?: SovereignCodec.encodeCollections(SovereignCodec.defaults)) }.getOrDefault(SovereignCodec.defaults)) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var dialog by remember { mutableStateOf<String?>(null) }
    val current = collections.firstOrNull { it.id == selected }
    fun save(next: List<NexusCollection>) { collections = next; prefs.edit().putString("v3_collections", SovereignCodec.encodeCollections(next)).apply() }
    Column(Modifier.fillMaxSize()) {
        if (current == null) {
            Text("Constelações de aplicativos", color = mode.gold, fontWeight = FontWeight.Bold)
            Text("Crie grupos com os apps que você usa juntos. Os ocultos permanecem fora das coleções.", color = muted, fontSize = 12.sp)
            Button(onClick = { name = ""; dialog = "create" }, enabled = collections.size < 40, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("+ Nova coleção") }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(collections, key = { it.id }) { group ->
                    val members = apps.filter { it.component.flattenToString() in group.apps }
                    Surface(Modifier.fillMaxWidth().clickable { selected = group.id; editing = false; query = "" }, shape = RoundedCornerShape(20.dp), color = mode.primary.copy(alpha = .10f), border = BorderStroke(1.dp, mode.gold.copy(alpha = .30f))) {
                        Column(Modifier.padding(18.dp)) {
                            Text("✦  ${group.name}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${members.size} disponíveis  /  abrir coleção ›", color = mode.gold, fontSize = 11.sp)
                            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { members.take(5).forEach { V2AppIcon(it, 35) } }
                            if (members.isEmpty()) Text("Toque para escolher seus aplicativos", color = muted, fontSize = 12.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        } else {
            TextButton(onClick = { selected = null }) { Text("‹ Todas as coleções") }
            Text(current.name, color = mode.gold, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { editing = !editing; query = "" }) { Text(if (editing) "Concluir" else "Escolher apps") }
                TextButton(onClick = { name = current.name; dialog = "rename" }) { Text("Renomear") }
                TextButton(onClick = { dialog = "delete" }) { Text("Excluir") }
            }
            if (editing) OutlinedTextField(query, { query = it }, label = { Text("Pesquisar aplicativo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            val source = apps.filter { (editing || it.component.flattenToString() in current.apps) && it.label.contains(query, true) }
            if (source.isEmpty()) Text("Nenhum app disponível. Use Escolher apps para preencher a coleção.", color = muted, modifier = Modifier.padding(vertical = 18.dp))
            LazyColumn {
                items(source, key = { it.component.flattenToString() }) { app ->
                    val id = app.component.flattenToString()
                    Row(Modifier.fillMaxWidth().clickable {
                        if (editing) save(collections.map { if (it.id == current.id) it.copy(apps = if (id in it.apps) it.apps - id else it.apps + id) else it }) else onLaunch(app)
                    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        V2AppIcon(app, 38)
                        Text(app.label, color = Color.White, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                        if (editing) Checkbox(checked = id in current.apps, onCheckedChange = {
                            save(collections.map { if (it.id == current.id) it.copy(apps = if (id in it.apps) it.apps - id else it.apps + id) else it })
                        }) else Text("↗", color = mode.gold)
                    }
                }
            }
        }
    }
    if (dialog != null) AlertDialog(onDismissRequest = { dialog = null }, title = { Text(when(dialog) { "create" -> "Nova coleção"; "delete" -> "Excluir coleção?"; else -> "Renomear coleção" }) },
        text = { if (dialog == "delete") Text("Somente o grupo será removido. Seus aplicativos continuam instalados.") else OutlinedTextField(name, { name = it.take(40) }, label = { Text("Nome") }, singleLine = true) },
        confirmButton = { TextButton(enabled = dialog == "delete" || name.isNotBlank(), onClick = {
            when(dialog) {
                "create" -> save(collections + NexusCollection(SovereignCodec.id(), name.trim(), emptyList()))
                "rename" -> save(collections.map { if (it.id == selected) it.copy(name = name.trim()) else it })
                "delete" -> { save(collections.filterNot { it.id == selected }); selected = null }
            }; dialog = null
        }) { Text(if (dialog == "delete") "Excluir" else "Salvar") } }, dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } })
}

@Composable
private fun MissionStudio(mode: NexusMode, prefs: SharedPreferences) {
    var missions by remember { mutableStateOf(runCatching { SovereignCodec.missions(prefs.getString("v3_missions", "[]") ?: "[]") }.getOrDefault(emptyList())) }
    var draft by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var removed by remember { mutableStateOf<NexusMission?>(null) }
    fun save(next: List<NexusMission>) { missions = next; prefs.edit().putString("v3_missions", SovereignCodec.encodeMissions(next)).apply() }
    Column {
        Text("${missions.count { it.done }} / ${missions.size} concluídas", color = mode.gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Suas tarefas ficam salvas neste aparelho.", color = muted, fontSize = 12.sp)
        OutlinedTextField(draft, { draft = it.take(200) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text(if (editId == null) "Próxima missão" else "Editar missão") }, maxLines = 3)
        Row {
            Button(enabled = draft.isNotBlank() && (missions.size < 200 || editId != null), onClick = {
                if (editId == null) save(missions + NexusMission(SovereignCodec.id(), draft.trim(), false))
                else save(missions.map { if (it.id == editId) it.copy(title = draft.trim()) else it })
                draft = ""; editId = null
            }) { Text(if (editId == null) "Adicionar" else "Salvar") }
            if (editId != null) TextButton(onClick = { editId = null; draft = "" }) { Text("Cancelar") }
        }
        if (removed != null) TextButton(onClick = { if (missions.size < 200) save(missions + removed!!); removed = null }) { Text("Desfazer exclusão") }
        if (missions.isEmpty()) Text("Uma missão de cada vez. Adicione a primeira acima.", color = muted, modifier = Modifier.padding(top = 20.dp))
        LazyColumn {
            items(missions.sortedBy { it.done }, key = { it.id }) { mission ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(mission.done, { save(missions.map { if (it.id == mission.id) it.copy(done = !it.done) else it }) })
                    Text(mission.title, color = if (mission.done) muted else Color.White, modifier = Modifier.weight(1f).clickable { editId = mission.id; draft = mission.title }.padding(8.dp))
                    TextButton(onClick = { save(missions.filterNot { it.id == mission.id }); removed = mission; if (editId == mission.id) { editId = null; draft = "" } }) { Text("Excluir", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun FocusStudio(mode: NexusMode, prefs: SharedPreferences) {
    val context = LocalContext.current
    val boot = remember { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
    var duration by remember { mutableIntStateOf(prefs.getInt("v3_focus_duration", 25).coerceIn(1, 120)) }
    var deadline by remember { mutableLongStateOf(if (prefs.getInt("v3_focus_boot", -2) == boot) prefs.getLong("v3_focus_end", 0L) else 0L) }
    var paused by remember { mutableLongStateOf(prefs.getLong("v3_focus_paused", duration * 60L).coerceIn(0L, 7200L)) }
    var remaining by remember { mutableLongStateOf(if (deadline > 0) focusRemaining(deadline, SystemClock.elapsedRealtime()) else paused) }
    var finished by remember { mutableStateOf(false) }
    fun persist() { prefs.edit().putInt("v3_focus_duration", duration).putInt("v3_focus_boot", boot).putLong("v3_focus_end", deadline).putLong("v3_focus_paused", paused).apply() }
    LaunchedEffect(deadline, paused) {
        while (deadline > 0) {
            remaining = focusRemaining(deadline, SystemClock.elapsedRealtime())
            if (remaining == 0L) { deadline = 0; paused = 0; finished = true; persist(); break }
            delay(250)
        }
        if (deadline == 0L) remaining = paused
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        SectionV2("CÂMARA DE FOCO", mode)
        SovereignCore(mode, deadline > 0 && prefs.getBoolean("v2_animations", true), SovereignVisual(core = "ORBIT", compact = true)) { }
        Text("%02d:%02d".format(remaining / 60, remaining % 60), fontSize = 58.sp, color = mode.gold, fontWeight = FontWeight.Light)
        Text(if (finished) "Sessão concluída. Respire um pouco." else if (deadline > 0) "Uma tarefa. Toda a sua atenção." else "Escolha a duração e comece.", color = muted)
        Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                finished = false
                if (deadline > 0) { paused = focusRemaining(deadline, SystemClock.elapsedRealtime()); deadline = 0 }
                else { if (paused <= 0) paused = duration * 60L; deadline = SystemClock.elapsedRealtime() + paused * 1000 }
                persist()
            }) { Text(if (deadline > 0) "Pausar" else "Iniciar") }
            OutlinedButton(onClick = { deadline = 0; paused = duration * 60L; finished = false; persist() }) { Text("Reiniciar") }
        }
        Text("Duração: $duration minutos", color = Color.White)
        Slider(value = duration.toFloat(), onValueChange = { duration = it.toInt(); paused = duration * 60L; finished = false; persist() }, valueRange = 1f..120f, enabled = deadline == 0L)
        Text("O contador mantém o tempo ao sair e voltar. Não bloqueia apps nem emite alarme em segundo plano. Reiniciar o celular encerra a contagem ativa.", color = muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 18.dp))
    }
}

@Composable
private fun IdentityStudio(mode: NexusMode, prefs: SharedPreferences) {
    var visual by remember { mutableStateOf(readSovereignVisual(prefs)) }
    var signature by rememberSaveable { mutableStateOf(visual.signature) }
    fun save(next: SovereignVisual) { visual = next; prefs.edit().putString("v3_signature", next.signature).putString("v3_core", next.core).putBoolean("v3_compact", next.compact).apply() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SovereignCore(mode, prefs.getBoolean("v2_animations", true), visual) { }
        Text("Três formas. A sua assinatura.", color = mode.gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        listOf("SERAPH" to "Serafim • seis asas geométricas", "ECLIPSE" to "Eclipse • coroa de luz", "ORBIT" to "Órbita • engenharia arcana").forEach { (id, label) ->
            ActionBarV2("✦", label.substringBefore(" •"), label.substringAfter("• "), mode, visual.core == id) { save(visual.copy(core = id)) }
        }
        OutlinedTextField(signature, { signature = it.take(28); if (signature.isNotBlank()) save(visual.copy(signature = signature)) }, label = { Text("Assinatura da tela inicial") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Núcleo compacto", color = Color.White); Text("Mais espaço para seus aplicativos", color = muted, fontSize = 12.sp) }
            Switch(visual.compact, { save(visual.copy(compact = it)) })
        }
        Text("As cores seguem o protocolo escolhido na aba Visual. Na tela inicial, as animações param automaticamente com bateria em 15% ou menos.", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun BackupStudio(mode: NexusMode, prefs: SharedPreferences, onRestored: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<JSONObject?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val snapshot = SovereignCodec.export(prefs.all)
            scope.launch {
                busy = true
                val result = withContext(Dispatchers.IO) { runCatching { requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter().use { it.write(snapshot) } } }
                busy = false; status = if (result.isSuccess) "Backup salvo." else "Não foi possível salvar o arquivo."
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { runCatching {
                val raw = requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader().use { reader ->
                    val builder = StringBuilder(); val buffer = CharArray(8192)
                    while (true) { val n = reader.read(buffer); if (n < 0) break; require(builder.length + n <= 1_000_000); builder.append(buffer, 0, n) }
                    builder.toString()
                }
                SovereignCodec.validateBackup(raw)
            } }
            busy = false
            result.onSuccess { pending = it; status = "Backup validado. Confirme para restaurar." }.onFailure { status = "Arquivo inválido ou incompatível. Nenhum dado foi alterado." }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Seu Nexus, preservado.", color = mode.gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Guarde coleções, missões, assinatura, tema, favoritos, apps ocultos e notas em um arquivo JSON. O backup não contém senhas, tokens de IA ou notificações.", color = muted)
        Spacer(Modifier.height(18.dp))
        Button(enabled = !busy, onClick = { runCatching { export.launch("Port-Nexus-Sovereign-backup.json") }.onFailure { status = "Nenhum seletor de arquivos disponível." } }, modifier = Modifier.fillMaxWidth()) { Text("Exportar meu Nexus") }
        OutlinedButton(enabled = !busy, onClick = { runCatching { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }.onFailure { status = "Nenhum seletor de arquivos disponível." } }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar de um arquivo") }
        Text(if (busy) "Processando arquivo…" else status, color = mode.gold, modifier = Modifier.padding(vertical = 18.dp))
        Text("O arquivo inclui suas notas e tarefas. Escolha onde guardá-lo pelo seletor do Android.", color = muted, fontSize = 12.sp)
    }
    if (pending != null) AlertDialog(onDismissRequest = { pending = null }, title = { Text("Restaurar este backup?") },
        text = { Text("Os campos presentes no arquivo substituirão suas preferências, notas, coleções e missões correspondentes. A configuração da IA será preservada.") },
        confirmButton = { TextButton(onClick = { SovereignCodec.applyBackup(prefs, pending!!); pending = null; Toast.makeText(context, "Nexus restaurado", Toast.LENGTH_SHORT).show(); onRestored() }) { Text("Restaurar") } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } })
}
