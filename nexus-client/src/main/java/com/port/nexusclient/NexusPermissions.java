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
        // Prerequisites first: autonomy, arcane reactions and proactive private
        // messages depend on these switches being enabled.
        ClientConfig.allowChat = true;
        ClientConfig.allowTelemetry = true;
        ClientConfig.allowMind = true;
        ClientConfig.allowAutonomy = true;
        ClientConfig.save();
        ClientLink.resetTelemetryBaseline();
        NexusMind.resetSession();

        NexusArcaneSensor.enabled(true);
        NexusArcaneSensor.scanNearby(true);
        NexusArcaneSensor.reactions(true);

        NexusPrivateMind.enable(true);
        NexusPrivateMind.memory(true);
        NexusPrivateMind.setProactive(true);
        NexusPrivateMind.setFreedom(true);

        NexusSocial.setting("diary", true);
        NexusSocial.setting("protection", true);
        NexusSocial.setting("share", true);
        NexusSpeaker.enable(true);

        // These options alone cannot collect audio. Actual Plasmo capture must
        // also pass the existing consent-list, online-roster and availability
        // checks. @a is the intersection with players already authorized.
        NexusHearing.remember(true);
        NexusHearing.answer(true);
        String hearing;
        try {
            int selected = NexusHearing.collectiveOn();
            NexusHearing.listen(true);
            hearing = "Audição Plasmo ativa para " + selected
                + " jogador(es) online previamente consentido(s) (@a).";
        } catch (IllegalStateException error) {
            NexusHearing.listen(false);
            hearing = "Audição pendente: " + error.getMessage();
        }
        ClientLink.sendHearingState();
        ClientLink.sendArcaneState();

        NexusClientMod.notifyPlayer("Modo TOTAL ativado: chat, telemetria, mente, autonomia, "
            + "arcano, canal privado, diário, proteção e voz local.");
        NexusClientMod.notifyPlayer(hearing);
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

        NexusHearing.listen(false); // Drops buffered audio immediately.
        NexusHearing.answer(false);
        NexusHearing.remember(false);
        NexusSpeaker.enable(false);

        NexusArcaneSensor.reactions(false);
        NexusArcaneSensor.scanNearby(false);
        NexusArcaneSensor.enabled(false);

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
        ClientLink.sendArcaneState();
        NexusClientMod.notifyPlayer("Modo TOTAL desligado. Captura, publicações e recursos "
            + "automáticos interrompidos. Memórias antigas preservadas.");
    }

    static void status() {
        NexusClientMod.notifyPlayer("Geral: " + ClientLink.status());
        NexusClientMod.notifyPlayer(NexusArcaneSensor.status());
        NexusClientMod.notifyPlayer(NexusPrivateMind.status());
        NexusClientMod.notifyPlayer(NexusSocial.status());
        NexusClientMod.notifyPlayer(NexusSpeaker.status());
    }
}
