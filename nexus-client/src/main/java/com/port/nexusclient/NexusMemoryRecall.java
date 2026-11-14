package com.port.nexusclient;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Deterministic, local retrieval. No embeddings, API calls or model guessing. */
final class NexusMemoryRecall {
    record Note(long id,String topic,String text,long updatedAt){}
    private record Ranked(Note note,int score){}
    private static final Set<String> STOP=Set.of(
        "para","porque","como","sobre","quando","onde","qual","quais","quem",
        "estou","voce","voces","comigo","minha","meus","meu","suas","suas",
        "esta","estao","essa","esse","isso","aquela","aquele","aquilo",
        "tinha","tenho","temos","sabia","lembra","lembrar","lembranca",
        "memoria","memorias","recorda","recordar","fale","conte","disse",
        "aqui","agora","daqui","depois","antes","todos","todas","dessa",
        "deste","sobre","isso","mais","menos","nexus","port","ainda"
    );
    private NexusMemoryRecall(){}

    static String fold(String value){
        return Normalizer.normalize(String.valueOf(value==null?"":value),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    static Set<String> words(String value){
        Set<String> result=new HashSet<>();
        for(String word:fold(value).split(" ")){
            if(word.length()>=3&&!STOP.contains(word))result.add(word);
        }
        return result;
    }
    static boolean asksInventory(String question){
        String q=fold(question);
        return q.matches(".*(quais|liste|listar|mostre|mostra|todas|todos|resumo).*\\b(memorias|lembrancas|anotacoes)\\b.*")
            || q.matches(".*\\b(o que voce lembra de mim|o que voce memorizou)\\b.*");
    }

    static List<Note> select(String question,List<Note> source,int limit){
        if(source==null||source.isEmpty()||limit<=0)return List.of();
        int max=Math.min(8,limit);
        if(asksInventory(question))
            return source.stream().sorted(Comparator.comparingLong(Note::updatedAt).reversed())
                .limit(max).toList();

        String q=fold(question);
        Set<String> tokens=words(question);
        if(tokens.isEmpty())return List.of();
        List<Ranked> ranked=new ArrayList<>();
        for(Note note:source){
            if(note==null)continue;
            String topic=fold(note.topic()),body=fold(note.text());
            Set<String> topicWords=words(note.topic()),bodyWords=words(note.text());
            int score=0;
            for(String token:tokens){
                if(topicWords.contains(token))score+=6;
                else if(bodyWords.contains(token))score+=2;
                else if(token.length()>4&&topic.contains(token))score+=3;
            }
            if(topic.length()>=4&&q.contains(topic))score+=12;
            if(score>0)ranked.add(new Ranked(note,score));
        }
        ranked.sort(Comparator.comparingInt(Ranked::score).reversed()
            .thenComparing(Comparator.comparingLong((Ranked r)->r.note().updatedAt()).reversed()));
        return ranked.stream().limit(max).map(Ranked::note).toList();
    }
}
