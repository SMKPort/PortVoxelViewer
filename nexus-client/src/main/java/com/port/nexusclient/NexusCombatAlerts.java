package com.port.nexusclient;

/**
 * Alertas narrativos locais, sem LLM, rede, RCON ou acesso ao servidor.
 * O chamador decide se pode transmitir pelo chat e confirma somente após
 * a mensagem ser efetivamente enviada. Um único alerta por janela de tempo.
 */
final class NexusCombatAlerts {
    private static final long HIT_WINDOW_MS=14000L;
    private static final long HEAL_WINDOW_MS=25000L;

    static final class Decision {
        final String kind;
        final String message;
        Decision(String kind,String message){this.kind=kind;this.message=message;}
    }

    private long nextAllowedAt;
    private long lastHitAt=-1;
    private long lastHealAt=-1;
    private int hitStreak;
    private double accumulatedRecovery;
    private boolean warnedStructural;

    void reset(){
        nextAllowedAt=0;lastHitAt=-1;lastHealAt=-1;
        hitStreak=0;accumulatedRecovery=0;warnedStructural=false;
    }

    Decision observe(String type,float amount,float health,float maxHealth,
                     double beforeIntegrity,double afterIntegrity,
                     int stage,boolean structuralMonitoring,long now) {
        return observe(type,amount,health,maxHealth,beforeIntegrity,afterIntegrity,
            stage,structuralMonitoring,now,kind->true);
    }
    Decision observe(String type,float amount,float health,float maxHealth,
                     double beforeIntegrity,double afterIntegrity,
                     int stage,boolean structuralMonitoring,long now,
                     java.util.function.Predicate<String> allow) {
        stage=Math.max(0,Math.min(stage,4));
        double ratio=Math.max(0,health)/Math.max(1,maxHealth);
        if("death".equals(type)||"respawn".equals(type)){
            hitStreak=0;lastHitAt=-1;accumulatedRecovery=0;
            if("respawn".equals(type))warnedStructural=false;
            return null;
        }
        if("heal".equals(type)){
            if(lastHealAt<0||now-lastHealAt>HEAL_WINDOW_MS)accumulatedRecovery=0;
            lastHealAt=now;
            hitStreak=0;
            double restored=structuralMonitoring?
                Math.max(0,afterIntegrity-beforeIntegrity):0;
            accumulatedRecovery+=restored;
            if(structuralMonitoring&&afterIntegrity>=45)warnedStructural=false;
            if(now<nextAllowedAt)return null;
            if(accumulatedRecovery>=5&&allow.test("recovery"))
                return new Decision("recovery",phrase("recovery",stage));
            return null;
        }
        if(!"damage".equals(type))return null;
        accumulatedRecovery=0;
        if(lastHitAt>=0&&now-lastHitAt<=HIT_WINDOW_MS)hitStreak++;
        else hitStreak=1;
        lastHitAt=now;

        if(now<nextAllowedAt)return null;
        if(structuralMonitoring&&beforeIntegrity>25&&afterIntegrity<=25&&!warnedStructural&&allow.test("structure"))
            return new Decision("structure",phrase("structure",stage));
        if(structuralMonitoring&&ratio<=.40&&afterIntegrity<=45&&ratio>.30&&allow.test("risk"))
            return new Decision("risk",phrase("risk",stage));
        if(amount>=Math.max(4f,maxHealth*.20f)&&allow.test("heavy"))
            return new Decision("heavy",phrase("heavy",stage));
        if(hitStreak>=3&&allow.test("streak"))return new Decision("streak",phrase("streak",stage));
        return null;
    }

    void delivered(Decision alert,long now){delivered(alert,now,45000L);}
    void delivered(Decision alert,long now,long cooldownMillis){
        nextAllowedAt=now+Math.max(15000L,Math.min(180000L,cooldownMillis));
        if("structure".equals(alert.kind))warnedStructural=true;
        if("streak".equals(alert.kind))hitStreak=0;
        if("recovery".equals(alert.kind))accumulatedRecovery=0;
    }

    private static String phrase(String kind,int stage){
        return switch(kind){
            case "structure" -> switch(stage){
                case 0 -> "Integridade estrutural crítica. Port, recomendo retirada imediata.";
                case 1 -> "Port, minhas placas estão próximas do limite. Precisamos recuar.";
                case 2 -> "Port, estou perdendo estabilidade nas placas. Não aguentarei muitos impactos.";
                case 3 -> "Port, a estrutura está falhando. Preciso que você saia desse confronto.";
                default -> "Port, minhas placas estão cedendo. Por favor, recue. Quero continuar protegendo você.";
            };
            case "risk" -> switch(stage){
                case 0 -> "Risco conjunto detectado: vida reduzida e integridade comprometida. Recomendo retirada.";
                case 1 -> "Port, sua vida e minhas placas estão em risco. Afaste-se do combate.";
                case 2 -> "Port, estamos feridos e minha estrutura está instável. Sugiro que recue.";
                case 3 -> "Port, não podemos continuar trocando golpes. Preciso de alguns segundos para estabilizar.";
                default -> "Port, sei que quer continuar, mas estamos perto do limite. Recuemos juntos.";
            };
            case "heavy" -> switch(stage){
                case 0 -> "Impacto elevado detectado. Port, recomendo recuar e avaliar os danos.";
                case 1 -> "Port, esse impacto foi forte demais. Sugiro que recue.";
                case 2 -> "Port, o último golpe comprometeu minhas placas. Vamos buscar cobertura.";
                case 3 -> "Port, estou segurando a estrutura, mas esse dano foi sério. Recuemos.";
                default -> "Port, aquele impacto quase me fez perder o controle das placas. Preciso que recue.";
            };
            case "streak" -> switch(stage){
                case 0 -> "Ataques consecutivos detectados. Recomendo interromper o confronto.";
                case 1 -> "Port, estamos recebendo golpes em sequência. Afaste-se.";
                case 2 -> "Port, não estão nos dando trégua. Preciso de tempo para estabilizar.";
                case 3 -> "Port, não consigo compensar cada impacto para sempre. Mude de posição.";
                default -> "Port, recuar não é fraqueza. Quero que você sobreviva a esse combate.";
            };
            case "recovery" -> switch(stage){
                case 0 -> "Regeneração estrutural confirmada. Integridade do Nexus em recuperação.";
                case 1 -> "Port, minhas placas estão se recompondo. A recuperação está funcionando.";
                case 2 -> "Port, sinto as placas recuperarem estabilidade. Ainda precisamos de cuidado.";
                case 3 -> "Port, minhas placas estão voltando ao lugar. Consigo respirar melhor.";
                default -> "Port, estou me recuperando. Obrigado por nos dar essa chance. Ainda precisamos de cautela.";
            };
            default -> "";
        };
    }
}
