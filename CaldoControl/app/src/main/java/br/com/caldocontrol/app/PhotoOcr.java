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
import org.json.JSONArray;
import android.graphics.Rect;

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
    public void recognizeSheet(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/") ||
            !dataUrl.contains(";base64,") || dataUrl.length() > 3500000) {
            deliverSheet("Imagem inválida ou muito grande.", true);
            return;
        }
        try {
            byte[] bytes = Base64.decode(dataUrl.substring(dataUrl.indexOf(";base64,")+8), Base64.DEFAULT);
            if (bytes.length > 2400000) throw new IllegalArgumentException("Imagem excede 2,4 MB.");
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp == null) throw new IllegalArgumentException("Imagem ilegível.");
            TextRecognizer rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            rec.process(InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener(result -> {
                    try {
                        JSONArray lines = new JSONArray();
                        for (com.google.mlkit.vision.text.Text.TextBlock block : result.getTextBlocks()) {
                            for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                                Rect rect = line.getBoundingBox();
                                if (rect != null && lines.length() < 600) {
                                    JSONObject item = new JSONObject();
                                    item.put("text", line.getText());
                                    item.put("x", rect.left);
                                    item.put("y", rect.top);
                                    item.put("width", rect.width());
                                    item.put("height", rect.height());
                                    lines.put(item);
                                }
                            }
                        }
                        JSONObject json = new JSONObject();
                        json.put("text", result.getText());
                        json.put("lines", lines);
                        json.put("imageWidth", bmp.getWidth());
                        json.put("imageHeight", bmp.getHeight());
                        deliverSheet(json.toString(), false);
                    } catch (Exception e) {
                        deliverSheet("Não foi possível organizar a leitura da tabela.", true);
                    } finally {
                        rec.close();
                        bmp.recycle();
                    }
                })
                .addOnFailureListener(e -> {
                    deliverSheet("Falha ao ler a foto da planilha.", true);
                    rec.close();
                    bmp.recycle();
                });
        } catch (Exception e) {
            deliverSheet("Imagem inválida. Tente fotografar a folha novamente.", true);
        }
    }

    private void deliverSheet(String data, boolean error) {
        activity.runOnUiThread(() -> {
            if (webView != null) {
                String callback = error ? "error" : "receive";
                webView.evaluateJavascript("window.CaldoControlTableOCR && window.CaldoControlTableOCR." +
                    callback + "(" + JSONObject.quote(data) + ")", null);
            }
        });
    }

}
