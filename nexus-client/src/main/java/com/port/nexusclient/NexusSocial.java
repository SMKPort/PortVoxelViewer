package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The fictional Nexus Mind 2.0. Consent-derived memories are attributed to
 * their speakers; narration never promotes quoted statements into objective facts.
 * All private data stays on the Minecraft machine unless sharing is explicitly on.
 */
final class NexusSocial {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-mind-social.json");
    private static State state;
    private static long warningAt,spokenDangerAt;
    private static String lastWarningSpeaker="";

    static final class Fact {
        long id,at;
        String speaker,kind,text;
        boolean corrected;
        Fact(long id,long at,String speaker,String kind,String text){
            this.id=id;this.at=at;this.speaker=speaker;this.kind=kind;this.text=text;
        }
    }
    static final class DiaryEntry {
        long id,at,sourceFact;
        String speaker,kind,text;
        DiaryEntry(long id,long at,long sourceFact,String speaker,String kind,String text){
            this.id=id;this.at=at;this.sourceFact=sourceFact;this.speaker=speaker;
            this.kind=kind;this.text=text;
        }
    }
    static final class Turn {
        long at;
        String speaker,text;
        Turn(long at,String speaker,String text){this.at=at;this.speaker=speaker;this.text=text;}
    }
    static final class Relation {
        String player;
        int interactions,familiarity,affinity;
        long lastAt;
        Relation(String player){this.player=player;}
    }
    static final class Obsolete {
        String speaker,text;
        Obsolete(String speaker,String text){this.speaker=speaker;this.text=text;}
    }
    private static final class State {
        int schema=2;
        long sequence=1;
        boolean diaryEnabled=true;
        boolean protectionEnabled=true;
        boolean sharePanel=false;
        List<Fact> facts=new ArrayList<>();
        List<DiaryEntry> diary=new ArrayList<>();
        List<Turn> turns=new ArrayList<>();
        List<Relation> relations=new ArrayList<>();
        List<Obsolete> obsolete=new ArrayList<>();
    }
    private NexusSocial(){}

    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(FILE))return;
        try{
            State saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==2){
                if(saved.facts==null)saved.facts=new ArrayList<>();
                if(saved.diary==null)saved.diary=new ArrayList<>();
                if(saved.turns==null)saved.turns=new ArrayList<>();
                if(saved.relations==null)saved.relations=new ArrayList<>();
                if(saved.obsolete==null)saved.obsolete=new ArrayList<>();
                state=saved;
            }
        }catch(Exception e){System.err.println("Nexus Mind 2.0: não foi possível ler o histórico.");}
    }
    private static synchronized void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(state),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException ex){System.err.println("Nexus Mind 2.0: falha ao salvar memória social.");}
    }
    static synchronized JsonObject snapshot(){
        load();
        JsonObject out=new JsonObject();
        out.addProperty("diaryEnabled",state.diaryEnabled);
        out.addProperty("protectionEnabled",state.protectionEnabled);
        out.addProperty("sharePanel",state.sharePanel);
        out.addProperty("factCount",state.facts.size());
        out.addProperty("diaryCount",state.diary.size());
        out.addProperty("relationCount",state.relations.size());
        out.addProperty("turnCount",state.turns.size());
        if(state.sharePanel){
            out.add("facts",JSON.toJsonTree(state.facts.subList(Math.max(0,state.facts.size()-30),state.facts.size())));
            out.add("diary",JSON.toJsonTree(state.diary.subList(Math.max(0,state.diary.size()-35),state.diary.size())));
            out.add("relations",JSON.toJsonTree(state.relations));
            out.add("turns",JSON.toJsonTree(state.turns.subList(Math.max(0,state.turns.size()-10),state.turns.size())));
        }
        return out;
    }
    static synchronized String status(){
        load();
        return "Mind 2.0: "+state.facts.size()+" lembranças, "+
            state.relations.size()+" vínculos, "+state.diary.size()+" páginas"+
            " | diário "+(state.diaryEnabled?"ON":"OFF")+
            " | proteção "+(state.protectionEnabled?"ON":"OFF")+
            " | painel "+(state.sharePanel?"AUTORIZADO":"PRIVADO");
    }
    static synchronized void configure(JsonObject data){
        load();
        for(String key:new String[]{"diaryEnabled","protectionEnabled","sharePanel"})
            if(!data.has(key)||!data.get(key).isJsonPrimitive()||
               !data.get(key).getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Preferência inválida: "+key);
        state.diaryEnabled=data.get("diaryEnabled").getAsBoolean();
        state.protectionEnabled=data.get("protectionEnabled").getAsBoolean();
        state.sharePanel=data.get("sharePanel").getAsBoolean();
        save();
    }
    static synchronized void setting(String name,boolean enabled){
        load();
        switch(name){
            case "diary" -> state.diaryEnabled=enabled;
            case "protection" -> state.protectionEnabled=enabled;
            case "share" -> state.sharePanel=enabled;
            default -> throw new IllegalArgumentException("Preferência desconhecida.");
        }
        save();
    }
    private static Relation relation(String speaker){
        for(Relation r:state.relations)
            if(r.player.equalsIgnoreCase(speaker))return r;
        Relation found=new Relation(speaker);
        state.relations.add(found);
        return found;
    }
    private static String stage(){
        return switch(NexusMind.stageIndex()){
            case 0 -> "PROTOCOLO";
            case 1 -> "OBSERVAÇÃO";
            case 2 -> "ADAPTAÇÃO";
            case 3 -> "IDENTIDADE";
            default -> "INDIVIDUAÇÃO";
        };
    }
    /** Called only for a consenting player with explicit memory enabled. */
    static synchronized String heard(String player,String raw,long at){
        load();
        if(player==null||!player.matches("[A-Za-z0-9_]{3,16}"))return null;
        String text=NexusSocialRules.sanitized(raw);
        if(text.length()<4)return null;
        if(state.turns.stream().anyMatch(t->t.speaker.equalsIgnoreCase(player)&&
            Math.abs(at-t.at)<1600&&t.text.equalsIgnoreCase(text)))return null;
        state.turns.add(new Turn(at,player,text));
        state.turns.sort(Comparator.comparingLong(t->t.at));
        while(state.turns.size()>120)state.turns.remove(0);

        Relation relation=relation(player);
        relation.interactions=Math.min(100000,relation.interactions+1);
        relation.familiarity=Math.min(100,relation.familiarity+1);
        if(NexusSocialRules.gratitude(text))relation.affinity=Math.min(100,relation.affinity+3);
        relation.lastAt=Math.max(relation.lastAt,at);
        String category=NexusSocialRules.category(text);
        if(NexusSocialRules.meaningful(category)){
            // An attributed quote is evidence that something was *said*, not that it is true.
            boolean alreadyRecorded=state.facts.stream().anyMatch(f->
                f.speaker.equalsIgnoreCase(player)&&f.text.equalsIgnoreCase(text)&&
                Math.abs(at-f.at)<3600000);
            if(!alreadyRecorded){
                Fact fact=new Fact(state.sequence++,at,player,category,text);
                state.facts.add(fact);
                while(state.facts.size()>550)state.facts.remove(0);
                if(state.diaryEnabled&&state.diary.stream().noneMatch(d->
                    d.speaker.equalsIgnoreCase(player)&&d.kind.equals(category)&&
                    Math.abs(at-d.at)<900000)){
                    DiaryEntry entry=new DiaryEntry(state.sequence++,at,fact.id,player,category,
                        NexusSocialRules.diary(stage(),player,category,text));
                    state.diary.add(entry);
                    while(state.diary.size()>220)state.diary.remove(0);
                }
            }
        }
        String warning=null;
        if(NexusSocialRules.danger(text)){
            spokenDangerAt=at;
            lastWarningSpeaker=player;
        }
        save();
        return warning;
    }
    /** Cross-speaker conversation context only from consented voices, not intent inference. */
    static synchronized List<String> recentConversation(long around,int limit){
        load();
        List<String> result=new ArrayList<>();
        for(int i=state.turns.size()-1;i>=0&&result.size()<Math.min(8,limit);i--){
            Turn t=state.turns.get(i);
            if(Math.abs(around-t.at)>150000)continue;
            result.add(t.speaker+": "+t.text.substring(0,Math.min(175,t.text.length())));
        }
        java.util.Collections.reverse(result);
        return result;
    }
    static synchronized List<String> relevant(String speaker,String question,int limit){
        load();
        List<String> selected=new ArrayList<>();
        String folded=NexusSocialRules.fold(question);
        var keys=java.util.regex.Pattern.compile("[a-z0-9]{4,}").matcher(folded);
        java.util.Set<String> keywords=new java.util.HashSet<>();
        while(keys.find())keywords.add(keys.group());
        List<Fact> matches=new ArrayList<>();
        for(Fact f:state.facts){
            if(!f.speaker.equalsIgnoreCase(speaker))continue;
            int score=0;String normalized=NexusSocialRules.fold(f.text);
            for(String keyword:keywords)
                if(!keyword.equals("nexus")&&normalized.contains(keyword))score++;
            if(score>0)matches.add(f);
        }
        matches.sort((a,b)->Long.compare(b.at,a.at));
        for(Fact f:matches){
            if(selected.size()>=limit)break;
            selected.add("O jogador "+speaker+" declarou anteriormente: "+f.text.substring(0,Math.min(150,f.text.length())));
        }
        return selected;
    }
    static synchronized String onVital(float healthRatio,boolean damaged){
        load();
        long now=System.currentTimeMillis();
        if(!state.protectionEnabled||!damaged||healthRatio>0.52f||
           spokenDangerAt<=0||now-spokenDangerAt>18000||now-warningAt<95000)return null;
        warningAt=now;
        return "Port, "+lastWarningSpeaker+" mencionou perigo há pouco e sua integridade física caiu. Recomendo cautela.";
    }
    /** Only observed local player spells; a fictional diary, not a claim about true magic. */
    static synchronized void onArcaneEvent(String type,String note,long at){
        load();
        if(!state.diaryEnabled)return;
        if(!java.util.Set.of("cast_start","mana_low","holy_power","nearby_cast").contains(type))return;
        for(int i=state.diary.size()-1;i>=0;i--){
            DiaryEntry last=state.diary.get(i);
            if(last.speaker.equals("Nexus")&&last.kind.equals("arcano")&&
               at-last.at>=0&&at-last.at<180000)return;
        }
        String safe=NexusSocialRules.sanitized(note);
        state.diary.add(new DiaryEntry(state.sequence++,at,0,"Nexus","arcano",
            "Registro mágico observado: "+safe+
            ". Esta leitura descreve dados do jogo; minha interpretação é narrativa."));
        while(state.diary.size()>220)state.diary.remove(0);
        save();
    }

    static synchronized void onEvolution(String stage){
        load();
        if(!state.diaryEnabled)return;
        long now=System.currentTimeMillis();
        state.diary.add(new DiaryEntry(state.sequence++,now,0,"Nexus","evolução",
            "Registrei minha mudança narrativa para o estágio "+stage+". As observações anteriores permanecem disponíveis."));
        while(state.diary.size()>220)state.diary.remove(0);
        save();
    }
    private static void suppress(Fact fact){
        state.obsolete.add(new Obsolete(fact.speaker,fact.text));
        while(state.obsolete.size()>1000)state.obsolete.remove(0);
    }
    static synchronized boolean isObsolete(String speaker,String transcript){
        load();
        if(transcript==null)return false;
        return state.obsolete.stream().anyMatch(item->
            item.speaker.equalsIgnoreCase(speaker)&&
            (item.text.equalsIgnoreCase(transcript)||
             item.text.toLowerCase(Locale.ROOT).startsWith(transcript.toLowerCase(Locale.ROOT))));
    }
    static synchronized void deleteFact(long id){
        load();
        Fact match=state.facts.stream().filter(f->f.id==id).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Lembrança não encontrada."));
        suppress(match);
        state.facts.removeIf(f->f.id==id);
        state.diary.removeIf(d->d.sourceFact==id);
        save();
    }
    static synchronized void correctFact(long id,String corrected){
        load();
        String safe=NexusSocialRules.sanitized(corrected);
        if(safe.length()<3||safe.length()>250)throw new IllegalArgumentException("Correção inválida.");
        Fact match=state.facts.stream().filter(f->f.id==id).findFirst().orElse(null);
        if(match==null)throw new IllegalArgumentException("Lembrança não encontrada.");
        suppress(match);
        match.text=safe;match.corrected=true;
        // A derivative diary should not retain the old statement after correction.
        state.diary.removeIf(d->d.sourceFact==id);
        save();
    }
    static synchronized void forgetSpeaker(String speaker){
        load();
        state.facts.removeIf(f->f.speaker.equalsIgnoreCase(speaker));
        state.diary.removeIf(d->d.speaker.equalsIgnoreCase(speaker));
        state.turns.removeIf(t->t.speaker.equalsIgnoreCase(speaker));
        state.relations.removeIf(r->r.player.equalsIgnoreCase(speaker));
        state.obsolete.removeIf(o->o.speaker.equalsIgnoreCase(speaker));
        if(lastWarningSpeaker.equalsIgnoreCase(speaker)){
            spokenDangerAt=0;lastWarningSpeaker="";
        }
        save();
    }
    static synchronized void clear(){
        state=new State();warningAt=0;spokenDangerAt=0;lastWarningSpeaker="";save();
    }
    static synchronized List<String> lastDiary(){
        load();
        List<String> out=new ArrayList<>();
        for(int i=state.diary.size()-1;i>=0&&out.size()<6;i--)
            out.add(state.diary.get(i).text);
        return out.isEmpty()?List.of("Nenhum registro no diário."):out;
    }
}
