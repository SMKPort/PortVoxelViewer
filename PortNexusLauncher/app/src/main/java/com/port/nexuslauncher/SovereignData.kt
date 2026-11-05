package com.port.nexuslauncher

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SovereignVisual(val signature: String = "PORT / MORNINGSTAR", val core: String = "SERAPH", val compact: Boolean = false)
data class NexusCollection(val id: String, val name: String, val apps: List<String>)
data class NexusMission(val id: String, val title: String, val done: Boolean)
fun readSovereignVisual(prefs: SharedPreferences) = SovereignVisual(
    prefs.getString("v3_signature", "PORT / MORNINGSTAR") ?: "PORT / MORNINGSTAR",
    prefs.getString("v3_core", "SERAPH") ?: "SERAPH", prefs.getBoolean("v3_compact", false))

/** Only explicitly allowed local settings can cross the backup boundary. */
object SovereignCodec {
    fun collections(raw: String): List<NexusCollection> {
        val a = JSONArray(raw)
        require(a.length() <= 40) { "Máximo de 40 coleções." }
        val result = (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val apps = o.getJSONArray("apps")
            require(apps.length() <= 500)
            NexusCollection(o.getString("id").also { require(it.length in 1..80) },
                o.getString("name").also { require(it.isNotBlank() && it.length <= 40) },
                (0 until apps.length()).map { apps.getString(it).also { id -> require(id.length in 1..500) } }.distinct())
        }
        require(result.map { it.id }.distinct().size == result.size)
        return result
    }
    fun encodeCollections(items: List<NexusCollection>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("apps", JSONArray(it.apps))) }
    }.toString()
    fun missions(raw: String): List<NexusMission> {
        val a = JSONArray(raw)
        require(a.length() <= 200) { "Máximo de 200 missões." }
        val result = (0 until a.length()).map { i -> val o = a.getJSONObject(i)
            NexusMission(o.getString("id").also { require(it.length in 1..80) },
                o.getString("title").also { require(it.isNotBlank() && it.length <= 200) }, o.getBoolean("done")) }
        require(result.map { it.id }.distinct().size == result.size)
        return result
    }
    fun encodeMissions(items: List<NexusMission>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)) }
    }.toString()
    fun id() = UUID.randomUUID().toString()
    val defaults = listOf(NexusCollection("study", "Engenharia", emptyList()), NexusCollection("music", "Estúdio", emptyList()), NexusCollection("play", "Universo Port", emptyList()))
    val keys = setOf("v3_signature", "v3_core", "v3_compact", "v3_collections", "v3_missions", "favorites", "v2_hidden", "v2_mode", "v2_drawer", "v2_animations", "v2_icon", "v2_notes")
    fun export(values: Map<String, *>): String {
        val data = JSONObject()
        values.filterKeys { it in keys }.forEach { (key, value) -> data.put(key, if (value is Set<*>) JSONArray(value.toList()) else value) }
        return JSONObject().put("format", "port-nexus-sovereign").put("version", 1).put("data", data).toString(2)
    }
    fun validateBackup(raw: String): JSONObject {
        require(raw.length <= 1_000_000) { "Backup muito grande." }
        val root = JSONObject(raw)
        require(root.getString("format") == "port-nexus-sovereign" && root.getInt("version") == 1) { "Backup incompatível." }
        val d = root.getJSONObject("data")
        d.keys().forEach { k ->
            require(k in keys) { "Campo não permitido: $k" }
            when (k) {
                "v3_collections" -> collections(d.getString(k))
                "v3_missions" -> missions(d.getString(k))
                "v3_signature" -> require(d.getString(k).isNotBlank() && d.getString(k).length <= 28)
                "v3_core" -> require(d.getString(k) in setOf("SERAPH", "ECLIPSE", "ORBIT"))
                "v3_compact", "v2_animations" -> require(d.get(k) is Boolean)
                "v2_icon" -> require(d.getDouble(k).isFinite() && d.getDouble(k) in 38.0..70.0)
                "v2_mode" -> require(d.getString(k) in setOf("ARCANO", "INFERNAL", "ANGELICAL", "INDUSTRIAL", "MUSICA", "ESTUDO", "PERFORMANCE"))
                "v2_drawer" -> require(d.getString(k) in setOf("GRADE", "ARSENAL", "INTELIGENTE"))
                "v2_notes" -> require(d.getString(k).length <= 100_000)
                "favorites", "v2_hidden" -> { val a = d.getJSONArray(k); require(a.length() <= 1000)
                    repeat(a.length()) { require(a.getString(it).length in 1..500) } }
            }
        }
        return d
    }
    fun applyBackup(prefs: SharedPreferences, data: JSONObject) {
        val d = validateBackup(JSONObject().put("format", "port-nexus-sovereign").put("version", 1).put("data", data).toString())
        val edit = prefs.edit()
        d.keys().forEach { k -> when (k) {
            "favorites", "v2_hidden" -> { val a = d.getJSONArray(k); edit.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet()) }
            "v3_compact", "v2_animations" -> edit.putBoolean(k, d.getBoolean(k))
            "v2_icon" -> edit.putFloat(k, d.getDouble(k).toFloat())
            else -> edit.putString(k, d.getString(k))
        } }
        edit.apply()
    }
}
fun focusRemaining(end: Long, now: Long): Long = ((end - now).coerceAtLeast(0L) + 999L) / 1000L
