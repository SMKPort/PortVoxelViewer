package com.portmods.voxelphysics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** A configurable client-side *visual overlay* cloth/appendage simulator. */
@Mod(value=VoxelPhysics.ID,dist=Dist.CLIENT)
public final class VoxelPhysics {
    public static final String ID="voxelphysics";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation FABRIC=ResourceLocation.fromNamespaceAndPath(ID,"textures/misc/fabric.png");
    private static final KeyMapping OPEN=new KeyMapping("key.voxelphysics.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.voxelphysics");
    private static final List<String> PARTS=List.of("CAPE","HAIR","SCARF","SASH","SLEEVES","CHAINS","TASSELS","WINGS");
    private static final String[] PRESETS={"CLASSICO","ANGELICAL","NINJA","ARCADIO"};
    private Config config=new Config();
    private final Map<UUID,Body> bodies=new HashMap<>();
    private int ticks;

    public VoxelPhysics(IEventBus bus){bus.addListener(this::registerKeys);NeoForge.EVENT_BUS.register(this);load();}
    private void registerKeys(RegisterKeyMappingsEvent e){e.register(OPEN);}
    private Path file(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/voxelphysics.json");}
    private void load(){try{if(Files.exists(file())){Config c=JSON.fromJson(Files.readString(file()),Config.class);if(c!=null)config=c;}}
        catch(Exception e){System.err.println("[VoxelPhysics] Load: "+e);}config.ensure();}
    private void save(){try{Files.createDirectories(file().getParent());Files.writeString(file(),JSON.toJson(config),StandardCharsets.UTF_8);}
        catch(Exception e){System.err.println("[VoxelPhysics] Save: "+e);}}

    @SubscribeEvent public void tick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();while(OPEN.consumeClick())mc.setScreen(new ConfigScreen());
        if(mc.level==null||mc.player==null){bodies.clear();return;}
        if(!config.enabled)return;
        ticks++;
        Collection<? extends Player> players=config.otherPlayers?mc.level.players():List.of(mc.player);
        Set<UUID> present=new HashSet<>();
        for(Player p:players){
            present.add(p.getUUID());Body state=bodies.computeIfAbsent(p.getUUID(),k->new Body());
            state.tick(p,config,ticks);
        }
        bodies.keySet().retainAll(present);
    }
    @SubscribeEvent public void render(RenderLevelStageEvent e){
        if(!config.enabled||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||e.getPoseStack()==null)return;
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var buffer=mc.renderBuffers().bufferSource();VertexConsumer vb=buffer.getBuffer(RenderType.entityTranslucent(FABRIC));
        Vec3 camera=e.getCamera().getPosition();PoseStack ps=e.getPoseStack();
        Collection<? extends Player> players=config.otherPlayers?mc.level.players():List.of(mc.player);
        for(Player p:players){
            if(p.isInvisible()||p.position().distanceToSqr(mc.player.position())>48*48)continue;
            Body b=bodies.get(p.getUUID());if(b==null)continue;
            ps.pushPose();ps.translate(p.getX()-camera.x,p.getY()-camera.y,p.getZ()-camera.z);
            ps.mulPose(Axis.YP.rotationDegrees(-p.yBodyRot));
            if(p.isCrouching())ps.translate(0,-.18,0);
            for(String part:PARTS){Part cfg=config.parts.get(part);if(cfg==null||!cfg.enabled)continue;
                renderPart(ps,vb,b,part,cfg);
            }
            ps.popPose();
        }
        buffer.endBatch(RenderType.entityTranslucent(FABRIC));
    }

