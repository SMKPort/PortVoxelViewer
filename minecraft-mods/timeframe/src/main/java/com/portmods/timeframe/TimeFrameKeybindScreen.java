package com.portmods.timeframe;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

/** Configures the standard Minecraft key bindings without replacing its options.txt handling. */
public final class TimeFrameKeybindScreen extends Screen {
    private final TimeFrame mod;
    private final Map<KeyMapping, Button> bindingButtons = new LinkedHashMap<>();
    private KeyMapping listening;
    private String message = "Clique em um atalho e pressione a tecla desejada.";

    public TimeFrameKeybindScreen(TimeFrame mod) {
        super(Component.literal("TimeFrame | Atalhos"));
        this.mod = mod;
    }

    @Override protected void init() {
        bindingButtons.clear();
        int w = Math.min(290, width - 28);
        int x = (width - w) / 2;
        int top = Math.max(48, height / 2 - 82);
        addBinding(TimeFrame.menuKey(), "Abrir menu", x, top, w);
        addBinding(TimeFrame.clipKey(), "Salvar clipe", x, top + 28, w);
        addBinding(TimeFrame.replayKey(), "Abrir replay 3D", x, top + 56, w);

        addRenderableWidget(Button.builder(Component.literal("Restaurar padroes (M / F9 / F10)"), b -> {
            listening = null;
            TimeFrame.restoreDefaultKeys();
            message = "Atalhos restaurados e salvos.";
            refreshButtons();
        }).bounds(x, top + 92, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Voltar ao TimeFrame"), b -> onClose())
                .bounds(x, top + 118, w, 20).build());
        refreshButtons();
    }

    private void addBinding(KeyMapping mapping, String name, int x, int y, int width) {
        Button button = Button.builder(Component.literal(name), b -> {
            listening = mapping;
            message = "Pressione a tecla. ESC cancela, Backspace remove o atalho.";
            refreshButtons();
        }).bounds(x, y, width, 20).build();
        bindingButtons.put(mapping, button);
        addRenderableWidget(button);
    }

    private void refreshButtons() {
        String[] titles = {"Abrir menu", "Salvar clipe", "Abrir replay 3D"};
        int i = 0;
        for (Map.Entry<KeyMapping, Button> entry : bindingButtons.entrySet()) {
            KeyMapping map = entry.getKey();
            String current = map.getTranslatedKeyMessage().getString();
            if (map.isUnbound()) current = "SEM TECLA";
            if (listening == map) current = "Pressione uma tecla...";
            entry.getValue().setMessage(Component.literal(titles[i++] + ": " + current));
        }
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listening = null;
                message = "Alteracao cancelada.";
            } else {
                InputConstants.Key selected = (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE)
                        ? InputConstants.UNKNOWN : InputConstants.getKey(key, scanCode);
                KeyMapping changed = listening;
                TimeFrame.rebind(changed, selected);
                listening = null;
                message = changed.getTranslatedKeyMessage().getString() + " foi salvo no Minecraft.";
                if (!changed.isUnbound()) {
                    for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
                        if (other != changed && !other.isUnbound() && changed.same(other)) {
                            message = "Atalho salvo, mas esta em conflito com outra acao. Altere em Controles.";
                            break;
                        }
                    }
                }
            }
            refreshButtons();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xEE111726);
        graphics.drawCenteredString(font, "TIMEFRAME | ATALHOS CONFIGURAVEIS", width / 2, 14, 0xFFD27A);
        graphics.drawCenteredString(font, "Alteracoes salvas automaticamente em options.txt", width / 2, 30, 0xA9C8DA);
        int y = Math.min(height - 29, Math.max(48, height / 2 - 82) + 147);
        graphics.drawCenteredString(font, message, width / 2, y, 0xE8D5AE);
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override public void onClose() {
        Minecraft.getInstance().setScreen(new TimeFrameScreen(mod));
    }

    @Override public boolean isPauseScreen() { return false; }
}
