package com.portmods.timeframe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.io.OutputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.stream.Stream;

/** A bounded record of visible nearby entities; no server replay packets or hidden data. */
public final class ReplayBuffer {
    private static final int MAX_FRAMES = 3_000; // 300 seconds at 10 Hz
    private static final int MAX_ENTITIES = 48;
    private static final Gson JSON = new GsonBuilder().create();
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private final ArrayDeque<Marker> markers = new ArrayDeque<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TimeFrame-ReplayWriter");
        t.setDaemon(true);
        return t;
    });
    private String world = "";
    private long ticks = 0;
    private float lastHealth = -1f;
    private boolean alive = true;
    private boolean deathFlag;
    private boolean combatActive;
    private boolean combatFlag;
    private boolean attackPressed;
    private long lastCombatTick;

    public void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (!world.isEmpty()) {
                frames.clear();
                markers.clear();
                world = "";
            }
            lastHealth = -1f;
            combatActive = false;
            combatFlag = false;
            attackPressed = false;
            return;
        }
        String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
        String dimension = mc.level.dimension().location().toString();
        String key = server + "|" + dimension;
        if (!key.equals(world)) {
            frames.clear();
            markers.clear();
            ticks = 0;
            lastHealth = -1f;
            alive = true;
            combatActive = false;
            combatFlag = false;
            attackPressed = false;
            world = key;
        }
        ticks++;
        boolean attacking = mc.options.keyAttack.isDown();
        if (attacking && !attackPressed && mc.hitResult instanceof EntityHitResult) {
            beginCombat();
            event("Golpe iniciado");
        }
        attackPressed = attacking;
        float health = mc.player.getHealth();
        if (lastHealth >= 0 && health < lastHealth) {
            event("Dano recebido: " + String.format(java.util.Locale.ROOT, "%.1f", lastHealth - health));
            beginCombat();
        }
        if (health <= 0f && alive) {
            event("Morte do jogador");
            alive = false;
            deathFlag = true;
            combatActive = false;
            combatFlag = false;
            saveReplay("morte");
        } else if (health > 0) {
            alive = true;
        }
        lastHealth = health;
        if (combatActive && ticks - lastCombatTick >= 20 * 15L && health > 0) {
            combatActive = false;
            combatFlag = true;
            event("Combate encerrado");
        }

        if (ticks % 2 != 0) return;
        Frame frame = new Frame();
        frame.tick = ticks;
        frame.timestamp = System.currentTimeMillis();
        frame.dimension = dimension;
        frame.player = actor(mc.player);
        List<Entity> entities = mc.level.getEntities(mc.player,
                new AABB(mc.player.blockPosition()).inflate(48));
        entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));
        int total = Math.min(MAX_ENTITIES, entities.size());
        for (int i = 0; i < total; i++) {
            Entity e = entities.get(i);
            if (e.isRemoved()) continue;
            frame.actors.add(actor(e));
        }
        frames.addLast(frame);
        while (frames.size() > MAX_FRAMES) frames.removeFirst();
        long cutoff = ticks - MAX_FRAMES * 2L;
        while (!markers.isEmpty() && markers.peekFirst().tick < cutoff) markers.removeFirst();
    }

    private static Actor actor(Entity e) {
        Actor a = new Actor();
        a.id = e.getUUID().toString();
        a.name = e.getName().getString();
        a.type = e.getType().toString();
        a.x = e.getX(); a.y = e.getY(); a.z = e.getZ();
        a.yaw = e.getYRot(); a.pitch = e.getXRot();
        a.width = e.getBbWidth(); a.height = e.getBbHeight();
        return a;
    }

    private void beginCombat() {
        combatActive = true;
        lastCombatTick = ticks;
    }
    public boolean takeCombatFlag() {
        boolean flag = combatFlag;
        combatFlag = false;
        return flag;
    }
    public boolean takeDeathFlag() {
        boolean flag = deathFlag;
        deathFlag = false;
        return flag;
    }
    public void event(String name) {
        markers.addLast(new Marker(ticks, name));
        while (markers.size() > 250) markers.removeFirst();
    }
    public int durationSeconds() {
        return frames.size() / 10;
    }
    public List<Frame> snapshot() {
        return new ArrayList<>(frames);
    }
    public List<Marker> eventSnapshot() { return new ArrayList<>(markers); }

    /** Copy data on client thread, compress and persist asynchronously. */
    public void saveReplay(String label) {
        if (frames.isEmpty()) return;
        Replay data = new Replay();
        data.version = 1;
        data.world = world;
        data.frames = snapshot();
        data.markers = eventSnapshot();
        data.captured = System.currentTimeMillis();
        String safe = label.replaceAll("[^a-zA-Z0-9_-]", "_");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path to = TimeFrameConfig.folder().resolve("replays/" + stamp + "-" + safe + ".json.gz");
        io.submit(() -> {
            try {
                Files.createDirectories(to.getParent());
                try (OutputStream stream = new GZIPOutputStream(Files.newOutputStream(to))) {
                    stream.write(JSON.toJson(data).getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) { System.err.println("[TimeFrame] Cannot export replay: " + e); }
        });
    }

    /** Load a previously saved replay from disk without contacting a server. */
    public Replay loadLatestReplay() throws IOException {
        Path directory = TimeFrameConfig.folder().resolve("replays");
        if (!Files.isDirectory(directory)) return null;
        Path chosen;
        try (Stream<Path> entries = Files.list(directory)) {
            chosen = entries.filter(p -> p.getFileName().toString().endsWith(".json.gz"))
                .max(Comparator.comparing(p -> p.getFileName().toString())).orElse(null);
        }
        if (chosen == null) return null;
        try (Reader reader = new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(chosen)), StandardCharsets.UTF_8)) {
            return JSON.fromJson(reader, Replay.class);
        }
    }

    public static final class Actor {
        public String id = UUID.randomUUID().toString(), name = "", type = "";
        public double x, y, z;
        public float yaw, pitch, width, height;
    }
    public static final class Frame {
        public long tick, timestamp;
        public String dimension = "";
        public Actor player;
        public List<Actor> actors = new ArrayList<>();
    }
    public static final class Marker {
        public long tick;
        public String event;
        public Marker(long tick, String event) { this.tick = tick; this.event = event; }
    }
    public static final class Replay {
        public int version;
        public long captured;
        public String world;
        public List<Frame> frames;
        public List<Marker> markers;
    }
}
