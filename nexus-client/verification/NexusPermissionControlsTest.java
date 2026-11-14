package com.port.nexusclient;

public final class NexusPermissionControlsTest {
    private static void check(boolean ok,String why) {
        if(!ok)throw new AssertionError(why);
    }
    private static void expectedError(Runnable action,String why) {
        try{action.run();throw new AssertionError("Missing rejection: "+why);}
        catch(IllegalStateException expected){}
    }
    public static void main(String[] args){
        expectedError(()->NexusPermissionControls.set("autonomy",true),
            "autonomy needs chat and mind");
        NexusPermissionControls.set("chat",true);
        NexusPermissionControls.set("mind",true);
        NexusPermissionControls.set("autonomy",true);
        check(NexusPermissionControls.enabled("autonomy"),"Autonomy on");
        NexusPermissionControls.set("chat",false);
        check(!NexusPermissionControls.enabled("autonomy"),"Revoking chat disables autonomy");
        NexusPermissionControls.set("chat",true);
        NexusPermissionControls.set("autonomy",true);
        NexusPermissionControls.set("mind",false);
        check(!NexusPermissionControls.enabled("autonomy"),"Revoking Mind disables autonomy");

        try{
            NexusPermissionControls.set("hearing",true);
            throw new AssertionError("Retired audio permission still accepted.");
        }catch(IllegalArgumentException expected){}
        check(ClientConfig.allowMind==false,"Mind remains under user control.");

        NexusPrivateMind.messages=3;
        NexusPermissionControls.set("privateMemory",true);
        NexusPermissionControls.set("privateMemory",false);
        check(NexusPrivateMind.messages==3,
            "Permission toggle may not erase private history");
        NexusPermissionControls.set("speaker",true);
        check(NexusSpeaker.enabled,"Local TTS permission");
        for(String removed:new String[]{"arcane","nearby","reactions"}){
            try{
                NexusPermissionControls.set(removed,true);
                throw new AssertionError("Removed magic detection still available: "+removed);
            }catch(IllegalArgumentException expected){}
        }

        NexusPermissionControls.set("share",true);
        check(NexusSocial.share,"Sharing is a separate opt-in");
        NexusPermissionControls.set("share",false);
        check(!NexusSocial.share,"Sharing can be revoked");

        System.out.println("NexusPermissionControlsTest OK");
    }
}
final class ClientConfig {
    static boolean allowChat,allowTelemetry,allowMind,allowAutonomy;
    static String token="";
    static void save(){}
}
final class ClientLink {
    static void resetTelemetryBaseline(){}
    static void sendHearingState(){}
}
final class NexusMind {static void resetSession(){}}
final class NexusHearing {
    static boolean authorized,ready,listening,reply,memory,collective;
    static boolean listeningEnabled(){return listening;}
    static boolean replyEnabled(){return reply;}
    static boolean memoryEnabled(){return memory;}
    static boolean collectiveEnabled(){return collective;}
    static boolean plasmoAvailable(){return ready;}
    static int authorizedOnlineCount(){return authorized?1:0;}
    static void listen(boolean on) {
        if(on&&(!authorized||!ready))throw new IllegalStateException("Voice consent/Plasmo");
        listening=on;
    }
    static void answer(boolean on){reply=on;}
    static void remember(boolean on){memory=on;}
    static void selectCollective(boolean on) {
        if(on&&!authorized)throw new IllegalStateException("Consent required");
        collective=on;
    }
}
final class NexusPrivateMind {
    static boolean active,proactive,memory,freedom; static int messages;
    static boolean enabled(){return active;}
    static boolean autonomous(){return proactive;}
    static boolean memoryEnabled(){return memory;}
    static boolean freedomEnabled(){return freedom;}
    static void enable(boolean on){active=on;if(!on)proactive=false;}
    static void setProactive(boolean on){
        if(on&&!active)throw new IllegalStateException("Enable private first");
        proactive=on;
    }
    static void setMemoryWithoutClearing(boolean on){memory=on;}
    static void setFreedom(boolean on){freedom=on;}
}
final class NexusSpeaker {
    static boolean enabled;
    static boolean settingEnabled(){return enabled;}
    static void enable(boolean on){enabled=on;}
}
final class NexusSocial {
    static boolean diary=true,protection=true,share;
    static boolean diaryEnabled(){return diary;}
    static boolean protectionEnabled(){return protection;}
    static boolean sharingEnabled(){return share;}
    static void setting(String id,boolean on) {
        switch(id){
            case "diary" -> diary=on;
            case "protection" -> protection=on;
            case "share" -> share=on;
        }
    }
}
