package br.com.caldocontrol.app;

import android.app.Activity;
import android.print.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int PICK_JSON = 1001;
    private static final int SAVE_DOCUMENT = 1002;
    private static final String APP_HOST = "appassets.androidplatform.net";
    private FrameLayout root;
    private WebView webView;
    private WebView reportView;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingText;
    private WebViewAssetLoader loader;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10,18,26));
        getWindow().setNavigationBarColor(Color.rgb(10,18,26));
        root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new AndroidBridge(), "CaldoAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equals(uri.getScheme()) && APP_HOST.equals(uri.getHost())
                    && uri.getPath() != null && uri.getPath().startsWith("/assets/"));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    String[] accepted = params.getAcceptTypes();
                    boolean image = false;
                    if (accepted != null) for (String t : accepted) if (t != null && (t.startsWith("image/") || t.contains("image"))) image = true;
                    pick.setType(image ? "image/*" : "application/json");
                    startActivityForResult(pick, PICK_JSON);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    cb.onReceiveValue(null);
                    showError("Não foi possível abrir os arquivos.");
                    return true;
                }
            }
        });
        webView.loadUrl("https://" + APP_HOST + "/assets/index.html");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private final class AndroidBridge {
        // HTTPS de leitura: TLS do Android, sem credenciais persistidas, sem redirecionamento, GET somente.
        @JavascriptInterface public void readGatewayJson(String endpoint, String requestId) {
            if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,40}")) return;
            new Thread(() -> {
                String content = null;
                String failure = null;
                java.net.HttpURLConnection conn = null;
                try {
                    java.net.URI uri = new java.net.URI(endpoint);
                    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                        || uri.getRawUserInfo() != null || uri.getRawFragment() != null)
                        throw new IllegalArgumentException("Exige endereço HTTPS sem usuário ou senha.");
                    java.net.URL url = uri.toURL();
                    conn = (java.net.HttpURLConnection)url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    int status = conn.getResponseCode();
                    if (status < 200 || status >= 300) throw new java.io.IOException("HTTP " + status);
                    int size = conn.getContentLength();
                    if (size > 65536) throw new java.io.IOException("Resposta muito grande.");
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    try (java.io.InputStream in = conn.getInputStream()) {
                        byte[] chunk = new byte[4096]; int count;
                        while ((count = in.read(chunk)) != -1) {
                            if (buffer.size() + count > 65536) throw new java.io.IOException("Limite de 64 KB.");
                            buffer.write(chunk, 0, count);
                        }
                    }
                    content = buffer.toString("UTF-8");
                } catch (Exception e) {
                    failure = e instanceof javax.net.ssl.SSLException ?
                        "Falha TLS/certificado. Consulte a TI/automação." : e.getMessage();
                } finally { if (conn != null) conn.disconnect(); }
                final String output = content, error = failure;
                runOnUiThread(() -> {
                    if (webView == null) return;
                    String cb = error == null ? "receive" : "error";
                    String val = error == null ? output : error;
                    webView.evaluateJavascript(
                        "window.CaldoGatewayNative && window.CaldoGatewayNative."+cb+"("+
                        org.json.JSONObject.quote(requestId)+","+
                        org.json.JSONObject.quote(val == null ? "Falha desconhecida" : val)+")", null);
                });
            }, "caldocontrol-gateway-read").start();
        }

        @JavascriptInterface public void recognizePhoto(String imageDataUrl) {
            new PhotoOcr(MainActivity.this, webView).recognize(imageDataUrl);
        }
        @JavascriptInterface public void saveFile(String name, String mime, String data) {
            runOnUiThread(() -> {
                if (data == null || data.length() > 8000000) {
                    showError("Arquivo muito grande para exportar.");
                    return;
                }
                String safeName = name == null ? "caldocontrol.json"
                    : name.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (safeName.length() < 1 || safeName.length() > 120) safeName = "caldocontrol.json";
                String kind = safeName.endsWith(".csv") ? "text/csv" : "application/json";
                pendingText = data;
                try {
                    Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    save.addCategory(Intent.CATEGORY_OPENABLE);
                    save.setType(kind);
                    save.putExtra(Intent.EXTRA_TITLE, safeName);
                    startActivityForResult(save, SAVE_DOCUMENT);
                } catch (Exception e) {
                    pendingText = null;
                    showError("Não foi possível exportar o arquivo.");
                }
            });
        }

        @JavascriptInterface public void printReport(String html) {
            runOnUiThread(() -> {
                if (html == null || html.length() > 1000000) {
                    showError("Relatório muito grande para imprimir.");
                    return;
                }
                if (reportView != null) {
                    root.removeView(reportView);
                    reportView.destroy();
                }
                reportView = new WebView(MainActivity.this);
                reportView.getSettings().setJavaScriptEnabled(false);
                reportView.setVisibility(View.INVISIBLE);
                root.addView(reportView, new FrameLayout.LayoutParams(-1, -1));
                final WebView currentPrint = reportView;
                reportView.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        if (currentPrint != reportView) return;
                        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        if (manager != null) {
                            manager.print("CaldoControl - Relatório por turno",
                                currentPrint.createPrintDocumentAdapter("Relatório CaldoControl"),
                                new PrintAttributes.Builder().build());
                        } else {
                            showError("Serviço de impressão indisponível.");
                        }
                    }
                });
                reportView.loadDataWithBaseURL("https://" + APP_HOST + "/assets/print.html",
                    html, "text/html", "UTF-8", null);
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_JSON) {
            if (fileCallback != null) {
                Uri selected = resultCode == RESULT_OK && data != null ? data.getData() : null;
                fileCallback.onReceiveValue(selected == null ? null : new Uri[]{selected});
                fileCallback = null;
            }
        } else if (requestCode == SAVE_DOCUMENT) {
            try {
                if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingText != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                        if (out == null) throw new IllegalStateException("Saída de arquivo indisponível");
                        out.write(pendingText.getBytes(StandardCharsets.UTF_8));
                    }
                    showError("Arquivo salvo.");
                }
            } catch (Exception e) {
                showError("Erro ao salvar arquivo.");
            } finally {
                pendingText = null;
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            root.removeView(webView);
            webView.destroy();
        }
        if (reportView != null) {
            root.removeView(reportView);
            reportView.destroy();
        }
        super.onDestroy();
    }
}
