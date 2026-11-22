package com.portmods.timeframe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * Interactive 3D schematic viewer for sampled positions. Intentionally NOT
 * a reproduction of arbitrary world blocks, models or mod-specific spells.
 */
public final class TimeFrameReplayScreen extends Screen {
    private final TimeFrame mod;
    private final List<ReplayBuffer.Frame> frames;
    private final List<ReplayBuffer.Marker> markers;
    private int index, counter;
    private boolean playing;
    private float speed = 1.0f, accumulator;
    private double yaw = Math.toRadians(32), pitch = Math.toRadians(22), distance = 15.0;
    private double panX, panY;
    private static final float[] SPEEDS = {0.25f, 0.5f, 1f, 2f, 4f};
    private String notice = "";

    public TimeFrameReplayScreen(TimeFrame mod) {
        this(mod, null);
    }
    public TimeFrameReplayScreen(TimeFrame mod, ReplayBuffer.Replay saved) {
        super(Component.literal("TimeFrame | Replay 3D"));
        this.mod = mod;
        this.frames = saved == null || saved.frames == null ? mod.replay().snapshot() : saved.frames;
        this.markers = saved == null || saved.markers == null ? mod.replay().eventSnapshot() : saved.markers;
        index = Math.max(0, frames.size() - 1);
    }

