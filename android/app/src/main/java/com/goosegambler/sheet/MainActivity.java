package com.goosegambler.sheet;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Обёртка над веб-листом персонажа: WebView + мост «Android» для вибрации,
 * сохранения копии в «Загрузки» и режима «не гасить экран».
 */
public class MainActivity extends Activity {
    private static final int FILE_REQ = 42;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(0xFF120F1A);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        s.setSupportZoom(false);

        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Выбери копию персонажа"), FILE_REQ);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == FILE_REQ && fileCallback != null) {
            Uri[] result = null;
            if (res == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Сначала спрашиваем страницу: закрыть окно/вернуться на вкладку «Бой».
        web.evaluateJavascript("(window.onAndroidBack && window.onAndroidBack()) ? 'y' : 'n'", v -> {
            if (v == null || !v.contains("y")) finish();
        });
    }

    /** Методы, доступные из JS как window.Android.* */
    private class Bridge {
        @JavascriptInterface
        public void vibrate(String json) {
            try {
                String[] parts = json.replace("[", "").replace("]", "").split(",");
                long[] pattern = new long[parts.length + 1];
                pattern[0] = 0;
                for (int i = 0; i < parts.length; i++) pattern[i + 1] = Long.parseLong(parts[i].trim());
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v == null || !v.hasVibrator()) return;
                if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, -1));
                else v.vibrate(pattern, -1);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void keepScreenOn(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public String saveFile(String name, String content) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    cv.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) throw new Exception("нет доступа к Загрузкам");
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) { os.write(bytes); }
                    return "Сохранено в «Загрузки»: " + name;
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    File f = new File(dir, name);
                    try (FileOutputStream os = new FileOutputStream(f)) { os.write(bytes); }
                    return "Сохранено: " + f.getAbsolutePath();
                }
            } catch (Exception e) {
                return "Не удалось сохранить: " + e.getMessage();
            }
        }
    }
}
