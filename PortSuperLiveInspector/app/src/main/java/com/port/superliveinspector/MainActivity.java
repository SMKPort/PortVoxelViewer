package com.port.superliveinspector;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only explorer for installed SuperLive package and user-authorized files.
 * Does not change balances, data, transactions or private app files.
 */
public final class MainActivity extends Activity {
    private static final String PACKAGE = "com.superlive.liveapp";
    private static final int FOLDER_REQUEST = 101;
    private static final int MAX_APK_ITEMS = 10000;
    private static final int MAX_TREE_ITEMS = 1500;
    private static final int BG = Color.rgb(18, 18, 26);
    private static final int PANEL = Color.rgb(34, 35, 45);
    private static final int GOLD = Color.rgb(234, 185, 69);
    private TextView output, status;
    private EditText filter;
    private ScrollView scroll;
    private volatile String rawReport = "";

    interface ReportTask { String run() throws Exception; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        rawReport = "PORT SUPERLIVE INSPECTOR\n\n" +
                "Toque em LOCALIZAR para ver o pacote instalado.\n" +
                "O app apenas lê arquivos permitidos pelo Android.";
        render();
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        return b;
    }

    private void row(LinearLayout root, String left, Runnable l, String right, Runnable r) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        Button a = button(left), b = button(right);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), 1);
        p.setMargins(0, 0, dp(4), dp(5));
        container.addView(a, p);
        LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(0, dp(52), 1);
        q.setMargins(dp(4), 0, 0, dp(5));
        container.addView(b, q);
        a.setOnClickListener(v -> l.run());
        b.setOnClickListener(v -> r.run());
        root.addView(container);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(18), dp(15), dp(12));
        root.setBackgroundColor(BG);
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("PORT / SUPERLIVE INSPECTOR");
        title.setTextColor(GOLD);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(19);
        root.addView(title);
        TextView desc = new TextView(this);
        desc.setText("Arquivos acessíveis • inspeção local • somente leitura");
        desc.setTextColor(Color.LTGRAY);
        desc.setTextSize(12);
        desc.setPadding(0, dp(3), 0, dp(14));
        root.addView(desc);

        row(root, "LOCALIZAR SUPERLIVE", () -> runTask("Localizando...", this::inspectPackage),
                "LISTAR APK", () -> runTask("Lendo APKs...", this::inspectApk));
        row(root, "VERIFICAR PASTAS", () -> runTask("Verificando...", this::inspectFolders),
                "ESCOLHER PASTA", this::chooseFolder);

        filter = new EditText(this);
        filter.setSingleLine(true);
        filter.setTextColor(Color.WHITE);
        filter.setHintTextColor(Color.GRAY);
        filter.setHint("Filtrar nomes no relatório...");
        filter.setTextSize(14);
        root.addView(filter, new LinearLayout.LayoutParams(-1, dp(50)));
        filter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { render(); }
            public void afterTextChanged(Editable s) {}
        });

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(PANEL);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1);
        sp.topMargin = dp(8);
        root.addView(scroll, sp);

        output = new TextView(this);
        output.setTextSize(12);
        output.setTextColor(Color.WHITE);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(output);

        Button copy = button("COPIAR RELATÓRIO");
        root.addView(copy, new LinearLayout.LayoutParams(-1, dp(50)));
        copy.setOnClickListener(v -> {
            ClipboardManager c = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (c != null) {
                c.setPrimaryClip(ClipData.newPlainText("SuperLive Inspector", rawReport));
                Toast.makeText(this, "Relatório copiado", Toast.LENGTH_SHORT).show();
            }
        });
        status = new TextView(this);
        status.setText("Sem root • sem acesso à internet");
        status.setTextSize(11);
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        root.addView(status);
    }

    private void render() {
        if (output == null) return;
        String query = filter == null ? "" : filter.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            output.setText(rawReport);
        } else {
            StringBuilder b = new StringBuilder();
            int lines = 0;
            for (String line : rawReport.split("\\r?\\n")) {
                if (line.toLowerCase(Locale.ROOT).contains(query)) {
                    b.append(line).append('\n');
                    if (++lines == 2500) { b.append("[LIMITE DE EXIBIÇÃO]\n"); break; }
                }
            }
            output.setText(lines == 0 ? "Nenhum resultado encontrado." : b.toString());
        }
        scroll.post(() -> scroll.scrollTo(0, 0));
    }

    private void runTask(String progress, ReportTask task) {
        status.setText(progress);
        new Thread(() -> {
            String result;
            try { result = task.run(); }
            catch (Exception e) { result = "Erro: " + e.getClass().getSimpleName() + " / " + e.getMessage(); }
            final String r = result;
            runOnUiThread(() -> {
                rawReport = r;
                render();
                status.setText("Inspeção concluída • somente leitura");
            });
        }).start();
    }

    private StringBuilder header(String name) {
        return new StringBuilder("PORT SUPERLIVE INSPECTOR\n" + name +
                "\nPacote alvo: " + PACKAGE + "\n--------------------------------\n");
    }

    private String inspectPackage() {
        StringBuilder b = header("INSTALAÇÃO E METADADOS");
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pkg = pm.getPackageInfo(PACKAGE, 0);
            ApplicationInfo app = pkg.applicationInfo;
            if (app == null) return b.append("Dados de instalação indisponíveis.").toString();
            b.append("Nome: ").append(pm.getApplicationLabel(app)).append('\n');
            b.append("Pacote: ").append(PACKAGE).append('\n');
            b.append("Versão: ").append(pkg.versionName).append('\n');
            b.append("Código: ").append(Build.VERSION.SDK_INT >= 28 ?
                    pkg.getLongVersionCode() : pkg.versionCode).append('\n');
            b.append("Android API: ").append(Build.VERSION.SDK_INT).append('\n');
            b.append("APK base: ").append(app.sourceDir).append('\n');
            b.append("APK público: ").append(app.publicSourceDir).append('\n');
            if (app.splitSourceDirs != null) {
                for (String s : app.splitSourceDirs) b.append("APK split: ").append(s).append('\n');
            }
            b.append("Bibliotecas: ").append(app.nativeLibraryDir).append('\n');
            b.append("Dados privados: ").append(app.dataDir).append('\n');
            b.append("Dados credenciais: ").append(app.credentialProtectedDataDir).append('\n');
            b.append("\nTESTES DE ACESSO\n");
            access(b, app.sourceDir);
            access(b, app.dataDir);
            access(b, app.nativeLibraryDir);
            b.append("\nExibir o caminho não concede permissão para ler a pasta.\n");
        } catch (PackageManager.NameNotFoundException e) {
            b.append("SuperLive não identificado neste perfil Android.\n");
        }
        return b.toString();
    }

    private void access(StringBuilder b, String path) {
        if (path == null) return;
        File f = new File(path);
        b.append(path).append("\n  existe=").append(f.exists())
                .append("  leitura=").append(f.canRead())
                .append("  pasta=").append(f.isDirectory()).append('\n');
    }

    private String inspectApk() {
        StringBuilder b = header("ARQUIVOS EMPACOTADOS NO APK");
        try {
            ApplicationInfo app = getPackageManager().getApplicationInfo(PACKAGE, 0);
            List<String> apks = new ArrayList<>();
            if (app.sourceDir != null) apks.add(app.sourceDir);
            if (app.splitSourceDirs != null) Collections.addAll(apks, app.splitSourceDirs);
            int total = 0;
            for (String path : apks) {
                b.append("\nAPK: ").append(path).append('\n');
                if (!new File(path).canRead()) {
                    b.append("[ACESSO BLOQUEADO]\n");
                    continue;
                }
                try (ZipFile zip = new ZipFile(path)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    int count = 0;
                    while (entries.hasMoreElements()) {
                        ZipEntry e = entries.nextElement();
                        count++;
                        if (total < MAX_APK_ITEMS) {
                            b.append(e.isDirectory() ? "DIR  " : "FILE ").append(e.getName());
                            if (!e.isDirectory() && e.getSize() >= 0) {
                                b.append(" [").append(e.getSize()).append(" bytes]");
                            }
                            b.append('\n');
                            total++;
                        }
                    }
                    b.append("Entradas no APK: ").append(count).append('\n');
                } catch (IOException | SecurityException ex) {
                    b.append("Falha ao abrir: ").append(ex.getMessage()).append('\n');
                }
            }
            if (apks.isEmpty()) b.append("Nenhum APK encontrado pelo sistema.\n");
            if (total >= MAX_APK_ITEMS) b.append("[LIMITE DE LISTAGEM ATINGIDO]\n");
            b.append("\nA lista contém somente arquivos de instalação, não bancos de dados privados.\n");
        } catch (PackageManager.NameNotFoundException e) {
            b.append("SuperLive não encontrado.\n");
        }
        return b.toString();
    }

    private String inspectFolders() {
        StringBuilder b = header("PASTAS ASSOCIADAS AO APLICATIVO");
        String storage = Environment.getExternalStorageDirectory().getAbsolutePath();
        String[] paths = {
                storage + "/Android/media/" + PACKAGE,
                storage + "/Android/data/" + PACKAGE,
                storage + "/Android/obb/" + PACKAGE,
                "/data/user/0/" + PACKAGE,
                "/data/data/" + PACKAGE
        };
        for (String path : paths) {
            File f = new File(path);
            b.append("\n").append(path).append('\n');
            b.append("Existe: ").append(f.exists()).append(" | Lê: ").append(f.canRead()).append('\n');
            if (f.isDirectory() && f.canRead()) {
                File[] items = f.listFiles();
                if (items == null) b.append("[LISTAGEM NEGADA]\n");
                else {
                    b.append("Itens imediatos: ").append(items.length).append('\n');
                    for (int i = 0; i < Math.min(items.length, 150); i++) {
                        b.append(items[i].isDirectory() ? "DIR  " : "FILE ")
                                .append(items[i].getName()).append('\n');
                    }
                    if (items.length > 150) b.append("[OUTROS ITENS OCULTOS]\n");
                }
            } else b.append("[INEXISTENTE, OCULTO OU PROTEGIDO PELO ANDROID]\n");
        }
        b.append("\nAndroid 11+ restringe arquivos privados e Android/data.\n");
        return b.toString();
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, FOLDER_REQUEST); }
        catch (Exception e) {
            Toast.makeText(this, "Seletor de pastas indisponível", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FOLDER_REQUEST || resultCode != RESULT_OK || data == null) return;
        Uri tree = data.getData();
        if (tree == null) return;
        try {
            getContentResolver().takePersistableUriPermission(
                    tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
        runTask("Lendo pasta autorizada...", () -> inspectTree(tree));
    }

    static final class Node {
        final String id, path;
        final int depth;
        Node(String id, String path, int depth) {
            this.id = id; this.path = path; this.depth = depth;
        }
    }

    private String inspectTree(Uri tree) {
        StringBuilder b = header("ARQUIVOS DA PASTA AUTORIZADA");
        b.append("URI: ").append(tree).append('\n');
        String root;
        try { root = DocumentsContract.getTreeDocumentId(tree); }
        catch (IllegalArgumentException e) { return b.append("Seleção inválida.").toString(); }
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(new Node(root, "", 0));
        int seen = 0;
        String[] cols = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
        };
        while (!queue.isEmpty() && seen < MAX_TREE_ITEMS) {
            Node node = queue.remove();
            if (node.depth > 6) continue;
            Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, node.id);
            try (Cursor cursor = getContentResolver().query(uri, cols, null, null, null)) {
                if (cursor == null) { b.append("[SEM LISTAGEM] ").append(node.path).append('\n'); continue; }
                int iId = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                int iName = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int iMime = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
                int iSize = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
                if (iId < 0 || iName < 0 || iMime < 0) continue;
                while (cursor.moveToNext() && seen < MAX_TREE_ITEMS) {
                    String id = cursor.getString(iId);
                    String name = cursor.getString(iName);
                    String mime = cursor.getString(iMime);
                    if (name == null) name = "(sem nome)";
                    boolean folder = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                    String relative = node.path + "/" + name;
                    b.append(folder ? "DIR  " : "FILE ").append(relative);
                    if (!folder && iSize >= 0 && !cursor.isNull(iSize)) {
                        b.append(" [").append(cursor.getLong(iSize)).append(" bytes]");
                    }
                    b.append('\n');
                    seen++;
                    if (folder && id != null && node.depth < 6) {
                        queue.add(new Node(id, relative, node.depth + 1));
                    }
                }
            } catch (Exception e) {
                b.append("[ERRO OU SEM PERMISSÃO] ").append(node.path)
                        .append(" : ").append(e.getMessage()).append('\n');
            }
        }
        b.append("\nArquivos/pastas encontrados: ").append(seen).append('\n');
        if (seen >= MAX_TREE_ITEMS) b.append("[LIMITE DE INSPEÇÃO]\n");
        return b.toString();
    }
}
