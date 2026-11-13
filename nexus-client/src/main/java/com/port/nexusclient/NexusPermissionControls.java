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
            case "hearing" -> NexusHearing.listeningEnabled();
            case "reply" -> NexusHearing.replyEnabled();
            case "hearingMemory" -> NexusHearing.memoryEnabled();
            case "collective" -> NexusHearing.collectiveEnabled();
            case "private" -> NexusPrivateMind.enabled();
            case "privateProactive" -> NexusPrivateMind.autonomous();
            case "privateMemory" -> NexusPrivateMind.memoryEnabled();
            case "privateFreedom" -> NexusPrivateMind.freedomEnabled();
            case "arcane" -> NexusArcaneSensor.settingEnabled();
            case "nearby" -> NexusArcaneSensor.nearbyEnabled();
            case "reactions" -> NexusArcaneSensor.reactionsEnabled();
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
            case "hearing" -> {
                NexusHearing.listen(on);
                ClientLink.sendHearingState();
            }
            case "reply" -> {
                if(on&&(!ClientConfig.allowChat||!ClientConfig.allowMind||!ClientConfig.allowAutonomy))
                    throw new IllegalStateException("Ative Chat, Mente e Autonomia ou use Ativar diálogo.");
                NexusHearing.answer(on);
                ClientLink.sendHearingState();
            }
            case "hearingMemory" -> {
                NexusHearing.remember(on);
                ClientLink.sendHearingState();
            }
            case "collective" -> {
                NexusHearing.selectCollective(on);
                ClientLink.sendHearingState();
            }
            case "private" -> NexusPrivateMind.enable(on);
            case "privateProactive" -> NexusPrivateMind.setProactive(on);
            case "privateMemory" -> {
                // Unlike the legacy memory-off chat command, this only stops
                // future writes. A permission menu must not silently erase data.
                NexusPrivateMind.setMemoryWithoutClearing(on);
            }
            case "privateFreedom" -> NexusPrivateMind.setFreedom(on);
            case "arcane" -> {
                if(!on)NexusArcaneSensor.reactions(false);
                NexusArcaneSensor.enabled(on);
                ClientLink.sendArcaneState();
            }
            case "nearby" -> {
                if(on&&!NexusArcaneSensor.settingEnabled())
                    throw new IllegalStateException("Ative o sensor arcano antes da percepção próxima.");
                NexusArcaneSensor.scanNearby(on);
                ClientLink.sendArcaneState();
            }
            case "reactions" -> {
                if(on&&(!ClientConfig.allowChat||!ClientConfig.allowMind||!ClientConfig.allowAutonomy))
                    throw new IllegalStateException("Reações exigem Chat, Mente e Autonomia.");
                NexusArcaneSensor.reactions(on);
                ClientLink.sendArcaneState();
            }
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

    /** One operation for players who have already agreed to be heard. */
    static void enableDialogue(){
        // Validate privacy and audio dependency before enabling publication.
        NexusHearing.listen(true);
        ClientConfig.allowChat=true;
        ClientConfig.allowMind=true;
        ClientConfig.allowAutonomy=true;
        ClientConfig.save();
        NexusHearing.answer(true);
        ClientLink.sendHearingState();
    }

    static String summary(){
        return (ClientConfig.token.isBlank()?"Render: SEM PAREAMENTO":"Render: PAREADO")+
            " | Plasmo: "+(NexusHearing.plasmoAvailable()?"OK":"INDISPONÍVEL")+
            " | Consentidos online: "+NexusHearing.authorizedOnlineCount();
    }
}
