package com.portmods.deepscan3d;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** DeepScan 3D: observations from rays received by the vanilla client only. */
@Mod(value = DeepScan3D.ID, dist = Dist.CLIENT)
public final class DeepScan3D {
    public static final String ID = "deepscan3d";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_VOXELS = 100_000;
    private static final int MAX_TRAIL = 15_000;
    private static final KeyMapping OPEN = new KeyMapping("key.deepscan3d.open", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.deepscan3d");
    private Store store = new Store();
    private final Map<Long, Voxel> index = new HashMap<>();
    private String currentWorld = "";
    private int ticks;
    private int backIndex = -1;
    private boolean replay;
    private int replayIndex;

    public DeepScan3D(IEventBus modBus) {
        modBus.addListener(this::keys);
        NeoForge.EVENT_BUS.register(this);
    }

    private void keys(RegisterKeyMappingsEvent e) { e.register(OPEN); }

    @SubscribeEvent public void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!currentWorld.isEmpty()) save();
            currentWorld = "";
            return;
        }
        String id = worldId(mc);
        if (!id.equals(currentWorld)) { save(); currentWorld = id; load(); }
        while (OPEN.consumeClick()) mc.setScreen(new MapScreen());
        ticks++;
        if (ticks % 4 == 0) {
            BlockPos p = mc.player.blockPosition();
            if (store.trail.isEmpty() || distance(store.trail.get(store.trail.size()-1), p) > 0.7) {
                store.trail.add(new Spot(p.getX(), p.getY(), p.getZ()));
                if (store.trail.size() > MAX_TRAIL) store.trail.remove(0);
            }
        }
        if (ticks % 12 == 0) scan(mc);
        if (ticks % 300 == 0) save();
        if (replay && ticks % 3 == 0) replayIndex = Math.min(store.trail.size()-1, replayIndex+1);
        if (backIndex >= 0 && backIndex < store.trail.size()) {
            Spot target = store.trail.get(backIndex);
            if (mc.player.position().distanceTo(new Vec3(target.x+0.5, target.y, target.z+0.5)) < 3.5) {
                backIndex -= 1;
            }
        }
    }

    private static double distance(Spot a, BlockPos p) {
        return Math.sqrt(Math.pow(a.x-p.getX(),2)+Math.pow(a.y-p.getY(),2)+Math.pow(a.z-p.getZ(),2));
    }

    private void scan(Minecraft mc) {
        Vec3 start = mc.player.getEyePosition();
        final int rayCount = 64;
        for (int i = 0; i < rayCount; i++) {
            double y = 1.0 - (2.0*i + 1.0)/rayCount;
            double a = i * 2.399963229728653;
            double t = Math.sqrt(Math.max(0, 1.0 - y*y));
            Vec3 d = new Vec3(Math.cos(a)*t,y,Math.sin(a)*t);
            Vec3 end = start.add(d.scale(23));
            BlockHitResult hit = mc.level.clip(new ClipContext(start,end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,mc.player));
            double range = hit.getType() == HitResult.Type.MISS ? 23 : Math.min(23,start.distanceTo(hit.getLocation()));
            for (double step = 0; step < range; step += 0.95) {
                BlockPos at = BlockPos.containing(start.add(d.scale(step)));
                if (!mc.level.hasChunkAt(at)) break;
                var state = mc.level.getBlockState(at);
                if (state.isAir()) record(at,"AIR", "minecraft:air");
                else if (!state.getFluidState().isEmpty()) record(at, state.getFluidState().isSource() ? "FLUID" : "FLOW", "fluid");
            }
            if (hit.getType() == HitResult.Type.BLOCK && mc.level.hasChunkAt(hit.getBlockPos())) {
                var at = hit.getBlockPos();
                String block = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(at).getBlock()).toString();
                boolean ore = block.contains("ore") || block.contains("ancient_debris");
                if (ore) record(at, "ORE", block);
                else if (!mc.level.getFluidState(at).isEmpty()) record(at,"FLUID",block);
                else record(at,"WALL",block);
            }
        }
    }

    private void record(BlockPos pos, String kind, String id) {
        long key = pos.asLong();
        Voxel before = index.get(key);
        if (before != null) {
            // Upgrade an open voxel to an observed surface when necessary.
            if (!"AIR".equals(before.kind) || "AIR".equals(kind)) return;
            before.kind = kind; before.block = id;
            return;
        }
        if (store.voxels.size() >= MAX_VOXELS) return;
        Voxel point = new Voxel(pos.getX(),pos.getY(),pos.getZ(),kind,id);
        store.voxels.add(point);
        index.put(key,point);
    }

    private static String worldId(Minecraft mc) {
        String host = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
        String dim = mc.level.dimension().location().toString();
        String raw = host + "_" + dim;
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_").substring(0, Math.min(100,raw.length()));
    }

    private Path file() { return Minecraft.getInstance().gameDirectory.toPath().resolve("deepscan3d/worlds/"+currentWorld+".json"); }
    private void load() {
        store = new Store(); index.clear(); ticks = 0; replay = false; backIndex = -1;
        try {
            Path f = file();
            if (Files.exists(f)) { Store read = JSON.fromJson(Files.readString(f), Store.class); if (read != null) store = read; }
            if (store.voxels == null) store.voxels = new ArrayList<>();
            if (store.trail == null) store.trail = new ArrayList<>();
            if (store.marks == null) store.marks = new ArrayList<>();
            if (store.routes == null) store.routes = new ArrayList<>();
            for (Voxel p : store.voxels) index.put(BlockPos.asLong(p.x,p.y,p.z),p);
        } catch (Exception ex) { System.err.println("[DeepScan] Load failed: "+ex); store = new Store(); }
    }
    private void save() {
        if (currentWorld.isEmpty()) return;
        try { Files.createDirectories(file().getParent()); Files.writeString(file(),JSON.toJson(store), StandardCharsets.UTF_8); }
        catch (IOException ex) { System.err.println("[DeepScan] Save failed: "+ex); }
    }
    private void mark(String name) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        BlockPos at = p.blockPosition();
        store.marks.add(new Mark(name.isBlank()?"Ponto " + (store.marks.size()+1):name,
                at.getX(),at.getY(),at.getZ())); save();
    }
    private void saveRoute() {
        List<Spot> path = new ArrayList<>(store.trail);
        store.routes.add(new Route("Rota "+(store.routes.size()+1),path));
        if (store.routes.size() > 12) store.routes.remove(0);
        save();
    }
    @SubscribeEvent public void drawHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || backIndex < 0 || backIndex >= store.trail.size()) return;
        Spot p = store.trail.get(backIndex);
        double dx = p.x + .5 - mc.player.getX(), dz = p.z+.5-mc.player.getZ();
        double dist = Math.sqrt(dx*dx+dz*dz);
        double bearing = Math.toDegrees(Math.atan2(-dx,dz));
        double delta = ((bearing - mc.player.getYRot() + 540)%360)-180;
        String arrow = Math.abs(delta)<22?"^":delta>0?">":"<";
        int center = e.getGuiGraphics().guiWidth()/2;
        e.getGuiGraphics().drawCenteredString(mc.font,"DeepScan | Voltar: "+arrow+" "+(int)dist+"m",center,16,0x55F4DD);
    }

    public static final class Voxel { int x,y,z; String kind,block;
        Voxel(int x,int y,int z,String kind,String block){this.x=x;this.y=y;this.z=z;this.kind=kind;this.block=block;}}
    public static final class Spot { int x,y,z; Spot(int x,int y,int z){this.x=x;this.y=y;this.z=z;} }
    public static final class Mark { String name; int x,y,z; Mark(String n,int x,int y,int z){this.name=n;this.x=x;this.y=y;this.z=z;} }
    public static final class Route { String name; List<Spot> path; Route(String n,List<Spot> p){name=n;path=p;} }
    public static final class Store { List<Voxel> voxels=new ArrayList<>(); List<Spot> trail=new ArrayList<>();
        List<Mark> marks=new ArrayList<>(); List<Route> routes=new ArrayList<>(); }

    private final class MapScreen extends Screen {
        private double angle = Math.toRadians(45), zoom = 4.0;
        private boolean showOres = true, showFluids = true, showWalls = false;
        private int layer = 999;
        private EditBox markName, blockFilter;
        MapScreen() { super(Component.literal("DeepScan 3D")); }
        @Override protected void init() {
            int by = height-48;
            markName = new EditBox(font,12,by-23,125,18,Component.literal("Nome do marcador"));
            markName.setHint(Component.literal("Nome do marcador")); addRenderableWidget(markName);
            blockFilter = new EditBox(font,12,39,171,18,Component.literal("Filtro bloco"));
            blockFilter.setHint(Component.literal("Filtro: ore / diamond / minecraft..."));addRenderableWidget(blockFilter);
            addRenderableWidget(Button.builder(Component.literal("Marcar"), b -> mark(markName.getValue()))
                    .bounds(142,by-23,64,18).build());
            addRenderableWidget(Button.builder(Component.literal("Salvar rota"),b -> saveRoute()).bounds(210,by-23,91,18).build());
            addRenderableWidget(Button.builder(Component.literal("Voltar"),b -> backIndex=store.trail.size()-1).bounds(305,by-23,66,18).build());
            addRenderableWidget(Button.builder(Component.literal("Replay"),b -> { replay=!replay; if(replay)replayIndex=0; }).bounds(12,by,66,18).build());
            addRenderableWidget(Button.builder(Component.literal("Minerios"),b -> showOres=!showOres).bounds(82,by,69,18).build());
            addRenderableWidget(Button.builder(Component.literal("Fluidos"),b -> showFluids=!showFluids).bounds(155,by,69,18).build());
            addRenderableWidget(Button.builder(Component.literal("Paredes"),b -> showWalls=!showWalls).bounds(228,by,69,18).build());
            addRenderableWidget(Button.builder(Component.literal("Camada"),b -> layer=layer==999?12:layer==12?4:999)
                    .bounds(301,by,70,18).build());
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial) {
            g.fill(0,0,width,height,0xF107101C);
            g.drawCenteredString(font,"DEEPSCAN 3D | mapa local de cavernas",width/2,8,0x7CF8DC);
            g.drawString(font,"Setas: girar/altura | Scroll: zoom | Filtro por ID de bloco",12,23,0xB6BBC5);
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
                double px=mc.player.getX(), py=mc.player.getY(), pz=mc.player.getZ();
                int cx=width/2,cy=(height-65)/2+20;
                int skip=Math.max(1,store.voxels.size()/18000);
                for(int i=0;i<store.voxels.size();i+=skip) {
                    Voxel v=store.voxels.get(i);
                    if (Math.abs(v.x-px)>65 || Math.abs(v.z-pz)>65)continue;
                    if (Math.abs(v.y-py)>layer)continue;
                    if ("ORE".equals(v.kind) && !showOres)continue;
                    if ("FLUID".equals(v.kind) && !showFluids)continue;
                    if ("WALL".equals(v.kind) && !showWalls)continue;
                    if (blockFilter != null && !blockFilter.getValue().isBlank() && !v.block.contains(blockFilter.getValue().toLowerCase(Locale.ROOT))) continue;
                    int[] q=project(v.x-px,v.y-py,v.z-pz,cx,cy);
                    if (q[0]<4||q[0]>width-4||q[1]<63||q[1]>height-82)continue;
                    int color=switch(v.kind){case "ORE"->0xFFFFC44E;case "FLUID"->0xFF459DFF;case "FLOW"->0xFF577FC4;case "WALL"->0xFF43505B;default->0xFF416A77;};
                    g.fill(q[0],q[1],q[0]+("ORE".equals(v.kind)?4:2),q[1]+2,color);
                }
                if(!store.trail.isEmpty()) {
                    int limit = replay?Math.min(replayIndex+1,store.trail.size()):store.trail.size();
                    int start = Math.max(1,limit-2200);
                    for(int i=start;i<limit;i++){
                        Spot a=store.trail.get(i-1),b=store.trail.get(i);
                        if(Math.abs(a.x-px)>70||Math.abs(a.z-pz)>70)continue;
                        int[] a2=project(a.x-px,a.y-py,a.z-pz,cx,cy),b2=project(b.x-px,b.y-py,b.z-pz,cx,cy);
                        line(g,a2,b2,0xFF61E8AC);
                    }
                }
                for(Mark m:store.marks) {
                    int[] q=project(m.x-px,m.y-py,m.z-pz,cx,cy);
                    if(q[0]>0&&q[0]<width&&q[1]>35&&q[1]<height-85){g.fill(q[0]-3,q[1]-3,q[0]+4,q[1]+4,0xFFF5AF54);g.drawString(font,m.name,q[0]+6,q[1]-4,0xFFFFD68F);}
                }
                for (Route r:store.routes) {
                    if(r.path==null)continue;
                    for(int i=1;i<r.path.size();i+=4){Spot a=r.path.get(i-1),b=r.path.get(i);
                        if(Math.abs(a.x-px)<70&&Math.abs(a.z-pz)<70)line(g,project(a.x-px,a.y-py,a.z-pz,cx,cy),project(b.x-px,b.y-py,b.z-pz,cx,cy),0xFFDD79FB);
                    }
                }
                g.fill(cx-4,cy-4,cx+5,cy+5,0xFFFF5D75);
            }
            g.drawString(font,"Pontos: "+store.voxels.size()+" | Trilhas: "+store.trail.size()+" | Marcadores: "+store.marks.size()+" | Rotas: "+store.routes.size(),12,height-20,0xE0E0E0);
            super.render(g,mx,my,partial);
        }
        private int[] project(double x,double y,double z,int cx,int cy) {
            double xx=x*Math.cos(angle)-z*Math.sin(angle);
            double zz=x*Math.sin(angle)+z*Math.cos(angle);
            return new int[]{cx+(int)(xx*zoom),cy+(int)((zz*0.40-y*0.9)*zoom)};
        }
        private void line(GuiGraphics g,int[] a,int[] b,int color) {
            int dx=b[0]-a[0],dy=b[1]-a[1];int steps=Math.max(Math.abs(dx),Math.abs(dy));
            if(steps>2000)return;
            if(steps==0)return;
            for(int i=0;i<=steps;i+=2){int x=a[0]+i*dx/steps,y=a[1]+i*dy/steps;
                if(x>=0&&x<width&&y>34&&y<height-82)g.fill(x,y,x+2,y+2,color);
            }
        }
        @Override public boolean mouseScrolled(double x,double y,double dx,double dy) {
            zoom=Math.max(1,Math.min(20,zoom+dy*.7));return true;
        }
        @Override public boolean keyPressed(int key,int scan,int modifiers) {
            if (markName.isFocused() || blockFilter.isFocused()) return super.keyPressed(key,scan,modifiers);
            if(key==GLFW.GLFW_KEY_LEFT){angle-=.15;return true;}
            if(key==GLFW.GLFW_KEY_RIGHT){angle+=.15;return true;}
            if(key==GLFW.GLFW_KEY_UP){layer=Math.min(999,layer+5);return true;}
            if(key==GLFW.GLFW_KEY_DOWN){layer=Math.max(1,layer==999?40:layer-5);return true;}
            return super.keyPressed(key,scan,modifiers);
        }
        @Override public void onClose(){save();super.onClose();}
        @Override public boolean isPauseScreen(){return false;}
    }
}
