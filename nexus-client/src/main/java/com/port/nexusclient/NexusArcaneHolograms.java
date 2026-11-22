package com.port.nexusclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Decorative 3D world-space projections, generated only on this client.
 * No server packets, custom blocks, shaders, modded server or network calls.
 */
final class NexusArcaneHolograms {
    private static boolean enabled=false;
    private static String mode="orb";
    private static long tick=0L;
    private static int pulse=0;
    private NexusArcaneHolograms(){}

    static boolean enabled(){return enabled;}
    static String mode(){return mode;}
    static void setEnabled(boolean value){
        enabled=value;
        if(!value)pulse=0;
    }
    static void setMode(String selected){
        if(!"orb".equals(selected)&&!"runes".equals(selected)&&!"schematic".equals(selected))
            throw new IllegalArgumentException("Escolha orb, runes ou schematic.");
        mode=selected;
        enabled=true;
    }
    static void pulse(){pulse=45;}
    static String status(){
        return "Hologramas "+(enabled?"ATIVOS":"OFF")+
            " | projeção "+mode+" | local ao cliente (sem custo).";
    }
    static void tick(){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled||mc.player==null||mc.level==null||mc.isPaused())return;
        if(++tick%3!=0)return;
        // A portable arcane focal point, always in front of the player's view.
        Vec3 eye=mc.player.getEyePosition();
        Vec3 look=mc.player.getLookAngle();
        Vec3 center=eye.add(look.scale(2.8));
        double x=center.x,y=center.y,z=center.z;
        double phase=tick*0.035;
        if("orb".equals(mode)){
            for(int i=0;i<18;i++){
                double a=phase+i*Math.PI/9;
                double height=Math.sin(a*1.7)*0.43;
                double radius=0.40*Math.sqrt(Math.max(0.10,1-(height*height)));
                project(mc,x+Math.cos(a)*radius,y+height,z+Math.sin(a)*radius,i%4==0);
            }
            for(int i=0;i<10;i++){
                double a=i*Math.PI/5-phase*0.7;
                project(mc,x+Math.cos(a)*0.64,y+Math.sin(a)*0.08,
                    z+Math.sin(a)*0.64,i%3==0);
            }
        }else if("runes".equals(mode)){
            for(int i=0;i<28;i++){
                double a=phase+i*Math.PI/14;
                double r=i%7==0?0.85:0.71;
                project(mc,x+Math.cos(a)*r,y+Math.sin(a)*r,z,i%5==0);
            }
            // Two crossing sigils with a deliberate 3D offset.
            for(int i=0;i<12;i++){
                double a=i*Math.PI/6-phase/2;
                project(mc,x+Math.cos(a)*0.45,y+Math.sin(a*2)*0.3,
                    z+Math.sin(a)*0.45,true);
            }
        }else{
            double size=0.45;
            for(int edge=0;edge<12;edge++){
                int axis=edge/4;
                int mask=edge%4;
                for(int i=0;i<=4;i++){
                    double t=(i/4.0)*2*size-size;
                    double u=((mask&1)==0?-size:size);
                    double v=((mask&2)==0?-size:size);
                    double dx=axis==0?t:axis==1?u:v;
                    double dy=axis==0?u:axis==1?t:u;
                    double dz=axis==0?v:axis==1?v:t;
                    // Deterministic wireframe points of a floating 3D cube.
                    project(mc,x+dx,y+dy,z+dz,(edge+i)%8==0);
                }
            }
        }
        if(pulse>0){
            pulse-=3;
            for(int i=0;i<8;i++){
                double a=(i*Math.PI/4)+phase*2;
                double r=0.3+(45-pulse)*0.013;
                project(mc,x+Math.cos(a)*r,y+Math.sin(a*2)*0.18,
                    z+Math.sin(a)*r,true);
            }
        }
    }
    private static void project(Minecraft mc,double x,double y,double z,boolean bright){
        if(bright)mc.level.addParticle(ParticleTypes.END_ROD,x,y,z,0,0,0);
        else mc.level.addParticle(ParticleTypes.ENCHANT,x,y,z,0,0,0);
    }
}
