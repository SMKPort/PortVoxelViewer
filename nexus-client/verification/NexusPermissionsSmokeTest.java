package com.port.nexusclient;

/**
 * Isolated permission orchestration checks that run on the CI JDK without
 * NeoForge or Minecraft. The real mod is compiled separately by Gradle.
 */
public final class NexusPermissionsSmokeTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void reset() {
        ClientConfig.allowChat=false;
        ClientConfig.allowTelemetry=false;
        ClientConfig.allowMind=false;
        ClientConfig.allowAutonomy=false;
        NexusHearing.consent=true;
        NexusHearing.plasmoReady=true;
        NexusHearing.listening=false;
        NexusHearing.memory=false;
        NexusHearing.answer=false;
        NexusHearing.collective=false;
        NexusPrivateMind.active=false;
        NexusPrivateMind.memoryEnabled=false;
        NexusPrivateMind.proactive=false;
        NexusPrivateMind.freedom=false;
        NexusPrivateMind.savedMessages=4;
        NexusSocial.diary=false;
        NexusSocial.protection=false;
        NexusSocial.share=false;
        NexusSpeaker.active=false;
        NexusAutoHeal.enabled=false;
        NexusBlessing.enabled=false;
        NexusCombatMacro.active=false;
        NexusCognition.monitor=false;
        NexusCognition.emergency=false;
        NexusCombatInsights.enabled=false;
        NexusNarrativeJournal.auto=false;
        NexusVoiceCasting.on=false;
        NexusArcaneTravel.pending=false;
        NexusArcaneSensor.on=false;
        NexusHudSettings.combat=false;NexusHudSettings.magic=false;
    }

    public static void main(String[] args) {
        reset();
        NexusPermissions.all();
        check(ClientConfig.allowChat && ClientConfig.allowTelemetry
            && ClientConfig.allowMind && ClientConfig.allowAutonomy,
            "Core permissions must be authorized together.");
        check(NexusPrivateMind.active && NexusPrivateMind.memoryEnabled
            && NexusPrivateMind.proactive && NexusPrivateMind.freedom,
            "Private channel permissions must be on.");
        check(NexusSocial.diary && NexusSocial.protection && NexusSocial.share,
            "Social settings must be on.");
        check(!NexusSpeaker.active,"Global activation must not enable potentially paid TTS.");
        check(!NexusVoiceCasting.on,
            "Enable everything must never silently activate the real microphone.");
        check(!NexusAutoHeal.enabled,
            "Global enable must not spend Heal mana without separate authorization.");
        check(!NexusCombatMacro.active,"Global enable must not activate offensive macros.");
        check(!NexusCognition.monitor&&!NexusCognition.emergency,
            "Global enable must not arm emergency automation without consent.");
        check(NexusArcaneSensor.on&&NexusHudSettings.combat&&NexusHudSettings.magic,
            "HUD and client-side magic readings are included in global enable.");
        check(!NexusHearing.listening && !NexusHearing.memory &&
            !NexusHearing.answer && !NexusHearing.collective,
            "Global enable must never activate retired audio capture.");
        NexusPermissions.status();
        NexusVoiceCasting.on=true;
        NexusAutoHeal.enabled=true;
        NexusBlessing.enabled=true;
        NexusCombatMacro.active=true;
        NexusCognition.monitor=true;NexusCognition.emergency=true;
        NexusCombatInsights.enabled=true;
        NexusNarrativeJournal.auto=true;
        NexusArcaneTravel.pending=true;
        NexusPermissions.off();
        check(!NexusArcaneTravel.pending, "Global off cancels pending Waystones requests.");
        check(!NexusCombatMacro.active,"Global off stops offensive macros.");
        check(!NexusCognition.monitor&&!NexusCognition.emergency,
            "Global off revokes Nexus 4 contextual emergency permissions.");
        check(!NexusCombatInsights.enabled&&!NexusNarrativeJournal.auto,
            "Global off revokes diagnostic and automated journal logging.");
        check(!NexusAutoHeal.enabled,"Global off must stop automatic healing.");
        check(!NexusBlessing.enabled,"Global off must stop automatic Blessing renewals.");
        check(!NexusVoiceCasting.on,"Disable everything closes the local microphone.");
        check(!ClientConfig.allowChat && !ClientConfig.allowTelemetry
            && !ClientConfig.allowMind && !ClientConfig.allowAutonomy,
            "Core permissions must all be off.");
        check(!NexusPrivateMind.active && !NexusPrivateMind.memoryEnabled
            && !NexusPrivateMind.proactive && !NexusPrivateMind.freedom,
            "Private channel must be off.");
        check(!NexusArcaneSensor.on&&!NexusHudSettings.combat&&!NexusHudSettings.magic,
            "Global off disables both local HUD panels and magic readings.");
        check(!NexusSocial.diary && !NexusSocial.protection && !NexusSocial.share,
            "Sharing and protection must be off.");
        check(!NexusSpeaker.active && !NexusHearing.listening
            && !NexusHearing.memory && !NexusHearing.answer,
            "Voice capture and processing must be off.");
        check(NexusPrivateMind.savedMessages == 4,
            "A global disable must preserve existing private memories.");

        reset();
        NexusHearing.consent=false;
        NexusPermissions.all();
        check(!NexusHearing.listening, "Cannot bypass third-party consent.");
        check(ClientConfig.allowMind && ClientConfig.allowTelemetry,
            "Unavailable hearing must not disable unrelated functions.");
        NexusPermissions.off();

        reset();
        NexusHearing.plasmoReady=false;
        NexusPermissions.all();
        check(!NexusHearing.listening, "Cannot capture without Plasmo Voice.");
        NexusPermissions.off();

        System.out.println("NexusPermissions smoke tests OK");
    }
}

