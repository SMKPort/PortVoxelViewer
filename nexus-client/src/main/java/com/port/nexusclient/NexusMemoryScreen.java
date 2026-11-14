package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Manage facts explicitly provided by the owner, never by the AI. */
final class NexusMemoryScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,MUTED=0xFFAA9A8D,GREEN=0xFFA3E1B2;
    private static final int PAGE_SIZE=4;
    private final Screen parent;
    private EditBox topicField,textField;
    private int page;
    private long selectedId;
    private String note;
    private long confirmDeleteId,confirmUntil;

    NexusMemoryScreen(Screen parent){
        this(parent,0,0,"Anote um fato para o Nexus lembrar quando o assunto surgir.");
    }
    private NexusMemoryScreen(Screen parent,int page,long selectedId,String note){
        super(Component.literal("Nexus Aterna | Memórias Manuais"));
        this.parent=parent;this.page=page;this.selectedId=selectedId;this.note=note;
    }
    private int left(){return (width-Math.min(550,width-20))/2;}
    private int panelWidth(){return Math.min(550,width-20);}
    private int visibleCount(){return Math.min(PAGE_SIZE,Math.max(1,(height-207)/27));}

    @Override protected void init(){
        int x=left(),w=panelWidth();
        topicField=new EditBox(font,x+8,61,w-16,20,Component.literal("Assunto da memória"));
        topicField.setMaxLength(80);
        topicField.setHint(Component.literal("Assunto (ex.: chave da oficina)"));
        addRenderableWidget(topicField);

        textField=new EditBox(font,x+8,90,w-16,20,Component.literal("Fato a recordar"));
        textField.setMaxLength(480);
        textField.setHint(Component.literal("Fato (ex.: Gabriel está com a chave)"));
        addRenderableWidget(textField);
        for(NexusOwnerMemories.Memory memory:NexusOwnerMemories.list()){
            if(memory.id==selectedId){
                topicField.setValue(memory.topic);
                textField.setValue(memory.text);
                break;
            }
        }

        int third=(w-29)/3,actionsY=119;
        addRenderableWidget(Button.builder(Component.literal(selectedId>0?"§aSALVAR ALTERAÇÃO":"§aSALVAR MEMÓRIA"),
            b->save()).bounds(x+8,actionsY,third,20).build());
        addRenderableWidget(Button.builder(Component.literal("NOVA"),b->open(0,"Nova memória."))
            .bounds(x+15+third,actionsY,third,20).build());
        addRenderableWidget(Button.builder(Component.literal("§cAPAGAR SELECIONADA"),b->delete())
            .bounds(x+22+third*2,actionsY,third,20).build());

        List<NexusOwnerMemories.Memory> memories=NexusOwnerMemories.list();
        int visible=visibleCount();
        int maxPage=Math.max(0,(memories.size()-1)/visible);
        page=Math.max(0,Math.min(page,maxPage));
        for(int i=page*visible;i<Math.min(memories.size(),(page+1)*visible);i++){
            NexusOwnerMemories.Memory memory=memories.get(i);
            String label=memory.id==selectedId?"§6● "+memory.topic:"§f"+memory.topic;
            int rowY=158+(i%visible)*27;
            addRenderableWidget(Button.builder(Component.literal(shorten(label,Math.max(50,w-42))),
                b->select(memory)).bounds(x+8,rowY,w-16,20).build());
        }
        int pageY=Math.min(height-57,163+visible*27);
        addRenderableWidget(Button.builder(Component.literal("◀"),b->{
            if(page>0)openPage(page-1);
        }).bounds(x+8,pageY,42,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),b->{
            if(page<maxPage)openPage(page+1);
        }).bounds(x+w-50,pageY,42,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-95,height-30,87,20).build());
    }

    private void save(){
        try{
            NexusOwnerMemories.Memory value=NexusOwnerMemories.save(
                selectedId,topicField.getValue(),textField.getValue());
            open(value.id,"Memória salva no PC. Ela será consultada quando pertinente.");
        }catch(IllegalArgumentException|IllegalStateException error){note=error.getMessage();}
    }
    private void delete(){
        if(selectedId<=0){note="Selecione uma memória da lista antes de apagar.";return;}
        long now=System.currentTimeMillis();
        if(confirmDeleteId!=selectedId||now>confirmUntil){
            confirmDeleteId=selectedId;confirmUntil=now+10000;
            note="Clique em APAGAR novamente para confirmar a exclusão definitiva.";
            return;
        }
        try{
            NexusOwnerMemories.delete(selectedId);
            open(0,"Memória apagada.");
        }catch(IllegalArgumentException|IllegalStateException error){note=error.getMessage();}
    }
    private void select(NexusOwnerMemories.Memory entry){
        selectedId=entry.id;confirmDeleteId=0;
        topicField.setValue(entry.topic);textField.setValue(entry.text);
        note="Editando: "+entry.topic+". Salve para substituir o registro.";
    }
    private void open(long id,String message){
        minecraft.setScreen(new NexusMemoryScreen(parent,page,id,message));
    }
    private void openPage(int next){
        minecraft.setScreen(new NexusMemoryScreen(parent,next,0,"Selecione uma memória para editar."));
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=panelWidth();
        g.fill(x-5,9,x+w+5,height-7,0xF00F0A13);
        g.fill(x-5,9,x+w+5,12,0xFFE9BB68);
        g.drawCenteredString(font,"NEXUS ATERNA  /  ARQUIVO DE MEMÓRIAS",width/2,21,GOLD);
        g.drawString(font,"Adicione assuntos e fatos em seu próprio arquivo local.",x+8,42,MUTED,false);
        int count=NexusOwnerMemories.list().size();
        g.drawString(font,"Memórias: "+count+"/200  |  Página "+(page+1),x+58,Math.min(height-52,285),MUTED,false);
        if(height>=365){
            g.drawString(font,shorten(note,w-16),x+8,height-48,GREEN,false);
            g.drawString(font,"As memórias relevantes são enviadas ao Gemini só quando você pergunta no privado.",
                x+8,height-38,MUTED,false);
        }
        super.render(g,mx,my,partialTick);
    }
    private String shorten(String text,int maxWidth){
        if(font==null||font.width(text)<=maxWidth)return text;
        while(text.length()>1&&font.width(text+"…")>maxWidth)
            text=text.substring(0,text.length()-1);
        return text+"…";
    }
}
