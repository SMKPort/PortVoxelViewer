package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Local-only, normalized HUD placement so GUI scale and resolution can change. */
final class NexusHudSettings {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-hud.json");
    private static State state;
    static final class Layout {
        float x,y,scale;
        boolean enabled=true;
        Layout(float x,float y,float scale){
            this.x=x;this.y=y;this.scale=scale;
        }
    }
    private static final class State {
        int schema=1;
        Layout combat=new Layout(.03f,.20f,1f);
        Layout magic=new Layout(.75f,.20f,1f);
    }
    private NexusHudSettings(){}
    static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(PATH))return;
        try{
            State saved=JSON.fromJson(Files.readString(PATH,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==1){
                state=saved;
                if(state.combat==null)state.combat=new Layout(.03f,.20f,1f);
                if(state.magic==null)state.magic=new Layout(.75f,.20f,1f);
                sanitize(state.combat);sanitize(state.magic);
            }
        }catch(Exception ex){System.err.println("Nexus HUD: posições antigas inválidas; usando padrão.");}
    }
    private static float clamp(float x,float min,float max,float fallback){
        return Float.isFinite(x)?Math.max(min,Math.min(max,x)):fallback;
    }
    private static void sanitize(Layout layout){
        layout.x=clamp(layout.x,0,1,0);layout.y=clamp(layout.y,0,1,0);
        layout.scale=clamp(layout.scale,.55f,2f,1f);
    }
    static synchronized Layout get(String id){
        load();
        Layout source="magic".equals(id)?state.magic:state.combat;
        Layout copy=new Layout(source.x,source.y,source.scale);
        copy.enabled=source.enabled;return copy;
    }
    static synchronized void place(String id,float x,float y){
        load();Layout item="magic".equals(id)?state.magic:state.combat;
        item.x=x;item.y=y;sanitize(item);save();
    }
    static synchronized void scale(String id,float value){
        load();Layout item="magic".equals(id)?state.magic:state.combat;
        item.scale=value;sanitize(item);save();
    }
    static synchronized void enabled(String id,boolean on){
        load();("magic".equals(id)?state.magic:state.combat).enabled=on;save();
    }
    static synchronized void reset(){
        state=new State();save();
    }
    private static void save(){
        try{
            Files.createDirectories(PATH.getParent());
            Path temp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(state),StandardCharsets.UTF_8);
            try{Files.move(temp,PATH,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,PATH,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception ex){System.err.println("Nexus HUD: não foi possível salvar os ajustes.");}
    }
}
