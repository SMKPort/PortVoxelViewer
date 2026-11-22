package com.port.nexusclient;

/** Reproduces the channel regression independently of Minecraft/NeoForge. */
public final class NexusChatRoutingTest {
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    private static void rejects(Runnable request,String reason){
        try{request.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError(reason);
    }
    public static void main(String[] args){
        for(String command:new String[]{"/rp","/f chat","/somake:rp","/nexusconversa"}){
            String expected=command.substring(1);
            var out=NexusChatRouting.prepare(
                "[Nexus Aterna] Desintegrando armadura.",
                "custom",command,"&6&l","&b");
            check(out.command().equals(expected),"The custom command must remain unchanged: "+command);
            check(!out.plainChat(),"Custom route must call sendCommand.");
            check(out.text().equals("&6&l[Nexus Aterna] &r&bDesintegrando armadura."),
                "Custom route must apply independent prefix and body styles.");
        }
        var noSlash=NexusChatRouting.prepare(
            "[Nexus Aterna] Teste.","custom","f chat","","");
        check(noSlash.command().equals("f chat"),"Unprefixed fixed subcommand must work.");
        check(noSlash.text().equals("[Nexus Aterna] Teste."),
            "Unstyled custom messages must remain plain.");
        var local=NexusChatRouting.prepare("[Nexus Aterna] Boa tarde!",
            "local","ignored","&6&l","&f");
        check(local.command().equals("l")&&
            local.text().equals("&6&l[Nexus Aterna] &r&fBoa tarde!"),
            "Local route must apply both independent styles.");
        var global=NexusChatRouting.prepare("[Nexus Aterna] Bom dia!",
            "global","","&6","&7");
        check(global.plainChat()&&global.command().isEmpty()&&
            global.text().equals("&6[Nexus Aterna] &r&7Bom dia!"),
            "Global route must use sendChat with owner-selected styles.");
        var legacy=NexusChatRouting.prepare("[Nexus Aterna] Teste.",
            "local","l","&6&l","");
        check(legacy.text().equals("&6&l[Nexus Aterna] Teste."),
            "Legacy local chat must preserve its formatting.");
        for(String unsafe:new String[]{"/op","minecraft:op","/execute run",
            "/ban player","/f chat;kill","/f @a","/f\nchat",
            "/f chat && op","/f chat \${message}","/lp user"}){
            rejects(()->NexusChatRouting.custom(unsafe),
                "Administrative or malformed custom command accepted: "+unsafe);
        }
        rejects(()->NexusChatRouting.prepare("[Nexus Aterna] Texto",
            "custom","/rp","&f;op","&f"),"Unsafe style must be blocked.");
        rejects(()->NexusChatRouting.prepare("[Nexus Aterna] Texto",
            "custom","/rp","&6","&f|op"),"Unsafe body style must be blocked.");
        rejects(()->NexusChatRouting.prepare("[Nexus Aterna] Texto",
            "outro","/rp","",""),"Unknown channel must be blocked.");
        rejects(()->NexusChatRouting.prepare("[Nexus Aterna] "+"x".repeat(225),
            "custom","/f chat","&6&l","&f"),
            "Minecraft 256-character packet budget must be respected.");
        System.out.println("NexusChatRoutingTest OK");
    }
}
