package com.port.nexusclient;

public final class NexusWakeWordsTest {
    private static void expect(boolean actual, boolean expected, String text) {
        if(actual!=expected)throw new AssertionError(text+" expected "+expected+" got "+actual);
    }

    public static void main(String[] args){
        for(String example:new String[]{
            "Nexus, você está me ouvindo?",
            "Ei, Nexus! Pode conversar comigo?",
            "Oi, NEXO, tudo bem?",
            "Ô, Nexus Aeterna, me ajuda?",
            "Então, Nexus, o que você acha?",
            "Cara, Nexus, responde a Alice?",
            "Nexus",
            "ALÔ, NEXUS: me responde",
            "Fala Nexus, está aí?"
        })expect(NexusWakeWords.addressed(example),true,example);
        for(String example:new String[]{
            "Oi, como vai?",
            "Eu gostei do Nexus que construímos",
            "O mod Nexus tem seis placas",
            "Nexuses e outros nomes",
            "",
            "NexusX, não é a palavra de ativação"
        })expect(NexusWakeWords.addressed(example),false,example);
        System.out.println("NexusWakeWordsTest OK");
    }
}
