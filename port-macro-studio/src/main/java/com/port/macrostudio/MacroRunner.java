package com.port.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sequential executor, called exclusively on the Minecraft client thread.
 * No loops, reflection, network hooks or arbitrary server commands.
 */
public final class MacroRunner {
    private static final long START_COOLDOWN_MS=10000, CHAT_COOLDOWN_MS=5000, MAX_RUN_MS=45000;
    private static boolean running=false,chatPermission=false,paused=false;
    private static long lastStart=0,lastChat=0,startedAt=0,waitUntil=0;
    private static int cursor=0,chatSent=0,selectedSlot=-1;
    private static String status="Pronto.";

    public static String status(){return status;}
    public static boolean running(){return running;}
    public static boolean paused(){return paused;}
    public static boolean allowChat(){return chatPermission;}
    public static void chatPermission(boolean yes){
        chatPermission=yes;
        status=yes?"Envio de mensagens autorizado para macros.":"Mensagens de chat bloqueadas.";
        say(status);
    }
    public static boolean start(int slot){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.getConnection()==null)return fail("Entre em um mundo antes de iniciar.");
        if(running)return fail("Uma macro já está em execução.");
        if(slot<0||slot>=MacroLibrary.MACROS)return fail("Slot de macro inválido.");
        List<MacroLibrary.Step> steps=MacroLibrary.macro(slot).steps;
        if(steps.isEmpty())return fail("Essa macro não tem etapas.");
        if(steps.size()>MacroLibrary.MAX_STEPS)return fail("Limite de etapas excedido.");
        long time=System.currentTimeMillis();
        if(time-lastStart<START_COOLDOWN_MS)return fail("Proteção contra repetição: aguarde alguns segundos.");
        selectedSlot=slot;lastStart=time;startedAt=time;waitUntil=time;cursor=0;chatSent=0;
        running=true;paused=false;status="Executando "+MacroLibrary.macro(slot).name;
        say("Executando macro "+(slot+1)+": "+MacroLibrary.macro(slot).name);
        return true;
    }
    public static void pauseToggle(){
        if(!running)return;
        paused=!paused;
        status=paused?"Macro pausada.":"Macro retomada.";
        say(status);
    }
    public static void stop(String reason){
        if(!running)return;
        running=false;paused=false;selectedSlot=-1;cursor=0;
        status=reason; say(reason);
    }
    private static boolean fail(String reason){status=reason;say(reason);return false;}

    public static void tick(){
        if(!running||paused)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.getConnection()==null){
            stop("Macro cancelada: Minecraft desconectado.");return;
        }
        long now=System.currentTimeMillis();
        if(now-startedAt>MAX_RUN_MS){stop("Macro interrompida: limite de 45 segundos.");return;}
        if(now<waitUntil)return;
        List<MacroLibrary.Step> steps=MacroLibrary.macro(selectedSlot).steps;
        if(cursor>=steps.size()){stop("Macro concluída.");return;}
        MacroLibrary.Step step=steps.get(cursor);
        try{
            switch(step.type){
                case "WAIT"->waitUntil=now+Math.max(1,Math.min(200,step.ticks))*50L;
                case "NOTIFY"->say(step.text==null?"Aviso":step.text);
                case "HOTBAR"->{
                    int slot=Math.max(1,Math.min(9,step.slot));
                    mc.player.getInventory().selected=slot-1;
                }
                case "HEALTH_BELOW"->{
                    if(!(mc.player.getHealth()<step.threshold)){
                        stop("Condição de vida não atendida. Macro interrompida.");return;
                    }
                }
                case "SNEAKING"->{
                    if(!mc.player.isShiftKeyDown()){
                        stop("Condição não atendida: jogador não está agachado.");return;
                    }
                }
                case "CHAT"->{
                    if(!chatPermission){stop("Chat bloqueado. Habilite com /portmacro chat on.");return;}
                    if(chatSent>=3){stop("Limite: máximo de três mensagens por execução.");return;}
                    String msg=(step.text==null?"":step.text).trim();
                    if(msg.isBlank()||msg.length()>120||msg.startsWith("/")||msg.matches("(?s).*[\r\n\\p{Cntrl}].*")){
                        stop("Mensagem inválida. Apenas texto normal, sem comandos.");return;
                    }
                    if(now-lastChat<CHAT_COOLDOWN_MS){
                        waitUntil=lastChat+CHAT_COOLDOWN_MS;return;
                    }
                    mc.getConnection().sendChat(msg);
                    lastChat=now;chatSent++;
                }
                case "STOP"->{stop("Macro encerrada por uma etapa STOP.");return;}
                default->{stop("Etapa não autorizada.");return;}
            }
        }catch(Exception e){stop("Etapa falhou: "+e.getClass().getSimpleName());return;}
        cursor++;
    }

    private static void say(String message){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.player.displayClientMessage(Component.literal("§6[PORT MACRO] §f"+message),false);
    }
    private MacroRunner(){}
}
