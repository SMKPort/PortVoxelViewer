package br.com.smkport.chuvaport;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

public final class MainActivity extends Activity {
    private static final String APP_HOST = "appassets.androidplatform.net";
    private static final String APP_URL = "https://" + APP_HOST + "/assets/index.html";
    private static final int REQUEST_LOCATION = 2107;

    private WebView webView;
    private GeolocationPermissions.Callback pendingLocationCallback;
    private String pendingLocationOrigin;

    private boolean locationGranted() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void onWebsiteLocationRequest(String origin, GeolocationPermissions.Callback callback) {
        final Uri requestOrigin = Uri.parse(origin);
        if (!"https".equalsIgnoreCase(requestOrigin.getScheme()) ||
            !APP_HOST.equalsIgnoreCase(requestOrigin.getHost())) {
            callback.invoke(origin, false, false);
            return;
        }
        if (locationGranted()) {
            callback.invoke(origin, true, false);
            return;
        }
        if (pendingLocationCallback != null) {
            pendingLocationCallback.invoke(pendingLocationOrigin, false, false);
        }
        pendingLocationCallback = callback;
        pendingLocationOrigin = origin;
        requestPermissions(new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_LOCATION);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 13, 19));
        getWindow().setNavigationBarColor(Color.rgb(16, 13, 19));

        webView = new WebView(this);
        setContentView(webView);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                boolean internal = "https".equalsIgnoreCase(uri.getScheme())
                    && APP_HOST.equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null && uri.getPath().startsWith("/assets/");
                if (internal) return false;
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                        // Manter a pagina do aplicativo se nao houver navegador externo.
                    }
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                onWebsiteLocationRequest(origin, callback);
            }
        });

        webView.loadUrl(APP_URL);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && pendingLocationCallback != null) {
            pendingLocationCallback.invoke(pendingLocationOrigin, locationGranted(), false);
            pendingLocationCallback = null;
            pendingLocationOrigin = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        if (pendingLocationCallback != null) {
            pendingLocationCallback.invoke(pendingLocationOrigin, false, false);
            pendingLocationCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