/* Minimal fakes for controller-only tests. Do not compile these with the mod. */
final class ClientConfig {
    static boolean allowChat, allowTelemetry, allowMind, allowAutonomy;
    static void save() {}
}
final class ClientLink {
    static void resetTelemetryBaseline() {}
    static void sendHearingState() {}
    static String status() { return "connected"; }
}
final class NexusMind {
    static void resetSession() {}
}
final class NexusPrivateMind {
    static boolean active, memoryEnabled, proactive, freedom;
    static int savedMessages;
    static void enable(boolean on) { active=on; if (!on) proactive=false; }
    static void memory(boolean on) { memoryEnabled=on; if (!on) savedMessages=0; }
    static void setMemoryWithoutClearing(boolean on) { memoryEnabled=on; }
    static void setProactive(boolean on) {
        if (on && !active) throw new IllegalStateException("Private disabled");
        proactive=on;
    }
    static void setFreedom(boolean on) { freedom=on; }
    static String status() { return "private"; }
}
final class NexusSocial {
    static boolean diary, protection, share;
    static void setting(String name, boolean on) {
        switch (name) {
            case "diary" -> diary=on;
            case "protection" -> protection=on;
            case "share" -> share=on;
            default -> throw new IllegalArgumentException(name);
        }
    }
    static String status() { return "social"; }
}
final class NexusSpeaker {
    static boolean active;
    static void enable(boolean on) { active=on; }
    static String status() { return "speaker"; }
}
final class NexusHearing {
    static boolean consent, plasmoReady, listening, memory, answer, collective;
    static void remember(boolean on) { memory=on; }
    static void answer(boolean on) { answer=on; }
    static int collectiveOn() {
        if (!consent) throw new IllegalStateException("Consent list empty");
        collective=true;
        return 1;
    }
    static void listen(boolean on) {
        if (on && (!consent || !plasmoReady))
            throw new IllegalStateException("Plasmo or consent unavailable");
        listening=on;
    }
}
final class NexusClientMod {
    static void notifyPlayer(String message) {
        if (message == null) throw new AssertionError("Message must not be null");
    }
}

final class NexusHudSettings {
    static boolean combat,magic;
    static class Layout { boolean enabled;Layout(boolean enabled){this.enabled=enabled;} }
    static void enabled(String id,boolean on){if(id.equals("magic"))magic=on;else combat=on;}
    static Layout get(String id){return new Layout(id.equals("magic")?magic:combat);}
}
final class NexusArcaneSensor {
    static boolean on;
    static void enabled(boolean enabled){on=enabled;}
    static void scanNearby(boolean enabled){}
}

final class NexusVoiceCasting {
    static boolean on;
    static void setEnabled(boolean enabled){on=enabled;}
    static String status(){return "offline"; }
}

final class NexusAutoHeal {
    static boolean enabled;
    static boolean enabled(){return enabled;}
    static void setEnabled(boolean on){enabled=on;}
    static int threshold(){return 35;}
    static String lastOutcome(){return "waiting";}
}

final class NexusCombatMacro {
    static boolean active;
    static void setEnabled(boolean on){active=on;}
    static String status(){return active?"macro on":"macro off";}
}

/** Stub for global-permissions tests only. */
final class NexusArcaneTravel {
    static boolean pending;
    static String cancel(){pending=false;return "cancelled";}
}

/** Stand-in only for isolated permission tests, not the Minecraft mod. */
final class NexusBlessing {
    static boolean enabled;
    static void setEnabled(boolean on){enabled=on;}
    static String summary(){return enabled?"Blessing ON":"Blessing OFF";}
}

/** Stand-ins for Nexus 4 opt-in modules in isolated global permission tests. */
final class NexusCognition {
    static boolean monitor,emergency;
    static void off(){monitor=false;emergency=false;}
    static String status(){return "nexus4";}
}
final class NexusCombatInsights {
    static boolean enabled;
    static void setEnabled(boolean on){enabled=on;}
}
final class NexusNarrativeJournal {
    static boolean auto;
    static void autoCritical(boolean on){auto=on;}
}
