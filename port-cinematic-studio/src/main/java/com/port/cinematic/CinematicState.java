package com.port.cinematic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entirely local camera entity. It is never spawned on the server or added to the client world.
 * This deliberately does not modify the real player coordinates, velocity or outgoing packets.
 */
public final class CinematicState {
    public record Keyframe(double x,double y,double z,float yaw,float pitch,int durationTicks) {}
    private record SavedScene(List<Keyframe> keyframes) {}

    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("port-cinematic-studio.json");
    private static final List<Keyframe> FRAMES=new ArrayList<>();
    private static ArmorStand cameraEntity;
    private static net.minecraft.client.multiplayer.ClientLevel cameraLevel;
    private static boolean active=false,playing=false,oldHideGui=false;
    private static double x,y,z;
    private static float yaw,pitch;
    private static int frameIndex=0,frameTick=0;
    private static double lastTimeline=0;

    public static boolean isActive(){return active;}
    public static boolean isPlaying(){return playing;}
    public static List<Keyframe> frames(){return List.copyOf(FRAMES);}
    public static int segment(){return frameIndex;}
    public static double timeline(){return lastTimeline;}
    public static double x(){return x;}
    public static double y(){return y;}
    public static double z(){return z;}
    public static float yaw(){return yaw;}
    public static float pitch(){return pitch;}

    public static void load(){
        try{
            if(!Files.isRegularFile(FILE))return;
            SavedScene data=GSON.fromJson(Files.readString(FILE),SavedScene.class);
            FRAMES.clear();
            if(data!=null&&data.keyframes()!=null){
                for(Keyframe frame:data.keyframes()){
                    if(FRAMES.size()>=128)break;
                    if(valid(frame))FRAMES.add(frame);
                }
            }
        }catch(Exception e){System.err.println("[Port Cinema] Não foi possível carregar a cena.");}
    }

