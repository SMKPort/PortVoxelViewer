package com.port.nexuslauncher
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class SovereignCodecTest {
    @Test fun collectionsRoundTripWithUnicode() {
        val items = listOf(NexusCollection("a", "Engenharia ✦", listOf("com.example/.Main", "com.music/.Play")))
        assertEquals(items, SovereignCodec.collections(SovereignCodec.encodeCollections(items)))
    }
    @Test fun missionsPreserveCompletionAndText() {
        val items = listOf(NexusMission("a", "Estudar cálculo\nLimites", true), NexusMission("b", "Ensaiar", false))
        assertEquals(items, SovereignCodec.missions(SovereignCodec.encodeMissions(items)))
    }
    @Test fun exportExcludesSecretsAndSignals() {
        val data = SovereignCodec.validateBackup(SovereignCodec.export(mapOf("v3_signature" to "PORT", "ai_token" to "secret", "nexus_ai_url" to "private", "notifications" to "private", "favorites" to setOf("app.one"))))
        assertEquals(setOf("v3_signature", "favorites"), data.keys().asSequence().toSet())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInjectedSettings() {
        SovereignCodec.validateBackup(JSONObject().put("format", "port-nexus-sovereign").put("version", 1).put("data", JSONObject().put("ai_token", "injected")).toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateCollectionIds() {
        SovereignCodec.collections(SovereignCodec.encodeCollections(listOf(NexusCollection("x", "A", emptyList()), NexusCollection("x", "B", emptyList()))))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOutOfBoundsIcon() {
        SovereignCodec.validateBackup(SovereignCodec.export(mapOf("v2_icon" to 100000f)))
    }
    @Test fun timerUsesDeadlineRatherThanNumberOfTicks() {
        assertEquals(25L, focusRemaining(30000L, 5000L))
        assertEquals(1L, focusRemaining(30000L, 29999L))
        assertEquals(0L, focusRemaining(30000L, 45000L))
    }
}
