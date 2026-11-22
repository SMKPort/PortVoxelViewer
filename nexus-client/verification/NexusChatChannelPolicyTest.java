package com.port.nexusclient;

public final class NexusChatChannelPolicyTest {
    private static void check(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }
    private static void invalid(Runnable callback,String reason){
        boolean rejected=false;
        try{callback.run();}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,reason);
    }
    public static void main(String[] args){
        check("somake:arcana".equals(NexusChatChannelPolicy.custom("/somake:arcana")),
            "custom server chat commands are not restricted to fixed whitelist");
        check("f chat".equals(NexusChatChannelPolicy.custom("/f chat")),
            "server chat subcommand may be statically configured");
        invalid(()->NexusChatChannelPolicy.custom("/op"),"no admin command");
        invalid(()->NexusChatChannelPolicy.custom("/minecraft:ban"),"no namespaced admin command");
        invalid(()->NexusChatChannelPolicy.custom("/f;op"),"no separators");
        invalid(()->NexusChatChannelPolicy.custom("/f @a"),"no selectors");
        invalid(()->NexusChatChannelPolicy.custom("/f chat\nop"),"no newlines");
        invalid(()->NexusChatChannelPolicy.custom("/execute run say"),"no execute");
        check("l".equals(NexusChatChannelPolicy.root("local","/op")),
            "local channel ignores unused custom command");
        check("".equals(NexusChatChannelPolicy.root("global","/op")),
            "global channel ignores unused custom command");
        check("&6&l".equals(NexusChatChannelPolicy.format("custom","&6&L")),
            "prefix formatting works on custom server-provided channel");
        check("&f".equals(NexusChatChannelPolicy.format("global","&f")),
            "global chat formatting not silently dropped");
        invalid(()->NexusChatChannelPolicy.format("custom","&c/kill"),
            "invalid embedded commands blocked");
        String tagged="[Nexus Aterna] Desintegrando armadura";
        String styled=NexusChatChannelPolicy.decorate(tagged,"&6&l","&f");
        check("&6&l[Nexus Aterna] &r&fDesintegrando armadura".equals(styled),
            "gold bold prefix must be reset before white message body");
        String blue=NexusChatChannelPolicy.decorate(tagged,"&6&l","&b");
        check("&6&l[Nexus Aterna] &r&bDesintegrando armadura".equals(blue),
            "body color independent from prefix color");
        String legacy=NexusChatChannelPolicy.decorate(tagged,"&6&l","");
        check("&6&l[Nexus Aterna] Desintegrando armadura".equals(legacy),
            "older clients remain compatible without separate body style");
        invalid(()->NexusChatChannelPolicy.decorate("Sem marcador","&6&l","&f"),
            "a styled reply must be identified as Nexus");
        System.out.println("NexusChatChannelPolicyTest OK");
    }
}
