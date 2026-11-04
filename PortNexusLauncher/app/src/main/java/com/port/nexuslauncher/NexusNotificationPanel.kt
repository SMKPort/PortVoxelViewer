package com.port.nexuslauncher

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SignalInk = Color(0xFF08070D)
private val SignalText = Color(0xFFFFF4E9)
private val SignalMuted = Color(0xFFCDBFC9)

/** O próprio launcher exibe o painel. A central original do Android continua disponível. */
@Composable
fun NexusNotificationPanel(
    mode: NexusMode,
    hasPermission: Boolean,
    isConnected: Boolean,
    notices: List<NexusNotice>,
    onClose: () -> Unit,
    onGrant: () -> Unit,
    onOpen: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onDismissAll: () -> Unit,
    onSystemAction: (NexusAction) -> Unit,
) {
    val count = notices.size
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF261321), SignalInk, SignalInk)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 17.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("N E X U S   /   S I G N A L", fontSize = 11.sp, fontWeight = FontWeight.Black, color = mode.gold)
                Text("CENTRAL DE NOTIFICAÇÕES", fontSize = 20.sp, fontWeight = FontWeight.Light, color = SignalText)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fechar central", tint = mode.gold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = mode.primary.copy(alpha = .18f)),
            border = BorderStroke(1.dp, mode.gold.copy(alpha = .5f)),
        ) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(58.dp).background(mode.primary.copy(alpha = .18f), CircleShape)
                        .border(1.dp, mode.gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Notifications, null, tint = mode.gold, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (count == 1) "1 SINAL ATIVO" else "$count SINAIS ATIVOS",
                        fontWeight = FontWeight.Bold, color = SignalText, fontSize = 17.sp)
                    Text(
                        when {
                            !hasPermission -> "Acesso ainda não autorizado"
                            !isConnected -> "Aguardando conexão ao Android"
                            count == 0 -> "Nenhuma notificação pendente"
                            else -> "Transmissões organizadas pelo Nexus"
                        }, color = SignalMuted, fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        Text("COMANDOS DO SISTEMA", color = mode.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SignalTile("Wi-Fi", mode, Icons.Rounded.Wifi) { onSystemAction(NexusAction.WIFI_SETTINGS) }
            SignalTile("Bluetooth", mode, Icons.Rounded.Bluetooth) { onSystemAction(NexusAction.BLUETOOTH_SETTINGS) }
            SignalTile("Áudio", mode, Icons.Rounded.VolumeUp) { onSystemAction(NexusAction.SOUND_SETTINGS) }
            SignalTile("Sistema", mode, Icons.Rounded.Settings) { onSystemAction(NexusAction.ANDROID_SETTINGS) }
        }
        Spacer(Modifier.height(22.dp))
        if (!hasPermission) {
            SignalNoticeCard(mode) {
                Text("ATIVAR RECEPÇÃO DE SINAIS", color = mode.gold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Para visualizar avisos de outros aplicativos, autorize o acesso às notificações nas configurações do Android. " +
                        "Isso permite ler conteúdo de mensagens e notificações. Você pode revogar o acesso quando quiser.",
                    color = SignalText, fontSize = 13.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGrant, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = mode.primary)
                ) { Text("ABRIR PERMISSÕES", color = Color.White) }
                Text("Os textos ficam somente em memória. Não são enviados a servidores nem gravados pelo Nexus.",
                    color = SignalMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            }
        } else if (!isConnected) {
            SignalNoticeCard(mode) {
                Text("CONECTANDO AO SISTEMA", color = mode.gold, fontWeight = FontWeight.Bold)
                Text("O acesso foi autorizado, mas o serviço ainda não está conectado. " +
                    "Abra novamente as configurações se as notificações não aparecerem.",
                    color = SignalMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 9.dp))
                Button(onClick = onGrant, colors = ButtonDefaults.buttonColors(containerColor = mode.primary)) {
                    Text("VERIFICAR ACESSO")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TRANSMISSÕES RECEBIDAS", color = mode.gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                if (notices.any { it.canDismiss }) {
                    Row(Modifier.clickable(onClick = onDismissAll).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = SignalMuted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("LIMPAR", color = SignalMuted, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (notices.isEmpty()) {
                SignalNoticeCard(mode) {
                    Text("Nenhum aviso aguardando leitura.", color = SignalText)
                    Text("A central acompanha as notificações enquanto o acesso estiver autorizado.",
                        color = SignalMuted, fontSize = 11.sp)
                }
            } else {
                listOf("CONVERSAS", "ATIVIDADES", "EM ANDAMENTO").forEach { group ->
                    val filtered = notices.filter { it.group == group }
                    if (filtered.isNotEmpty()) {
                        Text(group + "  /  " + filtered.size, color = mode.gold,
                            fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp))
                        filtered.forEach { notice ->
                            SignalNoticeCard(mode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp)
                                        .background(mode.primary.copy(alpha = .25f), CircleShape),
                                        contentAlignment = Alignment.Center) {
                                        Text(notice.appLabel.take(1).uppercase(), color = mode.gold,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(notice.appLabel.uppercase(), color = mode.gold,
                                            fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(notice.title, color = SignalText, fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(signalTime(notice.postedAt), color = SignalMuted, fontSize = 10.sp)
                                }
                                if (notice.body.isNotBlank()) {
                                    Text(notice.body, color = SignalMuted, fontSize = 12.sp,
                                        lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 8.dp))
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.End) {
                                    if (notice.canDismiss) {
                                        Text("DISPENSAR", modifier = Modifier.clickable { onDismiss(notice.key) }
                                            .padding(9.dp), color = SignalMuted, fontSize = 11.sp)
                                    }
                                    Text("ABRIR", modifier = Modifier.clickable { onOpen(notice.key) }
                                        .padding(9.dp), color = mode.gold, fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Text("Notificações e permissões continuam sob controle do Android.",
                color = SignalMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SignalNoticeCard(mode: NexusMode, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1320)),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, mode.gold.copy(alpha = .22f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) { content() }
    }
}

@Composable
private fun SignalTile(
    title: String,
    mode: NexusMode,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(100.dp).background(Color(0xFF211622), RoundedCornerShape(14.dp))
            .border(1.dp, mode.gold.copy(alpha = .34f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = mode.gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(7.dp))
        Text(title, color = SignalText, fontSize = 11.sp)
    }
}

private fun signalTime(time: Long): String = runCatching {
    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")
