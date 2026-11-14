package com.port.nexusclient;

/**
 * Centralized client-only permission switches.
 *
 * The owner can authorize all capabilities with one local command. It cannot
 * grant consent for other players, Minecraft server privileges, an OS device
 * permission or credentials for remote services.
 */
final class NexusPermissions {
    private NexusPermissions() {}

    static void all() {
        // Prerequisites first: autonomy and proactive private
        // messages depend on these switches being enabled.
        ClientConfig.allowChat = true;
        ClientConfig.allowTelemetry = true;
        ClientConfig.allowMind = true;
        ClientConfig.allowAutonomy = true;
        ClientConfig.save();
        ClientLink.resetTelemetryBaseline();
        NexusMind.resetSession();
        NexusHudSettings.enabled("combat",true);
        NexusHudSettings.enabled("magic",true);
        NexusArcaneSensor.enabled(true);
        NexusArcaneSensor.scanNearby(true);

        NexusPrivateMind.enable(true);
        NexusPrivateMind.memory(true);
        NexusPrivateMind.setProactive(true);
        NexusPrivateMind.setFreedom(true);

        NexusSocial.setting("diary", true);
        NexusSocial.setting("protection", true);
        NexusSocial.setting("share", true);
        // TTS Gemini may also be metered; leave it an independent opt-in.
        // The "Ativar tudo" button never turns on paid speech generation.

        // Legacy hearing is retired. Never enable paid audio capture.
        NexusHearing.listen(false);
        ClientLink.sendHearingState(); // Keep social/voice preferences in sync.

        NexusClientMod.notifyPlayer("Modo TOTAL ativado: chat, telemetria, mente, autonomia, "
            + "HUD de combate, leitura mágica, canal privado e diário. TTS opcional.");
        NexusClientMod.notifyPlayer("As mensagens públicas saem da sua conta. "
            + "O diário pode ser consultado no painel privado; use permissions off para revogar.");
    }

    static void off() {
        // Stop capture and outgoing messages before changing secondary settings.
        ClientConfig.allowChat = false;
        ClientConfig.allowTelemetry = false;
        ClientConfig.allowMind = false;
        ClientConfig.allowAutonomy = false;
        ClientConfig.save();
        ClientLink.resetTelemetryBaseline();
        NexusMind.resetSession();
        NexusHudSettings.enabled("combat",false);
        NexusHudSettings.enabled("magic",false);
        NexusArcaneSensor.scanNearby(false);
        NexusArcaneSensor.enabled(false);

        NexusVoiceCasting.setEnabled(false);
        NexusHearing.listen(false); // Legacy setting remains safely disabled.
        NexusSpeaker.enable(false);

        NexusPrivateMind.setProactive(false);
        NexusPrivateMind.enable(false);
        // The existing memory(false) command deletes history. The global kill
        // switch must never erase the user's saved private conversations.
        NexusPrivateMind.setMemoryWithoutClearing(false);
        NexusPrivateMind.setFreedom(false);

        NexusSocial.setting("share", false);
        NexusSocial.setting("protection", false);
        NexusSocial.setting("diary", false);

        ClientLink.sendHearingState();
        NexusClientMod.notifyPlayer("Modo TOTAL desligado. Publicações e recursos "
            + "automáticos interrompidos. Memórias antigas preservadas.");
    }

    static void status() {
        NexusClientMod.notifyPlayer("Geral: " + ClientLink.status());
        NexusClientMod.notifyPlayer("HUD: combate "+(NexusHudSettings.get("combat").enabled?"ON":"OFF")+" | magia "+(NexusHudSettings.get("magic").enabled?"ON":"OFF"));
        NexusClientMod.notifyPlayer(NexusPrivateMind.status());
        NexusClientMod.notifyPlayer(NexusSocial.status());
        NexusClientMod.notifyPlayer(NexusSpeaker.status());
        NexusClientMod.notifyPlayer("Voz offline: "+NexusVoiceCasting.status());
    }
}
