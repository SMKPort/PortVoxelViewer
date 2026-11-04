package com.port.mathsolver;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;
import java.io.File;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 3001;
    private WebView webView;
    private Uri cameraUri;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(18,13,19));
        getWindow().setNavigationBarColor(Color.rgb(18,13,19));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(16,13,18));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("file:///android_asset/");
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    public final class Bridge {
        @JavascriptInterface public void pickImage() {
            runOnUiThread(MainActivity.this::openImageChooser);
        }
        @JavascriptInterface public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (pm != null) pm.print("PortMathSolver_Exercicio", webView.createPrintDocumentAdapter("PortMathSolver_Exercicio"), new PrintAttributes.Builder().build());
            });
        }
        @JavascriptInterface public void copyText(String value) {
            runOnUiThread(() -> {
                ClipboardManager cb = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("Port Math Solver", value));
                Toast.makeText(MainActivity.this, "Resultado copiado", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void openImageChooser() {
        Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
        gallery.setType("image/*");
        gallery.addCategory(Intent.CATEGORY_OPENABLE);
        Intent chooser = Intent.createChooser(gallery, "Escolher imagem do exercício");
        try {
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists()) dir.mkdirs();
            File photo = new File(dir, "port-scan.jpg");
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photo);
            Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            capture.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            capture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{capture});
        } catch (Exception ignored) { cameraUri = null; }
        startActivityForResult(chooser, PICK_IMAGE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri selected = (data != null && data.getData() != null) ? data.getData() : cameraUri;
            if (selected == null) { sendError("Nenhuma foto encontrada."); return; }
            try {
                InputImage input = InputImage.fromFilePath(this, selected);
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(input)
                    .addOnSuccessListener(result -> sendJs("portOnOcr", result.getText()))
                    .addOnFailureListener(e -> sendError(e.getMessage() == null ? "Reconhecimento não realizado" : e.getMessage()));
            } catch (Exception e) {
                sendError(e.getMessage() == null ? "Falha ao abrir imagem" : e.getMessage());
            }
        }
    }

    private void sendJs(String name, String value) {
        runOnUiThread(() -> webView.evaluateJavascript("window." + name + "(" + JSONObject.quote(value) + ")", null));
    }
    private void sendError(String message) { sendJs("portOnOcrError", message); }
    @Override protected void onDestroy() {
        webView.removeJavascriptInterface("AndroidBridge");
        webView.destroy();
        super.onDestroy();
    }
}
