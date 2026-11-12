package com.port.performance;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;

/**
 * Opt-in, local-only optimization experiment. Never changes render distance,
 * particles, server state or multiplayer rules.
 * All option writes happen on Minecraft's client thread.
 */
final class BoosterEngine {
    private static final int BASELINE_SECONDS=30;
    private static final int TRIAL_SECONDS=30;
    private static final ArrayDeque<Integer> recentFps=new ArrayDeque<>();
    private static String mode="off",phase="idle",lastAction="Nenhum ajuste aplicado.";
    private static int secondsInPhase=0,validSamples=0;
    private static double sum=0,baselineAvg=0,trialAvg=0,changePct=0;
    private static double originalScaling=-1,appliedScaling=-1;
    private static boolean modified=false;
    private static boolean bootRestoreChecked=false;
    private BoosterEngine(){}

    static void sample(){
        Minecraft mc=Minecraft.getInstance();
        if(!bootRestoreChecked&&mc.options!=null){
            bootRestoreChecked=true;
            if(PerformanceConfig.pendingRestoreOriginal>0){
                double now=mc.options.entityDistanceScaling().get();
                // Restore only our own last applied value, not a manual user change.
                if(Math.abs(now-PerformanceConfig.pendingRestoreApplied)<0.025){
                    mc.options.entityDistanceScaling().set(PerformanceConfig.pendingRestoreOriginal);
                    mc.options.save();
                }
                PerformanceConfig.clearPendingRestore();
            }
        }
        if(mc.player==null||mc.level==null){
            if(modified)restore("Restaurado após sair do mundo.");
            if(!"off".equals(mode))mode="off";
            phase="idle";
            return;
        }
        int fps=PerformanceStats.fps();
        if(fps>0&&fps<=1000){
            recentFps.addLast(fps);
            if(recentFps.size()>120)recentFps.removeFirst();
        }
        if(!"adaptive".equals(mode)||!mc.isWindowActive())return;
        if(!"baseline".equals(phase)&&!"trial".equals(phase))return;
        if(fps<=0||fps>1000)return;
        secondsInPhase++;sum+=fps;validSamples++;
        if("baseline".equals(phase)&&secondsInPhase>=BASELINE_SECONDS){
            baselineAvg=sum/Math.max(1,validSamples);
            if(baselineAvg>=PerformanceConfig.boosterTargetFps){
                phase="target-met";
                lastAction="FPS já está acima da meta. Nenhum ajuste necessário.";
                return;
            }
            originalScaling=mc.options.entityDistanceScaling().get();
            // A conservative option, only after explicit user opt-in.
            double candidate=Math.max(0.5,Math.min(0.85,originalScaling*0.85));
            if(candidate>=originalScaling-0.025){
                phase="no-change";
                lastAction="Distância de entidades já estava baixa. Nada alterado.";
                return;
            }
            appliedScaling=candidate;
            PerformanceConfig.notePendingRestore(originalScaling,candidate);
            mc.options.entityDistanceScaling().set(candidate);
            mc.options.save();
            modified=true;
            phase="trial";
            secondsInPhase=0;validSamples=0;sum=0;
            lastAction="Testando distância de entidades "+Math.round(candidate*100)+"% por 30 s.";
        }else if("trial".equals(phase)&&secondsInPhase>=TRIAL_SECONDS){
            trialAvg=sum/Math.max(1,validSamples);
            changePct=baselineAvg>0?100*(trialAvg-baselineAvg)/baselineAvg:0;
            if(trialAvg>=baselineAvg+4&&changePct>=6){
                phase="kept";
                lastAction="Melhora observada: "+Math.round(changePct)+"%. Ajuste mantido, reversível.";
            }else{
                restore("Teste não mostrou ganho suficiente. Configuração restaurada.");
                phase="reverted";
            }
        }
    }

    static void observe(){
        restore("Restaurando opção anterior.");
        mode="observe";phase="observing";resetCounters();
        lastAction="Monitoramento ativo, sem otimizações automáticas.";
    }

    static void adaptive(){
        restore("Restaurando opção anterior.");
        mode="adaptive";phase="baseline";resetCounters();
        lastAction="Coletando 30 amostras de referência. Não altera render distance nem partículas.";
    }

    static void disable(){
        restore("Ajustes revertidos.");
        mode="off";phase="idle";resetCounters();
        lastAction="Booster desativado. Configurações originais preservadas.";
    }

    static void reset(){
        disable();recentFps.clear();baselineAvg=0;trialAvg=0;changePct=0;
        lastAction="Experimento reiniciado e opções restauradas.";
    }

    static void setTarget(int target){
        PerformanceConfig.boosterTargetFps=Math.max(30,Math.min(240,target));
        PerformanceConfig.save();
        if("adaptive".equals(mode))adaptive();
    }

    static void webAction(String action,Integer target){
        if(!PerformanceConfig.webControlEnabled)return;
        if(target!=null)setTarget(target);
        if(action==null)return;
        switch(action){
            case "observe"->observe();
            case "auto"->adaptive();
            case "off"->disable();
            case "reset"->reset();
            default->{}
        }
    }

    static void restore(String note){
        if(modified){
            Minecraft mc=Minecraft.getInstance();
            if(mc.options!=null){
                double actual=mc.options.entityDistanceScaling().get();
                if(Math.abs(actual-appliedScaling)<0.025){
                    mc.options.entityDistanceScaling().set(originalScaling);
                    mc.options.save();
                }else{
                    note="Outra alteração de distância foi detectada. Ajuste manual preservado.";
                }
            }
        }
        PerformanceConfig.clearPendingRestore();
        modified=false;originalScaling=-1;appliedScaling=-1;
        lastAction=note;
    }

    static String status(){
        return "Modo "+mode+" | Fase "+phase+" | Meta "+PerformanceConfig.boosterTargetFps+
            " FPS | "+lastAction;
    }

    private static void resetCounters(){
        secondsInPhase=0;validSamples=0;sum=0;
        baselineAvg=0;trialAvg=0;changePct=0;
    }

    static void snapshot(JsonObject json){
        JsonObject b=new JsonObject();
        b.addProperty("mode",mode);
        b.addProperty("phase",phase);
        b.addProperty("targetFps",PerformanceConfig.boosterTargetFps);
        b.addProperty("webControlEnabled",PerformanceConfig.webControlEnabled);
        b.addProperty("baselineAvgFps",Math.round(baselineAvg*10)/10.0);
        b.addProperty("trialAvgFps",Math.round(trialAvg*10)/10.0);
        b.addProperty("differencePct",Math.round(changePct*10)/10.0);
        b.addProperty("secondsInPhase",secondsInPhase);
        b.addProperty("originalEntityScale",originalScaling>0?originalScaling:1);
        b.addProperty("appliedEntityScale",modified?appliedScaling:-1);
        b.addProperty("changedOption",modified);
        b.addProperty("low1SampledFps",estimatedLow1());
        b.addProperty("lastAction",lastAction);
        json.add("booster",b);
    }

    private static double estimatedLow1(){
        if(recentFps.isEmpty())return 0;
        int[] values=recentFps.stream().mapToInt(Integer::intValue).sorted().toArray();
        int count=Math.max(1,(int)Math.ceil(values.length*0.01));
        long sum=0;
        for(int i=0;i<count;i++)sum+=values[i];
        return Math.round(sum*10.0/count)/10.0;
    }
}
