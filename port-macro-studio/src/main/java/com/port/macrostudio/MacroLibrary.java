package com.port.macrostudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Local-only macro definitions. Never load class names, JavaScript or free-form
 * commands from disk. The executor supports a strict allowlist of step types.
 */
public final class MacroLibrary {
    public static final int MACROS=4, MAX_STEPS=24;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("port-macro-studio.json");

    public static class Step {
        public String type="NOTIFY";
        public String text="Etapa concluída";
        public int ticks=20;
        public int slot=1;
        public float threshold=8;
        public Step(){}
        public Step(String type){this.type=type;}
        public static Step of(String type){return new Step(type);}
    }
    public static class Macro {
        public String name;
        public List<Step> steps=new ArrayList<>();
        public Macro(){}
        public Macro(String name){this.name=name;}
    }
    private static class Data {
        int selected=0;
        List<Macro> macros=new ArrayList<>();
    }
    private static Data data=new Data();

    public static void load(){
        data=defaults();
        if(Files.isRegularFile(FILE)){
            try{
                Data loaded=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Data.class);
                if(loaded!=null&&loaded.macros!=null)data=loaded;
            }catch(Exception e){System.err.println("[Port Macro] Arquivo inválido. Usando macros padrão.");}
        }
        sanitize();save();
    }
    private static Data defaults(){
        Data d=new Data();
        Macro a=new Macro("Apresentação RP");
        a.steps.add(notify("Preparando apresentação"));
        a.steps.add(waitTicks(40));
        a.steps.add(notify("Pronto para a cena"));
        d.macros.add(a);
        Macro b=new Macro("Troca rápida");
        b.steps.add(notify("Selecionando item da hotbar"));
        Step slot=Step.of("HOTBAR");slot.slot=1;b.steps.add(slot);
        d.macros.add(b);
        Macro c=new Macro("Segurança");
        Step guard=Step.of("HEALTH_BELOW");guard.threshold=10;c.steps.add(guard);
        c.steps.add(notify("Vida abaixo de 10 HP"));
        d.macros.add(c);
        Macro d4=new Macro("Personalizada");
        d4.steps.add(notify("Nova macro pronta"));
        d.macros.add(d4);
        return d;
    }
    private static Step notify(String message){Step s=Step.of("NOTIFY");s.text=message;return s;}
    private static Step waitTicks(int ticks){Step s=Step.of("WAIT");s.ticks=ticks;return s;}
    private static final List<String> ALLOWED=List.of(
        "WAIT","NOTIFY","CHAT","HOTBAR","HEALTH_BELOW","SNEAKING","STOP");

    private static void sanitize(){
        data.selected=Math.max(0,Math.min(MACROS-1,data.selected));
        if(data.macros==null)data.macros=new ArrayList<>();
        while(data.macros.size()>MACROS)data.macros.remove(data.macros.size()-1);
        while(data.macros.size()<MACROS)data.macros.add(new Macro("Macro "+(data.macros.size()+1)));
        for(int i=0;i<MACROS;i++){
            Macro m=data.macros.get(i);
            if(m==null){m=new Macro("Macro "+(i+1));data.macros.set(i,m);}
            if(m.name==null||m.name.isBlank())m.name="Macro "+(i+1);
            m.name=cleanText(m.name,30);
            if(m.steps==null)m.steps=new ArrayList<>();
            m.steps.removeIf(s->s==null||!ALLOWED.contains(s.type));
            if(m.steps.size()>MAX_STEPS)m.steps=new ArrayList<>(m.steps.subList(0,MAX_STEPS));
            for(Step s:m.steps){
                s.text=cleanText(s.text==null?"":s.text,120);
                s.ticks=Math.max(1,Math.min(200,s.ticks));
                s.slot=Math.max(1,Math.min(9,s.slot));
                s.threshold=Math.max(1,Math.min(20,Float.isFinite(s.threshold)?s.threshold:8));
            }
        }
    }
    private static String cleanText(String value,int max){
        return value.replaceAll("[\r\n\t\\p{Cntrl}]"," ").strip().substring(0,Math.min(max,value.strip().length()));
    }
    public static void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE,JSON.toJson(data),StandardCharsets.UTF_8);
        }catch(Exception e){System.err.println("[Port Macro] Falha ao salvar macros.");}
    }
    public static int selected(){return data.selected;}
    public static void select(int id){if(id>=0&&id<MACROS){data.selected=id;save();}}
    public static Macro macro(int i){return data.macros.get(Math.max(0,Math.min(MACROS-1,i)));}
    public static Macro selectedMacro(){return macro(data.selected);}
    public static boolean add(String kind){
        if(!ALLOWED.contains(kind)||selectedMacro().steps.size()>=MAX_STEPS)return false;
        Step s=Step.of(kind);
        if("CHAT".equals(kind))s.text="Mensagem de exemplo";
        if("NOTIFY".equals(kind))s.text="Notificação local";
        selectedMacro().steps.add(s);
        save();return true;
    }
    public static boolean delete(int index){
        List<Step> steps=selectedMacro().steps;
        if(index<0||index>=steps.size())return false;
        steps.remove(index);save();return true;
    }
    public static boolean move(int index,int delta){
        List<Step> steps=selectedMacro().steps;
        int next=index+delta;
        if(index<0||next<0||index>=steps.size()||next>=steps.size())return false;
        java.util.Collections.swap(steps,index,next);save();return true;
    }
    public static String description(Step s){
        return switch(s.type){
            case "WAIT"->"Aguardar "+(s.ticks/20.0)+" s";
            case "NOTIFY"->"Aviso: "+s.text;
            case "CHAT"->"Chat: "+s.text;
            case "HOTBAR"->"Hotbar: slot "+s.slot;
            case "HEALTH_BELOW"->"Continuar se HP < "+s.threshold;
            case "SNEAKING"->"Continuar se agachado";
            case "STOP"->"Encerrar execução";
            default->"Etapa desconhecida";
        };
    }
    private MacroLibrary(){}
}
