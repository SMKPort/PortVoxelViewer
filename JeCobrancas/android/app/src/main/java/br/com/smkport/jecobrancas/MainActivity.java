package br.com.smkport.jecobrancas;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import androidx.webkit.WebViewAssetLoader;

public final class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String HOME = "https://" + HOST + "/assets/index.html";
    private static final int REQ_SAVE = 820;
    private static final int REQ_OPEN = 821;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingFileContent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(14,14,14));
        getWindow().setNavigationBarColor(Color.rgb(14,14,14));
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        webView.addJavascriptInterface(new NativeBridge(), "JeNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null && uri.getPath().startsWith("/assets/")) return false;
                if ("https".equalsIgnoreCase(uri.getScheme())) openUrl(uri);
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    startActivityForResult(intent, REQ_OPEN);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Não foi possível abrir os arquivos", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
        webView.loadUrl(HOME);
    }

    private void openUrl(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this,"Nenhum aplicativo para abrir o link",Toast.LENGTH_LONG).show(); }
    }

    public final class NativeBridge {
        @JavascriptInterface public void openWhatsApp(String raw) {
            if (raw == null) return;
            Uri uri;
            try { uri = Uri.parse(raw); } catch (Exception e) { return; }
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"wa.me".equalsIgnoreCase(uri.getHost())) return;
            runOnUiThread(() -> openUrl(uri));
        }

        @JavascriptInterface public void saveFile(String name, String mime, String content) {
            if (name == null || content == null || content.length() > 10000000) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Backup muito grande ou inválido", Toast.LENGTH_LONG).show());
                return;
            }
            final String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_");
            final String safeMime = "text/csv;charset=utf-8".equals(mime) ? "text/csv" : "application/json";
            runOnUiThread(() -> {
                if (pendingFileContent != null) {
                    Toast.makeText(MainActivity.this,"Finalize o salvamento anterior",Toast.LENGTH_SHORT).show();
                    return;
                }
                pendingFileContent = content;
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(safeMime);
                    intent.putExtra(Intent.EXTRA_TITLE, safeName);
                    startActivityForResult(intent, REQ_SAVE);
                } catch (Exception e) {
                    pendingFileContent = null;
                    Toast.makeText(MainActivity.this,"Não foi possível salvar",Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OPEN) {
            if (fileCallback != null) {
                Uri[] values = resultCode == RESULT_OK && data != null && data.getData() != null
                    ? new Uri[]{data.getData()} : null;
                fileCallback.onReceiveValue(values);
                fileCallback = null;
            }
        } else if (requestCode == REQ_SAVE) {
            String contents = pendingFileContent;
            pendingFileContent = null;
            if (resultCode == RESULT_OK && contents != null && data != null && data.getData() != null) {
                try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
                    if (stream == null) throw new Exception("Arquivo indisponível");
                    stream.write(contents.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this,"Arquivo salvo com sucesso",Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this,"Falha ao salvar: "+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
