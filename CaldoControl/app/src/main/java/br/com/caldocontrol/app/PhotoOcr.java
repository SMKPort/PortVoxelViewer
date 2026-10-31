package br.com.caldocontrol.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.webkit.WebView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import org.json.JSONObject;

public final class PhotoOcr {
    private final Activity activity;
    private final WebView webView;
    public PhotoOcr(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }
    private void deliver(String value, boolean error) {
        activity.runOnUiThread(() -> {
            if (webView != null) {
                String callback = error ? "error" : "receive";
                webView.evaluateJavascript("window.CaldoControlOCR && window.CaldoControlOCR." +
                    callback + "(" + JSONObject.quote(value) + ")", null);
            }
        });
    }
    public void recognize(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/") ||
            !dataUrl.contains(";base64,") || dataUrl.length() > 3500000) {
            deliver("Imagem inválida ou grande demais.", true);
            return;
        }
        try {
            String encoded = dataUrl.substring(dataUrl.indexOf(";base64,") + 8);
            byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
            if (bytes.length > 2400000) throw new IllegalArgumentException("Imagem muito grande.");
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) throw new IllegalArgumentException("Foto ilegível.");
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(result -> {
                    deliver(result.getText(), false);
                    recognizer.close();
                })
                .addOnFailureListener(e -> {
                    deliver("Não foi possível reconhecer o texto da foto.", true);
                    recognizer.close();
                });
        } catch (Exception e) {
            deliver("Imagem inválida. Selecione uma foto JPG, PNG ou WebP.", true);
        }
    }
}
