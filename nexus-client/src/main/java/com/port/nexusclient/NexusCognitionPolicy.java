package com.port.nexusclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic interpretation of client-observable facts. No guesses about
 * server-side damage, hidden entities, unobserved effects or other players.
 */
final class NexusCognitionPolicy {
    record Facts(double health,double maxHealth,int hunger,double mana,double maxMana,
                 double armIntegrity,boolean armMonitored,boolean macroRunning,
                 boolean healAuthorized,boolean healEquipped,String dimension){}
    record Assessment(int healthPercent,int manaPercent,String risk,List<String> notes,
                      boolean critical){}

    private NexusCognitionPolicy(){}

    static int percent(double observed,double maximum){
        if(!Double.isFinite(observed)||!Double.isFinite(maximum)||maximum<=0||observed<0)
            return -1;
        return (int)Math.round(Math.max(0,Math.min(100,100*observed/maximum)));
    }
    static Assessment assess(Facts facts,int criticalThreshold){
        if(facts==null)return new Assessment(-1,-1,"SEM LEITURA",
            List.of("Aguardando o cliente entrar no mundo."),false);
        int health=percent(facts.health(),facts.maxHealth());
        int mana=percent(facts.mana(),facts.maxMana());
        boolean critical=health>=0&&health<=Math.max(10,Math.min(60,criticalThreshold));
        List<String> notes=new ArrayList<>();
        String risk=critical?"CRÍTICO":
            health>=0&&health<=50?"ELEVADO":
            facts.armMonitored()&&facts.armIntegrity()<=25?"ESTRUTURAL":
            mana>=0&&mana<=20?"MANA BAIXA":"ESTÁVEL";
        if(critical){
            notes.add("Vida crítica: sugiro recuar e interromper ataques.");
            if(facts.healAuthorized()&&!facts.healEquipped())
                notes.add("Heal automático autorizado, porém não está equipado.");
            else if(facts.healAuthorized())
                notes.add("Cura automática autorizada; o servidor ainda valida mana e cooldown.");
            else notes.add("Cura automática não autorizada. Nenhuma magia será acionada.");
        }else if(health>=0&&health<=50){
            notes.add("Vida reduzida. Recomendo cautela no próximo combate.");
        }
        if(facts.armMonitored()&&Double.isFinite(facts.armIntegrity())&&
            facts.armIntegrity()<=25)
            notes.add("Integridade RP do braço em nível crítico (indicador narrativo local).");
        if(mana>=0&&mana<=20)
            notes.add("Mana baixa. Algumas sequências de magias podem falhar.");
        if(facts.macroRunning())
            notes.add("Uma macro está solicitando magias. Evite ações conflitantes.");
        if(notes.isEmpty())
            notes.add("Sem alertas críticos nos indicadores atualmente observáveis.");
        return new Assessment(health,mana,risk,List.copyOf(notes),critical);
    }
}
