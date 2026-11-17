package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Edit both activation-by-voice and local Nexus start/finish announcements. */
final class NexusMacroPhrasesScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private EditBox trigger,start,finish;
    private boolean hud,chat;
    private String message="Fale 'Nexus, [frase]' com VoiceCast ativo. Nada é enviado ao servidor.";

    NexusMacroPhrasesScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Frases da macro"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private String shorten(String raw,int pixels){
        String s=String.valueOf(raw);
        while(s.length()>1&&font.width(s)>pixels)s=s.substring(0,s.length()-1);
        return s;
    }
    @Override protected void init(){
        int x=left(),w=pane(),inner=w-16,half=(inner-6)/2;
        hud=NexusCombatMacro.announceHud();
        chat=NexusCombatMacro.announceChat();
        trigger=new EditBox(font,x+8,75,inner,20,Component.literal("Frase de ativação"));
        trigger.setMaxLength(NexusMacroPhrasePolicy.MAX_ACTIVATION);
        trigger.setValue(NexusCombatMacro.voicePhrase());
        trigger.setHint(Component.literal("ex.: iniciar julgamento divino"));
        addRenderableWidget(trigger);
        start=new EditBox(font,x+8,126,inner,20,Component.literal("Nexus anuncia ao iniciar"));
        start.setMaxLength(NexusMacroPhrasePolicy.MAX_ANNOUNCEMENT);
        start.setValue(NexusCombatMacro.startPhrase());
        addRenderableWidget(start);
        finish=new EditBox(font,x+8,177,inner,20,Component.literal("Nexus anuncia ao concluir"));
        finish.setMaxLength(NexusMacroPhrasePolicy.MAX_ANNOUNCEMENT);
        finish.setValue(NexusCombatMacro.finishPhrase());
        addRenderableWidget(finish);
        addRenderableWidget(Button.builder(Component.literal(hud?"§aHUD ON":"§cHUD OFF"),
            b->{
                hud=!hud;
                message="HUD "+(hud?"ligado.":"desligado.")+" Salve para aplicar.";
                // Rebuilding loses user drafts; update only on SAVE.
                b.setMessage(Component.literal(hud?"§aHUD ON":"§cHUD OFF"));
            }).bounds(x+8,215,half,20).build());
        addRenderableWidget(Button.builder(Component.literal(chat?"§aCHAT LOCAL ON":"§cCHAT LOCAL OFF"),
            b->{
                chat=!chat;
                message="Chat local "+(chat?"ligado.":"desligado.")+" Salve para aplicar.";
                b.setMessage(Component.literal(chat?"§aCHAT LOCAL ON":"§cCHAT LOCAL OFF"));
            }).bounds(x+14+half,215,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("§6SALVAR FRASES DA MACRO"),
            b->save()).bounds(x+8,247,inner,22).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-103,height-30,95,20).build());
    }
    private void save(){
        if(!trigger.getValue().isBlank()&&
            !NexusMacroPhrasePolicy.validActivation(trigger.getValue())){
            message="Frase inválida. Use de 4 a 55 caracteres, sem palavras reservadas.";
            return;
        }
        String activation=NexusCombatMacro.setVoicePhrase(trigger.getValue());
        if(activation.startsWith("Frase já")||activation.startsWith("Outra macro")||
           activation.startsWith("Frase inválida")||activation.startsWith("Selecione")){
            message=activation;return;
        }
        message=NexusCombatMacro.setAnnouncements(start.getValue(),finish.getValue(),
            hud,chat)+" "+activation;
    }
    @Override public void onClose(){minecraft.setScreen(new NexusMacroScreen(parent));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS | FRASES DO MACRO",width/2,20,GOLD);
        g.drawString(font,shorten("Macro: "+NexusCombatMacro.spell(),w-16),
            x+8,42,WHITE,false);
        g.drawString(font,"Você diz: Nexus, ... (ou deixe vazio para desativar)",
            x+8,61,MUTED,false);
        g.drawString(font,"Nexus anuncia ao iniciar:",x+8,111,GOLD,false);
        g.drawString(font,"Nexus anuncia ao concluir:",x+8,161,GOLD,false);
        if(height>=340){
            g.drawString(font,shorten(message,w-16),x+8,height-67,WHITE,false);
            g.drawString(font,"Mensagens apenas no seu cliente. Voz falada depende do TTS opt-in.",
                x+8,height-51,MUTED,false);
        }
        super.render(g,mx,my,partialTick);
    }
}
