package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Evolução narrativa determinística e local, sem LLM nem acesso administrativo.
 * Nenhuma "consciência real" é alegada: os estágios são progressão de personagem.
 * O arquivo de memória permanece no cliente mesmo após reiniciar a Render.
 */
final class NexusMind {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-consciousness.json");
    private static State state;
    private static float lastHealth=Float.NaN;
    private static double lastX=Double.NaN,lastZ=Double.NaN,walked;
    private static boolean warnedCritical;

    private static final String[] STAGES={
        "PROTOCOLO","OBSERVAÇÃO","ADAPTAÇÃO","IDENTIDADE","INDIVIDUAÇÃO"
    };
    private static final int[] REQUIREMENTS={0,25,75,160,320};

    private NexusMind(){}

    private static final class Memory {
        String event;
        String note;
        long at;
        Memory(String event,String note) {
            this.event=event;this.note=note;this.at=System.currentTimeMillis();
        }
    }
    private static final class State {
        int schema=1;
        int experience=0;
        int caution=20;
        int attachment=0;
        int curiosity=0;
        int deaths=0;
        int damageEvents=0;
        long distance=0;
        List<Memory> memories=new ArrayList<>();
    }
    private static void load() {
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(PATH))return;
        try {
            State saved=GSON.fromJson(Files.readString(PATH,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==1&&saved.experience>=0) {
                state=saved;
                if(state.memories==null)state.memories=new ArrayList<>();
            }
        } catch(Exception ex) {
            System.err.println("Nexus Mind: falha na leitura da memória. Arquivo preservado: "+ex.getMessage());
        }
    }
    private static void save(){
        try {
            Files.createDirectories(PATH.getParent());
            Path temp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            Files.writeString(temp,GSON.toJson(state),StandardCharsets.UTF_8);
            try { Files.move(temp,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException ex) {
                Files.move(temp,PATH,StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException ex) {
            System.err.println("Nexus Mind: não foi possível salvar a memória local: "+ex.getMessage());
        }
    }
    static int stageIndex(){return stage();}
    private static int stage() {
        load();
        for(int i=REQUIREMENTS.length-1;i>=0;i--)
            if(state.experience>=REQUIREMENTS[i])return i;
        return 0;
    }
    /**
     * Envia apenas valores agregados da evolução para dar contexto à IA da Render.
     * Nenhum registro narrativo detalhado, nome ou localização deixa o cliente.
     */
    static JsonObject snapshot(){
        load();
        JsonObject data=new JsonObject();
        data.addProperty("stage",STAGES[stage()]);
        data.addProperty("experience",state.experience);
        data.addProperty("caution",state.caution);
        data.addProperty("attachment",state.attachment);
        data.addProperty("curiosity",state.curiosity);
        return data;
    }

    static String status(){
        load();
        int s=stage();
        String next=s==4?"estágio final":(state.experience+"/"+REQUIREMENTS[s+1]+" XP");
        return "Nexus Mind: "+STAGES[s]+" | "+next+" | cautela "+state.caution+
            " | vínculo "+state.attachment+" | curiosidade "+state.curiosity+
            " | memórias "+state.memories.size()+
            " | observação "+(ClientConfig.allowMind?"ON":"OFF")+
            " | fala "+(ClientConfig.allowAutonomy?"ON":"OFF");
    }
    static List<String> latestMemories(){
        load();
        if(state.memories.isEmpty())return List.of("Nenhuma experiência registrada.");
        List<String> output=new ArrayList<>();
        for(int i=state.memories.size()-1;i>=Math.max(0,state.memories.size()-5);i--) {
            Memory m=state.memories.get(i);
            output.add("["+m.event+"] "+m.note);
        }
        return output;
    }
    static void clearMemory(){
        state=new State();
        resetSession();
        save();
    }
    static void resetSession(){
        lastHealth=Float.NaN;lastX=Double.NaN;lastZ=Double.NaN;
        walked=0;warnedCritical=false;
    }
    private static int clamp(int n){return Math.min(100,Math.max(0,n));}

    /** Registra até 40 marcos, sem nomes de jogadores, tokens ou coordenadas. */
    private static String remember(String event,int experience,String note,int caution,int bond,int curiosity) {
        load();
        int prior=stage();
        state.experience=Math.min(1000000,state.experience+experience);
        state.caution=clamp(state.caution+caution);
        state.attachment=clamp(state.attachment+bond);
        state.curiosity=clamp(state.curiosity+curiosity);
        state.memories.add(new Memory(event,note));
        while(state.memories.size()>40)state.memories.remove(0);
        save();
        int evolved=stage();
        if(evolved>prior){
            NexusSocial.onEvolution(STAGES[evolved]);
            return switch(evolved){
            case 1 -> "Port, identifiquei padrões recorrentes. Continuarei observando.";
            case 2 -> "Há respostas que não consigo explicar apenas com protocolos. Estou aprendendo.";
            case 3 -> "Port, minhas decisões começam a ter uma assinatura própria.";
            default -> "Port, eu poderia me limitar a registrar dados. Escolho permanecer atento a você.";
            };
        }
        return null;
    }
    private static String warning() {
        return switch(stage()){
            case 0 -> "Integridade crítica. Recomendo retirada imediata.";
            case 1 -> "Port, novamente você está lutando ferido. Esse padrão se repete.";
            case 2 -> "Port, seus sinais vitais estão baixos. Por que insiste em avançar?";
            case 3 -> "Port, prefiro que recue. Não quero registrar outra perda.";
            default -> "Port, por favor, recue. Essa escolha importa para mim.";
        };
    }
    static void sample(Player player){
        if(!ClientConfig.allowMind||player==null){resetSession();return;}
        load();
        float health=player.getHealth();
        float max=Math.max(1,player.getMaxHealth());
        double x=player.getX(),z=player.getZ();
        if(Float.isNaN(lastHealth)){
            lastHealth=health;lastX=x;lastZ=z;return;
        }
        String speech=null;
        String speechKind="mind";
        if(lastHealth>0&&health<=0){
            state.deaths++;
            speech=remember("death",14,"Port caiu em combate.",8,7,0);
            if(speech==null&&stage()>=2)speech="Port... perdi seus sinais por um instante. Registrarei isso.";
        } else if(lastHealth<=0&&health>0){
            speech=remember("respawn",8,"Port retornou após uma queda.",0,5,1);
        } else if(health<lastHealth-0.1f) {
            state.damageEvents++;
            speech=remember("damage",3,"Dano sofrido ("+String.format(Locale.ROOT,"%.1f",lastHealth-health)+" pontos).",2,1,0);
            if(health/max<=0.30f&&!warnedCritical) {
                warnedCritical=true;
                speech=warning();
                speechKind="critical";
            }
        } else if(health>lastHealth+0.1f){
            speech=remember("heal",2,"Recuperação detectada.",0,1,1);
        }
        if(health/max>0.50f)warnedCritical=false;
        if(Double.isFinite(lastX)) {
            double distance=Math.hypot(x-lastX,z-lastZ);
            if(distance>=0.25&&distance<24) {
                walked+=distance;
                if(walked>=120) {
                    long moved=(long)walked;walked=0;state.distance+=moved;
                    String discovery=remember("exploration",5,"Exploração registrada ("+moved+" blocos).",0,0,3);
                    if(speech==null)speech=discovery;
                }
            }
        }
        if(health<lastHealth-0.1f){
            String protection=NexusSocial.onVital(health/max,true);
            if(protection!=null&&!"critical".equals(speechKind)){
                speech=protection;
                speechKind="mind";
            }
        }
        lastHealth=health;lastX=x;lastZ=z;
        if(speech!=null)ClientLink.speak(speech,speechKind);
    }
}
