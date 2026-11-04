package com.port.nexuslauncher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Dados efêmeros, mantidos somente em memória. Nenhum texto de notificações é salvo em disco. */
data class NexusNotice(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val canDismiss: Boolean,
    val group: String,
)

object NexusNotificationStore {
    var notices by mutableStateOf<List<NexusNotice>>(emptyList())
        private set
    var connected by mutableStateOf(false)
        private set

    private var listener: NexusNotificationListener? = null

    internal fun attach(service: NexusNotificationListener) {
        listener = service
        connected = true
        refresh()
    }

    internal fun detach(service: NexusNotificationListener) {
        if (listener !== service) return
        listener = null
        connected = false
        notices = emptyList()
    }

    internal fun refresh() {
        val service = listener ?: return
        notices = runCatching {
            service.activeNotifications.orEmpty().mapNotNull { item ->
                val n = item.notification
                val extras = n.extras ?: return@mapNotNull null
                val source = item.packageName
                val label = runCatching {
                    val app = service.packageManager.getApplicationInfo(source, 0)
                    service.packageManager.getApplicationLabel(app).toString()
                }.getOrElse { source.substringAfterLast('.') }

                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?.take(100)?.takeIf { it.isNotBlank() } ?: label
                val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT))
                    ?.toString()?.take(300).orEmpty()
                val category = when {
                    n.category == Notification.CATEGORY_MESSAGE ||
                        n.category == Notification.CATEGORY_CALL -> "CONVERSAS"
                    !item.isClearable -> "EM ANDAMENTO"
                    else -> "ATIVIDADES"
                }
                NexusNotice(
                    key = item.key,
                    packageName = source,
                    appLabel = label,
                    title = title,
                    body = body,
                    postedAt = item.postTime,
                    canDismiss = item.isClearable,
                    group = category,
                )
            }.sortedByDescending { it.postedAt }
        }.getOrDefault(emptyList())
    }

    /** Abre a ação original da notificação, se existir; nunca inventa um intent. */
    fun open(key: String): Boolean {
        val service = listener ?: return false
        return runCatching {
            val posted = service.activeNotifications?.firstOrNull { it.key == key } ?: return false
            val intent = posted.notification.contentIntent ?: return false
            intent.send()
            if (posted.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) {
                service.cancelNotification(key)
            }
            true
        }.getOrDefault(false)
    }

    fun dismiss(key: String) {
        val service = listener ?: return
        runCatching { service.cancelNotification(key) }
    }

    fun dismissClearable() {
        val service = listener ?: return
        val keys = notices.filter { it.canDismiss }.map { it.key }
        if (keys.isNotEmpty()) runCatching { service.cancelNotifications(keys.toTypedArray()) }
    }
}

class NexusNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NexusNotificationStore.attach(this)
    }

    override fun onListenerDisconnected() {
        NexusNotificationStore.detach(this)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NexusNotificationStore.refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NexusNotificationStore.refresh()
    }

    override fun onDestroy() {
        NexusNotificationStore.detach(this)
        super.onDestroy()
    }
}