    /** Ribbon panels are voxel-styled strips: spring positions control their flexible rows. */
    private void renderPart(PoseStack ps,VertexConsumer out,Body body,String part,Part cfg){
        switch(part){
            case "CAPE"->ribbon(ps,out,body,part,-.0,1.47,.27,.72,1.07,8,cfg);
            case "HAIR"->{ribbon(ps,out,body,part+"0",-.19,1.81,.24,.14,.48,5,cfg);
                          ribbon(ps,out,body,part+"1",0,1.82,.25,.16,.55,5,cfg);
                          ribbon(ps,out,body,part+"2",.19,1.81,.24,.14,.48,5,cfg);}
            case "SCARF"->{ribbon(ps,out,body,part+"0",-.14,1.44,.20,.16,.58,5,cfg);
                           ribbon(ps,out,body,part+"1",.14,1.44,.20,.16,.58,5,cfg);}
            case "SASH"->ribbon(ps,out,body,part,-.13,.99,.24,.27,.84,7,cfg);
            case "SLEEVES"->{ribbon(ps,out,body,part+"0",-.37,1.25,.0,.22,.60,5,cfg);
                             ribbon(ps,out,body,part+"1",.37,1.25,.0,.22,.60,5,cfg);}
            case "CHAINS"->{ribbon(ps,out,body,part+"0",-.26,1.08,.26,.06,.78,9,cfg);
                            ribbon(ps,out,body,part+"1",.26,1.08,.26,.06,.78,9,cfg);}
            case "TASSELS"->{ribbon(ps,out,body,part+"0",-.22,.92,.27,.10,.6,7,cfg);
                             ribbon(ps,out,body,part+"1",.22,.92,.27,.10,.6,7,cfg);}
            case "WINGS"->{ribbon(ps,out,body,part+"0",-.49,1.48,.19,.45,.83,8,cfg);
                           ribbon(ps,out,body,part+"1",.49,1.48,.19,.45,.83,8,cfg);}
            default->{}
        }
    }
    private void ribbon(PoseStack ps,VertexConsumer v,Body b,String key,double cx,double y,double z,double width,double length,int rows,Part opt){
        Chain chain=b.chains.computeIfAbsent(key,k->new Chain(rows));
        if(chain.nodes.length!=rows){chain=new Chain(rows);b.chains.put(key,chain);}
        float[] rgb=color(opt.color);
        float alpha=Math.max(.20f,Math.min(1f,opt.opacity));
        double ww=width*opt.width;cx+=opt.dx;y+=opt.dy;z+=opt.dz;
        for(int j=0;j<rows-1;j++){
            double f0=j/(double)(rows-1),f1=(j+1)/(double)(rows-1);
            Node a=chain.nodes[j],d=chain.nodes[j+1];
            float ya=(float)(y-f0*length),yb=(float)(y-f1*length);
            float xa=(float)(cx+a.x),xb=(float)(cx+d.x);
            float za=(float)(z+a.z+f0*.04),zb=(float)(z+d.z+f1*.04);
            float half=(float)(ww*.5);
            quad(ps,v,xa-half,ya,za,xa+half,ya,za,xb+half,yb,zb,xb-half,yb,zb,rgb,alpha);
            quad(ps,v,xa+half,ya,za,xa-half,ya,za,xb-half,yb,zb,xb+half,yb,zb,rgb,alpha*.82f);
        }
    }
    private static float[] color(String raw){try{int v=Integer.parseInt(raw.replace("#",""),16);
        return new float[]{((v>>16)&255)/255f,((v>>8)&255)/255f,(v&255)/255f};}
        catch(Exception e){return new float[]{1,1,1};}}
    private void quad(PoseStack ps,VertexConsumer v,float x1,float y1,float z1,float x2,float y2,float z2,
                      float x3,float y3,float z3,float x4,float y4,float z4,float[] rgb,float alpha){
        int rr=(int)(rgb[0]*255),gg=(int)(rgb[1]*255),bb=(int)(rgb[2]*255),aa=(int)(alpha*255);
        var m=ps.last().pose();
        v.addVertex(m,x1,y1,z1).setColor(rr,gg,bb,aa).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,0,1);
        v.addVertex(m,x2,y2,z2).setColor(rr,gg,bb,aa).setUv(1,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,0,1);
        v.addVertex(m,x3,y3,z3).setColor(rr,gg,bb,aa).setUv(1,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,0,1);
        v.addVertex(m,x4,y4,z4).setColor(rr,gg,bb,aa).setUv(0,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,0,1);
    }
    private static final class Node {double x,z,dx,dz;}
    private static final class Chain {Node[] nodes;
        Chain(int n){nodes=new Node[n];for(int i=0;i<n;i++)nodes[i]=new Node();}}
    private static final class Body {
        final Map<String,Chain> chains=new HashMap<>();Vec3 prev=Vec3.ZERO;
        void tick(Player p,Config c,int ticks){
            Vec3 now=p.position(),delta=prev.equals(Vec3.ZERO)?Vec3.ZERO:now.subtract(prev);prev=now;
            // Clamp teleports. Convert world velocity into body-local motion.
            if(delta.lengthSqr()>4)delta=Vec3.ZERO;
            double yaw=Math.toRadians(p.yBodyRot),localSide=delta.x*Math.cos(yaw)+delta.z*Math.sin(yaw);
            double localFront=-delta.x*Math.sin(yaw)+delta.z*Math.cos(yaw);
            double wind=Math.sin(ticks*.019+c.windSeed)*c.wind + Math.sin(ticks*.061)*c.wind*.3;
            double speed=Math.min(1.0,delta.length());
            double motion=p.isSprinting()?1.5:p.isFallFlying()?2.0:p.isCrouching()?.65:1.0;
            for(Chain chain:chains.values()){
                for(int i=0;i<chain.nodes.length;i++){
                    Node n=chain.nodes[i];if(i==0){n.x=n.z=n.dx=n.dz=0;continue;}
                    double length=i/(double)(chain.nodes.length-1);
                    double xTarget=length*length*(wind*.19-localSide*.40)*motion;
                    double zTarget=length*length*(.07+Math.abs(localFront)*.9+speed*.6)*motion;
                    if(p.isFallFlying())zTarget+=length*.25;
                    n.dx=(n.dx+(xTarget-n.x)*c.spring)*c.damping;
                    n.dz=(n.dz+(zTarget-n.z)*c.spring)*c.damping;
                    n.x=Math.max(-.65,Math.min(.65,n.x+n.dx));
                    n.z=Math.max(-.25,Math.min(.9,n.z+n.dz));
                }
            }
        }
    }
    public static final class Part {
        boolean enabled;String color="#FFFFFF";float opacity=.85f;double width=1.0;double dx=0,dy=0,dz=0;
        Part(boolean enabled,String color,float opacity){this.enabled=enabled;this.color=color;this.opacity=opacity;}
    }
    public static final class Config {
        boolean enabled=true,otherPlayers=false;
        double wind=.40,spring=.14,damping=.75,windSeed=1.0;
        String preset="CLASSICO";
        Map<String,Part> parts=new LinkedHashMap<>();
        Config(){ensure();}
        void ensure(){if(parts==null)parts=new LinkedHashMap<>();for(String p:PARTS)parts.putIfAbsent(p,new Part(true,"#FFFFFF",.85f));}
    }
    private void preset(String name){config.preset=name;config.parts.clear();
        for(String p:PARTS){String col=switch(name){case "ANGELICAL"->p.equals("WINGS")?"#FFF0C6":"#FFD87A";
            case "NINJA"->p.equals("CHAINS")?"#AD243D":"#24232C";
            case "ARCADIO"->p.equals("WINGS")?"#E1E8F8":"#8B243A";
            default->p.equals("CHAINS")?"#C7C4BE":"#E7E7E7";};
            config.parts.put(p,new Part(true,col,.85f));}save();}
    private final class ConfigScreen extends Screen {
        ConfigScreen(){super(Component.literal("Voxel Physics"));}
        @Override protected void init(){
            int x=width/2-180,y=50;
            addRenderableWidget(Button.builder(Component.literal("ON/OFF"),b->{config.enabled=!config.enabled;save();}).bounds(x,y,105,20).build());
            addRenderableWidget(Button.builder(Component.literal("Outros jogadores"),b->{config.otherPlayers=!config.otherPlayers;save();}).bounds(x+111,y,150,20).build());
            addRenderableWidget(Button.builder(Component.literal("Preset"),b->{int idx=Arrays.asList(PRESETS).indexOf(config.preset);
                preset(PRESETS[(idx+1)%PRESETS.length]);}).bounds(x+267,y,90,20).build());
            y+=28;
            addRenderableWidget(Button.builder(Component.literal("Vento -"),b->{config.wind=Math.max(0,config.wind-.1);save();}).bounds(x,y,86,20).build());
            addRenderableWidget(Button.builder(Component.literal("Vento +"),b->{config.wind=Math.min(3,config.wind+.1);save();}).bounds(x+92,y,86,20).build());
            addRenderableWidget(Button.builder(Component.literal("Rigidez -"),b->{config.spring=Math.max(.03,config.spring-.02);save();}).bounds(x+184,y,86,20).build());
            addRenderableWidget(Button.builder(Component.literal("Rigidez +"),b->{config.spring=Math.min(.5,config.spring+.02);save();}).bounds(x+276,y,86,20).build());
            y+=28;
            for(int i=0;i<PARTS.size();i++){
                final String part=PARTS.get(i);int px=x+(i%2)*181,py=y+(i/2)*27;
                addRenderableWidget(Button.builder(Component.literal(part),b->{config.parts.get(part).enabled=!config.parts.get(part).enabled;save();}).bounds(px,py,120,20).build());
                addRenderableWidget(Button.builder(Component.literal("-"),b->{Part c=config.parts.get(part);c.width=Math.max(.4,c.width-.1);save();}).bounds(px+124,py,24,20).build());
                addRenderableWidget(Button.builder(Component.literal("+"),b->{Part c=config.parts.get(part);c.width=Math.min(2,c.width+.1);save();}).bounds(px+151,py,24,20).build());
            }
            y+=4*27+8;
            addRenderableWidget(Button.builder(Component.literal("Amortecimento -"),b->{config.damping=Math.max(.3,config.damping-.03);save();}).bounds(x,y,170,20).build());
            addRenderableWidget(Button.builder(Component.literal("Amortecimento +"),b->{config.damping=Math.min(.98,config.damping+.03);save();}).bounds(x+175,y,170,20).build());
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xED101016);
            g.drawCenteredString(font,"VOXEL PHYSICS | fisica de acessorios client-side",width/2,14,0xF5DA90);
            g.drawCenteredString(font,String.format(Locale.ROOT,"Ativo: %s | Outros: %s | Preset: %s | Vento: %.1f | Rigidez: %.2f | Amort.: %.2f",
                    config.enabled,config.otherPlayers,config.preset,config.wind,config.spring,config.damping),width/2,32,0xFFFFFF);
            int x=width/2-180,y=126;
            for(int i=0;i<PARTS.size();i++){String part=PARTS.get(i);Part c=config.parts.get(part);
                g.drawString(font,(c.enabled?"[ON] ":"[OFF] ")+String.format(Locale.ROOT,"larg. %.1f",c.width),x+(i%2)*181,y+(i/2)*27,0xDBD1CB);}
            g.drawCenteredString(font,"Edite cores/opacidade/offsets no JSON: config/voxelphysics.json",width/2,height-35,0xC7BAC2);
            g.drawCenteredString(font,"Visual sobreposto, sem deformar modelos de outros mods.",width/2,height-20,0x9A9A9A);
            super.render(g,mx,my,partial);
        }
        @Override public void onClose(){save();super.onClose();}
        @Override public boolean isPauseScreen(){return false;}
    }
}
