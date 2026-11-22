package com.portmods.timeframe;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Explicitly local FFmpeg wrapper. Does not hook game/server packets or capture
 * devices until the player manually starts it.
 */
public final class ClipRecorder {
    private static final int SEGMENT_SECONDS = 10;
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");
    private final TimeFrameConfig settings;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TimeFrame-FFmpeg");
        t.setDaemon(true);
        return t;
    });
    private volatile Process process;
    private volatile Path session;
    private volatile String status = "Parado";
    private volatile List<String> devices = List.of();
    private volatile boolean starting;
    private volatile long started;
    private int ticks;

    public ClipRecorder(TimeFrameConfig config) { this.settings = config; }
    public String status() { return status; }
    public boolean recording() { return process != null && process.isAlive(); }
    public List<String> devices() { return devices; }
    public int elapsedSeconds() { return recording() ? (int)((System.currentTimeMillis() - started) / 1000) : 0; }

    public void tick() {
        ticks++;
        if (ticks % 100 != 0) return;
        if (recording()) io.submit(this::prune);
        else if (process != null && !starting) {
            process = null;
            if (status.equals("Gravando")) status = "FFmpeg terminou inesperadamente. Verifique os dispositivos.";
        }
    }

    private boolean windows() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"); }
    private boolean linux() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux"); }

    public void refreshDevices() {
        io.submit(() -> {
            List<String> found = new ArrayList<>();
            try {
                if (windows()) {
                    List<String> lines = runWithOutput(List.of(settings.ffmpeg, "-hide_banner",
                        "-list_devices", "true", "-f", "dshow", "-i", "dummy"), 6);
                    boolean audio = false;
                    for (String line : lines) {
                        if (line.contains("DirectShow audio devices")) { audio = true; continue; }
                        if (line.contains("DirectShow video devices")) { audio = false; continue; }
                        if (audio && !line.contains("Alternative name")) {
                            Matcher m = QUOTED.matcher(line);
                            if (m.find() && !found.contains(m.group(1))) found.add(m.group(1));
                        }
                    }
                } else if (linux()) {
                    for (String line : runWithOutput(List.of("pactl", "list", "short", "sources"), 5)) {
                        String[] fields = line.split("\\t");
                        if (fields.length >= 2 && !found.contains(fields[1])) found.add(fields[1]);
                    }
                } else {
                    status = "Selecao de dispositivos FFmpeg ainda nao implementada neste sistema.";
                    return;
                }
                devices = List.copyOf(found);
                status = found.isEmpty() ? "Nenhuma fonte de audio detectada. Verifique FFmpeg/pactl." :
                    "Fontes detectadas: " + found.size() + ". Selecione no painel.";
            } catch (Exception e) {
                status = "Erro detectando dispositivos: " + e.getMessage();
            }
        });
    }

    public void start() {
        if (recording() || starting) return;
        settings.normalize();
        if (!windows() && !linux()) {
            status = "Captura do desktop disponivel apenas Windows ou Linux/X11 nesta versao.";
            return;
        }
        starting = true;
        status = "Iniciando FFmpeg...";
        io.submit(() -> {
            try {
                Path root = TimeFrameConfig.folder().resolve("buffer");
                session = root.resolve(new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date()));
                Files.createDirectories(session);
                List<String> cmd = captureCommand(session.resolve("part-%06d.mkv"));
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                process = p;
                started = System.currentTimeMillis();
                status = "Gravando";
                Thread log = new Thread(() -> {
                    try (InputStream in = p.getInputStream()) {
                        byte[] bytes = new byte[8192];
                        int n;
                        while ((n = in.read(bytes)) != -1) {
                            String message = new String(bytes, 0, n, StandardCharsets.UTF_8);
                            if (message.contains("Error") || message.contains("error")
                                    || message.contains("Unknown input") || message.contains("Cannot open")) {
                                status = message.length() > 220 ? message.substring(0,220) : message;
                            }
                        }
                    } catch (IOException ignored) { }
                }, "TimeFrame-FFmpegLogs");
                log.setDaemon(true);
                log.start();
                if (!p.waitFor(1500, TimeUnit.MILLISECONDS)) {
                    status = "Gravando";
                } else {
                    status = "FFmpeg encerrou (" + p.exitValue() + "). Confira os dispositivos.";
                    process = null;
                }
            } catch (Exception e) {
                status = "Falha iniciando captura: " + e.getMessage();
                process = null;
            } finally { starting = false; }
        });
    }

    public void stop() {
        Process p = process;
        if (p == null) return;
        status = "Finalizando segmentos...";
        io.submit(() -> {
            try {
                OutputStream stdin = p.getOutputStream();
                stdin.write('q');
                stdin.flush();
                if (!p.waitFor(5, TimeUnit.SECONDS)) p.destroy();
            } catch (Exception ignored) {
                p.destroy();
            }
            process = null;
            status = "Gravacao parada";
        });
    }

    /** FFmpeg inputs are passed as separate arguments, never to a shell. */
    private List<String> captureCommand(Path output) {
        List<String> command = new ArrayList<>(List.of(settings.ffmpeg, "-hide_banner", "-loglevel", "warning", "-y"));
        if (windows()) {
            command.addAll(List.of("-f", "gdigrab", "-framerate", Integer.toString(settings.fps), "-i", "desktop"));
        } else {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isBlank()) display = ":0.0";
            command.addAll(List.of("-f", "x11grab", "-framerate", Integer.toString(settings.fps), "-i", display));
        }
        List<String> filters = new ArrayList<>();
        int input = 1;
        List<String> audioLabels = new ArrayList<>();
        if (settings.gameAudioEnabled && !settings.gameAudio.isBlank()) {
            appendAudioInput(command, settings.gameAudio);
            filters.add("[" + input + ":a]volume=" + (settings.gameVolume / 100f) + "[game]");
            audioLabels.add("[game]"); input++;
        }
        if (settings.microphoneEnabled && !settings.microphone.isBlank()) {
            appendAudioInput(command, settings.microphone);
            String filter = "[" + input + ":a]highpass=f=80";
            if (settings.microphoneNoiseFilter) filter += ",afftdn=nr=12";
            filter += ",volume=" + (settings.microphoneVolume / 100f) + "[mic]";
            filters.add(filter);
            audioLabels.add("[mic]");
        }
        command.addAll(List.of("-map", "0:v", "-c:v", "libx264", "-preset", "veryfast",
                "-crf", "24", "-pix_fmt", "yuv420p", "-g", Integer.toString(settings.fps * SEGMENT_SECONDS)));
        if (audioLabels.isEmpty()) {
            command.add("-an");
        } else if (audioLabels.size() == 2 && !settings.separateTracks) {
            filters.add("[game][mic]amix=inputs=2:duration=longest:dropout_transition=0[aout]");
            command.addAll(List.of("-filter_complex", String.join(";", filters),
                    "-map", "[aout]", "-c:a", "aac", "-b:a", "192k"));
        } else {
            command.addAll(List.of("-filter_complex", String.join(";", filters)));
            for (String audio : audioLabels) command.addAll(List.of("-map", audio));
            command.addAll(List.of("-c:a", "aac", "-b:a", "160k"));
        }
        command.addAll(List.of("-f", "segment", "-segment_time", Integer.toString(SEGMENT_SECONDS),
                "-segment_format", "matroska", "-reset_timestamps", "1", output.toString()));
        return command;
    }

    private void appendAudioInput(List<String> cmd, String device) {
        if (windows()) cmd.addAll(List.of("-f", "dshow", "-i", "audio=" + device));
        else cmd.addAll(List.of("-f", "pulse", "-i", device));
    }

    private static List<String> runWithOutput(List<String> cmd, int timeoutSeconds) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Process p = pb.start();
        if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("Tempo limite do comando externo.");
        }
        return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).lines().toList();
    }

    public void saveClip(String reason) {
        io.submit(() -> {
            Path source = session;
            if (source == null || !Files.isDirectory(source)) {
                status = "Sem buffer. Inicie a gravacao primeiro.";
                return;
            }
            try {
                List<Path> parts = listSegments(source);
                if (recording() && !parts.isEmpty()) parts.remove(parts.size() - 1); // active segment is not finished
                int count = Math.max(1, (settings.bufferSeconds + SEGMENT_SECONDS - 1) / SEGMENT_SECONDS);
                if (parts.size() > count) parts = new ArrayList<>(parts.subList(parts.size() - count, parts.size()));
                if (parts.isEmpty()) { status = "Aguardando o primeiro segmento completo (10s)."; return; }
                Path clips = TimeFrameConfig.folder().resolve("clips");
                Files.createDirectories(clips);
                String now = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
                String safe = reason.replaceAll("[^a-zA-Z0-9_-]", "_");
                Path temporary = clips.resolve(".tmp-" + now);
                Files.createDirectories(temporary);
                List<String> concat = new ArrayList<>();
                for (Path part : parts) {
                    Path copy = temporary.resolve(part.getFileName());
                    Files.copy(part, copy, StandardCopyOption.REPLACE_EXISTING);
                    concat.add("file '" + copy.getFileName() + "'");
                }
                Path listing = temporary.resolve("parts.txt");
                Files.write(listing, concat, StandardCharsets.UTF_8);
                Path output = clips.resolve(now + "-" + safe + ".mp4");
                List<String> cmd = List.of(settings.ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
                        "-f", "concat", "-safe", "0", "-i", listing.toString(), "-map", "0",
                        "-c", "copy", "-movflags", "+faststart", output.toString());
                List<String> result = runWithOutput(cmd, 120);
                if (!Files.isRegularFile(output) || Files.size(output) == 0) {
                    status = "Falha ao salvar MP4: " + String.join(" ", result).substring(0, Math.min(150, String.join(" ", result).length()));
                } else {
                    status = "Clipe salvo: " + output.getFileName();
                }
                for (Path copy : partsInDirectory(temporary)) Files.deleteIfExists(copy);
                Files.deleteIfExists(temporary);
            } catch (Exception e) {
                status = "Erro salvando clipe: " + e.getMessage();
            }
        });
    }

    private static List<Path> partsInDirectory(Path folder) throws IOException {
        try (Stream<Path> stream = Files.list(folder)) { return stream.toList(); }
    }
    private static List<Path> listSegments(Path folder) throws IOException {
        try (Stream<Path> stream = Files.list(folder)) {
            return new ArrayList<>(stream.filter(p -> p.getFileName().toString().endsWith(".mkv"))
                    .sorted(Comparator.comparing(Path::getFileName)).toList());
        }
    }

    private void prune() {
        Path folder = session;
        if (folder == null || !Files.isDirectory(folder)) return;
        try {
            List<Path> parts = listSegments(folder);
            int keep = Math.max(4, (settings.bufferSeconds + SEGMENT_SECONDS - 1) / SEGMENT_SECONDS + 2);
            for (int i = 0; i < parts.size() - keep; i++) Files.deleteIfExists(parts.get(i));
        } catch (Exception e) { System.err.println("[TimeFrame] Prune: " + e); }
    }

    public void testMicrophone() {
        if (settings.microphone.isBlank()) { status = "Escolha um microfone primeiro."; return; }
        io.submit(() -> {
            try {
                Files.createDirectories(TimeFrameConfig.folder());
                Path wav = TimeFrameConfig.folder().resolve("microphone-test.wav");
                List<String> cmd = new ArrayList<>(List.of(settings.ffmpeg, "-hide_banner", "-loglevel", "error", "-y"));
                appendAudioInput(cmd, settings.microphone);
                cmd.addAll(List.of("-t", "3", "-ac", "1", "-ar", "44100"));
                if (settings.microphoneNoiseFilter) cmd.addAll(List.of("-af", "highpass=f=80,afftdn=nr=12"));
                cmd.addAll(List.of("-c:a", "pcm_s16le", wav.toString()));
                runWithOutput(cmd, 10);
                status = Files.size(wav) > 0 ? "Teste salvo em timeframe/microphone-test.wav" : "Falha no teste do microfone";
            } catch (Exception e) { status = "Falha no teste: " + e.getMessage(); }
        });
    }
    public void openClipsFolder() {
        io.submit(() -> {
            try {
                Path folder = TimeFrameConfig.folder().resolve("clips");
                Files.createDirectories(folder);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder.toFile());
                else status = "Clipes: " + folder;
            } catch (Exception e) { status = "Nao foi possivel abrir pasta: " + e.getMessage(); }
        });
    }
}
