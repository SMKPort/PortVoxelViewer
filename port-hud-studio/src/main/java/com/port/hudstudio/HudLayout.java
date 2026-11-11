package com.port.hudstudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.neoforged.fml.loading.FMLPaths;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Normalized widget layout that persists locally and works without any other mod installed. */
public final class HudLayout {
    public static class Widget {
        public String id;
        public float x,y,scale,opacity;
        public boolean visible;
        public Widget(String id,float x,float y,float scale,float opacity,boolean visible){
            this.id=id;this.x=x;this.y=y;this.scale=scale;this.opacity=opacity;this.visible=visible;
        }
    }
    private static class State {
        String profile="RPG";
        boolean enabled=true;
        boolean autoHideCinema=true;
        Map<String,List<Widget>> profiles=new LinkedHashMap<>();
    }
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("port-hud-studio.json");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final String[] IDS={"health","armor","hunger","coords","fps","ping"};
    private static State state=new State();
    private static Method cinematicPlaying;
    private static boolean bridgeChecked;

    public static void load(){
        state=new State();
        try{
            if(Files.isRegularFile(PATH)){
                State loaded=GSON.fromJson(Files.readString(PATH),State.class);
                if(loaded!=null&&loaded.profiles!=null&&loaded.profiles.size()<30)state=loaded;
            }
        }catch(Exception e){System.err.println("[Port HUD] Layout inválido. Padrões carregados.");}
        defaultsIfMissing();
        save();
    }
    private static void defaultsIfMissing(){
        if(state.profiles==null)state.profiles=new LinkedHashMap<>();
        if(!state.profiles.containsKey("RPG"))state.profiles.put("RPG",defaults("RPG"));
        if(!state.profiles.containsKey("Minimal"))state.profiles.put("Minimal",defaults("Minimal"));
        if(!state.profiles.containsKey("Performance"))state.profiles.put("Performance",defaults("Performance"));
        if(!state.profiles.containsKey(state.profile))state.profile="RPG";
        // Keep sizes and normalized positions valid even if a configuration was edited externally.
        for(List<Widget> list:state.profiles.values()){
            if(list==null)continue;
            list.removeIf(w->w==null||w.id==null||!List.of(IDS).contains(w.id));
            for(Widget w:list){
                w.x=clamp(w.x,0,.95f);w.y=clamp(w.y,0,.95f);
                w.scale=clamp(w.scale,.6f,2.4f);w.opacity=clamp(w.opacity,.3f,1f);
            }
        }
    }
    private static List<Widget> defaults(String name){
        List<Widget> a=new ArrayList<>();
        a.add(new Widget("health",.02f,.08f,1,1,!name.equals("Performance")));
        a.add(new Widget("armor",.02f,.17f,1,1,name.equals("RPG")));
        a.add(new Widget("hunger",.02f,.26f,1,1,name.equals("RPG")));
        a.add(new Widget("coords",.78f,.08f,1,1,!name.equals("Minimal")));
        a.add(new Widget("fps",.78f,.17f,1,1,!name.equals("Minimal")));
        a.add(new Widget("ping",.78f,.26f,1,1,!name.equals("Minimal")));
        return a;
    }
    public static void save(){
        try{Files.createDirectories(PATH.getParent());Files.writeString(PATH,GSON.toJson(state));}
        catch(Exception e){System.err.println("[Port HUD] Falha ao salvar o HUD.");}
    }
    public static List<Widget> widgets(){
        List<Widget> list=state.profiles.get(state.profile);
        return list==null?List.of():list;
    }
    public static String profile(){return state.profile;}
    public static String nextProfile(){
        String[] names={"RPG","Minimal","Performance"};
        int idx=java.util.Arrays.asList(names).indexOf(state.profile);
        state.profile=names[(idx+1+names.length)%names.length];
        save();return state.profile;
    }
    public static void chooseProfile(String name){if(state.profiles.containsKey(name)){state.profile=name;save();}}
    public static boolean enabled(){return state.enabled;}
    public static void toggle(){state.enabled=!state.enabled;save();}
    public static boolean hideCinema(){return state.autoHideCinema;}
    public static void toggleCinema(){state.autoHideCinema=!state.autoHideCinema;save();}
    public static Widget find(String id){for(Widget w:widgets())if(w.id.equals(id))return w;return null;}
    public static void toggleWidget(String id){Widget w=find(id);if(w!=null){w.visible=!w.visible;save();}}
    public static void scaleWidget(String id,float delta){Widget w=find(id);if(w!=null){w.scale=clamp(w.scale+delta,.6f,2.4f);save();}}
    public static void opacityWidget(String id,float delta){Widget w=find(id);if(w!=null){w.opacity=clamp(w.opacity+delta,.3f,1);save();}}
    public static void moveWidget(String id,float nx,float ny){Widget w=find(id);if(w!=null){w.x=clamp(nx,0,.95f);w.y=clamp(ny,0,.95f);}}
    public static void resetCurrent(){state.profiles.put(state.profile,defaults(state.profile));save();}

