package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Runs only in the Minecraft client. Network I/O happens in HttpClient futures,
 * while packet sends, player reads and particles happen on the Minecraft thread.
 */
final class ClientLink {
    private static final Gson JSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8)).build();
    private static final AtomicBoolean POLLING = new AtomicBoolean(false);
    private static final NexusCombatAlerts COMBAT_ALERTS = new NexusCombatAlerts();
    private static int ticks;
    private static float previousHealth = Float.NaN;
    private static String previousEffects = "";
    private static String previousPlayer = "";
    private static long lastChatAt;
    private static boolean invalidTokenNotice;

    private ClientLink() {}

    static void reset() {
        ticks=0;previousHealth=Float.NaN;previousEffects="";previousPlayer="";
        NexusMind.resetSession();COMBAT_ALERTS.reset();NexusHearing.resetSession();NexusArcaneSensor.resetSession();NexusPrivateMind.resetSession();
        invalidTokenNotice=false;POLLING.set(false);
    }

    static void resetTelemetryBaseline(){
        previousHealth=Float.NaN;previousEffects="";previousPlayer="";COMBAT_ALERTS.reset();
    }
    static String status() {
        return (ClientConfig.token.isEmpty()?"SEM PAREAMENTO":"PAREADO")
            +" | Chat remoto: "+(ClientConfig.allowChat?"AUTORIZADO":"BLOQUEADO")
            +" | Telemetria: "+(ClientConfig.allowTelemetry?"ATIVA":"DESATIVADA")
            +" | Consciência: "+(ClientConfig.allowMind?"ATIVA":"DESATIVADA")
            +" | Autonomia RP: "+(ClientConfig.allowAutonomy?"AUTORIZADA":"BLOQUEADA")
            +" | Privado: "+(NexusPrivateMind.enabled()?"ATIVO":"OFF")
            +" | Serviço: "+ClientConfig.url+" | "+NexusVitals.status()+" | "+NexusHearing.status();
    }

    static void pair(String code) {
        if (!code.matches("[A-F0-9]{10}")) {
            NexusClientMod.notifyPlayer("Código inválido. Gere um novo código de dez caracteres no painel Nexus RP.");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            NexusClientMod.notifyPlayer("Entre em um mundo antes de parear o Nexus.");
            return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("code",code);
        data.addProperty("player",mc.player.getGameProfile().getName());
        NexusClientMod.notifyPlayer("Vinculando o Nexus por HTTPS...");
        post("/api/client/pair/exchange",data,false).thenAccept(res -> mc.execute(() -> {
            if(res!=null && res.has("token") && res.get("ok").getAsBoolean()){
                ClientConfig.token=res.get("token").getAsString();
                ClientConfig.allowChat=false;ClientConfig.allowTelemetry=false;
                ClientConfig.save();reset();
                NexusClientMod.notifyPlayer("Vinculado! Ative /nexusclient chat on e /nexusclient telemetry on se desejar.");
            } else {
                NexusClientMod.notifyPlayer("Falha no pareamento: "+errorText(res));
            }
        })).exceptionally(err -> {mc.execute(() ->
            NexusClientMod.notifyPlayer("Não foi possível alcançar o painel HTTPS. Verifique sua conexão."));
            return null;});
    }

    static void tick() {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.getConnection()==null){
            previousHealth=Float.NaN;previousPlayer="";previousEffects="";
            NexusMind.resetSession();COMBAT_ALERTS.reset();NexusHearing.resetSession();NexusArcaneSensor.resetSession();return;
        }
        NexusHearing.updateOnlinePlayers(mc.getConnection().getOnlinePlayers().stream()
            .map(info->info.getProfile().getName()).toList());
        NexusHearing.tick();
        ++ticks;
        if(ticks%2==0)NexusArcaneSensor.sample(mc.player);
        if(ticks%60!=0)return;
        NexusMind.sample(mc.player);
        if(ClientConfig.allowTelemetry)sendTelemetry(mc.player);
        if(ClientConfig.token.isEmpty())return;
        if(ticks%180==0)sendAlertState();
        if(ticks%120==0){sendHearingState();sendArcaneState();}
        if(POLLING.compareAndSet(false,true)){
            get("/api/client/poll",true).thenAccept(reply -> {
                if(reply!=null && reply.has("jobs")){
                    JsonArray jobs=reply.getAsJsonArray("jobs");
                    mc.execute(() -> {
                        for(var e:jobs){
                            try{
                                JsonObject j=e.getAsJsonObject();
                                execute(j);
                            }catch(Exception exception){
                                NexusClientMod.notifyPlayer("Erro na ação remota: "+exception.getMessage());
                            }
                        }
                    });
                }
            }).exceptionally(err -> null).whenComplete((value,err) -> POLLING.set(false));
        }
        if(ClientConfig.allowTelemetry){
            if(NexusVitals.isConfigured()){
                post("/api/client/monitor/state",NexusVitals.snapshot(),true).exceptionally(err -> null);
            }
            // Envio de contexto de personagem somente quando observação E
            // telemetria foram autorizadas pelo jogador, a cada ~30 segundos.
            if(ClientConfig.allowMind && ticks%600==0){
                post("/api/client/mind",NexusMind.snapshot(),true).exceptionally(err -> null);
            }
        }
    }

    private static void sendTelemetry(Player player){
        String name=player.getGameProfile().getName();
        float hp=player.getHealth();
        String effects=player.getActiveEffects().stream()
            .map(MobEffectInstance::getDescriptionId)
            .sorted().collect(Collectors.joining(","));
        if(!name.equals(previousPlayer)){
            previousPlayer=name;previousHealth=Float.NaN;previousEffects="";COMBAT_ALERTS.reset();
        }
        if(Float.isNaN(previousHealth)){
            event("health",name,0,hp,"Primeira leitura do cliente");
        }else if(previousHealth>0 && hp<=0){
            observeCombat("death",0,hp,player,name);
            event("death",name,0,0,"Morte detectada no cliente");
        }else if(previousHealth<=0 && hp>0){
            observeCombat("respawn",0,hp,player,name);
            event("respawn",name,0,hp,"Renascimento detectado no cliente");
        }else if(hp<previousHealth-.1f){
            float damage=previousHealth-hp;
            observeCombat("damage",damage,hp,player,name);
            event("damage",name,damage,hp,"Dano observado no cliente");
        }else if(hp>previousHealth+.1f){
            float healed=hp-previousHealth;
            observeCombat("heal",healed,hp,player,name);
            event("heal",name,healed,hp,"Regeneração observada no cliente");
        }
        if(!effects.equals(previousEffects)){
            event("effect",name,0,hp,effects.isEmpty()?"Nenhum efeito ativo":effects);
        }
        previousHealth=hp;previousEffects=effects;
    }

    private static void observeCombat(String type,float amount,float hp,Player player,String name){
        NexusPrivateMind.noticeCombat(type,hp/Math.max(1,player.getMaxHealth()));
        double before=NexusVitals.integrity();
        NexusVitals.record(type,amount,name);
        double after=NexusVitals.integrity();
        if(!ClientConfig.allowMind||!ClientConfig.allowAutonomy||!ClientConfig.allowChat)return;
        long now=System.currentTimeMillis();
        NexusCombatAlerts.Decision alert=COMBAT_ALERTS.observe(
            type,amount,hp,player.getMaxHealth(),before,after,
            NexusMind.stageIndex(),NexusVitals.isEnabled(),now,
            NexusAlertSettings::allows);
        if(alert!=null&&speak(alert.message,alert.kind))
            COMBAT_ALERTS.delivered(alert,now,NexusAlertSettings.cooldownMillis());
    }

    private static void event(String type,String name,float amount,float health,String note){
        JsonObject data=new JsonObject();
        data.addProperty("type",type);data.addProperty("player",name);
        data.addProperty("health",health);data.addProperty("amount",amount);
        data.addProperty("note",note.substring(0,Math.min(note.length(),140)));
        if(!ClientConfig.token.isEmpty())
            post("/api/client/telemetry",data,true).exceptionally(err -> null);
    }

    static void transcribeHearing(long clipId,String speaker,byte[] wav,List<String> allowedPlayers){
        if(ClientConfig.token.isBlank()){
            NexusHearing.onTranscriptionError(clipId,"Nexus não está pareado com a Render.",true);
            return;
        }
        JsonObject data=new JsonObject();
        data.addProperty("speaker",speaker);
        data.addProperty("audio",java.util.Base64.getEncoder().encodeToString(wav));
        data.addProperty("answer",false); // Uses local memory search after transcription.
        data.add("consentedPlayers",JSON.toJsonTree(allowedPlayers));
        Minecraft mc=Minecraft.getInstance();
        post("/api/client/hearing/transcribe",data,true,Duration.ofSeconds(45))
            .thenAccept(result -> mc.execute(() -> {
                if(result.has("ok")&&result.get("ok").getAsBoolean()&&result.has("transcript")){
                    NexusHearing.onTranscription(clipId,result.get("transcript").getAsString());
                }else{
                    String error=errorText(result);
                    String normalized=error.toLowerCase(java.util.Locale.ROOT);
                    boolean retry=normalized.contains("limite")||normalized.contains("aguarde")||
                        normalized.contains("ocupad")||normalized.contains("indispon")||
                        normalized.contains("tempo")||normalized.contains("http 5");
                    NexusHearing.onTranscriptionError(clipId,error,retry);
                }
            }))
            .exceptionally(error -> {
                mc.execute(() -> NexusHearing.onTranscriptionError(
                    clipId,"Serviço de transcrição sem resposta.",true));
                return null;
            });
    }

    static void replyHearing(String speaker,String utterance,List<String> memories){
        if(ClientConfig.token.isBlank())return;
        JsonObject payload=new JsonObject();
        payload.addProperty("speaker",speaker);
        payload.addProperty("transcript",utterance);
        payload.add("memories",JSON.toJsonTree(memories));
        Minecraft mc=Minecraft.getInstance();
        post("/api/client/hearing/reply",payload,true,Duration.ofSeconds(40))
            .thenAccept(result -> mc.execute(() -> {
                if(result.has("ok")&&result.get("ok").getAsBoolean()&&result.has("reply")){
                    NexusHearing.onReply(speaker,result.get("reply").getAsString());
                }else{
                    NexusClientMod.notifyPlayer("Audição: "+errorText(result));
                }
            }))
            .exceptionally(error -> {
                mc.execute(() -> NexusClientMod.notifyPlayer(
                    "Audição: não foi possível gerar uma resposta."));
                return null;
            });
    }

    static void synthesizeNexus(String text,int stage,String voice){
        if(ClientConfig.token.isEmpty()){
            NexusSpeaker.failed("Pareamento HTTPS necessário.");
            return;
        }
        JsonObject data=new JsonObject();
        data.addProperty("text",text);
        data.addProperty("stage",stage);
        data.addProperty("voice",voice);
        post("/api/client/tts",data,true,Duration.ofSeconds(48))
            .thenAccept(NexusSpeaker::accept)
            .exceptionally(error -> {NexusSpeaker.failed("Serviço de voz indisponível.");return null;});
    }

    static void askPrivateNexus(String text,JsonArray history,JsonObject traits,String stance){
        JsonObject request=new JsonObject();
        request.addProperty("consent",true);
        request.addProperty("question",text);
        request.add("history",history);
        request.add("traits",traits);
        request.addProperty("stance",stance);
        Minecraft mc=Minecraft.getInstance();
        post("/api/client/private/reply",request,true,Duration.ofSeconds(40))
            .thenAccept(result->mc.execute(()->{
                if(result!=null&&result.has("ok")&&result.get("ok").getAsBoolean()&&result.has("reply"))
                    NexusPrivateMind.answered(result.get("reply").getAsString());
                else NexusPrivateMind.failed(errorText(result));
            }))
            .exceptionally(error->{
                mc.execute(()->NexusPrivateMind.failed("Nexus privado: serviço de IA indisponível."));
                return null;
            });
    }

    static void sendArcaneState(){
        if(ClientConfig.token.isEmpty())return;
        post("/api/client/arcane/state",NexusArcaneSensor.snapshot(),true)
            .exceptionally(error->null);
    }

    static void sendHearingState(){
        if(ClientConfig.token.isEmpty())return;
        post("/api/client/hearing/state",NexusHearing.snapshot(),true).exceptionally(err -> null);
    }

    private static void sendAlertState(){
        if(ClientConfig.token.isEmpty())return;
        JsonObject data=NexusAlertSettings.snapshot();
        data.add("history",NexusAlertHistory.snapshot());
        post("/api/client/alerts/state",data,true).exceptionally(err -> null);
    }

    private static void execute(JsonObject job){
        String id=job.get("id").getAsString(),kind=job.get("type").getAsString();
        JsonObject payload=job.getAsJsonObject("payload");
        String output="";String error="";boolean success=false;
        try{
            if("chat".equals(kind)){
                output=chat(payload.get("message").getAsString(),
                    payload.has("channel")?payload.get("channel").getAsString():"local",
                    payload.has("command")?payload.get("command").getAsString():"",
                    payload.has("format")?payload.get("format").getAsString():"&6&l");success=true;
            }else if("mind_settings".equals(kind)){
                NexusSocial.configure(payload.getAsJsonObject("social"));
                NexusSpeaker.configure(payload.getAsJsonObject("speaker"));
                sendHearingState();
                output="Nexus Mind 2.0 configurado localmente.";
                success=true;
            }else if("mind_memory".equals(kind)){
                String actionName=payload.get("action").getAsString();
                if("delete".equals(actionName)){
                    NexusSocial.deleteFact(payload.get("id").getAsLong());
                    output="Lembrança semântica apagada.";
                }else if("correct".equals(actionName)){
                    NexusSocial.correctFact(payload.get("id").getAsLong(),payload.get("text").getAsString());
                    output="Lembrança corrigida no Nexus.";
                }else if("forget_player".equals(actionName)){
                    String who=payload.get("player").getAsString();
                    long removed=NexusHearing.forgetSpeaker(who);
                    output=removed+" transcrições e vínculos apagados de "+who+".";
                }else if("clear".equals(actionName)&&payload.has("confirm")&&payload.get("confirm").getAsBoolean()){
                    NexusHearing.clearMemory();
                    output="Todas as memórias sociais e transcrições apagadas.";
                }else throw new IllegalArgumentException("Comando de memória não autorizado.");
                sendHearingState();
                success=true;
            }else if("arcane_settings".equals(kind)){
                NexusArcaneSensor.configure(payload);
                sendArcaneState();
                output="Núcleo Arcano configurado no cliente.";
                success=true;
            }else if("hearing_settings".equals(kind)){
                NexusHearing.configure(payload);
                sendHearingState();
                output="Audição salva no Minecraft (sem armazenar áudio).";
                success=true;
            }else if("alerts_settings".equals(kind)){
                NexusAlertSettings.configure(payload);
                sendAlertState();
                output="Alertas atualizados e salvos no Nexus Client.";
                success=true;
            }else if("monitor_settings".equals(kind)){
                Minecraft mc=Minecraft.getInstance();
                if(mc.player==null)throw new IllegalStateException("Entre no mundo para configurar o monitor.");
                output=NexusVitals.configure(payload,mc.player.getGameProfile().getName());
                success=true;
            }else if("action".equals(kind)){
                output=action(payload.get("action").getAsString(),
                    payload.has("form")?payload.get("form").getAsString():"");success=true;
            }else error="Tipo de ação não autorizado.";
        }catch(Exception ex){error=ex.getMessage()==null?"Ação não executada.":ex.getMessage();}
        JsonObject ack=new JsonObject();
        ack.addProperty("id",id);ack.addProperty("success",success);
        ack.addProperty("output",output);ack.addProperty("error",error);
        post("/api/client/ack",ack,true).exceptionally(err -> null);
    }

    static boolean speak(String text){return speak(text,"mind");}
    static boolean speak(String text,String kind) {
        if(!ClientConfig.allowMind || !ClientConfig.allowAutonomy || !ClientConfig.allowChat ||
            !NexusAlertSettings.allows(kind))return false;
        String safe=text.replaceAll("[\\p{Cntrl}]", " ").trim();
        if(safe.isEmpty())return false;
        try {
            chat("[Nexus Aterna] "+safe,"local","l","&6&l",false);
            NexusAlertHistory.record(kind,safe);
            NexusSpeaker.onSpeech(safe,NexusMind.stageIndex());
            if(!ClientConfig.token.isEmpty())sendAlertState();
            return true;
        }
        catch(IllegalArgumentException | IllegalStateException ignored) { return false; }
    }

    private static String chat(String message,String channel,String command,String format){
        return chat(message,channel,command,format,true);
    }

    private static String chat(String message,String channel,String command,String format,boolean announce){
        Minecraft mc=Minecraft.getInstance();
        if(!ClientConfig.allowChat)
            throw new IllegalStateException("Chat remoto não autorizado. Use /nexusclient chat on.");
        if(mc.player==null||mc.getConnection()==null)
            throw new IllegalStateException("Você precisa estar conectado a um mundo.");
        message=message.replaceAll("[\\p{Cntrl}]", " ").trim();
        if(message.isEmpty()||message.length()>230||message.startsWith("/"))
            throw new IllegalArgumentException("Mensagem inválida ou comando bloqueado.");
        long now=System.currentTimeMillis();
        if(now-lastChatAt<4500)
            throw new IllegalStateException("Proteção contra spam. Aguarde alguns segundos.");
        // Only chat-channel roots are permitted, not arbitrary admin commands.
        final String root;
        if("local".equals(channel)){
            root="l";
        }else if("custom".equals(channel)){
            String candidate=command.toLowerCase(java.util.Locale.ROOT).trim();
            if(candidate.startsWith("/"))candidate=candidate.substring(1);
            if(!java.util.Set.of("l","local","g","global","rp","me","ooc","chat","ch","yell","shout","action").contains(candidate))
                throw new IllegalArgumentException("Canal de chat personalizado não autorizado.");
            root=candidate;
        }else if("global".equals(channel)){
            root="";
        }else{
            throw new IllegalArgumentException("Canal de chat inválido.");
        }
        // Format codes belong only to local chat, directly after "/l ".
        // They are inserted before the message, not interpreted by this client.
        if(format==null)format="";
        format=format.toLowerCase(java.util.Locale.ROOT);
        if(!format.matches("(?:&[0-9a-fk-or]){0,8}"))
            throw new IllegalArgumentException("Código de cor inválido.");
        String transmittedMessage=("local".equals(channel)?format:"")+message;
        lastChatAt=now;
        if(root.isEmpty()){
            mc.getConnection().sendChat(message);
        }else{
            // Use Minecraft's own client command API. Never fake T or keyboard input.
            // sendCommand takes the command without the leading slash.
            mc.getConnection().sendCommand(root+" "+transmittedMessage);
        }
        if(announce)NexusClientMod.notifyPlayer(root.isEmpty()?
            "Mensagem enviada no chat global.":
            "Mensagem enviada via /"+root+".");
        return "Mensagem enviada como "+mc.player.getGameProfile().getName()+
               (root.isEmpty()?" no chat global.":" pelo canal /"+root+".");
    }

    private static String action(String kind,String form){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)throw new IllegalStateException("Nenhum mundo aberto.");
        if(!List.of("integrar","desintegrar","remodelar","reparar","ritual","emote","emergencia").contains(kind))
            throw new IllegalArgumentException("Ação não autorizada.");
        String text=switch(kind){
            case "integrar" -> "As placas do Nexus se integram ao braço direito.";
            case "desintegrar" -> "As placas arcanas se dispersam.";
            case "remodelar" -> "Armadura remodelada: "+form.replaceAll("[^\\p{L}\\p{N} _-]","");
            case "reparar" -> "Pulso restaurador liberado.";
            case "ritual" -> "Canalização ritualística iniciada.";
            case "emote" -> "Emote solicitado. Vincule seu mod de emotes para uma animação real.";
            default -> "Protocolo de emergência ativado.";
        };
        NexusClientMod.notifyPlayer(text);
        // Particle projection exists only on this client, never modifies blocks
        // or other players and does not claim to animate Armourer's Workshop models.
        for(int i=0;i<22;i++){
            double angle=(Math.PI*2*i)/22d;
            double radius=.55+((i%4)*.12);
            mc.level.addParticle(ParticleTypes.ENCHANT,
                mc.player.getX()+Math.cos(angle)*radius,
                mc.player.getY()+.4+(i%8)*.18,
                mc.player.getZ()+Math.sin(angle)*radius,
                0,.03,0);
        }
        return "Efeito local: "+kind;
    }

    private static java.util.concurrent.CompletableFuture<JsonObject> get(String path,boolean auth){
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(ClientConfig.url+path))
            .timeout(Duration.ofSeconds(8)).GET();
        if(auth)builder.header("X-Nexus-Client",ClientConfig.token);
        return HTTP.sendAsync(builder.build(),HttpResponse.BodyHandlers.ofString())
            .thenApply(ClientLink::parse);
    }

    private static java.util.concurrent.CompletableFuture<JsonObject> post(String path,JsonObject data,boolean auth){
        return post(path,data,auth,Duration.ofSeconds(8));
    }
    private static java.util.concurrent.CompletableFuture<JsonObject> post(
        String path,JsonObject data,boolean auth,Duration timeout){
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(ClientConfig.url+path))
            .timeout(timeout).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(data)));
        if(auth)builder.header("X-Nexus-Client",ClientConfig.token);
        return HTTP.sendAsync(builder.build(),HttpResponse.BodyHandlers.ofString())
            .thenApply(ClientLink::parse);
    }

    private static JsonObject parse(HttpResponse<String> response){
        try{
            JsonObject parsed=JsonParser.parseString(response.body()).getAsJsonObject();
            if(response.statusCode()==401 && !ClientConfig.token.isEmpty() && !invalidTokenNotice){
                invalidTokenNotice=true;
                Minecraft.getInstance().execute(() ->
                    NexusClientMod.notifyPlayer("Sessão expirada. Gere outro código de pareamento no Nexus RP."));
            }
            return parsed;
        }catch(Exception exception){return new JsonObject();}
    }

    private static String errorText(JsonObject response){
        if(response!=null&&response.has("error"))return response.get("error").getAsString();
        return "código expirado ou resposta inválida.";
    }
}
