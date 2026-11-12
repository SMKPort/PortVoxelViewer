package com.port.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Client-side editor. No scripts, raw commands, reflection or remote execution.
 * Each action is a predefined step type with validated fields.
 */
public final class MacroStudioScreen extends Screen {
    private int selectedIndex=-1,scroll=0;
    private int left,screenWidth,visibleRows;
    private EditBox messageBox,macroName;
    private String message="Selecione uma etapa para editar.";
    private boolean initializing=false;
    private static final String[] FIRST={"WAIT","CHAT","NOTIFY","HOTBAR"};
    private static final String[] SECOND={"HEALTH_BELOW","SNEAKING","STOP"};
    private static final String[] TITLES={"Aguardar","Chat","Aviso","Slot","Vida menor","Agachado","Encerrar"};

    public MacroStudioScreen(){super(Component.literal("Port Macro Studio"));}

    @Override
    protected void init(){
        super.init();
        left=Math.max(6,width/2-198);
        screenWidth=Math.min(396,width-12);
        visibleRows=Math.max(1,Math.min(12,(height-268)/21));
        int buttonWidth=(screenWidth-18)/4;
        for(int slot=0;slot<MacroLibrary.MACROS;slot++){
            final int index=slot;
            addRenderableWidget(Button.builder(Component.literal("Macro "+(slot+1)),b->{
                MacroLibrary.select(index);
                selectedIndex=-1;scroll=0;refreshFields();
            }).bounds(left+slot*(buttonWidth+6),25,buttonWidth,19).build());
        }
        macroName=new EditBox(font,left,49,screenWidth-93,19,Component.literal("Nome da macro"));
        macroName.setMaxLength(30);
        macroName.setValue(MacroLibrary.selectedMacro().name);
        macroName.setResponder(name->{
            if(initializing)return;
            if(!name.isBlank()){
                MacroLibrary.selectedMacro().name=name;
                MacroLibrary.save();
            }
        });
        addRenderableWidget(macroName);
        addRenderableWidget(Button.builder(Component.literal("Salvar"),b->{
            MacroLibrary.save();message="Arquivo local salvo.";
        }).bounds(left+screenWidth-87,49,87,19).build());
        for(int i=0;i<FIRST.length;i++){
            final String type=FIRST[i];
            addRenderableWidget(Button.builder(Component.literal(TITLES[i]),b->append(type))
                .bounds(left+i*(buttonWidth+6),75,buttonWidth,19).build());
        }
        for(int i=0;i<SECOND.length;i++){
            final String type=SECOND[i];
            addRenderableWidget(Button.builder(Component.literal(TITLES[i+4]),b->append(type))
                .bounds(left+i*(buttonWidth+6),100,buttonWidth,19).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Chat ON/OFF"),b->{
            MacroRunner.chatPermission(!MacroRunner.allowChat());
            message=MacroRunner.allowChat()?"Chat autorizado somente nesta sessão.":"Chat bloqueado nesta sessão.";
        }).bounds(left+3*(buttonWidth+6),100,buttonWidth,19).build());

