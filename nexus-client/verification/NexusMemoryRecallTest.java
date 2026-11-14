package com.port.nexusclient;

import java.util.List;

public final class NexusMemoryRecallTest {
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args){
        List<NexusMemoryRecall.Note> notes=List.of(
            new NexusMemoryRecall.Note(1,"Chave da oficina",
                "Gabriel está com a chave da oficina.",100),
            new NexusMemoryRecall.Note(2,"Armadura do Nexus",
                "A placa do ombro direito precisa de reforço.",200),
            new NexusMemoryRecall.Note(3,"Mizora",
                "Mizora é amiga do Port.",300),
            new NexusMemoryRecall.Note(4,"Chave do depósito",
                "Leonardo guardou a chave do depósito.",400)
        );
        var result=NexusMemoryRecall.select("Nexus, quem está com a chave da oficina?",notes,3);
        check(!result.isEmpty()&&result.get(0).id()==1,
            "The specific key note outranks another key");
        check(NexusMemoryRecall.select("O estado da placa do ombro?",notes,3).get(0).id()==2,
            "Related words select armor note");
        check(NexusMemoryRecall.select("Como foi o meu dia?",notes,3).isEmpty(),
            "Do not inject unrelated facts");
        check(NexusMemoryRecall.select("Quem é Mizora?",notes,3).get(0).id()==3,
            "Canon can be retrieved when explicitly asked");
        check(NexusMemoryRecall.select("Como está a armadura?",notes,3).stream()
            .noneMatch(x->x.id()==3),"Do not prime unrelated Mizora memories");
        var inventory=NexusMemoryRecall.select("Quais memórias você guardou?",notes,3);
        check(inventory.size()==3&&inventory.get(0).id()==4,
            "Inventory returns latest user notes");
        check(NexusMemoryRecall.select("??",notes,3).isEmpty(),"Empty queries don't disclose notes");
        check(NexusMemoryRecall.select("Chave",notes,1).size()==1,"Limit enforced");
        check(NexusMemoryRecall.fold("Conjuração e ação").equals("conjuracao e acao"),
            "Accent-insensitive query");
        System.out.println("NexusMemoryRecallTest OK");
    }
}
