package com.port.nexusclient;

import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class ClientConfig {
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("nexus-client.properties");
    static String url = "https://nexus-aterna-rp.onrender.com";
    static String token = "";
    static boolean allowChat = false;
    static boolean allowTelemetry = false;
    static boolean allowMind = false;
    static boolean allowAutonomy = false;

    static void load() {
        if (!Files.isRegularFile(PATH)) return;
        Properties values = new Properties();
        try (InputStream in = Files.newInputStream(PATH)) {
            values.load(in);
            String loadedUrl = values.getProperty("url", url).trim();
            if (loadedUrl.startsWith("https://") && loadedUrl.length() < 255)
                url = loadedUrl.replaceAll("/+$", "");
            token = values.getProperty("token", "").trim();
            allowChat = Boolean.parseBoolean(values.getProperty("allowChat", "false"));
            allowTelemetry = Boolean.parseBoolean(values.getProperty("allowTelemetry", "false"));
            allowMind = Boolean.parseBoolean(values.getProperty("allowMind", "false"));
            allowAutonomy = Boolean.parseBoolean(values.getProperty("allowAutonomy", "false"));
        } catch (IOException exception) {
            System.err.println("Nexus Client: não foi possível ler o arquivo de configuração.");
        }
    }

    static void save() {
        Properties values = new Properties();
        values.setProperty("url", url);
        values.setProperty("token", token);
        values.setProperty("allowChat", Boolean.toString(allowChat));
        values.setProperty("allowTelemetry", Boolean.toString(allowTelemetry));
        values.setProperty("allowMind", Boolean.toString(allowMind));
        values.setProperty("allowAutonomy", Boolean.toString(allowAutonomy));
        try {
            Files.createDirectories(PATH.getParent());
            try (OutputStream out = Files.newOutputStream(PATH)) {
                values.store(out, "Nexus Aterna Client. Não compartilhe o token de pareamento.");
            }
        } catch (IOException exception) {
            System.err.println("Nexus Client: falha ao salvar as configurações.");
        }
    }

    private ClientConfig() {}
}