    private static boolean valid(Keyframe f){
        return f!=null&&Double.isFinite(f.x())&&Double.isFinite(f.y())&&Double.isFinite(f.z())
            &&Math.abs(f.x())<30_000_000&&Math.abs(f.y())<30_000_000&&Math.abs(f.z())<30_000_000
            &&Float.isFinite(f.yaw())&&Float.isFinite(f.pitch())&&f.durationTicks()>=1&&f.durationTicks()<=12000;
    }
    private static void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE,GSON.toJson(new SavedScene(FRAMES)),StandardCharsets.UTF_8);
        }catch(Exception e){System.err.println("[Port Cinema] Não foi possível salvar a cena.");}
    }

    public static boolean start(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return false;
        if(active)return true;
        cameraLevel=mc.level;
        Vec3 eye=mc.player.getEyePosition();
        x=eye.x;y=eye.y;z=eye.z;
        yaw=mc.player.getYRot();pitch=mc.player.getXRot();
        cameraEntity=new ArmorStand(EntityType.ARMOR_STAND,mc.level);
        cameraEntity.setInvisible(true);
        cameraEntity.setNoGravity(true);
        cameraEntity.noPhysics=true;
        active=true;playing=false;
        sync();
        mc.setCameraEntity(cameraEntity);
        return true;
    }
    public static void stop(){
        Minecraft mc=Minecraft.getInstance();
        ArmorStand previousCamera=cameraEntity;
        if(playing){mc.options.hideGui=oldHideGui;}
        playing=false;active=false;cameraEntity=null;cameraLevel=null;
        // Do not override a camera that another mod may have installed meanwhile.
        if(mc.player!=null&&mc.getCameraEntity()==previousCamera)mc.setCameraEntity(mc.player);
    }

    private static void sync(){
        if(!active||cameraEntity==null)return;
        // Armor stand has an eye height; offset its feet so the view occupies the requested coordinates.
        double offset=cameraEntity.getEyeHeight();
        cameraEntity.setPos(x,y-offset,z);
        cameraEntity.xo=x;cameraEntity.yo=y-offset;cameraEntity.zo=z;
        cameraEntity.setYRot(yaw);cameraEntity.setXRot(pitch);
        cameraEntity.yRotO=yaw;cameraEntity.xRotO=pitch;
    }

    public static void move(double dx,double dy,double dz){
        if(!active||playing)return;
        x=Math.max(-29999900,Math.min(29999900,x+dx));
        y=Math.max(-200,Math.min(1500,y+dy));
        z=Math.max(-29999900,Math.min(29999900,z+dz));
        sync();
    }
    public static void moveLocal(double forward,double strafe,double vertical){
        if(!active||playing)return;
        double r=Math.toRadians(yaw);
        move(-Math.sin(r)*forward+Math.cos(r)*strafe,vertical,Math.cos(r)*forward+Math.sin(r)*strafe);
    }
    public static void rotate(float dyaw,float dpitch){
        if(!active||playing)return;
        yaw+=dyaw;
        pitch=Math.max(-89,Math.min(89,pitch+dpitch));
        sync();
    }

    public static boolean addFrame(int seconds){
        if(!active||playing||FRAMES.size()>=128)return false;
        FRAMES.add(new Keyframe(x,y,z,yaw,pitch,Math.max(5,Math.min(12000,seconds*20))));
        save();return true;
    }
    public static boolean removeLast(){
        if(FRAMES.isEmpty())return false;
        FRAMES.remove(FRAMES.size()-1);save();return true;
    }
    public static void clear(){FRAMES.clear();save();}

    public static boolean play(){
        if(FRAMES.size()<2)return false;
        if(!start())return false;
        Minecraft mc=Minecraft.getInstance();
        oldHideGui=mc.options.hideGui;
        mc.options.hideGui=true;
        frameIndex=0;frameTick=0;lastTimeline=0;playing=true;
        setFrame(FRAMES.get(0));
        return true;
    }
    public static void haltPlayback(){
        if(!playing)return;
        playing=false;Minecraft.getInstance().options.hideGui=oldHideGui;
    }
    public static void seek(double progress){
        if(!active||FRAMES.size()<2||playing)return;
        double bounded=Math.max(0,Math.min(1,progress));
        int parts=FRAMES.size()-1;
        int index=Math.min(parts-1,(int)Math.floor(bounded*parts));
        double t=Math.min(1,bounded*parts-index);
        interpolate(FRAMES.get(index),FRAMES.get(index+1),t);
        lastTimeline=bounded;
    }
    private static void setFrame(Keyframe frame){
        x=frame.x();y=frame.y();z=frame.z();yaw=frame.yaw();pitch=frame.pitch();sync();
    }
    private static void interpolate(Keyframe a,Keyframe b,double t){
        t=Math.max(0,Math.min(1,t));double smooth=t*t*(3-2*t);
        x=a.x()+(b.x()-a.x())*smooth;
        y=a.y()+(b.y()-a.y())*smooth;
        z=a.z()+(b.z()-a.z())*smooth;
        float delta=(float)(((b.yaw()-a.yaw()+540)%360)-180);
        yaw=a.yaw()+delta*(float)smooth;
        pitch=a.pitch()+(b.pitch()-a.pitch())*(float)smooth;
        sync();
    }

    public static void tick(){
        Minecraft mc=Minecraft.getInstance();
        if(!active)return;
        if(mc.player==null||mc.level==null||mc.level!=cameraLevel||cameraEntity==null
          ||mc.getCameraEntity()!=cameraEntity||!mc.player.isAlive()){
            stop();return;
        }
        if(!playing)return;
        if(frameIndex>=FRAMES.size()-1){stop();return;}
        Keyframe a=FRAMES.get(frameIndex),b=FRAMES.get(frameIndex+1);
        int duration=Math.max(1,a.durationTicks());
        frameTick++;
        interpolate(a,b,(double)frameTick/duration);
        lastTimeline=((double)frameIndex+(double)frameTick/duration)/(FRAMES.size()-1);
        if(frameTick>=duration){
            frameTick=0;frameIndex++;
            if(frameIndex>=FRAMES.size()-1)stop();
        }
    }
    private CinematicState(){}
}
