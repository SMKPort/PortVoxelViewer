package com.port.calculus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

public class MainActivity extends Activity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff0c0b0f);
        getWindow().setNavigationBarColor(0xff0c0b0f);
        webView = new WebView(this);
        webView.setBackgroundColor(0xff0c0b0f);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new Bridge(), "Android");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class Bridge {
        @JavascriptInterface public void solve(String request) {
            String result;
            try {
                PyObject module = Python.getInstance().getModule("engine");
                result = module.callAttr("solve", request).toString();
            } catch (Exception error) {
                try {
                    result = new JSONObject().put("ok", false)
                        .put("error", "Erro no motor de cálculo: " + error.getMessage()).toString();
                } catch (Exception ignored) {
                    result = "{\"ok\":false,\"error\":\"Erro interno.\"}";
                }
            }
            final String safeResult = JSONObject.quote(result);
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.evaluateJavascript("window.onNativeResult(" + safeResult + ")", null);
                }
            });
        }
    }

    @Override public void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("Android");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
