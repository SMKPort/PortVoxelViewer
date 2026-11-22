package com.port.nexusclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** A strictly client-side, decorative 3D particle projection. Never modifies blocks. */
final class NexusHologram {
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-hologram.properties");
    private static boolean loaded,enabled;
    private static String model="orb";
    private static double scale=1.0,distance=2.4;
    private static int ticks;
    private NexusHologram(){}

    private static void load(){
        if(loaded)return;
        loaded=true;
        if(!Files.isRegularFile(PATH))return;
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(PATH)){
            p.load(in);
            enabled=Boolean.parseBoolean(p.getProperty("enabled","false"));
            String saved=p.getProperty("model","orb");
            if(validModel(saved))model=saved;
            scale=clamp(Double.parseDouble(p.getProperty("scale","1")),0.5,2.0,1);
            distance=clamp(Double.parseDouble(p.getProperty("distance","2.4")),1.5,5.0,2.4);
        }catch(Exception ignored){
            enabled=false;model="orb";scale=1;distance=2.4;
        }
    }
    private static double clamp(double n,double min,double max,double fallback){
        return Double.isFinite(n)?Math.max(min,Math.min(max,n)):fallback;
    }
    private static boolean validModel(String n){
        return "orb".equals(n)||"runes".equals(n)||"schematic".equals(n);
    }
    private static void save(){
        Properties p=new Properties();
        p.setProperty("enabled",Boolean.toString(enabled));
        p.setProperty("model",model);
        p.setProperty("scale",Double.toString(scale));
        p.setProperty("distance",Double.toString(distance));
        try{
            Files.createDirectories(PATH.getParent());
            try(OutputStream out=Files.newOutputStream(PATH)){
                p.store(out,"Nexus hologram: local-only particle visualization");
            }
        }catch(Exception ex){System.err.println("Nexus hologram: falha ao salvar preferências.");}
    }
    static boolean enabled(){load();return enabled;}
    static String model(){load();return model;}
    static double scale(){load();return scale;}
    static double distance(){load();return distance;}
    static void setEnabled(boolean value){load();enabled=value;save();}
    static void setModel(String name){
        load();
        if(!validModel(name))throw new IllegalArgumentException("Modelo: orb, runes ou schematic.");
        model=name;save();
    }
    static void nextModel(){
        setModel(switch(model()){
            case "orb" -> "runes";
            case "runes" -> "schematic";
            default -> "orb";
        });
    }
    static void adjustScale(double amount){load();scale=clamp(scale+amount,0.5,2,1);save();}
    static void adjustDistance(double amount){load();distance=clamp(distance+amount,1.5,5,2.4);save();}
    static String status(){
        load();return "Holograma "+(enabled?"ON":"OFF")+" | "+model+
            " | tamanho "+String.format(java.util.Locale.ROOT,"%.1f",scale)+
            " | distância "+String.format(java.util.Locale.ROOT,"%.1f",distance);
    }
    static void tick(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(!enabled||mc.player==null||mc.level==null)return;
        if(++ticks%5!=0)return;
        Vec3 look=mc.player.getLookAngle();
        Vec3 forward=new Vec3(look.x,0,look.z);
        if(forward.lengthSqr()<.001)forward=new Vec3(0,0,1);
        forward=forward.normalize();
        Vec3 right=new Vec3(forward.z,0,-forward.x);
        Vec3 center=mc.player.position().add(forward.scale(distance)).add(0,1.45,0);
        double time=ticks*0.035;
        if("orb".equals(model)){
            for(int i=0;i<22;i++){
                double phi=i*2.399963229728653, y=1.0-2.0*(i+.5)/22;
                double ring=Math.sqrt(Math.max(0,1-y*y)),ang=phi+time;
                dot(mc,center,right,forward,
                    ring*Math.cos(ang)*scale*.7,y*scale*.7,ring*Math.sin(ang)*scale*.7,i%3==0);
            }
        }else if("runes".equals(model)){
            for(int i=0;i<24;i++){
                double angle=i*Math.PI/12+time;
                double radius=(i%4==0?.95:.7)*scale;
                dot(mc,center,right,forward,
                    Math.cos(angle)*radius,Math.sin(angle)*radius,
                    Math.sin(time+i*.4)*.12*scale,i%4==0);
            }
        }else{
            // Four edges per layer and four upright edges: a rotating wireframe cube.
            double half=.65*scale;
            for(int i=0;i<24;i++){
                int edge=i%12;
                double u=((i/12)*2-1)*half;
                double x,y,z;
                if(edge<4){
                    x=edge<2?u:(edge==2?-half:half);
                    y=edge<2?(edge==0?-half:half):u;
                    z=-half;
                }else if(edge<8){
                    x=edge<6?u:(edge==6?-half:half);
                    y=edge<6?(edge==4?-half:half):u;
                    z=half;
                }else{
                    x=(edge%2==0?-half:half);y=u;z=edge<10?-half:half;
                }
                double c=Math.cos(time*.35),s=Math.sin(time*.35);
                dot(mc,center,right,forward,x*c-z*s,y,x*s+z*c,i%3==0);
            }
        }
    }
    private static void dot(Minecraft mc,Vec3 center,Vec3 right,Vec3 forward,
                            double x,double y,double z,boolean bright){
        Vec3 p=center.add(right.scale(x)).add(0,y,0).add(forward.scale(z));
        mc.level.addParticle(bright?ParticleTypes.END_ROD:ParticleTypes.ENCHANT,
            p.x,p.y,p.z,0,0,0);
    }
}
