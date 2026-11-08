package com.port.cardscanner;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Port Card Scanner: NFC tag diagnostics only.
 * Does not emulate a credential, read secured sectors, or capture RF waveforms.
 */
public final class MainActivity extends Activity implements NfcAdapter.ReaderCallback {

    private static final int BG = Color.rgb(9, 9, 14);
    private static final int PANEL = Color.rgb(23, 20, 27);
    private static final int PANEL_2 = Color.rgb(29, 22, 31);
    private static final int BORDER = Color.rgb(62, 45, 53);
    private static final int WHITE = Color.rgb(247, 242, 237);
    private static final int MUTED = Color.rgb(169, 158, 162);
    private static final int GOLD = Color.rgb(238, 193, 105);
    private static final int RED = Color.rgb(233, 62, 84);
    private static final int GREEN = Color.rgb(116, 212, 158);

    private final ArrayDeque<ScanResult> scans = new ArrayDeque<>();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", new Locale("pt", "BR"));
    private NfcAdapter adapter;
    private FrameLayout body;
    private LinearLayout navigation;
    private int selectedTab = 0;
    private boolean scanning = true;
    private boolean foreground = false;
    private boolean readerActive = false;
    private String status = "Aproxime um cartão NFC";
    private ScanResult latest;
    private String previousUid = "";
    private long lastDiscoveryMs = 0L;
    private int readCount = 0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        adapter = NfcAdapter.getDefaultAdapter(this);
        buildShell();
        showTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        configureReader();
        renderCurrentTab();
    }

    @Override
    protected void onPause() {
        foreground = false;
        if (adapter != null && readerActive) {
            try {
                adapter.disableReaderMode(this);
            } catch (RuntimeException ignored) {
                // NFC service or activity may already have stopped.
            }
        }
        readerActive = false;
        super.onPause();
    }

    private void configureReader() {
        if (adapter == null) {
            status = "Seu dispositivo não possui NFC disponível";
            return;
        }
        if (!adapter.isEnabled()) {
            status = "NFC desativado nos ajustes";
            return;
        }
        if (!scanning) {
            status = "Leitura pausada";
            if (readerActive) {
                try { adapter.disableReaderMode(this); } catch (RuntimeException ignored) { }
                readerActive = false;
            }
            return;
        }
        status = latest == null ? "Aproxime um cartão NFC" : "Aguardando próximo cartão";
        if (foreground && !readerActive) {
            int flags = NfcAdapter.FLAG_READER_NFC_A
                    | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F
                    | NfcAdapter.FLAG_READER_NFC_V
                    | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;
            try {
                adapter.enableReaderMode(this, this, flags, null);
                readerActive = true;
            } catch (RuntimeException e) {
                status = "Não foi possível iniciar a leitura NFC";
                readerActive = false;
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        final ScanResult result;
        try {
            result = inspectTag(tag);
        } catch (RuntimeException e) {
            runOnUiThread(() -> {
                status = "Cartão detectado, mas os detalhes falharam";
                renderCurrentTab();
            });
            return;
        }
        runOnUiThread(() -> {
            if (!scanning || !foreground) return;
            long now = SystemClock.elapsedRealtime();
            // Avoid duplicate reads while the same tag stays against the antenna.
            if (result.uid.equals(previousUid) && now - lastDiscoveryMs < 1500L) return;
            previousUid = result.uid;
            lastDiscoveryMs = now;
            readCount++;
            latest = result;
            scans.addFirst(result);
            while (scans.size() > 30) scans.removeLast();
            status = "Cartão detectado com sucesso";
            renderCurrentTab();
            Toast.makeText(this, "NFC detectado • " + result.family, Toast.LENGTH_SHORT).show();
        });
    }

    private ScanResult inspectTag(Tag tag) {
        ScanResult r = new ScanResult();
        byte[] id = tag.getId();
        r.uid = (id == null || id.length == 0) ? "Não fornecido" : asHex(id);
        r.uidBytes = id == null ? 0 : id.length;
        r.time = clock.format(new Date());
        String[] technologies = tag.getTechList();
        List<String> names = new ArrayList<>();
        if (technologies != null) {
            for (String tech : technologies) {
                String name = tech.substring(tech.lastIndexOf('.') + 1);
                names.add(name);
            }
        }
        r.technology = names.isEmpty() ? "Não identificada" : join(names, " · ");
        if (names.contains("IsoDep")) r.family = "ISO-DEP / Smartcard";
        else if (names.contains("MifareClassic")) r.family = "MIFARE Classic";
        else if (names.contains("MifareUltralight")) r.family = "MIFARE Ultralight";
        else if (names.contains("NfcA")) r.family = "NFC-A (13,56 MHz)";
        else if (names.contains("NfcB")) r.family = "NFC-B (13,56 MHz)";
        else if (names.contains("NfcF")) r.family = "NFC-F (13,56 MHz)";
        else if (names.contains("NfcV")) r.family = "NFC-V (13,56 MHz)";
        else r.family = "NFC não identificado";

        NfcA a = NfcA.get(tag);
        if (a != null) {
            r.details.add("ATQA: " + asHex(a.getAtqa()));
            r.details.add(String.format(Locale.US, "SAK: %02X", a.getSak() & 0xFF));
        }
        NfcB b = NfcB.get(tag);
        if (b != null) {
            r.details.add("NFC-B application data: " + asHex(b.getApplicationData()));
            r.details.add("NFC-B protocol info: " + asHex(b.getProtocolInfo()));
        }
        NfcF f = NfcF.get(tag);
        if (f != null) {
            r.details.add("NFC-F system code: " + asHex(f.getSystemCode()));
            r.details.add("NFC-F manufacturer: " + asHex(f.getManufacturer()));
        }
        NfcV v = NfcV.get(tag);
        if (v != null) {
            r.details.add(String.format(Locale.US, "NFC-V DSFID: %02X", v.getDsfId() & 0xFF));
            r.details.add(String.format(Locale.US, "NFC-V response flags: %02X", v.getResponseFlags() & 0xFF));
        }
        IsoDep iso = IsoDep.get(tag);
        if (iso != null) {
            if (iso.getHistoricalBytes() != null) r.details.add("ISO-DEP historical bytes: " + asHex(iso.getHistoricalBytes()));
            if (iso.getHiLayerResponse() != null) r.details.add("ISO-DEP higher layer: " + asHex(iso.getHiLayerResponse()));
        }
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            r.details.add("NDEF: " + ndef.getType());
            r.details.add("NDEF capacidade: " + ndef.getMaxSize() + " bytes");
            r.details.add("NDEF gravável: " + (ndef.isWritable() ? "sim" : "não"));
            NdefMessage msg = ndef.getCachedNdefMessage();
            if (msg != null) {
                r.details.add("NDEF registros: " + msg.getRecords().length);
                r.details.add("NDEF tamanho: " + msg.toByteArray().length + " bytes");
            }
        } else {
            r.details.add("NDEF: não identificado / não disponível");
        }
        r.details.add("Força do sinal (RSSI): não disponibilizada pelo Android NFC");
        return r;
    }

    private static String asHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "N/D";
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            if (result.length() > 0) result.append(' ');
            result.append(String.format(Locale.US, "%02X", b & 0xFF));
        }
        return result.toString();
    }

    private static String join(List<String> items, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) builder.append(separator);
            builder.append(item);
        }
        return builder.toString();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(22), dp(22), dp(22), dp(16));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView emblem = label("◈", 30, GOLD, true);
        emblem.setGravity(Gravity.CENTER);
        emblem.setBackground(round(PANEL_2, BORDER, 15));
        LinearLayout.LayoutParams emblemLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        emblemLp.rightMargin = dp(12);
        header.addView(emblem, emblemLp);

        LinearLayout headings = vertical();
        header.addView(headings, new LinearLayout.LayoutParams(0, -2, 1));
        headings.addView(label("PORT CARD", 23, WHITE, true));
        headings.addView(label("NFC SIGNAL ANALYZER", 10, GOLD, true));

        TextView version = label("v1.0", 11, GOLD, true);
        version.setPadding(dp(12), dp(8), dp(12), dp(8));
        version.setBackground(round(PANEL, BORDER, 20));
        header.addView(version);

        View line = new View(this);
        line.setBackgroundColor(BORDER);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

        navigation = new LinearLayout(this);
        navigation.setPadding(dp(12), dp(9), dp(12), dp(10));
        navigation.setBackgroundColor(PANEL);
        navigation.setGravity(Gravity.CENTER);
        root.addView(navigation, new LinearLayout.LayoutParams(-1, dp(75)));
    }

    private void showTab(int tab) {
        selectedTab = tab;
        renderCurrentTab();
    }

    private void renderCurrentTab() {
        if (body == null) return;
        body.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        body.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout column = vertical();
        column.setPadding(dp(20), dp(18), dp(20), dp(26));
        scroll.addView(column, new ScrollView.LayoutParams(-1, -2));
        if (selectedTab == 0) readerPage(column);
        else if (selectedTab == 1) historyPage(column);
        else aboutPage(column);
        renderNavigation();
    }

    private void renderNavigation() {
        navigation.removeAllViews();
        String[] labels = {"◉  LEITURA", "▤  HISTÓRICO", "ⓘ  SOBRE"};
        for (int i = 0; i < labels.length; i++) {
            final int next = i;
            TextView item = label(labels[i], 12, selectedTab == i ? GOLD : MUTED, selectedTab == i);
            item.setGravity(Gravity.CENTER);
            item.setBackground(round(selectedTab == i ? PANEL_2 : PANEL,
                    selectedTab == i ? GOLD : PANEL, 13));
            item.setOnClickListener(v -> showTab(next));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            navigation.addView(item, lp);
        }
    }

    private void readerPage(LinearLayout column) {
        TextView kicker = label("LEITOR DE CARTÕES", 12, GOLD, true);
        column.addView(kicker);
        addSpace(column, 7);
        column.addView(label("Aproxime. Detecte. Analise.", 22, WHITE, true));
        addSpace(column, 15);

        LinearLayout hero = vertical();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(18), dp(22), dp(18), dp(22));
        hero.setBackground(roundGradient(PANEL_2, Color.rgb(34, 17, 27), GOLD, 23));
        column.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        TextView eyebrow = label("◈   MONITOR NFC 13,56 MHz   ◈", 12, GOLD, true);
        eyebrow.setGravity(Gravity.CENTER);
        hero.addView(eyebrow);
        addSpace(hero, 17);

        FrameLayout radar = new FrameLayout(this);
        hero.addView(radar, new LinearLayout.LayoutParams(dp(142), dp(142)));
        View outer = circle(BORDER);
        radar.addView(outer, centered(dp(142), dp(142)));
        View middle = circle(PANEL_2);
        radar.addView(middle, centered(dp(130), dp(130)));
        View inner = circle(adapter != null && adapter.isEnabled() && scanning ? RED : BORDER);
        radar.addView(inner, centered(dp(106), dp(106)));
        View center = circle(Color.rgb(20, 14, 23));
        radar.addView(center, centered(dp(92), dp(92)));
        TextView symbol = label(latest != null && scanning ? "✓" : "⌁", 54,
                latest != null && scanning ? GREEN : GOLD, true);
        symbol.setGravity(Gravity.CENTER);
        radar.addView(symbol, centered(dp(90), dp(90)));
        addSpace(hero, 15);

        boolean ready = adapter != null && adapter.isEnabled() && scanning;
        TextView statusLabel = label(status, 18, ready ? WHITE : RED, true);
        statusLabel.setGravity(Gravity.CENTER);
        hero.addView(statusLabel);
        addSpace(hero, 6);
        TextView caption = label("Encoste o cartão na traseira do Poco M5", 12, MUTED, false);
        caption.setGravity(Gravity.CENTER);
        hero.addView(caption);
        addSpace(hero, 18);

        if (adapter != null && !adapter.isEnabled()) {
            hero.addView(button("ABRIR AJUSTES NFC", GOLD, BG, () -> {
                try { startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            }));
        } else {
            hero.addView(button(scanning ? "⏸  PAUSAR LEITURA" : "▶  ATIVAR LEITURA",
                    scanning ? GOLD : RED, BG, () -> {
                        scanning = !scanning;
                        configureReader();
                        renderCurrentTab();
                    }));
        }
        addSpace(column, 16);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        column.addView(stats);
        statBox(stats, String.valueOf(readCount), "LEITURAS NA SESSÃO");
        statBox(stats, adapter == null ? "OFF" : (adapter.isEnabled() ? "ON" : "OFF"), "NFC DO CELULAR");
        addSpace(column, 22);

        LinearLayout section = horizontal();
        section.setGravity(Gravity.CENTER_VERTICAL);
        column.addView(section);
        section.addView(label("ÚLTIMA DETECÇÃO", 13, GOLD, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        section.addView(label(latest == null ? "SEM DADOS" : latest.time, 12, MUTED, false));
        addSpace(column, 10);

        if (latest == null) {
            LinearLayout empty = panel();
            empty.addView(label("Nenhum cartão lido ainda", 17, WHITE, true));
            addSpace(empty, 7);
            empty.addView(label("Cartões compatíveis aparecem aqui automaticamente. " +
                    "Os dados exibidos são apenas os que o Android disponibiliza.", 13, MUTED, false));
            column.addView(empty);
        } else {
            renderResult(column, latest);
        }
    }

    private void renderResult(LinearLayout column, ScanResult result) {
        LinearLayout p = panel();
        column.addView(p);
        p.addView(label("TIPO DE CARTÃO", 10, GOLD, true));
        addSpace(p, 5);
        p.addView(label(result.family, 19, WHITE, true));
        addSpace(p, 17);
        p.addView(label("UID / IDENTIFICADOR APRESENTADO", 10, GOLD, true));
        addSpace(p, 5);
        TextView uid = label(result.uid, 15, WHITE, true);
        uid.setTextIsSelectable(true);
        p.addView(uid);
        addSpace(p, 6);
        p.addView(label(result.uidBytes + " byte(s). Pode ser aleatório em alguns cartões.",
                11, MUTED, false));
        addSpace(p, 18);
        p.addView(label("TECNOLOGIAS DETECTADAS", 10, GOLD, true));
        addSpace(p, 6);
        p.addView(label(result.technology, 13, WHITE, false));
        addSpace(p, 18);
        p.addView(label("METADADOS TÉCNICOS", 10, GOLD, true));
        addSpace(p, 7);
        for (String d : result.details) {
            p.addView(label("• " + d, 12, MUTED, false));
            addSpace(p, 4);
        }
        addSpace(p, 16);
        LinearLayout actions = horizontal();
        p.addView(actions);
        actions.addView(button("COPIAR", GOLD, BG, () -> copyReport(result)),
                weightedButton());
        addSpaceHorizontal(actions, 8);
        actions.addView(button("COMPARTILHAR", PANEL_2, GOLD, () -> shareReport(result)),
                weightedButton());
    }

    private void historyPage(LinearLayout column) {
        column.addView(label("REGISTROS DA SESSÃO", 12, GOLD, true));
        addSpace(column, 8);
        column.addView(label("Histórico NFC", 27, WHITE, true));
        addSpace(column, 7);
        column.addView(label("Até 30 registros em memória. Ao fechar o aplicativo, " +
                "o histórico não é armazenado permanentemente.", 13, MUTED, false));
        addSpace(column, 20);
        if (scans.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(label("Nenhuma leitura registrada", 17, WHITE, true));
            addSpace(empty, 8);
            empty.addView(label("Volte para Leitura e aproxime um cartão NFC.", 13, MUTED, false));
            column.addView(empty);
            return;
        }
        column.addView(button("LIMPAR HISTÓRICO", RED, WHITE, () -> {
            scans.clear();
            readCount = 0;
            latest = null;
            previousUid = "";
            status = "Aproxime um cartão NFC";
            renderCurrentTab();
        }));
        addSpace(column, 14);
        for (ScanResult scan : scans) {
            LinearLayout entry = panel();
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(10);
            column.addView(entry, lp);
            LinearLayout row = horizontal();
            entry.addView(row);
            row.addView(label(scan.family, 15, WHITE, true),
                    new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(label(scan.time, 12, GOLD, true));
            addSpace(entry, 7);
            TextView id = label(scan.uid, 12, MUTED, false);
            id.setTextIsSelectable(true);
            entry.addView(id);
            entry.setOnClickListener(v -> {
                latest = scan;
                showTab(0);
            });
        }
    }

    private void aboutPage(LinearLayout column) {
        column.addView(label("PORT LABS / UTILITÁRIOS", 12, GOLD, true));
        addSpace(column, 9);
        column.addView(label("Diagnóstico de cartões NFC", 25, WHITE, true));
        addSpace(column, 17);
        addInfoCard(column, "O QUE O APP CAPTURA",
                "Identificação de tags suportadas, UID quando fornecido, tecnologias Android, " +
                "metadados NFC-A/B/F/V, ISO-DEP e NDEF. Nenhum pacote é enviado à internet.");
        addSpace(column, 10);
        addInfoCard(column, "O QUE NÃO CAPTURA",
                "Sinal de rádio bruto, potência/RSSI, cartões RFID de 125 kHz, " +
                "chaves de autenticação ou comunicação entre o relógio de ponto e outro cartão.");
        addSpace(column, 10);
        addInfoCard(column, "SEGURANÇA E USO",
                "Use apenas com cartões próprios ou que você tem permissão para testar. " +
                "A leitura de um UID não permite substituir, duplicar ou emular automaticamente uma credencial.");
        addSpace(column, 10);
        addInfoCard(column, "COMPATIBILIDADE",
                "Android 8.0+ e hardware NFC. A versão do Poco M5 precisa ter NFC funcional e habilitado. " +
                "Para RFID 125 kHz seria necessário um leitor externo compatível.");
    }

    private void addInfoCard(LinearLayout parent, String title, String description) {
        LinearLayout p = panel();
        p.addView(label(title, 12, GOLD, true));
        addSpace(p, 9);
        p.addView(label(description, 14, WHITE, false));
        parent.addView(p);
    }

    private void statBox(LinearLayout row, String value, String title) {
        LinearLayout box = vertical();
        box.setPadding(dp(16), dp(16), dp(13), dp(16));
        box.setBackground(round(PANEL, BORDER, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(box, lp);
        box.addView(label(value, 25, GOLD, true));
        addSpace(box, 6);
        box.addView(label(title, 10, MUTED, true));
    }

    private String report(ScanResult result) {
        StringBuilder b = new StringBuilder();
        b.append("PORT CARD SCANNER 1.0\n")
                .append("Horário: ").append(result.time).append("\n")
                .append("Tipo: ").append(result.family).append("\n")
                .append("UID: ").append(result.uid).append("\n")
                .append("Tecnologias: ").append(result.technology).append("\n")
                .append("Detalhes:\n");
        for (String detail : result.details) b.append("- ").append(detail).append("\n");
        b.append("\nLeitura diagnóstica. Não é uma credencial emulada.\n");
        return b.toString();
    }

    private void copyReport(ScanResult result) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Leitura NFC", report(result)));
        Toast.makeText(this, "Relatório copiado", Toast.LENGTH_SHORT).show();
    }

    private void shareReport(ScanResult result) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Port Card Scanner • Leitura NFC");
        send.putExtra(Intent.EXTRA_TEXT, report(result));
        startActivity(Intent.createChooser(send, "Compartilhar relatório NFC"));
    }

    private LinearLayout panel() {
        LinearLayout p = vertical();
        p.setPadding(dp(18), dp(18), dp(18), dp(18));
        p.setBackground(round(PANEL, BORDER, 18));
        return p;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        view.setLineSpacing(dp(2), 1.05f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView button(String text, int bg, int fg, Runnable action) {
        TextView view = label(text, 12, fg, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(13), dp(13), dp(13), dp(13));
        view.setMinHeight(dp(50));
        view.setBackground(round(bg, bg == PANEL_2 ? BORDER : bg, 12));
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private LinearLayout.LayoutParams weightedButton() {
        return new LinearLayout.LayoutParams(0, -2, 1);
    }

    private void addSpace(ViewGroup container, int dp) {
        View v = new View(this);
        container.addView(v, new ViewGroup.LayoutParams(1, dp(dp)));
    }

    private void addSpaceHorizontal(LinearLayout container, int dp) {
        View v = new View(this);
        container.addView(v, new LinearLayout.LayoutParams(dp(dp), 1));
    }

    private View circle(int color) {
        View view = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        view.setBackground(g);
        return view;
    }

    private FrameLayout.LayoutParams centered(int width, int height) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        lp.gravity = Gravity.CENTER;
        return lp;
    }

    private GradientDrawable round(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable roundGradient(int start, int end, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(float amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }

    private static final class ScanResult {
        String uid;
        int uidBytes;
        String family;
        String technology;
        String time;
        final List<String> details = new ArrayList<>();
    }
}
