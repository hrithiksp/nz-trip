package com.hrithiksp.nztrip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full-screen wrapper around the trip planner. The page ships inside the app
 * (assets/index.html), so it works with no signal. Links to Maps, booking
 * sites and DOC pages open in the phone's own apps.
 */
public class MainActivity extends Activity {

    private static final String PAGE = "file:///android_asset/index.html";
    private static final Pattern RGB = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)");

    private FrameLayout root;
    private WebView web;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable barTick = new Runnable() {
        @Override public void run() {
            syncBars();
            handler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();

        // Draw edge to edge on every Android version, then pad by the system bars,
        // so the map sits between the status bar (and S25 camera hole) and the nav bar.
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#EEDFB6"));
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(i.left, i.top, i.right, i.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // remembered map style, packing ticks
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);               // the map handles its own pinch-zoom
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);                    // ignore system font scaling so the layout holds
        s.setMediaPlaybackRequiresUserGesture(true);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setBackgroundColor(Color.TRANSPARENT);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));   // Google Maps, browser, etc.
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                syncBars();
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl(PAGE);
    }

    /** Match the bar area and icon colour to the page (parchment, Apple-light or dark). */
    private void syncBars() {
        if (web == null) return;
        web.evaluateJavascript(
                "(function(){var a=document.getElementById('app');if(!a)return '';"
                        + "return getComputedStyle(a.dataset.style==='tale'?a:document.body).backgroundColor})()",
                value -> {
                    if (value == null) return;
                    Matcher m = RGB.matcher(value);
                    if (!m.find()) return;
                    int r = Integer.parseInt(m.group(1)), g = Integer.parseInt(m.group(2)), b = Integer.parseInt(m.group(3));
                    root.setBackgroundColor(Color.rgb(r, g, b));
                    setLightBars(0.299 * r + 0.587 * g + 0.114 * b > 150);
                });
    }

    private void setLightBars(boolean light) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c == null) return;
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            c.setSystemBarsAppearance(light ? mask : 0, mask);
        } else {
            View d = getWindow().getDecorView();
            int f = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (light) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            d.setSystemUiVisibility(f);
        }
    }

    /** Back stops the journey, then returns to the overview, then leaves the app. */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript(
                "(function(){try{if(typeof playing!=='undefined'&&playing){stopPlay();return '1'}"
                        + "if(typeof cur!=='undefined'&&cur.v!=='overview'){show('overview',null,true);return '1'}}"
                        + "catch(e){}return '0'})()",
                r -> { if (r == null || !r.contains("1")) finish(); });
    }

    @Override protected void onResume() { super.onResume(); web.onResume(); handler.post(barTick); }
    @Override protected void onPause() { handler.removeCallbacks(barTick); web.onPause(); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override protected void onDestroy() { handler.removeCallbacks(barTick); web.destroy(); super.onDestroy(); }
}
