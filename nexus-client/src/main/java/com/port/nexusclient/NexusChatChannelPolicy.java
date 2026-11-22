package com.port.nexusclient;

import java.util.Locale;
import java.util.Set;

/**
 * Accept an owner-configured static chat command prefix. The text being sent
 * never supplies its own slash command or fixed command arguments.
 * The Minecraft server enforces all actual permissions.
 */
final class NexusChatChannelPolicy {
    private static final Set<String> BLOCKED=Set.of(
        "op","deop","ban","ban-ip","banlist","pardon","pardon-ip","kick",
        "stop","restart","reload","save-all","save-off","save-on","whitelist",
        "give","clear","effect","attribute","gamemode","defaultgamemode",
        "difficulty","gamerule","setblock","fill","clone","summon","kill",
        "tp","teleport","spreadplayers","execute","function","data","item",
        "loot","place","setworldspawn","spawnpoint","worldborder","weather",
        "time","experience","xp","advancement","bossbar","schedule","debug",
        "jfr","forceload","locate","seed","team","scoreboard","tag",
        "tellraw","title","playsound","stopsound","particle","say",
        "msg","tell","w","r","reply","minecraft:me","help","list",
        "permission","permissions","lp","luckperms","pex","promote","demote",
        "sudo","runas","console","rcon","plugin","plugins",
        "bukkit:plugins","version","bukkit:version"
    );
    private NexusChatChannelPolicy(){}

    static String custom(String raw){
        if(raw==null)throw new IllegalArgumentException(
            "Informe o comando de chat personalizado.");
        String value=raw.trim().toLowerCase(Locale.ROOT);
        if(value.startsWith("/"))value=value.substring(1);
        // Allows a single command and up to 2 fixed subcommands, no chat text,
        // JSON, selectors, semicolons, newlines or placeholders.
        if(value.isEmpty()||value.length()>64||
           !value.matches("[a-z0-9_.:-]+(?: [a-z0-9_.:-]+){0,2}"))
            throw new IllegalArgumentException(
                "Comando personalizado inválido. Ex.: /rp ou /f chat.");
        String root=value.split(" ",2)[0];
        String bare=root.substring(root.lastIndexOf(':')+1);
        if(BLOCKED.contains(root)||BLOCKED.contains(bare))
            throw new IllegalArgumentException(
                "Comando administrativo não permitido como canal de chat.");
        return value;
    }
    static String root(String channel,String command){
        return switch(channel){
            case "local" -> "l";
            case "global" -> "";
            case "custom" -> custom(command);
            default -> throw new IllegalArgumentException("Canal de chat inválido.");
        };
    }
    static String format(String channel,String formatting){
        // Always validate the channel, even when it has no slash prefix.
        if(!Set.of("local","custom","global").contains(channel))
            throw new IllegalArgumentException("Canal de chat inválido.");
        String value=formatting==null?"":formatting.trim().toLowerCase(Locale.ROOT);
        if(!value.matches("(?:&[0-9a-fk-or]){0,8}"))
            throw new IllegalArgumentException("Código de cor inválido.");
        return value;
    }

    static String decorate(String message,String prefixStyle,String bodyStyle){
        final String tag="[Nexus Aterna] ";
        if(bodyStyle.isEmpty())return prefixStyle+message; // legacy client compatibility
        // &r is essential: a plain &f after &l may not reset bold.
        if(!message.startsWith(tag))
            throw new IllegalArgumentException("Identificador do Nexus ausente.");
        return prefixStyle+"[Nexus Aterna] &r"+bodyStyle+message.substring(tag.length());
    }
}
