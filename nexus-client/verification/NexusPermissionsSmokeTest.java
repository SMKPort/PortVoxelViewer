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
        NexusArcaneSensor.active=false;
        NexusArcaneSensor.nearby=false;
        NexusArcaneSensor.reactionsEnabled=false;
        NexusPrivateMind.active=false;
        NexusPrivateMind.memoryEnabled=false;
        NexusPrivateMind.proactive=false;
        NexusPrivateMind.freedom=false;
        NexusPrivateMind.savedMessages=4;
        NexusSocial.diary=false;
        NexusSocial.protection=false;
        NexusSocial.share=false;
        NexusSpeaker.active=false;
    }

    public static void main(String[] args) {
        reset();
        NexusPermissions.all();
        check(ClientConfig.allowChat && ClientConfig.allowTelemetry
            && ClientConfig.allowMind && ClientConfig.allowAutonomy,
            "Core permissions must be authorized together.");
        check(NexusArcaneSensor.active && NexusArcaneSensor.nearby
            && NexusArcaneSensor.reactionsEnabled, "Arcane features must be on.");
        check(NexusPrivateMind.active && NexusPrivateMind.memoryEnabled
            && NexusPrivateMind.proactive && NexusPrivateMind.freedom,
            "Private channel permissions must be on.");
        check(NexusSocial.diary && NexusSocial.protection && NexusSocial.share,
            "Social settings must be on.");
        check(!NexusSpeaker.active,"Global activation must not enable potentially paid TTS.");
        check(!NexusHearing.listening && !NexusHearing.memory &&
            !NexusHearing.answer && !NexusHearing.collective,
            "Global enable must never activate retired audio capture.");
        NexusPermissions.status();
        NexusPermissions.off();
        check(!ClientConfig.allowChat && !ClientConfig.allowTelemetry
            && !ClientConfig.allowMind && !ClientConfig.allowAutonomy,
            "Core permissions must all be off.");
        check(!NexusArcaneSensor.active && !NexusArcaneSensor.nearby
            && !NexusArcaneSensor.reactionsEnabled, "Arcane features must be off.");
        check(!NexusPrivateMind.active && !NexusPrivateMind.memoryEnabled
            && !NexusPrivateMind.proactive && !NexusPrivateMind.freedom,
            "Private channel must be off.");
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
    static void sendArcaneState() {}
    static String status() { return "connected"; }
}
final class NexusMind {
    static void resetSession() {}
}
final class NexusArcaneSensor {
    static boolean active, nearby, reactionsEnabled;
    static void enabled(boolean on) { active=on; }
    static void scanNearby(boolean on) { nearby=on; }
    static void reactions(boolean on) { reactionsEnabled=on; }
    static String status() { return "arcane"; }
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
