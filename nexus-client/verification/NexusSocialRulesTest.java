package com.port.nexusclient;

public final class NexusSocialRulesTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(NexusSocialRules.category("Eu prometo trazer um grimório").equals("promessa"),"promise");
        check(NexusSocialRules.category("Estou construindo uma oficina").equals("projeto"),"project");
        check(NexusSocialRules.category("Achei uma escritura de Anael").equals("lore"),"lore");
        check(NexusSocialRules.category("Bom dia, tudo bem?").equals("conversa"),"conversation");
        check(NexusSocialRules.danger("Cuidado, emboscada!"),"danger");
        check(!NexusSocialRules.danger("Como está sua oficina?"),"no false danger");
        check(NexusSocialRules.gratitude("Obrigado pela ajuda"),"gratitude");
        String protectedText=NexusSocialRules.sanitized(
            "Minha senha: ABCxyz123 e contato nome@example.com");
        check(!protectedText.contains("ABCxyz123")&&!protectedText.contains("nome@example.com"),
            "privacy scrub");
        check(NexusSocialRules.diary("IDENTIDADE","Outro","projeto","Construindo um castelo")
            .contains("Outro"),"attributed diary");
        System.out.println("Nexus Social Rules: all tests passed");
    }
}
