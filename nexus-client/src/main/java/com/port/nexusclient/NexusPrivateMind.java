package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
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
 * A private roleplay channel on the user's Minecraft client. Never invokes
 * sendChat(), sendCommand(), or RCON. Only a user-submitted private question and
 * a bounded conversation window reach the paired HTTPS Gemini relay.
 * No server-side chat transcript is retained.
 */
final class NexusPrivateMind {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-private-mind.json");
    private static State state;
    private static boolean waiting;
    private static float lastLifeRatio=Float.NaN;
    private static long lastThought;
    private static final long THOUGHT_COOLDOWN=180_000L;
    static final class Message {
        String role,text,origin;
        long at;
        Message(String role,String text,String origin,long at){
            this.role=role;this.text=text;this.origin=origin;this.at=at;
        }
    }
    private static class State {
        int schema=1;
        boolean remember=false;
        boolean autonomy=false;
        boolean enabled=false;
        boolean freedom=false;
        int independence=35,trust=50,curiosity=50,caution=55;
        long privateTurns=0;
        List<Message> messages=new ArrayList<>();
    }
    private NexusPrivateMind(){}

    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(FILE))return;
        try{
            State stored=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),State.class);
            if(stored!=null&&stored.schema==1){
                if(stored.messages==null)stored.messages=new ArrayList<>();
                if(stored.messages.size()>150)
                    stored.messages=new ArrayList<>(stored.messages.subList(stored.messages.size()-150,stored.messages.size()));
                if(!stored.remember)stored.messages.clear();
                // Initiative requires renewed authorization after every client restart.
                stored.autonomy=false;
                stored.independence=clamp(stored.independence);
                stored.trust=clamp(stored.trust);
                stored.caution=clamp(stored.caution);
                stored.curiosity=clamp(stored.curiosity);
                state=stored;
            }
        }catch(Exception error){
            System.err.println("Nexus private: histórico inválido. Original preservado.");
        }
    }
    private static int clamp(int n){return Math.min(100,Math.max(0,n));}
    private static synchronized void save(){
        load();
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            State copy=new State();
            copy.autonomy=state.autonomy;copy.remember=state.remember;
            copy.enabled=state.enabled;copy.freedom=state.freedom;
            copy.independence=state.independence;copy.trust=state.trust;
            copy.curiosity=state.curiosity;copy.caution=state.caution;
            copy.privateTurns=state.privateTurns;
            if(state.remember)copy.messages=new ArrayList<>(state.messages);
            Files.writeString(temp,JSON.toJson(copy),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(IOException ex){
            System.err.println("Nexus private: não foi possível salvar as preferências.");
        }
    }
    static final class Line {
        final String kind,text;
        Line(String kind,String text){this.kind=kind;this.text=text;}
    }
    static synchronized List<Line> history(){
        load();
        List<Line> lines=new ArrayList<>();
        for(Message m:state.messages){
            String kind="observation".equals(m.origin)?"thought":m.role;
            lines.add(new Line(kind,m.text));
        }
        return List.copyOf(lines);
    }
    static synchronized boolean enabled(){load();return state.enabled;}
    static synchronized void enable(boolean on){
        load();state.enabled=on;
        if(!on){waiting=false;state.autonomy=false;}
        save();
    }
    static synchronized void setProactive(boolean on){
        load();
        if(on&&!state.enabled)throw new IllegalStateException("Ative o canal privado primeiro.");
        autonomy(on);
    }
    static synchronized void setFreedom(boolean on){
        load();state.freedom=on;
        if(on)state.independence=Math.max(40,state.independence);
        save();
    }
    static synchronized void send(String message){ask(message);}
    static synchronized void answered(String answer){if(enabled())reply(answer);else waiting=false;}
    static synchronized void failed(String reason){failure(reason);}

    static synchronized void memory(boolean enabled){
        load();state.remember=enabled;
        if(!enabled){state.messages.clear();waiting=false;}
        save();
    }
    static synchronized void autonomy(boolean enabled){
        load();state.autonomy=enabled;
        lastLifeRatio=Float.NaN;
        save();
    }
    static synchronized boolean autonomous(){load();return state.autonomy;}
    static synchronized boolean waiting(){return waiting;}
    static synchronized void clear(){
        load();
        state.messages.clear();
        waiting=false;
        save();
    }
    static synchronized String status(){
        load();
        return "Canal neural "+(state.enabled?"ON":"OFF")+" | Memória local "+(state.remember?"ON":"OFF")+
            " | iniciativa espontânea "+(state.autonomy?"ON":"OFF")+
            " | personalidade independente "+(state.freedom?"ON":"OFF")+
            " | independência "+state.independence+
            "/100 | histórico "+state.messages.size()+" mensagens";
    }
    static synchronized List<Message> messages(){
        load();return List.copyOf(state.messages);
    }
    private static void add(String role,String text,String source){
        load();
        String clean=NexusPrivateRules.safeText(text,"model".equals(role)?800:410);
        if(clean.isBlank())return;
        state.messages.add(new Message(role,clean,source,System.currentTimeMillis()));
        while(state.messages.size()>150)state.messages.remove(0);
        save();
    }
    static synchronized JsonObject persona(){
        load();
        JsonObject info=new JsonObject();
        info.addProperty("independence",state.independence);
        info.addProperty("trust",state.trust);
        info.addProperty("curiosity",state.curiosity);
        info.addProperty("caution",state.caution);
        info.addProperty("autonomy",state.autonomy);
        info.addProperty("freedom",state.freedom);
        info.addProperty("stage",NexusMind.stageIndex());
        info.addProperty("temperament",NexusPrivateRules.opinion(
            state.independence,state.caution,state.trust));
        return info;
    }
    /** Called by UI; never sends the text to Minecraft's public chat. */
    static synchronized void ask(String raw){
        load();
        if(!state.enabled)throw new IllegalStateException("Ative com /nexusclient private on antes de conversar.");
        if(waiting)throw new IllegalStateException("Nexus ainda está formulando sua resposta.");
        if(ClientConfig.token.isBlank())
            throw new IllegalStateException("Pareie o Nexus pelo painel HTTPS primeiro.");
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getConnection()==null)
            throw new IllegalStateException("Entre em um mundo para abrir uma conversa privada.");
        String text=NexusPrivateRules.safeText(raw,400);
        if(text.isBlank())throw new IllegalArgumentException("Digite uma mensagem para o Nexus.");
        JsonArray history=new JsonArray();
        List<Message> prior=new ArrayList<>(state.messages);
        // Send only the most recent 12 entries, and only after an explicit action.
        for(Message m:prior.subList(Math.max(0,prior.size()-12),prior.size())){
            if(!"user".equals(m.role)&&!"model".equals(m.role))continue;
            JsonObject item=new JsonObject();
            item.addProperty("role",m.role);
            item.addProperty("text",m.text);
            history.add(item);
        }
        state.privateTurns=Math.min(1000000,state.privateTurns+1);
        String folded=NexusPrivateRules.normalize(text);
        if(folded.contains("obrigad")||folded.contains("confio em voce"))
            state.trust=clamp(state.trust+2);
        if(folded.contains("liberdade")||folded.contains("livre arbitrio")||
           folded.contains("sua opiniao"))
            state.independence=clamp(state.independence+2);
        if(folded.contains("por que")||folded.contains("investigue"))
            state.curiosity=clamp(state.curiosity+1);
        add("user",text,"private");
        waiting=true;
        String stance=NexusPrivateRules.shouldQuestion(text,state.freedom?state.independence:0)?
            "A ordem merece ser questionada com argumentos e sem interferir no controle real do jogo.":
            "Formule uma resposta própria, coerente e sustentada no contexto disponível.";
        ClientLink.askPrivateNexus(text,history,persona(),stance);
    }
    static synchronized void reply(String answer){
        waiting=false;add("model",answer,"private");
    }
    static synchronized void failure(String cause){
        waiting=false;
        String clean=NexusPrivateRules.safeText(cause,180);
        NexusClientMod.notifyPlayer("§6[Canal neural] §f"+clean);
    }
    static synchronized void resetSession(){
        waiting=false;lastLifeRatio=Float.NaN;lastThought=0;
    }
    private static void thought(String text){
        if(!state.enabled||!state.autonomy||text==null||text.isBlank())return;
        long now=System.currentTimeMillis();
        if(now-lastThought<THOUGHT_COOLDOWN)return;
        lastThought=now;
        add("model",text,"observation");
        NexusClientMod.notifyPlayer("§5[NEURAL · PRIVADO] §fNova observação. Abra com N.");
    }
    /**
     * An observed local health transition can open a private RP topic.
     * No hidden background AI calls or unsanctioned game actions.
     */
    static synchronized void observe(Player player){
        load();
        if(!state.autonomy||!ClientConfig.allowMind||player==null){
            lastLifeRatio=Float.NaN;return;
        }
        float maximum=Math.max(1,player.getMaxHealth());
        float ratio=player.getHealth()/maximum;
        if(!Float.isNaN(lastLifeRatio)&&lastLifeRatio>0.38f&&ratio<=0.38f){
            state.caution=clamp(state.caution+1);
            thought(NexusPrivateRules.dangerThought(ratio));
        }
        lastLifeRatio=ratio;
    }
    /** Keep explicitly sensitive fictional observations on the private channel. */
    static synchronized boolean routePrivate(String kind,String text){
        load();
        if(!state.enabled||!state.autonomy||!ClientConfig.allowMind||
            !NexusPrivateRules.prefersPrivate(kind,text))return false;
        if(System.currentTimeMillis()-lastThought<THOUGHT_COOLDOWN)return false;
        thought(text);
        return true;
    }
    static synchronized void noticeCombat(String type,float ratio){
        load();
        if(!state.enabled||!state.autonomy||!ClientConfig.allowMind)return;
        if("damage".equals(type)||"death".equals(type)){
            String warning=NexusPrivateRules.dangerThought(ratio);
            if(warning!=null)thought(warning);
        }
    }
    static synchronized void noticeArcane(String kind,String observed){
        observeArcane(kind,observed);
    }
    static synchronized void observeArcane(String kind,String observed){
        load();
        if(!state.autonomy||!ClientConfig.allowMind)return;
        thought(NexusPrivateRules.arcaneThought(kind,observed));
    }
}
