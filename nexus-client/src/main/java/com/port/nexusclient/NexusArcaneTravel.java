package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nexus 3.0 Arcane Travel. Local, explicit, two-step travel permission.
 * Never teleports directly, changes position or calls server admin commands.
 */
final class NexusArcaneTravel {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-waystones.json");
    private static final long REQUEST_MS=45_000L, CONFIRMED_MS=90_000L;
    private static final class Settings {
        int schema=1;
        Map<String,Map<String,String>> realms=new HashMap<>();
    }
    private record Pending(UUID id,String name,String realm,Object level,
                           long expiresAt,boolean confirmed) {}
    private static Settings settings;
    private static boolean readOnly;
    private static Pending pending;
    private static String status="Arcane Travel em espera.";
    private NexusArcaneTravel(){}

    private static synchronized void load(){
        if(settings!=null)return;
        settings=new Settings();
        if(!Files.isRegularFile(FILE))return;
        try{
            Settings saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Settings.class);
            if(saved==null || saved.schema!=1 || saved.realms==null)
                throw new IllegalArgumentException("Schema de favoritos desconhecido.");
            settings=saved;
        }catch(Exception e){
            // Do not erase original data on an unrecognized/corrupt file.
            readOnly=true;
            status="Favoritos inválidos: arquivo original preservado; edição bloqueada.";
        }
    }

    /** Realm-separated aliases; only a hash of a server address is persisted. */
    private static String realm(){
        Minecraft mc=Minecraft.getInstance();
        String label;
        if(mc.getCurrentServer()!=null)
            label="multiplayer:"+mc.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT);
        else if(mc.getSingleplayerServer()!=null)
            label="singleplayer:"+mc.getSingleplayerServer().getWorldData().getLevelName();
        else return "";
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256")
                .digest(label.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        }catch(Exception error){return "";}
    }

    private static synchronized Map<String,UUID> aliases(){
        load();
        Map<String,UUID> names=new HashMap<>();
        Map<String,String> in=settings.realms.get(realm());
        if(in!=null)for(var e:in.entrySet()){
            try{names.put(e.getKey(),UUID.fromString(e.getValue()));}
            catch(IllegalArgumentException ignored){}
        }
        return names;
    }

    private static synchronized void persist(){
        if(readOnly)throw new IllegalStateException("Arquivo de favoritos inválido.");
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(settings),StandardCharsets.UTF_8);
            try{Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception e){throw new IllegalStateException("Não consegui salvar os favoritos.",e);}
    }

    static synchronized List<NexusTravelPolicy.Destination> known(){
        return NexusWaystoneBridge.known();
    }

    static synchronized String status(){return status;}

    static synchronized List<String> favoritesSummary(){
        List<NexusTravelPolicy.Destination> known=known();
        Map<String,UUID> aliases=aliases();
        List<String> result=new ArrayList<>();
        for(var entry:aliases.entrySet()){
            String resolved=known.stream().filter(it->it.id().equals(entry.getValue()))
                .map(NexusTravelPolicy.Destination::name).findFirst()
                .orElse("(não disponível)");
            result.add(entry.getKey()+" = "+resolved);
        }
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    static synchronized String favorite(String alias,String targetQuery){
        if(!NexusWaystoneBridge.present())return "Waystones não instalado.";
        String key=NexusTravelPolicy.normalize(alias),scope=realm();
        if(key.length()<2||key.length()>36)return "Use um apelido de 2 a 36 caracteres.";
        if(scope.isEmpty())return "Entre em um mundo antes de salvar.";
        var found=NexusTravelPolicy.resolve(targetQuery,known(),aliases());
        if(!found.found())return found.error();
        load();
        if(readOnly)return status;
        settings.realms.computeIfAbsent(scope,it->new HashMap<>())
            .put(key,found.destination().id().toString());
        persist();
        return "Favorito '"+key+"' vinculado a "+found.destination().name()+".";
    }

    static synchronized String favoriteById(String alias,UUID destinationId){
        if(alias==null||destinationId==null)return "Favorito inválido.";
        String key=NexusTravelPolicy.normalize(alias),scope=realm();
        if(key.length()<2||key.length()>36||scope.isEmpty())return "Apelido/mundo inválido.";
        boolean available=known().stream().anyMatch(it->it.id().equals(destinationId));
        if(!available)return "Waystone não está na lista conhecida do cliente.";
        load();
        if(readOnly)return status;
        settings.realms.computeIfAbsent(scope,it->new HashMap<>())
            .put(key,destinationId.toString());
        persist();
        return "Favorito '"+key+"' salvo.";
    }

    static synchronized String removeFavorite(String alias){
        load();
        if(readOnly)return status;
        Map<String,String> found=settings.realms.get(realm());
        if(found==null||found.remove(NexusTravelPolicy.normalize(alias))==null)
            return "Favorito não encontrado.";
        persist();
        return "Favorito removido. Nenhuma Waystone foi modificada.";
    }

    static synchronized String request(String name){
        if(!NexusWaystoneBridge.present())return status="Waystones não instalado.";
        String scope=realm();
        if(scope.isEmpty())return status="Entre em um mundo para viajar.";
        var target=NexusTravelPolicy.resolve(name,known(),aliases());
        if(!target.found())return status=target.error();
        return requestDestination(target.destination());
    }

    static synchronized String requestDestination(NexusTravelPolicy.Destination target){
        Minecraft mc=Minecraft.getInstance();
        if(target==null||mc.level==null||mc.player==null||realm().isBlank())
            return status="Destino/mundo indisponível.";
        boolean stillKnown=known().stream().anyMatch(it->it.id().equals(target.id()));
        if(!stillKnown)return status="Waystone não está disponível no cliente.";
        pending=new Pending(target.id(),target.name(),realm(),mc.level,
            System.currentTimeMillis()+REQUEST_MS,false);
        return status="Solicitação para '"+target.name()+"'. Confirme com /nexusclient travel confirm "
            +"ou diga 'Nexus, confirmar viagem'.";
    }

    static synchronized String confirm(){
        Minecraft mc=Minecraft.getInstance();
        if(pending==null)return status="Nenhuma viagem aguardando confirmação.";
        long now=System.currentTimeMillis();
        if(now>pending.expiresAt()||!pending.realm().equals(realm())||mc.level!=pending.level()){
            pending=null;return status="Pedido expirou ou o mundo mudou.";
        }
        boolean stillKnown=known().stream().anyMatch(it->it.id().equals(pending.id()));
        if(!stillKnown){
            pending=null;return status="Waystone deixou de estar disponível. Viagem cancelada.";
        }
        pending=new Pending(pending.id(),pending.name(),pending.realm(),pending.level(),
            now+CONFIRMED_MS,true);
        if(mc.screen instanceof NexusTravelScreen) mc.setScreen(null);
        boolean menuRequested=false;
        if(!NexusWaystoneBridge.selectionMenuOpen())
            menuRequested=NexusWaystoneBridge.requestSafeInventoryMenu();
        status="Destino confirmado: "+pending.name()+". "
            +(menuRequested?"Abrindo a seleção oficial do Waystones.":
                "Abra uma Waystone ou item de viagem para selecionar o destino.");
        return status;
    }

    static synchronized String cancel(){
        pending=null;
        return status="Viagem cancelada pelo Port.";
    }

    static synchronized void tick(){
        if(pending==null || !pending.confirmed())return;
        Minecraft mc=Minecraft.getInstance();
        long now=System.currentTimeMillis();
        if(now>pending.expiresAt() || mc.level==null ||
           !pending.realm().equals(realm()) || mc.level!=pending.level()){
            pending=null;status="Viagem expirada ou mundo alterado.";return;
        }
        if(!NexusWaystoneBridge.selectionMenuOpen())return;
        boolean listed=NexusWaystoneBridge.menuContains(pending.id());
        if(!NexusTravelPolicy.maySelect(true,now<=pending.expiresAt(),
            pending.realm().equals(realm()),true,listed)){
            status="Destino não ofertado pelo menu do servidor. Viagem não enviada.";
            pending=null;
            NexusClientMod.notifyPlayer(status);
            return;
        }
        String destination=pending.name();
        boolean sent=NexusWaystoneBridge.chooseFromOpenMenu(pending.id());
        pending=null; // exactly one request; never retry blindly
        status=sent?"Teletransporte solicitado para "+destination+
            " (custos e permissões validados pelo servidor).":
            "Waystones recusou a seleção: "+NexusWaystoneBridge.lastError();
        NexusClientMod.notifyPlayer(status);
    }

    /** Called on Minecraft main thread. Vosk receives only a capped copy. */
    static synchronized List<String> voiceVocabulary(){
        if(!NexusWaystoneBridge.present())return List.of();
        LinkedHashSet<String> names=new LinkedHashSet<>();
        aliases().keySet().stream().sorted().forEach(names::add);
        for(var target:known())names.add(target.name());
        return NexusTravelPolicy.voicePhrases(names);
    }

    static void handleVoice(NexusTravelPolicy.VoiceIntent command){
        if(command==null)return;
        String message=switch(command.action()){
            case "request" -> request(command.destination());
            case "confirm" -> confirm();
            case "cancel" -> cancel();
            default -> "Comando de viagem desconhecido.";
        };
        NexusClientMod.notifyPlayer("[Arcane Travel] "+message);
    }

    static synchronized String pendingStatus(){
        return pending==null?"Nenhuma viagem pendente.":
            pending.name()+" | "+(pending.confirmed()?"CONFIRMADA":"AGUARDANDO CONFIRMAÇÃO");
    }
}
