package com.portmods.phantomstudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.mojang.math.Axis;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Purely local, line-rendered humanoid mannequins with scene keyframes. */
@Mod(value=PhantomStudio.ID, dist=Dist.CLIENT)
public final class PhantomStudio {
    public static final String ID = "phantomstudio";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final KeyMapping OPEN = new KeyMapping("key.phantomstudio.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_K,"key.categories.phantomstudio");
    private Library library = new Library();
    private int selectedScene, selectedActor, timeTicks, ticks;
    private boolean playing, visible = true;
    private String loadedWorld = "";
    private static final List<String> POSES = List.of("EM_PE","SENTADO","AJOELHADO","CONJURANDO","COMBATE","DEITADO","CORRENDO");
    private static final List<String> EQUIPMENT = List.of("NENHUM","ESPADA","CAJADO","ESCUDO","LIVRO");
    private static final List<String> EFFECTS = List.of("NENHUM","OURO","VERMELHO","AZUL","FUMACA","AURA","BRILHO");

    public PhantomStudio(IEventBus bus) { bus.addListener(this::registerKeys); NeoForge.EVENT_BUS.register(this); }
    private void registerKeys(RegisterKeyMappingsEvent e){e.register(OPEN);}
    private Scene scene() {
        if (library.scenes.isEmpty()) library.scenes.add(new Scene("Cena 1"));
        selectedScene=Math.floorMod(selectedScene,library.scenes.size());
        return library.scenes.get(selectedScene);
    }
    private Actor actor(){Scene s=scene();if(s.actors.isEmpty())return null;selectedActor=Math.floorMod(selectedActor,s.actors.size());return s.actors.get(selectedActor);}
    private String worldId(Minecraft mc){String server=mc.getCurrentServer()!=null?mc.getCurrentServer().ip:"singleplayer";
        return (server+"_"+mc.level.dimension().location()).replaceAll("[^a-zA-Z0-9._-]","_");}
    private Path path(){return Minecraft.getInstance().gameDirectory.toPath().resolve("phantomstudio/scenes/"+loadedWorld+".json");}
    private void load(){library=new Library();selectedScene=selectedActor=0;timeTicks=0;playing=false;
        try{if(Files.exists(path())){Library value=JSON.fromJson(Files.readString(path()),Library.class);if(value!=null&&value.scenes!=null)library=value;}}
        catch(Exception ex){System.err.println("[PhantomStudio] Cannot load scenes: "+ex);}scene();}
    private void save(){if(loadedWorld.isEmpty())return;try{Files.createDirectories(path().getParent());Files.writeString(path(),JSON.toJson(library), StandardCharsets.UTF_8);}
        catch(Exception ex){System.err.println("[PhantomStudio] Cannot save: "+ex);}}

    @SubscribeEvent public void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){if(!loadedWorld.isEmpty())save();loadedWorld="";return;}
        String world=worldId(mc);if(!loadedWorld.equals(world)){save();loadedWorld=world;load();}
        while(OPEN.consumeClick())mc.setScreen(new EditorScreen());
        ticks++;
        if(playing){timeTicks++;if(timeTicks>scene().duration){timeTicks=0;playing=false;}}
        if(ticks%6==0)for(Actor actor:scene().actors){
            Frame f=sample(actor,timeTicks);
            if(f==null)continue;
            double yy=f.y+1+Math.sin(ticks*.12)*.15;
            switch(actor.effect){
                case "OURO"->mc.level.addParticle(ParticleTypes.END_ROD,f.x+.4*Math.sin(ticks),yy,f.z+.4*Math.cos(ticks),0,.025,0);
                case "VERMELHO"->mc.level.addParticle(ParticleTypes.FLAME,f.x+.25*Math.sin(ticks),yy,f.z+.25*Math.cos(ticks),0,.015,0);
                case "AZUL"->mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,f.x+.3*Math.sin(ticks),yy,f.z+.3*Math.cos(ticks),0,.01,0);
                case "FUMACA"->mc.level.addParticle(ParticleTypes.SMOKE,f.x,yy,f.z,0,.02,0);
                case "AURA"->mc.level.addParticle(ParticleTypes.ENCHANT,f.x+.6*Math.sin(ticks*.4),yy-.7,f.z+.6*Math.cos(ticks*.4),0,.02,0);
                case "BRILHO"->mc.level.addParticle(ParticleTypes.GLOW,f.x+.3*Math.sin(ticks*.2),yy,f.z+.3*Math.cos(ticks*.2),0,.01,0);
                default->{}
            }
        }
        if(ticks%250==0)save();
    }

    private Frame sample(Actor a,int t){
        if(!playing || a.frames==null || a.frames.isEmpty())return new Frame(t,a.x,a.y,a.z,a.yaw,a.pose);
        List<Frame> frames=new ArrayList<>(a.frames);
        frames.sort(Comparator.comparingInt(f->f.tick));
        Frame previous=frames.get(0),next=previous;
        for(Frame frame:frames){if(frame.tick<=t)previous=frame;if(frame.tick>=t){next=frame;break;}next=frame;}
        if(t>=next.tick || next.tick<=previous.tick)return previous;
        double alpha=(t-previous.tick)/(double)(next.tick-previous.tick);
        return new Frame(t,lerp(previous.x,next.x,alpha),lerp(previous.y,next.y,alpha),lerp(previous.z,next.z,alpha),
                (float)lerp(previous.yaw,next.yaw,alpha),previous.pose);
    }
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}

    @SubscribeEvent public void renderLevel(RenderLevelStageEvent e){
        if(!visible || e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || e.getPoseStack()==null)return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        PoseStack ps=e.getPoseStack();Vec3 camera=e.getCamera().getPosition();
        VertexConsumer vb=mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        for(Actor a:scene().actors){
            Frame f=sample(a,timeTicks);if(f==null)continue;
            if(mc.player.position().distanceToSqr(f.x,f.y,f.z)>120*120)continue;
            int rgb=switch(a.effect){case "OURO"->0xFFD56B;case "VERMELHO"->0xF05470;case "AZUL"->0x54D9FF;case "BRILHO"->0xF2F5FF;case "AURA"->0xB278FF;default->0x80F7EE;};
            float r=((rgb>>16)&255)/255f,g=((rgb>>8)&255)/255f,b=(rgb&255)/255f;
            ps.pushPose();ps.translate(f.x-camera.x,f.y-camera.y,f.z-camera.z);
            ps.mulPose(Axis.YP.rotationDegrees(-f.yaw));
            drawGhost(ps,vb,f,a,r,g,b);
            ps.popPose();
        }
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }
    private static void box(PoseStack ps,VertexConsumer out,double x0,double y0,double z0,double x1,double y1,double z1,float r,float g,float b){
        LevelRenderer.renderLineBox(ps,out,new AABB(x0,y0,z0,x1,y1,z1),r,g,b,.72f);
    }
    private void drawGhost(PoseStack ps,VertexConsumer out,Frame f,Actor a,float r,float g,float b){
        double lift=f.pose.equals("DEITADO")?-.7:0;
        if(f.pose.equals("DEITADO")){ps.translate(0,.4,0);ps.mulPose(Axis.XP.rotationDegrees(90));}
        else if(f.pose.equals("SENTADO"))ps.translate(0,-.45,0);
        else if(f.pose.equals("AJOELHADO"))ps.translate(0,-.30,0);
        box(ps,out,-.28,.85+lift,-.16,.28,1.43+lift,.16,r,g,b);  // chest
        box(ps,out,-.23,1.45+lift,-.23,.23,1.93+lift,.23,r,g,b); // head
        boolean run=f.pose.equals("CORRENDO")||f.pose.equals("COMBATE");
        boolean magic=f.pose.equals("CONJURANDO");
        for(int side=-1;side<=1;side+=2){
            ps.pushPose();ps.translate(side*.37,1.35+lift,0);
            ps.mulPose(Axis.XP.rotationDegrees(magic?-95:run?(float)(Math.sin((timeTicks+side*8)*.27)*40):0));
            box(ps,out,-.10,-.57,-.12,.10,0,.12,r,g,b);ps.popPose();
            ps.pushPose();ps.translate(side*.15,.85+lift,0);
            ps.mulPose(Axis.XP.rotationDegrees(run?(float)(Math.sin((timeTicks+side*8)*.27)*34):f.pose.equals("SENTADO")?70:0));
            box(ps,out,-.12,-.82,-.12,.12,0,.12,r,g,b);ps.popPose();
        }
        switch(a.equipment){
            case "ESPADA"->{box(ps,out,.43,.10,-.06,.49,1.25,.06,r,g,b);box(ps,out,.23,.40,-.08,.7,.45,.08,r,g,b);}
            case "CAJADO"->box(ps,out,.47,-.02,-.04,.52,2.25,.04,r,g,b);
            case "ESCUDO"->box(ps,out,-.83,.48,-.15,-.51,1.35,.16,r,g,b);
            case "LIVRO"->box(ps,out,.40,1.03,-.3,.85,1.11,.12,r,g,b);
            default->{}
        }
    }

    private final class EditorScreen extends Screen {
        private EditBox actorName,sceneName;
        EditorScreen(){super(Component.literal("Phantom Studio"));}
        @Override protected void init(){
            int x=12,y=44;
            sceneName=new EditBox(font,x,y,122,20,Component.literal("Nome da cena"));sceneName.setHint(Component.literal("Nome da cena"));addRenderableWidget(sceneName);
            addRenderableWidget(Button.builder(Component.literal("Nova cena"),b->{library.scenes.add(new Scene(sceneName.getValue().isBlank()?"Cena "+(library.scenes.size()+1):sceneName.getValue()));selectedScene=library.scenes.size()-1;selectedActor=0;save();}).bounds(x+128,y,92,20).build());
            addRenderableWidget(Button.builder(Component.literal("Proxima cena"),b->{selectedScene++;selectedActor=0;timeTicks=0;save();}).bounds(x+224,y,105,20).build());
            y+=28;
            actorName=new EditBox(font,x,y,122,20,Component.literal("Nome do fantasma"));actorName.setHint(Component.literal("Nome do fantasma"));addRenderableWidget(actorName);
            addRenderableWidget(Button.builder(Component.literal("+ Fantasma"),b->{var player=Minecraft.getInstance().player;if(player==null)return;
                Scene s=scene();s.actors.add(new Actor(actorName.getValue().isBlank()?"Fantasma "+(s.actors.size()+1):actorName.getValue(),player.getX(),player.getY(),player.getZ(),player.getYRot()));
                selectedActor=s.actors.size()-1;save();}).bounds(x+128,y,92,20).build());
            addRenderableWidget(Button.builder(Component.literal("Proximo"),b->selectedActor++).bounds(x+224,y,105,20).build());
            y+=28;
            addRenderableWidget(Button.builder(Component.literal("Pose"),b->cycle("pose")).bounds(x,y,100,20).build());
            addRenderableWidget(Button.builder(Component.literal("Equipamento"),b->cycle("equipment")).bounds(x+106,y,112,20).build());
            addRenderableWidget(Button.builder(Component.literal("Efeito"),b->cycle("effect")).bounds(x+224,y,105,20).build());
            y+=28;
            addRenderableWidget(Button.builder(Component.literal("+ Keyframe"),b->{Actor a=actor();if(a==null)return;a.frames.removeIf(f->f.tick==timeTicks);
                a.frames.add(new Frame(timeTicks,a.x,a.y,a.z,a.yaw,a.pose));save();}).bounds(x,y,110,20).build());
            addRenderableWidget(Button.builder(Component.literal("Play/Pause"),b->playing=!playing).bounds(x+116,y,102,20).build());
            addRenderableWidget(Button.builder(Component.literal("Reset"),b->{timeTicks=0;playing=false;}).bounds(x+224,y,105,20).build());
            y+=28;
            addRenderableWidget(Button.builder(Component.literal("- 1s"),b->timeTicks=Math.max(0,timeTicks-20)).bounds(x,y,75,20).build());
            addRenderableWidget(Button.builder(Component.literal("+ 1s"),b->timeTicks=Math.min(scene().duration,timeTicks+20)).bounds(x+81,y,75,20).build());
            addRenderableWidget(Button.builder(Component.literal("- fantasma"),b->{Actor a=actor();if(a!=null){scene().actors.remove(a);selectedActor=0;save();}}).bounds(x+162,y,105,20).build());
            addRenderableWidget(Button.builder(Component.literal("Ver/ocultar"),b->visible=!visible).bounds(x+273,y,96,20).build());
            y+=28;
            addRenderableWidget(Button.builder(Component.literal("Posicionar em mim"),b->{Actor a=actor();var p=Minecraft.getInstance().player;if(a!=null&&p!=null){a.x=p.getX();a.y=p.getY();a.z=p.getZ();save();}}).bounds(x,y,165,20).build());
            addRenderableWidget(Button.builder(Component.literal("Salvar cena"),b->save()).bounds(x+171,y,120,20).build());
        }
        private void cycle(String what){Actor a=actor();if(a==null)return;
            switch(what){case "pose"->a.pose=next(POSES,a.pose);case "equipment"->a.equipment=next(EQUIPMENT,a.equipment);case "effect"->a.effect=next(EFFECTS,a.effect);default->{}}
            save();}
        private String next(List<String> list,String previous){return list.get((list.indexOf(previous)+1)%list.size());}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xEE071018);
            g.drawCenteredString(font,"PHANTOM STUDIO | hologramas locais",width/2,13,0x80F7EE);
            Scene s=scene();Actor a=actor();int yy=205;
            g.drawString(font,"Cena: "+s.name+" | Fantasmas: "+s.actors.size()+" | Tempo: "+(timeTicks/20)+"s / "+(s.duration/20)+"s",12,yy,0xFFFFFF);yy+=18;
            if(a!=null){g.drawString(font,"Selecionado: "+a.name+" | Pose: "+a.pose+" | Efeito: "+a.effect,12,yy,0xFFD9A7);yy+=16;
                g.drawString(font,"Equipamento: "+a.equipment+" | Frames: "+a.frames.size(),12,yy,0xEAEAEA);yy+=16;
                g.drawString(font,String.format(Locale.ROOT,"Pos: %.1f / %.1f / %.1f | Rot: %.0f",a.x,a.y,a.z,a.yaw),12,yy,0xC9C9C9);yy+=16;}
            g.drawString(font,"W/S: Z | A/D: X | Q/E: altura | R/T: rotacao | Setas: timeline",12,yy+4,0xAFC1C6);
            g.drawString(font,"Os fantasmas sao locais e tem visual holografico de linhas.",12,yy+20,0xAFC1C6);
            super.render(g,mx,my,partial);
        }
        @Override public boolean keyPressed(int key,int scan,int mods){
            if(actorName.isFocused()||sceneName.isFocused())return super.keyPressed(key,scan,mods);
            Actor a=actor();if(a==null)return super.keyPressed(key,scan,mods);
            boolean move=true;
            switch(key){case GLFW.GLFW_KEY_W->a.z-=.25;case GLFW.GLFW_KEY_S->a.z+=.25;case GLFW.GLFW_KEY_A->a.x-=.25;case GLFW.GLFW_KEY_D->a.x+=.25;
                case GLFW.GLFW_KEY_Q->a.y+=.25;case GLFW.GLFW_KEY_E->a.y-=.25;
                case GLFW.GLFW_KEY_R->a.yaw+=10;case GLFW.GLFW_KEY_T->a.yaw-=10;
                case GLFW.GLFW_KEY_LEFT->timeTicks=Math.max(0,timeTicks-5);
                case GLFW.GLFW_KEY_RIGHT->timeTicks=Math.min(scene().duration,timeTicks+5);
                default->move=false;
            }
            if(move){save();return true;}return super.keyPressed(key,scan,mods);
        }
        @Override public void onClose(){save();super.onClose();}
        @Override public boolean isPauseScreen(){return false;}
    }

    public static final class Library { List<Scene> scenes=new ArrayList<>(); }
    public static final class Scene {String name;List<Actor> actors=new ArrayList<>();int duration=20*60;
        Scene(String name){this.name=name;}}
    public static final class Actor {
        String name,pose="EM_PE",equipment="NENHUM",effect="AZUL";
        double x,y,z;float yaw;List<Frame> frames=new ArrayList<>();
        Actor(String n,double x,double y,double z,float yaw){name=n;this.x=x;this.y=y;this.z=z;this.yaw=yaw;
            frames.add(new Frame(0,x,y,z,yaw,pose));}
    }
    public static final class Frame {int tick;double x,y,z;float yaw;String pose;
        Frame(int t,double x,double y,double z,float yaw,String pose){tick=t;this.x=x;this.y=y;this.z=z;this.yaw=yaw;this.pose=pose;}}
}
