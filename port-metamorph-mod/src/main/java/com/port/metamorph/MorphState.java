package com.port.metamorph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Local persisted configuration. Incoming website payloads pass the same sanitizer. */
public final class MorphState {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG=FMLPaths.CONFIGDIR.get().resolve("port-metamorph.json");
    private static final Set<String> MATERIALS=Set.of("gold","iron","netherite","amethyst","redstone","copper");
    private static final Set<String> ANCHORS=Set.of("body","head","right_arm","left_arm");

    public static final class Cube {
        public String anchor="body",material="gold";
        public double x=0,y=1.0,z=0.0,sx=.15,sy=.15,sz=.15;
        public Cube(){}
        public Cube(String anchor,String material,double x,double y,double z,double sx,double sy,double sz){
            this.anchor=anchor;this.material=material;this.x=x;this.y=y;this.z=z;
            this.sx=sx;this.sy=sy;this.sz=sz;
        }
    }
    public static class Data {
        public String name="Normal";
        public String profile="normal";
        public boolean enabled=false,wings=false,nexus=false,horns=false;
        public float wingScale=1.0f,wingSpeed=2.0f,wingMotion=25.0f,armScale=1.0f,transitionSeconds=1.0f;
        public String material="gold";
        public List<Cube> cubes=new ArrayList<>();
    }
    private static Data current=new Data();
    private static float activation=0f;
    private static boolean lastEnabled=false;
    private static long tick=0;

    public static void load(){
        try{
            if(Files.isRegularFile(CONFIG)){
                Data candidate=JSON.fromJson(Files.readString(CONFIG,StandardCharsets.UTF_8),Data.class);
                if(candidate!=null)current=sanitize(candidate);
            }
        }catch(Exception e){System.err.println("[Port Metamorph] Config inválida; carregando padrão.");}
        activation=current.enabled?1:0;
        lastEnabled=current.enabled;
        save();
    }
    public static Data current(){return current;}
    public static float activation(){return activation;}
    public static long tickCount(){return tick;}
    public static void tick(){
        tick++;
        float step=1f/(Math.max(.2f,current.transitionSeconds)*20f);
        activation=Math.min(1f,Math.max(0f,activation+(current.enabled?step:-step)));
    }
    public static void setEnabled(boolean yes){current.enabled=yes;save();}
    public static void applyPreset(String name){
        if(name==null)return;
        String n=name.toLowerCase(java.util.Locale.ROOT);
        if(!Set.of("normal","nexus","angel","hybrid","custom").contains(n))return;
        current.profile=n;
        current.name=switch(n){
            case "nexus"->"Nexus Aterna";
            case "angel"->"Forma Angelical";
            case "hybrid"->"Forma Híbrida";
            case "custom"->"Forma Personalizada";
            default->"Normal";
        };
        if(!"custom".equals(n)){
            current.wings="angel".equals(n)||"hybrid".equals(n);
            current.nexus="nexus".equals(n)||"hybrid".equals(n);
            current.horns="hybrid".equals(n);
        }
        current.enabled=true;
        save();
    }
    public static void applyFromWeb(Data incoming){
        if(incoming==null)return;
        current=sanitize(incoming);
        save();
    }
    public static String json(){return JSON.toJson(current);}
    public static void reset(){current=new Data();activation=0f;save();}
    public static void save(){
        try{
            Files.createDirectories(CONFIG.getParent());
            Files.writeString(CONFIG,JSON.toJson(current),StandardCharsets.UTF_8);
        }catch(Exception e){System.err.println("[Port Metamorph] Falha ao salvar configuração.");}
    }
    private static Data sanitize(Data d){
        if(d.name==null||d.name.isBlank())d.name="Forma personalizada";
        d.name=d.name.replaceAll("[\\p{Cntrl}]"," ").strip();
        if(d.name.length()>40)d.name=d.name.substring(0,40);
        if(d.profile==null||!Set.of("normal","nexus","angel","hybrid","custom").contains(d.profile))d.profile="custom";
        d.wingScale=bound(d.wingScale,.3f,2.2f,1f);
        d.wingSpeed=bound(d.wingSpeed,0f,8f,2f);
        d.wingMotion=bound(d.wingMotion,0f,70f,25f);
        d.armScale=bound(d.armScale,.4f,1.8f,1f);
        d.transitionSeconds=bound(d.transitionSeconds,.2f,6f,1f);
        if(!MATERIALS.contains(d.material))d.material="gold";
        if(d.cubes==null)d.cubes=new ArrayList<>();
        if(d.cubes.size()>24)d.cubes=new ArrayList<>(d.cubes.subList(0,24));
        d.cubes.removeIf(c->c==null);
        for(Cube c:d.cubes){
            if(!ANCHORS.contains(c.anchor))c.anchor="body";
            if(!MATERIALS.contains(c.material))c.material="gold";
            c.x=bound(c.x,-1.7,1.7,0);
            c.y=bound(c.y,0,2.8,1);
            c.z=bound(c.z,-1.7,1.7,0);
            c.sx=bound(c.sx,.02,.9,.15);
            c.sy=bound(c.sy,.02,.9,.15);
            c.sz=bound(c.sz,.02,.9,.15);
        }
        return d;
    }
    private static float bound(float v,float low,float high,float fallback){
        return Float.isFinite(v)?Math.max(low,Math.min(high,v)):fallback;
    }
    private static double bound(double v,double low,double high,double fallback){
        return Double.isFinite(v)?Math.max(low,Math.min(high,v)):fallback;
    }
    private MorphState(){}
}
