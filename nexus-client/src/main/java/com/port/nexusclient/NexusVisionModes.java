package com.port.nexusclient;

/** Explicit focus presets. They never add hidden world state to the visual question. */
final class NexusVisionModes {
    enum Focus {
        GERAL("Geral", "Descreva somente o que está visível; diferencie observação de hipótese."),
        ESTRUTURAS("Construções", "Analise blocos, estruturas e arquitetura visíveis; sugira melhorias."),
        PERIGOS("Perigos", "Identifique ameaças e riscos visíveis; não invente mobs ocultos."),
        ITENS("Itens e HUD", "Examine itens, textos e indicadores legíveis; avise sobre incertezas.");
        final String label,hint;
        Focus(String label,String hint){this.label=label;this.hint=hint;}
    }

    static final int MAX_QUESTION=180;
    private NexusVisionModes(){}
    static Focus next(Focus focus){
        Focus[] all=Focus.values();
        return all[(focus.ordinal()+1)%all.length];
    }
    static String prompt(Focus focus,String userQuestion){
        Focus selected=focus==null?Focus.GERAL:focus;
        String request=userQuestion==null?"":userQuestion.replaceAll("[\\p{Cntrl}]"," ")
            .replaceAll("\\s+"," ").trim();
        if(request.isEmpty())request="O que você consegue identificar nesta captura?";
        String base=selected.hint+" Pergunta: ";
        int remaining=MAX_QUESTION-base.length();
        if(remaining<1)throw new IllegalStateException("Instrução de foco longa demais.");
        return base+request.substring(0,Math.min(remaining,request.length()));
    }
}
