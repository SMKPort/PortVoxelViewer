package com.port.nexusclient;

import java.util.Comparator;
import java.util.List;

/** Rejects conflicting outcomes from bilingual offline recognition. */
final class NexusVoiceCandidatePolicy {
    record Candidate(String language,String spoken,String intent,float confidence) {}
    record Decision(Candidate winner,String reason) {
        boolean accepted(){return winner!=null;}
    }
    private NexusVoiceCandidatePolicy(){}

    static Decision choose(List<Candidate> candidates,float threshold){
        if(candidates==null||candidates.isEmpty())
            return new Decision(null,"Nenhum comando reconhecido.");
        List<Candidate> accepted=candidates.stream()
            .filter(c->c!=null&&c.intent()!=null&&!c.intent().isBlank()
                &&Float.isFinite(c.confidence())&&c.confidence()>=threshold)
            .toList();
        if(accepted.isEmpty())
            return new Decision(null,"Sem comando válido com confiança suficiente.");
        if(accepted.stream().map(Candidate::intent).distinct().count()>1)
            return new Decision(null,"Modelos de voz discordaram. Repita o comando.");
        Candidate winner=accepted.stream().max(Comparator.comparingDouble(Candidate::confidence)
            .thenComparing(Candidate::language)).orElseThrow();
        return new Decision(winner,"Comando reconhecido por "+winner.language().toUpperCase()+".");
    }
}
