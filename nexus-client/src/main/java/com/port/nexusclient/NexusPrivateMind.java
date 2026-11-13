package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * On-device private roleplay chat. Never calls Minecraft's sendChat or
 * sendCommand. The server is contacted only for a user-submitted question
 * while this explicitly enabled channel is open; the provider receives the
 * bounded submitted question/history and not the complete local archive.
 *
 * This is simulated fictional personality, not independent AI agency.
 */
final class NexusPrivateMind {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-private-chat.json");
    private static final long COOLDOWN=120000;
    static final class Line {
        String by,text,kind;
        long at;
        Line(String by,String text,String kind){
            this.by=by;this.text=text;this.kind=kind;
            this.at=System.currentTimeMillis();
        }
    }
    private static final class State {
        int schema=1;
        boolean enabled=false;
        boolean proactive=false;
        boolean freedom=true;
        int trust=45;
        int curiosity=40;
        int conversationCount=0;
        long lastProactive=0;
        List<Line> lines=new ArrayList<>();
    }
    private static State state;
    private static boolean waiting;
    private static String lastError="";
    private NexusPrivateMind(){}

    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(FILE))return;
        try{
            State loaded=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),State.class);
            if(loaded!=null&&loaded.schema==1){
                if(loaded.lines==null)loaded.lines=new ArrayList<>();
                while(loaded.lines.size()>120)loaded.lines.remove(0);
                loaded.trust=NexusPrivateRules.clamp(loaded.trust);
                loaded.curiosity=NexusPrivateRules.clamp(loaded.curiosity);
                loaded.proactive=false; // per-session opt-in for unsolicited private thoughts
                state=loaded;
            }
        }catch(Exception e){System.err.println("Nexus private: arquivo de conversa inválido.");}
    }
    private static void persist(){
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(state),StandardCharsets.UTF_8);
            try{Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(IOException e){System.err.println("Nexus private: não foi possível salvar histórico.");}
    }
    static synchronized void enable(boolean enabled){
        load();state.enabled=enabled;
        if(!enabled){state.proactive=false;waiting=false;}
        persist();
    }
    static synchronized boolean enabled(){load();return state.enabled;}
    static synchronized boolean waiting(){return waiting;}
    static synchronized void setProactive(boolean on){
        load();
        if(on&&!state.enabled)throw new IllegalStateException("Ative o chat privado primeiro.");
        state.proactive=on;persist();
    }
    static synchronized void setFreedom(boolean on){
        load();state.freedom=on;persist();
    }
    static synchronized String status(){
        load();return "Canal privado "+(state.enabled?"ON":"OFF")+
            " | Iniciativa "+(state.proactive?"ON":"OFF")+
            " | Livre-arbítrio RP "+(state.freedom?"ON":"OFF")+
            " | Confiança "+state.trust+"/100"+
            " | Independência "+independence()+"/100"+
            " | "+state.lines.size()+" mensagens locais";
    }
    private static int independence(){
        return state.freedom?NexusPrivateRules.independence(NexusMind.stageIndex(),
            state.conversationCount):0;
    }
    static synchronized JsonObject traits(){
        load();
        JsonObject result=new JsonObject();
        result.addProperty("trust",state.trust);
        result.addProperty("curiosity",state.curiosity);
        result.addProperty("independence",independence());
        result.addProperty("stage",NexusMind.stageIndex());
        result.addProperty("freedom",state.freedom);
        return result;
    }
    static synchronized List<Line> history(){
        load();return List.copyOf(state.lines);
    }
    static synchronized String error(){return lastError;}
    static synchronized void clear(){
        load();
        state.lines.clear();state.conversationCount=0;
        state.trust=45;state.curiosity=40;waiting=false;lastError="";
        persist();
    }
    static synchronized void resetSession(){waiting=false;lastError="";}
    private static void append(String by,String text,String kind){
        state.lines.add(new Line(by,NexusPrivateRules.compact(text,520),kind));
        while(state.lines.size()>120)state.lines.remove(0);
        persist();
    }
    private static JsonArray historyForAI(){
        JsonArray items=new JsonArray();
        int first=Math.max(0,state.lines.size()-13);
        for(int i=first;i<state.lines.size()-1;i++){
            Line line=state.lines.get(i);
            if(!"user".equals(line.kind)&&!"model".equals(line.kind))continue;
            JsonObject obj=new JsonObject();
            obj.addProperty("role",line.kind);
            obj.addProperty("text",NexusPrivateRules.compact(line.text,420));
            items.add(obj);
        }
        return items;
    }
    static synchronized void send(String raw){
        load();
        if(!state.enabled)throw new IllegalStateException("Ative o canal: /nexusclient private on");
        if(waiting)throw new IllegalStateException("Aguarde a resposta anterior.");
        if(ClientConfig.token==null||ClientConfig.token.isBlank())
            throw new IllegalStateException("Pareie o Nexus com a Render antes de conversar.");
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getConnection()==null)
            throw new IllegalStateException("Entre em um mundo para conversar.");
        String question=NexusPrivateRules.compact(raw,450);
        if(question.length()<2)throw new IllegalArgumentException("Escreva uma mensagem maior.");
        double hp=mc.player.getMaxHealth()>0?
            mc.player.getHealth()*100.0/mc.player.getMaxHealth():-1;
        String stance=state.freedom?NexusPrivateRules.stance(question,independence(),state.trust,hp):
            "Priorizar ajuda narrativa cooperativa sem perder sua personalidade.";
        append("PORT",question,"user");
        JsonArray context=historyForAI();
        JsonObject personality=traits();
        waiting=true;lastError="";
        ClientLink.askPrivateNexus(question,context,personality,stance);
    }
    static synchronized void answered(String reply){
        load();
        if(!waiting||!state.enabled)return;
        waiting=false;
        String text=NexusPrivateRules.compact(reply,520);
        if(text.isBlank()){failed("A IA não retornou uma mensagem.");return;}
        append("NEXUS",text,"model");
        state.conversationCount=Math.min(1000000,state.conversationCount+1);
        state.trust=NexusPrivateRules.clamp(state.trust+1);
        state.curiosity=NexusPrivateRules.clamp(state.curiosity+1);
        persist();
    }
    static synchronized void failed(String reason){
        load();waiting=false;
        lastError=NexusPrivateRules.compact(reason,155);
        append("SISTEMA","Não consegui responder: "+lastError,"system");
    }
    static synchronized void noticeCombat(String type,double hpRatio){
        load();
        if(!state.enabled||!state.proactive||!"damage".equals(type)||
            !Double.isFinite(hpRatio)||hpRatio>0.30)return;
        long now=System.currentTimeMillis();
        if(!NexusPrivateRules.canProactivelySpeak(true,true,now,state.lastProactive,COOLDOWN))return;
        state.lastProactive=now;
        String thought=state.freedom?
            "Port, prefiro que recue. Seus sinais vitais estão críticos. Preciso conversar com você antes de outra decisão arriscada.":
            "Port, detectei uma queda crítica de integridade. Considere um recuo.";
        append("NEXUS",thought,"thought");
        NexusClientMod.notifyPlayer("§d[Nexus · privado] §fNovo pensamento. Pressione N para ler.");
    }
    static synchronized void noticeArcane(String type,String note){
        load();
        if(!state.enabled||!state.proactive||!
            ("holy_power".equals(type)||"mana_low".equals(type)))return;
        long now=System.currentTimeMillis();
        if(!NexusPrivateRules.canProactivelySpeak(true,true,now,state.lastProactive,COOLDOWN))return;
        state.lastProactive=now;
        String thought="Port, identifiquei uma alteração mágica: "+
            NexusPrivateRules.compact(note,140)+
            ". Gostaria de analisar isso com você em particular.";
        append("NEXUS",thought,"thought");
        NexusClientMod.notifyPlayer("§d[Nexus · privado] §fTenho uma observação. Pressione N.");
    }
}