    @Override protected void init() {
        int buttonWidth = Math.max(35, Math.min(82, (width - 38) / 7));
        int gap = 3;
        int x = 12;
        int y = height - 27;
        addRenderableWidget(Button.builder(Component.literal("-5s"), b -> seek(-50))
            .bounds(x, y, buttonWidth, 20).build()); x += buttonWidth + gap;
        addRenderableWidget(Button.builder(Component.literal(playing ? "Pausar" : "Play"), b -> {
            playing = !playing;
            b.setMessage(Component.literal(playing ? "Pausar" : "Play"));
        }).bounds(x, y, buttonWidth, 20).build()); x += buttonWidth + gap;
        addRenderableWidget(Button.builder(Component.literal("+5s"), b -> seek(50))
            .bounds(x, y, buttonWidth, 20).build()); x += buttonWidth + gap;
        addRenderableWidget(Button.builder(Component.literal("1.0x"), b -> {
            int found = 0;
            for (int i = 0; i < SPEEDS.length; i++) if (SPEEDS[i] == speed) found = i;
            speed = SPEEDS[(found + 1) % SPEEDS.length];
            b.setMessage(Component.literal(speed + "x"));
        }).bounds(x, y, buttonWidth, 20).build()); x += buttonWidth + gap;
        addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> {
            mod.replay().saveReplay("manual");
            notice = "Replay atual salvo";
        }).bounds(x, y, buttonWidth, 20).build()); x += buttonWidth + gap;
        addRenderableWidget(Button.builder(Component.literal("Ultimo"), b -> {
            try {
                ReplayBuffer.Replay saved = mod.replay().loadLatestReplay();
                if (saved == null || saved.frames == null || saved.frames.isEmpty()) {
                    notice = "Nenhum replay salvo";
                    return;
                }
                Minecraft.getInstance().setScreen(new TimeFrameReplayScreen(mod, saved));
            } catch (Exception ex) {
                notice = "Falha ao abrir replay: " + ex.getMessage();
            }
        }).bounds(x, y, buttonWidth, 20).build()); x += buttonWidth + gap;
        addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> onClose())
            .bounds(x, y, buttonWidth, 20).build());
    }
    private void seek(int delta) {
        index = Math.max(0, Math.min(frames.size() - 1, index + delta));
    }

    @Override public void tick() {
        if (!playing || frames.size() < 2) return;
        accumulator += speed / 2f;
        while (accumulator >= 1) {
            accumulator--;
            index++;
            if (index >= frames.size()) {
                index = frames.size() - 1;
                playing = false;
                break;
            }
        }
        counter++;
    }

    private record Point(int x, int y, boolean visible) {}
    private Point project(double x, double y, double z, ReplayBuffer.Actor player) {
        double dx = x - player.x - panX;
        double dy = y - player.y - 1.0 - panY;
        double dz = z - player.z;
        double xx = dx * Math.cos(yaw) - dz * Math.sin(yaw);
        double zz = dx * Math.sin(yaw) + dz * Math.cos(yaw);
        double yy = dy * Math.cos(pitch) - zz * Math.sin(pitch);
        double deep = distance + dy * Math.sin(pitch) + zz * Math.cos(pitch);
        if (deep < 0.7) return new Point(0, 0, false);
        double factor = Math.min(width, height) * 1.8 / deep;
        return new Point((int)(width / 2.0 + xx * factor), (int)(height / 2.0 - yy * factor), true);
    }
    private static void drawLine(GuiGraphics g, Point p, Point q, int color) {
        if (!p.visible || !q.visible) return;
        int dx = q.x - p.x, dy = q.y - p.y;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps > 3000) return;
        if (steps == 0) {
            g.fill(p.x, p.y, p.x + 2, p.y + 2, color);
            return;
        }
        int skip = Math.max(1, steps / 150);
        for (int i = 0; i <= steps; i += skip) {
            int x = p.x + dx * i / steps, y = p.y + dy * i / steps;
            if (x >= 2 && x < g.guiWidth() - 2 && y > 18 && y < g.guiHeight() - 35)
                g.fill(x, y, x + 2, y + 2, color);
        }
    }
    private void actor(GuiGraphics g, ReplayBuffer.Actor entity, ReplayBuffer.Actor player, int color, boolean self) {
        double half = Math.max(.15, Math.min(1.4, entity.width / 2.0));
        double height = Math.max(.55, Math.min(4.5, entity.height));
        Point[] corners = new Point[8];
        for (int i = 0; i < 8; i++) {
            double x = entity.x + (((i & 1) != 0) ? half : -half);
            double y = entity.y + (((i & 2) != 0) ? height : 0);
            double z = entity.z + (((i & 4) != 0) ? half : -half);
            corners[i] = project(x, y, z, player);
        }
        for (int i = 0; i < 8; i++) {
            if ((i & 1) == 0) drawLine(g, corners[i], corners[i | 1], color);
            if ((i & 2) == 0) drawLine(g, corners[i], corners[i | 2], color);
            if ((i & 4) == 0) drawLine(g, corners[i], corners[i | 4], color);
        }
        Point label = project(entity.x, entity.y + height + .25, entity.z, player);
        if (label.visible && label.x >= 0 && label.x < width && label.y > 20 && label.y < this.height - 40) {
            String name = self ? "VOCE" : entity.name;
            if (name.length() > 22) name = name.substring(0, 22);
            g.drawCenteredString(font, name, label.x, label.y, color | 0xFF000000);
        }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xFF0B111A);
        g.drawCenteredString(font, "TIMEFRAME | VISUALIZADOR TEMPORAL", width / 2, 7, 0xF4CA78);
        if (!frames.isEmpty()) {
            ReplayBuffer.Frame frame = frames.get(index);
            ReplayBuffer.Actor player = frame.player;
            if (player != null) {
                // A floor grid gives scale without claiming to re-create blocks.
                for (int i = -24; i <= 24; i += 4) {
                    drawLine(g, project(player.x - 24, player.y, player.z + i, player),
                            project(player.x + 24, player.y, player.z + i, player), 0x55406480);
                    drawLine(g, project(player.x + i, player.y, player.z - 24, player),
                            project(player.x + i, player.y, player.z + 24, player), 0x55406480);
                }
                for (ReplayBuffer.Actor a : frame.actors) actor(g, a, player, 0xFF71D9F7, false);
                actor(g, player, player, 0xFFFFCC69, true);
            }
            long elapsed = (frame.tick - frames.get(0).tick) / 20;
            long duration = (frames.get(frames.size() - 1).tick - frames.get(0).tick) / 20;
            g.drawString(font, "Tempo: " + elapsed + "s / " + duration + "s  | " + speed + "x"
                + "  |  Arraste: orbita, scroll: zoom, A/D: deslocar", 12, 24, 0xC6D8E6);
            String event = "";
            for (ReplayBuffer.Marker m : markers)
                if (m.tick <= frame.tick && m.tick >= frame.tick - 40) event = m.event;
            if (!event.isEmpty()) g.drawString(font, "EVENTO: " + event, 12, 38, 0xFF9585);
            int barY = height - 42;
            g.fill(12, barY, width - 12, barY + 6, 0xFF334151);
            int right = 12 + (int)((width - 24) * ((double)index / Math.max(1, frames.size() - 1)));
            g.fill(12, barY, right, barY + 6, 0xFFCEB066);
        } else {
            g.drawCenteredString(font, "Sem dados. Volte ao mundo e jogue alguns segundos.", width / 2, height / 2, 0xE0B0AE);
        }
        g.drawString(font, "Replay reconstruido por posicoes locais, sem blocos ou animacoes completas.",
            12, height - 56, 0x9AACBB);
        if (!notice.isBlank()) g.drawString(font, notice, 12, height - 70, 0xFFCB88);
        super.render(g, mx, my, partial);
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && my >= height - 46 && my <= height - 35 && !frames.isEmpty()) {
            index = Math.max(0, Math.min(frames.size() - 1,
                    (int)((mx - 12) / Math.max(1, width - 24) * (frames.size() - 1))));
            playing = false;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) {
            yaw += dx * .012;
            pitch = Math.max(-1.3, Math.min(1.3, pitch + dy * .012));
            return true;
        }
        if (button == 1) {
            panX -= dx * .025;
            panY += dy * .025;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        distance = Math.max(3, Math.min(120, distance * Math.pow(.88, sy)));
        return true;
    }
    @Override public boolean keyPressed(int key, int scanCode, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT) { seek(-1); playing = false; return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { seek(1); playing = false; return true; }
        if (key == GLFW.GLFW_KEY_A) { panX -= .7; return true; }
        if (key == GLFW.GLFW_KEY_D) { panX += .7; return true; }
        if (key == GLFW.GLFW_KEY_W) { panY += .7; return true; }
        if (key == GLFW.GLFW_KEY_S) { panY -= .7; return true; }
        return super.keyPressed(key, scanCode, mods);
    }
    @Override public boolean isPauseScreen() { return false; }
}