    public static boolean isCinematicPlaying(){
        if(!bridgeChecked){
            bridgeChecked=true;
            try{cinematicPlaying=Class.forName("com.port.cinematic.CinematicState").getMethod("isPlaying");}
            catch(Exception ignored){cinematicPlaying=null;}
        }
        if(cinematicPlaying==null)return false;
        try{return Boolean.TRUE.equals(cinematicPlaying.invoke(null));}
        catch(Exception ignored){return false;}
    }
    public static void render(GuiGraphics g,boolean editing,String selected){
        Minecraft mc=Minecraft.getInstance();
        if(!editing&&(!state.enabled||mc.options.hideGui||state.autoHideCinema&&isCinematicPlaying()))return;
        if(mc.player==null)return;
        for(Widget w:widgets()){
            if(!w.visible&&!editing)continue;
            int width=g.guiWidth(),height=g.guiHeight();
            int x=(int)(w.x*width),y=(int)(w.y*height);
            int boxWidth=(int)(142*w.scale),boxHeight=(int)(34*w.scale);
            x=Math.min(Math.max(1,x),Math.max(1,width-boxWidth-2));
            y=Math.min(Math.max(1,y),Math.max(1,height-boxHeight-2));
            int alpha=(int)(255*clamp(w.opacity,.3f,1f));
            if(!w.visible)alpha=95;
            int bg=(alpha<<24)|0x15131B;
            if(editing&&w.id.equals(selected))bg=(alpha<<24)|0x4B3332;
            g.pose().pushPose();
            g.pose().translate(x,y,0);
            g.pose().scale(w.scale,w.scale,1);
            g.fill(0,0,142,34,bg);
            g.fill(0,0,42,2,(alpha<<24)|0xDAB06A);
            g.drawString(mc.font,label(w.id),7,5,0xFFF2CB87,true);
            g.drawString(mc.font,value(w.id),7,18,0xFFF6F1F0,true);
            g.pose().popPose();
        }
    }
    public static boolean hit(Widget w,double mx,double my,int width,int height){
        int x=Math.min((int)(w.x*width),Math.max(1,width-(int)(142*w.scale)-2));
        int y=Math.min((int)(w.y*height),Math.max(1,height-(int)(34*w.scale)-2));
        return mx>=x&&mx<=x+142*w.scale&&my>=y&&my<=y+34*w.scale;
    }
    private static String label(String id){return switch(id){
        case "health"->"VIDA";case "armor"->"ARMADURA";case "hunger"->"FOME";
        case "coords"->"POSIÇÃO";case "fps"->"FPS";case "ping"->"PING";
        default->id.toUpperCase();
    };}
    private static String value(String id){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return "AGUARDANDO";
        return switch(id){
            case "health"->Math.round(mc.player.getHealth())+" / "+Math.round(mc.player.getMaxHealth())+" HP";
            case "armor"->mc.player.getArmorValue()+" PONTOS";
            case "hunger"->mc.player.getFoodData().getFoodLevel()+" / 20";
            case "coords"->mc.player.getBlockX()+"  "+mc.player.getBlockY()+"  "+mc.player.getBlockZ();
            case "fps"->mc.getFps()+" FPS";
            case "ping"->{PlayerInfo info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(mc.player.getUUID());
                yield info==null?"N/D":info.getLatency()+" ms";}
            default->"--";
        };
    }
    private static float clamp(float v,float lo,float hi){return Float.isFinite(v)?Math.max(lo,Math.min(hi,v)):lo;}
    private HudLayout(){}
}
