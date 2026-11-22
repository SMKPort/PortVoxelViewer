package com.port.nexusclient;

import java.util.Locale;
import java.util.Set;

/**
 * Mirrors the Nexus RP server's fixed chat-channel syntax.
 * The command prefix is chosen in the owner's settings, never by Gemini.
 * A deny-list prevents using administrative/server commands as chat channels.
 * All other commands still need to exist and be permitted on the game server.
 */
final class NexusChatRouting {
    private static final String LABEL="[Nexus Aterna]";
    private static final Set<String> BLOCKED_ROOTS=Set.of(
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
    private static final String COMMAND_PATTERN="[a-z0-9_.:-]+(?: [a-z0-9_.:-]+){0,2}";
    private static final String STYLE_PATTERN="(?:&[0-9a-fk-or]){0,8}";
    record Outgoing(String command,String text,boolean plainChat){}
    private NexusChatRouting(){}

    static String custom(String value){
        if(value==null)throw new IllegalArgumentException("Informe o comando do canal personalizado.");
        String candidate=value.trim().toLowerCase(Locale.ROOT);
        if(candidate.startsWith("/"))candidate=candidate.substring(1);
        if(candidate.isEmpty()||candidate.length()>64||!candidate.matches(COMMAND_PATTERN))
            throw new IllegalArgumentException(
                "Comando inválido. Use /rp, /f chat ou /somake:rp (sem a mensagem).");
        String root=candidate.split(" ")[0];
        String bare=root.substring(root.lastIndexOf(':')+1);
        if(BLOCKED_ROOTS.contains(root)||BLOCKED_ROOTS.contains(bare))
            throw new IllegalArgumentException("Comando administrativo bloqueado como canal de chat.");
        return candidate;
    }
    private static String style(String value,String name){
        String checked=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        if(!checked.matches(STYLE_PATTERN))
            throw new IllegalArgumentException("Código de cor inválido: "+name);
        return checked;
    }
    static Outgoing prepare(String message,String channel,String command,
                            String prefixFormat,String bodyFormat){
        if(message==null)throw new IllegalArgumentException("Mensagem vazia.");
        String safe=message.replaceAll("[\\p{Cntrl}]"," ").trim();
        if(safe.isEmpty()||safe.length()>230||safe.startsWith("/"))
            throw new IllegalArgumentException("Mensagem inválida ou longa demais.");
        final String root;
        if("local".equals(channel))root="l";
        else if("custom".equals(channel))root=custom(command);
        else if("global".equals(channel))root="";
        else throw new IllegalArgumentException("Canal de chat inválido.");
        String before=style(prefixFormat,"prefixo");
        String after=style(bodyFormat,"texto");
        // Only the owner-facing chat label is styled; the narrative body has
        // its own independent color and must not inherit bold unless requested.
        String out;
        if(safe.startsWith(LABEL)&&safe.length()>LABEL.length()
            &&Character.isWhitespace(safe.charAt(LABEL.length()))){
            String body=safe.substring(LABEL.length()).trim();
            out=before+LABEL+" "+(after.isEmpty()?"":"&r"+after)+body;
        }else{
            // Older relay clients can supply a message without the label.
            out=before+safe;
            if(!after.isEmpty())out=before+LABEL+" &r"+after+
                (safe.startsWith(LABEL)?safe.substring(LABEL.length()).trim():safe);
        }
        // Minecraft command argument length is capped; the relay also verifies
        // this, but check again for old or mismatched site deployments.
        if(out.length()+(root.isEmpty()?0:root.length()+1)>256)
            throw new IllegalArgumentException(
                "Mensagem longa para o canal. Reduza o texto ou as cores.");
        return new Outgoing(root,out,root.isEmpty());
    }
}
