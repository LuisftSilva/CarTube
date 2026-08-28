package com.pi.cartubesafe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://m.youtube.com/";

    private WebView webView;
    private TextView audioOnlyOverlay;
    private boolean videoAllowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogStore.i("MainActivity", "onCreate");
        buildUi();
        configureWebView();
        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setVisibility(View.GONE);
        webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(webView);

        audioOnlyOverlay = new TextView(this);
        audioOnlyOverlay.setText("Modo de condução seguro\nApenas áudio");
        audioOnlyOverlay.setTextColor(Color.WHITE);
        audioOnlyOverlay.setTextSize(22f);
        audioOnlyOverlay.setGravity(Gravity.CENTER);
        audioOnlyOverlay.setBackgroundColor(Color.BLACK);
        audioOnlyOverlay.setVisibility(View.VISIBLE);
        root.addView(audioOnlyOverlay, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
    }

    private void pauseVideo() {
        if (webView == null) return;
        try {
            webView.evaluateJavascript("(function(){document.querySelectorAll('video').forEach(function(v){v.pause();});})()", null);
        } catch (Exception ignored) {
        }
        webView.onPause();
        webView.pauseTimers();
    }

    private void setVideoAllowed(boolean allowed, String reason) {
        if (videoAllowed == allowed) return;
        videoAllowed = allowed;
        LogStore.i("MainActivity", "videoAllowed=" + allowed + "; reason=" + reason);
        applyVideoPolicy();
    }

    private void applyVideoPolicy() {
        if (webView == null || audioOnlyOverlay == null) return;
        webView.setVisibility(videoAllowed ? View.VISIBLE : View.GONE);
        audioOnlyOverlay.setVisibility(videoAllowed ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.resumeTimers();
            webView.onResume();
        }
        setVideoAllowed(false, "resumed_waiting_for_window_focus");
        LogStore.i("MainActivity", "onResume");
        LogStore.syncDriveBestEffort();
    }

    @Override
    protected void onPause() {
        LogStore.i("MainActivity", "onPause -> hide video; audio may continue");
        setVideoAllowed(false, "activity_paused");
        LogStore.syncDriveBestEffort();
        super.onPause();
    }

    @Override
    protected void onStop() {
        LogStore.i("MainActivity", "onStop");
        setVideoAllowed(false, "activity_stopped");
        LogStore.syncDriveBestEffort();
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setVideoAllowed(hasFocus, hasFocus ? "window_focused" : "window_focus_lost");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LogStore.i("MainActivity", "onDestroy");
        if (webView != null) {
            pauseVideo();
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        LogStore.syncDriveBestEffort();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (LogStore.handleDriveLinkResult(this, requestCode, resultCode, data)) {
            Toast.makeText(this, "Log ligado ao Drive.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
