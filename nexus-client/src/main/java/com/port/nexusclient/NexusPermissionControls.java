package com.port.nexusclient;

/**
 * Single local source for the GUI permission switches.
 * These are explicit user choices; none grants Minecraft server permissions,
 * OS microphone access, Gemini credentials, or third-party voice consent.
 */
final class NexusPermissionControls {
    private NexusPermissionControls(){}

    static boolean enabled(String id){
        return switch(id){
            case "chat" -> ClientConfig.allowChat;
            case "telemetry" -> ClientConfig.allowTelemetry;
            case "mind" -> ClientConfig.allowMind;
            case "autonomy" -> ClientConfig.allowAutonomy;
            case "private" -> NexusPrivateMind.enabled();
            case "privateProactive" -> NexusPrivateMind.autonomous();
            case "privateMemory" -> NexusPrivateMind.memoryEnabled();
            case "privateFreedom" -> NexusPrivateMind.freedomEnabled();
            case "magic" -> NexusArcaneSensor.settingEnabled();
            case "combatHud" -> NexusHudSettings.get("combat").enabled;
            case "speaker" -> NexusSpeaker.settingEnabled();
            case "diary" -> NexusSocial.diaryEnabled();
            case "protection" -> NexusSocial.protectionEnabled();
            case "share" -> NexusSocial.sharingEnabled();
            default -> throw new IllegalArgumentException("Permissão desconhecida: "+id);
        };
    }

    static void toggle(String id){set(id,!enabled(id));}

    static void set(String id,boolean on){
        switch(id){
            case "chat" -> {
                ClientConfig.allowChat=on;
                if(!on)ClientConfig.allowAutonomy=false;
                ClientConfig.save();
            }
            case "telemetry" -> {
                ClientConfig.allowTelemetry=on;ClientConfig.save();
                ClientLink.resetTelemetryBaseline();
            }
            case "mind" -> {
                ClientConfig.allowMind=on;
                if(!on)ClientConfig.allowAutonomy=false;
                ClientConfig.save();NexusMind.resetSession();
            }
            case "autonomy" -> {
                if(on&&(!ClientConfig.allowChat||!ClientConfig.allowMind))
                    throw new IllegalStateException("Ative Chat e Mente antes da autonomia.");
                ClientConfig.allowAutonomy=on;ClientConfig.save();
            }
            case "private" -> NexusPrivateMind.enable(on);
            case "privateProactive" -> NexusPrivateMind.setProactive(on);
            case "privateMemory" -> {
                // Unlike the legacy memory-off chat command, this only stops
                // future writes. A permission menu must not silently erase data.
                NexusPrivateMind.setMemoryWithoutClearing(on);
            }
            case "privateFreedom" -> NexusPrivateMind.setFreedom(on);
            case "magic" -> {
                NexusArcaneSensor.enabled(on);
                NexusArcaneSensor.scanNearby(on);
                NexusHudSettings.enabled("magic",on);
            }
            case "combatHud" -> NexusHudSettings.enabled("combat",on);
            case "speaker" -> {
                NexusSpeaker.enable(on);
                ClientLink.sendHearingState();
            }
            case "diary", "protection", "share" -> {
                NexusSocial.setting(id,on);
                ClientLink.sendHearingState();
            }
            default -> throw new IllegalArgumentException("Permissão desconhecida: "+id);
        }
    }

    static String summary(){
        return (ClientConfig.token.isBlank()?"Render: SEM PAREAMENTO":"Render: PAREADO")+
            " | Audição de jogadores: REMOVIDA";
    }
}
