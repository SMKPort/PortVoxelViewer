package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


/**
 * Fully client-side permission centre. Nothing is activated by merely
 * opening this menu; each change requires an explicit click.
 */
final class NexusPermissionsScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFEDE3D6,MUTED=0xFFB7A8A3;
    private static final int GREEN=0xFF9CE0AA,RED=0xFFFFA7A4;
    private enum Tab { GERAL, ARCANO, MEMORIAS }
    private record Entry(String id,String title){}
    private static final Entry[] GENERAL={
        new Entry("chat","Chat /l"),new Entry("telemetry","Telemetria"),
        new Entry("mind","Nexus Mind"),new Entry("autonomy","Autonomia RP"),
        new Entry("private","Canal privado"),new Entry("privateProactive","Iniciativa privada"),
        new Entry("speaker","Voz local (TTS)")
    };
    private static final Entry[] ARCANE={
        new Entry("arcane","Sensor arcano"),new Entry("nearby","Percepção próxima"),
        new Entry("reactions","Reações arcanas")
    };
    private static final Entry[] MEMORIES={
        new Entry("privateMemory","Memória privada"),new Entry("privateFreedom","Livre-arbítrio RP"),
        new Entry("diary","Diário narrativo"),new Entry("protection","Proteção contextual"),
        new Entry("share","Compartilhar no painel")
    };

    private final Screen parent;
    private final Tab tab;
    private String message;
    private String confirmation="";
    private String confirmationNick="";
    private long confirmUntil;

    NexusPermissionsScreen(){this(Tab.GERAL,null,"Menu de permissões aberto.");}
    NexusPermissionsScreen(Screen parent){this(Tab.GERAL,parent,"Menu de permissões aberto.");}
    private NexusPermissionsScreen(Tab tab,Screen parent,String message){
        super(Component.literal("Nexus Aterna | Permissões"));
        this.tab=tab;this.parent=parent;this.message=message;
    }

    private int panelW(){return Math.min(550,width-20);}
    private int left(){return (width-panelW())/2;}

    @Override
    protected void init(){
        int w=panelW(),x=left(),navW=(w-18)/3;
        String[] tabs={"GERAL","ARCANO","MEMÓRIAS"};
        for(int i=0;i<tabs.length;i++){
            final Tab next=Tab.values()[i];
            String label=(next==tab?"§6":"")+tabs[i];
            addRenderableWidget(Button.builder(Component.literal(label),
                b->open(next,"Permissões locais. Alterações são salvas no cliente."))
                .bounds(x+5+i*(navW+4),45,navW,20).build());
        }
        Entry[] entries=switch(tab){
            case GERAL -> GENERAL; case ARCANO -> ARCANE;
            case MEMORIAS -> MEMORIES;
        };
        int half=(w-20)/2,step=28;
        for(int i=0;i<entries.length;i++){
            Entry entry=entries[i];
            int bx=x+6+(i%2)*(half+6),by=77+(i/2)*step;
            addRenderableWidget(Button.builder(buttonText(entry),
                b->{
                    try{
                        if(entry.id.equals("share")&&!NexusPermissionControls.enabled("share")){
                            if(!confirm("share","",b,
                                "Compartilhar o diário com seu painel privado? Clique novamente."))return;
                        }
                        NexusPermissionControls.toggle(entry.id);
                        open(tab,entry.title+": "+(NexusPermissionControls.enabled(entry.id)?"ATIVO":"DESLIGADO"));
                    }catch(Exception error){message=entry.title+": "+error.getMessage();}
                }).bounds(bx,by,half,20).build());
        }

        int footerY=height-34,btnW=(w-22)/3;
        addRenderableWidget(Button.builder(Component.literal("§aATIVAR TUDO"),b->{
            if(!confirm("all","",b,
                "Atenção: ativa chat, TTS e compartilhamento do diário. Confirme."))return;
            try{
                NexusPermissions.all();
                open(tab,"Modo TOTAL ativo. Nenhuma escuta de jogadores é permitida.");
            }catch(Exception error){message="Ativação: "+error.getMessage();}
        }).bounds(x+5,footerY,btnW,20).build());
        addRenderableWidget(Button.builder(Component.literal("§cDESLIGAR TUDO"),b->{
            try{
                NexusPermissions.off();
                open(tab,"Recursos interrompidos. Memórias anteriores preservadas.");
            }catch(Exception error){message="Desativação: "+error.getMessage();}
        }).bounds(x+9+btnW,footerY,btnW,20).build());
        addRenderableWidget(Button.builder(Component.literal("FECHAR"),b->onClose())
            .bounds(x+13+btnW*2,footerY,btnW,20).build());
    }

    private Component buttonText(Entry entry){
        boolean enabled=NexusPermissionControls.enabled(entry.id);
        return Component.literal(entry.title+" "+(enabled?"§a[ON]":"§c[OFF]"));
    }

    private boolean confirm(String type,String nick,Button button,String prompt){
        long now=System.currentTimeMillis();
        if(!type.equals(confirmation)||!nick.equalsIgnoreCase(confirmationNick)||
            now>confirmUntil){
            confirmation=type;confirmationNick=nick;confirmUntil=now+12000;
            message=prompt;button.setMessage(Component.literal("§eCONFIRMAR"));
            return false;
        }
        confirmation="";confirmationNick="";confirmUntil=0;
        return true;
    }

    private void open(Tab next,String note){
        minecraft.setScreen(new NexusPermissionsScreen(next,parent,note));
    }

    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        int x=left(),w=panelW(),footerY=height-34;
        graphics.fill(x-5,8,x+w+5,height-8,0xEE100B10);
        graphics.fill(x-5,8,x+w+5,11,0xFFE2B55C);
        graphics.fill(x-5,38,x+w+5,39,0xFF6C2734);
        graphics.drawCenteredString(font,"NEXUS ATERNA   |   CENTRAL DE PERMISSÕES",
            width/2,18,GOLD);
        graphics.drawString(font,shortText(NexusPermissionControls.summary(),w-12),
            x+6,31,MUTED,false);

        if(tab==Tab.GERAL){
            if(height>=310){
                graphics.drawString(font,"O chat do Nexus usa a sua própria conta Minecraft.",
                    x+8,190,MUTED,false);
                graphics.drawString(font,"Audição e transcrição do Plasmo removidas.",
                    x+8,215,MUTED,false);
            }
        }else if(tab==Tab.ARCANO){
            graphics.drawString(font,"Leituras arcanas dependem do Iron's Spells presente no cliente.",
                x+8,147,MUTED,false);
            graphics.drawString(font,shortText(NexusArcaneSensor.status(),w-16),
                x+8,162,GOLD,false);
            graphics.drawString(font,"Reações precisam de Chat, Mente e Autonomia ativos.",
                x+8,179,MUTED,false);
        }else{
            graphics.drawString(font,"Memória privada: desligar aqui não apaga registros antigos.",
                x+8,169,MUTED,false);
            graphics.drawString(font,"Compartilhar no painel transmite o diário só após autorização.",
                x+8,186,MUTED,false);
            graphics.drawString(font,"Memórias antigas são preservadas, sem novas transcrições.",
                x+8,203,MUTED,false);
        }

        if(height>=350){
            graphics.drawString(font,shortText(message,w-16),x+8,footerY-36,
                message.startsWith("Não")||message.contains("falha")||message.contains("indisponível")?
                    RED:GREEN,false);
            graphics.drawString(font,shortText("Apenas client-side. F8 ou /nexusclient menu para abrir.",w-16),
                x+8,footerY-22,MUTED,false);
        }else if(height>=265){
            graphics.drawString(font,shortText(message,w-16),x+8,footerY-14,GREEN,false);
        }
        super.render(graphics,mouseX,mouseY,partialTick);
    }
    private String shortText(String value,int maxWidth){
        if(value==null)return "";
        if(font.width(value)<=maxWidth)return value;
        while(value.length()>1&&font.width(value+"…")>maxWidth)
            value=value.substring(0,value.length()-1);
        return value+"…";
    }
}