        messageBox=new EditBox(font,left,height-93,screenWidth,18,Component.literal("Mensagem da etapa"));
        messageBox.setMaxLength(120);
        messageBox.setResponder(value->{
            if(initializing)return;
            var step=selected();
            if(step!=null&&("NOTIFY".equals(step.type)||"CHAT".equals(step.type))){
                step.text=value;MacroLibrary.save();
            }
        });
        addRenderableWidget(messageBox);
        int controlWidth=(screenWidth-18)/4;
        addRenderableWidget(Button.builder(Component.literal("Valor -"),b->modify(-1))
            .bounds(left,height-69,controlWidth,19).build());
        addRenderableWidget(Button.builder(Component.literal("Valor +"),b->modify(1))
            .bounds(left+controlWidth+6,height-69,controlWidth,19).build());
        addRenderableWidget(Button.builder(Component.literal("Mover ↑"),b->shift(-1))
            .bounds(left+2*(controlWidth+6),height-69,controlWidth,19).build());
        addRenderableWidget(Button.builder(Component.literal("Mover ↓"),b->shift(1))
            .bounds(left+3*(controlWidth+6),height-69,controlWidth,19).build());
        addRenderableWidget(Button.builder(Component.literal("▶ Iniciar"),b->{
            if(MacroRunner.start(MacroLibrary.selected()))minecraft.setScreen(null);
            else message=MacroRunner.status();
        }).bounds(left,height-41,controlWidth,21).build());
        addRenderableWidget(Button.builder(Component.literal("■ Parar"),b->{
            MacroRunner.stop("Execução interrompida manualmente.");message=MacroRunner.status();
        }).bounds(left+controlWidth+6,height-41,controlWidth,21).build());
        addRenderableWidget(Button.builder(Component.literal("Excluir"),b->{
            if(MacroLibrary.delete(selectedIndex)){
                selectedIndex=Math.min(selectedIndex,MacroLibrary.selectedMacro().steps.size()-1);
                refreshFields();message="Etapa excluída.";
            }
        }).bounds(left+2*(controlWidth+6),height-41,controlWidth,21).build());
        addRenderableWidget(Button.builder(Component.literal("Página ▶"),b->{
            scroll=Math.min(Math.max(0,MacroLibrary.selectedMacro().steps.size()-visibleRows),scroll+visibleRows);
        }).bounds(left+3*(controlWidth+6),height-41,controlWidth,21).build());
        refreshFields();
    }

    private MacroLibrary.Step selected(){
        List<MacroLibrary.Step> steps=MacroLibrary.selectedMacro().steps;
        return selectedIndex>=0&&selectedIndex<steps.size()?steps.get(selectedIndex):null;
    }
    private void refreshFields(){
        initializing=true;
        if(macroName!=null)macroName.setValue(MacroLibrary.selectedMacro().name);
        MacroLibrary.Step step=selected();
        if(messageBox!=null){
            boolean editable=step!=null&&("NOTIFY".equals(step.type)||"CHAT".equals(step.type));
            messageBox.setEditable(editable);
            messageBox.setValue(editable?step.text:"");
        }
        initializing=false;
    }
    private void append(String type){
        if(!MacroLibrary.add(type)){message="Limite: 24 etapas por macro.";return;}
        selectedIndex=MacroLibrary.selectedMacro().steps.size()-1;
        scroll=Math.max(0,selectedIndex-visibleRows+1);
        refreshFields();
        message="Etapa "+type+" adicionada.";
    }
    private void modify(int delta){
        var step=selected();if(step==null){message="Selecione uma etapa primeiro.";return;}
        switch(step.type){
            case "WAIT"->step.ticks=Math.max(1,Math.min(200,step.ticks+delta*20));
            case "HOTBAR"->step.slot=Math.max(1,Math.min(9,step.slot+delta));
            case "HEALTH_BELOW"->step.threshold=Math.max(1,Math.min(20,step.threshold+delta));
            default->{message="Esta etapa não possui valor numérico.";return;}
        }
        MacroLibrary.save();message="Valor atualizado: "+MacroLibrary.description(step);
    }
    private void shift(int direction){
        if(MacroLibrary.move(selectedIndex,direction)){
            selectedIndex+=direction;
            if(selectedIndex<scroll)scroll=selectedIndex;
            if(selectedIndex>=scroll+visibleRows)scroll=selectedIndex-visibleRows+1;
            message="Ordem atualizada.";
        }
    }

    @Override
    public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xF0121019);
        g.fill(0,0,width,3,0xFFE6B770);
        g.drawCenteredString(font,"§6§lPORT MACRO STUDIO",width/2,8,0xFFF4D4A1);
        g.drawString(font,"ETAPAS / Clique para selecionar",left,125,0xFFE9B875,true);
        g.drawString(font,"Perfil "+(MacroLibrary.selected()+1)+": "+MacroLibrary.selectedMacro().name,
            left,136,0xFFCCB9C8,true);
        List<MacroLibrary.Step> items=MacroLibrary.selectedMacro().steps;
        int startY=148;
        for(int i=0;i<visibleRows&&scroll+i<items.size();i++){
            int idx=i+scroll,y=startY+i*21;
            int background=idx==selectedIndex?0xFF55403F:0xFF24202A;
            g.fill(left,y,left+screenWidth,y+19,background);
            g.fill(left,y,left+3,y+19,idx==selectedIndex?0xFFFFDC99:0xFF94714B);
            String label=(idx+1)+". "+MacroLibrary.description(items.get(idx));
            if(label.length()>56)label=label.substring(0,55)+"…";
            g.drawString(font,label,left+8,y+5,0xFFEEE1DB,true);
        }
        String selection=selected()==null?"NENHUMA ETAPA":MacroLibrary.description(selected());
        if(selection.length()>58)selection=selection.substring(0,57)+"…";
        g.drawString(font,"EDITAR: "+selection,left,height-107,0xFFFFC982,true);
        g.drawString(font,"Mensagens não aceitam comandos. Chat: "+(MacroRunner.allowChat()?"AUTORIZADO":"BLOQUEADO"),
            left,height-117,MacroRunner.allowChat()?0xFF8ED5AA:0xFFC5A2A9,true);
        g.drawCenteredString(font,message,width/2,height-13,0xFFB9ABB9);
        super.render(g,mouseX,mouseY,partial);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(super.mouseClicked(mouseX,mouseY,button))return true;
        if(button==0&&mouseX>=left&&mouseX<=left+screenWidth&&mouseY>=148
           &&mouseY<148+visibleRows*21){
            int idx=scroll+(int)((mouseY-148)/21);
            if(idx<MacroLibrary.selectedMacro().steps.size()){
                selectedIndex=idx;refreshFields();return true;
            }
        }
        return false;
    }
    @Override
    public void onClose(){MacroLibrary.save();super.onClose();}
}
