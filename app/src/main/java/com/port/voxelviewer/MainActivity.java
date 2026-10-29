package com.port.voxelviewer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final int OPEN = 10;
    private static final int SAVE = 11;
    private static final int MAX_IMPORT_BYTES = 16 * 1024 * 1024;
    private static final int MAX_EXPORT_BYTES = 28 * 1024 * 1024;
    private WebView browser;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] dataToSave;
    private String pendingJavascript;
    private boolean ready;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff090b13);
        getWindow().setNavigationBarColor(0xff090b13);
        browser = new WebView(this);
        browser.setBackgroundColor(0xff090b13);
        browser.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        setContentView(browser);

        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(false);
        browser.addJavascriptInterface(new SaveBridge(), "PortAndroid");

        browser.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    startActivityForResult(intent, OPEN);
                    return true;
                } catch (Exception error) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    notifyUser("Não foi possível abrir arquivos");
                    return false;
                }
            }
        });
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                return !req.getUrl().toString().startsWith("file:///android_asset/");
            }
            @Override public void onPageFinished(WebView v, String url) {
                ready = true;
                if (pendingJavascript != null) {
                    v.evaluateJavascript(pendingJavascript, null);
                    pendingJavascript = null;
                }
            }
        });
        browser.loadUrl("file:///android_asset/index.html");
        receiveFile(getIntent());
    }

    private void notifyUser(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    private final class SaveBridge {
        @JavascriptInterface public void saveFile(String dataURL, String name, String mime) {
            if ("image/webp".equals(mime)) saveEncoded(dataURL, name, mime, null, ".webp");
            else if ("video/mp4".equals(mime)) saveEncoded(dataURL, name, mime, null, ".mp4");
            else if ("application/json".equals(mime)) saveEncoded(dataURL, name, mime, null, ".bbmodel");
            else notifyUser("Tipo de exportação não autorizado");
        }
        @JavascriptInterface public void saveGIF(String base64, String name) {
            saveEncoded(base64, name, "image/gif", "GIF89a", ".gif");
        }
        @JavascriptInterface public void savePNG(String base64, String name) {
            saveEncoded(base64, name, "image/png", null, ".png");
        }
        private void saveEncoded(String dataURL, String name, String mime, String signature, String suffix) {
            try {
                String prefix = "data:" + mime + ";base64,";
                if (dataURL == null || !dataURL.startsWith(prefix)
                    || dataURL.length() > MAX_EXPORT_BYTES * 2)
                    throw new IllegalArgumentException("Arquivo de exportação inválido");
                byte[] decoded = Base64.decode(dataURL.substring(prefix.length()), Base64.DEFAULT);
                if (decoded.length < 8 || decoded.length > MAX_EXPORT_BYTES)
                    throw new IllegalArgumentException("Arquivo muito grande ou vazio");
                if (signature != null && !(decoded[0] == 'G' && decoded[1] == 'I' && decoded[2] == 'F'))
                    throw new IllegalArgumentException("GIF inválido");
                String safe = name == null ? "PortVoxel" + suffix
                    : name.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (!safe.toLowerCase().endsWith(suffix)) safe += suffix;
                final String filename = safe;
                runOnUiThread(() -> {
                    if (dataToSave != null) {
                        notifyUser("Aguarde o salvamento anterior");
                        return;
                    }
                    dataToSave = decoded;
                    try {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType(mime);
                        intent.putExtra(Intent.EXTRA_TITLE, filename);
                        startActivityForResult(intent, SAVE);
                    } catch (Exception ex) {
                        dataToSave = null;
                        notifyUser("Não foi possível abrir a pasta de destino");
                    }
                });
            } catch (Exception ex) {
                notifyUser("Falha ao exportar: " + ex.getMessage());
            }
        }
    }

    private void receiveFile(Intent intent) {
        if (intent == null) return;
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) uri = intent.getData();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (android.os.Build.VERSION.SDK_INT >= 33)
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
            else uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri == null) return;
        final Uri source = uri;
        new Thread(() -> {
            try {
                String name = "modelo.bbmodel";
                try (Cursor cursor = getContentResolver().query(source, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String result = cursor.getString(0);
                        if (result != null) name = result;
                    }
                } catch (Exception ignored) { }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (InputStream in = getContentResolver().openInputStream(source)) {
                    if (in == null) throw new Exception("Arquivo não acessível");
                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        if (output.size() + n > MAX_IMPORT_BYTES)
                            throw new Exception("Limite de arquivo: 16 MB");
                        output.write(buffer, 0, n);
                    }
                }
                byte[] bytes = output.toByteArray();
                boolean isPNG = bytes.length >= 8
                    && (bytes[0] & 255) == 137 && bytes[1] == 80
                    && bytes[2] == 78 && bytes[3] == 71
                    && bytes[4] == 13 && bytes[5] == 10
                    && bytes[6] == 26 && bytes[7] == 10;
                String js;
                if (isPNG) {
                    String dataURL = "data:image/png;base64,"
                        + Base64.encodeToString(bytes, Base64.NO_WRAP);
                    js = "window.PortVoxelSkin && window.PortVoxelSkin.openAndroidPng("
                        + JSONObject.quote(dataURL) + "," + JSONObject.quote(name) + ");";
                } else {
                    String text = new String(bytes, StandardCharsets.UTF_8);
                    js = "window.PortVoxelViewer && window.PortVoxelViewer.openAndroidJson("
                        + JSONObject.quote(text) + "," + JSONObject.quote(name) + ");";
                }
                runOnUiThread(() -> {
                    if (ready) browser.evaluateJavascript(js, null);
                    else pendingJavascript = js;
                });
            } catch (Exception ex) {
                notifyUser("Erro ao abrir .bbmodel: " + ex.getMessage());
            }
        }).start();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receiveFile(intent);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == OPEN && fileCallback != null) {
            Uri[] uris = (result == RESULT_OK && data != null && data.getData() != null)
                ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(uris);
            fileCallback = null;
        } else if (request == SAVE) {
            if (result == RESULT_OK && data != null && data.getData() != null && dataToSave != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out == null) throw new Exception("Pasta indisponível");
                    out.write(dataToSave);
                    out.flush();
                    notifyUser("Arquivo salvo com sucesso");
                } catch (Exception ex) {
                    notifyUser("Erro ao salvar: " + ex.getMessage());
                }
            }
            dataToSave = null;
        }
    }
    @Override public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }
    @Override protected void onPause() {
        if (browser != null) browser.onPause();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (browser != null) browser.onResume();
    }
    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (browser != null) {
            browser.stopLoading();
            browser.destroy();
        }
        super.onDestroy();
    }
}
