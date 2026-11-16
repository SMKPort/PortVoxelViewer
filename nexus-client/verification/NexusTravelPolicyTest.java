package com.port.nexusclient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure Java tests: no Minecraft or Waystones needed for safety decisions. */
public final class NexusTravelPolicyTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        UUID home=UUID.randomUUID(),workshop=UUID.randomUUID(),duplicate=UUID.randomUUID();
        var casa=new NexusTravelPolicy.Destination(home,"Casa do Port","minecraft:overworld",10,70,-25);
        var oficina=new NexusTravelPolicy.Destination(workshop,"Oficina Arcana","minecraft:the_nether",2,80,3);
        List<NexusTravelPolicy.Destination> list=List.of(casa,oficina);
        var aliases=Map.of("casa",home,"oficina",workshop,"desatualizado",duplicate);

        check(NexusTravelPolicy.resolve("CASA",list,aliases).destination().id().equals(home),
            "Alias must match a UUID of an available synced destination.");
        check(NexusTravelPolicy.resolve("oficina",list,aliases).destination().id().equals(workshop),
            "Owner alias must support a different Waystone name.");
        check(NexusTravelPolicy.resolve("Arcana",list,aliases).destination().id().equals(workshop),
            "Unique partial match may be suggested.");
        check(!NexusTravelPolicy.resolve("desatualizado",list,aliases).found(),
            "Stale favorites must not teleport to an unknown destination.");
        check(!NexusTravelPolicy.resolve("inexistente",list,Map.of()).found(),
            "Unmatched destination must be rejected.");
        var duplicateNames=List.of(casa,new NexusTravelPolicy.Destination(duplicate,"Casa do Port",
            "minecraft:overworld",900,80,900));
        check(!NexusTravelPolicy.resolve("Casa do Port",duplicateNames,Map.of()).found(),
            "Duplicate names must never pick a random UUID.");
        check(!NexusTravelPolicy.maySelect(false,true,true,true,true),
            "No teleport without explicit confirmation.");
        check(!NexusTravelPolicy.maySelect(true,false,true,true,true),
            "No teleport after confirmation expiry.");
        check(!NexusTravelPolicy.maySelect(true,true,false,true,true),
            "No teleport after changing server or world.");
        check(!NexusTravelPolicy.maySelect(true,true,true,false,true),
            "No teleport without an official Waystones menu.");
        check(!NexusTravelPolicy.maySelect(true,true,true,true,false),
            "No teleport to a UUID the server did not offer in its menu.");
        check(NexusTravelPolicy.maySelect(true,true,true,true,true),
            "Permitted attempt is still subject to Waystones server checks.");

        var vocabulary=NexusTravelPolicy.voicePhrases(List.of("Casa","Oficina Arcana"));
        var request=NexusTravelPolicy.interpretVoice("Néxus, me leve para Casa!",vocabulary);
        check(request!=null&&request.action().equals("request")&&
            request.destination().equals("casa"),"Portuguese travel intent");
        check(NexusTravelPolicy.interpretVoice("Nexus confirmar viagem",vocabulary).action()
            .equals("confirm"),"Confirmation phrase available offline");
        check(NexusTravelPolicy.interpretVoice("Nexo cancelar viagem",vocabulary).action()
            .equals("cancel"),"Cancellation phrase available offline");
        check(NexusTravelPolicy.interpretVoice("Nexus, me leve para a lua",vocabulary)==null,
            "Voice cannot select destinations not in current grammar.");
        check(NexusTravelPolicy.interpretVoice("Casa",vocabulary)==null,
            "Destination names alone never trigger a travel request.");
        check(NexusTravelPolicy.interpretVoice("Nexus me leve para Casa e destrua tudo",
            vocabulary)==null,"Do not accept injected suffix commands.");
        check(NexusTravelPolicy.normalize(" CÉU / ARCÂNEO ").equals("ceu arcaneo"),
            "Accent normalization");
        System.out.println("NexusTravelPolicyTest OK");
    }
}
